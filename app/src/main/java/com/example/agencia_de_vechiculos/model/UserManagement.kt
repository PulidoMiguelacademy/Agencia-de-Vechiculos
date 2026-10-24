package com.example.agencia_de_vechiculos.model

/**
 * Roles disponibles en el sistema (HU-36)
 */
enum class UserRole(val label: String) {
    ASESOR("Asesor comercial"),
    GERENTE("Gerente de sucursal"),
    ADMINISTRADOR("Administrador"),
    TALLER("Recepción / Técnico taller"),
}

/**
 * Estado del usuario en el sistema
 */
enum class UserStatus(val label: String) {
    ACTIVO("Activo"),
    INACTIVO("Desactivado"),
}

/**
 * Modelo de Usuario / Asesor comercial
 */
data class AppUser(
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole,
    val branch: Branch,
    val status: UserStatus = UserStatus.ACTIVO,
    val hasHistory: Boolean = true,
) {
    val isActive: Boolean
        get() = status == UserStatus.ACTIVO
}

/**
 * Estado de un Lead / Prospecto de cliente
 */
enum class LeadStatus(val label: String) {
    EN_SEGUIMIENTO("En seguimiento"),
    PENDIENTE_REASIGNACION("Pendiente de reasignar"),
    CONVERTIDO("Convertido a venta"),
    CERRADO("Cerrado / Inactivo"),
}

/**
 * Representa un Lead asignado a un asesor comercial
 */
data class Lead(
    val id: Int,
    val clientName: String,
    val clientPhone: String,
    val interestedVehicle: String,
    val branch: Branch,
    val assignedAdvisorId: Int?,
    val assignedAdvisorName: String?,
    val status: LeadStatus = LeadStatus.EN_SEGUIMIENTO,
) {
    val needsReassignment: Boolean
        get() = assignedAdvisorId == null || status == LeadStatus.PENDIENTE_REASIGNACION
}

/**
 * Resultados de la operación de guardar (crear/editar) un usuario
 */
sealed interface UserSaveResult {
    data class Success(val user: AppUser, val isEdit: Boolean) : UserSaveResult
    data class EmailAlreadyExists(val email: String, val message: String = "El correo $email ya está registrado en el sistema.") : UserSaveResult
    data class InvalidData(val message: String) : UserSaveResult
}

/**
 * Resultados de la operación de desactivar/eliminar un usuario
 */
sealed interface UserDeactivateResult {
    data class Deactivated(val user: AppUser, val unassignedLeadsCount: Int) : UserDeactivateResult
    data class CannotDeleteHasHistory(val user: AppUser, val message: String = "Los usuarios con historial no se eliminan, solo se desactivan.") : UserDeactivateResult
    data class InvalidData(val message: String) : UserDeactivateResult
}

/**
 * Resultado de reasignar un lead a un nuevo asesor
 */
sealed interface LeadReassignResult {
    data class Reassigned(val lead: Lead, val newAdvisor: AppUser) : LeadReassignResult
    data class InvalidData(val message: String) : LeadReassignResult
}
