
package com.example.agencia_de_vechiculos.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.ServiceOrderRepository
import androidx.compose.material3.Button
import androidx.compose.ui.platform.LocalContext
import com.example.agencia_de_vechiculos.ui.quotation.QuotationShare
import java.util.Locale

@Composable
fun ServiceHistoryScreen(
    repository: ServiceOrderRepository
) {
    var plate by remember { mutableStateOf("") }

    val history = repository.serviceHistory(plate)

    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Historial de servicios",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it },
            label = { Text("Placa del vehículo") },
            placeholder = { Text("Ej: ABC123") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = "Órdenes encontradas: ${history.size}"
        )

        if (plate.isNotBlank() && history.isEmpty()) {
            Text(
                text = "No se encontraron órdenes para esta placa."
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history, key = { it.id }) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Orden #${order.id}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text("Placa: ${order.plate}")
                        Text("Cliente: ${order.clientName}")
                        Text("Servicio: ${order.serviceType}")
                        Text("Fecha: ${order.openedDate}")
                        Text("Estado: ${order.status}")
                        Text(
                            "Kilometraje: " +
                                    (order.mileageKm?.let { "$it km" }
                                        ?: "No registrado")
                        )
                        Text(
                            "Costo: " +
                                    (order.totalCost?.let {
                                        "$" + java.text.NumberFormat.getIntegerInstance(
                                            Locale.forLanguageTag("es-CO")
                                        ).format(it)
                                    } ?: "Pendiente")
                        )


                        if (order.clientRequest.isNotBlank()) {
                            Text("Solicitud: ${order.clientRequest}")
                        }

                        Button(
                            onClick = {
                                try {
                                    val pdf = ServiceOrderPdfWriter.generate(
                                        context = context,
                                        order = order
                                    )

                                    QuotationShare.share(
                                        context = context,
                                        file = pdf
                                    )

                                    errorMessage = null

                                } catch (e: Exception) {
                                    errorMessage = e.message
                                        ?: "No fue posible generar el PDF"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Descargar / compartir orden PDF")
                        }
                    } // Cierra Column
                } // Cierra Card
            } // Cierra items
        } // Cierra LazyColumn
    } // Cierra Column principal
} // Cierra ServiceHistoryScreen
