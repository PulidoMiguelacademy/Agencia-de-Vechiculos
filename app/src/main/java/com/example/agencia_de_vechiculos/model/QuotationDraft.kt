
package com.example.agencia_de_vechiculos.model

data class QuotationDraft(
    val clientName: String,
    val clientDocument: String,
    val vehicleId: Int,
    val vehiclePrice: Long,
    val accessoriesPrice: Long = 0L,
    val discountPercent: Int = 0,
    val paymentMethod: PaymentMethod = PaymentMethod.CONTADO
) {

    // Precio del vehículo más accesorios
    val subtotal: Long
        get() = vehiclePrice + accessoriesPrice

    // Descuento calculado sobre el vehículo
    val discountAmount: Long
        get() = vehiclePrice * discountPercent / 100

    // Total final de la cotización
    val total: Long
        get() = subtotal - discountAmount

    // Descuentos superiores al 3% necesitan aprobación
    val requiresManagerApproval: Boolean
        get() = discountPercent > 3

    // El precio debe ser positivo
    // Los accesorios pueden costar cero, pero no ser negativos
    val hasValidPrices: Boolean
        get() = vehiclePrice > 0 && accessoriesPrice >= 0

    // El descuento debe estar entre 0 y 100
    val hasValidDiscount: Boolean
        get() = discountPercent in 0..100

    // El cliente debe tener nombre y documento
    val hasValidClient: Boolean
        get() = clientName.isNotBlank() &&
                clientDocument.isNotBlank()
}
