package com.example.agencia_de_vechiculos.ui.testdrive

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.data.sampleVehicles
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.TestDriveAppointment
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import com.example.agencia_de_vechiculos.model.TestDriveStatus
import com.example.agencia_de_vechiculos.model.Vehicle
import com.example.agencia_de_vechiculos.model.VehicleStatus

@Composable
fun TestDriveScreen(
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle> = remember {
        sampleVehicles.filter { it.status == VehicleStatus.DISPONIBLE || it.status == VehicleStatus.RESERVADO }
    },
    initialVehicleId: Int? = null,
    repository: TestDriveRepository = remember { TestDriveRepository() },
    onBackToCatalog: (() -> Unit)? = null,
) {
    var clientName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var selectedVehicleId by rememberSaveable {
        mutableIntStateOf(initialVehicleId ?: vehicles.firstOrNull()?.id ?: 1)
    }
    var selectedBranch by rememberSaveable { mutableStateOf(Branch.NORTE) }
    var date by rememberSaveable { mutableStateOf("2026-10-20") }
    var selectedTime by rememberSaveable { mutableStateOf("09:00") }

    var lastResult by remember { mutableStateOf<TestDriveBookingResult?>(null) }
    var appointments by remember { mutableStateOf(repository.allAppointments()) }

    val occupiedSlots = repository.occupiedSlots(selectedVehicleId, selectedBranch, date)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Agendar prueba de manejo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Duración: ${TestDriveAppointment.DURATION_MINUTES} min • Máx. ${TestDriveAppointment.MAX_ACTIVE_PER_CLIENT} pruebas activas por cliente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (onBackToCatalog != null) {
                    OutlinedButton(onClick = onBackToCatalog) {
                        Text("Ver catálogo")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Datos de la reserva",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Cliente (Nombre completo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() }.take(10) },
                        label = { Text("Teléfono de contacto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text("Vehículo a probar", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        vehicles.forEach { vehicle ->
                            FilterChip(
                                selected = selectedVehicleId == vehicle.id,
                                onClick = { selectedVehicleId = vehicle.id },
                                label = { Text("${vehicle.brand} ${vehicle.model} (${vehicle.year})") },
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
                        label = { Text("Fecha (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("2026-10-20", "2026-10-21", "2026-10-22").forEach { quickDate ->
                            AssistChip(
                                onClick = { date = quickDate },
                                label = { Text(quickDate) },
                            )
                        }
                    }

                    Text(
                        text = "Hora (bloques de 30 minutos)",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TestDriveRepository.TIME_SLOTS_30_MIN.forEach { slot ->
                            val isBusy = slot in occupiedSlots
                            FilterChip(
                                selected = selectedTime == slot,
                                onClick = { selectedTime = slot },
                                label = {
                                    Text(if (isBusy) "$slot (Ocupado)" else slot)
                                },
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val result = repository.schedule(
                                clientName = clientName,
                                phone = phone,
                                vehicleId = selectedVehicleId,
                                branch = selectedBranch,
                                date = date,
                                time = selectedTime,
                            )
                            lastResult = result
                            appointments = repository.allAppointments()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Confirmar cita de prueba")
                    }
                }
            }
        }

        lastResult?.let { result ->
            item {
                BookingFeedbackCard(
                    result = result,
                    onPickSuggestedSlot = { suggestedSlot ->
                        selectedTime = suggestedSlot
                        val retryResult = repository.schedule(
                            clientName = clientName,
                            phone = phone,
                            vehicleId = selectedVehicleId,
                            branch = selectedBranch,
                            date = date,
                            time = suggestedSlot,
                        )
                        lastResult = retryResult
                        appointments = repository.allAppointments()
                    },
                )
            }
        }

        item {
            Text(
                text = "Citas agendadas (${appointments.count { it.status == TestDriveStatus.ACTIVA }} activas)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        items(appointments.filter { it.status == TestDriveStatus.ACTIVA }, key = { it.id }) { appointment ->
            val vehicle = vehicles.find { it.id == appointment.vehicleId }
            AppointmentCard(
                appointment = appointment,
                vehicleTitle = vehicle?.let { "${it.brand} ${it.model} (${it.year})" } ?: "Vehículo #${appointment.vehicleId}",
                onCancel = {
                    repository.cancelAppointment(appointment.id)
                    appointments = repository.allAppointments()
                },
            )
        }
    }
}

@Composable
private fun BookingFeedbackCard(
    result: TestDriveBookingResult,
    onPickSuggestedSlot: (String) -> Unit,
) {
    when (result) {
        is TestDriveBookingResult.Confirmed -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "¡Cita confirmada!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = result.confirmationEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        is TestDriveBookingResult.SlotOccupied -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "El horario ${result.requestedTime} ya está ocupado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "Selecciona uno de estos horarios libres disponibles (30 min):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        result.suggestedTimes.forEach { slot ->
                            AssistChip(
                                onClick = { onPickSuggestedSlot(slot) },
                                label = { Text("Reservar $slot") },
                            )
                        }
                    }
                }
            }
        }

        is TestDriveBookingResult.MaxActiveReached -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Límite alcanzado: un cliente solo puede tener máximo ${result.maxAllowed} pruebas de manejo activas.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        is TestDriveBookingResult.InvalidData -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: TestDriveAppointment,
    vehicleTitle: String,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "$vehicleTitle • ${appointment.branch.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Cliente: ${appointment.clientName} (Tel: ${appointment.phone})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Fecha: ${appointment.date} a las ${appointment.time} (${appointment.durationMinutes} min)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            OutlinedButton(onClick = onCancel) {
                Text("Cancelar")
            }
        }
    }
}
