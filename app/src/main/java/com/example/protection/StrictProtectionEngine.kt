package com.example.protection

import android.net.Uri
import com.example.domain.model.BlockedResourceLog
import com.example.domain.model.SitePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

sealed class NavigationDecision {
    data object Allow : NavigationDecision()
    data class Block(val reason: String, val destinationHost: String) : NavigationDecision()
}

class StrictProtectionEngine(
    private val requestFilter: RequestFilter = DomainListRequestFilter(DefaultBlockedDomains.BUILT_IN_RULES),
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val blockedCount = AtomicInteger(0)
    private val _blockedCountFlow = MutableStateFlow(0)
    val blockedCountFlow: StateFlow<Int> = _blockedCountFlow.asStateFlow()

    private val logBuffer = ConcurrentLinkedDeque<BlockedResourceLog>()
    private val maxLogs = 50

    private val _logsFlow = MutableStateFlow<List<BlockedResourceLog>>(emptyList())
    val logsFlow: StateFlow<List<BlockedResourceLog>> = _logsFlow.asStateFlow()

    fun getFilter(): RequestFilter = requestFilter

    /**
     * Checks an observable main document navigation request.
     * Prevents malicious redirects and ad network escapes.
     */
    fun evaluateNavigation(
        destinationUri: Uri,
        currentOriginHost: String?,
        sitePolicy: SitePolicy?
    ): NavigationDecision {
        val scheme = destinationUri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") {
            logBlocked(
                host = destinationUri.host ?: scheme ?: "desconocido",
                reason = "Esquema peligroso o no soportado bloqueado ($scheme)",
                mainHost = currentOriginHost ?: "portal"
            )
            return NavigationDecision.Block(
                reason = "Esquema no permitido ($scheme). No se admiten intentos de apertura externa.",
                destinationHost = destinationUri.host ?: scheme ?: ""
            )
        }

        val destHost = HostNormalizer.normalize(destinationUri.host)
        if (destHost.isEmpty()) {
            return NavigationDecision.Block("URL sin host válido", "")
        }

        // 1. Is this destination a known ad/tracker/popup host?
        val resourceCheck = requestFilter.shouldBlockResource(destinationUri, currentOriginHost)
        if (resourceCheck.shouldBlock) {
            logBlocked(destHost, resourceCheck.reason ?: "Destino publicitario", currentOriginHost ?: destHost)
            return NavigationDecision.Block(
                reason = "Destino bloqueado por regla publicitaria: $destHost",
                destinationHost = destHost
            )
        }

        // If no origin is set yet, allow initial load of approved bookmark/event
        val normalizedOrigin = HostNormalizer.normalize(currentOriginHost)
        if (normalizedOrigin.isEmpty()) {
            return NavigationDecision.Allow
        }

        // 2. Main document origin boundary check
        val isSameOrigin = HostNormalizer.matchesRule(destHost, normalizedOrigin)
        if (isSameOrigin) {
            return NavigationDecision.Allow
        }

        // 3. Check explicitly authorized navigation destinations for this site
        val authorized = sitePolicy?.authorizedNavigationHosts?.map { HostNormalizer.normalize(it) } ?: emptyList()
        val isAuthorized = authorized.any { HostNormalizer.matchesRule(destHost, it) }

        if (isAuthorized) {
            return NavigationDecision.Allow
        }

        // Block navigation escape
        logBlocked(
            host = destHost,
            reason = "Salida del sitio a dominio no autorizado: $destHost",
            mainHost = normalizedOrigin
        )
        return NavigationDecision.Block(
            reason = "Se detuvo el cambio de destino a '$destHost'. No pertenece al origen autorizado ('$normalizedOrigin').",
            destinationHost = destHost
        )
    }

    /**
     * Checks secondary resource loads (iframes, scripts, xhr, images).
     */
    fun evaluateResource(uri: Uri, mainDocumentHost: String?): FilterDecision {
        val decision = requestFilter.shouldBlockResource(uri, mainDocumentHost)
        if (decision.shouldBlock) {
            logBlocked(
                host = HostNormalizer.normalize(uri.host),
                reason = decision.reason ?: "Recurso bloqueado",
                mainHost = mainDocumentHost ?: ""
            )
        }
        return decision
    }

    private fun logBlocked(host: String, reason: String, mainHost: String) {
        val count = blockedCount.incrementAndGet()
        _blockedCountFlow.value = count

        val log = BlockedResourceLog(
            timestamp = System.currentTimeMillis(),
            host = host.ifBlank { "url_sin_host" },
            reason = reason,
            mainDocumentHost = mainHost
        )

        logBuffer.addFirst(log)
        while (logBuffer.size > maxLogs) {
            logBuffer.pollLast()
        }
        _logsFlow.value = logBuffer.toList()
    }

    fun resetSessionCounters() {
        blockedCount.set(0)
        _blockedCountFlow.value = 0
        logBuffer.clear()
        _logsFlow.value = emptyList()
    }

    /**
     * Downloads an updated hosts file from an HTTPS source (e.g. curated hosts list)
     * and atomically applies it if valid.
     */
    suspend fun updateFilterListFromUrl(httpsUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(httpsUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code} al obtener lista"))
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Respuesta vacía"))
            }

            // Parse hosts syntax: lines with '0.0.0.0 host' or '127.0.0.1 host' or plain hosts
            val parsedHosts = mutableSetOf<String>()
            body.lineSequence().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1")) {
                        val host = parts[1].trim()
                        if (host != "localhost" && host.contains('.')) {
                            parsedHosts.add(host)
                        }
                    } else if (parts.size == 1 && parts[0].contains('.')) {
                        parsedHosts.add(parts[0])
                    }
                }
            }

            // Combine with built-in rules so core protections are never lost
            parsedHosts.addAll(DefaultBlockedDomains.BUILT_IN_RULES)

            val success = requestFilter.updateRules(parsedHosts)
            if (success) {
                Result.success(requestFilter.getRuleCount())
            } else {
                Result.failure(Exception("Lista con formato no válido o cantidad de reglas insuficiente"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
