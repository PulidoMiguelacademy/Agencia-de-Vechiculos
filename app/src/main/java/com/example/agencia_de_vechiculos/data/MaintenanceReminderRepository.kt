
package com.example.agencia_de_vechiculos.data

import androidx.compose.runtime.mutableStateListOf
import com.example.agencia_de_vechiculos.model.MaintenanceEvaluation
import com.example.agencia_de_vechiculos.model.MaintenanceReminder
import com.example.agencia_de_vechiculos.model.MaintenanceStatus
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.mutableStateMapOf
import com.example.agencia_de_vechiculos.model.ServiceOrderStatus

sealed interface MaintenanceResult {
    data class Success(val reminder: MaintenanceReminder) : MaintenanceResult
    data class Error(val message: String) : MaintenanceResult
}

class MaintenanceReminderRepository(
    private val workshopRepository: WorkshopRepository,
    private val serviceOrderRepository: ServiceOrderRepository
) {
    private val reminders = mutableStateListOf<MaintenanceReminder>()
    private var nextId = 1
    private val sentAlerts = mutableStateMapOf<Int, Int>()

    private fun formatter() =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }

    private fun parseDate(value: String): Calendar? {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) {
            return null
        }

        return try {
            val date = formatter().parse(value)
                ?: return null

            Calendar.getInstance().apply {
                time = date
            }
        } catch (_: ParseException) {
            null
        }
    }

    private fun normalizePlate(plate: String): String =
        plate.uppercase().filter { it.isLetterOrDigit() }

    fun allReminders(): List<MaintenanceReminder> =
        reminders.toList()

    fun create(
        plate: String,
        serviceName: String,
        referenceDate: String,
        referenceMileageKm: Int,
        intervalMonths: Int,
        intervalKm: Int
    ): MaintenanceResult {

        val vehicle = workshopRepository.registeredVehicle(plate)
            ?: return MaintenanceResult.Error(
                "El vehículo no está registrado en el taller"
            )

        if (serviceName.isBlank()) {
            return MaintenanceResult.Error(
                "Ingresa el tipo de mantenimiento"
            )
        }

        val date = parseDate(referenceDate)
            ?: return MaintenanceResult.Error(
                "La fecha debe ser válida y tener formato AAAA-MM-DD"
            )

        if (referenceMileageKm < 0) {
            return MaintenanceResult.Error(
                "El kilometraje no puede ser negativo"
            )
        }

        val currentMileage = serviceOrderRepository.lastMileage(
            vehicle.plate
        )

        if (referenceMileageKm > currentMileage) {
            return MaintenanceResult.Error(
                "El kilometraje de referencia supera el último registrado ($currentMileage km)"
            )
        }

        if (intervalMonths <= 0 || intervalKm <= 0) {
            return MaintenanceResult.Error(
                "Los intervalos deben ser mayores que cero"
            )
        }

        val dueMileage = referenceMileageKm.toLong() +
                intervalKm.toLong()

        if (dueMileage > Int.MAX_VALUE) {
            return MaintenanceResult.Error(
                "El kilometraje calculado supera el límite permitido"
            )
        }

        date.add(Calendar.MONTH, intervalMonths)

        val reminder = MaintenanceReminder(
            id = nextId++,
            plate = vehicle.plate,
            serviceName = serviceName.trim(),
            referenceDate = referenceDate,
            referenceMileageKm = referenceMileageKm,
            intervalMonths = intervalMonths,
            intervalKm = intervalKm,
            dueDate = formatter().format(date.time),
            dueMileageKm = dueMileage.toInt()
        )

        reminders.add(reminder)

        return MaintenanceResult.Success(reminder)
    }

    fun evaluate(
        reminder: MaintenanceReminder,
        today: String
    ): MaintenanceEvaluation {

        val currentMileage = serviceOrderRepository.lastMileage(
            reminder.plate
        )

        val currentDate = parseDate(today)
            ?: return MaintenanceEvaluation(
                reminder,
                currentMileage,
                MaintenanceStatus.AL_DIA,
                "No se pudo comprobar la fecha actual"
            )

        val dueDate = parseDate(reminder.dueDate)
            ?: return MaintenanceEvaluation(
                reminder,
                currentMileage,
                MaintenanceStatus.AL_DIA,
                "La fecha de vencimiento no es válida"
            )

        val daysRemaining = (
                (dueDate.timeInMillis / 86400000L) -
                        (currentDate.timeInMillis / 86400000L)
                )

        val kmRemaining = reminder.dueMileageKm - currentMileage

        val status = when {
            daysRemaining <= 0 || kmRemaining <= 0 ->
                MaintenanceStatus.VENCIDO

            daysRemaining <= 15 || kmRemaining <= 500 ->
                MaintenanceStatus.PROXIMO

            else -> MaintenanceStatus.AL_DIA
        }

        val message = when (status) {
            MaintenanceStatus.VENCIDO ->
                "Mantenimiento requerido por fecha o kilometraje"

            MaintenanceStatus.PROXIMO ->
                "Próximo mantenimiento: $daysRemaining días " +
                        "o $kmRemaining km restantes"

            MaintenanceStatus.AL_DIA ->
                "Al día: $daysRemaining días y " +
                        "$kmRemaining km restantes"
        }

        return MaintenanceEvaluation(
            reminder = reminder,
            currentMileageKm = currentMileage,
            status = status,
            message = message
        )
    }

    fun alertsSent(reminderId: Int): Int =
        sentAlerts[reminderId] ?: 0

    fun canSendAlert(reminderId: Int): Boolean =
        reminders.any { it.id == reminderId } &&
                alertsSent(reminderId) < 2

    fun registerAlert(reminderId: Int): Boolean {
        if (!canSendAlert(reminderId)) return false

        sentAlerts[reminderId] = alertsSent(reminderId) + 1
        return true
    }


    fun createFromCompletedOrder(
        orderId: Int,
        completedDate: String,
        intervalMonths: Int = 6,
        intervalKm: Int = 5000
    ): MaintenanceResult {

        val order = serviceOrderRepository
            .allServiceOrders()
            .firstOrNull { it.id == orderId }
            ?: return MaintenanceResult.Error(
                "La orden de servicio no existe"
            )

        if (order.status !=
            com.example.agencia_de_vechiculos.model.ServiceOrderStatus.FINALIZADA
        ) {
            return MaintenanceResult.Error(
                "La orden todavía no ha finalizado"
            )
        }

        val mileage = order.mileageKm
            ?: return MaintenanceResult.Error(
                "La orden no tiene kilometraje registrado"
            )

        if (reminders.any { it.plate == order.plate &&
                    it.referenceDate == completedDate &&
                    it.referenceMileageKm == mileage &&
                    it.serviceName == order.serviceType.label }) {
            return MaintenanceResult.Error(
                "Ya existe un recordatorio para este mantenimiento"
            )
        }

        return create(
            plate = order.plate,
            serviceName = order.serviceType.label,
            referenceDate = completedDate,
            referenceMileageKm = mileage,
            intervalMonths = intervalMonths,
            intervalKm = intervalKm
        )
    }


    fun completeServiceAndSchedule(
        orderId: Int,
        completedDate: String,
        totalCost: Long = 0L,
        intervalMonths: Int = 6,
        intervalKm: Int = 5000
    ): MaintenanceResult {

        val order = serviceOrderRepository.allServiceOrders()
            .firstOrNull { it.id == orderId }
            ?: return MaintenanceResult.Error(
                "La orden de servicio no existe"
            )

        if (order.status != ServiceOrderStatus.EN_DIAGNOSTICO) {
            return MaintenanceResult.Error(
                "La orden debe estar en diagnóstico y tener el acta firmada"
            )
        }

        if (order.mileageKm == null) {
            return MaintenanceResult.Error(
                "La orden no tiene kilometraje registrado"
            )
        }

        if (parseDate(completedDate) == null) {
            return MaintenanceResult.Error(
                "La fecha de finalización no es válida"
            )
        }

        if (completedDate < order.openedDate) {
            return MaintenanceResult.Error(
                "La finalización no puede ser anterior a la apertura"
            )
        }

        if (intervalMonths <= 0 || intervalKm <= 0) {
            return MaintenanceResult.Error(
                "Los intervalos deben ser mayores que cero"
            )
        }
        val existingReminder = reminders.any {
            it.plate == order.plate &&
                    it.referenceDate == completedDate &&
                    it.referenceMileageKm == order.mileageKm &&
                    it.serviceName == order.serviceType.label
        }

        if (existingReminder) {
            return MaintenanceResult.Error(
                "Ya existe un recordatorio para este mantenimiento"
            )
        }

        val preview = parseDate(completedDate)
            ?: return MaintenanceResult.Error(
                "La fecha de finalización no es válida"
            )

        preview.add(Calendar.MONTH, intervalMonths)

        val dueMileage = order.mileageKm.toLong() + intervalKm.toLong()

        if (dueMileage > Int.MAX_VALUE) {
            return MaintenanceResult.Error(
                "El kilometraje del próximo mantenimiento supera el límite permitido"
            )
        }
        if (totalCost < 0L) {
            return MaintenanceResult.Error(
                "El costo del servicio no puede ser negativo"
            )
        }

        val completed = serviceOrderRepository.completeOrder(
            orderId = orderId,
            completedDate = completedDate,
            totalCost = totalCost
        )

        if (!completed) {
            return MaintenanceResult.Error(
                "No fue posible finalizar la orden"
            )
        }


        return createFromCompletedOrder(
            orderId = orderId,
            completedDate = completedDate,
            intervalMonths = intervalMonths,
            intervalKm = intervalKm
        )
    }


    fun delete(id: Int) {
        reminders.removeAll { it.id == id }
        sentAlerts.remove(id)
    }
}
