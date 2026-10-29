
package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.DiscountApprovalStatus
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationDetail
import com.example.agencia_de_vechiculos.model.QuotationDraft
import com.example.agencia_de_vechiculos.model.QuotationStatus
import com.example.agencia_de_vechiculos.model.QuotationValidity
import com.example.agencia_de_vechiculos.model.VehicleStatus

sealed interface QuotationResult {
    data class Success(
        val quotation: Quotation
    ) : QuotationResult

    data class PendingApproval(
        val requestId: String,
        val message: String
    ) : QuotationResult

    data class Error(
        val message: String
    ) : QuotationResult
}

/**
 * HU-13: Generar cotizaciones.
 * Por ahora, los registros se almacenan en memoria.
 */
class QuotationRepository(
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
    initialQuotations: List<Quotation> = sampleQuotations,
) {

    // Cotizaciones compartidas con HU-24
    private val quotations = initialQuotations.toMutableList()

    // Detalles adicionales de HU-13
    private val quotationDetails = mutableListOf<QuotationDetail>()

    private val pendingApprovals =
        mutableMapOf<String, QuotationDraft>()

    // Evita procesar dos veces el mismo identificador de solicitud
    private val processedRequests = mutableMapOf<String, String>()

    // Calcula el siguiente folio a partir de los existentes
    private var nextId = (
            quotations.mapNotNull {
                it.folio.removePrefix("COT-").toIntOrNull()
            }.maxOrNull() ?: 0
            ) + 1

    fun allQuotations(): List<Quotation> =
        quotations.toList()

    fun quotationByFolio(folio: String): Quotation? =
        quotations.firstOrNull { it.folio == folio.trim() }

    fun detailByFolio(folio: String): QuotationDetail? =
        quotationDetails.firstOrNull { it.folio == folio.trim() }

    /**
     * Valida y genera una cotización.
     */
    fun generateQuotation(
        draft: QuotationDraft,
        createdAt: String,
        requestId: String? = null
    ): QuotationResult {

        // 1. Evitar solicitudes duplicadas
        if (requestId != null) {
            if (requestId.isBlank()) {
                return QuotationResult.Error(
                    "El identificador de solicitud no es válido"
                )
            }

            val existingFolio = processedRequests[requestId]

            if (existingFolio != null) {
                val existing = quotationByFolio(existingFolio)
                    ?: return QuotationResult.Error(
                        "No se encontró la cotización procesada"
                    )

                return QuotationResult.Success(existing)
            }
        }

        // 2. Validar datos del cliente
        if (!draft.hasValidClient) {
            return QuotationResult.Error(
                "El nombre y documento del cliente son obligatorios"
            )
        }

        // 3. Validar precios
        if (!draft.hasValidPrices) {
            return QuotationResult.Error(
                "Los precios de la cotización no son válidos"
            )
        }

        // 4. Validar descuento
        if (!draft.hasValidDiscount) {
            return QuotationResult.Error(
                "El descuento debe estar entre 0 y 100%"
            )
        }

        // 5. Comprobar que el vehículo exista
        val vehicle = vehicleRepository.vehicleById(draft.vehicleId)
            ?: return QuotationResult.Error(
                "El vehículo seleccionado no existe"
            )

        if (vehicle.status == VehicleStatus.VENDIDO) {
            return QuotationResult.Error(
                "No se puede cotizar un vehículo vendido"
            )
        }

        // 6. Verificar el precio original
        if (draft.vehiclePrice != vehicle.price) {
            return QuotationResult.Error(
                "El precio no coincide con el catálogo"
            )
        }

        // 7. Bloquear descuentos sin autorización
        if (draft.requiresManagerApproval) {

            val approvalId = requestId
                ?: return QuotationResult.Error(
                    "Se requiere un identificador de solicitud"
                )

            pendingApprovals[approvalId] = draft

            return QuotationResult.PendingApproval(
                requestId = approvalId,
                message = "Descuento pendiente de aprobación del gerente"
            )
        }

        // 8. Calcular los 15 días de vigencia
        val expiration = try {
            QuotationValidity.calculateExpirationDate(createdAt)
        } catch (e: IllegalArgumentException) {
            return QuotationResult.Error(
                "La fecha debe tener formato AAAA-MM-DD y ser válida"
            )
        }

        // 9. Generar folio
        val folio = "COT-${nextId.toString().padStart(4, '0')}"

        // 10. Crear la cotización principal
        val quotation = Quotation(
            folio = folio,
            clientName = draft.clientName.trim(),
            clientDocument = draft.clientDocument.trim(),
            vehicleId = draft.vehicleId,
            finalPrice = draft.total,
            paymentMethod = draft.paymentMethod,
            status = QuotationStatus.EMITIDA
        )

        // 11. Crear el desglose de HU-13
        val detail = QuotationDetail(
            folio = folio,
            vehiclePrice = draft.vehiclePrice,
            accessoriesPrice = draft.accessoriesPrice,
            discountPercent = draft.discountPercent,
            discountAmount = draft.discountAmount,
            createdAt = createdAt,
            expiresAt = expiration,
            approvalStatus = DiscountApprovalStatus.NO_REQUERIDA
        )

        // 12. Guardar ambos registros
        quotations.add(quotation)
        quotationDetails.add(detail)
        nextId++

        if (requestId != null) {
            processedRequests[requestId] = folio
        }

        return QuotationResult.Success(quotation)
    }

    /**
     * Permite aceptar o rechazar una cotización emitida.
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
                "Solo se puede cambiar una cotización emitida"
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
