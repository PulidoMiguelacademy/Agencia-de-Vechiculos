package com.example.agencia_de_vechiculos.model

enum class Branch(val label: String) {
    NORTE("Sede Norte"),
    CENTRO("Sede Centro"),
    SUR("Sede Sur"),
}

enum class TestDriveStatus { ACTIVA, COMPLETADA, CANCELADA }

/**
 * HU-15: Agendar una prueba de manejo.
 * Datos / Campos: Cliente, vehículo, sucursal, fecha, hora, teléfono.
 * Reglas: Duración de 30 minutos. Máximo 2 pruebas activas por cliente.
 */
data class TestDriveAppointment(
    val id: Int,
    val clientName: String,
    val clientEmail: String,
    val phone: String,
    val vehicleId: Int,
    val branch: Branch,
    val date: String,
    val time: String,
    val durationMinutes: Int = DURATION_MINUTES,
    val status: TestDriveStatus = TestDriveStatus.ACTIVA,
) {
    companion object {
        const val DURATION_MINUTES = 30
        const val MAX_ACTIVE_PER_CLIENT = 2
    }
}

sealed interface TestDriveBookingResult {
    /** Criterio 1: Dado un horario libre, cuando lo selecciono, la cita se confirma y recibo un correo. */
    data class Confirmed(
        val appointment: TestDriveAppointment,
        val confirmationEmail: String,
    ) : TestDriveBookingResult

    /** Criterio 2: Dado un horario ocupado, cuando intento reservarlo, el sistema ofrece otros horarios. */
    data class SlotOccupied(
        val requestedTime: String,
        val suggestedTimes: List<String>,
    ) : TestDriveBookingResult

    /** Regla HU-15: Máximo 2 pruebas activas por cliente. */
    data class MaxActiveReached(
        val maxAllowed: Int = TestDriveAppointment.MAX_ACTIVE_PER_CLIENT,
    ) : TestDriveBookingResult

    /** Validación de campos requeridos. */
    data class InvalidData(val message: String) : TestDriveBookingResult
}
