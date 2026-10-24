package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.DeliveryStatus
import com.example.agencia_de_vechiculos.model.SaleRecord
import com.example.agencia_de_vechiculos.model.SalesDashboardMetrics
import com.example.agencia_de_vechiculos.model.SalesGoal
import com.example.agencia_de_vechiculos.model.SalesPeriod

/**
 * Repositorio para calcular y proveer métricas del dashboard de ventas (HU-34)
 * 
 * Regla fundamental: Solo cuentan para las ventas, ingresos y metas aquellas ventas
 * que cuentan con entrega firmada (DeliveryStatus.CERRADA y signedBy no nulo).
 */
class SalesDashboardRepository(
    private val contractRepository: ContractRepository = ContractRepository(),
    private val deliveryRepository: DeliveryRepository = DeliveryRepository(contractRepository),
    private val vehicleRepository: VehicleRepository = VehicleRepository(),
) {
    /**
     * Lista de asesores disponibles para filtro
     */
    fun availableAdvisors(): List<String> {
        val advisorsFromContracts = contractRepository.allContracts().map { it.advisorName }
        val advisorsFromQuotations = contractRepository.allQuotations().map { it.advisorName }
        val defaultAdvisors = listOf("Carlos Mendoza", "Ana Lucía Martínez", "Roberto Gómez")
        return (advisorsFromContracts + advisorsFromQuotations + defaultAdvisors).distinct().sorted()
    }

    /**
     * Calcula las métricas del dashboard según el periodo y los filtros de sucursal y asesor.
     */
    fun getMetrics(
        period: SalesPeriod,
        branchFilter: Branch? = null,
        advisorFilter: String? = null,
        referenceDate: String = DEFAULT_REFERENCE_DATE,
    ): SalesDashboardMetrics {
        val allContracts = contractRepository.allContracts()

        // 1. Mapear cada contrato a una venta candidata
        val allSaleRecords = allContracts.map { contract ->
            val vehicle = vehicleRepository.vehicleById(contract.vehicleId)
            val vehicleTitle = if (vehicle != null) "${vehicle.brand} ${vehicle.model}" else "Vehículo #${contract.vehicleId}"
            val delivery = deliveryRepository.deliveryFor(contract.quotationFolio)

            SaleRecord(
                quotationFolio = contract.quotationFolio,
                clientName = contract.clientName,
                vehicleTitle = vehicleTitle,
                finalPrice = contract.finalPrice,
                branch = contract.branch,
                advisorName = contract.advisorName,
                deliveryDate = delivery?.deliveryDate ?: contract.signedDate,
                signedBy = delivery?.signedBy,
                deliveryStatus = delivery?.status ?: DeliveryStatus.EN_TRAMITES,
            )
        }

        // 2. Aplicar filtros de Sucursal y Asesor
        val branchAndAdvisorFiltered = allSaleRecords.filter { record ->
            val matchBranch = branchFilter == null || record.branch == branchFilter
            val matchAdvisor = advisorFilter.isNullOrBlank() || advisorFilter == ALL_ADVISORS_LABEL || record.advisorName == advisorFilter
            matchBranch && matchAdvisor
        }

        // 3. Aplicar filtro de Periodo
        val periodFiltered = branchAndAdvisorFiltered.filter { record ->
            val dateStr = record.deliveryDate ?: ""
            matchesPeriod(dateStr, period, referenceDate)
        }

        // 4. REGLA HU-34: Solo cuentan las ventas con entrega firmada
        val signedSales = periodFiltered.filter { it.isSignedDelivery }
        val pendingSalesCount = periodFiltered.count { !it.isSignedDelivery }

        // 5. Cálculos de Unidades e Ingresos
        val unitsSold = signedSales.size
        val totalRevenue = signedSales.sumOf { it.finalPrice }

        // 6. Definir Metas adaptadas al filtro
        val goals = calculateGoals(period, branchFilter, advisorFilter)

        val unitsPercentage = if (goals.targetUnits > 0) {
            (unitsSold.toFloat() / goals.targetUnits) * 100f
        } else {
            0f
        }

        val revenuePercentage = if (goals.targetRevenue > 0) {
            (totalRevenue.toFloat() / goals.targetRevenue) * 100f
        } else {
            0f
        }

        return SalesDashboardMetrics(
            period = period,
            selectedBranch = branchFilter,
            selectedAdvisor = if (advisorFilter == ALL_ADVISORS_LABEL) null else advisorFilter,
            unitsSold = unitsSold,
            totalRevenue = totalRevenue,
            targetUnits = goals.targetUnits,
            targetRevenue = goals.targetRevenue,
            unitsGoalPercentage = unitsPercentage,
            revenueGoalPercentage = revenuePercentage,
            signedSales = signedSales,
            pendingDeliveriesCount = pendingSalesCount,
        )
    }

    /**
     * Valida si una fecha (AAAA-MM-DD) corresponde al periodo seleccionado respecto a la fecha de referencia.
     */
    private fun matchesPeriod(dateStr: String, period: SalesPeriod, referenceDate: String): Boolean {
        if (period == SalesPeriod.TODOS) return true
        if (dateStr.length < 7) return false

        val yearMonthStr = dateStr.substring(0, 7) // e.g. "2026-10"
        val refYearMonthStr = if (referenceDate.length >= 7) referenceDate.substring(0, 7) else "2026-10"
        val yearStr = dateStr.substring(0, 4) // e.g. "2026"
        val refYearStr = if (referenceDate.length >= 4) referenceDate.substring(0, 4) else "2026"

        return when (period) {
            SalesPeriod.MES_ACTUAL -> yearMonthStr == refYearMonthStr
            SalesPeriod.TRIMESTRE_ACTUAL -> {
                if (yearStr != refYearStr) return false
                val month = dateStr.substring(5, 7).toIntOrNull() ?: return false
                val refMonth = if (referenceDate.length >= 7) referenceDate.substring(5, 7).toIntOrNull() ?: 10 else 10
                val quarter = (month - 1) / 3
                val refQuarter = (refMonth - 1) / 3
                quarter == refQuarter
            }
            SalesPeriod.ANIO_ACTUAL -> yearStr == refYearStr
            SalesPeriod.TODOS -> true
        }
    }

    /**
     * Define o escala las metas esperadas según el nivel de filtrado.
     */
    private fun calculateGoals(period: SalesPeriod, branchFilter: Branch?, advisorFilter: String?): SalesGoal {
        var baseUnits = when (period) {
            SalesPeriod.MES_ACTUAL -> 3
            SalesPeriod.TRIMESTRE_ACTUAL -> 8
            SalesPeriod.ANIO_ACTUAL -> 25
            SalesPeriod.TODOS -> 30
        }

        var baseRevenue = when (period) {
            SalesPeriod.MES_ACTUAL -> 180_000_000L
            SalesPeriod.TRIMESTRE_ACTUAL -> 500_000_000L
            SalesPeriod.ANIO_ACTUAL -> 1_500_000_000L
            SalesPeriod.TODOS -> 2_000_000_000L
        }

        if (branchFilter != null) {
            baseUnits = (baseUnits / 2).coerceAtLeast(1)
            baseRevenue /= 2
        }

        if (!advisorFilter.isNullOrBlank() && advisorFilter != ALL_ADVISORS_LABEL) {
            baseUnits = (baseUnits / 2).coerceAtLeast(1)
            baseRevenue /= 2
        }

        return SalesGoal(
            targetUnits = baseUnits,
            targetRevenue = baseRevenue,
        )
    }

    companion object {
        const val DEFAULT_REFERENCE_DATE = "2026-10-20"
        const val ALL_ADVISORS_LABEL = "Todos"
    }
}
