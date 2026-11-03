package com.sakareceipt.app

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {

    private val teal = Color.rgb(23, 168, 151)
    private val tealDark = Color.rgb(13, 118, 108)
    private val tealSoft = Color.rgb(224, 246, 243)
    private val amberSoft = Color.rgb(255, 244, 214)
    private val blueSoft = Color.rgb(224, 236, 255)
    private val violetSoft = Color.rgb(237, 231, 255)
    private val dark = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(120, 130, 145)
    private val bg = Color.rgb(246, 248, 250)

    private val density by lazy { resources.displayMetrics.density }
    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()
    private fun dpF(v: Float): Float = v * density

    private lateinit var headerView: View
    private lateinit var bottomBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = teal
        window.navigationBarColor = Color.WHITE
        showDashboard()
    }

    // ---------- SCREEN ----------

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }

        headerView = buildHeader()
        content.addView(headerView, matchWrap())

        content.addView(gapV(24))
        content.addView(sectionTitle("Dashboard", "Selamat datang di SakaReceipt"))
        content.addView(gapV(16))
        content.addView(statsRow())

        content.addView(gapV(26))
        content.addView(sectionLabel("BUAT STRUK BARU"))
        content.addView(gapV(12))
        content.addView(tileRow(
            menuTile("🧾", "Bukti Transaksi", "Transfer dan tarik tunai", blueSoft) {
                openFeature("Bukti Transaksi")
            },
            menuTile("⚡", "Token PLN", "Pembelian token listrik", amberSoft) {
                openFeature("Token PLN")
            }
        ))
        content.addView(gapV(12))
        content.addView(tileRow(
            menuTile("📄", "Bayar Tagihan", "PLN, PDAM, BPJS", violetSoft) {
                openFeature("Bayar Tagihan")
            },
            menuTile("📚", "Riwayat", "Struk sebelumnya", tealSoft) {
                openFeature("Riwayat")
            }
        ))

        content.addView(gapV(26))
        content.addView(sectionLabel("PENGATURAN"))
        content.addView(gapV(12))
        content.addView(settingRow("🖨", "Printer Bluetooth", "Hubungkan printer thermal") {
            openFeature("Printer Bluetooth")
        })
        content.addView(gapV(10))
        content.addView(settingRow("⚙", "Pengaturan Usaha", "Identitas dan informasi usaha") {
            openFeature("Pengaturan Usaha")
        })

        // Scroll area, dibatasi lebarnya agar rapi di tablet
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val screenWidth = resources.displayMetrics.widthPixels
        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                minOf(screenWidth, dp(560)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL
            )
        )

        bottomBar = buildBottomBar()

        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(bottomBar, matchWrap())

        // Menyesuaikan dengan status bar dan gesture bar di semua tipe HP
        root.setOnApplyWindowInsetsListener { _, insets ->
            headerView.setPadding(
                dp(20), dp(20) + insets.systemWindowInsetTop, dp(20), dp(22)
            )
            bottomBar.setPadding(
                dp(8), dp(8), dp(8), dp(10) + insets.systemWindowInsetBottom
            )
            insets
        }

        setContentView(root)
    }

    private fun openFeature(name: String) {
        Toast.makeText(this, "$name segera hadir", Toast.LENGTH_SHORT).show()
    }

    // ---------- HEADER ----------

    private fun buildHeader(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(22))
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(teal, tealDark)
        ).apply { cornerRadius = dpF(24f) }
        elevation = dpF(4f)

        addView(text("SAKARECEIPT", 12f, Color.WHITE, bold = true).apply {
            letterSpacing = 0.12f
        })
        addView(gapV(10))
        addView(text("Kelola struk lebih mudah.", 22f, Color.WHITE, bold = true, maxLines = 2))
        addView(gapV(6))
        addView(text("Smart Receipt Generator", 13f, Color.argb(220, 255, 255, 255), maxLines = 1))
    }

    // ---------- STATS ----------

    private fun statsRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setMeasureWithLargestChildEnabled(true)
        addView(statCard("🧾", "0", "Total Struk"), weightParams())
        addView(gapH(12))
        addView(statCard("📅", "0", "Hari Ini"), weightParams())
    }

    private fun statCard(icon: String, number: String, label: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(Color.WHITE, 18f)
            elevation = dpF(2f)

            addView(iconChip(icon, tealSoft))
            addView(gapV(10))
            addView(text(number, 24f, dark, bold = true, maxLines = 1))
            addView(gapV(2))
            addView(text(label, 12f, muted, maxLines = 1))
        }

    // ---------- MENU TILES ----------

    private fun tileRow(left: View, right: View): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setMeasureWithLargestChildEnabled(true)
        addView(left, weightParams())
        addView(gapH(12))
        addView(right, weightParams())
    }

    private fun menuTile(
        icon: String,
        title: String,
        desc: String,
        iconBg: Int,
        onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(16))
        background = rippleBg(Color.WHITE, 18f)
        elevation = dpF(2f)
        isClickable = true
        setOnClickListener { onClick() }

        addView(iconChip(icon, iconBg))
        addView(gapV(12))
        addView(text(title, 14f, dark, bold = true, maxLines = 2))
        addView(gapV(4))
        addView(text(desc, 12f, muted, maxLines = 2))
    }

    // ---------- SETTINGS ROWS ----------

    private fun settingRow(
        icon: String,
        title: String,
        desc: String,
        onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(14), dp(12))
        background = rippleBg(Color.WHITE, 16f)
        elevation = dpF(2f)
        isClickable = true
        setOnClickListener { onClick() }

        addView(iconChip(icon, tealSoft))

        val labels = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(text(title, 14f, dark, bold = true, maxLines = 1))
            addView(gapV(3))
            addView(text(desc, 12f, muted, maxLines = 2))
        }
        addView(labels)

        addView(text("›", 22f, muted))
    }

    // ---------- BOTTOM NAV ----------

    private fun buildBottomBar(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setBackgroundColor(Color.WHITE)
        elevation = dpF(12f)
        setPadding(dp(8), dp(8), dp(8), dp(10))

        addView(navItem("⌂", "Beranda", selected = true) {
            Toast.makeText(this@MainActivity, "Anda di Beranda", Toast.LENGTH_SHORT).show()
        }, weightParams())
        addView(navItem("▤", "Riwayat") { openFeature("Riwayat") }, weightParams())
        addView(navItem("⚙", "Pengaturan") { openFeature("Pengaturan") }, weightParams())
    }

    private fun navItem(
        icon: String,
        label: String,
        selected: Boolean = false,
        onClick: () -> Unit
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(0, dp(4), 0, dp(4))
        isClickable = true
        setOnClickListener { onClick() }

        val color = if (selected) teal else muted

        addView(TextView(this@MainActivity).apply {
            text = icon
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(color)
            setPadding(dp(18), dp(4), dp(18), dp(4))
            background = if (selected) rounded(tealSoft, 16f) else null
        })
        addView(gapV(2))
        addView(text(label, 11f, color, bold = selected, maxLines = 1))
    }

    // ---------- SHARED HELPERS ----------

    private fun rounded(color: Int, radiusDp: Float): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpF(radiusDp)
        }

    private fun rippleBg(color: Int, radiusDp: Float): RippleDrawable =
        RippleDrawable(
            ColorStateList.valueOf(Color.argb(38, 23, 168, 151)),
            rounded(color, radiusDp),
            null
        )

    private fun iconChip(icon: String, soft: Int): TextView = TextView(this).apply {
        text = icon
        textSize = 20f
        gravity = Gravity.CENTER
        background = rounded(soft, 14f)
        layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
    }

    private fun sectionTitle(title: String, sub: String): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, 22f, dark, bold = true, maxLines = 1))
            addView(gapV(4))
            addView(text(sub, 13f, muted, maxLines = 2))
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
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp))
    }

    private fun gapH(widthDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), 1)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun weightParams() = LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1f
    )
}
