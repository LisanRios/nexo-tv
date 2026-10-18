package com.example.data.repository

import com.example.data.local.CategoryDao
import com.example.data.local.CategoryEntity
import com.example.data.local.FavoriteDao
import com.example.data.local.FavoriteEntity
import com.example.data.local.SitePolicyDao
import com.example.data.local.SitePolicyEntity
import com.example.domain.model.Category
import com.example.domain.model.Favorite
import com.example.domain.model.SitePolicy
import com.example.protection.HostNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.util.UUID

class FavoritesRepository(
    private val favoriteDao: FavoriteDao,
    private val categoryDao: CategoryDao,
    private val sitePolicyDao: SitePolicyDao
) {
    companion object {
        const val CATEGORY_DEPORTES_ID = "cat_deportes"

        // 8 Preconfigured sports favorites required by specification
        val INITIAL_FAVORITES = listOf(
            Favorite(
                id = "fav_tv_libre",
                title = "TV Libre",
                url = "https://tvlibre.st/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 0,
                pinned = true
            ),
            Favorite(
                id = "fav_futbol_para_todos",
                title = "Fútbol para Todos",
                url = "https://futbolparatodos.ad/home.php",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 1,
                pinned = false
            ),
            Favorite(
                id = "fav_roja_directa",
                title = "Roja Directa",
                url = "https://rojadirectatvmas.net/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 2,
                pinned = false
            ),
            Favorite(
                id = "fav_rustico_tv",
                title = "Rústico TV",
                url = "https://rusticotvplus.org/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 3,
                pinned = false
            ),
            Favorite(
                id = "fav_pelota_libre",
                title = "Pelota Libre",
                url = "https://pelotalibre.la/default.php",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 4,
                pinned = false
            ),
            Favorite(
                id = "fav_futbol_libre_fullhd",
                title = "Fútbol Libre Full HD",
                url = "https://futbollibrefullhd.org/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 5,
                pinned = false
            ),
            Favorite(
                id = "fav_al_angulo_tv",
                title = "Al Ángulo TV",
                url = "https://alangulotv.sx/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 6,
                pinned = false
            ),
            Favorite(
                id = "fav_tu_deporte",
                title = "Tu Deporte",
                url = "https://tudeporte.lat/",
                categoryId = CATEGORY_DEPORTES_ID,
                sortOrder = 7,
                pinned = false
            )
        )
    }

    suspend fun seedInitialFavoritesIfEmpty() {
        if (favoriteDao.count() == 0) {
            categoryDao.insertOrUpdate(
                CategoryEntity(
                    id = CATEGORY_DEPORTES_ID,
                    name = "Deportes",
                    sortOrder = 0
                )
            )
            favoriteDao.insertAll(
                INITIAL_FAVORITES.map { fav ->
                    FavoriteEntity(
                        id = fav.id,
                        title = fav.title,
                        url = fav.url,
                        categoryId = fav.categoryId,
                        sortOrder = fav.sortOrder,
                        pinned = fav.pinned,
                        createdAt = fav.createdAt,
                        updatedAt = fav.updatedAt
                    )
                }
            )
        }
    }

    fun getAllFavorites(): Flow<List<Favorite>> =
        favoriteDao.getAllFavorites().map { list ->
            list.map { it.toDomain() }
        }

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list ->
            list.map { Category(it.id, it.name, it.sortOrder) }
        }

    suspend fun addFavorite(title: String, url: String, categoryId: String? = null, pinned: Boolean = false) {
        val id = "fav_" + UUID.randomUUID().toString().take(8)
        val entity = FavoriteEntity(
            id = id,
            title = title.trim(),
            url = url.trim(),
            categoryId = categoryId,
            sortOrder = 999,
            pinned = pinned,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        favoriteDao.insertOrUpdate(entity)
    }

    suspend fun updateFavorite(favorite: Favorite) {
        val entity = FavoriteEntity(
            id = favorite.id,
            title = favorite.title.trim(),
            url = favorite.url.trim(),
            categoryId = favorite.categoryId,
            sortOrder = favorite.sortOrder,
            pinned = favorite.pinned,
            createdAt = favorite.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        favoriteDao.insertOrUpdate(entity)
    }

    suspend fun deleteFavorite(id: String) {
        favoriteDao.deleteById(id)
    }

    suspend fun moveFavorite(id: String, moveUp: Boolean, currentList: List<Favorite>) {
        val index = currentList.indexOfFirst { it.id == id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex in currentList.indices) {
            val itemA = currentList[index]
            val itemB = currentList[targetIndex]
            favoriteDao.updateSortOrder(itemA.id, itemB.sortOrder)
            favoriteDao.updateSortOrder(itemB.id, itemA.sortOrder)
        }
    }

    suspend fun togglePinned(id: String, currentPinned: Boolean) {
        favoriteDao.updatePinned(id, !currentPinned)
    }

    suspend fun getPolicyForHost(host: String): SitePolicy {
        val normalized = HostNormalizer.normalize(host)
        val entity = sitePolicyDao.getPolicyForHost(normalized)
        return if (entity != null) {
            val hostsList = mutableListOf<String>()
            try {
                val array = JSONArray(entity.authorizedNavigationHostsJson)
                for (i in 0 until array.length()) hostsList.add(array.getString(i))
            } catch (_: Exception) {}
            SitePolicy(
                originHost = entity.originHost,
                authorizedNavigationHosts = hostsList,
                strictProtectionEnabled = entity.strictProtectionEnabled
            )
        } else {
            SitePolicy(originHost = normalized, authorizedNavigationHosts = emptyList(), strictProtectionEnabled = true)
        }
    }

    suspend fun addAuthorizedHost(originHost: String, newHost: String) {
        val normalizedOrigin = HostNormalizer.normalize(originHost)
        val normalizedNew = HostNormalizer.normalize(newHost)
        val currentPolicy = getPolicyForHost(normalizedOrigin)
        val updatedHosts = (currentPolicy.authorizedNavigationHosts + normalizedNew).distinct()

        val array = JSONArray()
        updatedHosts.forEach { array.put(it) }

        sitePolicyDao.insertOrUpdate(
            SitePolicyEntity(
                originHost = normalizedOrigin,
                authorizedNavigationHostsJson = array.toString(),
                strictProtectionEnabled = true
            )
        )
    }

    private fun FavoriteEntity.toDomain() = Favorite(
        id = id,
        title = title,
        url = url,
        categoryId = categoryId,
        sortOrder = sortOrder,
        pinned = pinned,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
