package com.example.agencia_de_vechiculos.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 
 * la tarjeta que muestra el resultado de una acción, con los mismos colores que
 * usa el equipo en la prueba de manejo: el color principal suave
 * (primaryContainer) si salió bien y rojo claro (errorContainer) si hubo un
 * problema
 * 
 * los colores salen del tema de la app, así cambian solos con el modo oscuro y
 * con el color del teléfono. El título es opcional y va en negrita; sin título,
 * el mensaje va en semi-negrita. Debajo del mensaje se puede poner contenido
 * extra (content), por ejemplo los botones con otros horarios
 * 
 */
@Composable
fun FeedbackCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    /**
     * 
     * el color de fondo y el del texto, según si es un error o no
     * 
     */
    val containerColor = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (title == null) FontWeight.SemiBold else null,
                color = contentColor,
            )
            content()
        }
    }
}
