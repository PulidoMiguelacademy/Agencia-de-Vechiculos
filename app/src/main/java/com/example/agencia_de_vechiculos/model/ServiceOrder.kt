package com.example.agencia_de_vechiculos.model

enum class FuelLevel(val label: String) {
    RESERVA("Reserva"),
    UN_CUARTO("1/4"),
    MEDIO("1/2"),
    TRES_CUARTOS("3/4"),
    LLENO("Lleno"),
}

enum class VehicleArea(val label: String) {
    CAPO("Capó"),
    PARACHOQUES_DELANTERO("Parachoques delantero"),
    PARACHOQUES_TRASERO("Parachoques trasero"),
    LATERAL_IZQUIERDO("Lateral izquierdo"),
    LATERAL_DERECHO("Lateral derecho"),
    TECHO("Techo"),
    VIDRIOS("Vidrios"),
    RINES("Rines"),
}

data class PreviousDamage(
    val area: VehicleArea,
    val description: String,
    val photos: List<String>,
)

enum class ServiceOrderStatus(val label: String) {
    ABIERTA("Abierta"),
    EN_DIAGNOSTICO("En diagnóstico"),
}

data class ServiceOrder(
    val id: Int,
    val appointmentId: Int,
    val plate: String,
    val clientName: String,
    val serviceType: ServiceType,
    val openedDate: String,
    val mileageKm: Int? = null,
    val fuelLevel: FuelLevel? = null,
    val belongings: String = "",
    val clientRequest: String = "",
    val damages: List<PreviousDamage> = emptyList(),
    val signedBy: String? = null,
    val status: ServiceOrderStatus = ServiceOrderStatus.ABIERTA,
) {
    val missingFields: List<String>
        get() = buildList {
            if (mileageKm == null) add("Kilometraje")
            if (fuelLevel == null) add("Combustible")
            if (clientRequest.isBlank()) add("Solicitud del cliente")
        }

    val isRecordComplete: Boolean
        get() = missingFields.isEmpty()

    val canStartWork: Boolean
        get() = status == ServiceOrderStatus.EN_DIAGNOSTICO
}
