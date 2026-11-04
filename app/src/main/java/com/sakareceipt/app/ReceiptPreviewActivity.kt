
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
import com.google.mlkit.vision.text.Text
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
        textRecognizer = TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )
        handleIncomingIntent(intent)
    }

    /* ---------------- SHARE ANDROID ---------------- */

    private fun handleIncomingIntent(incoming: Intent?) {
        if (incoming == null) {
            showShareError()
            return
        }

        when (incoming.action) {
            Intent.ACTION_SEND -> {
                val sharedText = incoming.getCharSequenceExtra(
                    Intent.EXTRA_TEXT
                )?.toString()?.trim()

                val sharedUri = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    incoming.getParcelableExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    incoming.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                when {
                    sharedUri != null -> loadImage(sharedUri)
                    !sharedText.isNullOrEmpty() -> showParsedText(sharedText)
                    else -> showShareError()
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    incoming.getParcelableArrayListExtra(
                        Intent.EXTRA_STREAM,
                        Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    incoming.getParcelableArrayListExtra<Uri>(
                        Intent.EXTRA_STREAM
                    )
                }

                val firstUri = uris?.firstOrNull()
                if (firstUri != null) loadImage(firstUri)
                else showShareError()
            }

            else -> {
                val uri = incoming.data
                if (uri != null) loadImage(uri)
                else showShareError()
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
            appendLine("Rekening pengirim: ${parsed.senderAccount}")
            appendLine("Penerima: ${parsed.recipient}")
            appendLine("Rekening penerima: ${parsed.recipientAccount}")
            appendLine("No. referensi: ${parsed.reference}")
            appendLine()
            appendLine("TEKS ASLI")
            append(raw)
        }
    }

    /* ---------------- GAMBAR ---------------- */

    private fun loadImage(uri: Uri): Boolean {
        val bitmap = decodeSampled(uri)
        if (bitmap == null) {
            Toast.makeText(
                this,
                "Gambar tidak bisa dibuka",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        imageView.setImageBitmap(bitmap)
        infoView.text = "Ukuran gambar: ${bitmap.width} x ${bitmap.height} px"
        runOcr(bitmap)
        return true
    }

    private fun decodeSampled(uri: Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            val boundsStream = contentResolver.openInputStream(uri)
                ?: return null

            boundsStream.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null
            }

            var sample = 1

            while (
                bounds.outWidth / (sample * 2) >= MAX_DECODE_PX ||
                bounds.outHeight / (sample * 2) >= MAX_DECODE_PX
            ) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
            }

            val dataStream = contentResolver.openInputStream(uri)
                ?: return null

            dataStream.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca gambar: $uri", e)
            null
        }
    }

    /* ---------------- OCR ---------------- */

    private fun runOcr(bitmap: Bitmap) {
        ocrResultView.text = "Sedang membaca teks gambar..."

        val image = InputImage.fromBitmap(bitmap, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val result = rebuildRows(
                    visionText,
                    bitmap.height
                ).trim().ifEmpty {
                    visionText.text.trim()
                }

                if (result.isNotEmpty()) {
                    displayParsedResult(result)
                } else {
                    ocrResultView.text =
                        "Tidak ada teks yang terdeteksi. Coba gambar yang lebih jelas."
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "OCR gagal", error)
                ocrResultView.text =
                    "Gagal membaca teks. Silakan coba lagi."
            }
    }

    /* ---------------- SUSUN BARIS OCR ---------------- */

    private fun rebuildRows(
        visionText: Text,
        imageHeight: Int
    ): String {
        class Piece(
            val text: String,
            val left: Int,
            val centerY: Float,
            val height: Float
        )

        val clockRegex = Regex("""^\d{1,2}[:.]\d{2}\b.{0,6}$""")
        val pieces = mutableListOf<Piece>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                val value = line.text.trim()
                if (value.isEmpty()) continue

                val cy = box.exactCenterY()

                if (
                    imageHeight > 0 &&
                    cy < imageHeight * 0.06f &&
                    clockRegex.matches(value)
                ) continue

                pieces.add(
                    Piece(
                        value,
                        box.left,
                        cy,
                        box.height().toFloat()
                    )
                )
            }
        }

        if (pieces.isEmpty()) return ""

        pieces.sortBy { it.centerY }

        val rows = mutableListOf<MutableList<Piece>>()
        var rowCenter = 0f
        var rowHeight = 0f

        for (p in pieces) {
            val current = rows.lastOrNull()
            val tolerance = 0.5f * minOf(p.height, rowHeight)

            if (
                current != null &&
                kotlin.math.abs(p.centerY - rowCenter) <= tolerance
            ) {
                current.add(p)
                rowCenter = current.map { it.centerY }.average().toFloat()
                rowHeight = current.map { it.height }.average().toFloat()
            } else {
                rows.add(mutableListOf(p))
                rowCenter = p.centerY
                rowHeight = p.height
            }
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text }
        }
    }

    /* ---------------- PARSED DATA ---------------- */

    private data class ParsedReceipt(
        val type: String = "",
        val amount: String = "",
        val date: String = "",
        val time: String = "",
        val sender: String = "",
        val senderAccount: String = "",
        val recipient: String = "",
        val recipientAccount: String = "",
        val reference: String = ""
    )

    
private fun parseReceipt(raw: String): ParsedReceipt {
    val lines = raw.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val lower = raw.lowercase()

    fun valueAfterLabel(line: String, label: String): String {
        return line.replaceFirst(
            Regex("""(?i)^${Regex.escape(label)}\s*:\s*"""),
            ""
        ).trim()
    }

    fun cleanAccount(value: String): String {
        return value.replace(Regex("""\s+"""), "").trim()
    }

    fun isNameValue(value: String): Boolean {
        val v = value.trim()
        if (v.length < 2 || v.length > 60) return false
        if (v.any { it.isDigit() }) return false
        if (v.none { it.isLetter() }) return false

        val invalid = Regex(
            """(?i)\b(rekening|nominal|transfer|transaksi|berhasil|tujuan|pengirim|penerima|struk|token|pln|total|admin|biaya|bank|detail|reference|account|payment|bri|seabank|dana)\b"""
        )

        return !invalid.containsMatchIn(v)
    }

    // JENIS TRANSAKSI
    val type = when {
        "bi-fast" in lower -> "Transfer BI-FAST"
        "token listrik" in lower || "pln prabayar" in lower ->
            "Token PLN"
        "tarik tunai" in lower || "penarikan" in lower ->
            "Tarik Tunai"
        "setor tunai" in lower || "setoran" in lower ->
            "Setor Tunai"
        "transfer" in lower || "send money" in lower ||
            "kirim uang" in lower -> "Transfer"
        "pln" in lower || "pdam" in lower ||
            "bpjs" in lower || "tagihan" in lower ||
            "pembayaran" in lower -> "Bayar Tagihan"
        "top up" in lower || "topup" in lower -> "Top Up"
        else -> ""
    }

    // NOMINAL
    val amount = lines.firstNotNullOfOrNull { line ->
        if (
            Regex("""(?i)\b(biaya|admin|fee|total|pajak|saldo|jumlah total)\b""")
                .containsMatchIn(line)
        ) return@firstNotNullOfOrNull null

        Regex("""(?i)rp\.?\s*[\d.,]+""")
            .find(line)?.value?.trim()
    }.orEmpty()

    // TANGGAL, termasuk format OCR 31 Oct 202619:00
    val monthNames =
        """januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember|jan|feb|mar|apr|jun|jul|agu|aug|sep|okt|oct|nov|des|dec"""

    val dateRegex = Regex(
        """(?i)\b\d{1,2}\s+(?:$monthNames)\s+\d{4}(?=\s|[,|:]|\d{1,2}[:.]\d{2}|$)|\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b|\b\d{4}[/-]\d{1,2}[/-]\d{1,2}\b"""
    )

    val date = dateRegex.find(raw)?.value.orEmpty()

    // WAKTU
    val time = Regex(
        """\b(?:[01]?\d|2[0-3]):[0-5]\d(?::[0-5]\d)?\b"""
    ).find(raw)?.value.orEmpty()

    // REFERENSI
    val reference = lines.firstNotNullOfOrNull { line ->
        val match = Regex(
            """(?i)^(?:no\.?\s*referensi|nomor\s*referensi|kode\s*referensi)\s*[:：]?\s*(.*)$"""
        ).find(line) ?: return@firstNotNullOfOrNull null

        match.groupValues[1].trim()
            .takeIf { it.matches(Regex("""[A-Za-z0-9\-/]{4,}""")) }
    }.orEmpty()

    var sender = ""
    var senderAccount = ""
    var recipient = ""
    var recipientAccount = ""

    // FORMAT SEABANK
    val fromLine = lines.firstOrNull {
        it.startsWith("Dari ", true)
    }

    if (fromLine != null) {
        sender = fromLine.substringAfter("Dari ").trim()
    }

    val toLine = lines.firstOrNull {
        it.startsWith("Ke ", true)
    }

    if (toLine != null) {
        recipient = toLine.substringAfter("Ke ").trim()
    }

    val seaBankLine = lines.firstOrNull {
        it.startsWith("SeaBank:", true)
    }

    if (seaBankLine != null) {
        senderAccount = cleanAccount(
            valueAfterLabel(seaBankLine, "SeaBank")
        )
    }

    val briLine = lines.firstOrNull {
        it.startsWith("BRI:", true)
    }

    if (briLine != null) {
        recipientAccount = cleanAccount(
            valueAfterLabel(briLine, "BRI")
        )
    }

    // FORMAT DANA
    val nameIndex = lines.indexOfFirst {
        it.startsWith("Name ", true)
    }

    if (nameIndex >= 0) {
        recipient = lines[nameIndex]
            .substringAfter("Name ", "")
            .trim()

        val continuation = lines.getOrNull(nameIndex + 1)
            ?.takeIf { it.matches(Regex("""[A-Za-z]{1,5}""")) }
            .orEmpty()

        recipient += continuation
        sender = "DANA"
    }

    val bankAccountIndex = lines.indexOfFirst {
        it.contains("Bank Account", true)
    }

    if (bankAccountIndex >= 0) {
        val accountLine = lines[bankAccountIndex]

        val masked = Regex(
            """\*{2,}\s*\d+"""
        ).find(accountLine)?.value.orEmpty()

        val continuation = lines.getOrNull(bankAccountIndex + 1)
            ?.takeIf { it.matches(Regex("""\d{2,}""")) }
            .orEmpty()

        if (masked.isNotEmpty()) {
            recipientAccount = cleanAccount(masked + continuation)
        }
    }

    // FORMAT QITA BRI
    val briIndexes = lines.indices.filter { index ->
        Regex("""(?i)^BANK\s+BRI\s*-\s*5655""")
            .containsMatchIn(lines[index])
    }

    if (briIndexes.size >= 2) {
        fun getBriAccount(index: Int): String {
            val remainder = lines[index].replaceFirst(
                Regex("""(?i)^BANK\s+BRI\s*-\s*5655"""),
                ""
            )
            return cleanAccount(remainder)
        }

        senderAccount = getBriAccount(briIndexes[0])
        recipientAccount = getBriAccount(briIndexes[1])

        fun findNameBefore(index: Int, lowerLimit: Int): String {
            for (i in index - 1 downTo lowerLimit) {
                if (isNameValue(lines[i])) return lines[i]
            }
            return ""
        }

        val senderName = findNameBefore(briIndexes[0], 0)
        val recipientName = findNameBefore(
            briIndexes[1],
            briIndexes[0] + 1
        )

        if (senderName.isNotEmpty()) sender = senderName
        if (recipientName.isNotEmpty()) recipient = recipientName
    }

    return ParsedReceipt(
        type = type,
        amount = amount,
        date = date,
        time = time,
        sender = sender,
        senderAccount = senderAccount,
        recipient = recipient,
        recipientAccount = recipientAccount,
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
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == REQUEST_RE_PICK &&
            resultCode == RESULT_OK
        ) {
            val uri = data?.data ?: return
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
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                setMargins(dp(16), dp(8), dp(16), dp(8))
            }
        )

        infoView = text("", 12f, muted)

        root.addView(
            infoView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(20), 0, dp(20), dp(4))
            }
        )

        ocrResultView = text(
            "Hasil pembacaan teks akan muncul di sini.",
            13f,
            dark
        ).apply {
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
            text(
                "Pastikan teks pada gambar terlihat jelas sebelum diproses.",
                12f,
                muted
            ),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
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

        val title = text(
            "Preview Struk",
            16f,
            dark,
            bold = true
        ).apply {
            gravity = Gravity.END
        }

        bar.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
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

        val pickAgain = text(
            "Pilih Ulang",
            14f,
            Color.WHITE,
            bold = true
        ).apply {
            gravity = Gravity.CENTER
            background = rounded(teal, 14f)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            setOnClickListener { openPicker() }
        }

        bar.addView(
            pickAgain,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        return bar
    }

    /* ---------------- HELPER ---------------- */

    private fun rounded(
        color: Int,
        radiusDp: Float
    ): GradientDrawable =
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
