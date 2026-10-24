package com.example.agencia_de_vechiculos.model

/**
 * Periodos de tiempo para filtrar en el dashboard de ventas (HU-34)
 */
enum class SalesPeriod(val label: String) {
    MES_ACTUAL("Mes actual"),
    TRIMESTRE_ACTUAL("Trimestre actual"),
    ANIO_ACTUAL("Año actual"),
    TODOS("Todos los periodos"),
}

/**
 * Representa el registro de una venta individual evaluada para el dashboard.
 */
data class SaleRecord(
    val quotationFolio: String,
    val clientName: String,
    val vehicleTitle: String,
    val finalPrice: Long,
    val branch: Branch,
    val advisorName: String,
    val deliveryDate: String?,
    val signedBy: String?,
    val deliveryStatus: DeliveryStatus,
) {
    /**
     * Regla HU-34: Solo cuentan las ventas con entrega firmada.
     * Es verdadera si la entrega está en estado CERRADA y cuenta con el nombre de quien firmó el acta.
     */
    val isSignedDelivery: Boolean
        get() = deliveryStatus == DeliveryStatus.CERRADA && !signedBy.isNullOrBlank()
}

/**
 * Representa las metas de ventas fijadas para el periodo/sucursal
 */
data class SalesGoal(
    val targetUnits: Int,
    val targetRevenue: Long,
)

/**
 * Las métricas consolidadas calculadas para el dashboard de ventas (HU-34)
 */
data class SalesDashboardMetrics(
    val period: SalesPeriod,
    val selectedBranch: Branch?,
    val selectedAdvisor: String?,
    val unitsSold: Int,
    val totalRevenue: Long,
    val targetUnits: Int,
    val targetRevenue: Long,
    val unitsGoalPercentage: Float,
    val revenueGoalPercentage: Float,
    val signedSales: List<SaleRecord>,
    val pendingDeliveriesCount: Int,
)
