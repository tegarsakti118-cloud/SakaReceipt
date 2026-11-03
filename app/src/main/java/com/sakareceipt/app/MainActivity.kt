package com.sakareceipt.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "SakaReceipt"
            textSize = 30f
            setTextColor(Color.rgb(23, 168, 151))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Smart Receipt Generator"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(title)
        layout.addView(subtitle)

        setContentView(layout)
    }
}
