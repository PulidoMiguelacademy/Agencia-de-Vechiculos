package com.example.agencia_de_vechiculos.model

enum class PaymentMethod(val label: String) {
    CONTADO("Contado"),
    CREDITO("Crédito"),
    TRANSFERENCIA("Transferencia"),
}

enum class QuotationStatus(val label: String) {
    EMITIDA("Emitida"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
}

data class Quotation(
    val folio: String,
    val clientName: String,
    val clientDocument: String,
    val vehicleId: Int,
    val finalPrice: Long,
    val paymentMethod: PaymentMethod,
    val status: QuotationStatus,
)

enum class DocumentType(val label: String) {
    IDENTIFICACION("Identificación"),
    COMPROBANTE_DOMICILIO("Comprobante de domicilio"),
    COMPROBANTE_INGRESOS("Comprobante de ingresos"),
}

enum class DocumentStatus(val label: String) {
    PENDIENTE("Pendiente de validación"),
    VALIDADO("Validado"),
    RECHAZADO("Rechazado"),
}

data class CustomerFile(
    val clientDocument: String,
    val documents: Map<DocumentType, DocumentStatus>,
) {
    val missingDocuments: List<DocumentType>
        get() = DocumentType.entries.filter { documents[it] != DocumentStatus.VALIDADO }

    val isComplete: Boolean
        get() = missingDocuments.isEmpty()
}

data class Reservation(
    val id: Int,
    val vehicleId: Int,
    val clientDocument: String,
    val depositAmount: Long,
)

enum class ContractStatus(val label: String) {
    GENERADO("Generado"),
    FIRMADO("Firmado"),
}

data class SaleContract(
    val id: Int,
    val quotationFolio: String,
    val clientName: String,
    val clientDocument: String,
    val vehicleId: Int,
    val vin: String,
    val finalPrice: Long,
    val deposit: Long,
    val paymentMethod: PaymentMethod,
    val status: ContractStatus = ContractStatus.GENERADO,
    val signedDate: String? = null,
) {
    val balance: Long
        get() = finalPrice - deposit
}

sealed interface ContractGenerationResult {
    data class Generated(val contract: SaleContract) : ContractGenerationResult

    data class IncompleteFile(val missingDocuments: List<DocumentType>) : ContractGenerationResult

    data class AlreadyGenerated(val contract: SaleContract) : ContractGenerationResult

    data class InvalidData(val message: String) : ContractGenerationResult
}

sealed interface ContractSignResult {
    data class Signed(val contract: SaleContract, val vehicle: Vehicle) : ContractSignResult

    data class InvalidData(val message: String) : ContractSignResult
}
