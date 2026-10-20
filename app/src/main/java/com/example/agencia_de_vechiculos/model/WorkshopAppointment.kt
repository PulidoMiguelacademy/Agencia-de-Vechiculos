package com.example.agencia_de_vechiculos.model

enum class ServiceType(val label: String) {
    MANTENIMIENTO("Mantenimiento preventivo"),
    CAMBIO_ACEITE("Cambio de aceite"),
    FRENOS("Revisión de frenos"),
    ALINEACION("Alineación y balanceo"),
    DIAGNOSTICO("Diagnóstico general"),
}

data class RegisteredVehicle(
    val plate: String,
    val clientName: String,
    val brand: String,
    val model: String,
    val year: Int,
)

data class WorkshopSlot(
    val date: String,
    val time: String,
)

data class WorkshopAppointment(
    val id: Int,
    val clientName: String,
    val plate: String,
    val serviceType: ServiceType,
    val branch: Branch,
    val date: String,
    val time: String,
    val comments: String = "",
)

sealed interface WorkshopBookingResult {
    data class Confirmed(
        val appointment: WorkshopAppointment,
        val reminder: String,
    ) : WorkshopBookingResult

    data class NoCapacity(
        val requested: WorkshopSlot,
        val alternatives: List<WorkshopSlot>,
    ) : WorkshopBookingResult

    data class UnregisteredVehicle(val plate: String) : WorkshopBookingResult

    data class InvalidData(val message: String) : WorkshopBookingResult
}
