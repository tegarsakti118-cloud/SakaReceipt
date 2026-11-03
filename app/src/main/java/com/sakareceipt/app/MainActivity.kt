package com.sakareceipt.app

import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/* ------------------------------------------------------------------ */
/*  IKON VEKTOR (digambar dengan Canvas, tidak perlu file drawable)    */
/* ------------------------------------------------------------------ */

enum class IconType {
    RECEIPT, TRANSFER, BOLT, DOCUMENT, HISTORY, CALENDAR,
    PRINTER, STORE, SLIDERS, HOME, CHEVRON
}

class IconView(
    context: Context,
    private val icon: IconType,
    tint: Int
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.8f
        color = tint
    }
    private val rect = RectF()

    fun setTint(tint: Int) {
        paint.color = tint
        invalidate()
    }

    private fun path(block: Path.() -> Unit): Path = Path().apply(block)

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) =
        c.drawLine(x1, y1, x2, y2, paint)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scale = minOf(width, height) / 24f
        canvas.save()
        canvas.translate((width - 24f * scale) / 2f, (height - 24f * scale) / 2f)
        canvas.scale(scale, scale)

        when (icon) {
            IconType.RECEIPT -> {
                canvas.drawPath(path {
                    moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f)
                    lineTo(16f, 19.5f); lineTo(14f, 21f); lineTo(12f, 19.5f)
                    lineTo(10f, 21f); lineTo(8f, 19.5f); lineTo(6f, 21f); close()
                }, paint)
                line(canvas, 9f, 8f, 15f, 8f)
                line(canvas, 9f, 12f, 15f, 12f)
                line(canvas, 9f, 16f, 12.5f, 16f)
            }

            IconType.TRANSFER -> {
                line(canvas, 4f, 8f, 19f, 8f)
                canvas.drawPath(path {
                    moveTo(15f, 4f); lineTo(19f, 8f); lineTo(15f, 12f)
                }, paint)
                line(canvas, 20f, 16f, 5f, 16f)
                canvas.drawPath(path {
                    moveTo(9f, 12f); lineTo(5f, 16f); lineTo(9f, 20f)
                }, paint)
            }

            IconType.BOLT -> {
                canvas.drawPath(path {
                    moveTo(13.5f, 2.5f); lineTo(5f, 13.5f); lineTo(11f, 13.5f)
                    lineTo(10f, 21.5f); lineTo(19f, 10f); lineTo(13f, 10f); close()
                }, paint)
            }

            IconType.DOCUMENT -> {
                canvas.drawPath(path {
                    moveTo(6f, 3f); lineTo(14f, 3f); lineTo(19f, 8f)
                    lineTo(19f, 21f); lineTo(6f, 21f); close()
                }, paint)
                canvas.drawPath(path {
                    moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f)
                }, paint)
                line(canvas, 9f, 13f, 16f, 13f)
                line(canvas, 9f, 17f, 16f, 17f)
            }

            IconType.HISTORY -> {
                canvas.drawCircle(12f, 12f, 8.5f, paint)
                canvas.drawPath(path {
                    moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
                }, paint)
            }

            IconType.CALENDAR -> {
                rect.set(4f, 5f, 20f, 20f)
                canvas.drawRoundRect(rect, 3f, 3f, paint)
                line(canvas, 4f, 10f, 20f, 10f)
                line(canvas, 8f, 3f, 8f, 7f)
                line(canvas, 16f, 3f, 16f, 7f)
                line(canvas, 8.5f, 14f, 8.6f, 14f)
                line(canvas, 12f, 14f, 12.1f, 14f)
                line(canvas, 15.5f, 14f, 15.6f, 14f)
            }

            IconType.PRINTER -> {
                canvas.drawPath(path {
                    moveTo(7f, 9f); lineTo(7f, 3f); lineTo(17f, 3f); lineTo(17f, 9f)
                }, paint)
                rect.set(3.5f, 9f, 20.5f, 17.5f)
                canvas.drawRoundRect(rect, 2.5f, 2.5f, paint)
                canvas.drawPath(path {
                    moveTo(7f, 14f); lineTo(17f, 14f); lineTo(17f, 21f)
                    lineTo(7f, 21f); close()
                }, paint)
            }

            IconType.STORE -> {
                canvas.drawPath(path {
                    moveTo(3.5f, 9f); lineTo(5f, 4f); lineTo(19f, 4f); lineTo(20.5f, 9f)
                }, paint)
                line(canvas, 3.5f, 9f, 20.5f, 9f)
                canvas.drawPath(path {
                    moveTo(5f, 9f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 9f)
                }, paint)
                canvas.drawPath(path {
                    moveTo(10f, 20f); lineTo(10f, 14.5f); lineTo(14f, 14.5f); lineTo(14f, 20f)
                }, paint)
            }

            IconType.SLIDERS -> {
                line(canvas, 4f, 7f, 6.5f, 7f); line(canvas, 11.5f, 7f, 20f, 7f)
                canvas.drawCircle(9f, 7f, 2.5f, paint)
                line(canvas, 4f, 12f, 12.5f, 12f); line(canvas, 17.5f, 12f, 20f, 12f)
                canvas.drawCircle(15f, 12f, 2.5f, paint)
                line(canvas, 4f, 17f, 5.5f, 17f); line(canvas, 10.5f, 17f, 20f, 17f)
                canvas.drawCircle(8f, 17f, 2.5f, paint)
            }

            IconType.HOME -> {
                canvas.drawPath(path {
                    moveTo(3.5f, 11f); lineTo(12f, 4f); lineTo(20.5f, 11f)
                }, paint)
                canvas.drawPath(path {
                    moveTo(6f, 9.5f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 9.5f)
                }, paint)
                canvas.drawPath(path {
                    moveTo(10f, 20f); lineTo(10f, 14.5f); lineTo(14f, 14.5f); lineTo(14f, 20f)
                }, paint)
            }

            IconType.CHEVRON -> {
                canvas.drawPath(path {
                    moveTo(9f, 5.5f); lineTo(15.5f, 12f); lineTo(9f, 18.5f)
                }, paint)
            }
        }
        canvas.restore()
    }
}

/* ------------------------------------------------------------------ */
/*  ACTIVITY                                                           */
/* ------------------------------------------------------------------ */

class MainActivity : Activity() {

    // Palet warna
    private val teal = Color.rgb(23, 168, 151)
    private val tealDark = Color.rgb(11, 112, 102)
    private val tealSoft = Color.rgb(224, 246, 243)
    private val blue = Color.rgb(37, 99, 235)
    private val blueSoft = Color.rgb(226, 236, 255)
    private val amber = Color.rgb(217, 119, 6)
    private val amberSoft = Color.rgb(255, 243, 212)
    private val violet = Color.rgb(124, 58, 237)
    private val violetSoft = Color.rgb(238, 232, 255)
    private val dark = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(110, 120, 135)
    private val bg = Color.rgb(246, 248, 250)

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()
    private fun dpF(v: Float): Float = v * density

    private lateinit var headerView: View
    private lateinit var bodyView: LinearLayout
    private lateinit var bottomBar: View

    private var insetL = 0
    private var insetT = 0
    private var insetR = 0
    private var insetB = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installCrashCatcher()

        // Jika peluncuran sebelumnya crash, tampilkan penyebabnya di layar
        val prefs = getSharedPreferences("crash_log", MODE_PRIVATE)
        val lastCrash = prefs.getString("last", null)
        if (lastCrash != null) {
            prefs.edit().remove("last").apply()
            showCrashScreen(lastCrash)
            return
        }

        try {
            setupEdgeToEdge()
            showDashboard()
            applyNavBarAppearance()
        } catch (t: Throwable) {
            showCrashScreen(Log.getStackTraceString(t))
        }
    }

    /* ---------------- PENANGKAP CRASH (untuk debugging dari HP) ---------------- */

    private fun installCrashCatcher() {
        val prefs = getSharedPreferences("crash_log", MODE_PRIVATE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                prefs.edit().putString("last", Log.getStackTraceString(error)).commit()
            } catch (ignored: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private fun showCrashScreen(trace: String) {
        val info = TextView(this).apply {
            text = trace
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setTextColor(dark)
            setTextIsSelectable(true)
        }

        val title = text("SakaReceipt berhenti karena error", 16f, dark, bold = true)
        val hint = text(
            "Tekan \"Salin error\" lalu kirim teksnya ke Claude.",
            12f, muted
        )

        val copyBtn = Button(this).apply {
            text = "Salin error"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("SakaReceipt error", trace))
                Toast.makeText(this@MainActivity, "Error disalin", Toast.LENGTH_SHORT).show()
            }
        }

        val retryBtn = Button(this).apply {
            text = "Coba buka aplikasi"
            setOnClickListener {
                try {
                    setupEdgeToEdge()
                    showDashboard()
                    applyNavBarAppearance()
                } catch (t: Throwable) {
                    showCrashScreen(Log.getStackTraceString(t))
                }
            }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(48), dp(16), dp(24))
            addView(title)
            addView(gapV(6))
            addView(hint)
            addView(gapV(12))
            addView(copyBtn)
            addView(retryBtn)
            addView(gapV(12))
            addView(info)
        }

        setContentView(ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(column)
        })
    }

    /* ---------------- SYSTEM BARS ---------------- */

    @Suppress("DEPRECATION")
    private fun setupEdgeToEdge() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            var flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            if (Build.VERSION.SDK_INT >= 26) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    // Dipanggil SETELAH setContentView. Memanggil insetsController sebelum itu
    // bisa crash (NullPointerException) di Android 11/12.
    private fun applyNavBarAppearance() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                window.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        } catch (ignored: Throwable) {
        }
    }

    @Suppress("DEPRECATION")
    private fun readInsets(insets: WindowInsets) {
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            insetL = i.left; insetT = i.top; insetR = i.right; insetB = i.bottom
        } else {
            insetL = insets.systemWindowInsetLeft
            insetT = insets.systemWindowInsetTop
            insetR = insets.systemWindowInsetRight
            insetB = insets.systemWindowInsetBottom
        }
    }

    // Padding mengikuti status bar, gesture bar, poni, dan lebar layar (tablet/lipat)
    private fun applyInsets() {
        val screenW = resources.displayMetrics.widthPixels
        val contentW = minOf(screenW, dp(600))
        val side = (screenW - contentW) / 2

        headerView.setPadding(
            side + dp(20) + insetL,
            insetT + dp(24),
            side + dp(20) + insetR,
            dp(54)
        )
        bodyView.setPadding(
            side + dp(16) + insetL, 0,
            side + dp(16) + insetR, dp(24)
        )
        bottomBar.setPadding(
            dp(8) + insetL, dp(8),
            dp(8) + insetR, dp(8) + insetB
        )
    }

    /* ---------------- SCREEN ---------------- */

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
        }

        headerView = buildHeader()
        column.addView(headerView, matchWrap())

        bodyView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
        }
        column.addView(bodyView, matchWrap())

        val animated = mutableListOf<View>(headerView)

        fun add(v: View, params: ViewGroup.LayoutParams? = null, animate: Boolean = true) {
            if (params != null) bodyView.addView(v, params) else bodyView.addView(v)
            if (animate) animated.add(v)
        }

        // Kartu statistik menimpa sebagian header
        add(
            statsRow(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = -dp(34) }
        )

        add(gapV(26), animate = false)
        add(sectionLabel("BUAT STRUK BARU"))
        add(gapV(12), animate = false)

        add(tileRow(
            menuTile(
                IconType.TRANSFER, blue, blueSoft,
                "Bukti Transaksi", "Transfer dan tarik tunai"
            ) { openFeature("Bukti Transaksi") },
            menuTile(
                IconType.BOLT, amber, amberSoft,
                "Token PLN", "Pembelian token listrik"
            ) { openFeature("Token PLN") }
        ))
        add(gapV(12), animate = false)
        add(tileRow(
            menuTile(
                IconType.DOCUMENT, violet, violetSoft,
                "Bayar Tagihan", "PLN, PDAM, BPJS"
            ) { openFeature("Bayar Tagihan") },
            menuTile(
                IconType.HISTORY, teal, tealSoft,
                "Riwayat", "Struk sebelumnya"
            ) { openFeature("Riwayat") }
        ))

        add(gapV(26), animate = false)
        add(sectionLabel("PENGATURAN"))
        add(gapV(12), animate = false)
        add(settingRow(
            IconType.PRINTER, "Printer Bluetooth", "Hubungkan printer thermal"
        ) { openFeature("Printer Bluetooth") })
        add(gapV(10), animate = false)
        add(settingRow(
            IconType.STORE, "Pengaturan Usaha", "Identitas dan informasi usaha"
        ) { openFeature("Pengaturan Usaha") })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            addView(column)
        }

        bottomBar = buildBottomBar()

        root.addView(
            scroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        root.addView(bottomBar, matchWrap())

        root.setOnApplyWindowInsetsListener { _, insets ->
            readInsets(insets)
            applyInsets()
            insets
        }
        applyInsets()

        setContentView(root)
        root.requestApplyInsets()

        animateIn(animated)
    }

    private fun animateIn(views: List<View>) {
        views.forEachIndexed { index, v ->
            v.alpha = 0f
            v.translationY = dpF(14f)
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(50L * index)
                .setDuration(320L)
                .start()
        }
    }

private val imagePicker =
    registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(
                this,
                "Gambar berhasil dipilih",
                Toast.LENGTH_SHORT
            ).show()

            // Tahap berikutnya: tampilkan gambar dan proses OCR.
        }
    }

private fun openFeature(name: String) {
    when (name) {
        "Bukti Transaksi" -> {
            imagePicker.launch("image/*")
        }

        else -> {
            Toast.makeText(
                this,
                "$name segera hadir",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

    /* ---------------- HEADER ---------------- */

    // Header dengan dua lingkaran dekoratif tipis
    private inner class HeaderLayout(context: Context) : LinearLayout(context) {
        private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

        init { setWillNotDraw(false) }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
            dot.alpha = 24
            canvas.drawCircle(width - dpF(36f), dpF(24f), dpF(96f), dot)
            dot.alpha = 16
            canvas.drawCircle(width - dpF(120f), dpF(92f), dpF(44f), dot)
            canvas.restore()
        }
    }

    private fun buildHeader(): View = HeaderLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val r = dpF(30f)
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(teal, tealDark)
        ).apply { cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r) }

        // Baris merek
        val brand = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = FrameLayout(this@MainActivity).apply {
            background = rounded(Color.argb(46, 255, 255, 255), 12f)
            addView(
                IconView(this@MainActivity, IconType.RECEIPT, Color.WHITE),
                FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
            )
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(36), dp(36)))
        brand.addView(gapH(10))
        brand.addView(text("SAKARECEIPT", 13f, Color.WHITE, bold = true).apply {
            letterSpacing = 0.14f
        })
        addView(brand)

        addView(gapV(18))
        addView(text("Kelola struk lebih mudah.", 24f, Color.WHITE, bold = true, maxLines = 2))
        addView(gapV(6))
        addView(
            text(
                "Smart Receipt Generator",
                13f, Color.argb(225, 255, 255, 255), maxLines = 1
            )
        )
    }

    // Baris horizontal yang semua anaknya otomatis setinggi anak tertinggi
    private inner class EqualRow(context: Context) : LinearLayout(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            var maxH = 0
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                if (c.visibility != View.GONE) maxH = maxOf(maxH, c.measuredHeight)
            }
            for (i in 0 until childCount) {
                val c = getChildAt(i)
                if (c.visibility != View.GONE && c.measuredHeight != maxH) {
                    c.measure(
                        View.MeasureSpec.makeMeasureSpec(c.measuredWidth, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(maxH, View.MeasureSpec.EXACTLY)
                    )
                }
            }
            setMeasuredDimension(measuredWidth, maxH + paddingTop + paddingBottom)
        }
    }

    /* ---------------- STATISTIK ---------------- */

    private fun statsRow(): View = EqualRow(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(
            statCard(IconType.RECEIPT, teal, tealSoft, "0", "Total Struk"),
            weightParams()
        )
        addView(gapH(12))
        addView(
            statCard(IconType.CALENDAR, blue, blueSoft, "0", "Hari Ini"),
            weightParams()
        )
    }

    private fun statCard(
        icon: IconType, tint: Int, soft: Int, number: String, label: String
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        background = rounded(Color.WHITE, 20f)
        elevation = dpF(5f)
        contentDescription = "$label: $number"

        addView(iconChip(icon, tint, soft, 44, 22))

        val col = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            addView(text(number, 22f, dark, bold = true, maxLines = 1))
            addView(gapV(2))
            addView(text(label, 12f, muted, maxLines = 1))
        }
        addView(
            col,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )
    }

    /* ---------------- MENU UTAMA ---------------- */

    private fun tileRow(left: View, right: View): View = EqualRow(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, weightParams())
        addView(gapH(12))
        addView(right, weightParams())
    }

    private fun menuTile(
        icon: IconType, tint: Int, soft: Int,
        title: String, desc: String,
        onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(16))
        background = rippleBg(Color.WHITE, 20f)
        elevation = dpF(2f)
        isClickable = true
        contentDescription = "$title. $desc"
        setOnClickListener { onClick() }

        addView(iconChip(icon, tint, soft, 46, 24))
        addView(gapV(14))
        addView(text(title, 14f, dark, bold = true, maxLines = 2))
        addView(gapV(4))
        addView(text(desc, 12f, muted, maxLines = 2))
    }

    /* ---------------- PENGATURAN ---------------- */

    private fun settingRow(
        icon: IconType, title: String, desc: String, onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(14), dp(12))
        background = rippleBg(Color.WHITE, 18f)
        elevation = dpF(2f)
        isClickable = true
        contentDescription = "$title. $desc"
        setOnClickListener { onClick() }

        addView(iconChip(icon, teal, tealSoft, 46, 24))

        val labels = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
            addView(text(title, 14f, dark, bold = true, maxLines = 1))
            addView(gapV(3))
            addView(text(desc, 12f, muted, maxLines = 2))
        }
        addView(
            labels,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        addView(
            IconView(this@MainActivity, IconType.CHEVRON, muted),
            LinearLayout.LayoutParams(dp(20), dp(20))
        )
    }

    /* ---------------- BOTTOM NAV ---------------- */

    private fun buildBottomBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.WHITE)
        elevation = dpF(12f)

        addView(
            navItem(IconType.HOME, "Beranda", selected = true) {
                Toast.makeText(this@MainActivity, "Anda sudah di Beranda", Toast.LENGTH_SHORT).show()
            },
            weightParams()
        )
        addView(
            navItem(IconType.HISTORY, "Riwayat") { openFeature("Riwayat") },
            weightParams()
        )
        addView(
            navItem(IconType.SLIDERS, "Pengaturan") { openFeature("Pengaturan") },
            weightParams()
        )
    }

    private fun navItem(
        icon: IconType, label: String, selected: Boolean = false, onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(0, dp(4), 0, dp(2))
        isClickable = true
        contentDescription = label
        setOnClickListener { onClick() }

        val color = if (selected) teal else muted

        val pill = FrameLayout(this@MainActivity).apply {
            if (selected) background = rounded(tealSoft, 16f)
            addView(
                IconView(this@MainActivity, icon, color),
                FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
            )
        }
        addView(pill, LinearLayout.LayoutParams(dp(60), dp(32)))
        addView(gapV(2))
        addView(text(label, 11f, color, bold = selected, maxLines = 1).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
    }

    /* ---------------- HELPER ---------------- */

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpF(radiusDp)
        }

    private fun rippleBg(color: Int, radiusDp: Float): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.argb(40, 23, 168, 151)),
            rounded(color, radiusDp),
            rounded(Color.WHITE, radiusDp)
        )

    private fun iconChip(
        icon: IconType, tint: Int, soft: Int, sizeDp: Int, iconDp: Int
    ): View = FrameLayout(this).apply {
        background = rounded(soft, 14f)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        addView(
            IconView(this@MainActivity, icon, tint),
            FrameLayout.LayoutParams(dp(iconDp), dp(iconDp), Gravity.CENTER)
        )
    }

    private fun sectionLabel(value: String): TextView =
        text(value, 12f, dark, bold = true).apply { letterSpacing = 0.08f }

    private fun text(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean = false,
        maxLines: Int = 0
    ): TextView = TextView(this).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
        if (maxLines > 0) {
            this.maxLines = maxLines
            ellipsize = TextUtils.TruncateAt.END
        }
    }

    private fun gapV(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp)
        )
    }

    private fun gapH(widthDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), 1)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    // Lebar bagi rata. Tinggi disamakan oleh EqualRow, bukan oleh MATCH_PARENT.
    private fun weightParams() = LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
    )
}

   dependencies {
    implementation 'androidx.activity:activity-ktx:1.9.3'
   }
