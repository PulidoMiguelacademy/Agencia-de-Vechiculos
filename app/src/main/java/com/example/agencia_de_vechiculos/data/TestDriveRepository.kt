package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.TestDriveAppointment
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import com.example.agencia_de_vechiculos.model.TestDriveStatus

class TestDriveRepository(
    initialAppointments: List<TestDriveAppointment> = sampleAppointments,
) {
    private val appointments = initialAppointments.toMutableList()
    private var nextId = (appointments.maxOfOrNull { it.id } ?: 0) + 1

    fun allAppointments(): List<TestDriveAppointment> = appointments.toList()

    fun activeAppointmentsForClient(clientName: String, phone: String = ""): List<TestDriveAppointment> {
        val normalizedName = clientName.trim().lowercase()
        val normalizedPhone = phone.trim()
        return appointments.filter { appointment ->
            appointment.status == TestDriveStatus.ACTIVA && (
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

    /** Devuelve los horarios ya reservados (activos) para ese vehículo o sucursal en esa fecha. */
    fun occupiedSlots(vehicleId: Int, branch: Branch, date: String): Set<String> =
        appointments
            .filter {
                it.status == TestDriveStatus.ACTIVA &&
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

        // Criterio 2: Dado un horario ocupado, cuando intento reservarlo, el sistema me ofrece otros horarios.
        val occupied = occupiedSlots(vehicleId, branch, cleanDate)
        if (cleanTime in occupied) {
            val alternatives = availableSlots(vehicleId, branch, cleanDate)
            return TestDriveBookingResult.SlotOccupied(
                requestedTime = cleanTime,
                suggestedTimes = alternatives,
            )
        }

        // Criterio 1: Dado un horario libre, cuando lo selecciono, la cita se confirma y recibo un correo.
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
            "Correo de confirmación enviado a $cleanEmail: Cita #${appointment.id} confirmada el $cleanDate a las $cleanTime (${TestDriveAppointment.DURATION_MINUTES} min) en ${branch.label}."

        return TestDriveBookingResult.Confirmed(
            appointment = appointment,
            confirmationEmail = emailNotification,
        )
    }

    fun cancelAppointment(appointmentId: Int): Boolean {
        val idx = appointments.indexOfFirst { it.id == appointmentId }
        if (idx == -1) return false
        appointments[idx] = appointments[idx].copy(status = TestDriveStatus.CANCELADA)
        return true
    }

    companion object {
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
