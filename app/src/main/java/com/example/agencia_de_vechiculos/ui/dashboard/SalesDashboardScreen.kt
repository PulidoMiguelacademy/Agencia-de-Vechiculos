package com.example.agencia_de_vechiculos.ui.dashboard

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.ContractRepository
import com.example.agencia_de_vechiculos.data.DeliveryRepository
import com.example.agencia_de_vechiculos.data.SalesDashboardRepository
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.SaleRecord
import com.example.agencia_de_vechiculos.model.SalesPeriod
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla principal del Dashboard de Ventas (HU-34)
 * 
 * Permite visualizar unidades vendidas, ingresos y cumplimiento de metas
 * filtrando por periodo, sucursal y asesor.
 * 
 * Regla: Solo cuentan las ventas con entrega firmada.
 */
@Composable
fun SalesDashboardScreen(
    modifier: Modifier = Modifier,
    contractRepository: ContractRepository = remember { ContractRepository() },
    deliveryRepository: DeliveryRepository = remember { DeliveryRepository(contractRepository) },
    vehicleRepository: VehicleRepository = remember { VehicleRepository() },
    repository: SalesDashboardRepository = remember {
        SalesDashboardRepository(
            contractRepository = contractRepository,
            deliveryRepository = deliveryRepository,
            vehicleRepository = vehicleRepository,
        )
    },
) {
    var selectedPeriod by rememberSaveable { mutableStateOf(SalesPeriod.MES_ACTUAL) }
    var selectedBranch by rememberSaveable { mutableStateOf<Branch?>(null) }
    var selectedAdvisor by rememberSaveable { mutableStateOf("Todos") }

    val advisors = remember { listOf("Todos") + repository.availableAdvisors() }

    // Calcular métricas actuales según los filtros seleccionados
    val metrics = remember(selectedPeriod, selectedBranch, selectedAdvisor) {
        repository.getMetrics(
            period = selectedPeriod,
            branchFilter = selectedBranch,
            advisorFilter = if (selectedAdvisor == "Todos") null else selectedAdvisor,
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Dashboard de ventas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Ventas, ingresos y metas por periodo • Regla: Solo cuentan entregas firmadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- SECCIÓN DE FILTROS ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Filtro 1: Periodo
                Text("Periodo", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SalesPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) },
                        )
                    }
                }

                // Filtro 2: Sucursal (Criterio 2)
                Text("Sucursal", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedBranch == null,
                        onClick = { selectedBranch = null },
                        label = { Text("Todas las sedes") },
                    )
                    Branch.entries.forEach { branch ->
                        FilterChip(
                            selected = selectedBranch == branch,
                            onClick = { selectedBranch = branch },
                            label = { Text(branch.label) },
                        )
                    }
                }

                // Filtro 3: Asesor (Criterio 2)
                Text("Asesor comercial", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    advisors.forEach { advisor ->
                        FilterChip(
                            selected = selectedAdvisor == advisor,
                            onClick = { selectedAdvisor = advisor },
                            label = { Text(advisor) },
                        )
                    }
                }
            }
        }

        // --- TARJETAS KPI DE UNIDADES E INGRESOS (Criterio 1) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Card Unidades
                KpiCard(
                    title = "Unidades vendidas",
                    valueText = "${metrics.unitsSold} veh",
                    subtitleText = "Meta: ${metrics.targetUnits} veh (${formatPercentage(metrics.unitsGoalPercentage)})",
                    progress = (metrics.unitsGoalPercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f),
                )

                // Card Ingresos
                KpiCard(
                    title = "Ingresos totales",
                    valueText = formatPrice(metrics.totalRevenue),
                    subtitleText = "Meta: ${formatPrice(metrics.targetRevenue)} (${formatPercentage(metrics.revenueGoalPercentage)})",
                    progress = (metrics.revenueGoalPercentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // --- CUMPLIMIENTO DE METAS ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Cumplimiento de metas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val badgeText = when {
                            metrics.unitsGoalPercentage >= 100f && metrics.revenueGoalPercentage >= 100f -> "🏆 Metas superadas"
                            metrics.unitsGoalPercentage >= 100f || metrics.revenueGoalPercentage >= 100f -> "🎯 Buen desempeño"
                            else -> "📈 En progreso"
                        }
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Progress Bar Unidades
                    GoalProgressRow(
                        label = "Unidades (${metrics.unitsSold} / ${metrics.targetUnits})",
                        percentage = metrics.unitsGoalPercentage,
                    )

                    // Progress Bar Ingresos
                    GoalProgressRow(
                        label = "Ingresos (${formatPrice(metrics.totalRevenue)} / ${formatPrice(metrics.targetRevenue)})",
                        percentage = metrics.revenueGoalPercentage,
                    )
                }
            }
        }

        // --- NOTA DE APLICACIÓN DE LA REGLA ---
        if (metrics.pendingDeliveriesCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = "ℹ️ Regla aplicada: ${metrics.pendingDeliveriesCount} ${if (metrics.pendingDeliveriesCount == 1) "venta" else "ventas"} en este periodo/filtro no se ${if (metrics.pendingDeliveriesCount == 1) "incluye" else "incluyen"} por tener la entrega pendiente de firma.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // --- DETALLE DE VENTAS CON ENTREGA FIRMADA ---
        item {
            Text(
                text = "Ventas contabilizadas (${metrics.signedSales.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (metrics.signedSales.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No se encontraron ventas con entrega firmada para los filtros seleccionados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            items(metrics.signedSales) { sale ->
                SaleRecordCard(sale = sale)
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    valueText: String,
    subtitleText: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(subtitleText, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun GoalProgressRow(
    label: String,
    percentage: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                formatPercentage(percentage),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SaleRecordCard(sale: SaleRecord) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${sale.quotationFolio} • ${sale.clientName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = formatPrice(sale.finalPrice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text("Vehículo: ${sale.vehicleTitle}", style = MaterialTheme.typography.bodySmall)
            Text("Sede: ${sale.branch.label} • Asesor: ${sale.advisorName}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Entrega firmada el ${sale.deliveryDate ?: "Sin fecha"} por ${sale.signedBy ?: "Cliente"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatPrice(value: Long): String =
    "$ ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)}"

private fun formatPercentage(value: Float): String =
    "%.1f%%".format(Locale.US, value)
