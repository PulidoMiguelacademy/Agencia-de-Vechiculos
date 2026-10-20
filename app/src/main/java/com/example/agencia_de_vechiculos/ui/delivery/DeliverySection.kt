package com.example.agencia_de_vechiculos.ui.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.DeliveryRepository
import com.example.agencia_de_vechiculos.model.Delivery
import com.example.agencia_de_vechiculos.model.DeliveryCloseResult
import com.example.agencia_de_vechiculos.model.DeliveryProcedure
import com.example.agencia_de_vechiculos.model.DeliveryScheduleResult
import com.example.agencia_de_vechiculos.model.DeliveryStatus
import com.example.agencia_de_vechiculos.model.ProcedureAdvanceResult
import com.example.agencia_de_vechiculos.model.ProcedureStatus

@Composable
fun DeliverySection(
    delivery: Delivery,
    clientName: String,
    today: String,
    repository: DeliveryRepository,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val folio = delivery.quotationFolio
    var references by remember(folio) { mutableStateOf(mapOf<DeliveryProcedure, String>()) }
    var deliveryDate by remember(folio) { mutableStateOf(delivery.deliveryDate ?: today) }
    var signer by remember(folio) { mutableStateOf(clientName) }
    var message by remember(folio) { mutableStateOf<Pair<String, Boolean>?>(null) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Entrega del vehículo • ${delivery.status.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Sin revisión pre-entrega aprobada no se entrega",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DeliveryProcedure.entries.forEach { procedure ->
                val status = delivery.procedures[procedure] ?: ProcedureStatus.PENDIENTE
                val canAdvance = delivery.status == DeliveryStatus.EN_TRAMITES && status != ProcedureStatus.COMPLETADO
                val needsReference = status == ProcedureStatus.EN_PROCESO && procedure.referenceLabel != null

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(procedure.label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = delivery.statusLabel(procedure),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status == ProcedureStatus.COMPLETADO) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        delivery.references[procedure]?.let {
                            Text("${procedure.referenceLabel}: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (canAdvance) {
                        OutlinedButton(
                            onClick = {
                                val result = repository.advance(folio, procedure, references[procedure].orEmpty())
                                message = when (result) {
                                    is ProcedureAdvanceResult.Advanced ->
                                        "${procedure.label}: ${result.delivery.statusLabel(procedure)}" to false
                                    is ProcedureAdvanceResult.InvalidData -> result.message to true
                                }
                                onChange()
                            },
                        ) {
                            Text(
                                when {
                                    status == ProcedureStatus.PENDIENTE -> "Iniciar"
                                    procedure == DeliveryProcedure.REVISION_PRE_ENTREGA -> "Aprobar"
                                    else -> "Completar"
                                }
                            )
                        }
                    }
                }
                if (canAdvance && needsReference) {
                    OutlinedTextField(
                        value = references[procedure].orEmpty(),
                        onValueChange = { references = references + (procedure to it) },
                        label = { Text(procedure.referenceLabel.orEmpty()) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            HorizontalDivider()
            when (delivery.status) {
                DeliveryStatus.CERRADA -> Text(
                    text = "Venta cerrada. Acta firmada por ${delivery.signedBy} el ${delivery.deliveryDate}.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                else -> {
                    delivery.deliveryDate?.let { Text("Entrega programada para el $it") }
                    OutlinedTextField(
                        value = deliveryDate,
                        onValueChange = { deliveryDate = it },
                        label = { Text("Fecha de entrega (AAAA-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = {
                            message = when (val result = repository.schedule(folio, deliveryDate, today)) {
                                is DeliveryScheduleResult.Scheduled ->
                                    "Entrega programada para el ${result.delivery.deliveryDate}." to false
                                is DeliveryScheduleResult.PendingProcedures -> {
                                    val missing = result.pendingProcedures.joinToString(", ") { it.label }
                                    val rule = if (DeliveryProcedure.REVISION_PRE_ENTREGA in result.pendingProcedures) {
                                        " Sin revisión pre-entrega aprobada no se entrega."
                                    } else {
                                        ""
                                    }
                                    "No se puede programar la entrega. Falta completar: $missing.$rule" to true
                                }
                                is DeliveryScheduleResult.InvalidData -> result.message to true
                            }
                            onChange()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (delivery.status == DeliveryStatus.PROGRAMADA) "Reprogramar entrega" else "Programar entrega")
                    }

                    if (delivery.status == DeliveryStatus.PROGRAMADA) {
                        OutlinedTextField(
                            value = signer,
                            onValueChange = { signer = it },
                            label = { Text("Nombre de quien firma el acta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                message = when (val result = repository.closeSale(folio, signer, today)) {
                                    is DeliveryCloseResult.Closed -> "Acta firmada. La venta quedó cerrada." to false
                                    is DeliveryCloseResult.InvalidData -> result.message to true
                                }
                                onChange()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Firmar acta y cerrar venta")
                        }
                    }
                }
            }

            message?.let { (text, isError) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
