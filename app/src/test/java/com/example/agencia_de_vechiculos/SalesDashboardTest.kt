package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.ContractRepository
import com.example.agencia_de_vechiculos.data.DeliveryRepository
import com.example.agencia_de_vechiculos.data.SalesDashboardRepository
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.data.defaultSampleContracts
import com.example.agencia_de_vechiculos.data.defaultSampleDeliveries
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.DeliveryProcedure
import com.example.agencia_de_vechiculos.model.SalesPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SalesDashboardTest {

    private lateinit var vehicleRepository: VehicleRepository
    private lateinit var contractRepository: ContractRepository
    private lateinit var deliveryRepository: DeliveryRepository
    private lateinit var repository: SalesDashboardRepository

    @Before
    fun setUp() {
        vehicleRepository = VehicleRepository()
        contractRepository = ContractRepository(
            vehicleRepository = vehicleRepository,
            initialContracts = defaultSampleContracts,
        )
        deliveryRepository = DeliveryRepository(
            contractRepository = contractRepository,
            initialDeliveries = defaultSampleDeliveries,
        )
        repository = SalesDashboardRepository(
            contractRepository = contractRepository,
            deliveryRepository = deliveryRepository,
            vehicleRepository = vehicleRepository,
        )
    }

    @Test
    fun criterio1_alElegirPeriodo_muestraUnidadesVendidasEIngresos() {
        val metricsMonth = repository.getMetrics(period = SalesPeriod.MES_ACTUAL, referenceDate = "2026-10-20")

        // En octubre de 2026 hay 2 ventas con entrega firmada: COT-0001 (59,500,000) y COT-0003 (140,000,000)
        assertEquals(2, metricsMonth.unitsSold)
        assertEquals(199_500_000L, metricsMonth.totalRevenue)
        assertEquals(2, metricsMonth.signedSales.size)

        val metricsAll = repository.getMetrics(period = SalesPeriod.TODOS)
        // En todos los periodos se incluye también COT-0005 de septiembre (95,000,000)
        assertEquals(3, metricsAll.unitsSold)
        assertEquals(294_500_000L, metricsAll.totalRevenue)
    }

    @Test
    fun criterio2_alFiltrarPorSucursal_actualizaMetricas() {
        val metricsNorte = repository.getMetrics(
            period = SalesPeriod.MES_ACTUAL,
            branchFilter = Branch.NORTE,
            referenceDate = "2026-10-20",
        )
        assertEquals(1, metricsNorte.unitsSold)
        assertEquals(59_500_000L, metricsNorte.totalRevenue)
        assertEquals(Branch.NORTE, metricsNorte.selectedBranch)

        val metricsSur = repository.getMetrics(
            period = SalesPeriod.MES_ACTUAL,
            branchFilter = Branch.SUR,
            referenceDate = "2026-10-20",
        )
        assertEquals(1, metricsSur.unitsSold)
        assertEquals(140_000_000L, metricsSur.totalRevenue)
    }

    @Test
    fun criterio2_alFiltrarPorAsesor_actualizaMetricas() {
        val metricsCarlos = repository.getMetrics(
            period = SalesPeriod.MES_ACTUAL,
            advisorFilter = "Carlos Mendoza",
            referenceDate = "2026-10-20",
        )
        assertEquals(1, metricsCarlos.unitsSold)
        assertEquals(59_500_000L, metricsCarlos.totalRevenue)
        assertEquals("Carlos Mendoza", metricsCarlos.selectedAdvisor)

        val metricsRoberto = repository.getMetrics(
            period = SalesPeriod.MES_ACTUAL,
            advisorFilter = "Roberto Gómez",
            referenceDate = "2026-10-20",
        )
        assertEquals(1, metricsRoberto.unitsSold)
        assertEquals(140_000_000L, metricsRoberto.totalRevenue)
    }

    @Test
    fun regla_soloCuentanVentasConEntregaFirmada_excluyeEntregasPendientes() {
        // COT-0002 tiene contrato firmado pero su entrega está en estado EN_TRAMITES (sin firma de acta)
        val metricsMonth = repository.getMetrics(period = SalesPeriod.MES_ACTUAL, referenceDate = "2026-10-20")

        // COT-0002 no debe contarse en unidades ni ingresos
        val foliosInMetrics = metricsMonth.signedSales.map { it.quotationFolio }
        assertTrue(foliosInMetrics.contains("COT-0001"))
        assertTrue(foliosInMetrics.contains("COT-0003"))
        assertFalse(foliosInMetrics.contains("COT-0002"))

        // La entrega pendiente debe registrarse en pendingDeliveriesCount
        assertEquals(1, metricsMonth.pendingDeliveriesCount)
    }

    @Test
    fun dinamico_alFirmarActaDeEntrega_seSumaAutomaticamenteAlDashboard() {
        val folio = "COT-0002"
        // 1. Completar todos los trámites
        DeliveryProcedure.entries.forEach { proc ->
            deliveryRepository.advance(folio, proc, "REF-123")
            deliveryRepository.advance(folio, proc, "REF-123")
        }
        // 2. Programar entrega
        deliveryRepository.schedule(folio, "2026-10-20", "2026-10-20")
        // 3. Firmar acta
        deliveryRepository.closeSale(folio, "Laura Gómez", "2026-10-20")

        // 4. Consultar métricas del dashboard nuevamente
        val updatedMetrics = repository.getMetrics(period = SalesPeriod.MES_ACTUAL, referenceDate = "2026-10-20")

        // Ahora COT-0002 (185,000,000) debe contarse
        assertEquals(3, updatedMetrics.unitsSold)
        assertEquals(384_500_000L, updatedMetrics.totalRevenue)
        assertEquals(0, updatedMetrics.pendingDeliveriesCount)
    }

    @Test
    fun metas_calculaPorcentajesDeCumplimientoCorrectamente() {
        val metrics = repository.getMetrics(period = SalesPeriod.MES_ACTUAL, referenceDate = "2026-10-20")

        // Meta para Mes Actual base: 3 unidades, $180,000,000
        assertEquals(3, metrics.targetUnits)
        assertEquals(180_000_000L, metrics.targetRevenue)

        // Cumplimiento unidades: 2 de 3 = 66.66%
        assertEquals(66.66667f, metrics.unitsGoalPercentage, 0.01f)

        // Cumplimiento ingresos: 199,500,000 de 180,000,000 = 110.83%
        assertEquals(110.833336f, metrics.revenueGoalPercentage, 0.01f)
    }
}
