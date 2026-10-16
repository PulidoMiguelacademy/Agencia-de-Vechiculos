package com.example.agencia_de_vechiculos.model

enum class VehicleStatus { DISPONIBLE, RESERVADO, VENDIDO }

enum class VehicleCondition(val label: String) { NUEVO("Nuevo"), USADO("Usado") }

data class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Long,
    val condition: VehicleCondition,
    val mileageKm: Int,
    val status: VehicleStatus,
    val photoUrl: String? = null,
)
