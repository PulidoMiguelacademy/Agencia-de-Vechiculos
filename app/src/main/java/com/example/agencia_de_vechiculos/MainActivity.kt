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
import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.data.VehicleRepository
import com.example.agencia_de_vechiculos.data.WorkshopRepository
import com.example.agencia_de_vechiculos.ui.catalog.CatalogScreen
import com.example.agencia_de_vechiculos.ui.contract.ContractScreen
import com.example.agencia_de_vechiculos.ui.testdrive.TestDriveScreen
import com.example.agencia_de_vechiculos.ui.theme.AgenciadevechiculosTheme
import com.example.agencia_de_vechiculos.ui.workshop.WorkshopScreen

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
                val deliveryRepository = remember { DeliveryRepository(contractRepository) }
                val workshopRepository = remember { WorkshopRepository() }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            listOf("Catálogo", "Pruebas", "Contratos", "Taller").forEachIndexed { index, label ->
                                val tabModifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                val tabPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                if (selectedTab == index) {
                                    Button(onClick = { selectedTab = index }, modifier = tabModifier, contentPadding = tabPadding) {
                                        Text(label, textAlign = TextAlign.Center, maxLines = 1)
                                    }
                                } else {
                                    OutlinedButton(onClick = { selectedTab = index }, modifier = tabModifier, contentPadding = tabPadding) {
                                        Text(label, textAlign = TextAlign.Center, maxLines = 1)
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
                            else -> WorkshopScreen(repository = workshopRepository)
                        }
                    }
                }
            }
        }
    }
}
