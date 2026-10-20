package com.example.agencia_de_vechiculos.model

/**
 *
 * los servicios que se pueden pedir en el taller. Cada uno trae el texto que se
 * muestra en la pantalla y en el recordatorio
 *
 */
enum class ServiceType(val label: String) {
    MANTENIMIENTO("Mantenimiento preventivo"),
    CAMBIO_ACEITE("Cambio de aceite"),
    FRENOS("Revisión de frenos"),
    ALINEACION("Alineación y balanceo"),
    DIAGNOSTICO("Diagnóstico general"),
}

/**
 *
 * un vehículo de un cliente que está registrado en la agencia. Solo estos
 * vehículos pueden tener cita de taller
 *
 */
data class RegisteredVehicle(
    /**
     *
     * la placa del vehículo, por ejemplo ABC123. Con ella se busca al agendar
     *
     */
    val plate: String,

    /**
     *
     * el nombre del dueño. Es el cliente de la cita, así no hay que escribirlo
     *
     */
    val clientName: String,

    /**
     *
     * la marca del vehículo
     *
     */
    val brand: String,

    /**
     *
     * el modelo del vehículo
     *
     */
    val model: String,

    /**
     *
     * el año del vehículo
     *
     */
    val year: Int,
)

/**
 *
 * un horario del taller: un día y una hora. Se usa para el horario que se pide
 * y para las otras opciones que se ofrecen cuando no hay cupo
 *
 */
data class WorkshopSlot(
    /**
     *
     * el día (AAAA-MM-DD)
     *
     */
    val date: String,

    /**
     *
     * la hora (HH:MM)
     *
     */
    val time: String,
)

/**
 *
 * una cita de servicio en el taller (HU-26)
 *
 */
data class WorkshopAppointment(
    /**
     *
     * el número de la cita
     *
     */
    val id: Int,

    /**
     *
     * el nombre del cliente, tomado del vehículo registrado
     *
     */
    val clientName: String,

    /**
     *
     * la placa del vehículo que entra al taller
     *
     */
    val plate: String,

    /**
     *
     * el servicio que se va a hacer
     *
     */
    val serviceType: ServiceType,

    /**
     *
     * la sucursal donde se atiende. Son las mismas de la prueba de manejo
     *
     */
    val branch: Branch,

    /**
     *
     * el día de la cita (AAAA-MM-DD)
     *
     */
    val date: String,

    /**
     *
     * la hora de la cita (HH:MM)
     *
     */
    val time: String,

    /**
     *
     * lo que el cliente quiera contar del vehículo, por ejemplo un ruido. Puede
     * quedar vacío
     *
     */
    val comments: String = "",
)

/**
 *
 * lo que puede pasar al reservar una cita de taller
 *
 */
sealed interface WorkshopBookingResult {
    /**
     *
     * la cita quedó agendada. Trae la cita y el recordatorio que se le envía al
     * cliente
     *
     */
    data class Confirmed(
        val appointment: WorkshopAppointment,
        val reminder: String,
    ) : WorkshopBookingResult

    /**
     *
     * el horario pedido no tiene cupo. Trae ese horario y otras opciones libres
     *
     */
    data class NoCapacity(
        val requested: WorkshopSlot,
        val alternatives: List<WorkshopSlot>,
    ) : WorkshopBookingResult

    /**
     *
     * la placa no pertenece a ningún vehículo registrado
     *
     */
    data class UnregisteredVehicle(val plate: String) : WorkshopBookingResult

    /**
     *
     * algún dato no sirve (la placa vacía, la fecha o la hora). Trae el mensaje
     * para el cliente
     *
     */
    data class InvalidData(val message: String) : WorkshopBookingResult
}
