```kotlin
package com.sakareceipt.app

object PLNReceiptParser {

    private val PLN_KEYWORDS = listOf(
        "pln prabayar", "token listrik", "nomor meter", "id pelanggan",
        "stroom token", "kwh", "nomor token", "pembelian token", "stroom/token"
    )

    fun isPLNReceipt(rawText: String): Boolean {
        val lower = rawText.lowercase()
        var matchCount = 0
        for (kw in PLN_KEYWORDS) {
            if (lower.contains(kw)) {
                matchCount++
                if (matchCount >= 2) return true
            }
        }
        return false
    }

    fun parse(rawText: String): PLNReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lower = rawText.lowercase()

        val tokenNumber = extract20DigitToken(rawText)
        val customerName = extractField(lines, listOf("nama", "nama pelanggan", "customer"))
        val idPelanggan = extractField(lines, listOf("id pelanggan", "idpel", "id pel"))
        val meterNumber = extractField(lines, listOf("no meter", "no. meter", "nomor meter"))
        val refNumber = extractReferenceNumber(rawText, tokenNumber)
        val kwh = extractKwh(rawText)
        val date = extractDate(rawText)
        val time = extractTime(rawText)

        val totalAmount = extractAmount(rawText, listOf("total", "total bayar", "total pembayaran"))
        val adminFee = extractAmount(rawText, listOf("admin", "biaya admin"))
        val tokenAmount = extractAmount(rawText, listOf("nominal", "stroom"))

        return PLNReceiptData(
            customerName = customerName,
            idPelanggan = idPelanggan,
            meterNumber = if (meterNumber.isNotBlank()) meterNumber else idPelanggan,
            tokenNumber = tokenNumber,
            tokenAmount = tokenAmount,
            adminFee = adminFee,
            totalAmount = totalAmount.ifBlank { tokenAmount },
            date = date,
            time = time,
            referenceNumber = refNumber,
            kwh = kwh,
            isSuccess = lower.contains("berhasil") || lower.contains("sukses")
        )
    }

    private fun extract20DigitToken(rawText: String): String {
        val regex20Digit = Regex("""\b\d{20}\b""")
        val matchDirect = regex20Digit.find(rawText)
        if (matchDirect != null) return matchDirect.value

        val regexGroups = Regex("""\b(\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4})\b""")
        val matchGroup = regexGroups.find(rawText)
        if (matchGroup != null) {
            return matchGroup.value.replace(Regex("""[\s-]"""), "")
        }

        val allDigitsInText = rawText.lines().map { line ->
            line.replace(Regex("""[^\d]"""), "")
        }.filter { it.length == 20 }

        return allDigitsInText.firstOrNull() ?: ""
    }

    private fun extractField(lines: List<String>, labels: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i]
            for (label in labels) {
                if (line.lowercase().startsWith(label) || line.lowercase().contains("$label :")) {
                    val parts = line.split(":", limit = 2)
                    if (parts.size > 1 && parts[1].trim().isNotEmpty()) {
                        return parts[1].trim()
                    }
                    if (i + 1 < lines.size) {
                        return lines[i + 1].trim()
                    }
                }
            }
        }
        return ""
    }

    private fun extractReferenceNumber(rawText: String, tokenNumber: String): String {
        val lines = rawText.lines()
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("ref") || lower.contains("referensi") || lower.contains("no. ref")) {
                val digits = line.replace(Regex("""[^\d]"""), "")
                if (digits.isNotEmpty() && digits != tokenNumber) {
                    return digits
                }
            }
        }
        return ""
    }

    private fun extractKwh(rawText: String): String {
        val regex = Regex("""(\d+[.,]?\d*)\s*(?:kwh|KWH)""", RegexOption.IGNORE_CASE)
        val match = regex.find(rawText)
        return match?.groupValues?.get(1)?.let { "$it KWH" } ?: ""
    }

    private fun extractDate(rawText: String): String {
        val regex = Regex("""\b\d{4}-\d{2}-\d{2}\b|\b\d{2}[/-]\d{2}[/-]\d{4}\b""")
        return regex.find(rawText)?.value ?: ""
    }

    private fun extractTime(rawText: String): String {
        val regex = Regex("""\b\d{2}:\d{2}(?::\d{2})?\b""")
        return regex.find(rawText)?.value ?: ""
    }

    private fun extractAmount(rawText: String, labels: List<String>): String {
        val lines = rawText.lines()
        for (line in lines) {
            val lower = line.lowercase()
            for (label in labels) {
                if (lower.contains(label)) {
                    val match = Regex("""(?:rp\.?\s*)?([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE).find(line)
                    if (match != null) return match.groupValues[1]
                }
            }
        }
        return ""
    }
}
```