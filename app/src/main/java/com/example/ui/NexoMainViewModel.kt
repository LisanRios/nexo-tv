package com.example.ui

import android.app.UiModeManager
import com.example.NexoTvApplication
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.data.repository.AgendaRepository
import com.example.data.repository.FavoritesRepository
import com.example.domain.model.AgendaSource
import com.example.domain.model.BlockedResourceLog
import com.example.domain.model.Category
import com.example.domain.model.Favorite
import com.example.domain.model.SitePolicy
import com.example.domain.model.SportEvent
import com.example.protection.HostNormalizer
import com.example.protection.StrictProtectionEngine
import com.example.transfer.ExportData
import com.example.transfer.ImportPreview
import com.example.transfer.TransferManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    SITES,
    AGENDA,
    BROWSER,
    EDIT_FAVORITE,
    EDIT_EVENT,
    SOURCES,
    TRANSFER,
    SETTINGS
}

enum class ControlMode(val label: String) {
    FOCUS("Foco DOM"),
    CURSOR("Puntero Virtual"),
    SCROLL("Desplazamiento")
}

data class DiagnosticReport(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkInt: Int,
    val webViewVersion: String,
    val safeBrowsingSupported: Boolean,
    val multiProfileSupported: Boolean,
    val activeFilterRules: Int,
    val isTvMode: Boolean
)

class NexoMainViewModel(
    private val favoritesRepository: FavoritesRepository,
    private val agendaRepository: AgendaRepository,
    val protectionEngine: StrictProtectionEngine,
    context: Context
) : ViewModel() {

    // Device form factor detection (with override capability)
    private val _isTvMode = MutableStateFlow(detectIsTv(context))
    val isTvMode: StateFlow<Boolean> = _isTvMode.asStateFlow()

    fun toggleTvModeOverride() {
        _isTvMode.value = !_isTvMode.value
    }

    // Navigation BackStack
    private val screenStack = mutableListOf(Screen.SITES)
    private val _currentScreen = MutableStateFlow(Screen.SITES)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            screenStack.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            _currentScreen.value = screenStack.last()
            return true
        }
        return false
    }

    // Search and Filters
    val favoritesQuery = MutableStateFlow("")
    val selectedCategoryId = MutableStateFlow<String?>(null)

    val agendaQuery = MutableStateFlow("")
    val selectedSportFilter = MutableStateFlow<String?>(null)

    // Data streams
    val categories: StateFlow<List<Category>> = favoritesRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Favorite>> = combine(
        favoritesRepository.getAllFavorites(),
        favoritesQuery,
        selectedCategoryId
    ) { allFavs, query, catId ->
        allFavs.filter { fav ->
            val matchesQuery = query.isBlank() || fav.title.contains(query, ignoreCase = true) || fav.url.contains(query, ignoreCase = true)
            val matchesCat = catId == null || fav.categoryId == catId
            matchesQuery && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agendaSources: StateFlow<List<AgendaSource>> = agendaRepository.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agendaEvents: StateFlow<List<SportEvent>> = combine(
        agendaRepository.getEventsWithLinks(),
        agendaQuery,
        selectedSportFilter
    ) { allEvents, query, sport ->
        allEvents.filter { event ->
            val matchesQuery = query.isBlank() ||
                event.title.contains(query, ignoreCase = true) ||
                (event.competition?.contains(query, ignoreCase = true) == true)
            val matchesSport = sport == null || event.sport.equals(sport, ignoreCase = true)
            matchesQuery && matchesSport
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Browser State
    val browserUrl = MutableStateFlow<String?>(null)
    val browserOriginHost = MutableStateFlow<String?>(null)
    val browserTitle = MutableStateFlow("Navegador")
    val browserLoading = MutableStateFlow(false)
    val browserControlMode = MutableStateFlow(ControlMode.FOCUS)
    val isToolbarVisible = MutableStateFlow(true)
    val isFullscreenVideo = MutableStateFlow(false)
    val isImmersiveWeb = MutableStateFlow(false)
    val blockedNotice = MutableStateFlow<String?>(null)
    val showBlockedDialog = MutableStateFlow(false)

    // Editing State
    val editingFavorite = MutableStateFlow<Favorite?>(null)
    val editingEvent = MutableStateFlow<SportEvent?>(null)

    // Transfer State
    val importPreview = MutableStateFlow<ImportPreview?>(null)
    val transferStatusMessage = MutableStateFlow<String?>(null)
    val qrBitmap = MutableStateFlow<Bitmap?>(null)
    val compactExportText = MutableStateFlow<String?>(null)

    // Diagnostics
    val diagnosticReport = MutableStateFlow(buildDiagnosticReport(context))

    init {
        // Initial silent check of sources
        viewModelScope.launch {
            agendaRepository.refreshAllEnabledSources()
        }
    }

    fun openBrowser(url: String, title: String? = null) {
        val uri = Uri.parse(url)
        val host = HostNormalizer.normalize(uri.host)
        browserUrl.value = url
        browserOriginHost.value = host
        browserTitle.value = title ?: host
        browserControlMode.value = ControlMode.FOCUS
        isToolbarVisible.value = true
        isFullscreenVideo.value = false
        isImmersiveWeb.value = false
        blockedNotice.value = null
        protectionEngine.resetSessionCounters()
        navigateTo(Screen.BROWSER)
    }

    fun closeBrowser() {
        browserUrl.value = null
        browserOriginHost.value = null
        isFullscreenVideo.value = false
        isToolbarVisible.value = true
        navigateBack()
    }

    fun cycleControlMode() {
        val next = when (browserControlMode.value) {
            ControlMode.FOCUS -> ControlMode.CURSOR
            ControlMode.CURSOR -> ControlMode.SCROLL
            ControlMode.SCROLL -> ControlMode.FOCUS
        }
        browserControlMode.value = next
    }

    fun toggleToolbar() {
        isToolbarVisible.value = !isToolbarVisible.value
    }

    fun toggleImmersiveWeb() {
        isImmersiveWeb.value = !isImmersiveWeb.value
        if (isImmersiveWeb.value) {
            isToolbarVisible.value = false
        }
    }

    fun notifyNavigationBlocked(reason: String, destinationHost: String) {
        blockedNotice.value = "Bloqueado: $reason"
    }

    fun clearBlockedNotice() {
        blockedNotice.value = null
    }

    // Favorite Management
    fun saveFavorite(title: String, url: String, categoryId: String?, pinned: Boolean) {
        viewModelScope.launch {
            val existing = editingFavorite.value
            if (existing != null) {
                favoritesRepository.updateFavorite(
                    existing.copy(
                        title = title,
                        url = url,
                        categoryId = categoryId,
                        pinned = pinned
                    )
                )
            } else {
                favoritesRepository.addFavorite(title, url, categoryId, pinned)
            }
            editingFavorite.value = null
            navigateBack()
        }
    }

    fun deleteFavorite(id: String) {
        viewModelScope.launch {
            favoritesRepository.deleteFavorite(id)
        }
    }

    fun moveFavorite(id: String, up: Boolean) {
        viewModelScope.launch {
            favoritesRepository.moveFavorite(id, up, favorites.value)
        }
    }

    fun togglePinned(id: String, currentPinned: Boolean) {
        viewModelScope.launch {
            favoritesRepository.togglePinned(id, currentPinned)
        }
    }

    fun editFavorite(favorite: Favorite) {
        editingFavorite.value = favorite
        navigateTo(Screen.EDIT_FAVORITE)
    }

    fun createNewFavorite() {
        editingFavorite.value = null
        navigateTo(Screen.EDIT_FAVORITE)
    }

    // Manual Event Management
    fun createNewEvent() {
        editingEvent.value = null
        navigateTo(Screen.EDIT_EVENT)
    }

    fun saveManualEvent(
        title: String,
        sport: String?,
        competition: String?,
        dateText: String?,
        pageUrl: String,
        label: String?
    ) {
        viewModelScope.launch {
            agendaRepository.addManualEvent(title, sport, competition, dateText, pageUrl, label)
            navigateBack()
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            agendaRepository.deleteEvent(id)
        }
    }

    fun refreshAllAgendaSources() {
        viewModelScope.launch {
            agendaRepository.refreshAllEnabledSources()
        }
    }

    fun refreshSingleSource(sourceId: String) {
        viewModelScope.launch {
            agendaRepository.refreshSource(sourceId)
        }
    }

    // Transfer Management
    fun generateExportPackage(): String {
        val data = ExportData(
            favorites = favorites.value,
            categories = categories.value,
            manualEvents = agendaEvents.value.filter { it.eventType == com.example.domain.model.EventType.MANUAL },
            sitePolicies = emptyList()
        )
        val json = TransferManager.exportToJson(data)
        val compact = TransferManager.encodeToCompactString(favorites.value)
        compactExportText.value = compact
        TransferManager.generateQrBitmap(compact).onSuccess { qrBitmap.value = it }
        return json
    }

    fun processImportJson(jsonString: String) {
        val existingUrls = favorites.value.map { it.url }.toSet()
        val result = TransferManager.parseAndValidateJson(jsonString, existingUrls)
        result.onSuccess {
            importPreview.value = it
            transferStatusMessage.value = null
        }.onFailure {
            importPreview.value = null
            transferStatusMessage.value = "Error: ${it.localizedMessage}"
        }
    }

    fun processCompactText(compactText: String) {
        val result = TransferManager.decodeFromCompactString(compactText)
        result.onSuccess { favList ->
            val preview = ImportPreview(
                schemaVersion = 1,
                newFavoritesCount = favList.count { it.url !in favorites.value.map { f -> f.url } },
                existingFavoritesCount = favList.size - favList.count { it.url !in favorites.value.map { f -> f.url } },
                newManualEventsCount = 0,
                favorites = favList,
                categories = emptyList(),
                manualEvents = emptyList(),
                sitePolicies = emptyList()
            )
            importPreview.value = preview
            transferStatusMessage.value = null
        }.onFailure {
            transferStatusMessage.value = "Error: ${it.localizedMessage}"
        }
    }

    fun applyImportPreview() {
        val preview = importPreview.value ?: return
        viewModelScope.launch {
            for (fav in preview.favorites) {
                favoritesRepository.addFavorite(fav.title, fav.url, fav.categoryId, fav.pinned)
            }
            for (evt in preview.manualEvents) {
                val link = evt.links.firstOrNull()?.pageUrl ?: ""
                agendaRepository.addManualEvent(
                    evt.title,
                    evt.sport,
                    evt.competition,
                    evt.originalDateText,
                    link,
                    "Importado"
                )
            }
            importPreview.value = null
            transferStatusMessage.value = "¡Importación completada con éxito!"
        }
    }

    fun cancelImportPreview() {
        importPreview.value = null
    }

    fun clearWebData(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            (context.applicationContext as NexoTvApplication).cleanVolatileWebState()
            withContext(Dispatchers.Main) {
                transferStatusMessage.value = "Datos temporales de navegación eliminados correctamente."
            }
        }
    }

    private fun detectIsTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            return true
        }
        val isLeanback = context.packageManager.hasSystemFeature("android.software.leanback")
        val isTelevision = context.packageManager.hasSystemFeature("android.hardware.type.television")
        return isLeanback || isTelevision
    }

    private fun buildDiagnosticReport(context: Context): DiagnosticReport {
        val webViewPackage = WebViewCompat.getCurrentWebViewPackage(context)
        val safeBrowsing = WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)
        val multiProfile = WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
        return DiagnosticReport(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            webViewVersion = webViewPackage?.versionName ?: "Desconocida",
            safeBrowsingSupported = safeBrowsing,
            multiProfileSupported = multiProfile,
            activeFilterRules = protectionEngine.getFilter().getRuleCount(),
            isTvMode = _isTvMode.value
        )
    }

    class Factory(
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as NexoTvApplication
            return NexoMainViewModel(
                favoritesRepository = app.favoritesRepository,
                agendaRepository = app.agendaRepository,
                protectionEngine = app.protectionEngine,
                context = context
            ) as T
        }
    }
}
