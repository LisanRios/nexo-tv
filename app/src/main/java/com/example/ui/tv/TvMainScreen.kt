package com.example.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NexoMainViewModel
import com.example.ui.Screen
import com.example.ui.common.tvFocusable
import com.example.ui.theme.NexoBlueLight
import com.example.ui.theme.NexoBluePrimary
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoSurfaceCard
import com.example.ui.theme.NexoSurfaceElevated
import com.example.ui.theme.NexoTextSecondary

enum class TvTab(val label: String, val icon: ImageVector) {
    SITES("Sitios", Icons.Default.Language),
    AGENDA("Agenda", Icons.Default.CalendarMonth),
    SOURCES("Fuentes", Icons.Default.Dns),
    TRANSFER("Transferir", Icons.Default.SyncAlt),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

@Composable
fun TvMainScreen(
    viewModel: NexoMainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TvTab.SITES) }

    // Remote Back handling: if on a subtab, return to SITES tab
    BackHandler(enabled = selectedTab != TvTab.SITES) {
        selectedTab = TvTab.SITES
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexoNavyDark)
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(NexoBluePrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NEXO TV",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Transmisiones Deportivas",
                        fontSize = 11.sp,
                        color = NexoBlueLight
                    )
                }
            }

            // Tabs for TV Remote
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Button(
                        onClick = { selectedTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) NexoBluePrimary else NexoSurfaceCard
                        ),
                        modifier = Modifier.tvFocusable("tv_tab_${tab.name}")
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else NexoTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else NexoTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NexoSurfaceElevated)
        )

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            Crossfade(targetState = selectedTab, label = "tvTabCrossfade") { tab ->
                when (tab) {
                    TvTab.SITES -> TvSitesScreen(viewModel = viewModel)
                    TvTab.AGENDA -> TvAgendaScreen(viewModel = viewModel)
                    TvTab.SOURCES -> TvSourcesScreen(viewModel = viewModel)
                    TvTab.TRANSFER -> TvTransferScreen(viewModel = viewModel)
                    TvTab.SETTINGS -> TvSettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
