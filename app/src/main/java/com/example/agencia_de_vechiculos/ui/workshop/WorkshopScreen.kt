package com.example.agencia_de_vechiculos.ui.workshop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.ServiceOrderRepository
import com.example.agencia_de_vechiculos.data.WorkshopRepository
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.ServiceType
import com.example.agencia_de_vechiculos.model.WorkshopBookingResult
import com.example.agencia_de_vechiculos.model.WorkshopSlot
import com.example.agencia_de_vechiculos.ui.common.FeedbackCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 
 * la pantalla del taller: agendar citas (HU-26) y recibir los vehículos (HU-27).
 * Arriba se elige cuál de las dos se muestra
 * 
 * el cliente escribe la placa (o la elige de los vehículos registrados), el
 * servicio, la sucursal, la fecha y el horario. Los horarios sin cupo se marcan,
 * pero se pueden elegir para ver otras opciones. Al confirmar se muestra el
 * recordatorio o, si no hay cupo, las otras opciones, que agendan con un toque.
 * Debajo se ven las citas de ese día en esa sucursal
 * 
 */
@Composable
fun WorkshopScreen(
    modifier: Modifier = Modifier,
    repository: WorkshopRepository = remember { WorkshopRepository() },
    serviceOrderRepository: ServiceOrderRepository = remember { ServiceOrderRepository(repository) },
) {
    /**
     * 
     * la fecha de hoy (AAAA-MM-DD), para no dejar agendar en días que ya pasaron
     * 
     */
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    var plate by rememberSaveable { mutableStateOf("") }
    var selectedService by rememberSaveable { mutableStateOf(ServiceType.MANTENIMIENTO) }
    var selectedBranch by rememberSaveable { mutableStateOf(Branch.NORTE) }
    var date by rememberSaveable { mutableStateOf("2026-10-20") }
    var selectedTime by rememberSaveable { mutableStateOf(WorkshopRepository.TIME_SLOTS.first()) }
    var comments by rememberSaveable { mutableStateOf("") }

    /**
     * 
     * true cuando se muestra la recepción de vehículos en vez del formulario para
     * agendar
     * 
     */
    var showReception by rememberSaveable { mutableStateOf(false) }

    /**
     * 
     * el resultado del último intento de agendar
     * 
     */
    var lastResult by remember { mutableStateOf<WorkshopBookingResult?>(null) }

    /**
     * 
     * cambia cada vez que se intenta agendar, para que la pantalla vuelva a leer las
     * citas y los horarios libres
     * 
     */
    var version by remember { mutableIntStateOf(0) }

    /**
     * 
     * el vehículo registrado de la placa escrita, o null si no hay ninguno
     * 
     */
    val vehicle = repository.registeredVehicle(plate)

    /**
     * 
     * las citas y los horarios libres de la sucursal y el día elegidos
     * 
     */
    val dayAppointments = remember(selectedBranch, date, version) { repository.appointmentsFor(selectedBranch, date) }
    val availableSlots = remember(selectedBranch, date, version) { repository.availableSlots(selectedBranch, date) }

    /**
     * 
     * intenta agendar en un horario. También se usa al tocar una de las otras
     * opciones, por eso primero deja esa fecha y esa hora en el formulario
     * 
     */
    fun book(slot: WorkshopSlot) {
        date = slot.date
        selectedTime = slot.time
        lastResult = repository.schedule(plate, selectedService, selectedBranch, slot.date, slot.time, comments, today)
        version++
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = !showReception, onClick = { showReception = false }, label = { Text("Agendar cita") })
            FilterChip(selected = showReception, onClick = { showReception = true }, label = { Text("Recepción") })
        }

        if (showReception) {
            ReceptionScreen(repository = serviceOrderRepository, today = today)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = "Citas de taller",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Cupo de ${WorkshopRepository.DAILY_CAPACITY} citas por día en cada sucursal • Solo vehículos registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Agendar cita de servicio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )

                            OutlinedTextField(
                                value = plate,
                                onValueChange = { plate = it.uppercase().take(8) },
                                label = { Text("Placa del vehículo") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            when {
                                vehicle != null -> Text(
                                    text = "${vehicle.clientName} • ${vehicle.brand} ${vehicle.model} ${vehicle.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                plate.isNotBlank() -> Text(
                                    text = "Esta placa no está registrada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                repository.allRegisteredVehicles().forEach {
                                    AssistChip(
                                        onClick = { plate = it.plate },
                                        label = { Text("${it.plate} • ${it.brand} ${it.model}") },
                                    )
                                }
                            }

                            Text("Tipo de servicio", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ServiceType.entries.forEach { service ->
                                    FilterChip(
                                        selected = selectedService == service,
                                        onClick = { selectedService = service },
                                        label = { Text(service.label) },
                                    )
                                }
                            }

                            Text("Sucursal", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Branch.entries.forEach { branch ->
                                    FilterChip(
                                        selected = selectedBranch == branch,
                                        onClick = { selectedBranch = branch },
                                        label = { Text(branch.label) },
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = date,
                                onValueChange = { date = it },
                                label = { Text("Fecha de la cita (AAAA-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("2026-10-20", "2026-10-21", "2026-10-22").forEach { quickDate ->
                                    AssistChip(onClick = { date = quickDate }, label = { Text(quickDate) })
                                }
                            }

                            Text(
                                text = "Horario • ${dayAppointments.size} de ${WorkshopRepository.DAILY_CAPACITY} citas ese día",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                WorkshopRepository.TIME_SLOTS.forEach { slot ->
                                    FilterChip(
                                        selected = selectedTime == slot,
                                        onClick = { selectedTime = slot },
                                        label = { Text(if (slot in availableSlots) slot else "$slot • Sin cupo") },
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = comments,
                                onValueChange = { comments = it },
                                label = { Text("Comentarios (opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Button(
                                onClick = { book(WorkshopSlot(date, selectedTime)) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Confirmar cita")
                            }
                        }
                    }
                }

                lastResult?.let { result ->
                    item {
                        BookingResultCard(result = result, onPickAlternative = { book(it) })
                    }
                }

                item {
                    Text(
                        text = "Citas del ${date.trim()} en ${selectedBranch.label}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (dayAppointments.isEmpty()) {
                        Text("No hay citas agendadas ese día.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                items(dayAppointments.sortedBy { it.time }, key = { it.id }) { appointment ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "${appointment.plate} • ${appointment.clientName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(appointment.serviceType.label, style = MaterialTheme.typography.bodyMedium)
                            if (appointment.comments.isNotEmpty()) {
                                Text("Comentarios: ${appointment.comments}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "Fecha: ${appointment.date} a las ${appointment.time} • ${appointment.branch.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 
 * la tarjeta con el resultado de agendar: el recordatorio si salió bien, o el
 * problema en rojo. Cuando no hay cupo muestra las otras opciones como botones
 * 
 */
@Composable
private fun BookingResultCard(
    result: WorkshopBookingResult,
    onPickAlternative: (WorkshopSlot) -> Unit,
) {
    when (result) {
        is WorkshopBookingResult.Confirmed -> FeedbackCard(
            title = "¡Cita agendada!",
            message = result.reminder,
            isError = false,
        )
        is WorkshopBookingResult.NoCapacity -> FeedbackCard(
            title = "Sin cupo el ${result.requested.date} a las ${result.requested.time}",
            message = if (result.alternatives.isEmpty()) {
                "No hay horarios libres en los próximos días en esta sucursal."
            } else {
                "Selecciona uno de estos horarios libres disponibles:"
            },
            isError = true,
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                result.alternatives.forEach { slot ->
                    AssistChip(
                        onClick = { onPickAlternative(slot) },
                        label = { Text("Reservar ${slot.date} ${slot.time}") },
                    )
                }
            }
        }
        is WorkshopBookingResult.UnregisteredVehicle -> FeedbackCard(
            message = "La placa ${result.plate} no está registrada. Solo se agendan vehículos registrados.",
            isError = true,
        )
        is WorkshopBookingResult.InvalidData -> FeedbackCard(message = result.message, isError = true)
    }
}
