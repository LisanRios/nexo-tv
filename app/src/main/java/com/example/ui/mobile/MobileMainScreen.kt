package com.example.ui.mobile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Favorite
import com.example.domain.model.SportEvent
import com.example.transfer.TransferManager
import com.example.ui.NexoMainViewModel
import com.example.ui.common.SiteMonogram
import com.example.ui.common.SportBadge
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoEmerald
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoRed
import com.example.ui.theme.NexoSurfaceCard
import com.example.ui.theme.NexoSurfaceElevated
import com.example.ui.theme.NexoTextSecondary

enum class MobileTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SITES("Sitios", Icons.Default.Language),
    AGENDA("Agenda", Icons.Default.CalendarMonth),
    TRANSFER("Transferir", Icons.Default.SyncAlt),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

@Composable
fun MobileMainScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MobileTab.SITES) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NexoNavyDark,
        bottomBar = {
            NavigationBar(
                containerColor = NexoSurfaceCard,
                contentColor = Color.White
            ) {
                MobileTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = NexoBlueLight,
                            indicatorColor = NexoBluePrimary,
                            unselectedIconColor = NexoTextSecondary,
                            unselectedTextColor = NexoTextSecondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == MobileTab.SITES) {
                FloatingActionButton(
                    onClick = { viewModel.createNewFavorite() },
                    containerColor = NexoBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mobile_fab_add_site")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo sitio")
                }
            } else if (currentTab == MobileTab.AGENDA) {
                FloatingActionButton(
                    onClick = { viewModel.createNewEvent() },
                    containerColor = NexoBluePrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mobile_fab_add_event")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo evento")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(targetState = currentTab, label = "mobileTabCrossfade") { tab ->
                when (tab) {
                    MobileTab.SITES -> MobileSitesTab(viewModel = viewModel)
                    MobileTab.AGENDA -> MobileAgendaTab(viewModel = viewModel)
                    MobileTab.TRANSFER -> MobileTransferTab(viewModel = viewModel)
                    MobileTab.SETTINGS -> MobileSettingsTab(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MobileSitesTab(viewModel: NexoMainViewModel) {
    val favorites by viewModel.favorites.collectAsState()
    var favoriteToDelete by remember { mutableStateOf<Favorite?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Mis Sitios Deportivos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Favoritos sincronizados para tu televisor y móvil", fontSize = 13.sp, color = NexoTextSecondary)

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favorites, key = { it.id }) { fav ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SiteMonogram(title = fav.title, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(fav.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Text(fav.url, color = NexoTextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        IconButton(onClick = { viewModel.openBrowser(fav.url, fav.title) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Abrir", tint = NexoBlueLight)
                        }
                        IconButton(onClick = { viewModel.editFavorite(fav) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                        }
                        IconButton(onClick = { favoriteToDelete = fav }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexoRed)
                        }
                    }
                }
            }
        }
    }

    favoriteToDelete?.let { fav ->
        AlertDialog(
            onDismissRequest = { favoriteToDelete = null },
            title = { Text("Eliminar favorito") },
            text = { Text("¿Deseas eliminar '${fav.title}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFavorite(fav.id)
                        favoriteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { favoriteToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun MobileAgendaTab(viewModel: NexoMainViewModel) {
    val events by viewModel.agendaEvents.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Agenda Deportiva", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Eventos y transmisiones disponibles", fontSize = 13.sp, color = NexoTextSecondary)
            }
            IconButton(onClick = { viewModel.refreshAllAgendaSources() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = NexoBlueLight)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay eventos en la agenda. Pulsa actualizar para consultar fuentes.", color = NexoTextSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(events, key = { it.id }) { evt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SportBadge(sport = evt.sport ?: "Deportes")
                                Spacer(Modifier.width(8.dp))
                                Text(evt.originalDateText ?: "Inicio previsto", color = NexoBlueLight, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(evt.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Spacer(Modifier.height(8.dp))
                            if (evt.links.isNotEmpty()) {
                                Button(
                                    onClick = { viewModel.openBrowser(evt.links.first().pageUrl, evt.title) },
                                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ver transmisión")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MobileTransferTab(viewModel: NexoMainViewModel) {
    val context = LocalContext.current
    var showQrModal by remember { mutableStateOf(false) }
    val preview by viewModel.importPreview.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()

    // Activity result launcher to pick an image containing a QR code
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                }
                TransferManager.decodeQrFromBitmap(bitmap).onSuccess { qrText ->
                    viewModel.processCompactText(qrText)
                }.onFailure { err ->
                    viewModel.transferStatusMessage.value = err.localizedMessage
                }
            } catch (e: Exception) {
                viewModel.transferStatusMessage.value = "Error al leer imagen: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Transferencia y Códigos QR", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Pasa tus favoritos fácilmente al televisor", fontSize = 13.sp, color = NexoTextSecondary)

        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Mostrar Código QR para escanear en TV", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.generateExportPackage()
                        showQrModal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generar Código QR")
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Escanear imagen de QR", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { pickImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Seleccionar imagen con QR")
                }
            }
        }
    }

    if (showQrModal && qrBitmap != null) {
        AlertDialog(
            onDismissRequest = { showQrModal = false },
            title = { Text("Código QR de tus Favoritos") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = qrBitmap!!.asImageBitmap(),
                        contentDescription = "Código QR",
                        modifier = Modifier.size(240.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Usa este código o el texto compacto NEXO para transferir tus sitios al televisor.",
                        fontSize = 12.sp,
                        color = NexoTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQrModal = false }) { Text("Cerrar") }
            }
        )
    }

    preview?.let { prev ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelImportPreview() },
            title = { Text("Confirmar importación") },
            text = {
                Text("Se importarán ${prev.newFavoritesCount} sitios nuevos.")
            },
            confirmButton = {
                Button(onClick = { viewModel.applyImportPreview() }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImportPreview() }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun MobileSettingsTab(viewModel: NexoMainViewModel) {
    val context = LocalContext.current
    val diag by viewModel.diagnosticReport.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dispositivo: ${diag.manufacturer} ${diag.deviceModel}", color = Color.White)
                Text("Android: ${diag.androidVersion} (API ${diag.sdkInt})", color = Color.White)
                Text("WebView: ${diag.webViewVersion}", color = Color.White)
                Text("Reglas de bloqueo activas: ${diag.activeFilterRules}", color = NexoBlueLight)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.clearWebData(context) },
            colors = ButtonDefaults.buttonColors(containerColor = NexoSurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Limpiar caché de navegación")
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.toggleTvModeOverride() },
            colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Tv, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cambiar a modo TV")
        }
    }
}
