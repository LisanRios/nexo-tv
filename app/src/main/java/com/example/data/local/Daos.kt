package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY pinned DESC, sortOrder ASC, createdAt ASC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    suspend fun getFavoriteById(id: String): FavoriteEntity?

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()

    @Query("UPDATE favorites SET sortOrder = :order, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE favorites SET pinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePinned(id: String, pinned: Boolean, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SitePolicyDao {
    @Query("SELECT * FROM site_policies WHERE originHost = :originHost LIMIT 1")
    suspend fun getPolicyForHost(originHost: String): SitePolicyEntity?

    @Query("SELECT * FROM site_policies")
    fun getAllPolicies(): Flow<List<SitePolicyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(policy: SitePolicyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(policies: List<SitePolicyEntity>)
}

@Dao
interface AgendaSourceDao {
    @Query("SELECT * FROM agenda_sources ORDER BY title ASC")
    fun getAllSources(): Flow<List<AgendaSourceEntity>>

    @Query("SELECT * FROM agenda_sources WHERE id = :id LIMIT 1")
    suspend fun getSourceById(id: String): AgendaSourceEntity?

    @Query("SELECT COUNT(*) FROM agenda_sources")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(source: AgendaSourceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<AgendaSourceEntity>)

    @Query("UPDATE agenda_sources SET lastAttemptAt = :attemptAt, lastSuccessAt = :successAt, status = :status, technicalMessage = :message WHERE id = :id")
    suspend fun updateStatus(id: String, attemptAt: Long?, successAt: Long?, status: String, message: String?)
}

@Dao
interface AgendaDao {
    @Query("SELECT * FROM sport_events ORDER BY startsAt ASC, title ASC")
    fun getAllEvents(): Flow<List<SportEventEntity>>

    @Query("SELECT * FROM sport_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): SportEventEntity?

    @Query("SELECT * FROM event_links WHERE eventId = :eventId")
    suspend fun getLinksForEvent(eventId: String): List<EventLinkEntity>

    @Query("SELECT * FROM event_links")
    fun getAllLinks(): Flow<List<EventLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SportEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SportEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<EventLinkEntity>)

    @Query("DELETE FROM event_links WHERE eventId = :eventId")
    suspend fun deleteLinksForEvent(eventId: String)

    @Query("DELETE FROM sport_events WHERE id = :id")
    suspend fun deleteEventById(id: String)

    @Query("DELETE FROM sport_events WHERE sourceId = :sourceId AND eventType = 'AUTOMATIC'")
    suspend fun clearAutomaticEventsForSource(sourceId: String)

    @Query("DELETE FROM event_links WHERE sourceId = :sourceId")
    suspend fun clearLinksForSource(sourceId: String)

    @Transaction
    suspend fun replaceAutomaticEvents(sourceId: String, events: List<SportEventEntity>, links: List<EventLinkEntity>) {
        clearAutomaticEventsForSource(sourceId)
        clearLinksForSource(sourceId)
        insertEvents(events)
        insertLinks(links)
    }

    @Transaction
    suspend fun saveEventWithLinks(event: SportEventEntity, links: List<EventLinkEntity>) {
        insertEvent(event)
        deleteLinksForEvent(event.id)
        insertLinks(links)
    }
}
