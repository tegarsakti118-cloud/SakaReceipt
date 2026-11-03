from pathlib import Path

p = Path("app/src/main/java/com/sakareceipt/app/ReceiptPreviewActivity.kt")
s = p.read_text()

old_create = '''        val uri = intent?.data
        if (uri == null) {
            Toast.makeText(this, "Tidak ada gambar yang dipilih", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadImage(uri)'''

new_create = '''        handleIncomingIntent(intent)'''

if old_create not in s:
    raise SystemExit("Bagian onCreate tidak ditemukan.")

s = s.replace(old_create, new_create, 1)

marker = '''    /* ---------------- MEMUAT GAMBAR ---------------- */'''

functions = r'''    /* ---------------- MENERIMA SHARE ANDROID ---------------- */

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) {
            Toast.makeText(this, "Tidak ada data diterima", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        when (incoming.action) {
            Intent.ACTION_SEND -> {
                val sharedText = incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)
                    ?.toString()
                    ?.trim()

                val sharedUri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incoming.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                when {
                    !sharedText.isNullOrEmpty() -> showParsedText(sharedText)
                    sharedUri != null -> loadImage(sharedUri)
                    else -> showShareError()
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    incoming.getParcelableArrayListExtra(
                        Intent.EXTRA_STREAM, Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }

                val firstUri = uris?.firstOrNull()
                if (firstUri != null) {
                    loadImage(firstUri)
                } else {
                    showShareError()
                }
            }

            else -> {
                val uri = incoming.data
                if (uri != null) loadImage(uri) else showShareError()
            }
        }
    }

    private fun showShareError() {
        Toast.makeText(
            this,
            "Data yang dibagikan tidak didukung",
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun showParsedText(raw: String) {
        // Sembunyikan area gambar jika sumber mengirim teks langsung.
        (imageView.parent as? View)?.visibility = View.GONE

        infoView.text = "Teks transaksi diterima dari aplikasi lain"
        displayParsedResult(raw)
    }

    private fun displayParsedResult(raw: String) {
        val parsed = parseReceipt(raw)

        ocrResultView.text = buildString {
            appendLine("HASIL EKSTRAKSI")
            appendLine("Jenis transaksi: ${parsed.type}")
            appendLine("Nominal: ${parsed.amount}")
            appendLine("Tanggal: ${parsed.date}")
            appendLine("Waktu: ${parsed.time}")
            appendLine("Pengirim: ${parsed.sender}")
            appendLine("Penerima: ${parsed.recipient}")
            appendLine("No. referensi: ${parsed.reference}")
            appendLine()
            appendLine("TEKS ASLI")
            append(raw)
        }
    }

'''

if marker not in s:
    raise SystemExit("Lokasi penyisipan fungsi tidak ditemukan.")

s = s.replace(marker, functions + marker, 1)

old_ocr = '''                ocrResultView.text = if (result.isNotEmpty()) {
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

new_ocr = '''                if (result.isNotEmpty()) {
                    displayParsedResult(result)
                } else {
                    ocrResultView.text =
                        "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }'''

if old_ocr not in s:
    raise SystemExit("Bagian hasil OCR tidak ditemukan.")

s = s.replace(old_ocr, new_ocr, 1)

p.write_text(s)
print("Share Intent berhasil ditambahkan.")
