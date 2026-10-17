package com.example.agencia_de_vechiculos.model

enum class Branch(val label: String) {
    NORTE("Sede Norte"),
    CENTRO("Sede Centro"),
    SUR("Sede Sur"),
}

enum class TestDriveStatus(val label: String) {
    ACTIVA("Agendada"),
    EN_CURSO("En curso"),
    COMPLETADA("Completada"),
    CANCELADA("Cancelada"),
}

/**
 * HU-16: Nivel de interés del cliente al finalizar la prueba de manejo.
 * Criterio 3: Al registrar el nivel de interés, se programa un seguimiento.
 */
enum class InterestLevel(
    val label: String,
    val followUpDays: Int,
    val followUpPlan: String,
) {
    ALTO(
        label = "Alto",
        followUpDays = 1,
        followUpPlan = "Llamada prioritaria en 24h y envío de cotización formal para cierre",
    ),
    MEDIO(
        label = "Medio",
        followUpDays = 3,
        followUpPlan = "Contacto en 72h con opciones de financiamiento y planes de pago",
    ),
    BAJO(
        label = "Bajo",
        followUpDays = 7,
        followUpPlan = "Seguimiento en 7 días con alternativas de otros modelos del catálogo",
    ),
}

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
    val licenseNumber: String? = null,
    val licenseExpirationDate: String? = null,
) {
    companion object {
        const val DURATION_MINUTES = 30
        const val MAX_ACTIVE_PER_CLIENT = 2
    }
}

/**
 * HU-16: Registrar el resultado de la prueba de manejo.
 * Datos / Campos: Licencia, vigencia, kilometraje recorrido, nivel de interés, observaciones.
 * Reglas: Sin licencia vigente no hay prueba.
 */
data class TestDriveOutcome(
    val appointmentId: Int,
    val licenseNumber: String,
    val licenseExpirationDate: String,
    val distanceKm: Int,
    val interestLevel: InterestLevel,
    val observations: String,
    val scheduledFollowUp: String,
)

sealed interface TestDriveBookingResult {
    /** Criterio 1 (HU-15): Dado un horario libre, cuando lo selecciono, la cita se confirma y recibo un correo. */
    data class Confirmed(
        val appointment: TestDriveAppointment,
        val confirmationEmail: String,
    ) : TestDriveBookingResult

    /** Criterio 2 (HU-15): Dado un horario ocupado, cuando intento reservarlo, el sistema ofrece otros horarios. */
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

sealed interface TestDriveStartResult {
    /** Criterio 1 (HU-16): Dado una cita del día, cuando registro la licencia vigente, entonces la prueba puede iniciar. */
    data class Started(
        val appointment: TestDriveAppointment,
    ) : TestDriveStartResult

    /** Criterio 2 y Regla (HU-16): Dado una licencia vencida, cuando intento iniciar, el sistema no lo permite. */
    data class LicenseExpired(
        val licenseNumber: String,
        val expirationDate: String,
        val appointmentDate: String,
        val message: String = "Sin licencia vigente no hay prueba: la licencia $licenseNumber venció el $expirationDate.",
    ) : TestDriveStartResult

    /** Validaciones de datos o cita. */
    data class InvalidStartData(val message: String) : TestDriveStartResult
}

sealed interface TestDriveCompleteResult {
    /** Criterio 3 (HU-16): Dado que termina la prueba, cuando registre el nivel de interés, se programa un seguimiento. */
    data class Completed(
        val appointment: TestDriveAppointment,
        val outcome: TestDriveOutcome,
    ) : TestDriveCompleteResult

    /** Regla (HU-16): Sin licencia vigente no hay prueba. */
    data class LicenseNotValidated(
        val message: String = "Sin licencia vigente no hay prueba: primero debes validar una licencia vigente e iniciar la prueba.",
    ) : TestDriveCompleteResult

    /** Validación de campos del resultado. */
    data class InvalidOutcomeData(val message: String) : TestDriveCompleteResult
}
