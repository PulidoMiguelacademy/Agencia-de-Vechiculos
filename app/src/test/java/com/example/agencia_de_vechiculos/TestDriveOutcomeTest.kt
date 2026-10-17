package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.InterestLevel
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import com.example.agencia_de_vechiculos.model.TestDriveCompleteResult
import com.example.agencia_de_vechiculos.model.TestDriveStartResult
import com.example.agencia_de_vechiculos.model.TestDriveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestDriveOutcomeTest {

    @Test
    fun criterio1_citaDelDiaConLicenciaVigente_permiteIniciarLaPrueba() {
        val repository = TestDriveRepository(initialAppointments = emptyList())
        val booked = repository.schedule(
            clientName = "Camilo Vargas",
            phone = "3005556677",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-20",
            time = "09:00",
        ) as TestDriveBookingResult.Confirmed

        val startResult = repository.startTestDrive(
            appointmentId = booked.appointment.id,
            licenseNumber = "LIC-889900",
            licenseExpirationDate = "2028-12-31",
            currentDate = "2026-10-20",
        )

        assertTrue(startResult is TestDriveStartResult.Started)
        val started = (startResult as TestDriveStartResult.Started).appointment
        assertEquals(TestDriveStatus.EN_CURSO, started.status)
        assertEquals("LIC-889900", started.licenseNumber)
        assertEquals("2028-12-31", started.licenseExpirationDate)
    }

    @Test
    fun criterio2_licenciaVencida_noPermiteIniciarLaPrueba() {
        val repository = TestDriveRepository(initialAppointments = emptyList())
        val booked = repository.schedule(
            clientName = "Sofía Castro",
            phone = "3112223344",
            vehicleId = 2,
            branch = Branch.CENTRO,
            date = "2026-10-20",
            time = "10:00",
        ) as TestDriveBookingResult.Confirmed

        val startResult = repository.startTestDrive(
            appointmentId = booked.appointment.id,
            licenseNumber = "LIC-111222",
            licenseExpirationDate = "2025-05-10",
            currentDate = "2026-10-20",
        )

        assertTrue(startResult is TestDriveStartResult.LicenseExpired)
        val expired = startResult as TestDriveStartResult.LicenseExpired
        assertEquals("LIC-111222", expired.licenseNumber)
        assertEquals("2025-05-10", expired.expirationDate)
        assertTrue(expired.message.contains("Sin licencia vigente no hay prueba"))
        // Verifica que la cita sigue en estado ACTIVA y no inició
        assertEquals(
            TestDriveStatus.ACTIVA,
            repository.allAppointments().first { it.id == booked.appointment.id }.status,
        )
    }

    @Test
    fun criterio3_alTerminarLaPruebaYRegistrarNivelDeInteres_programaSeguimiento() {
        val repository = TestDriveRepository(initialAppointments = emptyList())
        val booked = repository.schedule(
            clientName = "Juan Pablo",
            phone = "3204445566",
            vehicleId = 5,
            branch = Branch.SUR,
            date = "2026-10-20",
            time = "14:00",
        ) as TestDriveBookingResult.Confirmed

        repository.startTestDrive(
            appointmentId = booked.appointment.id,
            licenseNumber = "LIC-777888",
            licenseExpirationDate = "2029-06-30",
            currentDate = "2026-10-20",
        )

        val completeResult = repository.completeTestDrive(
            appointmentId = booked.appointment.id,
            distanceKm = 12,
            interestLevel = InterestLevel.ALTO,
            observations = "Le gustó mucho la respuesta del motor híbrido y el espacio interior",
        )

        assertTrue(completeResult is TestDriveCompleteResult.Completed)
        val completed = completeResult as TestDriveCompleteResult.Completed
        assertEquals(TestDriveStatus.COMPLETADA, completed.appointment.status)
        assertEquals("LIC-777888", completed.outcome.licenseNumber)
        assertEquals("2029-06-30", completed.outcome.licenseExpirationDate)
        assertEquals(12, completed.outcome.distanceKm)
        assertEquals(InterestLevel.ALTO, completed.outcome.interestLevel)
        assertEquals(
            "Le gustó mucho la respuesta del motor híbrido y el espacio interior",
            completed.outcome.observations,
        )
        assertTrue(completed.outcome.scheduledFollowUp.contains("Seguimiento programado"))
        assertNotNull(repository.outcomeFor(booked.appointment.id))
    }

    @Test
    fun regla_sinLicenciaVigente_noPermiteRegistrarResultadoFinal() {
        val repository = TestDriveRepository(initialAppointments = emptyList())
        val booked = repository.schedule(
            clientName = "Luis Torres",
            phone = "3007778899",
            vehicleId = 3,
            branch = Branch.NORTE,
            date = "2026-10-20",
            time = "15:00",
        ) as TestDriveBookingResult.Confirmed

        // Intenta finalizar sin haber validado licencia vigente primero
        val result = repository.completeTestDrive(
            appointmentId = booked.appointment.id,
            distanceKm = 8,
            interestLevel = InterestLevel.MEDIO,
            observations = "Intento sin licencia validada",
        )

        assertTrue(result is TestDriveCompleteResult.LicenseNotValidated)
    }
}
