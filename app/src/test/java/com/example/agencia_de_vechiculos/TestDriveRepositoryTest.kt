package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.TestDriveRepository
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.TestDriveAppointment
import com.example.agencia_de_vechiculos.model.TestDriveBookingResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestDriveRepositoryTest {

    @Test
    fun criterio1_horarioLibre_confirmaCitaYEnviaCorreo() {
        val repository = TestDriveRepository(initialAppointments = emptyList())

        val result = repository.schedule(
            clientName = "Andrés Gómez",
            phone = "3001112233",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "09:00",
        )

        assertTrue(result is TestDriveBookingResult.Confirmed)
        val confirmed = result as TestDriveBookingResult.Confirmed
        assertEquals("Andrés Gómez", confirmed.appointment.clientName)
        assertEquals("3001112233", confirmed.appointment.phone)
        assertEquals(1, confirmed.appointment.vehicleId)
        assertEquals(Branch.NORTE, confirmed.appointment.branch)
        assertEquals("2026-10-25", confirmed.appointment.date)
        assertEquals("09:00", confirmed.appointment.time)
        assertEquals(30, confirmed.appointment.durationMinutes)
        assertTrue(confirmed.confirmationEmail.contains("Correo de confirmación enviado"))
    }

    @Test
    fun criterio2_horarioOcupado_ofreceOtrosHorariosLibres() {
        val repository = TestDriveRepository(initialAppointments = emptyList())
        repository.schedule(
            clientName = "Cliente Uno",
            phone = "3001111111",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "10:00",
        )

        val result = repository.schedule(
            clientName = "Cliente Dos",
            phone = "3002222222",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "10:00",
        )

        assertTrue(result is TestDriveBookingResult.SlotOccupied)
        val occupied = result as TestDriveBookingResult.SlotOccupied
        assertEquals("10:00", occupied.requestedTime)
        assertFalse(occupied.suggestedTimes.contains("10:00"))
        assertTrue(occupied.suggestedTimes.isNotEmpty())
        assertTrue(occupied.suggestedTimes.contains("09:30"))
        assertTrue(occupied.suggestedTimes.contains("10:30"))
    }

    @Test
    fun regla_maximoDosPruebasActivasPorCliente() {
        val repository = TestDriveRepository(initialAppointments = emptyList())

        val first = repository.schedule(
            clientName = "Mariana Restrepo",
            phone = "3150001122",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "09:00",
        )
        val second = repository.schedule(
            clientName = "Mariana Restrepo",
            phone = "3150001122",
            vehicleId = 2,
            branch = Branch.SUR,
            date = "2026-10-26",
            time = "11:00",
        )
        val third = repository.schedule(
            clientName = "Mariana Restrepo",
            phone = "3150001122",
            vehicleId = 3,
            branch = Branch.CENTRO,
            date = "2026-10-27",
            time = "14:00",
        )

        assertTrue(first is TestDriveBookingResult.Confirmed)
        assertTrue(second is TestDriveBookingResult.Confirmed)
        assertTrue(third is TestDriveBookingResult.MaxActiveReached)
        assertEquals(
            TestDriveAppointment.MAX_ACTIVE_PER_CLIENT,
            (third as TestDriveBookingResult.MaxActiveReached).maxAllowed,
        )
    }

    @Test
    fun cancelarCitaActiva_permiteAgendarUnaNuevaAlMismoCliente() {
        val repository = TestDriveRepository(initialAppointments = emptyList())

        val first = repository.schedule(
            clientName = "Daniel",
            phone = "3209998877",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "09:00",
        ) as TestDriveBookingResult.Confirmed

        repository.schedule(
            clientName = "Daniel",
            phone = "3209998877",
            vehicleId = 2,
            branch = Branch.CENTRO,
            date = "2026-10-25",
            time = "10:30",
        )

        repository.cancelAppointment(first.appointment.id)

        val afterCancel = repository.schedule(
            clientName = "Daniel",
            phone = "3209998877",
            vehicleId = 3,
            branch = Branch.SUR,
            date = "2026-10-26",
            time = "15:00",
        )

        assertTrue(afterCancel is TestDriveBookingResult.Confirmed)
    }

    @Test
    fun datosObligatoriosVacios_retornaInvalidData() {
        val repository = TestDriveRepository(initialAppointments = emptyList())

        val sinNombre = repository.schedule(
            clientName = "   ",
            phone = "3001234567",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "09:00",
        )
        val telefonoCorto = repository.schedule(
            clientName = "Pedro",
            phone = "123",
            vehicleId = 1,
            branch = Branch.NORTE,
            date = "2026-10-25",
            time = "09:00",
        )

        assertTrue(sinNombre is TestDriveBookingResult.InvalidData)
        assertTrue(telefonoCorto is TestDriveBookingResult.InvalidData)
    }
}
