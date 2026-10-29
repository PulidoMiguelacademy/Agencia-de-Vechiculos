
package com.example.agencia_de_vechiculos.data

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeliveryChannel(val label: String) {
    CORREO("Correo electrónico"),
    WHATSAPP("WhatsApp")
}

enum class DeliveryStatus(val label: String) {
    COMPARTIR_INICIADO("Compartir iniciado"),
    ERROR("Error al compartir")
}

data class QuotationDeliveryAttempt(
    val id: Int,
    val folio: String,
    val recipient: String,
    val channel: DeliveryChannel,
    val status: DeliveryStatus,
    val attemptedAt: String,
    val errorMessage: String? = null
)

class QuotationDeliveryRepository {

    private val attempts =
        mutableStateListOf<QuotationDeliveryAttempt>()

    private var nextId = 1

    fun attemptsByFolio(folio: String): List<QuotationDeliveryAttempt> =
        attempts.filter { it.folio == folio }
            .sortedByDescending { it.id }

    fun registerAttempt(
        folio: String,
        recipient: String,
        channel: DeliveryChannel,
        status: DeliveryStatus,
        errorMessage: String? = null
    ) {
        val date = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        attempts.add(
            QuotationDeliveryAttempt(
                id = nextId++,
                folio = folio,
                recipient = recipient.trim(),
                channel = channel,
                status = status,
                attemptedAt = date,
                errorMessage = errorMessage
            )
        )
    }
}
