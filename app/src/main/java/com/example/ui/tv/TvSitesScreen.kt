package com.example.ui.tv

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Favorite
import com.example.protection.HostNormalizer
import com.example.ui.NexoMainViewModel
import com.example.ui.common.SiteMonogram
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
fun TvSitesScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.favoritesQuery.collectAsState()
    var favoriteToDelete by remember { mutableStateOf<Favorite?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.favoritesQuery.value = it },
                placeholder = { Text("Buscar en mis sitios...", color = NexoTextSecondary) },
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
                    .tvFocusable("sites_search_input")
            )

            Button(
                onClick = { viewModel.createNewFavorite() },
                colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                modifier = Modifier.tvFocusable("add_site_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Sitio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Sites Grid
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay sitios que coincidan con la búsqueda", color = NexoTextSecondary, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.favoritesQuery.value = "" },
                        modifier = Modifier.tvFocusable("reset_search_button")
                    ) {
                        Text("Ver todos los sitios")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.id }) { fav ->
                    TvSiteCard(
                        favorite = fav,
                        onOpen = { viewModel.openBrowser(fav.url, fav.title) },
                        onTogglePin = { viewModel.togglePinned(fav.id, fav.pinned) },
                        onMoveUp = { viewModel.moveFavorite(fav.id, true) },
                        onMoveDown = { viewModel.moveFavorite(fav.id, false) },
                        onEdit = { viewModel.editFavorite(fav) },
                        onDelete = { favoriteToDelete = fav }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    favoriteToDelete?.let { fav ->
        AlertDialog(
            onDismissRequest = { favoriteToDelete = null },
            title = { Text("Eliminar favorito") },
            text = { Text("¿Deseas eliminar '${fav.title}' de tu lista de sitios?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFavorite(fav.id)
                        favoriteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NexoRed),
                    modifier = Modifier.tvFocusable("confirm_delete_site_btn")
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { favoriteToDelete = null },
                    modifier = Modifier.tvFocusable("cancel_delete_site_btn")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TvSiteCard(
    favorite: Favorite,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val host = remember(favorite.url) {
        try {
            Uri.parse(favorite.url).host ?: favorite.url
        } catch (_: Exception) {
            favorite.url
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable("site_card_${favorite.id}"),
        colors = CardDefaults.cardColors(containerColor = NexoSurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SiteMonogram(title = favorite.title, size = 52.dp)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = favorite.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (favorite.pinned) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Fijado",
                                tint = NexoBlueLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = host,
                        fontSize = 13.sp,
                        color = NexoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = NexoBluePrimary),
                    modifier = Modifier.tvFocusable("open_site_${favorite.id}")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Abrir", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row {
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.tvFocusable("move_up_${favorite.id}")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir orden", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.tvFocusable("move_down_${favorite.id}")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar orden", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.tvFocusable("pin_${favorite.id}")
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (favorite.pinned) "Desfijar" else "Fijar",
                            tint = if (favorite.pinned) NexoBlueLight else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.tvFocusable("edit_${favorite.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.LightGray)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.tvFocusable("delete_${favorite.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = NexoRed)
                    }
                }
            }
        }
    }
}
