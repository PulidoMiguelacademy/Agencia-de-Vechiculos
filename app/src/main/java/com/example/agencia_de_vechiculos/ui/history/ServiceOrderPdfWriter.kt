
package com.example.agencia_de_vechiculos.ui.history

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.agencia_de_vechiculos.model.ServiceOrder
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object ServiceOrderPdfWriter {

    private fun money(value: Long): String =
        "$" + NumberFormat.getIntegerInstance(
            Locale.forLanguageTag("es-CO")
        ).format(value)

    fun generate(
        context: Context,
        order: ServiceOrder
    ): File {

        val document = PdfDocument()

        try {
            val page = document.startPage(
                PdfDocument.PageInfo.Builder(
                    595, 842, 1
                ).create()
            )

            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 13f
            }

            fun write(
                text: String,
                y: Float,
                bold: Boolean = false
            ) {
                paint.isFakeBoldText = bold
                canvas.drawText(text, 45f, y, paint)
                paint.isFakeBoldText = false
            }

            paint.textSize = 20f
            write("AGENCIA DE VEHICULOS", 60f, true)

            paint.textSize = 16f
            write("ORDEN DE SERVICIO #${order.id}", 105f, true)

            paint.textSize = 13f

            write("Placa: ${order.plate}", 155f)
            write("Cliente: ${order.clientName}", 180f)
            write("Servicio: ${order.serviceType.label}", 205f)

            write("Fecha de ingreso: ${order.openedDate}", 250f)
            write(
                "Fecha de finalizacion: " +
                        (order.completedDate ?: "Pendiente"),
                275f
            )

            write(
                "Kilometraje: ${order.mileageKm ?: "No registrado"} km",
                310f
            )

            write("Estado: ${order.status.label}", 345f)

            paint.textSize = 16f

            write(
                "COSTO TOTAL: " +
                        (order.totalCost?.let(::money) ?: "Pendiente"),
                405f,
                true
            )

            paint.textSize = 13f

            write(
                "Firmado por: ${order.signedBy ?: "Pendiente"}",
                455f
            )

            document.finishPage(page)

            val directory = File(
                context.cacheDir,
                "service_orders"
            ).apply { mkdirs() }

            val file = File(
                directory,
                "ORDEN-${order.id}.pdf"
            )

            FileOutputStream(file).use {
                document.writeTo(it)
            }

            return file

        } finally {
            document.close()
        }
    }
}
