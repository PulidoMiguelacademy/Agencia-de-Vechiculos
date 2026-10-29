package com.example.agencia_de_vechiculos.ui.quotation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.PaymentMethod
import com.example.agencia_de_vechiculos.model.QuotationDraft
import java.text.NumberFormat
import java.util.Locale
import com.example.agencia_de_vechiculos.data.QuotationRepository
import com.example.agencia_de_vechiculos.data.QuotationResult
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import android.content.ActivityNotFoundException
import com.example.agencia_de_vechiculos.ui.quotation.QuotationPdfWriter
import com.example.agencia_de_vechiculos.ui.quotation.QuotationShare
import com.example.agencia_de_vechiculos.data.DeliveryChannel
import com.example.agencia_de_vechiculos.data.DeliveryStatus
import com.example.agencia_de_vechiculos.data.QuotationDeliveryRepository

private fun formatCOP(amount: Long): String {
    val formatter = NumberFormat.getIntegerInstance(
        Locale.forLanguageTag("es-CO")
    )

    return "$${formatter.format(amount)}"
}

@Composable
fun QuotationScreen(
    vehicleRepository: VehicleRepository,
    quotationRepository: QuotationRepository,
    deliveryRepository: QuotationDeliveryRepository
) {
    val vehicles = remember(vehicleRepository) {
        vehicleRepository.catalog()
    }

    var clientName by remember { mutableStateOf("") }
    var clientDocument by remember { mutableStateOf("") }
    var selectedVehicleId by remember { mutableStateOf<Int?>(null) }
    var requestId by rememberSaveable {
        mutableStateOf(UUID.randomUUID().toString())
    }

    var generatedFolio by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var errorMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var accessoriesText by remember { mutableStateOf("0") }
    var discountText by remember { mutableStateOf("0") }
    var paymentMethod by remember {
        mutableStateOf(PaymentMethod.CONTADO)
    }

    val selectedVehicle = vehicles.firstOrNull {
        it.id == selectedVehicleId
    }

    val accessories = accessoriesText.toLongOrNull()
    val discount = discountText.toIntOrNull()
    var pendingMessage by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val context = LocalContext.current
    val draft = if (
        selectedVehicle != null &&
        accessories != null &&
        discount != null
    ) {
        QuotationDraft(
            clientName = clientName,
            clientDocument = clientDocument,
            vehicleId = selectedVehicle.id,
            vehiclePrice = selectedVehicle.price,
            accessoriesPrice = accessories,
            discountPercent = discount,
            paymentMethod = paymentMethod
        )
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Generar cotización",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = clientName,
            onValueChange = { clientName = it },
            label = { Text("Nombre del cliente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = clientDocument,
            onValueChange = { clientDocument = it },
            label = { Text("Documento del cliente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Selecciona un vehículo")

        vehicles.forEach { vehicle ->
            Row {
                RadioButton(
                    selected = selectedVehicleId == vehicle.id,
                    onClick = { selectedVehicleId = vehicle.id }
                )

                Text(
                    text = "${vehicle.brand} ${vehicle.model} - ${formatCOP(vehicle.price)}",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        OutlinedTextField(
            value = accessoriesText,
            onValueChange = { accessoriesText = it },
            label = { Text("Valor de accesorios") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = discountText,
            onValueChange = { discountText = it },
            label = { Text("Descuento (%)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Forma de pago")

        PaymentMethod.entries.forEach { method ->
            Row {
                RadioButton(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method }
                )

                Text(
                    text = method.label,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        if (draft != null) {
            HorizontalDivider()

            Text(
                text = "Subtotal: ${formatCOP(draft.subtotal)}"
            )

            Text(
                text = "Descuento: ${formatCOP(draft.discountAmount)}"
            )

            Text(
                text = "Total: ${formatCOP(draft.total)}",
                style = MaterialTheme.typography.titleLarge
            )

            if (draft.requiresManagerApproval) {
                Text(
                    text = "Requiere aprobación del gerente",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Button(

            onClick = {
                val currentDraft = draft

                if (currentDraft != null && generatedFolio == null) {

                    errorMessage = null

                    val today = SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                    ).format(Date())

                    val result = quotationRepository.generateQuotation(
                        draft = currentDraft,
                        createdAt = today,
                        requestId = requestId
                    )

                    when (result) {

                        is QuotationResult.Success -> {
                            generatedFolio = result.quotation.folio
                            pendingMessage = null
                        }

                        is QuotationResult.PendingApproval -> {
                            pendingMessage = result.message
                            errorMessage = null
                        }

                        is QuotationResult.Error -> {
                            errorMessage = result.message
                        }
                    }
                }
            },
            enabled = draft != null &&
                    draft.hasValidClient &&
                    draft.hasValidPrices &&
                    draft.hasValidDiscount &&
                    pendingMessage == null &&
                    generatedFolio == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generar cotización")
        }

        pendingMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        generatedFolio?.let { folio ->

            val quotation = quotationRepository.quotationByFolio(folio)
            val detail = quotationRepository.detailByFolio(folio)
            var recipient by rememberSaveable(folio) {
                mutableStateOf("")
            }

            var selectedChannel by rememberSaveable(folio) {
                mutableStateOf(DeliveryChannel.CORREO)
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Cotización generada correctamente",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Folio: $folio")

                    quotation?.let {
                        Text("Cliente: ${it.clientName}")
                        Text("Total: ${formatCOP(it.finalPrice)}")
                        Text("Estado: ${it.status.label}")
                    }

                    detail?.let {
                        Text("Generada el: ${it.createdAt}")
                        Text("Válida hasta: ${it.expiresAt}")
                    }
                }
            }

            var contactAuthorized by rememberSaveable(folio) {
                mutableStateOf(false)
            }

            Checkbox(
                checked = contactAuthorized,
                onCheckedChange = { contactAuthorized = it }
            )

            Text(
                text = "El cliente autorizó recibir la cotización " +
                        "por un medio electrónico."
            )

            Text(
                "Medio de envío",
                style = MaterialTheme.typography.titleMedium
            )

            DeliveryChannel.entries.forEach { channel ->
                Row {
                    RadioButton(
                        selected = selectedChannel == channel,
                        onClick = { selectedChannel = channel }
                    )
                    Text(
                        text = channel.label,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = {
                    Text(
                        if (selectedChannel == DeliveryChannel.CORREO)
                            "Correo electrónico del cliente"
                        else
                            "Número de WhatsApp del cliente"
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )


            Button(
                    onClick = {
                        try {
                            val currentQuotation = quotation
                                ?: error("No se encontró la cotización")

                            val currentDetail = detail
                                ?: error("No se encontró el detalle")

                            val vehicle = vehicleRepository.vehicleById(
                                currentQuotation.vehicleId
                            ) ?: error("No se encontró el vehículo")

                            val pdf = QuotationPdfWriter.generate(
                                context = context,
                                quotation = currentQuotation,
                                detail = currentDetail,
                                vehicle = vehicle
                            )

                            QuotationShare.share(
                                context = context,
                                file = pdf
                            )

                            deliveryRepository.registerAttempt(
                                folio = folio,
                                recipient = recipient,
                                channel = selectedChannel,
                                status = DeliveryStatus.COMPARTIR_INICIADO
                            )

                            errorMessage = null

                        } catch (e: Exception) {
                            val message = e.message
                                ?: "No fue posible compartir el PDF"

                            deliveryRepository.registerAttempt(
                                folio = folio,
                                recipient = recipient,
                                channel = selectedChannel,
                                status = DeliveryStatus.ERROR,
                                errorMessage = message
                            )

                            errorMessage = message
                        }
                    },
                enabled = contactAuthorized &&
                        recipient.isNotBlank() &&
                        (selectedChannel != DeliveryChannel.CORREO ||
                                android.util.Patterns.EMAIL_ADDRESS
                                    .matcher(recipient.trim()).matches()) &&
                        quotation != null &&
                        detail != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compartir cotización en PDF")
            }


            val attempts = deliveryRepository.attemptsByFolio(folio)

            if (attempts.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    "Historial de intentos de envío",
                    style = MaterialTheme.typography.titleMedium
                )

                attempts.forEach { attempt ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Canal previsto: ${attempt.channel.label}")
                            Text("Destinatario: ${attempt.recipient}")
                            Text("Fecha: ${attempt.attemptedAt}")
                            Text("Estado: ${attempt.status.label}")

                            attempt.errorMessage?.let {
                                Text(
                                    it,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }


            OutlinedButton(
                onClick = {
                    clientName = ""
                    clientDocument = ""
                    selectedVehicleId = null
                    accessoriesText = "0"
                    discountText = "0"
                    paymentMethod = PaymentMethod.CONTADO
                    pendingMessage = null
                    generatedFolio = null
                    errorMessage = null
                    requestId = UUID.randomUUID().toString()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nueva cotización")
            }
        }

    }
}
