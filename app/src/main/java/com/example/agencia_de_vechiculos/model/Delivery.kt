package com.example.agencia_de_vechiculos.model

enum class DeliveryProcedure(val label: String, val referenceLabel: String?) {
    FACTURA("Factura", "Número de factura"),
    PLACAS("Placas", "Placa asignada"),
    SEGURO("Seguro", "Número de póliza"),
    REVISION_PRE_ENTREGA("Revisión pre-entrega", null),
}

enum class ProcedureStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    COMPLETADO("Completado"),
}

enum class DeliveryStatus(val label: String) {
    EN_TRAMITES("En trámites"),
    PROGRAMADA("Entrega programada"),
    CERRADA("Venta cerrada"),
}

data class Delivery(
    val quotationFolio: String,
    val procedures: Map<DeliveryProcedure, ProcedureStatus> =
        DeliveryProcedure.entries.associateWith { ProcedureStatus.PENDIENTE },
    val references: Map<DeliveryProcedure, String> = emptyMap(),
    val deliveryDate: String? = null,
    val signedBy: String? = null,
    val status: DeliveryStatus = DeliveryStatus.EN_TRAMITES,
) {
    val pendingProcedures: List<DeliveryProcedure>
        get() = DeliveryProcedure.entries.filter { procedures[it] != ProcedureStatus.COMPLETADO }

    val isPreDeliveryApproved: Boolean
        get() = procedures[DeliveryProcedure.REVISION_PRE_ENTREGA] == ProcedureStatus.COMPLETADO

    fun statusLabel(procedure: DeliveryProcedure): String {
        val status = procedures[procedure] ?: ProcedureStatus.PENDIENTE
        return if (procedure == DeliveryProcedure.REVISION_PRE_ENTREGA && status == ProcedureStatus.COMPLETADO) {
            "Aprobada"
        } else {
            status.label
        }
    }
}
