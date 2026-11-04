```kotlin
package com.sakareceipt.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.ByteArrayOutputStream

class PLNReceiptPreviewActivity : Activity() {

    private lateinit var data: PLNReceiptData
    private lateinit var generatedBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        data = intent.getParcelableExtra("PLN_DATA") ?: PLNReceiptData()
        generatedBitmap = PLNReceiptGenerator.generateBitmap(this, data)

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(240, 242, 245))
            isFillViewport = true
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 32)
        }

        val previewImage = ImageView(this).apply {
            setImageBitmap(generatedBitmap)
            adjustViewBounds = true
            elevation = 8f
        }

        val btnCopyToken = Button(this).apply {
            text = "Salin Nomor Token"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Token PLN", data.tokenNumber))
                Toast.makeText(this@PLNReceiptPreviewActivity, "Nomor Token disalin", Toast.LENGTH_SHORT).show()
            }
        }

        val btnShare = Button(this).apply {
            text = "Bagikan Struk"
            setOnClickListener { shareReceiptImage() }
        }

        val btnPrint = Button(this).apply {
            text = "Cetak Printer Thermal (58mm)"
            setBackgroundColor(Color.rgb(23, 168, 151))
            setTextColor(Color.WHITE)
            setOnClickListener {
                Toast.makeText(this@PLNReceiptPreviewActivity, "Mencari Printer Bluetooth 58mm...", Toast.LENGTH_LONG).show()
            }
        }

        column.addView(previewImage)
        column.addView(gapV(24))
        column.addView(btnCopyToken)
        column.addView(gapV(10))
        column.addView(btnShare)
        column.addView(gapV(10))
        column.addView(btnPrint)

        root.addView(column)
        setContentView(root)
    }

    private fun shareReceiptImage() {
        val bytes = ByteArrayOutputStream()
        generatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, generatedBitmap, "Struk_PLN_${data.referenceNumber}", null)
        val imageUri = Uri.parse(path)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, imageUri)
        }
        startActivity(Intent.createChooser(shareIntent, "Bagikan Struk PLN"))
    }

    private fun gapV(dp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp)
    }
}
```