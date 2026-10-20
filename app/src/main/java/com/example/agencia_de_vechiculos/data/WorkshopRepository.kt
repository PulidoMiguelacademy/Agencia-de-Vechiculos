package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.RegisteredVehicle
import com.example.agencia_de_vechiculos.model.ServiceType
import com.example.agencia_de_vechiculos.model.WorkshopAppointment
import com.example.agencia_de_vechiculos.model.WorkshopBookingResult
import com.example.agencia_de_vechiculos.model.WorkshopSlot
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WorkshopRepository(
    private val registeredVehicles: List<RegisteredVehicle> = sampleRegisteredVehicles,
    initialAppointments: List<WorkshopAppointment> = sampleWorkshopAppointments,
) {
    private val appointments = initialAppointments.toMutableList()
    private var nextId = (appointments.maxOfOrNull { it.id } ?: 0) + 1

    fun allAppointments(): List<WorkshopAppointment> = appointments.toList()

    fun allRegisteredVehicles(): List<RegisteredVehicle> = registeredVehicles.toList()

    fun registeredVehicle(plate: String): RegisteredVehicle? {
        val cleanPlate = normalizePlate(plate)
        return registeredVehicles.firstOrNull { normalizePlate(it.plate) == cleanPlate }
    }

    fun appointmentsFor(branch: Branch, date: String): List<WorkshopAppointment> =
        appointments.filter { it.branch == branch && it.date == date.trim() }

    fun availableSlots(branch: Branch, date: String): List<String> {
        val booked = appointmentsFor(branch, date)
        if (booked.size >= DAILY_CAPACITY) return emptyList()
        val occupied = booked.map { it.time }.toSet()
        return TIME_SLOTS.filterNot { it in occupied }
    }

    fun schedule(
        plate: String,
        serviceType: ServiceType,
        branch: Branch,
        date: String,
        time: String,
        comments: String = "",
        today: String? = null,
    ): WorkshopBookingResult {
        val cleanDate = date.trim()
        val cleanTime = time.trim()

        if (plate.isBlank()) {
            return WorkshopBookingResult.InvalidData("Ingresa la placa del vehículo")
        }
        val vehicle = registeredVehicle(plate)
            ?: return WorkshopBookingResult.UnregisteredVehicle(normalizePlate(plate))

        if (parseDate(cleanDate) == null) {
            return WorkshopBookingResult.InvalidData("Ingresa la fecha de la cita en formato AAAA-MM-DD")
        }
        if (today != null && cleanDate < today) {
            return WorkshopBookingResult.InvalidData("La fecha de la cita no puede ser anterior a hoy ($today)")
        }
        if (cleanTime !in TIME_SLOTS) {
            return WorkshopBookingResult.InvalidData("Selecciona uno de los horarios del taller")
        }

        if (cleanTime !in availableSlots(branch, cleanDate)) {
            return WorkshopBookingResult.NoCapacity(
                requested = WorkshopSlot(cleanDate, cleanTime),
                alternatives = alternatives(branch, cleanDate),
            )
        }

        val appointment = WorkshopAppointment(
            id = nextId++,
            clientName = vehicle.clientName,
            plate = vehicle.plate,
            serviceType = serviceType,
            branch = branch,
            date = cleanDate,
            time = cleanTime,
            comments = comments.trim(),
        )
        appointments.add(appointment)

        val reminder =
            "Recordatorio enviado a ${vehicle.clientName}: cita #${appointment.id} de ${serviceType.label.lowercase()} " +
                "para el vehículo ${vehicle.plate} el $cleanDate a las $cleanTime en ${branch.label}. " +
                "Te avisaremos otra vez un día antes."

        return WorkshopBookingResult.Confirmed(appointment, reminder)
    }

    private fun alternatives(branch: Branch, date: String): List<WorkshopSlot> {
        val options = mutableListOf<WorkshopSlot>()
        var day = date
        repeat(DAYS_TO_SEARCH) {
            availableSlots(branch, day).forEach { options.add(WorkshopSlot(day, it)) }
            if (options.size >= MAX_ALTERNATIVES) return options.take(MAX_ALTERNATIVES)
            day = nextDay(day)
        }
        return options
    }

    companion object {
        const val DAILY_CAPACITY = 4
        private const val MAX_ALTERNATIVES = 6
        private const val DAYS_TO_SEARCH = 7

        val TIME_SLOTS = listOf("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00")

        private fun normalizePlate(plate: String): String =
            plate.uppercase().filter { it.isLetterOrDigit() }

        private fun dateFormat(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        private fun parseDate(date: String): Calendar? {
            if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) return null
            return try {
                Calendar.getInstance().apply { time = dateFormat().parse(date)!! }
            } catch (e: ParseException) {
                null
            }
        }

        private fun nextDay(date: String): String {
            val calendar = parseDate(date) ?: return date
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            return dateFormat().format(calendar.time)
        }
    }
}

val sampleRegisteredVehicles = listOf(
    RegisteredVehicle("ABC123", "Carlos Pérez", "Renault", "Duster", 2020),
    RegisteredVehicle("DEF456", "Laura Gómez", "Mazda", "3", 2022),
    RegisteredVehicle("GHI789", "Andrés Ramírez", "Toyota", "Corolla", 2025),
    RegisteredVehicle("JKL012", "Sofía Torres", "Kia", "Picanto", 2020),
)

val sampleWorkshopAppointments = listOf(
    WorkshopAppointment(1, "Laura Gómez", "DEF456", ServiceType.CAMBIO_ACEITE, Branch.NORTE, "2026-10-20", "09:00"),
    WorkshopAppointment(2, "Andrés Ramírez", "GHI789", ServiceType.MANTENIMIENTO, Branch.NORTE, "2026-10-21", "08:00"),
    WorkshopAppointment(3, "Sofía Torres", "JKL012", ServiceType.FRENOS, Branch.NORTE, "2026-10-21", "09:00"),
    WorkshopAppointment(4, "Laura Gómez", "DEF456", ServiceType.ALINEACION, Branch.NORTE, "2026-10-21", "10:00"),
    WorkshopAppointment(5, "Carlos Pérez", "ABC123", ServiceType.DIAGNOSTICO, Branch.NORTE, "2026-10-21", "11:00"),
)
