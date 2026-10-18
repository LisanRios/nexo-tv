package com.example.data.repository

import com.example.agenda.AgendaFetchResult
import com.example.agenda.AgendaSourceAdapter
import com.example.agenda.FutbolLibreAdapter
import com.example.agenda.TvLibreAdapter
import com.example.data.local.AgendaDao
import com.example.data.local.AgendaSourceDao
import com.example.data.local.AgendaSourceEntity
import com.example.data.local.EventLinkEntity
import com.example.data.local.SportEventEntity
import com.example.domain.model.AgendaSource
import com.example.domain.model.AgendaSourceStatus
import com.example.domain.model.EventLink
import com.example.domain.model.EventType
import com.example.domain.model.SportEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.concurrent.TimeUnit

class AgendaRepository(
    private val agendaDao: AgendaDao,
    private val agendaSourceDao: AgendaSourceDao,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    private val adapters: Map<String, AgendaSourceAdapter> = mapOf(
        "adapter_tvlibre" to TvLibreAdapter(okHttpClient),
        "adapter_futbollibre" to FutbolLibreAdapter(okHttpClient)
    )

    companion object {
        val INITIAL_SOURCES = listOf(
            AgendaSource(
                id = "src_tvlibre",
                title = "TV Libre",
                baseUrl = "https://tvlibre.st/",
                adapterId = "adapter_tvlibre",
                enabled = true,
                status = AgendaSourceStatus.NOT_CONFIGURED,
                technicalMessage = "Adaptador HTML público disponible"
            ),
            AgendaSource(
                id = "src_futbollibre_fullhd",
                title = "Fútbol Libre Full HD",
                baseUrl = "https://futbollibrefullhd.org/",
                adapterId = "adapter_futbollibre",
                enabled = true,
                status = AgendaSourceStatus.NOT_CONFIGURED,
                technicalMessage = "Adaptador de tabla de partidos disponible"
            ),
            AgendaSource(
                id = "src_pelota_libre",
                title = "Pelota Libre",
                baseUrl = "https://pelotalibre.la/default.php",
                adapterId = "adapter_futbollibre",
                enabled = true,
                status = AgendaSourceStatus.NOT_CONFIGURED,
                technicalMessage = "Espejo compatible con adaptador de partidos"
            ),
            AgendaSource(
                id = "src_roja_directa",
                title = "Roja Directa",
                baseUrl = "https://rojadirectatvmas.net/",
                adapterId = null,
                enabled = true,
                status = AgendaSourceStatus.PENDING_IMPLEMENTATION,
                technicalMessage = "Estructura pendiente de fixture verificado; accesible por favoritos"
            ),
            AgendaSource(
                id = "src_rustico_tv",
                title = "Rústico TV",
                baseUrl = "https://rusticotvplus.org/",
                adapterId = null,
                enabled = true,
                status = AgendaSourceStatus.PENDING_IMPLEMENTATION,
                technicalMessage = "Estructura pendiente de fixture verificado"
            ),
            AgendaSource(
                id = "src_futbol_para_todos",
                title = "Fútbol para Todos",
                baseUrl = "https://futbolparatodos.ad/home.php",
                adapterId = null,
                enabled = false,
                status = AgendaSourceStatus.ACCESS_ERROR,
                technicalMessage = "Inaccesible en revisión previa; verificar conectividad en dispositivo"
            ),
            AgendaSource(
                id = "src_al_angulo_tv",
                title = "Al Ángulo TV",
                baseUrl = "https://alangulotv.sx/",
                adapterId = null,
                enabled = false,
                status = AgendaSourceStatus.ACCESS_ERROR,
                technicalMessage = "Inaccesible en revisión previa; verificar conectividad en dispositivo"
            ),
            AgendaSource(
                id = "src_tu_deporte",
                title = "Tu Deporte",
                baseUrl = "https://tudeporte.lat/",
                adapterId = null,
                enabled = true,
                status = AgendaSourceStatus.PENDING_IMPLEMENTATION,
                technicalMessage = "Estructura pendiente de análisis de endpoints públicos"
            )
        )
    }

    suspend fun seedInitialSourcesIfEmpty() {
        if (agendaSourceDao.count() == 0) {
            agendaSourceDao.insertAll(
                INITIAL_SOURCES.map { src ->
                    AgendaSourceEntity(
                        id = src.id,
                        title = src.title,
                        baseUrl = src.baseUrl,
                        adapterId = src.adapterId,
                        enabled = src.enabled,
                        lastAttemptAt = src.lastAttemptAt,
                        lastSuccessAt = src.lastSuccessAt,
                        status = src.status.name,
                        technicalMessage = src.technicalMessage
                    )
                }
            )
        }
    }

    fun getAllSources(): Flow<List<AgendaSource>> =
        agendaSourceDao.getAllSources().map { list ->
            list.map { it.toDomain() }
        }

    fun getEventsWithLinks(): Flow<List<SportEvent>> =
        combine(agendaDao.getAllEvents(), agendaDao.getAllLinks()) { eventEntities, linkEntities ->
            val linksByEventId = linkEntities.groupBy { it.eventId }
            val events = eventEntities.map { entity ->
                val links = linksByEventId[entity.id]?.map { linkEntity ->
                    EventLink(
                        id = linkEntity.id,
                        eventId = linkEntity.eventId,
                        sourceId = linkEntity.sourceId,
                        pageUrl = linkEntity.pageUrl,
                        label = linkEntity.label
                    )
                } ?: emptyList()
                entity.toDomain(links)
            }
            deduplicateEvents(events)
        }

    /**
     * Deduplicates events that represent the same match/game across sources.
     * Combines links without losing original source labels.
     */
    fun deduplicateEvents(events: List<SportEvent>): List<SportEvent> {
        val result = mutableListOf<SportEvent>()
        val grouped = mutableMapOf<String, SportEvent>()

        for (event in events) {
            val key = normalizeForDeduplication(event.title, event.sport)
            val existing = grouped[key]
            if (existing != null) {
                // Merge links
                val mergedLinks = (existing.links + event.links).distinctBy { it.pageUrl }
                val merged = existing.copy(links = mergedLinks)
                grouped[key] = merged
            } else {
                grouped[key] = event
            }
        }
        return grouped.values.toList()
    }

    private fun normalizeForDeduplication(title: String, sport: String?): String {
        val cleanTitle = title.lowercase()
            .replace("[^a-z0-9]".toRegex(), "")
        return "${sport?.lowercase() ?: ""}_$cleanTitle"
    }

    suspend fun refreshSource(sourceId: String) = withContext(Dispatchers.IO) {
        val sourceEntity = agendaSourceDao.getSourceById(sourceId) ?: return@withContext
        val adapterId = sourceEntity.adapterId
        if (adapterId.isNullOrBlank() || !adapters.containsKey(adapterId)) {
            agendaSourceDao.updateStatus(
                id = sourceId,
                attemptAt = System.currentTimeMillis(),
                successAt = sourceEntity.lastSuccessAt,
                status = AgendaSourceStatus.PENDING_IMPLEMENTATION.name,
                message = "Sin adaptador verificado disponible para esta fuente"
            )
            return@withContext
        }

        val adapter = adapters[adapterId]!!
        val source = sourceEntity.toDomain()

        agendaSourceDao.updateStatus(
            id = sourceId,
            attemptAt = System.currentTimeMillis(),
            successAt = sourceEntity.lastSuccessAt,
            status = AgendaSourceStatus.UPDATING.name,
            message = "Consultando fuente..."
        )

        val result: AgendaFetchResult = try {
            adapter.fetchEvents(source)
        } catch (e: Exception) {
            AgendaFetchResult(
                status = AgendaSourceStatus.ACCESS_ERROR,
                message = "Error en adaptador: ${e.localizedMessage}"
            )
        }

        val now = System.currentTimeMillis()
        val successTime = if (result.status == AgendaSourceStatus.AVAILABLE) now else sourceEntity.lastSuccessAt

        agendaSourceDao.updateStatus(
            id = sourceId,
            attemptAt = now,
            successAt = successTime,
            status = result.status.name,
            message = result.message
        )

        if (result.status == AgendaSourceStatus.AVAILABLE && result.events.isNotEmpty()) {
            val eventEntities = result.events.map { event ->
                SportEventEntity(
                    id = event.id,
                    title = event.title,
                    sport = event.sport,
                    competition = event.competition,
                    participants = event.participants,
                    startsAt = event.startsAt,
                    originalTimezone = event.originalTimezone,
                    originalDateText = event.originalDateText,
                    sourceId = event.sourceId,
                    sourceEventId = event.sourceEventId,
                    fetchedAt = event.fetchedAt,
                    expiresAt = event.expiresAt,
                    eventType = EventType.AUTOMATIC.name
                )
            }
            val linkEntities = result.events.flatMap { it.links }.map { link ->
                EventLinkEntity(
                    id = link.id,
                    eventId = link.eventId,
                    sourceId = link.sourceId,
                    pageUrl = link.pageUrl,
                    label = link.label
                )
            }
            agendaDao.replaceAutomaticEvents(sourceId, eventEntities, linkEntities)
        }
    }

    suspend fun refreshAllEnabledSources() = withContext(Dispatchers.IO) {
        val sources = INITIAL_SOURCES.filter { it.enabled && it.adapterId != null }
        for (source in sources) {
            refreshSource(source.id)
        }
    }

    suspend fun addManualEvent(
        title: String,
        sport: String?,
        competition: String?,
        dateText: String?,
        pageUrl: String,
        sourceLabel: String?
    ) {
        val eventId = "evt_man_" + UUID.randomUUID().toString().take(8)
        val linkId = "lnk_man_" + UUID.randomUUID().toString().take(8)

        val eventEntity = SportEventEntity(
            id = eventId,
            title = title.trim(),
            sport = sport?.trim()?.ifBlank { "Deportes" },
            competition = competition?.trim()?.ifBlank { "Evento deportivo" },
            participants = null,
            startsAt = null,
            originalTimezone = "America/Argentina/Buenos_Aires",
            originalDateText = dateText?.trim()?.ifBlank { "Horario sin confirmar" },
            sourceId = "manual",
            sourceEventId = null,
            fetchedAt = System.currentTimeMillis(),
            expiresAt = null,
            eventType = EventType.MANUAL.name
        )

        val linkEntity = EventLinkEntity(
            id = linkId,
            eventId = eventId,
            sourceId = "manual",
            pageUrl = pageUrl.trim(),
            label = sourceLabel?.trim()?.ifBlank { "Enlace directo" }
        )

        agendaDao.saveEventWithLinks(eventEntity, listOf(linkEntity))
    }

    suspend fun deleteEvent(eventId: String) {
        agendaDao.deleteEventById(eventId)
        agendaDao.deleteLinksForEvent(eventId)
    }

    private fun AgendaSourceEntity.toDomain() = AgendaSource(
        id = id,
        title = title,
        baseUrl = baseUrl,
        adapterId = adapterId,
        enabled = enabled,
        lastAttemptAt = lastAttemptAt,
        lastSuccessAt = lastSuccessAt,
        status = try { AgendaSourceStatus.valueOf(status) } catch (_: Exception) { AgendaSourceStatus.NOT_CONFIGURED },
        technicalMessage = technicalMessage
    )

    private fun SportEventEntity.toDomain(links: List<EventLink>) = SportEvent(
        id = id,
        title = title,
        sport = sport,
        competition = competition,
        participants = participants,
        startsAt = startsAt,
        originalTimezone = originalTimezone,
        originalDateText = originalDateText,
        sourceId = sourceId,
        sourceEventId = sourceEventId,
        fetchedAt = fetchedAt,
        expiresAt = expiresAt,
        eventType = try { EventType.valueOf(eventType) } catch (_: Exception) { EventType.MANUAL },
        links = links
    )
}
