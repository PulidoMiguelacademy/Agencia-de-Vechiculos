package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.ContractStatus
import com.example.agencia_de_vechiculos.model.Delivery
import com.example.agencia_de_vechiculos.model.DeliveryCloseResult
import com.example.agencia_de_vechiculos.model.DeliveryProcedure
import com.example.agencia_de_vechiculos.model.DeliveryScheduleResult
import com.example.agencia_de_vechiculos.model.DeliveryStatus
import com.example.agencia_de_vechiculos.model.ProcedureAdvanceResult
import com.example.agencia_de_vechiculos.model.ProcedureStatus

/**
 * 
 * guarda las entregas de los vehículos vendidos y maneja sus trámites, la fecha de
 * entrega y el acta (HU-25)
 * 
 * usa el ContractRepository para saber si el contrato de la venta ya está firmado:
 * solo entonces existe la entrega. Por ahora todo vive en memoria
 * 
 */
class DeliveryRepository(
    private val contractRepository: ContractRepository = ContractRepository(),
) {
    /**
     * 
     * las entregas abiertas, buscadas por el folio de la cotización
     * 
     */
    private val deliveries = mutableMapOf<String, Delivery>()

    /**
     * 
     * devuelve la entrega de una venta, o null si el contrato no existe o todavía no
     * está firmado
     * 
     * la primera vez que se pide la entrega de un contrato firmado, la crea con todos
     * los trámites pendientes
     * 
     */
    fun deliveryFor(quotationFolio: String): Delivery? {
        val contract = contractRepository.contractFor(quotationFolio.trim()) ?: return null
        if (contract.status != ContractStatus.FIRMADO) return null
        return deliveries.getOrPut(contract.quotationFolio) { Delivery(quotationFolio = contract.quotationFolio) }
    }

    /**
     * 
     * pasa un trámite a su siguiente estado: de pendiente a en proceso, y de en
     * proceso a completado
     * 
     * para completar la factura, las placas o el seguro hay que mandar su número en
     * reference, y ese número queda guardado. Los trámites ya no cambian cuando la
     * entrega está programada o la venta cerrada
     * 
     */
    fun advance(
        quotationFolio: String,
        procedure: DeliveryProcedure,
        reference: String = "",
    ): ProcedureAdvanceResult {
        val delivery = deliveryFor(quotationFolio)
            ?: return ProcedureAdvanceResult.InvalidData(NOT_SIGNED_MESSAGE)

        if (delivery.status != DeliveryStatus.EN_TRAMITES) {
            return ProcedureAdvanceResult.InvalidData(
                "Los trámites ya no se pueden cambiar: ${delivery.status.label.lowercase()}"
            )
        }

        val next = when (delivery.procedures[procedure] ?: ProcedureStatus.PENDIENTE) {
            ProcedureStatus.PENDIENTE -> ProcedureStatus.EN_PROCESO
            ProcedureStatus.EN_PROCESO -> ProcedureStatus.COMPLETADO
            ProcedureStatus.COMPLETADO -> return ProcedureAdvanceResult.InvalidData(
                "El trámite ${procedure.label} ya está completado"
            )
        }

        val cleanReference = reference.trim()
        val referenceLabel = procedure.referenceLabel
        if (next == ProcedureStatus.COMPLETADO && referenceLabel != null && cleanReference.isEmpty()) {
            return ProcedureAdvanceResult.InvalidData(
                "Para completar ${procedure.label} ingresa el ${referenceLabel.lowercase()}"
            )
        }

        val updated = delivery.copy(
            procedures = delivery.procedures + (procedure to next),
            references = if (next == ProcedureStatus.COMPLETADO && referenceLabel != null) {
                delivery.references + (procedure to cleanReference)
            } else {
                delivery.references
            },
        )
        deliveries[delivery.quotationFolio] = updated
        return ProcedureAdvanceResult.Advanced(updated, procedure)
    }

    /**
     * 
     * programa el día de la entrega
     * 
     * si falta algún trámite no se deja programar y se devuelve cuáles faltan. Como la
     * revisión pre-entrega es uno de ellos, sin la revisión aprobada no hay entrega.
     * La fecha debe venir como AAAA-MM-DD y no puede ser anterior a hoy (today). Si ya
     * estaba programada, cambia la fecha
     * 
     */
    fun schedule(quotationFolio: String, deliveryDate: String, today: String): DeliveryScheduleResult {
        val delivery = deliveryFor(quotationFolio)
            ?: return DeliveryScheduleResult.InvalidData(NOT_SIGNED_MESSAGE)

        if (delivery.status == DeliveryStatus.CERRADA) {
            return DeliveryScheduleResult.InvalidData("La venta ya está cerrada")
        }

        if (delivery.pendingProcedures.isNotEmpty()) {
            return DeliveryScheduleResult.PendingProcedures(delivery.pendingProcedures)
        }

        val cleanDate = deliveryDate.trim()
        if (!DATE_REGEX.matches(cleanDate)) {
            return DeliveryScheduleResult.InvalidData("Ingresa la fecha de entrega en formato AAAA-MM-DD")
        }
        if (cleanDate < today) {
            return DeliveryScheduleResult.InvalidData("La fecha de entrega no puede ser anterior a hoy ($today)")
        }

        val updated = delivery.copy(deliveryDate = cleanDate, status = DeliveryStatus.PROGRAMADA)
        deliveries[delivery.quotationFolio] = updated
        return DeliveryScheduleResult.Scheduled(updated)
    }

    /**
     * 
     * registra que el cliente firmó el acta de entrega y cierra la venta
     * 
     * solo se puede el mismo día de la entrega (cuando today es igual a la fecha
     * programada), con la revisión pre-entrega aprobada y con el nombre de quien
     * firma
     * 
     */
    fun closeSale(quotationFolio: String, signedBy: String, today: String): DeliveryCloseResult {
        val delivery = deliveryFor(quotationFolio)
            ?: return DeliveryCloseResult.InvalidData(NOT_SIGNED_MESSAGE)

        if (delivery.status == DeliveryStatus.CERRADA) {
            return DeliveryCloseResult.InvalidData("La venta ya fue cerrada con el acta firmada por ${delivery.signedBy}")
        }
        if (delivery.status != DeliveryStatus.PROGRAMADA) {
            return DeliveryCloseResult.InvalidData("Primero debes programar la entrega")
        }
        if (!delivery.isPreDeliveryApproved) {
            return DeliveryCloseResult.InvalidData("Sin revisión pre-entrega aprobada no se entrega el vehículo")
        }
        if (delivery.deliveryDate != today) {
            return DeliveryCloseResult.InvalidData(
                "El acta solo se firma el día de la entrega: ${delivery.deliveryDate}"
            )
        }

        val cleanSigner = signedBy.trim()
        if (cleanSigner.isEmpty()) {
            return DeliveryCloseResult.InvalidData("Escribe el nombre de quien firma el acta de entrega")
        }

        val updated = delivery.copy(signedBy = cleanSigner, status = DeliveryStatus.CERRADA)
        deliveries[delivery.quotationFolio] = updated
        return DeliveryCloseResult.Closed(updated)
    }

    companion object {
        /**
         * 
         * el mensaje que se muestra cuando se intenta gestionar la entrega de una venta
         * que todavía no tiene el contrato firmado
         * 
         */
        private const val NOT_SIGNED_MESSAGE = "La entrega solo se gestiona cuando el contrato de venta está firmado"

        /**
         * 
         * el formato que debe tener la fecha de entrega: AAAA-MM-DD
         * 
         */
        private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}
