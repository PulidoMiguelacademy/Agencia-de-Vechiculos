package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.InterestLevel
import com.example.agencia_de_vechiculos.model.TestDriveAppointment
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import com.example.agencia_de_vechiculos.model.TestDriveCompleteResult
import com.example.agencia_de_vechiculos.model.TestDriveOutcome
import com.example.agencia_de_vechiculos.model.TestDriveStartResult
import com.example.agencia_de_vechiculos.model.TestDriveStatus

class TestDriveRepository(
    initialAppointments: List<TestDriveAppointment> = sampleAppointments,
) {
    private val appointments = initialAppointments.toMutableList()
    private val outcomes = mutableMapOf<Int, TestDriveOutcome>()
    private var nextId = (appointments.maxOfOrNull { it.id } ?: 0) + 1

    fun allAppointments(): List<TestDriveAppointment> = appointments.toList()

    fun allOutcomes(): List<TestDriveOutcome> = outcomes.values.toList()

    fun outcomeFor(appointmentId: Int): TestDriveOutcome? = outcomes[appointmentId]

    fun activeAppointmentsForClient(clientName: String, phone: String = ""): List<TestDriveAppointment> {
        val normalizedName = clientName.trim().lowercase()
        val normalizedPhone = phone.trim()
        return appointments.filter { appointment ->
            (appointment.status == TestDriveStatus.ACTIVA || appointment.status == TestDriveStatus.EN_CURSO) && (
                (normalizedName.isNotEmpty() && appointment.clientName.trim().lowercase() == normalizedName) ||
                    (normalizedPhone.isNotEmpty() && appointment.phone.trim() == normalizedPhone)
                )
        }
    }

    /** Devuelve los horarios de 30 minutos que están libres para una fecha, sucursal y vehículo. */
    fun availableSlots(vehicleId: Int, branch: Branch, date: String): List<String> {
        val occupied = occupiedSlots(vehicleId, branch, date)
        return TIME_SLOTS_30_MIN.filterNot { it in occupied }
    }

    /** Devuelve los horarios ya reservados (activos o en curso) para ese vehículo o sucursal en esa fecha. */
    fun occupiedSlots(vehicleId: Int, branch: Branch, date: String): Set<String> =
        appointments
            .filter {
                (it.status == TestDriveStatus.ACTIVA || it.status == TestDriveStatus.EN_CURSO) &&
                    it.date == date.trim() &&
                    (it.vehicleId == vehicleId || it.branch == branch)
            }
            .map { it.time }
            .toSet()

    /**
     * HU-15: Reserva una prueba de manejo validando campos, límite de 2 pruebas activas por cliente,
     * disponibilidad del horario (bloques de 30 min) y envío de confirmación por correo.
     */
    fun schedule(
        clientName: String,
        phone: String,
        vehicleId: Int,
        branch: Branch,
        date: String,
        time: String,
        clientEmail: String = defaultEmailFor(clientName),
    ): TestDriveBookingResult {
        val cleanName = clientName.trim()
        val cleanPhone = phone.trim()
        val cleanDate = date.trim()
        val cleanTime = time.trim()
        val cleanEmail = clientEmail.trim().ifEmpty { defaultEmailFor(cleanName) }

        if (cleanName.isEmpty()) {
            return TestDriveBookingResult.InvalidData("El nombre del cliente es obligatorio")
        }
        if (cleanPhone.length < 7) {
            return TestDriveBookingResult.InvalidData("Ingresa un número de teléfono válido")
        }
        if (cleanDate.isEmpty()) {
            return TestDriveBookingResult.InvalidData("Selecciona una fecha para la prueba de manejo")
        }
        if (cleanTime.isEmpty()) {
            return TestDriveBookingResult.InvalidData("Selecciona una hora para la prueba de manejo")
        }

        // Regla HU-15: Máximo 2 pruebas activas por cliente.
        val activeForClient = activeAppointmentsForClient(cleanName, cleanPhone)
        if (activeForClient.size >= TestDriveAppointment.MAX_ACTIVE_PER_CLIENT) {
            return TestDriveBookingResult.MaxActiveReached()
        }

        // Criterio 2 (HU-15): Dado un horario ocupado, cuando intento reservarlo, el sistema me ofrece otros horarios.
        val occupied = occupiedSlots(vehicleId, branch, cleanDate)
        if (cleanTime in occupied) {
            val alternatives = availableSlots(vehicleId, branch, cleanDate)
            return TestDriveBookingResult.SlotOccupied(
                requestedTime = cleanTime,
                suggestedTimes = alternatives,
            )
        }

        // Criterio 1 (HU-15): Dado un horario libre, cuando lo selecciono, la cita se confirma y recibo un correo.
        val appointment = TestDriveAppointment(
            id = nextId++,
            clientName = cleanName,
            clientEmail = cleanEmail,
            phone = cleanPhone,
            vehicleId = vehicleId,
            branch = branch,
            date = cleanDate,
            time = cleanTime,
            durationMinutes = TestDriveAppointment.DURATION_MINUTES,
            status = TestDriveStatus.ACTIVA,
        )
        appointments.add(appointment)

        val emailNotification =
            "Correo de confirmación enviado a $cleanEmail: Cita #${appointment.id} confirmada el $cleanDate a las $cleanTime por ${TestDriveAppointment.DURATION_MINUTES} min en ${branch.label}."

        return TestDriveBookingResult.Confirmed(
            appointment = appointment,
            confirmationEmail = emailNotification,
        )
    }

    /**
     * HU-16 (Criterios 1 y 2 + Regla "Sin licencia vigente no hay prueba"):
     * Valida la licencia del cliente y su fecha de vigencia para permitir o rechazar el inicio de la prueba.
     */
    fun startTestDrive(
        appointmentId: Int,
        licenseNumber: String,
        licenseExpirationDate: String,
        currentDate: String? = null,
    ): TestDriveStartResult {
        val index = appointments.indexOfFirst { it.id == appointmentId }
        if (index == -1) {
            return TestDriveStartResult.InvalidStartData("La cita seleccionada no existe")
        }

        val appointment = appointments[index]
        if (appointment.status == TestDriveStatus.CANCELADA || appointment.status == TestDriveStatus.COMPLETADA) {
            return TestDriveStartResult.InvalidStartData("La cita no se encuentra activa para iniciar")
        }

        val cleanLicense = licenseNumber.trim()
        val cleanExpiration = licenseExpirationDate.trim()
        val referenceDate = currentDate?.trim()?.ifEmpty { appointment.date } ?: appointment.date

        if (cleanLicense.isEmpty()) {
            return TestDriveStartResult.InvalidStartData("El número de licencia es obligatorio")
        }
        if (!DATE_REGEX.matches(cleanExpiration)) {
            return TestDriveStartResult.InvalidStartData("Ingresa la vigencia de la licencia en formato AAAA-MM-DD")
        }

        // Criterio 1: debe ser una cita del día.
        if (referenceDate != appointment.date) {
            return TestDriveStartResult.InvalidStartData(
                "Solo se puede iniciar una cita programada para el día $referenceDate. Fecha de la cita: ${appointment.date}"
            )
        }

        // Criterio 2 y Regla: Dado una licencia vencida, cuando intento iniciar, el sistema no lo permite.
        if (cleanExpiration < referenceDate) {
            return TestDriveStartResult.LicenseExpired(
                licenseNumber = cleanLicense,
                expirationDate = cleanExpiration,
                appointmentDate = referenceDate,
            )
        }

        // Criterio 1: Dado una cita del día, cuando registro la licencia vigente, entonces la prueba puede iniciar.
        val updated = appointment.copy(
            status = TestDriveStatus.EN_CURSO,
            licenseNumber = cleanLicense,
            licenseExpirationDate = cleanExpiration,
        )
        appointments[index] = updated
        return TestDriveStartResult.Started(updated)
    }

    /**
     * HU-16 (Criterio 3 + Datos: kilometraje recorrido, nivel de interés, observaciones):
     * Registra el resultado de una prueba iniciada con licencia vigente y programa automáticamente el seguimiento.
     */
    fun completeTestDrive(
        appointmentId: Int,
        distanceKm: Int,
        interestLevel: InterestLevel,
        observations: String,
    ): TestDriveCompleteResult {
        val index = appointments.indexOfFirst { it.id == appointmentId }
        if (index == -1) {
            return TestDriveCompleteResult.InvalidOutcomeData("La cita seleccionada no existe")
        }

        val appointment = appointments[index]

        // Regla HU-16: Sin licencia vigente no hay prueba.
        val licenseNumber = appointment.licenseNumber
        val licenseExpiration = appointment.licenseExpirationDate
        if (
            appointment.status != TestDriveStatus.EN_CURSO ||
            licenseNumber.isNullOrBlank() ||
            licenseExpiration.isNullOrBlank() ||
            licenseExpiration < appointment.date
        ) {
            return TestDriveCompleteResult.LicenseNotValidated()
        }

        if (distanceKm <= 0) {
            return TestDriveCompleteResult.InvalidOutcomeData("El kilometraje recorrido debe ser mayor a 0 km")
        }

        val cleanObservations = observations.trim().ifEmpty { "Sin observaciones adicionales" }
        val followUpDescription =
            "Seguimiento programado para ${appointment.clientName}, tel. ${appointment.phone}, en ${interestLevel.followUpDays} día o más: ${interestLevel.followUpPlan}."

        val outcome = TestDriveOutcome(
            appointmentId = appointment.id,
            licenseNumber = licenseNumber,
            licenseExpirationDate = licenseExpiration,
            distanceKm = distanceKm,
            interestLevel = interestLevel,
            observations = cleanObservations,
            scheduledFollowUp = followUpDescription,
        )

        val completedAppointment = appointment.copy(status = TestDriveStatus.COMPLETADA)
        appointments[index] = completedAppointment
        outcomes[appointment.id] = outcome

        return TestDriveCompleteResult.Completed(
            appointment = completedAppointment,
            outcome = outcome,
        )
    }

    fun cancelAppointment(appointmentId: Int): Boolean {
        val idx = appointments.indexOfFirst { it.id == appointmentId }
        if (idx == -1) return false
        appointments[idx] = appointments[idx].copy(status = TestDriveStatus.CANCELADA)
        return true
    }

    companion object {
        private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        /** Bloques de 30 minutos (Regla HU-15: Duración de 30 minutos). */
        val TIME_SLOTS_30_MIN = listOf(
            "09:00",
            "09:30",
            "10:00",
            "10:30",
            "11:00",
            "11:30",
            "14:00",
            "14:30",
            "15:00",
            "15:30",
            "16:00",
            "16:30",
        )

        private fun defaultEmailFor(name: String): String {
            val slug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), ".").trim('.')
            return if (slug.isNotEmpty()) "$slug@cliente.com" else "cliente@agencia.com"
        }
    }
}

val sampleAppointments = listOf(
    TestDriveAppointment(
        id = 1,
        clientName = "Carlos Pérez",
        clientEmail = "carlos.perez@cliente.com",
        phone = "3001234567",
        vehicleId = 1,
        branch = Branch.NORTE,
        date = "2026-10-20",
        time = "10:00",
    ),
    TestDriveAppointment(
        id = 2,
        clientName = "Laura Gómez",
        clientEmail = "laura.gomez@cliente.com",
        phone = "3109876543",
        vehicleId = 2,
        branch = Branch.CENTRO,
        date = "2026-10-20",
        time = "11:00",
    ),
)
