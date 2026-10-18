package com.example

import android.app.Application
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.data.local.NexoTvDatabase
import com.example.data.repository.AgendaRepository
import com.example.data.repository.FavoritesRepository
import com.example.protection.StrictProtectionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class NexoTvApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: NexoTvDatabase
        private set

    lateinit var favoritesRepository: FavoritesRepository
        private set

    lateinit var agendaRepository: AgendaRepository
        private set

    lateinit var protectionEngine: StrictProtectionEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Ensure cache directory permissions and clean hierarchy for Chromium WebView
        ensureCacheDirectoryAccessible()

        database = NexoTvDatabase.getInstance(this)
        favoritesRepository = FavoritesRepository(
            favoriteDao = database.favoriteDao(),
            categoryDao = database.categoryDao(),
            sitePolicyDao = database.sitePolicyDao()
        )
        agendaRepository = AgendaRepository(
            agendaDao = database.agendaDao(),
            agendaSourceDao = database.agendaSourceDao()
        )
        protectionEngine = StrictProtectionEngine()

        // Seed initial favorites and sources if first launch
        applicationScope.launch(Dispatchers.IO) {
            favoritesRepository.seedInitialFavoritesIfEmpty()
            agendaRepository.seedInitialSourcesIfEmpty()
        }
    }

    /**
     * Ensures WebView disk cache directory exists with proper read/write/execute permissions,
     * preventing "Simple Cache Backend: cache directory inaccessible right after creation" errors.
     */
    private fun ensureCacheDirectoryAccessible() {
        try {
            val baseCache = cacheDir ?: return
            baseCache.setReadable(true, false)
            baseCache.setWritable(true, false)
            baseCache.setExecutable(true, false)

            val webViewDir = File(baseCache, "WebView")
            if (webViewDir.exists()) {
                val httpCache = File(webViewDir, "Default/HTTP Cache")
                if (httpCache.exists() && (!httpCache.isDirectory || !httpCache.canWrite())) {
                    httpCache.deleteRecursively()
                }
                webViewDir.walkBottomUp().forEach { file ->
                    file.setReadable(true, false)
                    file.setWritable(true, false)
                    if (file.isDirectory) file.setExecutable(true, false)
                }
            } else {
                webViewDir.mkdirs()
                webViewDir.setReadable(true, false)
                webViewDir.setWritable(true, false)
                webViewDir.setExecutable(true, false)
            }

            // Pre-create the Code Cache/js directory with proper permissions
            val jsCodeCache = File(baseCache, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!jsCodeCache.exists()) {
                jsCodeCache.mkdirs()
            }
            jsCodeCache.setReadable(true, false)
            jsCodeCache.setWritable(true, false)
            jsCodeCache.setExecutable(true, false)

            // Clean up any stale or corrupted variations seed files that trigger "Seed missing signature"
            val appWebviewDir = File(dataDir, "app_webview")
            if (appWebviewDir.exists()) {
                val seed = File(appWebviewDir, "variations_seed")
                val seedNew = File(appWebviewDir, "variations_seed_new")
                if (seed.exists() && seed.length() < 32) {
                    seed.delete()
                }
                if (seedNew.exists() && seedNew.length() < 32) {
                    seedNew.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Cleans volatile web data without removing user bookmarks/agendas.
     */
    fun cleanVolatileWebState() {
        try {
            CookieManager.getInstance().removeSessionCookies(null)
            WebStorage.getInstance().deleteAllData()
        } catch (_: Exception) {}
    }

    companion object {
        lateinit var instance: NexoTvApplication
            private set
    }
}
