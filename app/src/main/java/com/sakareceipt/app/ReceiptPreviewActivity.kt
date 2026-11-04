```kotlin
package com.sakareceipt.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.InputStream

class ReceiptPreviewActivity : Activity() {

    private lateinit var imageView: ImageView
    private lateinit var infoView: TextView
    private lateinit var ocrResultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(246, 248, 250))
            isFillViewport = true
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        infoView = TextView(this).apply {
            text = "Memproses Gambar Struk..."
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 41, 59))
        }

        imageView = ImageView(this).apply {
            adjustViewBounds = true
            maxHeight = 600
        }

        ocrResultView = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.rgb(50, 60, 70))
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
            setTextIsSelectable(true)
        }

        column.addView(infoView)
        column.addView(gapV(16))
        column.addView(imageView)
        column.addView(gapV(16))
        column.addView(ocrResultView)

        root.addView(column)
        setContentView(root)

        handleIncomingIntent(intent)
    }

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
                    !sharedText.isNullOrEmpty() -> displayParsedResult(sharedText)
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

    private fun loadImage(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(bitmap)
            runOCR(bitmap)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun runOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                if (resultText.isNotEmpty()) {
                    displayParsedResult(resultText)
                } else {
                    ocrResultView.text =
                        "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }
            }
            .addOnFailureListener { e ->
                ocrResultView.text = "Gagal membaca gambar OCR: ${e.message}"
            }
    }

    private fun displayParsedResult(raw: String) {
        // DETEKSI OTOMATIS TOKEN PLN
        if (PLNReceiptParser.isPLNReceipt(raw)) {
            val plnData = PLNReceiptParser.parse(raw)
            val intent = Intent(this, PLNReceiptEditorActivity::class.java).apply {
                putExtra("PLN_DATA", plnData)
            }
            startActivity(intent)
            finish()
            return
        }

        // EKSTRAKSI TRANSAKSI UMUM (Transfer, DANA, SeaBank, BRI, dll)
        val parsed = parseReceipt(raw)

        infoView.text = "Hasil Ekstraksi Transaksi"
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
            appendLine("TEKS OCR ASLI")
            append(raw)
        }
    }

    /* ---------------- EKSTRAKSI DATA TRANSAKSI UMUM ---------------- */

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
                    """(?:rp\s*)[0-9][0-9.,]*""",
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
                        """(?:rp\s*)[0-9][0-9.,]*""",
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

        if (amount.isEmpty()) {
            amount = Regex(
                """rp\s*[0-9][0-9.,]*""",
                RegexOption.IGNORE_CASE
            ).find(raw)?.value.orEmpty()
        }

        val date = Regex(
            """\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2})\b"""
        ).find(raw)?.value.orEmpty()

        val time = Regex(
            """\b(?:[01]?\d|2[0-3]):[0-5]\d(?::[0-5]\d)?\b"""
        ).find(raw)?.value.orEmpty()

        val reference = findLabelValue(
            listOf("No. Transaksi", "No Transaksi", "ID Transaksi",
                "No. Referensi", "Nomor Referensi", "Reference", "Kode Transaksi")
        ).ifBlank {
            Regex("""\b\d{12,}\b""").find(raw)?.value.orEmpty()
        }

        var sender = findLabelValue(
            listOf("Nama Pengirim", "Pengirim", "Dari")
        )
        var recipient = findLabelValue(
            listOf("Nama Penerima", "Penerima", "Kepada", "Tujuan")
        )

        if (sender.isBlank() || recipient.isBlank()) {
            val accountIndexes = lines.indices.filter {
                lines[it].matches(
                    Regex("(?i).*(?:BRI|BCA|BNI|MANDIRI|SEABANK|DANA|GOPAY|OVO|SHOPEEPAY)\s*[:.-].*")
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

    private fun gapV(dp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp)
    }
}
```