package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.FuelType
import com.example.agencia_de_vechiculos.model.VehicleFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleFilterTest {
    private val repository = VehicleRepository()

    @Test
    fun filtroPorMarca_ignoraMayusculas() {
        val result = repository.catalog(VehicleFilter(brand = "toyota"))
        assertEquals(2, result.size)
        assertTrue(result.all { it.brand == "Toyota" })
    }

    @Test
    fun filtroPorRangoDePrecio() {
        val result = repository.catalog(VehicleFilter(minPrice = 90_000_000, maxPrice = 100_000_000))
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.price in 90_000_000..100_000_000 })
    }

    @Test
    fun filtroPorAnioYCombustible() {
        val result = repository.catalog(VehicleFilter(year = 2025, fuel = FuelType.ELECTRICO))
        assertEquals(1, result.size)
        assertEquals("Zoe", result.first().model)
    }

    @Test
    fun filtrosCombinados_aplicanTodosALaVez() {
        val result = repository.catalog(VehicleFilter(brand = "Toyota", fuel = FuelType.DIESEL))
        assertEquals(1, result.size)
        assertEquals("Hilux", result.first().model)
    }

    @Test
    fun sinCoincidencias_devuelveListaVaciaYUnaPagina() {
        val page = repository.catalogPage(0, filter = VehicleFilter(brand = "Ferrari"))
        assertTrue(page.items.isEmpty())
        assertEquals(1, page.totalPages)
        assertFalse(page.hasNext)
    }

    @Test
    fun filtro_noMuestraVendidosAunqueCoincidan() {
        // Nissan Versa está VENDIDO.
        assertTrue(repository.catalog(VehicleFilter(brand = "Nissan")).isEmpty())
    }

    @Test
    fun precioMinimoMayorAlMaximo_esInvalido() {
        assertTrue(VehicleFilter(minPrice = 100, maxPrice = 50).hasInvalidPriceRange)
        assertFalse(VehicleFilter(minPrice = 50, maxPrice = 50).hasInvalidPriceRange)
        assertFalse(VehicleFilter(minPrice = 50).hasInvalidPriceRange)
    }
}
