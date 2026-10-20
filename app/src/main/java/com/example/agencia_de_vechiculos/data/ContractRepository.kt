package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.ContractGenerationResult
import com.example.agencia_de_vechiculos.model.CustomerFile
import com.example.agencia_de_vechiculos.model.DocumentStatus
import com.example.agencia_de_vechiculos.model.DocumentType
import com.example.agencia_de_vechiculos.model.PaymentMethod
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationStatus
import com.example.agencia_de_vechiculos.model.Reservation
import com.example.agencia_de_vechiculos.model.SaleContract
import com.example.agencia_de_vechiculos.model.Vehicle
import com.example.agencia_de_vechiculos.model.VehicleStatus

class ContractRepository(
    private val quotations: List<Quotation> = sampleQuotations,
    private val customerFiles: List<CustomerFile> = sampleCustomerFiles,
    private val reservations: List<Reservation> = sampleReservations,
    private val vehicles: List<Vehicle> = sampleVehicles,
) {
    private val contracts = mutableListOf<SaleContract>()
    private var nextId = 1

    fun allQuotations(): List<Quotation> = quotations.toList()

    fun allContracts(): List<SaleContract> = contracts.toList()

    fun contractFor(quotationFolio: String): SaleContract? =
        contracts.firstOrNull { it.quotationFolio == quotationFolio }

    fun customerFileFor(clientDocument: String): CustomerFile? =
        customerFiles.firstOrNull { it.clientDocument == clientDocument }

    fun reservationFor(vehicleId: Int, clientDocument: String): Reservation? =
        reservations.firstOrNull { it.vehicleId == vehicleId && it.clientDocument == clientDocument }

    fun generate(quotationFolio: String): ContractGenerationResult {
        val quotation = quotations.firstOrNull { it.folio == quotationFolio.trim() }
            ?: return ContractGenerationResult.InvalidData("La cotización seleccionada no existe")

        if (quotation.status != QuotationStatus.ACEPTADA) {
            return ContractGenerationResult.InvalidData(
                "Solo se puede generar el contrato de una cotización aceptada. Estado actual: ${quotation.status.label}"
            )
        }

        contractFor(quotation.folio)?.let { return ContractGenerationResult.AlreadyGenerated(it) }

        val customerFile = customerFileFor(quotation.clientDocument)
        if (customerFile == null) {
            return ContractGenerationResult.IncompleteFile(DocumentType.entries.toList())
        }
        if (!customerFile.isComplete) {
            return ContractGenerationResult.IncompleteFile(customerFile.missingDocuments)
        }

        val vehicle = vehicles.firstOrNull { it.id == quotation.vehicleId }
            ?: return ContractGenerationResult.InvalidData("El vehículo de la cotización no existe")
        if (vehicle.status == VehicleStatus.VENDIDO) {
            return ContractGenerationResult.InvalidData("El vehículo ${vehicle.brand} ${vehicle.model} ya fue vendido")
        }

        val deposit = reservationFor(vehicle.id, quotation.clientDocument)?.depositAmount ?: 0

        val contract = SaleContract(
            id = nextId++,
            quotationFolio = quotation.folio,
            clientName = quotation.clientName,
            clientDocument = quotation.clientDocument,
            vehicleId = vehicle.id,
            vin = vehicle.vin,
            finalPrice = quotation.finalPrice,
            deposit = deposit,
            paymentMethod = quotation.paymentMethod,
        )
        contracts.add(contract)
        return ContractGenerationResult.Generated(contract)
    }
}

val sampleQuotations = listOf(
    Quotation(
        folio = "COT-0001",
        clientName = "Carlos Pérez",
        clientDocument = "1012345678",
        vehicleId = 4,
        finalPrice = 59_500_000,
        paymentMethod = PaymentMethod.CREDITO,
        status = QuotationStatus.ACEPTADA,
    ),
    Quotation(
        folio = "COT-0002",
        clientName = "Laura Gómez",
        clientDocument = "52345678",
        vehicleId = 9,
        finalPrice = 185_000_000,
        paymentMethod = PaymentMethod.CONTADO,
        status = QuotationStatus.ACEPTADA,
    ),
    Quotation(
        folio = "COT-0003",
        clientName = "Andrés Ramírez",
        clientDocument = "1098765432",
        vehicleId = 2,
        finalPrice = 140_000_000,
        paymentMethod = PaymentMethod.TRANSFERENCIA,
        status = QuotationStatus.ACEPTADA,
    ),
    Quotation(
        folio = "COT-0004",
        clientName = "Sofía Torres",
        clientDocument = "1023456789",
        vehicleId = 7,
        finalPrice = 133_000_000,
        paymentMethod = PaymentMethod.CREDITO,
        status = QuotationStatus.EMITIDA,
    ),
)

val sampleCustomerFiles = listOf(
    CustomerFile(
        clientDocument = "1012345678",
        documents = mapOf(
            DocumentType.IDENTIFICACION to DocumentStatus.VALIDADO,
            DocumentType.COMPROBANTE_DOMICILIO to DocumentStatus.VALIDADO,
            DocumentType.COMPROBANTE_INGRESOS to DocumentStatus.VALIDADO,
        ),
    ),
    CustomerFile(
        clientDocument = "52345678",
        documents = mapOf(
            DocumentType.IDENTIFICACION to DocumentStatus.VALIDADO,
            DocumentType.COMPROBANTE_DOMICILIO to DocumentStatus.RECHAZADO,
            DocumentType.COMPROBANTE_INGRESOS to DocumentStatus.PENDIENTE,
        ),
    ),
    CustomerFile(
        clientDocument = "1098765432",
        documents = mapOf(
            DocumentType.IDENTIFICACION to DocumentStatus.VALIDADO,
            DocumentType.COMPROBANTE_DOMICILIO to DocumentStatus.VALIDADO,
            DocumentType.COMPROBANTE_INGRESOS to DocumentStatus.VALIDADO,
        ),
    ),
)

val sampleReservations = listOf(
    Reservation(id = 1, vehicleId = 4, clientDocument = "1012345678", depositAmount = 5_000_000),
    Reservation(id = 2, vehicleId = 9, clientDocument = "52345678", depositAmount = 10_000_000),
)
