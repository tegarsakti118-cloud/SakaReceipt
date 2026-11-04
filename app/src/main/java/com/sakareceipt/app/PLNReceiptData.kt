```kotlin
package com.sakareceipt.app

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PLNReceiptData(
    val customerName: String = "",
    val idPelanggan: String = "",
    val meterNumber: String = "",
    val tokenNumber: String = "",
    val tokenAmount: String = "",
    val adminFee: String = "",
    val totalAmount: String = "",
    val date: String = "",
    val time: String = "",
    val referenceNumber: String = "",
    val tariff: String = "",
    val power: String = "",
    val kwh: String = "",
    val isSuccess: Boolean = true
) : Parcelable {

    fun getFormattedToken(): String {
        val cleanToken = tokenNumber.replace("\\s+".toRegex(), "")
        if (cleanToken.length != 20) return tokenNumber
        val line1 = "${cleanToken.substring(0, 4)} ${cleanToken.substring(4, 8)} ${cleanToken.substring(8, 12)}"
        val line2 = "${cleanToken.substring(12, 16)} ${cleanToken.substring(16, 20)}"
        return "$line1\n$line2"
    }

    fun isValidToken(): Boolean {
        val cleanToken = tokenNumber.replace("\\s+".toRegex(), "")
        return cleanToken.length == 20 && cleanToken.all { it.isDigit() }
    }
}
```