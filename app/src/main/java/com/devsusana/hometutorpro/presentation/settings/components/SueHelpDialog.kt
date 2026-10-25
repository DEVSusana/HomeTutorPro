package com.devsusana.hometutorpro.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Educational dialog displaying supported voice commands and examples for SUE.
 */
@Composable
fun SueHelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guía de Ayuda de SUE") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "SUE es tu asistente inteligente local. Puedes pedirle que realice tareas mediante comandos de voz coloquiales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Consejo: En los comandos de mover, cancelar o añadir clases, sustituye los nombres de ejemplo (Pepe, Ana) por los alumnos que tengas registrados en tu agenda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Ejemplos de Comandos Soportados:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                HelpExampleItem(
                    intent = "Crear Alumno",
                    examples = listOf(
                        "\"Crea un alumno llamado Pepe a 15 la hora\"",
                        "\"Añade un estudiante llamado Ana de bachillerato a 15 la hora\""
                    )
                )
                HelpExampleItem(
                    intent = "Editar Alumno (Dirección, Portero y Mudanzas)",
                    examples = listOf(
                        "\"Pepe se ha mudado a Calle Mayor 12\"",
                        "\"Añade a Hugo en su dirección el código de portero 387\"",
                        "\"El portero de Ana es el 14B\"",
                        "\"Cambia la dirección de Carlos a Avenida de la Paz 8\""
                    )
                )
                HelpExampleItem(
                    intent = "Editar Alumno (Tarifas, Teléfono y Curso)",
                    examples = listOf(
                        "\"Cambia el precio de Carlos a 18 la hora\"",
                        "\"Ponle la tarifa a Ana a 15 euros\"",
                        "\"El nuevo teléfono de Carlos es 612345678\"",
                        "\"Cambia el curso de Pepe a 2º de Bachillerato\""
                    )
                )
                HelpExampleItem(
                    intent = "Notas Pedagógicas del Alumno",
                    examples = listOf(
                        "\"Pon una nota en Carlos que diga repasar formulación\"",
                        "\"Añade una nota a Ana: examen el jueves\""
                    )
                )
                HelpExampleItem(
                    intent = "Añadir Clases y Refuerzos (Pon, Añade)",
                    examples = listOf(
                        "\"Pon una clase a Pepe el viernes a las 5\"",
                        "\"Ponle a Ana una clase extra mañana\"",
                        "\"Añade una clase extra para Carlos el jueves a las 17:00\""
                    )
                )
                HelpExampleItem(
                    intent = "Mover o Reprogramar Clases (Mueve, Cambia, Pasa)",
                    examples = listOf(
                        "\"Mueve la clase de Pepe del lunes al viernes\"",
                        "\"Muévele la clase a Ana a las 5 de la tarde\"",
                        "\"Cambia a Carlos para el domingo a las 18:00\"",
                        "\"Reprograma la clase de Ana de hoy para mañana\""
                    )
                )
                HelpExampleItem(
                    intent = "Cancelar o Quitar Clases (Quita, Cancela, Anula)",
                    examples = listOf(
                        "\"Quita la clase de Pepe de este viernes\"",
                        "\"Quítale la clase a Ana de hoy\"",
                        "\"Cancela la clase de Carlos de este martes\"",
                        "\"Borra el horario de Ana los martes\""
                    )
                )
                HelpExampleItem(
                    intent = "Consultar Horario",
                    examples = listOf(
                        "\"¿Qué clases tengo el viernes?\"",
                        "\"¿Qué clase tengo hoy a las 18:00?\"",
                        "\"¿Cuál es mi siguiente clase?\""
                    )
                )
                HelpExampleItem(
                    intent = "Consultar Huecos Libres",
                    examples = listOf(
                        "\"¿Tengo huecos libres hoy?\"",
                        "\"¿Qué huecos libres tengo esta semana?\""
                    )
                )
                HelpExampleItem(
                    intent = "Consultar Clases Canceladas",
                    examples = listOf(
                        "\"¿Qué clases tengo canceladas hoy?\"",
                        "\"¿Tengo alguna clase cancelada el jueves?\""
                    )
                )
                HelpExampleItem(
                    intent = "Pagos y Deudas",
                    examples = listOf(
                        "\"Pepe me ha pagado 30 euros en efectivo\"",
                        "\"Súmale 20 euros a la deuda de Ana\""
                    )
                )
                HelpExampleItem(
                    intent = "Iniciar Clases",
                    examples = listOf(
                        "\"Inicia una clase con Pepe de 60 minutos\""
                    )
                )
                HelpExampleItem(
                    intent = "Materiales y Recursos Compartidos",
                    examples = listOf(
                        "\"¿Qué material le he enviado a Carlos?\"",
                        "\"¿Qué recursos he compartido recientemente?\""
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendido")
            }
        }
    )
}

@Composable
private fun HelpExampleItem(intent: String, examples: List<String>) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = intent,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        examples.forEach { example ->
            Text(
                text = example,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)
            )
        }
    }
}
