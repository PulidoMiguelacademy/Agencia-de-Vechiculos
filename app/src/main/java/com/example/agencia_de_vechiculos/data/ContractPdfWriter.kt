package com.example.agencia_de_vechiculos.data

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.agencia_de_vechiculos.model.SaleContract
import com.example.agencia_de_vechiculos.model.Vehicle
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class ContractPdfWriter(private val outputDir: File) {

    fun write(contract: SaleContract, vehicle: Vehicle, issuedDate: String): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        PageWriter(page.canvas).apply {
            title("CONTRATO DE COMPRAVENTA DE VEHÍCULO")
            line("Contrato N.º ${contract.id}  ·  Cotización ${contract.quotationFolio}  ·  Emitido el $issuedDate")
            space()

            section("Comprador")
            field("Nombre", contract.clientName)
            field("Documento", contract.clientDocument)
            space()

            section("Vehículo")
            field("Marca y modelo", "${vehicle.brand} ${vehicle.model}")
            field("Año", vehicle.year.toString())
            field("Condición", vehicle.condition.label)
            field("VIN", contract.vin)
            space()

            section("Condiciones de la venta")
            field("Precio final", formatPrice(contract.finalPrice))
            field("Anticipo de la reserva", formatPrice(contract.deposit))
            field("Saldo a pagar", formatPrice(contract.balance))
            field("Forma de pago", contract.paymentMethod.label)
            field("Fecha de firma", contract.signedDate ?: "____________________")
            space()
            space()

            signatures("Firma del comprador", "Firma de la agencia")
        }
        document.finishPage(page)

        outputDir.mkdirs()
        val file = File(outputDir, "contrato-${contract.quotationFolio}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private class PageWriter(private val canvas: Canvas) {
        private var y = MARGIN.toFloat()

        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        private val labelPaint = Paint(textPaint).apply { typeface = Typeface.DEFAULT_BOLD }

        fun title(text: String) {
            y += 18f
            canvas.drawText(text, MARGIN.toFloat(), y, titlePaint)
            y += 12f
            canvas.drawLine(MARGIN.toFloat(), y, (PAGE_WIDTH - MARGIN).toFloat(), y, textPaint)
            y += 8f
        }

        fun section(text: String) {
            y += 20f
            canvas.drawText(text, MARGIN.toFloat(), y, sectionPaint)
            y += 4f
        }

        fun line(text: String) {
            y += 16f
            canvas.drawText(text, MARGIN.toFloat(), y, textPaint)
        }

        fun field(label: String, value: String) {
            y += 16f
            canvas.drawText("$label:", MARGIN.toFloat(), y, labelPaint)
            canvas.drawText(value, (MARGIN + LABEL_WIDTH).toFloat(), y, textPaint)
        }

        fun space() {
            y += 12f
        }

        fun signatures(left: String, right: String) {
            y += 40f
            val half = (PAGE_WIDTH - 2 * MARGIN) / 2f
            val leftX = MARGIN.toFloat()
            val rightX = MARGIN + half + 20f
            canvas.drawLine(leftX, y, leftX + half - 20f, y, textPaint)
            canvas.drawLine(rightX, y, rightX + half - 20f, y, textPaint)
            y += 14f
            canvas.drawText(left, leftX, y, textPaint)
            canvas.drawText(right, rightX, y, textPaint)
        }
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 56
        private const val LABEL_WIDTH = 160

        private fun formatPrice(value: Long): String =
            "$ ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)}"
    }
}
