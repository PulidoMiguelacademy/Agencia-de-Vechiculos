package com.example.agencia_de_vechiculos.data

import com.example.agencia_de_vechiculos.model.AppUser
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.Lead
import com.example.agencia_de_vechiculos.model.LeadReassignResult
import com.example.agencia_de_vechiculos.model.LeadStatus
import com.example.agencia_de_vechiculos.model.UserDeactivateResult
import com.example.agencia_de_vechiculos.model.UserRole
import com.example.agencia_de_vechiculos.model.UserSaveResult
import com.example.agencia_de_vechiculos.model.UserStatus

/**
 * Repositorio para la gestión de usuarios, asesores y reasignación de leads (HU-36)
 * 
 * Reglas:
 * - El correo es único en el sistema.
 * - Los usuarios con historial no se eliminan, solo se desactivan.
 * - Al desactivar un asesor, sus leads activos quedan pendientes por reasignar.
 */
class UserRepository(
    initialUsers: List<AppUser> = defaultSampleUsers,
    initialLeads: List<Lead> = defaultSampleLeads,
) {
    private val users = initialUsers.toMutableList()
    private val leads = initialLeads.toMutableList()

    private var nextUserId = (users.maxOfOrNull { it.id } ?: 0) + 1
    private var nextLeadId = (leads.maxOfOrNull { it.id } ?: 0) + 1

    /**
     * Devuelve todos los usuarios
     */
    fun allUsers(): List<AppUser> = users.toList()

    /**
     * Devuelve los usuarios activos
     */
    fun activeUsers(): List<AppUser> = users.filter { it.isActive }

    /**
     * Devuelve los asesores comerciales activos
     */
    fun activeAdvisors(): List<AppUser> = users.filter { it.role == UserRole.ASESOR && it.isActive }

    /**
     * Busca usuario por ID
     */
    fun userById(id: Int): AppUser? = users.firstOrNull { it.id == id }

    /**
     * Busca usuario por Correo
     */
    fun userByEmail(email: String): AppUser? = users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }

    /**
     * Devuelve todos los leads
     */
    fun allLeads(): List<Lead> = leads.toList()

    /**
     * Devuelve los leads que están pendientes por reasignar a un asesor
     */
    fun unassignedLeads(): List<Lead> = leads.filter { it.needsReassignment }

    /**
     * Crea o edita un usuario (HU-36 Criterio 1 y Criterio 3)
     */
    fun saveUser(
        id: Int?,
        name: String,
        email: String,
        role: UserRole,
        branch: Branch,
        status: UserStatus = UserStatus.ACTIVO,
    ): UserSaveResult {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isEmpty()) {
            return UserSaveResult.InvalidData("Ingresa el nombre del usuario.")
        }

        if (cleanEmail.isEmpty() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return UserSaveResult.InvalidData("Ingresa un correo electrónico válido (ej. usuario@agencia.com).")
        }

        // Criterio 3 & Regla: El correo es único
        val duplicateEmailUser = users.firstOrNull {
            it.email.equals(cleanEmail, ignoreCase = true) && it.id != id
        }

        if (duplicateEmailUser != null) {
            return UserSaveResult.EmailAlreadyExists(cleanEmail)
        }

        if (id == null) {
            // Criterio 1: Crear nuevo usuario con su rol y sucursal
            val newUser = AppUser(
                id = nextUserId++,
                name = cleanName,
                email = cleanEmail,
                role = role,
                branch = branch,
                status = status,
                hasHistory = false,
            )
            users.add(newUser)
            return UserSaveResult.Success(newUser, isEdit = false)
        } else {
            // Editar usuario existente
            val index = users.indexOfFirst { it.id == id }
            if (index == -1) {
                return UserSaveResult.InvalidData("El usuario especificado no existe.")
            }

            val current = users[index]
            val updatedUser = current.copy(
                name = cleanName,
                email = cleanEmail,
                role = role,
                branch = branch,
                status = status,
            )
            users[index] = updatedUser

            // Criterio 2: Si pasa a inactivo, se reasignan sus leads
            if (current.isActive && status == UserStatus.INACTIVO) {
                unassignLeadsForAdvisor(id)
            }

            return UserSaveResult.Success(updatedUser, isEdit = true)
        }
    }

    /**
     * Desactiva un usuario (HU-36 Criterio 2)
     */
    fun deactivateUser(userId: Int): UserDeactivateResult {
        val index = users.indexOfFirst { it.id == userId }
        if (index == -1) {
            return UserDeactivateResult.InvalidData("El usuario especificado no existe.")
        }

        val user = users[index]
        if (user.status == UserStatus.INACTIVO) {
            return UserDeactivateResult.InvalidData("El usuario ${user.name} ya se encuentra desactivado.")
        }

        val deactivatedUser = user.copy(status = UserStatus.INACTIVO)
        users[index] = deactivatedUser

        // Criterio 2: Si es un asesor que se va, sus leads quedan por reasignar
        val unassignedCount = unassignLeadsForAdvisor(userId)

        return UserDeactivateResult.Deactivated(deactivatedUser, unassignedCount)
    }

    /**
     * Intenta eliminar un usuario respetando la regla:
     * "Los usuarios con historial no se eliminan, solo se desactivan."
     */
    fun attemptDeleteUser(userId: Int): UserDeactivateResult {
        val user = userById(userId) ?: return UserDeactivateResult.InvalidData("El usuario especificado no existe.")

        if (user.hasHistory) {
            // Regla: No eliminar, solo desactivar
            deactivateUser(userId)
            return UserDeactivateResult.CannotDeleteHasHistory(
                user = user.copy(status = UserStatus.INACTIVO),
                message = "El usuario ${user.name} tiene historial en el sistema y no se puede eliminar. Se ha cambiado su estado a Desactivado.",
            )
        }

        // Si es un usuario nuevo sin historial, se remueve
        users.removeIf { it.id == userId }
        val unassignedCount = unassignLeadsForAdvisor(userId)
        return UserDeactivateResult.Deactivated(user.copy(status = UserStatus.INACTIVO), unassignedCount)
    }

    /**
     * Reasigna un lead a un nuevo asesor comercial activo
     */
    fun reassignLead(leadId: Int, newAdvisorId: Int): LeadReassignResult {
        val leadIndex = leads.indexOfFirst { it.id == leadId }
        if (leadIndex == -1) {
            return LeadReassignResult.InvalidData("El lead especificado no existe.")
        }

        val newAdvisor = userById(newAdvisorId)
        if (newAdvisor == null || !newAdvisor.isActive || newAdvisor.role != UserRole.ASESOR) {
            return LeadReassignResult.InvalidData("Selecciona un asesor comercial activo para reasignar.")
        }

        val lead = leads[leadIndex]
        val updatedLead = lead.copy(
            assignedAdvisorId = newAdvisor.id,
            assignedAdvisorName = newAdvisor.name,
            status = LeadStatus.EN_SEGUIMIENTO,
        )
        leads[leadIndex] = updatedLead

        return LeadReassignResult.Reassigned(updatedLead, newAdvisor)
    }

    /**
     * Desvincula los leads activos de un asesor desactivado
     */
    private fun unassignLeadsForAdvisor(advisorId: Int): Int {
        var count = 0
        leads.forEachIndexed { index, lead ->
            if (lead.assignedAdvisorId == advisorId) {
                leads[index] = lead.copy(
                    assignedAdvisorId = null,
                    assignedAdvisorName = null,
                    status = LeadStatus.PENDIENTE_REASIGNACION,
                )
                count++
            }
        }
        return count
    }
}

/**
 * Usuarios iniciales de ejemplo
 */
val defaultSampleUsers = listOf(
    AppUser(1, "Carlos Mendoza", "carlos.mendoza@agencia.com", UserRole.ASESOR, Branch.NORTE, UserStatus.ACTIVO, hasHistory = true),
    AppUser(2, "Ana Lucía Martínez", "ana.martinez@agencia.com", UserRole.ASESOR, Branch.CENTRO, UserStatus.ACTIVO, hasHistory = true),
    AppUser(3, "Roberto Gómez", "roberto.gomez@agencia.com", UserRole.ASESOR, Branch.SUR, UserStatus.ACTIVO, hasHistory = true),
    AppUser(4, "Juan Pérez", "juan.perez@agencia.com", UserRole.GERENTE, Branch.NORTE, UserStatus.ACTIVO, hasHistory = true),
)

/**
 * Leads iniciales de ejemplo
 */
val defaultSampleLeads = listOf(
    Lead(1, "Pedro Morales", "3101234567", "Chevrolet Onix", Branch.NORTE, 1, "Carlos Mendoza", LeadStatus.EN_SEGUIMIENTO),
    Lead(2, "Sonia López", "3119876543", "Toyota Corolla", Branch.NORTE, 1, "Carlos Mendoza", LeadStatus.EN_SEGUIMIENTO),
    Lead(3, "Gabriel Castro", "3125554433", "Chevrolet Tracker", Branch.CENTRO, 2, "Ana Lucía Martínez", LeadStatus.EN_SEGUIMIENTO),
)
