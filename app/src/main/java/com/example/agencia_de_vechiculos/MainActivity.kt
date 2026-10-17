package com.example.agencia_de_vechiculos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.ui.catalog.CatalogScreen
import com.example.agencia_de_vechiculos.ui.testdrive.TestDriveScreen
import com.example.agencia_de_vechiculos.ui.theme.AgenciadevechiculosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AgenciadevechiculosTheme {
                var showTestDrive by rememberSaveable { mutableStateOf(true) }
                var selectedVehicleId by rememberSaveable { mutableStateOf<Int?>(null) }
                val testDriveRepository = remember { TestDriveRepository() }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            if (!showTestDrive) {
                                Button(
                                    onClick = { showTestDrive = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp),
                                ) {
                                    Text("Catálogo")
                                }
                                OutlinedButton(
                                    onClick = { showTestDrive = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp),
                                ) {
                                    Text("Prueba de manejo")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showTestDrive = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp),
                                ) {
                                    Text("Catálogo")
                                }
                                Button(
                                    onClick = { showTestDrive = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp),
                                ) {
                                    Text("Prueba de manejo")
                                }
                            }
                        }

                        if (showTestDrive) {
                            TestDriveScreen(
                                initialVehicleId = selectedVehicleId,
                                repository = testDriveRepository,
                                onBackToCatalog = { showTestDrive = false },
                            )
                        } else {
                            CatalogScreen(
                                onScheduleTestDrive = { vehicle ->
                                    selectedVehicleId = vehicle.id
                                    showTestDrive = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
