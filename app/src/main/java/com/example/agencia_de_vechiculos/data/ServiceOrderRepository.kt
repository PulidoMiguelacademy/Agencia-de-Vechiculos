package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.FuelLevel
import com.example.agencia_de_vechiculos.model.PreviousDamage
import com.example.agencia_de_vechiculos.model.ReceptionResult
import com.example.agencia_de_vechiculos.model.ServiceOrder
import com.example.agencia_de_vechiculos.model.ServiceOrderOpenResult
import com.example.agencia_de_vechiculos.model.ServiceOrderSignResult
import com.example.agencia_de_vechiculos.model.ServiceOrderStatus
import com.example.agencia_de_vechiculos.model.VehicleArea
import com.example.agencia_de_vechiculos.model.WorkshopAppointment

/**
 * 
 * guarda las órdenes de servicio del taller y su acta de recepción (HU-27)
 * 
 * usa el WorkshopRepository de las citas para saber qué citas hay hoy y el último
 * kilometraje de cada vehículo registrado. Por ahora todo vive en memoria
 * 
 */
class ServiceOrderRepository(
    private val workshopRepository: WorkshopRepository = WorkshopRepository(),
) {
    /**
     * 
     * las órdenes abiertas o ya en diagnóstico
     * 
     */
    private val orders = mutableListOf<ServiceOrder>()

    /**
     * 
     * el número que le toca a la próxima orden
     * 
     */
    private var nextId = 1

    /**
     * 
     * devuelve las citas de taller de hoy, ordenadas por hora
     * 
     */
    fun appointmentsOfDay(today: String): List<WorkshopAppointment> =
        workshopRepository.allAppointments().filter { it.date == today }.sortedBy { it.time }

    /**
     * 
     * devuelve la orden de una cita, o null si el vehículo todavía no ha llegado
     * 
     */
    fun orderForAppointment(appointmentId: Int): ServiceOrder? =
        orders.firstOrNull { it.appointmentId == appointmentId }

    /**
     * 
     * devuelve el último kilometraje registrado de un vehículo: el mayor entre el que
     * tiene guardado el vehículo y el de sus órdenes anteriores
     * 
     * exceptOrderId deja por fuera la orden que se está llenando, para que su propio
     * kilometraje no cuente como el anterior
     * 
     */
    fun lastMileage(plate: String, exceptOrderId: Int? = null): Int {
        val registered = workshopRepository.registeredVehicle(plate)?.lastMileageKm ?: 0
        val recorded = orders
            .filter { it.plate == plate && it.id != exceptOrderId }
            .mapNotNull { it.mileageKm }
            .maxOrNull() ?: 0
        return maxOf(registered, recorded)
    }

    /**
     * 
     * registra la llegada del vehículo de una cita y abre su orden de servicio
     * 
     * solo se puede con una cita de hoy. Si la cita ya tenía orden, devuelve esa misma
     * 
     */
    fun openOrder(appointmentId: Int, today: String): ServiceOrderOpenResult {
        val appointment = workshopRepository.allAppointments().firstOrNull { it.id == appointmentId }
            ?: return ServiceOrderOpenResult.InvalidData("La cita seleccionada no existe")

        if (appointment.date != today) {
            return ServiceOrderOpenResult.InvalidData(
                "Solo se registra la llegada de una cita del día. Esta cita es el ${appointment.date}"
            )
        }

        orderForAppointment(appointment.id)?.let { return ServiceOrderOpenResult.AlreadyOpened(it) }

        val order = ServiceOrder(
            id = nextId++,
            appointmentId = appointment.id,
            plate = appointment.plate,
            clientName = appointment.clientName,
            serviceType = appointment.serviceType,
            openedDate = today,
        )
        orders.add(order)
        return ServiceOrderOpenResult.Opened(order)
    }

    /**
     * 
     * guarda en el acta el kilometraje, el combustible, las pertenencias y lo que pide
     * el cliente
     * 
     * el kilometraje no puede ser menor que el último registrado del vehículo
     * 
     */
    fun recordReception(
        orderId: Int,
        mileageKm: Int?,
        fuelLevel: FuelLevel?,
        belongings: String,
        clientRequest: String,
    ): ReceptionResult {
        val index = editableIndex(orderId) ?: return ReceptionResult.InvalidData(NOT_EDITABLE_MESSAGE)
        val order = orders[index]

        if (mileageKm == null || mileageKm < 0) {
            return ReceptionResult.InvalidData("Ingresa el kilometraje del vehículo")
        }
        val last = lastMileage(order.plate, exceptOrderId = order.id)
        if (mileageKm < last) {
            return ReceptionResult.MileageBelowLast(last)
        }
        if (fuelLevel == null) {
            return ReceptionResult.InvalidData("Selecciona el nivel de combustible")
        }

        val updated = order.copy(
            mileageKm = mileageKm,
            fuelLevel = fuelLevel,
            belongings = belongings.trim(),
            clientRequest = clientRequest.trim(),
        )
        orders[index] = updated
        return ReceptionResult.Saved(updated)
    }

    /**
     * 
     * agrega un daño previo al acta. Solo se puede con al menos una foto del daño
     * 
     */
    fun addDamage(
        orderId: Int,
        area: VehicleArea,
        description: String,
        photos: List<String>,
    ): ReceptionResult {
        val index = editableIndex(orderId) ?: return ReceptionResult.InvalidData(NOT_EDITABLE_MESSAGE)

        if (photos.isEmpty()) {
            return ReceptionResult.InvalidData("Sube al menos una foto del daño en ${area.label.lowercase()}")
        }

        val damage = PreviousDamage(
            area = area,
            description = description.trim().ifEmpty { "Sin descripción" },
            photos = photos,
        )
        val updated = orders[index].copy(damages = orders[index].damages + damage)
        orders[index] = updated
        return ReceptionResult.Saved(updated)
    }

    /**
     * 
     * registra que el cliente firmó el acta de recepción
     * 
     * si al acta le faltan datos no se deja firmar y se dice cuáles. Al firmar, la
     * orden pasa a diagnóstico y ya se puede iniciar el trabajo
     * 
     */
    fun sign(orderId: Int, signedBy: String): ServiceOrderSignResult {
        val index = editableIndex(orderId) ?: return ServiceOrderSignResult.InvalidData(NOT_EDITABLE_MESSAGE)
        val order = orders[index]

        if (!order.isRecordComplete) {
            return ServiceOrderSignResult.IncompleteRecord(order.missingFields)
        }

        val cleanSigner = signedBy.trim()
        if (cleanSigner.isEmpty()) {
            return ServiceOrderSignResult.InvalidData("Escribe el nombre de quien firma el acta de recepción")
        }

        val signed = order.copy(signedBy = cleanSigner, status = ServiceOrderStatus.EN_DIAGNOSTICO)
        orders[index] = signed
        return ServiceOrderSignResult.Signed(signed)
    }

    /**
     * 
     * busca una orden que todavía se pueda cambiar, es decir, que siga abierta.
     * Devuelve su posición en la lista, o null si no existe o ya se firmó
     * 
     */
    private fun editableIndex(orderId: Int): Int? {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index == -1 || orders[index].status != ServiceOrderStatus.ABIERTA) return null
        return index
    }

    companion object {
        /**
         * 
         * el mensaje que se muestra al intentar cambiar una orden que no existe o que ya
         * está firmada
         * 
         */
        private const val NOT_EDITABLE_MESSAGE =
            "La orden no existe o el acta ya fue firmada: el vehículo ya está en diagnóstico"
    }
}
