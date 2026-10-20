package com.example.agencia_de_vechiculos.ui.workshop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.ServiceOrderRepository
import com.example.agencia_de_vechiculos.model.FuelLevel
import com.example.agencia_de_vechiculos.model.PreviousDamage
import com.example.agencia_de_vechiculos.model.ReceptionResult
import com.example.agencia_de_vechiculos.model.ServiceOrder
import com.example.agencia_de_vechiculos.model.ServiceOrderOpenResult
import com.example.agencia_de_vechiculos.model.ServiceOrderSignResult
import com.example.agencia_de_vechiculos.model.ServiceOrderStatus
import com.example.agencia_de_vechiculos.model.VehicleArea
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReceptionScreen(
    repository: ServiceOrderRepository,
    today: String,
    modifier: Modifier = Modifier,
) {
    var version by remember { mutableIntStateOf(0) }
    val appointments = remember(version) { repository.appointmentsOfDay(today) }
    var selectedAppointmentId by remember { mutableStateOf(appointments.firstOrNull()?.id) }
    var message by remember { mutableStateOf<ScreenMessage?>(null) }

    val appointment = appointments.firstOrNull { it.id == selectedAppointmentId }
    val order = remember(selectedAppointmentId, version) {
        selectedAppointmentId?.let { repository.orderForAppointment(it) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Recepción de vehículos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Citas de hoy ($today) • El kilometraje no puede ser menor al último registrado • Sin firma no se inicia el trabajo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (appointments.isEmpty()) {
            item {
                Text(
                    text = "No hay citas de taller para hoy. Agenda una en \"Agendar cita\" con la fecha $today.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            item {
                Text("Cita del día", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    appointments.forEach {
                        FilterChip(
                            selected = selectedAppointmentId == it.id,
                            onClick = {
                                selectedAppointmentId = it.id
                                message = null
                            },
                            label = { Text("${it.time} • ${it.plate} • ${it.clientName}") },
                        )
                    }
                }
            }
        }

        if (appointment != null && order == null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Cita #${appointment.id} • ${appointment.time} en ${appointment.branch.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("${appointment.plate} • ${appointment.clientName}")
                        Text(appointment.serviceType.label)
                        if (appointment.comments.isNotEmpty()) Text("Comentarios: ${appointment.comments}")
                        Button(
                            onClick = {
                                message = when (val result = repository.openOrder(appointment.id, today)) {
                                    is ServiceOrderOpenResult.Opened -> ScreenMessage(
                                        Section.LLEGADA,
                                        "Llegada registrada. Se abrió la orden de servicio N.º ${result.order.id}.",
                                    )
                                    is ServiceOrderOpenResult.AlreadyOpened -> ScreenMessage(
                                        Section.LLEGADA,
                                        "Esta cita ya tiene la orden N.º ${result.order.id}.",
                                        isError = true,
                                    )
                                    is ServiceOrderOpenResult.InvalidData ->
                                        ScreenMessage(Section.LLEGADA, result.message, isError = true)
                                }
                                version++
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Registrar llegada")
                        }
                    }
                }
            }
        }

        message?.takeIf { it.section == Section.LLEGADA || (it.section == Section.FIRMA && order?.canStartWork == true) }?.let {
            item { MessageText(it) }
        }

        if (order != null) {
            item {
                OrderHeader(order)
            }
            if (order.status == ServiceOrderStatus.ABIERTA) {
                item {
                    ReceptionForm(
                        order = order,
                        lastMileageKm = repository.lastMileage(order.plate, exceptOrderId = order.id),
                        message = message?.takeIf { it.section == Section.RECEPCION },
                        onSave = { mileage, fuel, belongings, request ->
                            message = when (val result = repository.recordReception(order.id, mileage, fuel, belongings, request)) {
                                is ReceptionResult.Saved ->
                                    ScreenMessage(Section.RECEPCION, "Datos de la recepción guardados en el acta.")
                                is ReceptionResult.MileageBelowLast -> ScreenMessage(
                                    Section.RECEPCION,
                                    "El kilometraje no puede ser menor al último registrado: " +
                                        "${formatNumber(result.lastMileageKm)} km.",
                                    isError = true,
                                )
                                is ReceptionResult.InvalidData ->
                                    ScreenMessage(Section.RECEPCION, result.message, isError = true)
                            }
                            version++
                        },
                    )
                }
                item {
                    DamageForm(
                        damages = order.damages,
                        message = message?.takeIf { it.section == Section.DANOS },
                        onAdd = { area, description, photos ->
                            val result = repository.addDamage(order.id, area, description, photos)
                            message = when (result) {
                                is ReceptionResult.Saved ->
                                    ScreenMessage(Section.DANOS, "Daño en ${area.label.lowercase()} agregado al acta.")
                                is ReceptionResult.MileageBelowLast -> null
                                is ReceptionResult.InvalidData -> ScreenMessage(Section.DANOS, result.message, isError = true)
                            }
                            version++
                            result is ReceptionResult.Saved
                        },
                    )
                }
                item {
                    SignatureForm(
                        clientName = order.clientName,
                        message = message?.takeIf { it.section == Section.FIRMA },
                        onSign = { signer ->
                            message = when (val result = repository.sign(order.id, signer)) {
                                is ServiceOrderSignResult.Signed -> ScreenMessage(
                                    Section.FIRMA,
                                    "Acta firmada. La orden pasó a \"${result.order.status.label}\".",
                                )
                                is ServiceOrderSignResult.IncompleteRecord -> ScreenMessage(
                                    Section.FIRMA,
                                    "El acta está incompleta. Falta: ${result.missingFields.joinToString(", ")}.",
                                    isError = true,
                                )
                                is ServiceOrderSignResult.InvalidData ->
                                    ScreenMessage(Section.FIRMA, result.message, isError = true)
                            }
                            version++
                        },
                    )
                }
            } else {
                item {
                    SignedRecord(order)
                }
            }
        }
    }
}

@Composable
private fun OrderHeader(order: ServiceOrder) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Orden de servicio N.º ${order.id} • ${order.status.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("${order.plate} • ${order.clientName}")
            Text("${order.serviceType.label} • Abierta el ${order.openedDate}")
            Text(
                text = if (order.canStartWork) "Ya se puede iniciar el trabajo" else "Sin firma del cliente no se inicia el trabajo",
                style = MaterialTheme.typography.bodySmall,
                color = if (order.canStartWork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReceptionForm(
    order: ServiceOrder,
    lastMileageKm: Int,
    message: ScreenMessage?,
    onSave: (Int?, FuelLevel?, String, String) -> Unit,
) {
    var mileage by remember(order.id) { mutableStateOf(order.mileageKm?.toString().orEmpty()) }
    var fuel by remember(order.id) { mutableStateOf(order.fuelLevel) }
    var belongings by remember(order.id) { mutableStateOf(order.belongings) }
    var request by remember(order.id) { mutableStateOf(order.clientRequest) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Acta de recepción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = mileage,
                onValueChange = { mileage = it.filter(Char::isDigit).take(7) },
                label = { Text("Kilometraje") },
                supportingText = { Text("Último registrado: ${formatNumber(lastMileageKm)} km") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Combustible", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FuelLevel.entries.forEach { level ->
                    FilterChip(selected = fuel == level, onClick = { fuel = level }, label = { Text(level.label) })
                }
            }
            OutlinedTextField(
                value = belongings,
                onValueChange = { belongings = it },
                label = { Text("Pertenencias en el vehículo (opcional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = request,
                onValueChange = { request = it },
                label = { Text("Solicitud del cliente") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { onSave(mileage.toIntOrNull(), fuel, belongings, request) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar recepción")
            }
            message?.let { MessageText(it) }
        }
    }
}

@Composable
private fun DamageForm(
    damages: List<PreviousDamage>,
    message: ScreenMessage?,
    onAdd: (VehicleArea, String, List<String>) -> Boolean,
) {
    var area by remember { mutableStateOf(VehicleArea.PARACHOQUES_DELANTERO) }
    var description by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf(listOf<String>()) }
    val pickPhotos = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS),
    ) { uris -> photos = (photos + uris.map(Uri::toString)).distinct().take(MAX_PHOTOS) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Daños previos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (damages.isEmpty()) {
                Text("Sin daños marcados.", style = MaterialTheme.typography.bodySmall)
            }
            damages.forEach { DamageItem(it) }

            HorizontalDivider()
            Text("Marcar un daño", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VehicleArea.entries.forEach {
                    FilterChip(selected = area == it, onClick = { area = it }, label = { Text(it.label) })
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción del daño") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (photos.isEmpty()) "Subir fotos" else "Subir más fotos (${photos.size} de $MAX_PHOTOS)")
            }
            if (photos.isNotEmpty()) PhotoRow(photos)
            Button(
                onClick = {
                    if (onAdd(area, description, photos)) {
                        description = ""
                        photos = emptyList()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Agregar daño al acta")
            }
            message?.let { MessageText(it) }
        }
    }
}

@Composable
private fun SignatureForm(
    clientName: String,
    message: ScreenMessage?,
    onSign: (String) -> Unit,
) {
    var signer by remember { mutableStateOf(clientName) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Firma del cliente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = signer,
                onValueChange = { signer = it },
                label = { Text("Nombre de quien firma el acta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = { onSign(signer) }, modifier = Modifier.fillMaxWidth()) {
                Text("Firmar acta de recepción")
            }
            message?.let { MessageText(it) }
        }
    }
}

@Composable
private fun SignedRecord(order: ServiceOrder) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Acta de recepción firmada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Kilometraje: ${order.mileageKm?.let { formatNumber(it) } ?: "-"} km")
            Text("Combustible: ${order.fuelLevel?.label ?: "-"}")
            Text("Pertenencias: ${order.belongings.ifEmpty { "Ninguna" }}")
            Text("Solicitud del cliente: ${order.clientRequest}")
            Text("Daños previos: ${if (order.damages.isEmpty()) "ninguno" else order.damages.size}")
            order.damages.forEach { DamageItem(it) }
            Text("Firmada por ${order.signedBy}", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DamageItem(damage: PreviousDamage) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${damage.area.label}: ${damage.description}", fontWeight = FontWeight.SemiBold)
        PhotoRow(damage.photos)
    }
}

@Composable
private fun PhotoRow(photos: List<String>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photos.forEach { PhotoThumbnail(it) }
    }
}

@Composable
private fun PhotoThumbnail(uri: String) {
    val context = LocalContext.current
    val bitmap = remember(uri) { loadThumbnail(context.contentResolver, Uri.parse(uri)) }
    Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Foto del daño",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp),
            )
        } else {
            Text("Foto", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun loadThumbnail(resolver: android.content.ContentResolver, uri: Uri): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMBNAIL_PX && bounds.outHeight / (sample * 2) >= THUMBNAIL_PX) {
        sample *= 2
    }
    resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
} catch (e: Exception) {
    null
}

@Composable
private fun MessageText(message: ScreenMessage) {
    Text(
        text = message.text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
    )
}

private enum class Section { LLEGADA, RECEPCION, DANOS, FIRMA }

private data class ScreenMessage(val section: Section, val text: String, val isError: Boolean = false)

private const val MAX_PHOTOS = 5
private const val THUMBNAIL_PX = 200

private fun formatNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)
