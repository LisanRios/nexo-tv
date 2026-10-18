package com.example

import com.example.domain.model.Favorite
import com.example.transfer.ExportData
import com.example.transfer.TransferManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TransferManagerTest {

    @Test
    fun testJsonExportAndImport_roundtrip() {
        val testFavorites = listOf(
            Favorite(id = "fav_1", title = "TV Libre", url = "https://tvlibre.st/", pinned = true),
            Favorite(id = "fav_2", title = "Fútbol Libre", url = "https://futbollibrefullhd.org/", pinned = false)
        )

        val exportData = ExportData(
            favorites = testFavorites,
            categories = emptyList(),
            manualEvents = emptyList(),
            sitePolicies = emptyList()
        )

        val json = TransferManager.exportToJson(exportData)
        assertTrue(json.contains("schemaVersion"))
        assertTrue(json.contains("tvlibre.st"))

        val previewResult = TransferManager.parseAndValidateJson(json, emptySet())
        assertTrue(previewResult.isSuccess)
        val preview = previewResult.getOrNull()
        assertNotNull(preview)
        assertEquals(2, preview?.favorites?.size)
        assertEquals(2, preview?.newFavoritesCount)
    }

    @Test
    fun testCompactString_roundtrip() {
        val testFavorites = listOf(
            Favorite(id = "fav_1", title = "TV Libre", url = "https://tvlibre.st/", pinned = true),
            Favorite(id = "fav_2", title = "Pelota Libre", url = "https://pelotalibre.la/default.php", pinned = false)
        )

        val compact = TransferManager.encodeToCompactString(testFavorites)
        assertTrue(compact.startsWith("NEXO:"))

        val decodeResult = TransferManager.decodeFromCompactString(compact)
        assertTrue(decodeResult.isSuccess)
        val decoded = decodeResult.getOrNull()
        assertNotNull(decoded)
        assertEquals(2, decoded?.size)
        assertEquals("TV Libre", decoded?.get(0)?.title)
        assertEquals("https://tvlibre.st/", decoded?.get(0)?.url)
    }

    @Test
    fun testImportValidation_rejectsInvalidSchemes() {
        val malformedJson = """
            {
               "schemaVersion": 1,
               "favorites": [
                  { "title": "Evil App", "url": "javascript:alert(1)" },
                  { "title": "Local DB", "url": "file:///data/data/com.example/db" }
               ]
            }
        """.trimIndent()

        val result = TransferManager.parseAndValidateJson(malformedJson, emptySet())
        assertTrue(result.isSuccess)
        // Invalid non-http schemes are skipped during validation
        assertEquals(0, result.getOrNull()?.favorites?.size)
    }
}
