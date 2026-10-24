package com.example.agencia_de_vechiculos.model

/**
 *
 * las formas en que el cliente puede pagar el vehículo. Cada una trae el texto
 * que se muestra en la pantalla y en el PDF
 *
 */
enum class PaymentMethod(val label: String) {
    CONTADO("Contado"),
    CREDITO("Crédito"),
    TRANSFERENCIA("Transferencia"),
}

/**
 *
 * en qué va una cotización. Solo una cotización aceptada puede convertirse en
 * contrato
 *
 */
enum class QuotationStatus(val label: String) {
    EMITIDA("Emitida"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
}

/**
 *
 * la propuesta de venta que se le hizo al cliente (HU-13). Aquí solo están los
 * datos que necesita el contrato, para no volver a escribirlos
 *
 */
data class Quotation(
    /**
     *
     * el código que identifica la cotización, por ejemplo COT-0001
     *
     */
    val folio: String,

    /**
     *
     * el nombre del cliente
     *
     */
    val clientName: String,

    /**
     *
     * el número de documento del cliente. Con él se buscan su expediente y su
     * reserva
     *
     */
    val clientDocument: String,

    /**
     *
     * el vehículo que se cotizó
     *
     */
    val vehicleId: Int,

    /**
     *
     * el precio que se acordó con el cliente, en pesos
     *
     */
    val finalPrice: Long,

    /**
     *
     * cómo va a pagar el cliente
     *
     */
    val paymentMethod: PaymentMethod,

    /**
     *
     * en qué va la cotización
     *
     */
    val status: QuotationStatus,

    /**
     *
     * la sucursal o sede donde se realiza la cotización
     *
     */
    val branch: Branch = Branch.NORTE,

    /**
     *
     * el nombre del asesor comercial encargado
     *
     */
    val advisorName: String = "Carlos Mendoza",
)

/**
 *
 * los documentos que debe tener el expediente del cliente (HU-23) para poder
 * firmar el contrato
 *
 */
enum class DocumentType(val label: String) {
    IDENTIFICACION("Identificación"),
    COMPROBANTE_DOMICILIO("Comprobante de domicilio"),
    COMPROBANTE_INGRESOS("Comprobante de ingresos"),
}

/**
 *
 * en qué va la revisión de un documento. Solo cuenta como entregado el que
 * está validado
 *
 */
enum class DocumentStatus(val label: String) {
    PENDIENTE("Pendiente de validación"),
    VALIDADO("Validado"),
    RECHAZADO("Rechazado"),
}

/**
 *
 * el expediente del cliente: sus documentos y el estado de cada uno
 *
 * sabe por sí solo qué documentos faltan y si está completo, así la generación
 * del contrato puede decirle al asesor exactamente qué falta
 *
 */
data class CustomerFile(
    /**
     *
     * el número de documento del cliente dueño del expediente
     *
     */
    val clientDocument: String,

    /**
     *
     * cada documento cargado con su estado. Un documento que no está aquí
     * todavía no se ha subido
     *
     */
    val documents: Map<DocumentType, DocumentStatus>,
) {
    /**
     *
     * los documentos que faltan: los que no se han subido y los que están
     * pendientes o rechazados
     *
     */
    val missingDocuments: List<DocumentType>
        get() = DocumentType.entries.filter { documents[it] != DocumentStatus.VALIDADO }

    /**
     *
     * true cuando todos los documentos están validados
     *
     */
    val isComplete: Boolean
        get() = missingDocuments.isEmpty()
}

/**
 *
 * la reserva de un vehículo a nombre de un cliente (HU-08). Su anticipo se
 * descuenta del precio en el contrato
 *
 */
data class Reservation(
    /**
     *
     * el número de la reserva
     *
     */
    val id: Int,

    /**
     *
     * el vehículo reservado
     *
     */
    val vehicleId: Int,

    /**
     *
     * el número de documento del cliente que reservó
     *
     */
    val clientDocument: String,

    /**
     *
     * lo que el cliente ya pagó para apartar el vehículo, en pesos
     *
     */
    val depositAmount: Long,
)

/**
 *
 * en qué va el contrato: recién generado o ya firmado por el cliente
 *
 */
enum class ContractStatus(val label: String) {
    GENERADO("Generado"),
    FIRMADO("Firmado"),
}

/**
 *
 * el contrato de venta de un vehículo (HU-24)
 *
 * se arma con los datos de la cotización aceptada, el VIN del vehículo y el
 * anticipo de la reserva, así nadie tiene que volver a escribirlos
 *
 */
data class SaleContract(
    /**
     *
     * el número del contrato
     *
     */
    val id: Int,

    /**
     *
     * la cotización de la que salió el contrato. Cada cotización tiene un solo
     * contrato
     *
     */
    val quotationFolio: String,

    /**
     *
     * el nombre del comprador
     *
     */
    val clientName: String,

    /**
     *
     * el número de documento del comprador
     *
     */
    val clientDocument: String,

    /**
     *
     * el vehículo que se vende
     *
     */
    val vehicleId: Int,

    /**
     *
     * el número de serie del vehículo (17 caracteres), que lo identifica en el
     * contrato
     *
     */
    val vin: String,

    /**
     *
     * el precio acordado en la cotización, en pesos
     *
     */
    val finalPrice: Long,

    /**
     *
     * el anticipo que el cliente pagó en la reserva. Es 0 si no reservó
     *
     */
    val deposit: Long,

    /**
     *
     * cómo va a pagar el cliente
     *
     */
    val paymentMethod: PaymentMethod,

    /**
     *
     * en qué va el contrato. Empieza como generado
     *
     */
    val status: ContractStatus = ContractStatus.GENERADO,

    /**
     *
     * el día en que el cliente firmó (AAAA-MM-DD). Está vacío hasta que se
     * registra la firma
     *
     */
    val signedDate: String? = null,

    /**
     *
     * la sucursal o sede donde se atiende la venta
     *
     */
    val branch: Branch = Branch.NORTE,

    /**
     *
     * el asesor comercial a cargo
     *
     */
    val advisorName: String = "Carlos Mendoza",
) {
    /**
     *
     * lo que le falta pagar al cliente: el precio final menos el anticipo de la
     * reserva
     *
     */
    val balance: Long
        get() = finalPrice - deposit
}

/**
 *
 * lo que puede pasar al intentar generar un contrato
 *
 */
sealed interface ContractGenerationResult {
    /**
     *
     * el contrato se creó
     *
     */
    data class Generated(val contract: SaleContract) : ContractGenerationResult

    /**
     *
     * el expediente del cliente no está completo. Trae los documentos que faltan
     * para mostrárselos al asesor
     *
     */
    data class IncompleteFile(val missingDocuments: List<DocumentType>) : ContractGenerationResult

    /**
     *
     * la cotización ya tenía contrato. Trae ese contrato en vez de crear otro
     *
     */
    data class AlreadyGenerated(val contract: SaleContract) : ContractGenerationResult

    /**
     *
     * algún dato no sirve (la cotización no existe, no está aceptada o el
     * vehículo ya se vendió). Trae el mensaje para el asesor
     *
     */
    data class InvalidData(val message: String) : ContractGenerationResult
}

/**
 *
 * lo que puede pasar al registrar la firma de un contrato
 *
 */
sealed interface ContractSignResult {
    /**
     *
     * la firma quedó registrada. Trae el contrato firmado y el vehículo, que ya
     * está como vendido
     *
     */
    data class Signed(val contract: SaleContract, val vehicle: Vehicle) : ContractSignResult

    /**
     *
     * no se pudo registrar la firma. Trae el mensaje para el asesor
     *
     */
    data class InvalidData(val message: String) : ContractSignResult
}
