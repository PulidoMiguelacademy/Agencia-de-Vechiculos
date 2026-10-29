package com.example.agencia_de_vechiculos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.ContractRepository
import com.example.agencia_de_vechiculos.data.DeliveryRepository
import com.example.agencia_de_vechiculos.data.ServiceOrderRepository
import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.data.WorkshopRepository
import com.example.agencia_de_vechiculos.ui.catalog.CatalogScreen
import com.example.agencia_de_vechiculos.ui.contract.ContractScreen
import com.example.agencia_de_vechiculos.ui.testdrive.TestDriveScreen
import com.example.agencia_de_vechiculos.ui.theme.AgenciadevechiculosTheme
import com.example.agencia_de_vechiculos.ui.workshop.WorkshopScreen
import com.example.agencia_de_vechiculos.ui.quotation.QuotationScreen
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import com.example.agencia_de_vechiculos.data.QuotationRepository
import com.example.agencia_de_vechiculos.ui.history.ServiceHistoryScreen
import com.example.agencia_de_vechiculos.data.MaintenanceReminderRepository
import com.example.agencia_de_vechiculos.ui.maintenance.MaintenanceReminderScreen
import com.example.agencia_de_vechiculos.data.QuotationDeliveryRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AgenciadevechiculosTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(1) }
                var selectedVehicleId by rememberSaveable { mutableStateOf<Int?>(null) }
                val testDriveRepository = remember { TestDriveRepository() }
                val vehicleRepository = remember { VehicleRepository() }
                val contractRepository = remember { ContractRepository(vehicleRepository = vehicleRepository) }
                val quotationRepository = remember { QuotationRepository(vehicleRepository = vehicleRepository) }
                val deliveryRepository = remember { DeliveryRepository(contractRepository) }
                val workshopRepository = remember { WorkshopRepository() }
                val serviceOrderRepository = remember { ServiceOrderRepository(workshopRepository) }
                val maintenanceRepository = remember {
                    MaintenanceReminderRepository(
                        workshopRepository = workshopRepository,
                        serviceOrderRepository = serviceOrderRepository
                    )
                }
                val quotationDeliveryRepository = remember {
                    QuotationDeliveryRepository()
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Catálogo",
                                "Pruebas",
                                "Contratos",
                                "Taller",
                                "Cotizaciones",
                                "Historial",
                                "Mantenimiento"

                            ).forEachIndexed { index, label ->

                                val tabModifier = Modifier.widthIn(min = 90.dp)

                                val tabPadding = PaddingValues(
                                    horizontal = 12.dp,
                                    vertical = 8.dp
                                )

                                if (selectedTab == index) {
                                    Button(
                                        onClick = { selectedTab = index },
                                        modifier = tabModifier,
                                        contentPadding = tabPadding
                                    ) {
                                        Text(label, maxLines = 1)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedTab = index },
                                        modifier = tabModifier,
                                        contentPadding = tabPadding
                                    ) {
                                        Text(label, maxLines = 1)
                                    }
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> CatalogScreen(
                                repository = vehicleRepository,
                                onScheduleTestDrive = { vehicle ->
                                    selectedVehicleId = vehicle.id
                                    selectedTab = 1
                                },
                            )
                            1 -> TestDriveScreen(
                                initialVehicleId = selectedVehicleId,
                                repository = testDriveRepository,
                                onBackToCatalog = { selectedTab = 0 },
                            )

                            2 -> ContractScreen(
                                vehicleRepository = vehicleRepository,
                                repository = contractRepository,
                                deliveryRepository = deliveryRepository,
                            )

                            3 -> WorkshopScreen(
                                repository = workshopRepository,
                                serviceOrderRepository = serviceOrderRepository,
                            )

                            4 -> QuotationScreen(
                                vehicleRepository = vehicleRepository,
                                quotationRepository = quotationRepository,
                                deliveryRepository = quotationDeliveryRepository
                            )

                            5 -> ServiceHistoryScreen(
                                repository = serviceOrderRepository
                            )

                            6 -> MaintenanceReminderScreen(
                                repository = maintenanceRepository,
                                workshopRepository = workshopRepository,
                                serviceOrderRepository = serviceOrderRepository,
                                onScheduleAppointment = {
                                    selectedTab = 3
                                }
                            )

                            else -> CatalogScreen(
                                repository = vehicleRepository,
                                onScheduleTestDrive = { vehicle ->
                                    selectedVehicleId = vehicle.id
                                    selectedTab = 1
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
