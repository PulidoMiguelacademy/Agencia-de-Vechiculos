package com.example.agencia_de_vechiculos.model

/**
 *
 * los trámites que hay que terminar antes de entregar el vehículo (HU-25)
 *
 * cada uno trae el texto que se muestra y el nombre del dato que se pide al
 * completarlo (el número de factura, la placa o la póliza). La revisión
 * pre-entrega no pide ningún dato, por eso su referenceLabel es null
 *
 */
enum class DeliveryProcedure(val label: String, val referenceLabel: String?) {
    FACTURA("Factura", "Número de factura"),
    PLACAS("Placas", "Placa asignada"),
    SEGURO("Seguro", "Número de póliza"),
    REVISION_PRE_ENTREGA("Revisión pre-entrega", null),
}

/**
 *
 * en qué va un trámite. Avanza siempre en este orden: pendiente, en proceso y
 * completado
 *
 */
enum class ProcedureStatus(val label: String) {
    PENDIENTE("Pendiente"),
    EN_PROCESO("En proceso"),
    COMPLETADO("Completado"),
}

/**
 *
 * en qué va la entrega: haciendo los trámites, con la fecha de entrega ya
 * programada o con la venta cerrada porque el cliente firmó el acta
 *
 */
enum class DeliveryStatus(val label: String) {
    EN_TRAMITES("En trámites"),
    PROGRAMADA("Entrega programada"),
    CERRADA("Venta cerrada"),
}

/**
 *
 * la entrega de un vehículo vendido. Se abre cuando el contrato de venta está
 * firmado
 *
 * guarda cómo va cada trámite y sus números, la fecha de entrega y quién firmó el
 * acta. Sabe por sí sola qué trámites faltan y si la revisión pre-entrega está
 * aprobada, así se puede decidir si se deja programar la entrega
 *
 */
data class Delivery(
    /**
     *
     * la cotización de la venta. Sirve para encontrar la entrega desde el contrato
     *
     */
    val quotationFolio: String,

    /**
     *
     * cómo va cada trámite. Todos empiezan pendientes
     *
     */
    val procedures: Map<DeliveryProcedure, ProcedureStatus> =
        DeliveryProcedure.entries.associateWith { ProcedureStatus.PENDIENTE },

    /**
     *
     * el número de cada trámite completado: el de la factura, la placa y el de la
     * póliza del seguro
     *
     */
    val references: Map<DeliveryProcedure, String> = emptyMap(),

    /**
     *
     * el día de la entrega (AAAA-MM-DD). Está vacío hasta que se programa
     *
     */
    val deliveryDate: String? = null,

    /**
     *
     * el nombre de quien firmó el acta de entrega. Está vacío hasta que se firma
     *
     */
    val signedBy: String? = null,

    /**
     *
     * en qué va la entrega. Empieza haciendo los trámites
     *
     */
    val status: DeliveryStatus = DeliveryStatus.EN_TRAMITES,
) {
    /**
     *
     * los trámites que todavía no están completados
     *
     */
    val pendingProcedures: List<DeliveryProcedure>
        get() = DeliveryProcedure.entries.filter { procedures[it] != ProcedureStatus.COMPLETADO }

    /**
     *
     * true cuando la revisión pre-entrega ya se aprobó. Sin ella no se entrega
     * el vehículo
     *
     */
    val isPreDeliveryApproved: Boolean
        get() = procedures[DeliveryProcedure.REVISION_PRE_ENTREGA] == ProcedureStatus.COMPLETADO

    /**
     *
     * el texto del estado de un trámite para mostrarlo en la pantalla. En la
     * revisión pre-entrega, completado se dice aprobada
     *
     */
    fun statusLabel(procedure: DeliveryProcedure): String {
        val status = procedures[procedure] ?: ProcedureStatus.PENDIENTE
        return if (procedure == DeliveryProcedure.REVISION_PRE_ENTREGA && status == ProcedureStatus.COMPLETADO) {
            "Aprobada"
        } else {
            status.label
        }
    }
}

/**
 *
 * lo que puede pasar al avanzar un trámite
 *
 */
sealed interface ProcedureAdvanceResult {
    /**
     *
     * el trámite pasó a su siguiente estado. Trae la entrega actualizada y el
     * trámite que avanzó
     *
     */
    data class Advanced(val delivery: Delivery, val procedure: DeliveryProcedure) : ProcedureAdvanceResult

    /**
     *
     * el trámite no pudo avanzar. Trae el mensaje para el asesor
     *
     */
    data class InvalidData(val message: String) : ProcedureAdvanceResult
}

/**
 *
 * lo que puede pasar al programar la entrega
 *
 */
sealed interface DeliveryScheduleResult {
    /**
     *
     * la entrega quedó programada. Trae la entrega con su fecha
     *
     */
    data class Scheduled(val delivery: Delivery) : DeliveryScheduleResult

    /**
     *
     * todavía hay trámites sin completar, así que no se programa. Trae cuáles son
     *
     */
    data class PendingProcedures(val pendingProcedures: List<DeliveryProcedure>) : DeliveryScheduleResult

    /**
     *
     * algún dato no sirve, por ejemplo la fecha. Trae el mensaje para el asesor
     *
     */
    data class InvalidData(val message: String) : DeliveryScheduleResult
}

/**
 *
 * lo que puede pasar al firmar el acta de entrega
 *
 */
sealed interface DeliveryCloseResult {
    /**
     *
     * el acta quedó firmada y la venta cerrada. Trae la entrega actualizada
     *
     */
    data class Closed(val delivery: Delivery) : DeliveryCloseResult

    /**
     *
     * no se pudo firmar el acta. Trae el mensaje para el asesor
     *
     */
    data class InvalidData(val message: String) : DeliveryCloseResult
}
