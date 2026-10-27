package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.PaymentMethod
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationStatus
import com.example.agencia_de_vechiculos.model.VehicleStatus

/**
 * Resultados posibles al generar una cotización.
 */
sealed interface QuotationResult {
    data class Success(val quotation: Quotation) : QuotationResult
    data class Error(val message: String) : QuotationResult
}

/**
 * Repositorio de cotizaciones (HU-13).
 * Por ahora almacena la información en memoria.
 */
class QuotationRepository(
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
    initialQuotations: List<Quotation> = sampleQuotations,
) {

    private val quotations = initialQuotations.toMutableList()

    private var nextId = (
            quotations.mapNotNull {
                it.folio.removePrefix("COT-").toIntOrNull()
            }.maxOrNull() ?: 0
            ) + 1

    /**
     * Devuelve todas las cotizaciones.
     */
    fun allQuotations(): List<Quotation> =
        quotations.toList()

    /**
     * Busca una cotización por su folio.
     */
    fun quotationByFolio(folio: String): Quotation? =
        quotations.firstOrNull {
            it.folio == folio.trim()
        }

    /**
     * Genera una cotización después de validar sus datos.
     */
    fun generateQuotation(
        clientName: String,
        clientDocument: String,
        vehicleId: Int,
        finalPrice: Long,
        paymentMethod: PaymentMethod,
    ): QuotationResult {

        val cleanName = clientName.trim()
        val cleanDocument = clientDocument.trim()

        if (cleanName.isEmpty()) {
            return QuotationResult.Error(
                "El nombre del cliente es obligatorio"
            )
        }

        if (cleanDocument.isEmpty()) {
            return QuotationResult.Error(
                "El documento del cliente es obligatorio"
            )
        }

        val vehicle = vehicleRepository.vehicleById(vehicleId)
            ?: return QuotationResult.Error(
                "El vehículo seleccionado no existe"
            )

        if (vehicle.status == VehicleStatus.VENDIDO) {
            return QuotationResult.Error(
                "No se puede cotizar un vehículo vendido"
            )
        }

        if (finalPrice <= 0) {
            return QuotationResult.Error(
                "El precio debe ser mayor que cero"
            )
        }

        val folio = "COT-${nextId.toString().padStart(4, '0')}"

        val quotation = Quotation(
            folio = folio,
            clientName = cleanName,
            clientDocument = cleanDocument,
            vehicleId = vehicleId,
            finalPrice = finalPrice,
            paymentMethod = paymentMethod,
            status = QuotationStatus.EMITIDA,
        )

        quotations.add(quotation)
        nextId++

        return QuotationResult.Success(quotation)
    }

    /**
     * Actualiza el estado de una cotización emitida.
     */
    fun updateStatus(
        folio: String,
        newStatus: QuotationStatus,
    ): QuotationResult {

        val index = quotations.indexOfFirst {
            it.folio == folio.trim()
        }

        if (index == -1) {
            return QuotationResult.Error(
                "La cotización no existe"
            )
        }

        val current = quotations[index]

        if (current.status != QuotationStatus.EMITIDA) {
            return QuotationResult.Error(
                "Solo se puede cambiar el estado de una cotización emitida"
            )
        }

        if (newStatus == QuotationStatus.EMITIDA) {
            return QuotationResult.Error(
                "Selecciona aceptar o rechazar la cotización"
            )
        }

        val updated = current.copy(status = newStatus)
        quotations[index] = updated

        return QuotationResult.Success(updated)
    }
}
