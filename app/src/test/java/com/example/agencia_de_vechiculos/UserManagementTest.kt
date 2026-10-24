package com.example.agencia_de_vechiculos

import com.example.agencia_de_vechiculos.data.UserRepository
import com.example.agencia_de_vechiculos.data.defaultSampleLeads
import com.example.agencia_de_vechiculos.data.defaultSampleUsers
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.LeadReassignResult
import com.example.agencia_de_vechiculos.model.LeadStatus
import com.example.agencia_de_vechiculos.model.UserDeactivateResult
import com.example.agencia_de_vechiculos.model.UserRole
import com.example.agencia_de_vechiculos.model.UserSaveResult
import com.example.agencia_de_vechiculos.model.UserStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserManagementTest {

    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        repository = UserRepository(
            initialUsers = defaultSampleUsers,
            initialLeads = defaultSampleLeads,
        )
    }

    @Test
    fun criterio1_formularioAlta_creaUsuarioConRolYSucursal() {
        val result = repository.saveUser(
            id = null,
            name = "María Fernanda Rivas",
            email = "maria.rivas@agencia.com",
            role = UserRole.GERENTE,
            branch = Branch.SUR,
            status = UserStatus.ACTIVO,
        )

        assertTrue(result is UserSaveResult.Success)
        val success = result as UserSaveResult.Success
        assertFalse(success.isEdit)
        assertEquals("María Fernanda Rivas", success.user.name)
        assertEquals("maria.rivas@agencia.com", success.user.email)
        assertEquals(UserRole.GERENTE, success.user.role)
        assertEquals(Branch.SUR, success.user.branch)
        assertEquals(UserStatus.ACTIVO, success.user.status)

        // Verificar que quedó registrado en el repositorio
        val storedUser = repository.userByEmail("maria.rivas@agencia.com")
        assertTrue(storedUser != null)
    }

    @Test
    fun criterio2_desactivarAsesor_pierdeAccesoYLiberaLeadsParaReasignar() {
        // Asesor 1 (Carlos Mendoza) tiene 2 leads asignados inicialmente
        val initialUser = repository.userById(1)!!
        assertTrue(initialUser.isActive)

        val result = repository.deactivateUser(userId = 1)

        assertTrue(result is UserDeactivateResult.Deactivated)
        val deactivated = result as UserDeactivateResult.Deactivated
        assertEquals(UserStatus.INACTIVO, deactivated.user.status)
        assertFalse(deactivated.user.isActive)
        assertEquals(2, deactivated.unassignedLeadsCount)

        // Verificar que los leads de Carlos quedaron sin asesor y pendientes por reasignar
        val unassignedLeads = repository.unassignedLeads()
        assertEquals(2, unassignedLeads.size)
        assertTrue(unassignedLeads.all { it.assignedAdvisorId == null && it.status == LeadStatus.PENDIENTE_REASIGNACION })
    }

    @Test
    fun criterio3_correoYaRegistrado_rechazaCreacion() {
        // carlos.mendoza@agencia.com ya existe en defaultSampleUsers
        val result = repository.saveUser(
            id = null,
            name = "Carlos Clon",
            email = "CARLOS.MENDOZA@AGENCIA.COM", // Verifica insensibilidad a mayúsculas
            role = UserRole.ASESOR,
            branch = Branch.NORTE,
        )

        assertTrue(result is UserSaveResult.EmailAlreadyExists)
        val duplicate = result as UserSaveResult.EmailAlreadyExists
        assertEquals("carlos.mendoza@agencia.com", duplicate.email)
    }

    @Test
    fun criterio3_correoYaRegistrado_rechazaEdicionAEmailDeOtroUsuario() {
        // Intentar cambiar el correo del usuario 2 al correo del usuario 1
        val result = repository.saveUser(
            id = 2,
            name = "Ana Lucía Martínez",
            email = "carlos.mendoza@agencia.com",
            role = UserRole.ASESOR,
            branch = Branch.CENTRO,
        )

        assertTrue(result is UserSaveResult.EmailAlreadyExists)
    }

    @Test
    fun regla_usuarioConHistorial_noSeEliminaSoloSeDesactiva() {
        // El usuario 1 tiene historial (hasHistory = true)
        val result = repository.attemptDeleteUser(userId = 1)

        assertTrue(result is UserDeactivateResult.CannotDeleteHasHistory)
        val cannotDelete = result as UserDeactivateResult.CannotDeleteHasHistory
        assertEquals(UserStatus.INACTIVO, cannotDelete.user.status)

        // El usuario no fue removido del repositorio, pero quedó desactivado
        val userInRepo = repository.userById(1)
        assertTrue(userInRepo != null)
        assertEquals(UserStatus.INACTIVO, userInRepo!!.status)
    }

    @Test
    fun reasignarLead_asignaLeadPendienteANuevoAsesorActivo() {
        // 1. Desactivar asesor 1 para que sus 2 leads queden desasignados
        repository.deactivateUser(userId = 1)

        val unassignedLead = repository.unassignedLeads().first()
        val targetAdvisorId = 2 // Ana Lucía Martínez (Asesor activo)

        // 2. Reasignar lead al asesor 2
        val reassignResult = repository.reassignLead(
            leadId = unassignedLead.id,
            newAdvisorId = targetAdvisorId,
        )

        assertTrue(reassignResult is LeadReassignResult.Reassigned)
        val success = reassignResult as LeadReassignResult.Reassigned
        assertEquals(targetAdvisorId, success.lead.assignedAdvisorId)
        assertEquals("Ana Lucía Martínez", success.lead.assignedAdvisorName)
        assertEquals(LeadStatus.EN_SEGUIMIENTO, success.lead.status)
    }
}
