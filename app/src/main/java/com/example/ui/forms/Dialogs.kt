package com.example.ui.forms

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Favorite
import com.example.domain.model.SportEvent
import com.example.ui.common.tvFocusable
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoRed
import com.example.ui.theme.NexoSurfaceBorder
import com.example.ui.theme.NexoTextSecondary

@Composable
fun EditFavoriteDialog(
    initialFavorite: Favorite?,
    onSave: (title: String, url: String, categoryId: String?, pinned: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialFavorite?.title ?: "") }
    var url by remember { mutableStateOf(initialFavorite?.url ?: "https://") }
    var pinned by remember { mutableStateOf(initialFavorite?.pinned ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialFavorite == null) "Nuevo Sitio Favorito" else "Editar Sitio") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del sitio", color = NexoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("edit_fav_title_input")
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Dirección web (URL)", color = NexoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("edit_fav_url_input")
                )

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = NexoBluePrimary),
                        modifier = Modifier.tvFocusable("edit_fav_pinned_checkbox")
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Fijar al inicio de la lista", color = Color.White, fontSize = 14.sp)
                }

                errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = NexoRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanUrl = url.trim()
                    if (cleanTitle.isBlank()) {
                        errorMessage = "Por favor ingresa un nombre para el sitio"
                        return@Button
                    }
                    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                        errorMessage = "La URL debe iniciar con https:// o http://"
                        return@Button
                    }
                    onSave(cleanTitle, cleanUrl, initialFavorite?.categoryId, pinned)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                modifier = Modifier.tvFocusable("save_favorite_btn")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.tvFocusable("cancel_favorite_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditEventDialog(
    initialEvent: SportEvent?,
    onSave: (title: String, sport: String?, competition: String?, dateText: String?, pageUrl: String, label: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var sport by remember { mutableStateOf(initialEvent?.sport ?: "Fútbol") }
    var competition by remember { mutableStateOf(initialEvent?.competition ?: "") }
    var dateText by remember { mutableStateOf(initialEvent?.originalDateText ?: "Hoy") }
    var pageUrl by remember { mutableStateOf(initialEvent?.links?.firstOrNull()?.pageUrl ?: "https://") }
    var label by remember { mutableStateOf(initialEvent?.links?.firstOrNull()?.label ?: "Transmisión principal") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialEvent == null) "Agregar Evento Manual" else "Editar Evento") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Partido / Evento (ej. Real Madrid vs Barcelona)", color = NexoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("event_title_input")
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = sport,
                        onValueChange = { sport = it },
                        label = { Text("Deporte", color = NexoTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoBlueLight,
                            unfocusedBorderColor = NexoSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable("event_sport_input")
                    )

                    Spacer(Modifier.width(8.dp))

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Horario / Fecha", color = NexoTextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NexoBlueLight,
                            unfocusedBorderColor = NexoSurfaceBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .tvFocusable("event_date_input")
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = competition,
                    onValueChange = { competition = it },
                    label = { Text("Competición / Torneo", color = NexoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("event_comp_input")
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = pageUrl,
                    onValueChange = { pageUrl = it },
                    label = { Text("Enlace a la página", color = NexoTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("event_url_input")
                )

                errorMessage?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = NexoRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanUrl = pageUrl.trim()
                    if (cleanTitle.isBlank()) {
                        errorMessage = "Ingresa el título del evento"
                        return@Button
                    }
                    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                        errorMessage = "La URL del evento debe iniciar con https:// o http://"
                        return@Button
                    }
                    onSave(cleanTitle, sport, competition, dateText, cleanUrl, label)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                modifier = Modifier.tvFocusable("save_event_btn")
            ) {
                Text("Guardar Evento")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.tvFocusable("cancel_event_btn")
            ) {
                Text("Cancelar")
            }
        }
    )
}
