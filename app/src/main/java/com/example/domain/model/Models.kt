package com.example.domain.model

enum class AgendaSourceStatus(val label: String) {
    NOT_CONFIGURED("Sin configurar"),
    PENDING_IMPLEMENTATION("Pendiente de implementación"),
    UPDATING("Actualizando…"),
    AVAILABLE("Disponible"),
    NO_EVENTS("Sin eventos publicados"),
    ACCESS_ERROR("Error de acceso"),
    FORMAT_CHANGED("Formato cambiado"),
    OUTDATED("Datos desactualizados")
}

enum class EventType {
    MANUAL,
    IMPORTED,
    AUTOMATIC
}

data class Favorite(
    val id: String,
    val title: String,
    val url: String,
    val categoryId: String? = null,
    val sortOrder: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Category(
    val id: String,
    val name: String,
    val sortOrder: Int = 0
)

data class SitePolicy(
    val originHost: String,
    val authorizedNavigationHosts: List<String> = emptyList(),
    val strictProtectionEnabled: Boolean = true
)

data class AgendaSource(
    val id: String,
    val title: String,
    val baseUrl: String,
    val adapterId: String? = null,
    val enabled: Boolean = true,
    val lastAttemptAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val status: AgendaSourceStatus = AgendaSourceStatus.NOT_CONFIGURED,
    val technicalMessage: String? = null
)

data class EventLink(
    val id: String,
    val eventId: String,
    val sourceId: String,
    val pageUrl: String,
    val label: String? = null
)

data class SportEvent(
    val id: String,
    val title: String,
    val sport: String? = null,
    val competition: String? = null,
    val participants: String? = null,
    val startsAt: Long? = null,
    val originalTimezone: String? = null,
    val originalDateText: String? = null,
    val sourceId: String,
    val sourceEventId: String? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val eventType: EventType = EventType.MANUAL,
    val links: List<EventLink> = emptyList()
)

data class BlockedResourceLog(
    val timestamp: Long = System.currentTimeMillis(),
    val host: String,
    val reason: String,
    val mainDocumentHost: String
)
