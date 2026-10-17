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
import com.example.agencia_de_vechiculos.model.InterestLevel
import com.example.agencia_de_vechiculos.model.TestDriveAppointment
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import com.example.agencia_de_vechiculos.model.TestDriveCompleteResult
import com.example.agencia_de_vechiculos.model.TestDriveOutcome
import com.example.agencia_de_vechiculos.model.TestDriveStartResult
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

    var lastBookingResult by remember { mutableStateOf<TestDriveBookingResult?>(null) }
    var appointments by remember { mutableStateOf(repository.allAppointments()) }
    var outcomes by remember { mutableStateOf(repository.allOutcomes()) }

    val activeOrInProgress = appointments.filter {
        it.status == TestDriveStatus.ACTIVA || it.status == TestDriveStatus.EN_CURSO
    }
    var selectedAppointmentId by rememberSaveable {
        mutableIntStateOf(activeOrInProgress.firstOrNull()?.id ?: 1)
    }
    var licenseNumber by rememberSaveable { mutableStateOf("") }
    var licenseExpiration by rememberSaveable { mutableStateOf("2028-12-31") }
    var distanceKmText by rememberSaveable { mutableStateOf("") }
    var selectedInterest by rememberSaveable { mutableStateOf(InterestLevel.ALTO) }
    var observations by rememberSaveable { mutableStateOf("") }

    var lastStartResult by remember { mutableStateOf<TestDriveStartResult?>(null) }
    var lastCompleteResult by remember { mutableStateOf<TestDriveCompleteResult?>(null) }

    val occupiedSlots = repository.occupiedSlots(selectedVehicleId, selectedBranch, date)
    val selectedAppointment = appointments.find { it.id == selectedAppointmentId }

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
                        text = "Pruebas de manejo",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Duración: ${TestDriveAppointment.DURATION_MINUTES} min • Máx. ${TestDriveAppointment.MAX_ACTIVE_PER_CLIENT} activas • Licencia vigente obligatoria",
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
                        text = "Agendar cita de prueba",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del cliente") },
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
                                label = { Text("${vehicle.brand} ${vehicle.model} ${vehicle.year}") },
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
                        label = { Text("Fecha de la cita") },
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
                        text = "Horario disponible",
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
                                    Text(if (isBusy) "$slot • Ocupado" else slot)
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
                            lastBookingResult = result
                            appointments = repository.allAppointments()
                            if (result is TestDriveBookingResult.Confirmed) {
                                selectedAppointmentId = result.appointment.id
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Confirmar cita de prueba")
                    }
                }
            }
        }

        lastBookingResult?.let { result ->
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
                        lastBookingResult = retryResult
                        appointments = repository.allAppointments()
                        if (retryResult is TestDriveBookingResult.Confirmed) {
                            selectedAppointmentId = retryResult.appointment.id
                        }
                    },
                )
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Registrar resultado de la prueba",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Valida la licencia vigente del cliente antes de iniciar la prueba y registra el resultado al finalizar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text("Cita seleccionada", style = MaterialTheme.typography.labelLarge)
                    if (activeOrInProgress.isEmpty()) {
                        Text(
                            text = "No hay citas pendientes en este momento.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            activeOrInProgress.forEach { appt ->
                                FilterChip(
                                    selected = selectedAppointmentId == appt.id,
                                    onClick = {
                                        selectedAppointmentId = appt.id
                                        lastStartResult = null
                                        lastCompleteResult = null
                                    },
                                    label = {
                                        Text("${appt.clientName} • ${appt.date} ${appt.time} • ${appt.status.label}")
                                    },
                                )
                            }
                        }
                    }

                    Text("Validación de licencia", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("Número de licencia") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = licenseExpiration,
                            onValueChange = { licenseExpiration = it },
                            label = { Text("Fecha de vigencia") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = {
                                licenseNumber = if (licenseNumber.isBlank()) "LIC-10293847" else licenseNumber
                                licenseExpiration = "2028-12-31"
                            },
                            label = { Text("Cargar licencia vigente") },
                        )
                        AssistChip(
                            onClick = {
                                licenseNumber = if (licenseNumber.isBlank()) "LIC-99887766" else licenseNumber
                                licenseExpiration = "2024-05-10"
                            },
                            label = { Text("Cargar licencia vencida") },
                        )
                    }

                    Button(
                        onClick = {
                            val result = repository.startTestDrive(
                                appointmentId = selectedAppointmentId,
                                licenseNumber = licenseNumber,
                                licenseExpirationDate = licenseExpiration,
                            )
                            lastStartResult = result
                            lastCompleteResult = null
                            appointments = repository.allAppointments()
                        },
                        enabled = activeOrInProgress.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Validar licencia e iniciar prueba")
                    }

                    lastStartResult?.let { startRes ->
                        StartResultFeedback(startRes)
                    }

                    Text("Evaluación final de la prueba", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Kilometraje recorrido en km") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text("Nivel de interés del cliente", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InterestLevel.entries.forEach { level ->
                            FilterChip(
                                selected = selectedInterest == level,
                                onClick = { selectedInterest = level },
                                label = { Text("Interés ${level.label}") },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = observations,
                        onValueChange = { observations = it },
                        label = { Text("Observaciones de la prueba") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val canComplete = selectedAppointment?.status == TestDriveStatus.EN_CURSO
                    Button(
                        onClick = {
                            val km = distanceKmText.toIntOrNull() ?: 0
                            val result = repository.completeTestDrive(
                                appointmentId = selectedAppointmentId,
                                distanceKm = km,
                                interestLevel = selectedInterest,
                                observations = observations,
                            )
                            lastCompleteResult = result
                            appointments = repository.allAppointments()
                            outcomes = repository.allOutcomes()
                            if (result is TestDriveCompleteResult.Completed) {
                                distanceKmText = ""
                                observations = ""
                                val nextActive = appointments.firstOrNull {
                                    it.status == TestDriveStatus.ACTIVA || it.status == TestDriveStatus.EN_CURSO
                                }
                                if (nextActive != null) {
                                    selectedAppointmentId = nextActive.id
                                }
                            }
                        },
                        enabled = canComplete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (canComplete) {
                                "Registrar resultado y programar seguimiento"
                            } else {
                                "Inicia la prueba con licencia vigente para habilitar"
                            }
                        )
                    }

                    lastCompleteResult?.let { completeRes ->
                        CompleteResultFeedback(completeRes)
                    }
                }
            }
        }

        if (outcomes.isNotEmpty()) {
            item {
                Text(
                    text = "Resultados y seguimientos programados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            items(outcomes, key = { it.appointmentId }) { outcome ->
                val appt = appointments.find { it.id == outcome.appointmentId }
                val vehicle = vehicles.find { it.id == appt?.vehicleId }
                OutcomeCard(
                    outcome = outcome,
                    clientName = appt?.clientName ?: "Cliente",
                    vehicleTitle = vehicle?.let { "${it.brand} ${it.model} ${it.year}" } ?: "Vehículo",
                )
            }
        }

        item {
            Text(
                text = "Citas agendadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        items(activeOrInProgress, key = { it.id }) { appointment ->
            val vehicle = vehicles.find { it.id == appointment.vehicleId }
            AppointmentCard(
                appointment = appointment,
                vehicleTitle = vehicle?.let { "${it.brand} ${it.model} ${it.year}" } ?: "Vehículo #${appointment.vehicleId}",
                onSelectForResult = {
                    selectedAppointmentId = appointment.id
                },
                onCancel = {
                    repository.cancelAppointment(appointment.id)
                    appointments = repository.allAppointments()
                },
            )
        }
    }
}

@Composable
private fun StartResultFeedback(result: TestDriveStartResult) {
    when (result) {
        is TestDriveStartResult.Started -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Licencia vigente validada — Prueba en curso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Cita de ${result.appointment.clientName} iniciada con licencia ${result.appointment.licenseNumber} vigente hasta ${result.appointment.licenseExpirationDate}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        is TestDriveStartResult.LicenseExpired -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "No se permite iniciar la prueba por licencia vencida",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        is TestDriveStartResult.InvalidStartData -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteResultFeedback(result: TestDriveCompleteResult) {
    when (result) {
        is TestDriveCompleteResult.Completed -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "¡Resultado registrado y seguimiento programado!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = result.outcome.scheduledFollowUp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        is TestDriveCompleteResult.LicenseNotValidated -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        is TestDriveCompleteResult.InvalidOutcomeData -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    outcome: TestDriveOutcome,
    clientName: String,
    vehicleTitle: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$clientName • $vehicleTitle",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Interés: ${outcome.interestLevel.label}") },
                )
            }
            Text(
                text = "Licencia: ${outcome.licenseNumber} • Vigente hasta ${outcome.licenseExpirationDate} • Recorrido: ${outcome.distanceKm} km",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Observaciones: ${outcome.observations}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = outcome.scheduledFollowUp,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
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
                        text = "Selecciona uno de estos horarios libres disponibles:",
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
    onSelectForResult: () -> Unit,
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
                    text = "Cliente: ${appointment.clientName} • Tel: ${appointment.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Fecha: ${appointment.date} a las ${appointment.time} • ${appointment.durationMinutes} min • Estado: ${appointment.status.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AssistChip(
                    onClick = onSelectForResult,
                    label = { Text("Seleccionar") },
                )
                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
        }
    }
}
