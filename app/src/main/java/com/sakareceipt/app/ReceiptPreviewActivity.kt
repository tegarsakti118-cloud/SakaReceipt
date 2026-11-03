package com.sakareceipt.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ReceiptPreviewActivity : Activity() {

    private companion object {
        const val REQUEST_RE_PICK = 2001
        const val MAX_DECODE_PX = 1600
        const val TAG = "ReceiptPreview"
    }

    private val teal = Color.rgb(23, 168, 151)
    private val dark = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(110, 120, 135)
    private val bg = Color.rgb(246, 248, 250)
    private val frameBg = Color.rgb(226, 232, 240)

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()
    private fun dpF(v: Float): Float = v * density

    private lateinit var imageView: ImageView
    private lateinit var infoView: TextView
    private lateinit var ocrResultView: TextView
    private lateinit var textRecognizer: TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        handleIncomingIntent(intent)
    }

    /* ---------------- MENERIMA SHARE ANDROID ---------------- */

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

    /* ---------------- MEMUAT GAMBAR ---------------- */

    private fun loadImage(uri: Uri): Boolean {
        val bitmap = decodeSampled(uri)
        if (bitmap == null) {
            Toast.makeText(this, "Gambar tidak bisa dibuka", Toast.LENGTH_SHORT).show()
            return false
        }
        imageView.setImageBitmap(bitmap)
        infoView.text = "Ukuran gambar: ${bitmap.width} x ${bitmap.height} px"
        runOcr(bitmap)
        return true
    }

    // Membaca ukuran asli dulu, lalu memperkecil saat decode agar tidak kehabisan memori
    private fun decodeSampled(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = contentResolver.openInputStream(uri) ?: return null
            boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= MAX_DECODE_PX ||
                bounds.outHeight / (sample * 2) >= MAX_DECODE_PX
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val dataStream = contentResolver.openInputStream(uri) ?: return null
            dataStream.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca gambar: $uri", e)
            null
        }
    }

    /* ---------------- OCR TEKS ---------------- */

    private fun runOcr(bitmap: Bitmap) {
        ocrResultView.text = "Sedang membaca teks gambar..."

        val image = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val result = visionText.text.trim()
                if (result.isNotEmpty()) {
                    displayParsedResult(result)
                } else {
                    ocrResultView.text =
                        "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "OCR gagal", error)
                ocrResultView.text = "Gagal membaca teks. Silakan coba lagi."
            }
    }

    /* ---------------- EKSTRAKSI DATA TRANSAKSI ---------------- */

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

    override fun onDestroy() {
        if (::textRecognizer.isInitialized) {
            textRecognizer.close()
        }
        super.onDestroy()
    }

    /* ---------------- PILIH ULANG ---------------- */

    @Suppress("DEPRECATION")
    private fun openPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(
            Intent.createChooser(intent, "Pilih screenshot struk"),
            REQUEST_RE_PICK
        )
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RE_PICK && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            // Jika gagal, gambar sebelumnya tetap ditampilkan
            loadImage(uri)
        }
    }

    /* ---------------- TAMPILAN ---------------- */

    private fun buildLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        root.addView(buildTopBar(), matchWrap())

        val frame = FrameLayout(this).apply {
            background = rounded(frameBg, 18f)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Preview screenshot transaksi"
        }
        frame.addView(
            imageView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            frame,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(dp(16), dp(8), dp(16), dp(8))
            }
        )

        infoView = text("", 12f, muted)
        root.addView(
            infoView,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(20), 0, dp(20), dp(4))
            }
        )
        ocrResultView = text("Hasil pembacaan teks akan muncul di sini.", 13f, dark).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val ocrScroll = ScrollView(this).apply {
            background = rounded(Color.WHITE, 12f)
            isFillViewport = true
            addView(ocrResultView)
        }

        root.addView(
            ocrScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(130)
            ).apply {
                setMargins(dp(16), dp(4), dp(16), dp(8))
            }
        )

        root.addView(
            text("Pastikan teks pada gambar terlihat jelas sebelum diproses.", 12f, muted),
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(20), 0, dp(20), dp(12))
            }
        )

        root.addView(buildBottomBar(), matchWrap())
        return root
    }

    private fun buildTopBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val back = text("‹  Kembali", 14f, teal, bold = true).apply {
            isClickable = true
            setOnClickListener { finish() }
        }
        bar.addView(back)

        val title = text("Preview Struk", 16f, dark, bold = true).apply {
            gravity = Gravity.END
        }
        bar.addView(
            title,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
        return bar
    }

    private fun buildBottomBar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        val pickAgain = text("Pilih Ulang", 14f, Color.WHITE, bold = true).apply {
            gravity = Gravity.CENTER
            background = rounded(teal, 14f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            setOnClickListener { openPicker() }
        }
        bar.addView(
            pickAgain,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        return bar
    }

    /* ---------------- HELPER ---------------- */

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpF(radiusDp)
        }

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}