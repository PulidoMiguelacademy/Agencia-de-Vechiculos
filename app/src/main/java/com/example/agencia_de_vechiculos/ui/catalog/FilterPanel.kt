package com.example.agencia_de_vechiculos.ui.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.model.FuelType
import com.example.agencia_de_vechiculos.model.VehicleFilter

/** Valores tal como los escribe el usuario en el formulario de filtros. */
data class FilterForm(
    val brand: String = "",
    val model: String = "",
    val minPrice: String = "",
    val maxPrice: String = "",
    val year: String = "",
    val fuel: FuelType? = null,
) {
    fun toFilter() = VehicleFilter(
        brand = brand,
        model = model,
        minPrice = minPrice.toLongOrNull(),
        maxPrice = maxPrice.toLongOrNull(),
        year = year.toIntOrNull(),
        fuel = fuel,
    )

    /** Regla HU-05: el precio mínimo no puede ser mayor al máximo. */
    val priceError: String?
        get() = if (toFilter().hasInvalidPriceRange) {
            "El precio mínimo no puede ser mayor al máximo"
        } else {
            null
        }

    companion object {
        val Saver = listSaver<FilterForm, String>(
            save = { listOf(it.brand, it.model, it.minPrice, it.maxPrice, it.year, it.fuel?.name.orEmpty()) },
            restore = {
                FilterForm(it[0], it[1], it[2], it[3], it[4], FuelType.entries.firstOrNull { f -> f.name == it[5] })
            },
        )
    }
}

@Composable
fun FilterPanel(
    form: FilterForm,
    onFormChange: (FilterForm) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val priceError = form.priceError
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Filtrar vehículos", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.brand,
                    onValueChange = { onFormChange(form.copy(brand = it)) },
                    label = { Text("Marca") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.model,
                    onValueChange = { onFormChange(form.copy(model = it)) },
                    label = { Text("Modelo") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.minPrice,
                    onValueChange = { onFormChange(form.copy(minPrice = it.digitsOnly())) },
                    label = { Text("Precio mín.") },
                    singleLine = true,
                    isError = priceError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.maxPrice,
                    onValueChange = { onFormChange(form.copy(maxPrice = it.digitsOnly())) },
                    label = { Text("Precio máx.") },
                    singleLine = true,
                    isError = priceError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            if (priceError != null) {
                Text(priceError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = form.year,
                onValueChange = { onFormChange(form.copy(year = it.digitsOnly().take(4))) },
                label = { Text("Año") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FuelType.entries.forEach { fuel ->
                    FilterChip(
                        selected = form.fuel == fuel,
                        onClick = { onFormChange(form.copy(fuel = if (form.fuel == fuel) null else fuel)) },
                        label = { Text(fuel.label) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSearch, enabled = priceError == null) { Text("Buscar") }
                OutlinedButton(onClick = onClear) { Text("Limpiar") }
            }
        }
    }
}

private fun String.digitsOnly() = filter { it.isDigit() }
