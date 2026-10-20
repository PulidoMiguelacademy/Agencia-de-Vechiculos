package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.ContractGenerationResult
import com.example.agencia_de_vechiculos.model.ContractSignResult
import com.example.agencia_de_vechiculos.model.ContractStatus
import com.example.agencia_de_vechiculos.model.CustomerFile
import com.example.agencia_de_vechiculos.model.DocumentStatus
import com.example.agencia_de_vechiculos.model.DocumentType
import com.example.agencia_de_vechiculos.model.PaymentMethod
import com.example.agencia_de_vechiculos.model.Quotation
import com.example.agencia_de_vechiculos.model.QuotationStatus
import com.example.agencia_de_vechiculos.model.Reservation
import com.example.agencia_de_vechiculos.model.SaleContract
import com.example.agencia_de_vechiculos.model.VehicleStatus

/**
 * 
 * guarda los contratos de venta y se encarga de generarlos y de registrar su firma
 * (HU-24)
 * 
 * por ahora todo vive en memoria: las cotizaciones, los expedientes y las reservas
 * son datos de ejemplo hasta que existan sus propias HU (13, 23 y 08). Usa el
 * mismo VehicleRepository del catálogo, así un vehículo vendido deja de aparecer
 * allí
 * 
 */
class ContractRepository(
    private val quotations: List<Quotation> = sampleQuotations,
    private val customerFiles: List<CustomerFile> = sampleCustomerFiles,
    private val reservations: List<Reservation> = sampleReservations,
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
) {
    /**
     * 
     * los contratos que ya se generaron
     * 
     */
    private val contracts = mutableListOf<SaleContract>()

    /**
     * 
     * el número que le toca al próximo contrato
     * 
     */
    private var nextId = 1

    /**
     * 
     * devuelve todas las cotizaciones, aceptadas o no
     * 
     */
    fun allQuotations(): List<Quotation> = quotations.toList()

    /**
     * 
     * devuelve todos los contratos generados
     * 
     */
    fun allContracts(): List<SaleContract> = contracts.toList()

    /**
     * 
     * devuelve el contrato de una cotización, o null si todavía no tiene
     * 
     */
    fun contractFor(quotationFolio: String): SaleContract? =
        contracts.firstOrNull { it.quotationFolio == quotationFolio }

    /**
     * 
     * devuelve el expediente de un cliente, o null si no tiene
     * 
     */
    fun customerFileFor(clientDocument: String): CustomerFile? =
        customerFiles.firstOrNull { it.clientDocument == clientDocument }

    /**
     * 
     * devuelve la reserva que hizo un cliente sobre un vehículo, o null si no reservó
     * 
     */
    fun reservationFor(vehicleId: Int, clientDocument: String): Reservation? =
        reservations.firstOrNull { it.vehicleId == vehicleId && it.clientDocument == clientDocument }

    /**
     * 
     * genera el contrato de una cotización
     * 
     * revisa, en orden, que la cotización exista y esté aceptada, que no tenga ya un
     * contrato (una cotización genera uno solo), que el expediente del cliente esté
     * completo y que el vehículo no se haya vendido. Si todo está bien, arma el
     * contrato con los datos de la cotización, el VIN del vehículo y el anticipo de
     * la reserva, que se descuenta del precio (si no hubo reserva, el anticipo es 0)
     * 
     */
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

        val vehicle = vehicleRepository.vehicleById(quotation.vehicleId)
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

    /**
     * 
     * registra que el cliente firmó el contrato de una cotización
     * 
     * revisa que el contrato exista, que no se haya firmado antes y que la fecha venga
     * como AAAA-MM-DD. Después marca el contrato como firmado con esa fecha y pasa el
     * vehículo a vendido
     * 
     */
    fun registerSigned(quotationFolio: String, signedDate: String): ContractSignResult {
        val index = contracts.indexOfFirst { it.quotationFolio == quotationFolio.trim() }
        if (index == -1) {
            return ContractSignResult.InvalidData("Primero debes generar el contrato de la cotización")
        }

        val contract = contracts[index]
        if (contract.status == ContractStatus.FIRMADO) {
            return ContractSignResult.InvalidData("El contrato ya fue registrado como firmado el ${contract.signedDate}")
        }

        val cleanDate = signedDate.trim()
        if (!DATE_REGEX.matches(cleanDate)) {
            return ContractSignResult.InvalidData("Ingresa la fecha de firma en formato AAAA-MM-DD")
        }

        val vehicle = vehicleRepository.markAsSold(contract.vehicleId)
            ?: return ContractSignResult.InvalidData("El vehículo del contrato no existe")

        val signed = contract.copy(status = ContractStatus.FIRMADO, signedDate = cleanDate)
        contracts[index] = signed
        return ContractSignResult.Signed(signed, vehicle)
    }

    companion object {
        /**
         * 
         * el formato que debe tener la fecha de firma: AAAA-MM-DD
         * 
         */
        private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    }
}

/**
 * 
 * cotizaciones de ejemplo, una para cada caso: lista para firmar, con el expediente
 * incompleto, sin reserva y sin aceptar
 * 
 */
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

/**
 * 
 * expedientes de ejemplo de los clientes de las cotizaciones. El de Sofía Torres
 * no existe a propósito
 * 
 */
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

/**
 * 
 * reservas de ejemplo con el anticipo que pagó cada cliente
 * 
 */
val sampleReservations = listOf(
    Reservation(id = 1, vehicleId = 4, clientDocument = "1012345678", depositAmount = 5_000_000),
    Reservation(id = 2, vehicleId = 9, clientDocument = "52345678", depositAmount = 10_000_000),
)
