
package com.example.agencia_de_vechiculos.ui.quotation

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationDetail
import com.example.agencia_de_vechiculos.model.Vehicle
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object QuotationPdfWriter {

    private fun money(value: Long): String {
        val formatter = NumberFormat.getIntegerInstance(
            Locale.forLanguageTag("es-CO")
        )
        return "$${formatter.format(value)}"
    }

    fun generate(
        context: Context,
        quotation: Quotation,
        detail: QuotationDetail,
        vehicle: Vehicle
    ): File {

        require(quotation.folio == detail.folio) {
            "Los datos de la cotización no coinciden"
        }

        require(quotation.vehicleId == vehicle.id) {
            "El vehículo no corresponde a la cotización"
        }

        val document = PdfDocument()

        try {
            val pageInfo = PdfDocument.PageInfo.Builder(
                595, 842, 1
            ).create()

            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 14f
            }

            fun line(text: String, y: Float, bold: Boolean = false) {
                paint.isFakeBoldText = bold
                canvas.drawText(text, 45f, y, paint)
                paint.isFakeBoldText = false
            }

            paint.textSize = 22f
            line("AGENCIA DE VEHICULOS", 65f, true)

            paint.textSize = 16f
            line("COTIZACION ${quotation.folio}", 105f, true)

            paint.textSize = 13f

            line("Cliente: ${quotation.clientName}", 155f)
            line("Documento: ${quotation.clientDocument}", 180f)

            line(
                "Vehiculo: ${vehicle.brand} ${vehicle.model} ${vehicle.year}",
                220f
            )

            line("Precio base: ${money(detail.vehiclePrice)}", 260f)
            line("Accesorios: ${money(detail.accessoriesPrice)}", 285f)

            line(
                "Descuento (${detail.discountPercent}%): " +
                        money(detail.discountAmount),
                310f
            )

            paint.textSize = 17f

            line(
                "TOTAL: ${money(quotation.finalPrice)}",
                360f,
                true
            )

            paint.textSize = 13f

            line(
                "Forma de pago: ${quotation.paymentMethod.label}",
                410f
            )

            line("Fecha de emision: ${detail.createdAt}", 450f)
            line("Valida hasta: ${detail.expiresAt}", 475f)
            line("Estado: ${quotation.status.label}", 510f)

            paint.color = Color.DKGRAY
            paint.textSize = 11f

            line(
                "Documento generado por Agencia de Vehiculos.",
                760f
            )

            document.finishPage(page)

            val directory = File(
                context.cacheDir,
                "quotes"
            ).apply { mkdirs() }

            val file = File(
                directory,
                "${quotation.folio}.pdf"
            )

            FileOutputStream(file).use { output ->
                document.writeTo(output)
            }

            return file

        } finally {
            document.close()
        }
    }
}
