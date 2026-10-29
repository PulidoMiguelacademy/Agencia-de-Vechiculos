
package com.example.agencia_de_vechiculos.model

enum class DiscountApprovalStatus {
    NO_REQUERIDA,
    PENDIENTE,
    APROBADA
}

data class QuotationDetail(
    val folio: String,
    val vehiclePrice: Long,
    val accessoriesPrice: Long,
    val discountPercent: Int,
    val discountAmount: Long,
    val createdAt: String,
    val expiresAt: String,
    val approvalStatus: DiscountApprovalStatus
) {
    val subtotal: Long
        get() = vehiclePrice + accessoriesPrice

    val total: Long
        get() = subtotal - discountAmount
}
