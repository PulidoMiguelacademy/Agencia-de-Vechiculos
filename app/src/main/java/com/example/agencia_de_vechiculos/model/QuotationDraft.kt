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
    val subtotal: Long
        get() = vehiclePrice + accessoriesPrice

    val discountAmount: Long
        get() = vehiclePrice * discountPercent / 100

    val total: Long
        get() = subtotal - discountAmount

    val requiresManagerApproval: Boolean
        get() = discountPercent > 3
}
