package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.domain.model.AgendaSourceStatus
import com.example.domain.model.EventType
import org.json.JSONArray

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val categoryId: String?,
    val sortOrder: Int,
    val pinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int
)

@Entity(tableName = "site_policies")
data class SitePolicyEntity(
    @PrimaryKey val originHost: String,
    val authorizedNavigationHostsJson: String,
    val strictProtectionEnabled: Boolean
)

@Entity(tableName = "agenda_sources")
data class AgendaSourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val baseUrl: String,
    val adapterId: String?,
    val enabled: Boolean,
    val lastAttemptAt: Long?,
    val lastSuccessAt: Long?,
    val status: String,
    val technicalMessage: String?
)

@Entity(tableName = "sport_events")
data class SportEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sport: String?,
    val competition: String?,
    val participants: String?,
    val startsAt: Long?,
    val originalTimezone: String?,
    val originalDateText: String?,
    val sourceId: String,
    val sourceEventId: String?,
    val fetchedAt: Long,
    val expiresAt: Long?,
    val eventType: String
)

@Entity(tableName = "event_links")
data class EventLinkEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val sourceId: String,
    val pageUrl: String,
    val label: String?
)

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        value.forEach { array.put(it) }
        return array.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) { }
        return list
    }
}
