package com.example.agencia_de_vechiculos.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.CatalogPage
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.Vehicle
import com.example.agencia_de_vechiculos.model.VehicleCondition
import com.example.agencia_de_vechiculos.model.VehicleStatus
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    repository: VehicleRepository = remember { VehicleRepository() },
    onScheduleTestDrive: ((Vehicle) -> Unit)? = null,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    // `form` es lo que se está escribiendo; `applied` solo cambia al pulsar "Buscar".
    var form by rememberSaveable(stateSaver = FilterForm.Saver) { mutableStateOf(FilterForm()) }
    var applied by rememberSaveable(stateSaver = FilterForm.Saver) { mutableStateOf(FilterForm()) }
    val catalogPage = repository.catalogPage(page, filter = applied.toFilter())

    CatalogContent(
        catalogPage = catalogPage,
        form = form,
        onFormChange = { form = it },
        onSearch = {
            applied = form
            page = 0
        },
        onClear = {
            form = FilterForm()
            applied = FilterForm()
            page = 0
        },
        onPrevious = { page = catalogPage.page - 1 },
        onNext = { page = catalogPage.page + 1 },
        onScheduleTestDrive = onScheduleTestDrive,
        modifier = modifier,
    )
}

@Composable
fun CatalogContent(
    catalogPage: CatalogPage,
    form: FilterForm,
    onFormChange: (FilterForm) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onScheduleTestDrive: ((Vehicle) -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Catálogo de vehículos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item { FilterPanel(form, onFormChange, onSearch, onClear) }
        if (catalogPage.items.isEmpty()) {
            // HU-05 criterio 2: sin coincidencias se informa al usuario.
            item { Text("Sin resultados", style = MaterialTheme.typography.titleMedium) }
        }
        items(catalogPage.items, key = { it.id }) {
            VehicleCard(vehicle = it, onScheduleTestDrive = onScheduleTestDrive)
        }
        item {
            // Criterio 2: al llegar al final de la lista se puede pasar de página.
            PaginationBar(catalogPage, onPrevious, onNext)
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    modifier: Modifier = Modifier,
    onScheduleTestDrive: ((Vehicle) -> Unit)? = null,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column {
            // Criterio 1: foto. Placeholder hasta contar con una librería de imágenes (photoUrl).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Foto: ${vehicle.brand} ${vehicle.model}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${vehicle.brand} ${vehicle.model}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AssistChip(onClick = {}, label = { Text(vehicle.condition.label) })
                }
                Text("Año ${vehicle.year}", style = MaterialTheme.typography.bodyMedium)
                if (vehicle.condition == VehicleCondition.USADO) {
                    Text(
                        "${formatNumber(vehicle.mileageKm.toLong())} km",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    formatPrice(vehicle.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (vehicle.status == VehicleStatus.RESERVADO) {
                    Text(
                        "Reservado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (onScheduleTestDrive != null && vehicle.status == VehicleStatus.DISPONIBLE) {
                    OutlinedButton(
                        onClick = { onScheduleTestDrive(vehicle) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Agendar prueba de manejo")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaginationBar(
    page: CatalogPage,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = onPrevious, enabled = page.hasPrevious) { Text("Anterior") }
        Text("Página ${page.page + 1} de ${page.totalPages}")
        Button(onClick = onNext, enabled = page.hasNext) { Text("Siguiente") }
    }
}

private fun formatNumber(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")).format(value)

private fun formatPrice(value: Long): String = "$ ${formatNumber(value)}"
