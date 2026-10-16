package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.FuelType
import com.example.agencia_de_vechiculos.model.FuelType.DIESEL
import com.example.agencia_de_vechiculos.model.FuelType.ELECTRICO
import com.example.agencia_de_vechiculos.model.FuelType.GASOLINA
import com.example.agencia_de_vechiculos.model.FuelType.HIBRIDO
import com.example.agencia_de_vechiculos.model.Vehicle
import com.example.agencia_de_vechiculos.model.VehicleCondition
import com.example.agencia_de_vechiculos.model.VehicleCondition.NUEVO
import com.example.agencia_de_vechiculos.model.VehicleCondition.USADO
import com.example.agencia_de_vechiculos.model.VehicleFilter
import com.example.agencia_de_vechiculos.model.VehicleStatus
import com.example.agencia_de_vechiculos.model.VehicleStatus.DISPONIBLE
import com.example.agencia_de_vechiculos.model.VehicleStatus.RESERVADO
import com.example.agencia_de_vechiculos.model.VehicleStatus.VENDIDO

data class CatalogPage(
    val items: List<Vehicle>,
    val page: Int,
    val totalPages: Int,
) {
    val hasPrevious get() = page > 0
    val hasNext get() = page < totalPages - 1
}

class VehicleRepository(private val vehicles: List<Vehicle> = sampleVehicles) {

    /** Regla HU-04: solo se muestran vehículos "Disponibles" o "Reservados". */
    fun catalog(filter: VehicleFilter = VehicleFilter()): List<Vehicle> =
        vehicles.filter { (it.status == DISPONIBLE || it.status == RESERVADO) && filter.matches(it) }

    fun catalogPage(
        page: Int,
        pageSize: Int = PAGE_SIZE,
        filter: VehicleFilter = VehicleFilter(),
    ): CatalogPage {
        val all = catalog(filter)
        val totalPages = maxOf(1, (all.size + pageSize - 1) / pageSize)
        val safePage = page.coerceIn(0, totalPages - 1)
        return CatalogPage(
            items = all.drop(safePage * pageSize).take(pageSize),
            page = safePage,
            totalPages = totalPages,
        )
    }

    companion object {
        const val PAGE_SIZE = 6
    }
}

private fun v(
    id: Int, brand: String, model: String, year: Int, price: Long,
    condition: VehicleCondition, km: Int, status: VehicleStatus, fuel: FuelType,
) = Vehicle(id, brand, model, year, price, condition, km, status, fuel)

val sampleVehicles = listOf(
    v(1, "Toyota", "Corolla", 2025, 98_000_000, NUEVO, 0, DISPONIBLE, HIBRIDO),
    v(2, "Mazda", "CX-30", 2024, 142_000_000, NUEVO, 0, DISPONIBLE, GASOLINA),
    v(3, "Chevrolet", "Onix", 2021, 52_000_000, USADO, 41_000, DISPONIBLE, GASOLINA),
    v(4, "Renault", "Duster", 2020, 61_000_000, USADO, 68_500, RESERVADO, GASOLINA),
    v(5, "Kia", "Sportage", 2025, 175_000_000, NUEVO, 0, DISPONIBLE, HIBRIDO),
    v(6, "Nissan", "Versa", 2019, 48_500_000, USADO, 82_000, VENDIDO, GASOLINA),
    v(7, "Hyundai", "Tucson", 2023, 135_000_000, USADO, 22_300, DISPONIBLE, GASOLINA),
    v(8, "Ford", "Escape", 2022, 118_000_000, USADO, 35_900, DISPONIBLE, GASOLINA),
    v(9, "Volkswagen", "Tiguan", 2025, 189_000_000, NUEVO, 0, RESERVADO, DIESEL),
    v(10, "Suzuki", "Swift", 2018, 39_000_000, USADO, 97_400, DISPONIBLE, GASOLINA),
    v(11, "Honda", "CR-V", 2024, 168_000_000, NUEVO, 0, DISPONIBLE, HIBRIDO),
    v(12, "Chevrolet", "Tracker", 2021, 70_000_000, USADO, 52_000, VENDIDO, GASOLINA),
    v(13, "Toyota", "Hilux", 2023, 210_000_000, USADO, 28_000, DISPONIBLE, DIESEL),
    v(14, "Mazda", "3", 2022, 95_000_000, USADO, 31_200, DISPONIBLE, GASOLINA),
    v(15, "Renault", "Zoe", 2025, 120_000_000, NUEVO, 0, DISPONIBLE, ELECTRICO),
    v(16, "Kia", "Picanto", 2020, 41_000_000, USADO, 60_100, RESERVADO, GASOLINA),
)
