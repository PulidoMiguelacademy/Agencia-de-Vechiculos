package com.example.agencia_de_vechiculos.model

/**
 * 
 * cuánto combustible trae el vehículo al llegar al taller
 * 
 */
enum class FuelLevel(val label: String) {
    RESERVA("Reserva"),
    UN_CUARTO("1/4"),
    MEDIO("1/2"),
    TRES_CUARTOS("3/4"),
    LLENO("Lleno"),
}

/**
 * 
 * las zonas del vehículo donde se puede marcar un daño previo
 * 
 */
enum class VehicleArea(val label: String) {
    CAPO("Capó"),
    PARACHOQUES_DELANTERO("Parachoques delantero"),
    PARACHOQUES_TRASERO("Parachoques trasero"),
    LATERAL_IZQUIERDO("Lateral izquierdo"),
    LATERAL_DERECHO("Lateral derecho"),
    TECHO("Techo"),
    VIDRIOS("Vidrios"),
    RINES("Rines"),
}

/**
 * 
 * un daño que el vehículo ya tenía al llegar al taller. Queda en el acta de
 * recepción con sus fotos, así nadie puede reclamar después que lo hizo el taller
 * 
 */
data class PreviousDamage(
    /**
     * 
     * en qué zona está el daño
     * 
     */
    val area: VehicleArea,

    /**
     * 
     * cómo es el daño, por ejemplo rayón de 10 cm
     * 
     */
    val description: String,

    /**
     * 
     * las fotos del daño, guardadas como el enlace (uri) que da el selector de fotos.
     * Todo daño tiene al menos una
     * 
     */
    val photos: List<String>,
)

/**
 * 
 * en qué va la orden de servicio: abierta mientras se llena el acta, y en
 * diagnóstico cuando el cliente la firma
 * 
 */
enum class ServiceOrderStatus(val label: String) {
    ABIERTA("Abierta"),
    EN_DIAGNOSTICO("En diagnóstico"),
}

/**
 * 
 * la orden de servicio que se abre cuando llega al taller el vehículo de una cita
 * del día (HU-27), con su acta de recepción
 * 
 * el acta guarda cómo llegó el vehículo: kilometraje, combustible, pertenencias,
 * lo que pide el cliente y los daños que ya tenía. Sabe por sí sola qué le falta
 * para estar completa y si ya se puede empezar a trabajar en el vehículo
 * 
 */
data class ServiceOrder(
    /**
     * 
     * el número de la orden
     * 
     */
    val id: Int,

    /**
     * 
     * la cita de taller de la que salió la orden
     * 
     */
    val appointmentId: Int,

    /**
     * 
     * la placa del vehículo
     * 
     */
    val plate: String,

    /**
     * 
     * el nombre del cliente, tomado de la cita
     * 
     */
    val clientName: String,

    /**
     * 
     * el servicio que se pidió en la cita
     * 
     */
    val serviceType: ServiceType,

    /**
     * 
     * el día en que llegó el vehículo y se abrió la orden (AAAA-MM-DD)
     * 
     */
    val openedDate: String,

    /**
     * 
     * el kilometraje con que llegó. Está vacío hasta que se llena el acta
     * 
     */
    val mileageKm: Int? = null,

    /**
     * 
     * el combustible con que llegó. Está vacío hasta que se llena el acta
     * 
     */
    val fuelLevel: FuelLevel? = null,

    /**
     * 
     * las cosas del cliente que quedan dentro del vehículo. Puede quedar vacío
     * 
     */
    val belongings: String = "",

    /**
     * 
     * lo que el cliente pide que se revise o se arregle
     * 
     */
    val clientRequest: String = "",

    /**
     * 
     * los daños previos marcados en el acta
     * 
     */
    val damages: List<PreviousDamage> = emptyList(),

    /**
     * 
     * el nombre de quien firmó el acta. Está vacío hasta que se firma
     * 
     */
    val signedBy: String? = null,

    /**
     * 
     * en qué va la orden. Empieza abierta
     * 
     */
    val status: ServiceOrderStatus = ServiceOrderStatus.ABIERTA,
) {
    /**
     * 
     * lo que le falta al acta para poder firmarse: el kilometraje, el combustible y
     * la solicitud del cliente
     * 
     */
    val missingFields: List<String>
        get() = buildList {
            if (mileageKm == null) add("Kilometraje")
            if (fuelLevel == null) add("Combustible")
            if (clientRequest.isBlank()) add("Solicitud del cliente")
        }

    /**
     * 
     * true cuando el acta tiene todo lo necesario para firmarse
     * 
     */
    val isRecordComplete: Boolean
        get() = missingFields.isEmpty()

    /**
     * 
     * true cuando ya se puede empezar a trabajar en el vehículo. Solo pasa con el acta
     * firmada: sin firma no se inicia el trabajo
     * 
     */
    val canStartWork: Boolean
        get() = status == ServiceOrderStatus.EN_DIAGNOSTICO
}

/**
 * 
 * lo que puede pasar al registrar la llegada de un vehículo
 * 
 */
sealed interface ServiceOrderOpenResult {
    /**
     * 
     * se abrió la orden de servicio
     * 
     */
    data class Opened(val order: ServiceOrder) : ServiceOrderOpenResult

    /**
     * 
     * la cita ya tenía orden. Trae esa orden en vez de abrir otra
     * 
     */
    data class AlreadyOpened(val order: ServiceOrder) : ServiceOrderOpenResult

    /**
     * 
     * no se pudo abrir la orden, por ejemplo porque la cita no es de hoy. Trae el
     * mensaje para el asesor
     * 
     */
    data class InvalidData(val message: String) : ServiceOrderOpenResult
}

/**
 * 
 * lo que puede pasar al guardar los datos del acta o al agregar un daño
 * 
 */
sealed interface ReceptionResult {
    /**
     * 
     * quedó guardado en el acta. Trae la orden actualizada
     * 
     */
    data class Saved(val order: ServiceOrder) : ReceptionResult

    /**
     * 
     * el kilometraje escrito es menor que el último registrado. Trae ese último
     * 
     */
    data class MileageBelowLast(val lastMileageKm: Int) : ReceptionResult

    /**
     * 
     * no se pudo guardar. Trae el mensaje para el asesor
     * 
     */
    data class InvalidData(val message: String) : ReceptionResult
}

/**
 * 
 * lo que puede pasar al firmar el acta de recepción
 * 
 */
sealed interface ServiceOrderSignResult {
    /**
     * 
     * el acta quedó firmada y la orden pasó a diagnóstico
     * 
     */
    data class Signed(val order: ServiceOrder) : ServiceOrderSignResult

    /**
     * 
     * al acta le faltan datos. Trae cuáles
     * 
     */
    data class IncompleteRecord(val missingFields: List<String>) : ServiceOrderSignResult

    /**
     * 
     * no se pudo firmar, por ejemplo sin el nombre. Trae el mensaje para el asesor
     * 
     */
    data class InvalidData(val message: String) : ServiceOrderSignResult
}
