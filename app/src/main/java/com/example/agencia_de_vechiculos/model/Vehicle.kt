package com.example.agencia_de_vechiculos.model

enum class VehicleStatus { DISPONIBLE, RESERVADO, VENDIDO }

enum class VehicleCondition(val label: String) { NUEVO("Nuevo"), USADO("Usado") }

enum class FuelType(val label: String) {
    GASOLINA("Gasolina"),
    DIESEL("Diésel"),
    HIBRIDO("Híbrido"),
    ELECTRICO("Eléctrico"),
}

data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Long,
    val condition: VehicleCondition,
    val mileageKm: Int,
    val status: VehicleStatus,
    val fuel: FuelType,
    val vin: String,
    val photoUrl: String? = null,
)
