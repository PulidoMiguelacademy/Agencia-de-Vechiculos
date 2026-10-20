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

/**
 * 
 * guarda las citas de taller y los vehículos registrados, y se encarga de agendar
 * revisando el cupo (HU-26)
 * 
 * cada sucursal atiende un vehículo por horario y como máximo DAILY_CAPACITY citas
 * por día. Por ahora todo vive en memoria con datos de ejemplo
 * 
 */
class WorkshopRepository(
    private val registeredVehicles: List<RegisteredVehicle> = sampleRegisteredVehicles,
    initialAppointments: List<WorkshopAppointment> = sampleWorkshopAppointments,
) {
    /**
     * 
     * las citas agendadas
     * 
     */
    private val appointments = initialAppointments.toMutableList()

    /**
     * 
     * el número que le toca a la próxima cita: uno más que la última que exista
     * 
     */
    private var nextId = (appointments.maxOfOrNull { it.id } ?: 0) + 1

    /**
     * 
     * devuelve todas las citas agendadas
     * 
     */
    fun allAppointments(): List<WorkshopAppointment> = appointments.toList()

    /**
     * 
     * devuelve todos los vehículos registrados
     * 
     */
    fun allRegisteredVehicles(): List<RegisteredVehicle> = registeredVehicles.toList()

    /**
     * 
     * busca un vehículo registrado por su placa, o devuelve null si no existe
     * 
     * no importan las mayúsculas, los espacios ni los guiones: abc 123, ABC-123 y
     * abc123 son la misma placa
     * 
     */
    fun registeredVehicle(plate: String): RegisteredVehicle? {
        val cleanPlate = normalizePlate(plate)
        return registeredVehicles.firstOrNull { normalizePlate(it.plate) == cleanPlate }
    }

    /**
     * 
     * devuelve las citas de una sucursal en un día
     * 
     */
    fun appointmentsFor(branch: Branch, date: String): List<WorkshopAppointment> =
        appointments.filter { it.branch == branch && it.date == date.trim() }

    /**
     * 
     * devuelve los horarios libres de una sucursal en un día
     * 
     * si el día ya llegó al cupo no queda ninguno; si no, son los horarios del taller
     * que todavía no tienen cita
     * 
     */
    fun availableSlots(branch: Branch, date: String): List<String> {
        val booked = appointmentsFor(branch, date)
        if (booked.size >= DAILY_CAPACITY) return emptyList()
        val occupied = booked.map { it.time }.toSet()
        return TIME_SLOTS.filterNot { it in occupied }
    }

    /**
     * 
     * agenda una cita de taller
     * 
     * revisa, en orden, que la placa esté registrada, que la fecha sea real y no
     * anterior a hoy (today), y que la hora sea un horario del taller. Si ese horario
     * no tiene cupo, devuelve otras opciones. Si todo está bien, guarda la cita con el
     * nombre del dueño del vehículo y arma el recordatorio para el cliente
     * 
     */
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

    /**
     * 
     * busca otras opciones cuando el horario pedido no tiene cupo
     * 
     * empieza por los horarios libres del mismo día y, si no alcanzan, sigue con los
     * días siguientes, hasta juntar MAX_ALTERNATIVES o revisar DAYS_TO_SEARCH días
     * 
     */
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
        /**
         * 
         * el cupo del taller: cuántas citas por día recibe cada sucursal
         * 
         */
        const val DAILY_CAPACITY = 4

        /**
         * 
         * cuántas opciones se ofrecen como máximo y cuántos días hacia adelante se buscan
         * 
         */
        private const val MAX_ALTERNATIVES = 6
        private const val DAYS_TO_SEARCH = 7

        /**
         * 
         * los horarios en que el taller recibe vehículos, en la mañana y en la tarde
         * 
         */
        val TIME_SLOTS = listOf("08:00", "09:00", "10:00", "11:00", "14:00", "15:00", "16:00")

        /**
         * 
         * deja la placa en mayúsculas y solo con letras y números, para comparar placas
         * escritas de formas distintas
         * 
         */
        private fun normalizePlate(plate: String): String =
            plate.uppercase().filter { it.isLetterOrDigit() }

        /**
         * 
         * el formato de las fechas (AAAA-MM-DD). No acepta fechas que no existen, como el
         * 30 de febrero
         * 
         */
        private fun dateFormat(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        /**
         * 
         * convierte una fecha escrita en una fecha de calendario, o devuelve null si está
         * mal escrita o no existe
         * 
         */
        private fun parseDate(date: String): Calendar? {
            if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(date)) return null
            return try {
                Calendar.getInstance().apply { time = dateFormat().parse(date)!! }
            } catch (e: ParseException) {
                null
            }
        }

        /**
         * 
         * devuelve el día siguiente a una fecha. Sirve para buscar opciones en los días
         * que vienen
         * 
         */
        private fun nextDay(date: String): String {
            val calendar = parseDate(date) ?: return date
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            return dateFormat().format(calendar.time)
        }
    }
}

/**
 * 
 * vehículos registrados de ejemplo, de los mismos clientes de las cotizaciones
 * 
 */
val sampleRegisteredVehicles = listOf(
    RegisteredVehicle("ABC123", "Carlos Pérez", "Renault", "Duster", 2020, 68_500),
    RegisteredVehicle("DEF456", "Laura Gómez", "Mazda", "3", 2022, 31_200),
    RegisteredVehicle("GHI789", "Andrés Ramírez", "Toyota", "Corolla", 2025, 1_200),
    RegisteredVehicle("JKL012", "Sofía Torres", "Kia", "Picanto", 2020, 60_100),
)

/**
 * 
 * citas de ejemplo en la Sede Norte: el 2026-10-20 tiene ocupadas las 09:00 y el
 * 2026-10-21 está lleno, para poder ver los dos casos sin cupo
 * 
 */
val sampleWorkshopAppointments = listOf(
    WorkshopAppointment(1, "Laura Gómez", "DEF456", ServiceType.CAMBIO_ACEITE, Branch.NORTE, "2026-10-20", "09:00"),
    WorkshopAppointment(2, "Andrés Ramírez", "GHI789", ServiceType.MANTENIMIENTO, Branch.NORTE, "2026-10-21", "08:00"),
    WorkshopAppointment(3, "Sofía Torres", "JKL012", ServiceType.FRENOS, Branch.NORTE, "2026-10-21", "09:00"),
    WorkshopAppointment(4, "Laura Gómez", "DEF456", ServiceType.ALINEACION, Branch.NORTE, "2026-10-21", "10:00"),
    WorkshopAppointment(5, "Carlos Pérez", "ABC123", ServiceType.DIAGNOSTICO, Branch.NORTE, "2026-10-21", "11:00"),
)
