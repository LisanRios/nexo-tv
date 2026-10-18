package com.example

import com.example.data.repository.AgendaRepository
import com.example.domain.model.EventLink
import com.example.domain.model.EventType
import com.example.domain.model.SportEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgendaDeduplicationTest {

    @Test
    fun testEventDeduplication_mergesMatchingTitlesAcrossSources() {
        val link1 = EventLink(id = "l1", eventId = "e1", sourceId = "src_tvlibre", pageUrl = "https://tvlibre.st/partido/river-boca", label = "TV Libre")
        val link2 = EventLink(id = "l2", eventId = "e2", sourceId = "src_futbollibre", pageUrl = "https://futbollibrefullhd.org/partido/river-vs-boca", label = "Fútbol Libre")

        val event1 = SportEvent(
            id = "e1",
            title = "River Plate vs Boca Juniors",
            sport = "Fútbol",
            competition = "Liga Profesional",
            sourceId = "src_tvlibre",
            eventType = EventType.AUTOMATIC,
            links = listOf(link1)
        )

        val event2 = SportEvent(
            id = "e2",
            title = "River Plate vs Boca Juniors",
            sport = "Fútbol",
            competition = "Torneo Local",
            sourceId = "src_futbollibre",
            eventType = EventType.AUTOMATIC,
            links = listOf(link2)
        )

        // Mock DAOs are not needed just to test pure deduplication logic
        val repo = AgendaRepository(
            agendaDao = FakeAgendaDao(),
            agendaSourceDao = FakeAgendaSourceDao()
        )

        val deduplicated = repo.deduplicateEvents(listOf(event1, event2))
        assertEquals(1, deduplicated.size)
        assertEquals(2, deduplicated[0].links.size)
    }

    private class FakeAgendaDao : com.example.data.local.AgendaDao {
        override fun getAllEvents() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.SportEventEntity>())
        override suspend fun getEventById(id: String) = null
        override suspend fun getLinksForEvent(eventId: String) = emptyList<com.example.data.local.EventLinkEntity>()
        override fun getAllLinks() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.EventLinkEntity>())
        override suspend fun insertEvent(event: com.example.data.local.SportEventEntity) {}
        override suspend fun insertEvents(events: List<com.example.data.local.SportEventEntity>) {}
        override suspend fun insertLinks(links: List<com.example.data.local.EventLinkEntity>) {}
        override suspend fun deleteLinksForEvent(eventId: String) {}
        override suspend fun deleteEventById(id: String) {}
        override suspend fun clearAutomaticEventsForSource(sourceId: String) {}
        override suspend fun clearLinksForSource(sourceId: String) {}
    }

    private class FakeAgendaSourceDao : com.example.data.local.AgendaSourceDao {
        override fun getAllSources() = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.AgendaSourceEntity>())
        override suspend fun getSourceById(id: String) = null
        override suspend fun count() = 0
        override suspend fun insertOrUpdate(source: com.example.data.local.AgendaSourceEntity) {}
        override suspend fun insertAll(sources: List<com.example.data.local.AgendaSourceEntity>) {}
        override suspend fun updateStatus(id: String, attemptAt: Long?, successAt: Long?, status: String, message: String?) {}
    }
}
