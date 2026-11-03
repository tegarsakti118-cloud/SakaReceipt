
package com.sakareceipt.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private val teal = Color.rgb(23, 168, 151)
    private val dark = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(120, 130, 145)
    private val bg = Color.rgb(246, 248, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    private fun background(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun space(height: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                height
            )
        }
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 24)
        }

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 25, 22, 25)
            background = background(teal, 24f)
        }

        header.addView(text("SAKARECEIPT", 13f, Color.WHITE, true))
        header.addView(space(12))
        header.addView(text("Kelola struk lebih mudah.", 22f, Color.WHITE, true))
        header.addView(space(5))
        header.addView(text(
            "Smart Receipt Generator",
            13f,
            Color.WHITE
        ))

        content.addView(header)

        // DASHBOARD TITLE
        content.addView(space(24))
        content.addView(text("Dashboard", 22f, dark, true))
        content.addView(space(5))
        content.addView(text(
            "Selamat datang di SakaReceipt",
            13f,
            muted
        ))

        // STATISTICS
        content.addView(space(18))

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        stats.addView(statCard("0", "Total Struk"), LinearLayout.LayoutParams(0, 100, 1f))
        stats.addView(View(this), LinearLayout.LayoutParams(12, 1))
        stats.addView(statCard("0", "Hari Ini"), LinearLayout.LayoutParams(0, 100, 1f))

        content.addView(stats)

        // MAIN MENU
        content.addView(space(25))
        content.addView(text("BUAT STRUK BARU", 13f, dark, true))
        content.addView(space(12))

        val row1 = menuRow(
            "🧾", "Bukti Transaksi", "Transfer dan tarik tunai",
            "⚡", "Token PLN", "Pembelian token listrik"
        )
        content.addView(row1)

        content.addView(space(12))

        val row2 = menuRow(
            "📄", "Bayar Tagihan", "PLN, PDAM, BPJS",
            "📚", "Riwayat", "Struk sebelumnya"
        )
        content.addView(row2)

        // OTHER MENU
        content.addView(space(25))
        content.addView(text("PENGATURAN", 13f, dark, true))
        content.addView(space(12))

        content.addView(simpleMenu("🖨", "Printer Bluetooth", "Hubungkan printer thermal"))
        content.addView(space(10))
        content.addView(simpleMenu("⚙", "Pengaturan Usaha", "Identitas dan informasi usaha"))

        content.addView(space(15))

        scroll.addView(content)

        // BOTTOM NAVIGATION
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = 12f
            setPadding(5, 12, 5, 12)
        }

        bottom.addView(navItem("⌂", "Beranda", true), weightParams())
        bottom.addView(navItem("▤", "Riwayat"), weightParams())
        bottom.addView(navItem("⚙", "Pengaturan"), weightParams())

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        root.addView(bottom)

        setContentView(root)
    }

    private fun weightParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, 55, 1f)
    }

    private fun statCard(number: String, label: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = background(Color.WHITE, 18f)
            elevation = 2f
        }

        card.addView(text(number, 25f, teal, true))
        card.addView(space(4))
        card.addView(text(label, 12f, muted))

        return card
    }

    private fun menuRow(
        icon1: String, title1: String, desc1: String,
        icon2: String, title2: String, desc2: String
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(
            menuCard(icon1, title1, desc1),
            LinearLayout.LayoutParams(0, 135, 1f)
        )

        row.addView(View(this), LinearLayout.LayoutParams(12, 1))

        row.addView(
            menuCard(icon2, title2, desc2),
            LinearLayout.LayoutParams(0, 135, 1f)
        )

        return row
    }

    private fun menuCard(icon: String, title: String, desc: String): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 12, 12)
            background = background(Color.WHITE, 18f)
            elevation = 2f
        }

        card.addView(text(icon, 25f, teal))
        card.addView(space(8))
        card.addView(text(title, 14f, dark, true))
        card.addView(space(4))
        card.addView(text(desc, 11f, muted))

        return card
    }

    private fun simpleMenu(icon: String, title: String, desc: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 14, 16, 14)
            background = background(Color.WHITE, 16f)
            elevation = 2f
        }

        val iconView = text(icon, 24f, teal)
        row.addView(iconView)

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 0, 0, 0)
        }

        labels.addView(text(title, 14f, dark, true))
        labels.addView(space(4))
        labels.addView(text(desc, 11f, muted))

        row.addView(labels)

        return row
    }

    private fun navItem(
        icon: String,
        label: String,
        selected: Boolean = false
    ): View {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val color = if (selected) teal else muted

        item.addView(text(icon, 22f, color, true))
        item.addView(space(3))
        item.addView(text(label, 10f, color, selected))

        return item
    }
}
