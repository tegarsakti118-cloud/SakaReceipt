from pathlib import Path

p = Path("app/src/main/java/com/sakareceipt/app/ReceiptPreviewActivity.kt")
s = p.read_text()

start = s.find("    private fun parseReceipt(raw: String): ParsedReceipt {")
end = s.find("    override fun onDestroy() {", start)

if start < 0 or end < 0:
    raise SystemExit("Fungsi parser tidak ditemukan. File tidak diubah.")

new = r'''    private fun parseReceipt(raw: String): ParsedReceipt {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val lower = raw.lowercase()

        fun findLabelValue(labels: List<String>): String {
            for (i in lines.indices) {
                val line = lines[i]

                for (label in labels) {
                    val pattern = Regex(
                        "^${Regex.escape(label)}\\s*[:：-]?\\s*(.*)$",
                        RegexOption.IGNORE_CASE
                    )

                    val match = pattern.find(line) ?: continue
                    val sameLine = match.groupValues[1].trim()

                    if (sameLine.isNotEmpty()) return sameLine

                    for (next in i + 1 until minOf(i + 3, lines.size)) {
                        val candidate = lines[next]
                        if (!candidate.matches(Regex("(?i)^(dari|ke|kepada|pengirim|penerima|tujuan|total|nominal|tanggal|waktu|referensi|no\\. transaksi).*$"))) {
                            return candidate
                        }
                    }
                }
            }
            return ""
        }

        val type = when {
            "bi-fast" in lower -> "Transfer BI-FAST"
            "transfer" in lower -> "Transfer"
            "tarik tunai" in lower || "penarikan" in lower -> "Tarik Tunai"
            "setor tunai" in lower || "setoran" in lower -> "Setor Tunai"
            "token listrik" in lower || "pln prabayar" in lower -> "Token PLN"
            "pln" in lower || "pdam" in lower || "bpjs" in lower ||
                "tagihan" in lower || "pembayaran" in lower -> "Bayar Tagihan"
            "top up" in lower || "topup" in lower -> "Top Up"
            else -> ""
        }

        // Cari nominal berdasarkan label agar biaya admin tidak salah terbaca.
        val amountLabels = listOf(
            "jumlah transfer", "nominal transfer", "jumlah pembayaran",
            "total pembayaran", "jumlah total", "nominal", "jumlah", "total"
        )

        var amount = ""

        for (i in lines.indices) {
            val line = lines[i].lowercase()
            if (amountLabels.any { line.contains(it) } &&
                !line.contains("biaya") && !line.contains("admin") &&
                !line.contains("fee")
            ) {
                val sameLineAmount = Regex(
                    """(?:rp\\s*)[0-9][0-9.,]*""",
                    RegexOption.IGNORE_CASE
                ).find(lines[i])?.value

                if (sameLineAmount != null) {
                    amount = sameLineAmount
                    break
                }

                for (j in i + 1 until minOf(i + 4, lines.size)) {
                    val candidate = lines[j]
                    if (candidate.contains("biaya", true) ||
                        candidate.contains("admin", true) ||
                        candidate.contains("fee", true)
                    ) continue

                    val found = Regex(
                        """(?:rp\\s*)[0-9][0-9.,]*""",
                        RegexOption.IGNORE_CASE
                    ).find(candidate)?.value

                    if (found != null) {
                        amount = found
                        break
                    }
                }

                if (amount.isNotEmpty()) break
            }
        }

        // Fallback: ambil nominal rupiah pertama jika label tidak dikenali.
        if (amount.isEmpty()) {
            amount = Regex(
                """rp\\s*[0-9][0-9.,]*""",
                RegexOption.IGNORE_CASE
            ).find(raw)?.value.orEmpty()
        }

        val date = Regex(
            """\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b"""
        ).find(raw)?.value.orEmpty()

        val time = Regex(
            """\\b(?:[01]?\\d|2[0-3]):[0-5]\\d(?::[0-5]\\d)?\\b"""
        ).find(raw)?.value.orEmpty()

        val reference = findLabelValue(
            listOf("No. Transaksi", "No Transaksi", "ID Transaksi",
                "No. Referensi", "Nomor Referensi", "Reference", "Kode Transaksi")
        ).ifBlank {
            Regex("""\\b\\d{12,}\\b""").find(raw)?.value.orEmpty()
        }

        var sender = findLabelValue(
            listOf("Nama Pengirim", "Pengirim", "Dari")
        )
        var recipient = findLabelValue(
            listOf("Nama Penerima", "Penerima", "Kepada", "Tujuan")
        )

        // Fallback untuk format OCR yang memisahkan nama dan rekening.
        if (sender.isBlank() || recipient.isBlank()) {
            val accountIndexes = lines.indices.filter {
                lines[it].matches(
                    Regex("(?i).*(?:BRI|BCA|BNI|MANDIRI|SEABANK|DANA|GOPAY|OVO|SHOPEEPAY)\\s*[:.-].*")
                )
            }

            if (accountIndexes.size >= 2) {
                val first = accountIndexes[0]
                val second = accountIndexes[1]

                if (sender.isBlank() && first > 0) {
                    sender = lines[first - 1]
                }
                if (recipient.isBlank() && second > 0) {
                    recipient = lines[second - 1]
                }
            }
        }

        return ParsedReceipt(
            type = type,
            amount = amount,
            date = date,
            time = time,
            sender = sender,
            recipient = recipient,
            reference = reference
        )
    }

'''

s = s[:start] + new + s[end:]
p.write_text(s)
print("Universal OCR Parser berhasil dipasang.")
