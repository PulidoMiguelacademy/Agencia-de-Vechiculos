package com.example.agencia_de_vechiculos.ui.user

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agencia_de_vechiculos.data.UserRepository
import com.example.agencia_de_vechiculos.model.AppUser
import com.example.agencia_de_vechiculos.model.Branch
import com.example.agencia_de_vechiculos.model.Lead
import com.example.agencia_de_vechiculos.model.LeadReassignResult
import com.example.agencia_de_vechiculos.model.UserDeactivateResult
import com.example.agencia_de_vechiculos.model.UserRole
import com.example.agencia_de_vechiculos.model.UserSaveResult
import com.example.agencia_de_vechiculos.model.UserStatus
import com.example.agencia_de_vechiculos.ui.common.FeedbackCard

/**
 * Pantalla principal para la Gestión de Usuarios, Asesores y Leads (HU-36)
 */
@Composable
fun UserManagementScreen(
    modifier: Modifier = Modifier,
    repository: UserRepository = remember { UserRepository() },
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) } // 0: Usuarios, 1: Leads por reasignar
    var version by remember { mutableIntStateOf(0) }

    val allUsers = remember(version) { repository.allUsers() }
    val unassignedLeads = remember(version) { repository.unassignedLeads() }
    val activeAdvisors = remember(version) { repository.activeAdvisors() }

    var editingUser by remember { mutableStateOf<AppUser?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var screenMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Gestión de usuarios y asesores",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Alta, edición, desactivación y reasignación de leads • Regla: Correo único",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // --- PESTAÑAS INTERNAS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = {
                        selectedSection = 0
                        screenMessage = null
                    },
                    label = { Text("Usuarios (${allUsers.size})") },
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = {
                        selectedSection = 1
                        screenMessage = null
                    },
                    label = { Text("Leads por reasignar (${unassignedLeads.size})") },
                )
            }
        }

        screenMessage?.let { (text, isError) ->
            item {
                FeedbackCard(message = text, isError = isError)
            }
        }

        // --- SECCIÓN 0: USUARIOS ---
        if (selectedSection == 0) {
            if (!isCreatingNew && editingUser == null) {
                item {
                    Button(
                        onClick = {
                            isCreatingNew = true
                            screenMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("+ Crear nuevo usuario")
                    }
                }
            }

            // Formulario de Alta / Edición
            if (isCreatingNew || editingUser != null) {
                item {
                    UserFormCard(
                        user = editingUser,
                        onSave = { id, name, email, role, branch, status ->
                            val result = repository.saveUser(id, name, email, role, branch, status)
                            when (result) {
                                is UserSaveResult.Success -> {
                                    val action = if (result.isEdit) "actualizado" else "creado"
                                    screenMessage = "Usuario ${result.user.name} $action exitosamente con rol ${result.user.role.label} en ${result.user.branch.label}." to false
                                    isCreatingNew = false
                                    editingUser = null
                                    version++
                                }
                                is UserSaveResult.EmailAlreadyExists -> {
                                    screenMessage = result.message to true
                                }
                                is UserSaveResult.InvalidData -> {
                                    screenMessage = result.message to true
                                }
                            }
                        },
                        onCancel = {
                            isCreatingNew = false
                            editingUser = null
                        },
                    )
                }
            }

            items(allUsers) { user ->
                UserCard(
                    user = user,
                    onEdit = {
                        editingUser = user
                        isCreatingNew = false
                        screenMessage = null
                    },
                    onDeactivate = {
                        val result = repository.deactivateUser(user.id)
                        when (result) {
                            is UserDeactivateResult.Deactivated -> {
                                val leadNote = if (result.unassignedLeadsCount > 0) {
                                    " Sus ${result.unassignedLeadsCount} leads pasaron a 'Pendientes por reasignar'."
                                } else ""
                                screenMessage = "El usuario ${result.user.name} fue desactivado.$leadNote" to false
                                version++
                            }
                            is UserDeactivateResult.CannotDeleteHasHistory -> {
                                screenMessage = result.message to true
                            }
                            is UserDeactivateResult.InvalidData -> {
                                screenMessage = result.message to true
                            }
                        }
                    },
                    onDeleteAttempt = {
                        val result = repository.attemptDeleteUser(user.id)
                        when (result) {
                            is UserDeactivateResult.CannotDeleteHasHistory -> {
                                screenMessage = result.message to true
                                version++
                            }
                            is UserDeactivateResult.Deactivated -> {
                                screenMessage = "El usuario fue desactivado." to false
                                version++
                            }
                            is UserDeactivateResult.InvalidData -> {
                                screenMessage = result.message to true
                            }
                        }
                    },
                )
            }
        } else {
            // --- SECCIÓN 1: LEADS POR REASIGNAR ---
            if (unassignedLeads.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No hay leads pendientes por reasignar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            } else {
                items(unassignedLeads) { lead ->
                    UnassignedLeadCard(
                        lead = lead,
                        activeAdvisors = activeAdvisors,
                        onReassign = { leadId, advisorId ->
                            val result = repository.reassignLead(leadId, advisorId)
                            when (result) {
                                is LeadReassignResult.Reassigned -> {
                                    screenMessage = "El lead de ${result.lead.clientName} fue reasignado al asesor ${result.newAdvisor.name}." to false
                                    version++
                                }
                                is LeadReassignResult.InvalidData -> {
                                    screenMessage = result.message to true
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserFormCard(
    user: AppUser?,
    onSave: (id: Int?, name: String, email: String, role: UserRole, branch: Branch, status: UserStatus) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(user) { mutableStateOf(user?.name.orEmpty()) }
    var email by remember(user) { mutableStateOf(user?.email.orEmpty()) }
    var selectedRole by remember(user) { mutableStateOf(user?.role ?: UserRole.ASESOR) }
    var selectedBranch by remember(user) { mutableStateOf(user?.branch ?: Branch.NORTE) }
    var selectedStatus by remember(user) { mutableStateOf(user?.status ?: UserStatus.ACTIVO) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (user == null) "Nuevo usuario" else "Editar usuario N.º ${user.id}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre completo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Rol del usuario", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                UserRole.entries.forEach { role ->
                    FilterChip(
                        selected = selectedRole == role,
                        onClick = { selectedRole = role },
                        label = { Text(role.label) },
                    )
                }
            }

            Text("Sucursal", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Branch.entries.forEach { branch ->
                    FilterChip(
                        selected = selectedBranch == branch,
                        onClick = { selectedBranch = branch },
                        label = { Text(branch.label) },
                    )
                }
            }

            Text("Estado", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { selectedStatus = status },
                        label = { Text(status.label) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onSave(user?.id, name, email, selectedRole, selectedBranch, selectedStatus) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: AppUser,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onDeleteAttempt: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = user.status.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (user.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text("Correo: ${user.email}", style = MaterialTheme.typography.bodySmall)
            Text("Rol: ${user.role.label} • Sucursal: ${user.branch.label}", style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Text("Editar")
                }
                if (user.isActive) {
                    OutlinedButton(onClick = onDeactivate, modifier = Modifier.weight(1f)) {
                        Text("Desactivar")
                    }
                } else {
                    OutlinedButton(onClick = onDeleteAttempt, modifier = Modifier.weight(1f)) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnassignedLeadCard(
    lead: Lead,
    activeAdvisors: List<AppUser>,
    onReassign: (leadId: Int, advisorId: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedAdvisor by remember { mutableStateOf<AppUser?>(activeAdvisors.firstOrNull()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Lead #${lead.id} • ${lead.clientName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("Teléfono: ${lead.clientPhone} • Vehículo: ${lead.interestedVehicle}", style = MaterialTheme.typography.bodySmall)
            Text("Sede: ${lead.branch.label} • Estado: ${lead.status.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

            if (activeAdvisors.isEmpty()) {
                Text(
                    text = "No hay asesores comerciales activos disponibles para reasignar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text("Asignar a un asesor comercial activo:", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedAdvisor?.name ?: "Seleccionar asesor",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        activeAdvisors.forEach { advisor ->
                            DropdownMenuItem(
                                text = { Text("${advisor.name} (${advisor.branch.label})") },
                                onClick = {
                                    selectedAdvisor = advisor
                                    expanded = false
                                },
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        selectedAdvisor?.let { onReassign(lead.id, it.id) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reasignar asesor")
                }
            }
        }
    }
}
