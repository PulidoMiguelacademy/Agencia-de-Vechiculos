
package com.example.agencia_de_vechiculos.model

data class MaintenanceReminder(
    val id: Int,
    val plate: String,
    val serviceName: String,
    val referenceDate: String,
    val referenceMileageKm: Int,
    val intervalMonths: Int,
    val intervalKm: Int,
    val dueDate: String,
    val dueMileageKm: Int
)

enum class MaintenanceStatus(val label: String) {
    AL_DIA("Al día"),
    PROXIMO("Próximo"),
    VENCIDO("Mantenimiento requerido")
}

data class MaintenanceEvaluation(
    val reminder: MaintenanceReminder,
    val currentMileageKm: Int,
    val status: MaintenanceStatus,
    val message: String
)
