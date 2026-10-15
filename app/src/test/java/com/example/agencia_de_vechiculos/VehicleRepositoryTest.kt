package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.model.VehicleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleRepositoryTest {
    private val repository = VehicleRepository()

    @Test
    fun catalogo_noIncluyeVendidos() {
        assertTrue(repository.catalog().none { it.status == VehicleStatus.VENDIDO })
    }

    @Test
    fun catalogo_soloDisponiblesOReservados() {
        assertTrue(
            repository.catalog().all {
                it.status == VehicleStatus.DISPONIBLE || it.status == VehicleStatus.RESERVADO
            }
        )
    }

    @Test
    fun paginacion_dividePorTamanoDePagina() {
        val first = repository.catalogPage(0, pageSize = 5)
        assertEquals(5, first.items.size)
        assertEquals(3, first.totalPages) // 14 visibles / 5
        assertFalse(first.hasPrevious)
        assertTrue(first.hasNext)
    }

    @Test
    fun paginacion_ultimaPaginaNoTieneSiguiente() {
        val last = repository.catalogPage(99, pageSize = 5)
        assertEquals(2, last.page)
        assertEquals(4, last.items.size)
        assertFalse(last.hasNext)
    }
}
