package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.EventType
import com.example.domain.model.SportEvent
import com.example.ui.NexoMainViewModel
import com.example.ui.common.SportBadge
import com.example.ui.common.tvFocusable
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoRed
import com.example.ui.theme.NexoSurfaceBorder
import com.example.ui.theme.NexoSurfaceCard
import com.example.ui.theme.NexoSurfaceElevated
import com.example.ui.theme.NexoTextSecondary

@Composable
fun TvAgendaScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.agendaEvents.collectAsState()
    val searchQuery by viewModel.agendaQuery.collectAsState()
    val selectedSport by viewModel.selectedSportFilter.collectAsState()

    var eventWithLinksToPick by remember { mutableStateOf<SportEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<SportEvent?>(null) }

    val sportsList = listOf("Todos", "Fútbol", "Básquet", "Tenis", "Automovilismo", "Combate")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.agendaQuery.value = it },
                placeholder = { Text("Buscar partidos o competiciones...", color = NexoTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = NexoBlueLight) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NexoBlueLight,
                    unfocusedBorderColor = NexoSurfaceBorder,
                    focusedContainerColor = NexoSurfaceCard,
                    unfocusedContainerColor = NexoSurfaceCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .width(380.dp)
                    .tvFocusable("agenda_search_input")
            )

            Row {
                Button(
                    onClick = { viewModel.refreshAllAgendaSources() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                    modifier = Modifier.tvFocusable("refresh_agenda_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Actualizar Fuentes")
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { viewModel.createNewEvent() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                    modifier = Modifier.tvFocusable("add_manual_event_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar Manual")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Sports Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sportsList.forEach { sportName ->
                val isSelected = (sportName == "Todos" && selectedSport == null) || (selectedSport == sportName)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.selectedSportFilter.value = if (sportName == "Todos") null else sportName
                    },
                    label = { Text(sportName, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NexoBluePrimary,
                        selectedLabelColor = Color.White,
                        containerColor = NexoSurfaceCard,
                        labelColor = NexoTextSecondary
                    ),
                    modifier = Modifier.tvFocusable("sport_chip_$sportName")
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Events List
        if (events.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay eventos disponibles en este filtro", color = NexoTextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.refreshAllAgendaSources() },
                        modifier = Modifier.tvFocusable("empty_refresh_btn")
                    ) {
                        Text("Consultar fuentes públicas ahora")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events, key = { it.id }) { event ->
                    TvEventCard(
                        event = event,
                        onOpenLink = { url, title -> viewModel.openBrowser(url, title) },
                        onPickLinks = { eventWithLinksToPick = event },
                        onDelete = { eventToDelete = event }
                    )
                }
            }
        }
    }

    // Multiple Links Picker Dialog
    eventWithLinksToPick?.let { evt ->
        AlertDialog(
            onDismissRequest = { eventWithLinksToPick = null },
            title = { Text(evt.title) },
            text = {
                Column {
                    Text("Selecciona una de las páginas disponibles:", color = NexoTextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    evt.links.forEachIndexed { index, link ->
                        Button(
                            onClick = {
                                viewModel.openBrowser(link.pageUrl, evt.title)
                                eventWithLinksToPick = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .tvFocusable("pick_link_${evt.id}_$index")
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(link.label ?: "Opción ${index + 1}", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { eventWithLinksToPick = null },
                    modifier = Modifier.tvFocusable("close_pick_links_btn")
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Delete confirmation for manual events
    eventToDelete?.let { evt ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Eliminar evento") },
            text = { Text("¿Deseas eliminar '${evt.title}' de la agenda?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(evt.id)
                        eventToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoRed),
                    modifier = Modifier.tvFocusable("confirm_delete_event_btn")
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { eventToDelete = null },
                    modifier = Modifier.tvFocusable("cancel_delete_event_btn")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TvEventCard(
    event: SportEvent,
    onOpenLink: (url: String, title: String) -> Unit,
    onPickLinks: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable("event_card_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SportBadge(sport = event.sport ?: "Deportes")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = event.competition ?: "Transmisión en vivo",
                        fontSize = 12.sp,
                        color = NexoTextSecondary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "•  ${event.originalDateText ?: "Inicio previsto"}",
                        fontSize = 12.sp,
                        color = NexoBlueLight
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = event.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (event.links.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    val sourcesSummary = event.links.mapNotNull { it.label }.distinct().joinToString(", ")
                    if (sourcesSummary.isNotBlank()) {
                        Text(
                            text = "Fuentes: $sourcesSummary",
                            fontSize = 12.sp,
                            color = NexoTextSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (event.links.size > 1) {
                    Button(
                        onClick = onPickLinks,
                        colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                        modifier = Modifier.tvFocusable("open_links_btn_${event.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver enlaces (${event.links.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (event.links.isNotEmpty()) {
                    val link = event.links.first()
                    Button(
                        onClick = { onOpenLink(link.pageUrl, event.title) },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                        modifier = Modifier.tvFocusable("open_single_link_btn_${event.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir transmisión", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (event.eventType == EventType.MANUAL) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.tvFocusable("delete_manual_event_${event.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexoRed)
                    }
                }
            }
        }
    }
}
