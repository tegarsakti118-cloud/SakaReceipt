```kotlin
package com.sakareceipt.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*

class PLNReceiptEditorActivity : Activity() {

    private lateinit var etCustomerName: EditText
    private lateinit var etMeterNumber: EditText
    private lateinit var etTokenNumber: EditText
    private lateinit var etKwh: EditText
    private lateinit var etTotalAmount: EditText
    private lateinit var etRefNumber: EditText
    private lateinit var etDate: EditText
    private lateinit var etTime: EditText

    private var initialData: PLNReceiptData = PLNReceiptData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialData = intent.getParcelableExtra("PLN_DATA") ?: PLNReceiptData()

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(246, 248, 250))
            isFillViewport = true
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
        }

        val title = TextView(this).apply {
            text = "Edit Data Token PLN"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 41, 59))
        }

        layout.addView(title)
        layout.addView(gapV(24))

        etCustomerName = createField(layout, "Nama Pelanggan", initialData.customerName)
        etMeterNumber = createField(layout, "No Meter / ID Pelanggan", initialData.meterNumber)
        etTokenNumber = createField(layout, "Nomor Token (20 Digit)", initialData.tokenNumber)
        etKwh = createField(layout, "Jumlah KWh", initialData.kwh)
        etTotalAmount = createField(layout, "Total Pembayaran (Rp)", initialData.totalAmount)
        etRefNumber = createField(layout, "Nomor Referensi", initialData.referenceNumber)
        etDate = createField(layout, "Tanggal (YYYY-MM-DD)", initialData.date)
        etTime = createField(layout, "Waktu (HH:MM:SS)", initialData.time)

        val btnCopyToken = Button(this).apply {
            text = "Salin Nomor Token"
            setBackgroundColor(Color.rgb(217, 119, 6))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val token = etTokenNumber.text.toString().replace("\\s+".toRegex(), "")
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Token PLN", token))
                Toast.makeText(this@PLNReceiptEditorActivity, "Token disalin", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(btnCopyToken)
        layout.addView(gapV(12))

        val btnGenerate = Button(this).apply {
            text = "Buat Struk Token PLN"
            setBackgroundColor(Color.rgb(23, 168, 151))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val tokenStr = etTokenNumber.text.toString().replace("\\s+".toRegex(), "")
                if (tokenStr.length != 20) {
                    Toast.makeText(this@PLNReceiptEditorActivity, "Nomor Token harus 20 digit!", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val updatedData = PLNReceiptData(
                    customerName = etCustomerName.text.toString().trim(),
                    meterNumber = etMeterNumber.text.toString().trim(),
                    idPelanggan = etMeterNumber.text.toString().trim(),
                    tokenNumber = tokenStr,
                    kwh = etKwh.text.toString().trim(),
                    totalAmount = etTotalAmount.text.toString().trim(),
                    referenceNumber = etRefNumber.text.toString().trim(),
                    date = etDate.text.toString().trim(),
                    time = etTime.text.toString().trim()
                )

                val intent = Intent(this@PLNReceiptEditorActivity, PLNReceiptPreviewActivity::class.java).apply {
                    putExtra("PLN_DATA", updatedData)
                }
                startActivity(intent)
            }
        }
        layout.addView(btnGenerate)

        root.addView(layout)
        setContentView(root)
    }

    private fun createField(parent: LinearLayout, labelText: String, defaultValue: String): EditText {
        val label = TextView(this).apply {
            text = labelText
            textSize = 12f
            setTextColor(Color.rgb(110, 120, 135))
            typeface = Typeface.DEFAULT_BOLD
        }
        val editText = EditText(this).apply {
            setText(defaultValue)
            textSize = 14f
            setPadding(24, 20, 24, 20)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                setStroke(2, Color.rgb(220, 225, 230))
                cornerRadius = 12f
            }
        }
        parent.addView(label)
        parent.addView(gapV(6))
        parent.addView(editText)
        parent.addView(gapV(16))
        return editText
    }

    private fun gapV(dp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp)
    }
}
```