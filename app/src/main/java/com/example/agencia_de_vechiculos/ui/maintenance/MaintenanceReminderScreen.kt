
package com.example.agencia_de_vechiculos.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.MaintenanceReminderRepository
import com.example.agencia_de_vechiculos.data.MaintenanceResult
import com.example.agencia_de_vechiculos.data.WorkshopRepository
import com.example.agencia_de_vechiculos.model.MaintenanceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.agencia_de_vechiculos.data.ServiceOrderRepository
import com.example.agencia_de_vechiculos.model.ServiceOrderStatus

@Composable
fun MaintenanceReminderScreen(
    repository: MaintenanceReminderRepository,
    workshopRepository: WorkshopRepository,
    serviceOrderRepository: ServiceOrderRepository,
    onScheduleAppointment: () -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var serviceName by remember { mutableStateOf("Cambio de aceite") }
    var referenceDate by remember { mutableStateOf("") }
    var referenceMileage by remember { mutableStateOf("") }
    var intervalMonths by remember { mutableStateOf("6") }
    var intervalKm by remember { mutableStateOf("5000") }
    var message by remember { mutableStateOf("") }
    var serviceCosts by remember {
        mutableStateOf(mapOf<Int, String>())
    }

    val today = SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(Date())

    val reminders = repository.allReminders()
    val pendingOrders = serviceOrderRepository.allServiceOrders()
        .filter { it.status == ServiceOrderStatus.EN_DIAGNOSTICO }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Recordatorios de mantenimiento",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            "Registra el próximo mantenimiento por fecha " +
                    "o kilometraje, lo que ocurra primero."
        )

        Text(
            "Vehículos registrados: " +
                    workshopRepository.allRegisteredVehicles()
                        .joinToString { it.plate }
        )

        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it },
            label = { Text("Placa") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = serviceName,
            onValueChange = { serviceName = it },
            label = { Text("Tipo de mantenimiento") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = referenceDate,
            onValueChange = { referenceDate = it },
            label = { Text("Fecha de último mantenimiento") },
            placeholder = { Text("2026-10-29") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = referenceMileage,
            onValueChange = { referenceMileage = it },
            label = { Text("Kilometraje del último mantenimiento") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = intervalMonths,
            onValueChange = { intervalMonths = it },
            label = { Text("Intervalo en meses") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = intervalKm,
            onValueChange = { intervalKm = it },
            label = { Text("Intervalo en kilómetros") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                val km = referenceMileage.toIntOrNull()
                val months = intervalMonths.toIntOrNull()
                val kmInterval = intervalKm.toIntOrNull()

                if (km == null || months == null || kmInterval == null) {
                    message = "Ingresa valores numéricos válidos"
                } else {
                    when (
                        val result = repository.create(
                            plate = plate,
                            serviceName = serviceName,
                            referenceDate = referenceDate,
                            referenceMileageKm = km,
                            intervalMonths = months,
                            intervalKm = kmInterval
                        )
                    ) {
                        is MaintenanceResult.Success -> {
                            message = "Recordatorio registrado correctamente"
                            plate = ""
                            referenceDate = ""
                            referenceMileage = ""
                        }

                        is MaintenanceResult.Error -> {
                            message = result.message
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar recordatorio")
        }

        if (message.isNotBlank()) {
            Text(
                message,
                color = MaterialTheme.colorScheme.primary
            )
        }


        Text(
            "Finalizar servicios",
            style = MaterialTheme.typography.titleMedium
        )

        if (pendingOrders.isEmpty()) {
            Text("No hay órdenes pendientes de finalizar.")
        }

        pendingOrders.forEach { order ->

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Orden #${order.id}")
                    Text("Vehículo: ${order.plate}")
                    Text("Servicio: ${order.serviceType.label}")
                    Text("Kilometraje: ${order.mileageKm ?: 0} km")
                    OutlinedTextField(
                        value = serviceCosts[order.id] ?: "",
                        onValueChange = { value ->
                            serviceCosts = serviceCosts + (order.id to value)
                        },
                        label = { Text("Costo total del servicio (COP)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val cost = serviceCosts[order.id]?.toLongOrNull()
                            when (
                                val result = if (cost == null || cost < 0L) {
                                    MaintenanceResult.Error(
                                        "Ingresa un costo válido, igual o mayor que cero"
                                    )
                                } else {
                                    repository.completeServiceAndSchedule(
                                        orderId = order.id,
                                        completedDate = today,
                                        totalCost = cost
                                    )
                                }
                            ) {
                                is MaintenanceResult.Success -> {
                                    message =
                                        "Orden #${order.id} finalizada. " +
                                                "Próximo mantenimiento calculado."
                                }

                                is MaintenanceResult.Error -> {
                                    message = result.message
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finalizar servicio y programar mantenimiento")
                    }
                }
            }
        }


        HorizontalDivider()

        Text(
            "Recordatorios registrados (${reminders.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (reminders.isEmpty()) {
            Text("Todavía no hay recordatorios registrados.")
        }

        reminders.forEach { reminder ->
            val evaluation = repository.evaluate(reminder, today)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "${reminder.plate} - ${reminder.serviceName}",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Próxima fecha: ${reminder.dueDate}")
                    Text("Próximo kilometraje: ${reminder.dueMileageKm} km")
                    Text("Kilometraje registrado: ${evaluation.currentMileageKm} km")

                    Text(
                        "Estado: ${evaluation.status.label}",
                        color = when (evaluation.status) {
                            MaintenanceStatus.VENCIDO ->
                                MaterialTheme.colorScheme.error
                            MaintenanceStatus.PROXIMO ->
                                MaterialTheme.colorScheme.tertiary
                            MaintenanceStatus.AL_DIA ->
                                MaterialTheme.colorScheme.primary
                        }
                    )

                    Text(evaluation.message)


                    if (evaluation.status != MaintenanceStatus.AL_DIA) {

                        Text(
                            "Avisos registrados: " +
                                    "${repository.alertsSent(reminder.id)}/2"
                        )

                        Button(
                            onClick = {
                                val registered = repository.registerAlert(reminder.id)

                                message = if (registered) {
                                    "Aviso registrado correctamente"
                                } else {
                                    "Este mantenimiento ya alcanzó el máximo de 2 avisos"
                                }
                            },
                            enabled = repository.canSendAlert(reminder.id),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Registrar aviso")
                        }
                        OutlinedButton(
                            onClick = onScheduleAppointment,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Agendar mantenimiento en Taller")
                        }
                    }


                    OutlinedButton(
                        onClick = {
                            repository.delete(reminder.id)
                        }
                    ) {
                        Text("Eliminar recordatorio")
                    }
                }
            }
        }
    }
}
