
package com.example.agencia_de_vechiculos.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object QuotationValidity {

    private fun dateFormat() =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun calculateExpirationDate(createdAt: String): String {
        val format = dateFormat()

        val parsedDate = format.parse(createdAt)
            ?: throw IllegalArgumentException("Fecha inválida")

        require(format.format(parsedDate) == createdAt) {
            "La fecha debe tener formato AAAA-MM-DD"
        }

        val calendar = Calendar.getInstance(
            TimeZone.getTimeZone("UTC")
        )

        calendar.time = parsedDate
        calendar.add(Calendar.DAY_OF_MONTH, 15)

        return format.format(calendar.time)
    }
}
