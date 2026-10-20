package com.example.agencia_de_vechiculos.ui.contract

import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.example.agencia_de_vechiculos.data.ContractPdfWriter
import com.example.agencia_de_vechiculos.data.ContractRepository
import com.example.agencia_de_vechiculos.data.DeliveryRepository
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.ContractGenerationResult
import com.example.agencia_de_vechiculos.model.ContractSignResult
import com.example.agencia_de_vechiculos.model.ContractStatus
import com.example.agencia_de_vechiculos.model.DocumentType
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationStatus
import com.example.agencia_de_vechiculos.model.SaleContract
import com.example.agencia_de_vechiculos.ui.common.FeedbackCard
import com.example.agencia_de_vechiculos.ui.delivery.DeliverySection
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 
 * la pantalla de contratos de venta (HU-24) y de la entrega del vehículo (HU-25)
 * 
 * el asesor elige una cotización y ve sus datos y su expediente. Desde aquí genera
 * el contrato con su PDF, registra la firma (y el vehículo pasa a vendido) y puede
 * ver el PDF. Cuando el contrato está firmado aparece la entrega del vehículo,
 * donde se hacen los trámites y se cierra la venta. Cada acción muestra un
 * mensaje: en el color principal si salió bien y en rojo si no
 * 
 */
@Composable
fun ContractScreen(
    modifier: Modifier = Modifier,
    vehicleRepository: VehicleRepository = remember { VehicleRepository() },
    repository: ContractRepository = remember { ContractRepository(vehicleRepository = vehicleRepository) },
    deliveryRepository: DeliveryRepository = remember { DeliveryRepository(repository) },
) {
    val context = LocalContext.current

    /**
     * 
     * guarda los PDF en la carpeta contratos, dentro de los archivos privados de la app
     * 
     */
    val pdfWriter = remember { ContractPdfWriter(File(context.filesDir, "contratos")) }

    /**
     * 
     * la fecha de hoy como AAAA-MM-DD. Se usa como fecha de emisión del PDF y como
     * fecha de firma propuesta
     * 
     */
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val quotations = remember { repository.allQuotations() }
    var selectedFolio by rememberSaveable { mutableStateOf(quotations.first().folio) }
    var signedDate by rememberSaveable { mutableStateOf(today) }

    /**
     * 
     * cambia cada vez que se genera o se firma un contrato, para que la pantalla vuelva
     * a leer el contrato del repositorio
     * 
     */
    var version by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<ScreenMessage?>(null) }

    /**
     * 
     * el último PDF creado de cada cotización
     * 
     */
    var pdfFiles by remember { mutableStateOf(mapOf<String, File>()) }

    /**
     * 
     * la cotización cuyo PDF se está mostrando, o null si no se muestra ninguno
     * 
     */
    var previewFolio by remember { mutableStateOf<String?>(null) }

    val quotation = quotations.first { it.folio == selectedFolio }
    val contract = remember(selectedFolio, version) { repository.contractFor(selectedFolio) }

    /**
     * 
     * la entrega del vehículo de la cotización elegida. Solo existe cuando el contrato
     * está firmado
     * 
     */
    val delivery = remember(selectedFolio, version) { deliveryRepository.deliveryFor(selectedFolio) }
    val pdfFile = pdfFiles[selectedFolio]

    /**
     * 
     * crea (o vuelve a crear) el PDF de un contrato y lo guarda en pdfFiles
     * 
     */
    fun writePdf(saleContract: SaleContract) {
        val vehicle = vehicleRepository.vehicleById(saleContract.vehicleId) ?: return
        pdfFiles = pdfFiles + (saleContract.quotationFolio to pdfWriter.write(saleContract, vehicle, today))
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Contratos de venta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Un contrato por cotización aceptada • El anticipo de la reserva se descuenta del precio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text("Cotización", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quotations.forEach {
                    FilterChip(
                        selected = selectedFolio == it.folio,
                        onClick = {
                            selectedFolio = it.folio
                            message = null
                            previewFolio = null
                        },
                        label = { Text("${it.folio} • ${it.clientName}") },
                    )
                }
            }
        }

        item {
            QuotationCard(quotation, repository, vehicleRepository)
        }

        item {
            Button(
                onClick = {
                    message = when (val result = repository.generate(selectedFolio)) {
                        is ContractGenerationResult.Generated -> {
                            writePdf(result.contract)
                            ScreenMessage("Contrato N.º ${result.contract.id} generado con su PDF.")
                        }
                        is ContractGenerationResult.AlreadyGenerated -> ScreenMessage(
                            "La cotización ${result.contract.quotationFolio} ya tiene el contrato N.º ${result.contract.id}.",
                            isError = true,
                        )
                        is ContractGenerationResult.IncompleteFile -> ScreenMessage(
                            "El expediente está incompleto. Falta validar: " +
                                result.missingDocuments.joinToString(", ") { it.label } + ".",
                            isError = true,
                        )
                        is ContractGenerationResult.InvalidData -> ScreenMessage(result.message, isError = true)
                    }
                    version++
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generar contrato")
            }
        }

        message?.let {
            item {
                FeedbackCard(message = it.text, isError = it.isError)
            }
        }

        if (contract != null) {
            item {
                ContractCard(contract)
            }

            if (contract.status == ContractStatus.GENERADO) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Registrar contrato firmado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            OutlinedTextField(
                                value = signedDate,
                                onValueChange = { signedDate = it },
                                label = { Text("Fecha de firma (AAAA-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = {
                                    message = when (val result = repository.registerSigned(selectedFolio, signedDate)) {
                                        is ContractSignResult.Signed -> {
                                            writePdf(result.contract)
                                            ScreenMessage(
                                                "Contrato firmado. El vehículo ${result.vehicle.brand} ${result.vehicle.model} pasó a “Vendido”."
                                            )
                                        }
                                        is ContractSignResult.InvalidData -> ScreenMessage(result.message, isError = true)
                                    }
                                    version++
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Registrar firma")
                            }
                        }
                    }
                }
            }

            if (delivery != null) {
                item {
                    DeliverySection(
                        delivery = delivery,
                        clientName = contract.clientName,
                        today = today,
                        repository = deliveryRepository,
                        onChange = { version++ },
                    )
                }
            }

            if (pdfFile != null) {
                item {
                    OutlinedButton(
                        onClick = { previewFolio = if (previewFolio == selectedFolio) null else selectedFolio },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (previewFolio == selectedFolio) "Ocultar PDF" else "Ver PDF")
                    }
                }
                if (previewFolio == selectedFolio) {
                    item {
                        PdfPreview(pdfFile)
                    }
                }
            }
        }
    }
}

/**
 * 
 * la tarjeta con los datos de la cotización elegida: estado, cliente, vehículo con
 * su VIN, precio, forma de pago, anticipo y cómo va cada documento del expediente
 * 
 * los documentos que faltan salen en rojo, igual que la cotización si no está
 * aceptada
 * 
 */
@Composable
private fun QuotationCard(
    quotation: Quotation,
    repository: ContractRepository,
    vehicleRepository: VehicleRepository,
) {
    val vehicle = vehicleRepository.vehicleById(quotation.vehicleId)
    val customerFile = repository.customerFileFor(quotation.clientDocument)
    val reservation = repository.reservationFor(quotation.vehicleId, quotation.clientDocument)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Cotización ${quotation.folio}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Estado: ${quotation.status.label}",
                color = if (quotation.status == QuotationStatus.ACEPTADA) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
            Text("Cliente: ${quotation.clientName} • Doc. ${quotation.clientDocument}")
            if (vehicle != null) {
                Text("Vehículo: ${vehicle.brand} ${vehicle.model} ${vehicle.year}")
                Text("VIN: ${vehicle.vin}")
            }
            Text("Precio final: ${formatPrice(quotation.finalPrice)} • ${quotation.paymentMethod.label}")
            Text("Anticipo de la reserva: ${formatPrice(reservation?.depositAmount ?: 0)}")

            Text("Expediente", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
            DocumentType.entries.forEach { type ->
                val status = customerFile?.documents?.get(type)
                Text(
                    text = "${type.label}: ${status?.label ?: "Sin cargar"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (customerFile?.missingDocuments?.contains(type) == false) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    }
}

/**
 * 
 * la tarjeta con los datos del contrato generado, con el saldo a pagar en negrita
 * 
 */
@Composable
private fun ContractCard(contract: SaleContract) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Contrato N.º ${contract.id} • ${contract.status.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Cliente: ${contract.clientName}")
            Text("VIN: ${contract.vin}")
            Text("Precio final: ${formatPrice(contract.finalPrice)}")
            Text("Anticipo: ${formatPrice(contract.deposit)}")
            Text(
                text = "Saldo a pagar: ${formatPrice(contract.balance)}",
                fontWeight = FontWeight.Bold,
            )
            Text("Forma de pago: ${contract.paymentMethod.label}")
            Text("Fecha de firma: ${contract.signedDate ?: "Pendiente"}")
        }
    }
}

/**
 * 
 * muestra la primera hoja del PDF como una imagen dentro de la app
 * 
 * usa PdfRenderer, que ya viene en Android, para convertir la hoja en imagen al
 * doble de tamaño y que se lea bien. Vuelve a dibujarla si el archivo cambia, por
 * ejemplo al registrar la firma
 * 
 */
@Composable
private fun PdfPreview(file: File) {
    val bitmap = remember(file, file.lastModified()) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { page ->
                    createBitmap(page.width * 2, page.height * 2).apply {
                        eraseColor(Color.WHITE)
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Vista previa del contrato en PDF",
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 
 * el mensaje que se muestra después de una acción. isError lo pinta de rojo
 * 
 */
private data class ScreenMessage(val text: String, val isError: Boolean = false)

/**
 * 
 * escribe un precio como en el catálogo, con puntos de miles: $ 59.500.000
 * 
 */
private fun formatPrice(value: Long): String =
    "$ ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)}"
