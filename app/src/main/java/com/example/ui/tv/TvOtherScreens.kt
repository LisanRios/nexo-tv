package com.example.ui.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AgendaSource
import com.example.domain.model.AgendaSourceStatus
import com.example.ui.NexoMainViewModel
import com.example.ui.common.tvFocusable
import com.example.ui.theme.NexoAmber
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoEmerald
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoRed
import com.example.ui.theme.NexoSurfaceBorder
import com.example.ui.theme.NexoSurfaceCard
import com.example.ui.theme.NexoSurfaceElevated
import com.example.ui.theme.NexoTextSecondary

@Composable
fun TvSourcesScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val sources by viewModel.agendaSources.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Fuentes de Agenda y Estado de Conectividad", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Estado transparente según inspección y disponibilidad real", fontSize = 13.sp, color = NexoTextSecondary)
            }
            Button(
                onClick = { viewModel.refreshAllAgendaSources() },
                colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                modifier = Modifier.tvFocusable("refresh_all_sources_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Probar Todas")
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sources, key = { it.id }) { src ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("source_card_${src.id}"),
                    colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
                    shape = RoundedCornerShape(12.dp)
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
                                Text(src.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(10.dp))
                                SourceStatusBadge(src.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(src.baseUrl, fontSize = 13.sp, color = NexoBlueLight)
                            src.technicalMessage?.let { msg ->
                                Spacer(Modifier.height(4.dp))
                                Text(msg, fontSize = 12.sp, color = NexoTextSecondary)
                            }
                        }

                        if (src.adapterId != null) {
                            Button(
                                onClick = { viewModel.refreshSingleSource(src.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                                modifier = Modifier.tvFocusable("check_source_${src.id}")
                            ) {
                                Text("Probar", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceStatusBadge(status: AgendaSourceStatus) {
    val (color, text) = when (status) {
        AgendaSourceStatus.AVAILABLE -> Pair(NexoEmerald, "Disponible")
        AgendaSourceStatus.UPDATING -> Pair(NexoBlueLight, "Consultando…")
        AgendaSourceStatus.PENDING_IMPLEMENTATION -> Pair(NexoAmber, "Pendiente")
        AgendaSourceStatus.ACCESS_ERROR -> Pair(NexoRed, "Error de Acceso")
        AgendaSourceStatus.NO_EVENTS -> Pair(Color.Gray, "Sin Eventos")
        AgendaSourceStatus.FORMAT_CHANGED -> Pair(NexoAmber, "Formato Cambiado")
        AgendaSourceStatus.OUTDATED -> Pair(NexoAmber, "Desactualizado")
        AgendaSourceStatus.NOT_CONFIGURED -> Pair(Color.Gray, "Sin Configurar")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TvTransferScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    var compactInput by remember { mutableStateOf("") }
    val preview by viewModel.importPreview.collectAsState()
    val statusMessage by viewModel.transferStatusMessage.collectAsState()
    var exportJsonText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text("Transferencia de Sitios y Favoritos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Importa desde el móvil o exporta tus sitios mediante código compacto para televisor", fontSize = 13.sp, color = NexoTextSecondary)

        Spacer(Modifier.height(20.dp))

        // Import Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("1. Importar paquete compacto (teclado remoto o pegar)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = compactInput,
                    onValueChange = { compactInput = it },
                    placeholder = { Text("Pega o escribe aquí el código NEXO:...", color = NexoTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NexoBlueLight,
                        unfocusedBorderColor = NexoSurfaceBorder,
                        focusedContainerColor = NexoNavyDark,
                        unfocusedContainerColor = NexoNavyDark,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusable("compact_transfer_input")
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (compactInput.isNotBlank()) {
                            viewModel.processCompactText(compactInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                    modifier = Modifier.tvFocusable("process_compact_import_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Procesar Código de Importación")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Export Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("2. Exportar datos actuales", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Row {
                    Button(
                        onClick = {
                            exportJsonText = viewModel.generateExportPackage()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                        modifier = Modifier.tvFocusable("generate_export_btn")
                    ) {
                        Text("Generar Copia de Respaldo")
                    }
                }

                exportJsonText?.let { txt ->
                    Spacer(Modifier.height(12.dp))
                    Text("Código compacto para copiar/transferir al móvil u otro TV:", fontSize = 12.sp, color = NexoTextSecondary)
                    viewModel.compactExportText.value?.let { cpt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NexoNavyDark, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(cpt, color = NexoBlueLight, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            Spacer(Modifier.height(14.dp))
            Text(msg, color = NexoEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Import Preview Confirmation Dialog
    preview?.let { prev ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImportPreview() },
            title = { Text("Confirmar Importación") },
            text = {
                Column {
                    Text("Se detectaron:", color = Color.White)
                    Text("• ${prev.newFavoritesCount} sitios nuevos", color = NexoEmerald)
                    Text("• ${prev.existingFavoritesCount} sitios ya existentes", color = NexoTextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text("¿Deseas agregar estos sitios a tus favoritos de Nexo TV?")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyImportPreview() },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                    modifier = Modifier.tvFocusable("confirm_apply_import_btn")
                ) {
                    Text("Aplicar Importación")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelImportPreview() },
                    modifier = Modifier.tvFocusable("cancel_import_btn")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TvSettingsScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diag by viewModel.diagnosticReport.collectAsState()
    val isTv by viewModel.isTvMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        Text("Ajustes y Diagnóstico del Dispositivo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Información de compatibilidad con Google TV Streamer / FlowBox TV y estado de seguridad", fontSize = 13.sp, color = NexoTextSecondary)

        Spacer(Modifier.height(18.dp))

        // Diagnostic Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Informe de Entorno y WebView", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("• Dispositivo: ${diag.manufacturer} ${diag.deviceModel}", color = Color.White, fontSize = 13.sp)
                Text("• Android: ${diag.androidVersion} (API ${diag.sdkInt})", color = Color.White, fontSize = 13.sp)
                Text("• Proveedor WebView: ${diag.webViewVersion}", color = Color.White, fontSize = 13.sp)
                Text("• Google Safe Browsing: ${if (diag.safeBrowsingSupported) "Soportado" else "No disponible en este motor"}", color = Color.White, fontSize = 13.sp)
                Text("• Reglas de filtrado activas: ${diag.activeFilterRules} dominios", color = NexoBlueLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("• Modo de interfaz actual: ${if (isTv) "Android TV (Flechas y OK)" else "Móvil / Táctil"}", color = Color.White, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Remote Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Guía del Control Remoto (Sin teclas de colores ni micrófono)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Text("• Flechas (D-Pad): Mover foco en la portada, mover cursor virtual o hacer scroll en web.", color = Color.White, fontSize = 13.sp)
                Text("• Tecla OK: Abrir sitio seleccionado, activar botones o hacer clic con el puntero.", color = Color.White, fontSize = 13.sp)
                Text("• Tecla Atrás: 1. Cerrar diálogo -> 2. Salir de video fullscreen -> 3. Mostrar barra de herramientas -> 4. Volver al portal.", color = NexoBlueLight, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.clearWebData(context) },
                colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                modifier = Modifier.tvFocusable("clean_cache_btn")
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Limpiar Caché y Sesiones Web")
            }

            Button(
                onClick = { viewModel.toggleTvModeOverride() },
                colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                modifier = Modifier.tvFocusable("toggle_ui_mode_btn")
            ) {
                Icon(if (isTv) Icons.Default.PhoneAndroid else Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isTv) "Alternar a vista Móvil" else "Alternar a vista TV")
            }
        }
    }
}
