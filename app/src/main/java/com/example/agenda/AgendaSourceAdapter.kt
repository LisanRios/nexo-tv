package com.example.agenda

import com.example.domain.model.AgendaSource
import com.example.domain.model.AgendaSourceStatus
import com.example.domain.model.EventType
import com.example.domain.model.SportEvent
import com.example.domain.model.EventLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AgendaFetchResult(
    val status: AgendaSourceStatus,
    val events: List<SportEvent> = emptyList(),
    val message: String? = null
)

interface AgendaSourceAdapter {
    val adapterId: String
    suspend fun fetchEvents(source: AgendaSource): AgendaFetchResult
}

/**
 * Public HTML parser adapter for TV Libre agenda / event listings.
 */
class TvLibreAdapter(private val okHttpClient: OkHttpClient) : AgendaSourceAdapter {
    override val adapterId = "adapter_tvlibre"

    override suspend fun fetchEvents(source: AgendaSource): AgendaFetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.baseUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext AgendaFetchResult(
                    status = AgendaSourceStatus.ACCESS_ERROR,
                    message = "Servidor respondió con código HTTP ${response.code}"
                )
            }

            val html = response.body?.string() ?: ""
            if (html.isBlank()) {
                return@withContext AgendaFetchResult(
                    status = AgendaSourceStatus.NO_EVENTS,
                    message = "Página vacía o sin contenido legible"
                )
            }

            val doc = Jsoup.parse(html, source.baseUrl)
            val parsedEvents = mutableListOf<SportEvent>()

            // Find event rows, list items or links to match broadcasts
            val candidateElements = doc.select("a[href*=/en-vivo/], a[href*=/partido/], a[href*=/canal/], tr:has(a), li:has(a[href])")

            for (element in candidateElements.take(30)) {
                val title = element.text().trim()
                val href = element.attr("abs:href").ifBlank { element.selectFirst("a")?.attr("abs:href") ?: "" }

                if (title.length in 5..120 && href.isNotBlank() && !title.contains("Politica", ignoreCase = true) && !title.contains("Contacto", ignoreCase = true)) {
                    val eventId = "evt_tvl_" + UUID.randomUUID().toString().take(8)
                    val link = EventLink(
                        id = "lnk_" + UUID.randomUUID().toString().take(8),
                        eventId = eventId,
                        sourceId = source.id,
                        pageUrl = href,
                        label = "TV Libre"
                    )
                    parsedEvents.add(
                        SportEvent(
                            id = eventId,
                            title = title,
                            sport = guessSport(title),
                            competition = "Transmisión en vivo",
                            sourceId = source.id,
                            fetchedAt = System.currentTimeMillis(),
                            eventType = EventType.AUTOMATIC,
                            originalDateText = "Inicio previsto",
                            links = listOf(link)
                        )
                    )
                }
            }

            if (parsedEvents.isEmpty()) {
                // If the DOM structure changed or has no active listings
                AgendaFetchResult(
                    status = AgendaSourceStatus.NO_EVENTS,
                    message = "No se encontraron eventos listados en el formato esperado"
                )
            } else {
                AgendaFetchResult(
                    status = AgendaSourceStatus.AVAILABLE,
                    events = parsedEvents,
                    message = "${parsedEvents.size} transmisiones detectadas"
                )
            }
        } catch (e: Exception) {
            AgendaFetchResult(
                status = AgendaSourceStatus.ACCESS_ERROR,
                message = "Fallo de conexión: ${e.localizedMessage ?: "Error de red"}"
            )
        }
    }

    private fun guessSport(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("vs") || lower.contains("futbol") || lower.contains("liga") || lower.contains("copa") -> "Fútbol"
            lower.contains("nba") || lower.contains("basket") || lower.contains("baloncesto") -> "Básquet"
            lower.contains("f1") || lower.contains("formula") || lower.contains("gran premio") -> "Automovilismo"
            lower.contains("atp") || lower.contains("wta") || lower.contains("tennis") || lower.contains("tenis") -> "Tenis"
            lower.contains("box") || lower.contains("ufc") || lower.contains("mma") -> "Combate"
            else -> "Deportes"
        }
    }
}

/**
 * Public HTML parser adapter for Fútbol Libre / Pelota Libre mirrors where tables are available.
 */
class FutbolLibreAdapter(private val okHttpClient: OkHttpClient) : AgendaSourceAdapter {
    override val adapterId = "adapter_futbollibre"

    override suspend fun fetchEvents(source: AgendaSource): AgendaFetchResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(source.baseUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext AgendaFetchResult(
                    status = AgendaSourceStatus.ACCESS_ERROR,
                    message = "HTTP ${response.code} recibido"
                )
            }

            val html = response.body?.string() ?: ""
            val doc = Jsoup.parse(html, source.baseUrl)
            val parsedEvents = mutableListOf<SportEvent>()

            // Select matches list usually wrapped in tables, list-group, or card items
            val matches = doc.select(".partido, .match, .agenda-item, table tr, a[href*=/partido/], a[href*=/canal/]")

            for (elem in matches.take(30)) {
                val text = elem.text().trim()
                val href = elem.attr("abs:href").ifBlank { elem.selectFirst("a")?.attr("abs:href") ?: "" }

                if (text.length in 6..120 && href.isNotBlank()) {
                    val eventId = "evt_fl_" + UUID.randomUUID().toString().take(8)
                    val link = EventLink(
                        id = "lnk_" + UUID.randomUUID().toString().take(8),
                        eventId = eventId,
                        sourceId = source.id,
                        pageUrl = href,
                        label = source.title
                    )
                    parsedEvents.add(
                        SportEvent(
                            id = eventId,
                            title = text,
                            sport = "Fútbol",
                            competition = "Liga / Torneo",
                            sourceId = source.id,
                            fetchedAt = System.currentTimeMillis(),
                            eventType = EventType.AUTOMATIC,
                            originalDateText = "Inicio previsto",
                            links = listOf(link)
                        )
                    )
                }
            }

            if (parsedEvents.isEmpty()) {
                AgendaFetchResult(
                    status = AgendaSourceStatus.NO_EVENTS,
                    message = "Sin partidos encontrados en la portada actual"
                )
            } else {
                AgendaFetchResult(
                    status = AgendaSourceStatus.AVAILABLE,
                    events = parsedEvents,
                    message = "${parsedEvents.size} partidos encontrados"
                )
            }
        } catch (e: Exception) {
            AgendaFetchResult(
                status = AgendaSourceStatus.ACCESS_ERROR,
                message = "Fallo de conexión o bloqueo: ${e.localizedMessage ?: "Error de red"}"
            )
        }
    }
}
