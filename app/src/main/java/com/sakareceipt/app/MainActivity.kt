
package com.sakareceipt.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val teal = Color.rgb(23, 168, 151)
    private val dark = Color.rgb(30, 41, 59)
    private val light = Color.rgb(245, 248, 250)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDashboard()
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(light)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(teal)
        }

        header.addView(TextView(this).apply {
            text = "SakaReceipt"
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        })

        header.addView(TextView(this).apply {
            text = "Smart Receipt Generator"
            textSize = 13f
            setTextColor(Color.WHITE)
        })

        root.addView(header)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 20)
        }

        content.addView(TextView(this).apply {
            text = "Dashboard"
            textSize = 23f
            setTextColor(dark)
            setTypeface(null, Typeface.BOLD)
        })

        content.addView(TextView(this).apply {
            text = "Kelola dan buat struk transaksi dengan mudah."
            textSize = 14f
            setTextColor(Color.GRAY)
        })

        content.addView(space(20))

        content.addView(TextView(this).apply {
            text = "BUAT STRUK BARU"
            textSize = 12f
            setTextColor(Color.GRAY)
            setTypeface(null, Typeface.BOLD)
        })

        addMenu(content, "Bukti Transaksi", "Transfer, tarik tunai, setor tunai")
        addMenu(content, "Token PLN", "Buat struk pembelian token listrik")
        addMenu(content, "Bayar Tagihan", "PLN, PDAM, BPJS dan lainnya")

        content.addView(space(22))

        content.addView(TextView(this).apply {
            text = "LAINNYA"
            textSize = 12f
            setTextColor(Color.GRAY)
            setTypeface(null, Typeface.BOLD)
        })

        addMenu(content, "Riwayat Struk", "Lihat struk yang pernah dibuat")
        addMenu(content, "Printer Bluetooth", "Pengaturan printer thermal")
        addMenu(content, "Pengaturan", "Identitas usaha dan informasi aplikasi")

        val scroll = ScrollView(this).apply {
            addView(content)
        }

        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        val footer = TextView(this).apply {
            text = "SAKARECEIPT  •  VERSION 1.0"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(12, 16, 12, 16)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(footer)
        setContentView(root)
    }

    private fun space(height: Int): TextView {
        return TextView(this).apply {
            this.height = height
        }
    }

    private fun addMenu(
        parent: LinearLayout,
        title: String,
        description: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 17, 18, 17)
            setBackgroundColor(Color.WHITE)
            elevation = 2f
        }

        card.addView(TextView(this).apply {
            text = title
            textSize = 16f
            setTextColor(dark)
            setTypeface(null, Typeface.BOLD)
        })

        card.addView(TextView(this).apply {
            text = description
            textSize = 12f
            setTextColor(Color.GRAY)
        })

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 10, 0, 4)
        parent.addView(card, params)
    }
}
