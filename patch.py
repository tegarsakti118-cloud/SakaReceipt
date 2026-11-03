from pathlib import Path

p = Path("app/src/main/java/com/sakareceipt/app/ReceiptPreviewActivity.kt")
s = p.read_text()

old = '''                ocrResultView.text = if (result.isNotEmpty()) {
                    result
                } else {
                    "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }'''

new = '''                ocrResultView.text = if (result.isNotEmpty()) {
                    val parsed = parseReceipt(result)
                    buildString {
                        appendLine("HASIL EKSTRAKSI")
                        appendLine("Jenis transaksi: ${parsed.type}")
                        appendLine("Nominal: ${parsed.amount}")
                        appendLine("Tanggal: ${parsed.date}")
                        appendLine("Waktu: ${parsed.time}")
                        appendLine("Pengirim: ${parsed.sender}")
                        appendLine("Penerima: ${parsed.recipient}")
                        appendLine("No. referensi: ${parsed.reference}")
                        appendLine()
                        appendLine("TEKS OCR ASLI")
                        append(result)
                    }
                } else {
                    "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }'''

if old not in s:
    raise SystemExit("Blok OCR tidak ditemukan. File tidak diubah.")

s = s.replace(old, new, 1)

marker = '''    override fun onDestroy() {'''

parser = r'''    /* ---------------- EKSTRAKSI DATA TRANSAKSI ---------------- */

    private data class ParsedReceipt(
        val type: String = "",
        val amount: String = "",
        val date: String = "",
        val time: String = "",
        val sender: String = "",
        val recipient: String = "",
        val reference: String = ""
    )

    private fun parseReceipt(raw: String): ParsedReceipt {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }

        fun findValue(labels: List<String>): String {
            for (i in lines.indices) {
                val line = lines[i]
                for (label in labels) {
                    val regex = Regex(
                        "^${Regex.escape(label)}\\\\s*[:：-]?\\\\s*(.*)$",
                        RegexOption.IGNORE_CASE
                    )
                    val match = regex.find(line)
                    if (match != null) {
                        val value = match.groupValues[1].trim()
                        if (value.isNotEmpty()) return value
                        if (i + 1 < lines.size) return lines[i + 1]
                    }
                }
            }
            return ""
        }

        val lower = raw.lowercase()

        val type = when {
            "tarik tunai" in lower || "penarikan" in lower -> "Tarik Tunai"
            "setor tunai" in lower || "setoran" in lower -> "Setor Tunai"
            "token listrik" in lower || "pln prabayar" in lower -> "Token PLN"
            "tagihan pln" in lower || "pln pascabayar" in lower -> "Tagihan PLN"
            "bpjs" in lower || "pdam" in lower || "tagihan" in lower -> "Bayar Tagihan"
            "top up" in lower || "topup" in lower -> "Top Up"
            "transfer" in lower -> "Transfer"
            else -> ""
        }

        val amountLine = lines.firstOrNull {
            val l = it.lowercase()
            listOf("total", "nominal", "jumlah", "jumlah transfer", "total pembayaran")
                .any { label -> label in l } &&
                Regex("""(?:rp\\.?\\s*)?[0-9][0-9.,]*""", RegexOption.IGNORE_CASE).containsMatchIn(it)
        } ?: ""

        val amountMatch = Regex(
            """(?:rp\\.?\\s*)?([0-9][0-9.,]*)""",
            RegexOption.IGNORE_CASE
        ).find(amountLine)

        val date = Regex(
            """\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b"""
        ).find(raw)?.value ?: ""

        val time = Regex(
            """\\b(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?\\b"""
        ).find(raw)?.value ?: ""

        return ParsedReceipt(
            type = type,
            amount = amountMatch?.groupValues?.get(1) ?: "",
            date = date,
            time = time,
            sender = findValue(listOf("Nama pengirim", "Pengirim", "Dari")),
            recipient = findValue(listOf("Nama penerima", "Penerima", "Kepada", "Tujuan")),
            reference = findValue(
                listOf("ID transaksi", "No. referensi", "Nomor referensi", "Reference", "Ref", "Kode transaksi")
            )
        )
    }

'''

if marker not in s:
    raise SystemExit("Lokasi penyisipan parser tidak ditemukan. File tidak diubah.")

s = s.replace(marker, parser + marker, 1)
p.write_text(s)
print("Patch Stage 2C berhasil diterapkan.")
