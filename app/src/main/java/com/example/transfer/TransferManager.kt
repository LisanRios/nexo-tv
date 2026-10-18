package com.example.transfer

import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.model.Category
import com.example.domain.model.EventLink
import com.example.domain.model.EventType
import com.example.domain.model.Favorite
import com.example.domain.model.SitePolicy
import com.example.domain.model.SportEvent
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class ExportData(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "Nexo TV",
    val favorites: List<Favorite>,
    val categories: List<Category>,
    val manualEvents: List<SportEvent>,
    val sitePolicies: List<SitePolicy>
)

data class ImportPreview(
    val schemaVersion: Int,
    val newFavoritesCount: Int,
    val existingFavoritesCount: Int,
    val newManualEventsCount: Int,
    val favorites: List<Favorite>,
    val categories: List<Category>,
    val manualEvents: List<SportEvent>,
    val sitePolicies: List<SitePolicy>
)

object TransferManager {
    const val CURRENT_SCHEMA_VERSION = 1

    /**
     * Serializes export data to standard readable JSON.
     */
    fun exportToJson(data: ExportData): String {
        val root = JSONObject()
        root.put("schemaVersion", data.schemaVersion)
        root.put("exportedAt", data.exportedAt)
        root.put("app", data.app)

        val favArray = JSONArray()
        data.favorites.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("title", f.title)
            obj.put("url", f.url)
            obj.put("categoryId", f.categoryId ?: JSONObject.NULL)
            obj.put("sortOrder", f.sortOrder)
            obj.put("pinned", f.pinned)
            favArray.put(obj)
        }
        root.put("favorites", favArray)

        val catArray = JSONArray()
        data.categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("sortOrder", c.sortOrder)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val evtArray = JSONArray()
        data.manualEvents.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("sport", e.sport ?: JSONObject.NULL)
            obj.put("competition", e.competition ?: JSONObject.NULL)
            obj.put("originalDateText", e.originalDateText ?: JSONObject.NULL)
            val linksArray = JSONArray()
            e.links.forEach { l ->
                val lObj = JSONObject()
                lObj.put("pageUrl", l.pageUrl)
                lObj.put("label", l.label ?: JSONObject.NULL)
                linksArray.put(lObj)
            }
            obj.put("links", linksArray)
            evtArray.put(obj)
        }
        root.put("manualEvents", evtArray)

        val polArray = JSONArray()
        data.sitePolicies.forEach { p ->
            val obj = JSONObject()
            obj.put("originHost", p.originHost)
            val authArray = JSONArray()
            p.authorizedNavigationHosts.forEach { authArray.put(it) }
            obj.put("authorizedHosts", authArray)
            polArray.put(obj)
        }
        root.put("sitePolicies", polArray)

        return root.toString(2)
    }

    /**
     * Parses and validates JSON payload to generate an import preview.
     */
    fun parseAndValidateJson(jsonString: String, existingFavoriteUrls: Set<String>): Result<ImportPreview> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("schemaVersion", -1)
            if (version > CURRENT_SCHEMA_VERSION) {
                return Result.failure(Exception("Versión de esquema futura incompatible (v$version). Actualice la app."))
            }
            if (version < 1) {
                return Result.failure(Exception("Formato JSON no reconocido o sin versión de esquema válida."))
            }

            val favs = mutableListOf<Favorite>()
            val favArray = root.optJSONArray("favorites") ?: JSONArray()
            for (i in 0 until favArray.length()) {
                val obj = favArray.getJSONObject(i)
                val title = obj.getString("title").trim()
                val url = obj.getString("url").trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    favs.add(
                        Favorite(
                            id = obj.optString("id", "fav_${System.currentTimeMillis()}_$i"),
                            title = title,
                            url = url,
                            categoryId = if (obj.isNull("categoryId")) null else obj.optString("categoryId"),
                            sortOrder = obj.optInt("sortOrder", i),
                            pinned = obj.optBoolean("pinned", false)
                        )
                    )
                }
            }

            val cats = mutableListOf<Category>()
            val catArray = root.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                cats.add(
                    Category(
                        id = obj.optString("id", "cat_$i"),
                        name = obj.getString("name"),
                        sortOrder = obj.optInt("sortOrder", i)
                    )
                )
            }

            val manualEvents = mutableListOf<SportEvent>()
            val evtArray = root.optJSONArray("manualEvents") ?: JSONArray()
            for (i in 0 until evtArray.length()) {
                val obj = evtArray.getJSONObject(i)
                val eventId = obj.optString("id", "evt_imp_$i")
                val linksList = mutableListOf<EventLink>()
                val linksArr = obj.optJSONArray("links") ?: JSONArray()
                for (j in 0 until linksArr.length()) {
                    val lObj = linksArr.getJSONObject(j)
                    val pUrl = lObj.getString("pageUrl")
                    if (pUrl.startsWith("http://") || pUrl.startsWith("https://")) {
                        linksList.add(
                            EventLink(
                                id = "lnk_${eventId}_$j",
                                eventId = eventId,
                                sourceId = "imported",
                                pageUrl = pUrl,
                                label = if (lObj.isNull("label")) null else lObj.optString("label")
                            )
                        )
                    }
                }
                manualEvents.add(
                    SportEvent(
                        id = eventId,
                        title = obj.getString("title"),
                        sport = if (obj.isNull("sport")) null else obj.optString("sport"),
                        competition = if (obj.isNull("competition")) null else obj.optString("competition"),
                        originalDateText = if (obj.isNull("originalDateText")) null else obj.optString("originalDateText"),
                        sourceId = "imported",
                        eventType = EventType.IMPORTED,
                        links = linksList
                    )
                )
            }

            val policies = mutableListOf<SitePolicy>()
            val polArray = root.optJSONArray("sitePolicies") ?: JSONArray()
            for (i in 0 until polArray.length()) {
                val obj = polArray.getJSONObject(i)
                val authList = mutableListOf<String>()
                val authArr = obj.optJSONArray("authorizedHosts") ?: JSONArray()
                for (k in 0 until authArr.length()) authList.add(authArr.getString(k))
                policies.add(
                    SitePolicy(
                        originHost = obj.getString("originHost"),
                        authorizedNavigationHosts = authList,
                        strictProtectionEnabled = true
                    )
                )
            }

            val newCount = favs.count { it.url !in existingFavoriteUrls }
            val existingCount = favs.size - newCount

            Result.success(
                ImportPreview(
                    schemaVersion = version,
                    newFavoritesCount = newCount,
                    existingFavoritesCount = existingCount,
                    newManualEventsCount = manualEvents.size,
                    favorites = favs,
                    categories = cats,
                    manualEvents = manualEvents,
                    sitePolicies = policies
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Error al interpretar JSON: ${e.localizedMessage}"))
        }
    }

    /**
     * Encodes a small package of favorites to a compact compressed string for TV keyboard entry.
     */
    fun encodeToCompactString(favorites: List<Favorite>): String {
        val root = JSONObject()
        root.put("v", 1)
        val arr = JSONArray()
        favorites.take(20).forEach { f ->
            val item = JSONObject()
            item.put("t", f.title)
            item.put("u", f.url)
            item.put("p", if (f.pinned) 1 else 0)
            arr.put(item)
        }
        root.put("f", arr)

        val raw = root.toString()
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(raw.toByteArray(Charsets.UTF_8)) }
        return "NEXO:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray())
    }

    /**
     * Decodes compact string entered via TV keyboard or QR text.
     */
    fun decodeFromCompactString(compactText: String): Result<List<Favorite>> {
        return try {
            val trimmed = compactText.trim()
            val base64Data = if (trimmed.startsWith("NEXO:")) trimmed.removePrefix("NEXO:") else trimmed
            val compressedBytes = Base64.getUrlDecoder().decode(base64Data)
            val bis = ByteArrayInputStream(compressedBytes)
            val jsonStr = GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { it.readText() }

            val root = JSONObject(jsonStr)
            val arr = root.getJSONArray("f")
            val favs = mutableListOf<Favorite>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val title = item.getString("t")
                val url = item.getString("u")
                val pinned = item.optInt("p", 0) == 1
                favs.add(
                    Favorite(
                        id = "fav_cpt_$i",
                        title = title,
                        url = url,
                        sortOrder = i,
                        pinned = pinned
                    )
                )
            }
            Result.success(favs)
        } catch (e: Exception) {
            Result.failure(Exception("Texto compacto inválido o corrupto: ${e.localizedMessage}"))
        }
    }

    /**
     * Generates a QR Code Bitmap for small payloads.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 400): Result<Bitmap> {
        return try {
            if (content.length > 1500) {
                return Result.failure(Exception("El paquete excede el tamaño seguro para código QR (${content.length} caracteres). Use exportación de archivo JSON."))
            }
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decodes a QR code from a locally selected Bitmap.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): Result<String> {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap)
            Result.success(result.text)
        } catch (e: Exception) {
            Result.failure(Exception("No se detectó un código QR legible en la imagen"))
        }
    }
}
