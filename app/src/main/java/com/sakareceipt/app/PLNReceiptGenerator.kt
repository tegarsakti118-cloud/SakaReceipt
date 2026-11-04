```kotlin
package com.sakareceipt.app

import android.content.Context
import android.graphics.*

object PLNReceiptGenerator {

    private const val BITMAP_WIDTH = 384 // 58mm thermal printer standard width

    fun generateBitmap(context: Context, data: PLNReceiptData): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.MONOSPACE
        }

        var currentY = 30f
        val bitmap = Bitmap.createBitmap(BITMAP_WIDTH, 1200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        // Header SAKAPAY
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        drawCenteredText(canvas, "SAKAPAY", currentY, paint)
        currentY += 30f

        paint.textSize = 18f
        paint.typeface = Typeface.MONOSPACE
        drawDashedLine(canvas, currentY, paint)
        currentY += 25f

        drawCenteredText(canvas, "TOKEN LISTRIK PRABAYAR", currentY, paint)
        currentY += 25f

        val dateTimeStr = "${data.date}  ${data.time}".trim()
        if (dateTimeStr.isNotEmpty()) {
            drawCenteredText(canvas, dateTimeStr, currentY, paint)
            currentY += 25f
        }

        drawDashedLine(canvas, currentY, paint)
        currentY += 35f

        // Token Digits Display
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val tokenFormatted = data.getFormattedToken().split("\n")
        for (tokenLine in tokenFormatted) {
            drawCenteredText(canvas, tokenLine, currentY, paint)
            currentY += 35f
        }

        currentY += 10f
        paint.textSize = 16f
        paint.typeface = Typeface.MONOSPACE
        drawCenteredText(canvas, "MASUKKAN 20 ANGKA DI METERAN", currentY, paint)
        currentY += 25f

        drawDashedLine(canvas, currentY, paint)
        currentY += 25f

        // Customer Details
        paint.textSize = 18f
        currentY = drawLeftRightRow(canvas, "NO METER :", if (data.meterNumber.isNotBlank()) data.meterNumber else "-", currentY, paint)
        currentY = drawLeftRightRow(canvas, "NAMA     :", if (data.customerName.isNotBlank()) data.customerName else "-", currentY, paint)
        if (data.kwh.isNotBlank()) {
            currentY = drawLeftRightRow(canvas, "KWH      :", data.kwh, currentY, paint)
        }

        drawDashedLine(canvas, currentY, paint)
        currentY += 25f

        // Amounts & Ref
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        currentY = drawLeftRightRow(canvas, "TOTAL", "RP ${if (data.totalAmount.isNotBlank()) data.totalAmount else "0"}", currentY, paint)
        currentY += 5f

        paint.typeface = Typeface.MONOSPACE
        if (data.referenceNumber.isNotBlank()) {
            currentY = drawLeftRightRow(canvas, "REF:", data.referenceNumber, currentY, paint)
            currentY += 20f
        }

        // Footer Messages
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        drawCenteredText(canvas, "** TRANSAKSI BERHASIL **", currentY, paint)
        currentY += 25f

        paint.textSize = 16f
        paint.typeface = Typeface.MONOSPACE
        drawCenteredText(canvas, "TERIMA KASIH ATAS KEPERCAYAAN", currentY, paint)
        currentY += 22f
        drawCenteredText(canvas, "ANDA CS / BANTUAN:", currentY, paint)
        currentY += 22f
        drawCenteredText(canvas, "0857-8024-3664 SIMPAN STRUK INI", currentY, paint)
        currentY += 22f
        drawCenteredText(canvas, "SEBAGAI BUKTI SAH", currentY, paint)
        currentY += 22f
        drawCenteredText(canvas, "TID: AD4ECF", currentY, paint)
        currentY += 40f

        return Bitmap.createBitmap(bitmap, 0, 0, BITMAP_WIDTH, currentY.toInt())
    }

    private fun drawCenteredText(canvas: Canvas, text: String, y: Float, paint: Paint) {
        val x = (BITMAP_WIDTH - paint.measureText(text)) / 2f
        canvas.drawText(text, x, y, paint)
    }

    private fun drawDashedLine(canvas: Canvas, y: Float, paint: Paint) {
        drawCenteredText(canvas, "--------------------------------", y, paint)
    }

    private fun drawLeftRightRow(canvas: Canvas, left: String, right: String, y: Float, paint: Paint): Float {
        val margin = 16f
        canvas.drawText(left, margin, y, paint)
        val rightX = BITMAP_WIDTH - margin - paint.measureText(right)
        canvas.drawText(right, rightX, y, paint)
        return y + 25f
    }
}
```