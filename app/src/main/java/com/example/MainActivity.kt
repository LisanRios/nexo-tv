package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.NexoMainViewModel
import com.example.ui.Screen
import com.example.ui.browser.BrowserScreen
import com.example.ui.forms.EditEventDialog
import com.example.ui.forms.EditFavoriteDialog
import com.example.ui.mobile.MobileMainScreen
import com.example.ui.theme.NexoNavyDark
import com.example.ui.theme.NexoTvTheme
import com.example.ui.tv.TvMainScreen

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: NexoMainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = NexoMainViewModel.Factory(this)
        viewModel = ViewModelProvider(this, factory)[NexoMainViewModel::class.java]

        setContent {
            NexoTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NexoNavyDark
                ) {
                    NexoAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun NexoAppContent(viewModel: NexoMainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isTvMode by viewModel.isTvMode.collectAsState()
    val editingFavorite by viewModel.editingFavorite.collectAsState()
    val editingEvent by viewModel.editingEvent.collectAsState()

    // Screen navigation
    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.BROWSER -> {
                BrowserScreen(viewModel = viewModel)
            }
            else -> {
                if (isTvMode) {
                    TvMainScreen(viewModel = viewModel)
                } else {
                    MobileMainScreen(viewModel = viewModel)
                }
            }
        }

        // Overlay Dialogs
        if (currentScreen == Screen.EDIT_FAVORITE) {
            EditFavoriteDialog(
                initialFavorite = editingFavorite,
                onSave = { title, url, catId, pinned ->
                    viewModel.saveFavorite(title, url, catId, pinned)
                },
                onDismiss = {
                    viewModel.navigateBack()
                }
            )
        }

        if (currentScreen == Screen.EDIT_EVENT) {
            EditEventDialog(
                initialEvent = editingEvent,
                onSave = { title, sport, competition, dateText, pageUrl, label ->
                    viewModel.saveManualEvent(title, sport, competition, dateText, pageUrl, label)
                },
                onDismiss = {
                    viewModel.navigateBack()
                }
            )
        }
    }
}
