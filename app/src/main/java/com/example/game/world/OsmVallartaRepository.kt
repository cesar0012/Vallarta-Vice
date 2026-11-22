package com.example.game.world

import com.example.game.engine.MaterialId
import com.example.game.engine.OsmBuildingFootprint
import com.example.game.engine.OsmNamedPlace
import com.example.game.engine.OsmPolygonMeshBuilder
import com.example.game.engine.OsmPoolFootprint
import com.example.game.engine.OsmSectorChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class OsmSyncResult(
    val chunk: OsmSectorChunk?,
    val statusText: String,
    val success: Boolean
)

/**
 * Live 1:1 OpenStreetMap 3D Footprint & Pool Streamer for inland Puerto Vallarta neighborhoods
 * (Fluvial, Versalles, Pitillal, Libramiento, Gringo Gulch) complementing the instant pre-baked
 * 2,592-building coastal pack!
 *
 * Uses official `api.openstreetmap.org/api/0.6/map.json` FIRST (fast 1-second JSON response!)
 * with 3 Overpass mirrors as automatic fallback.
 */
class OsmVallartaRepository {

    companion object {
        const val SECTOR_SIZE_M = 550f

        private val OVERPASS_ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )

        private val VALLARTA_WALL_PALETTE = arrayOf(
            floatArrayOf(0.96f, 0.94f, 0.90f),
            floatArrayOf(0.95f, 0.87f, 0.74f),
            floatArrayOf(0.93f, 0.72f, 0.56f),
            floatArrayOf(0.94f, 0.82f, 0.62f),
            floatArrayOf(0.92f, 0.66f, 0.58f),
            floatArrayOf(0.88f, 0.92f, 0.95f),
            floatArrayOf(0.95f, 0.91f, 0.84f),
            floatArrayOf(0.84f, 0.89f, 0.92f),
            floatArrayOf(0.96f, 0.78f, 0.66f),
            floatArrayOf(0.91f, 0.94f, 0.88f),
            floatArrayOf(0.89f, 0.84f, 0.78f),
            floatArrayOf(0.86f, 0.93f, 0.96f)
        )

        fun sectorIndexFor(worldX: Float, worldZ: Float): Pair<Int, Int> {
            val sx = floor(worldX / SECTOR_SIZE_M).toInt()
            val sz = floor(worldZ / SECTOR_SIZE_M).toInt()
            return sx to sz
        }

        fun sectorKey(worldX: Float, worldZ: Float): String {
            val (sx, sz) = sectorIndexFor(worldX, worldZ)
            return "${sx}_${sz}"
        }

        fun sectorKeyFromIndex(sx: Int, sz: Int): String = "${sx}_${sz}"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(9, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val loadedSectors = ConcurrentHashMap.newKeySet<String>()
    private val inFlightSectors = ConcurrentHashMap.newKeySet<String>()

    fun markSectorPrebaked(sectorKey: String) {
        loadedSectors.add(sectorKey)
    }

    fun sectorKeyFor(worldX: Float, worldZ: Float): String = sectorKey(worldX, worldZ)

    fun isSectorLoadedOrLoading(worldX: Float, worldZ: Float): Boolean {
        val k = sectorKey(worldX, worldZ)
        return k in loadedSectors || k in inFlightSectors
    }

    fun isSectorIndexLoadedOrLoading(sx: Int, sz: Int): Boolean {
        val k = sectorKeyFromIndex(sx, sz)
        return k in loadedSectors || k in inFlightSectors
    }

    suspend fun fetchPolygonSectorNear(
        worldX: Float,
        worldZ: Float,
        heroBuildings: List<WorldBuilding>
    ): OsmSyncResult {
        val (sx, sz) = sectorIndexFor(worldX, worldZ)
        return fetchSectorByIndex(sx, sz, heroBuildings)
    }

    suspend fun fetchSectorByIndex(
        sx: Int,
        sz: Int,
        heroBuildings: List<WorldBuilding>
    ): OsmSyncResult = withContext(Dispatchers.IO) {
        val key = sectorKeyFromIndex(sx, sz)
        if (key in loadedSectors) {
            return@withContext OsmSyncResult(null, "OSM 3D sector activo", true)
        }
        if (!inFlightSectors.add(key)) {
            return@withContext OsmSyncResult(null, "Sincronizando OSM 3D...", true)
        }

        try {
            val minX = sx * SECTOR_SIZE_M - 15f
            val maxX = (sx + 1) * SECTOR_SIZE_M + 15f
            val minZ = sz * SECTOR_SIZE_M - 15f
            val maxZ = (sz + 1) * SECTOR_SIZE_M + 15f

            val north = VallartaGeography.worldToLat(minZ)
            val south = VallartaGeography.worldToLat(maxZ)
            val west = VallartaGeography.worldToLon(minX)
            val east = VallartaGeography.worldToLon(maxX)

            // Skip sectors purely in deep Pacific Ocean
            val midZ = (minZ + maxZ) * 0.5f
            if (VallartaGeography.signedCoastDist(maxX, midZ) < -240f) {
                loadedSectors.add(key)
                return@withContext OsmSyncResult(null, "Sector oceánico", true)
            }

            // 1. Primary fast path: official OSM API 0.6 map.json (~1s response!)
            val rawMapJson = executeOsmMapApi(west, south, east, north)
            val parsed = if (rawMapJson != null) {
                parseOsmMapApiJson(rawMapJson, sx, sz, heroBuildings)
            } else {
                // 2. Automatic fallback: Overpass mirrors
                val query = """
                    [out:json][timeout:14];
                    (
                      way["building"]($south,$west,$north,$east);
                      way["leisure"="swimming_pool"]($south,$west,$north,$east);
                    );
                    out geom;
                """.trimIndent()
                val rawOverpass = executeOverpassWithFailover(query)
                    ?: return@withContext OsmSyncResult(null, "Ciudad 3D 1:1 PV activa", false)
                parseOverpassJson(rawOverpass, sx, sz, heroBuildings)
            }

            loadedSectors.add(key)
            val (footprints, pools, namedPlaces) = parsed
            if (footprints.isEmpty() && pools.isEmpty()) {
                return@withContext OsmSyncResult(null, "Sector OSM sincronizado", true)
            }

            val meshData = OsmPolygonMeshBuilder.buildSectorMesh(footprints, pools)
            val chunk = OsmSectorChunk(
                sectorKey = key,
                meshData = meshData,
                buildingCount = footprints.size,
                poolCount = pools.size,
                namedPlaces = namedPlaces,
                colliders = footprints.mapNotNull { fp ->
                    val baseY = max(2.0f, VallartaGeography.terrainHeight(fp.centerX, fp.centerZ))
                    ColliderBox.fromPolygonRing(fp.ring, baseY, fp.height, climbable = true)
                }
            )
            val poolLabel = if (pools.isNotEmpty()) " y ${pools.size} albercas" else ""
            OsmSyncResult(
                chunk = chunk,
                statusText = "OSM 3D 1:1 activo (+${footprints.size} edificios$poolLabel)",
                success = true
            )
        } catch (_: Exception) {
            OsmSyncResult(null, "Mapa 1:1 PV 3D activo", false)
        } finally {
            inFlightSectors.remove(key)
        }
    }

    private fun executeOsmMapApi(west: Double, south: Double, east: Double, north: Double): String? {
        return try {
            val url = "https://api.openstreetmap.org/api/0.6/map.json?bbox=$west,$south,$east,$north"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "VallartaViceAndroid3D/5.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val str = resp.body?.string()
                    if (!str.isNullOrBlank() && str.trimStart().startsWith("{")) str else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseOsmMapApiJson(
        rawJson: String,
        sx: Int,
        sz: Int,
        heroBuildings: List<WorldBuilding>
    ): Triple<List<OsmBuildingFootprint>, List<OsmPoolFootprint>, List<OsmNamedPlace>> {
        val json = JSONObject(rawJson)
        val elements = json.optJSONArray("elements") ?: return Triple(emptyList(), emptyList(), emptyList())

        val nodeMap = HashMap<Long, Pair<Float, Float>>(min(elements.length(), 8192))
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            if (el.optString("type") == "node") {
                val id = el.optLong("id", -1L)
                val lat = el.optDouble("lat", Double.NaN)
                val lon = el.optDouble("lon", Double.NaN)
                if (id >= 0L && !lat.isNaN() && !lon.isNaN()) {
                    nodeMap[id] = VallartaGeography.lonToWorldX(lon) to VallartaGeography.latToWorldZ(lat)
                }
            }
        }

        val (monuments, resorts) = splitHeroExclusions(heroBuildings)
        val rawBuildings = ArrayList<Pair<Float, OsmBuildingFootprint>>(256)
        val pools = ArrayList<OsmPoolFootprint>(64)
        val namedPlaces = ArrayList<OsmNamedPlace>(32)

        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            if (el.optString("type") != "way") continue
            val tags = el.optJSONObject("tags") ?: continue
            val isBldg = tags.has("building")
            val isPool = tags.optString("leisure") == "swimming_pool"
            if (!isBldg && !isPool) continue

            val nds = el.optJSONArray("nodes") ?: continue
            if (nds.length() < 3) continue
            val ring = ArrayList<Pair<Float, Float>>(nds.length())
            var sumX = 0f; var sumZ = 0f
            for (j in 0 until nds.length()) {
                val pt = nodeMap[nds.optLong(j, -1L)] ?: continue
                ring.add(pt)
                sumX += pt.first; sumZ += pt.second
            }
            if (ring.size > 3 && hypot(ring.first().first - ring.last().first, ring.first().second - ring.last().second) < 0.25f) {
                ring.removeAt(ring.lastIndex)
            }
            if (ring.size < 3) continue
            val cx = sumX / ring.size
            val cz = sumZ / ring.size
            if (sectorIndexFor(cx, cz) != (sx to sz)) continue

            val id = el.optLong("id", i.toLong())
            processWayPolygon(id, tags, ring, cx, cz, isPool, monuments, resorts, rawBuildings, pools, namedPlaces)
        }

        val footprints = if (rawBuildings.size > 1200) {
            rawBuildings.sortedByDescending { it.first }.take(1200).map { it.second }
        } else rawBuildings.map { it.second }
        return Triple(footprints, pools, namedPlaces)
    }

    private fun parseOverpassJson(
        rawJson: String,
        sx: Int,
        sz: Int,
        heroBuildings: List<WorldBuilding>
    ): Triple<List<OsmBuildingFootprint>, List<OsmPoolFootprint>, List<OsmNamedPlace>> {
        val json = JSONObject(rawJson)
        val elements = json.optJSONArray("elements") ?: return Triple(emptyList(), emptyList(), emptyList())
        val (monuments, resorts) = splitHeroExclusions(heroBuildings)
        val rawBuildings = ArrayList<Pair<Float, OsmBuildingFootprint>>(256)
        val pools = ArrayList<OsmPoolFootprint>(64)
        val namedPlaces = ArrayList<OsmNamedPlace>(32)

        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val geom = el.optJSONArray("geometry") ?: continue
            if (geom.length() < 3) continue
            val ring = ArrayList<Pair<Float, Float>>(geom.length())
            var sumX = 0f; var sumZ = 0f
            for (j in 0 until geom.length()) {
                val pt = geom.optJSONObject(j) ?: continue
                val lat = pt.optDouble("lat", Double.NaN)
                val lon = pt.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                val wx = VallartaGeography.lonToWorldX(lon)
                val wz = VallartaGeography.latToWorldZ(lat)
                ring.add(wx to wz)
                sumX += wx; sumZ += wz
            }
            if (ring.size > 3 && hypot(ring.first().first - ring.last().first, ring.first().second - ring.last().second) < 0.25f) {
                ring.removeAt(ring.lastIndex)
            }
            if (ring.size < 3) continue
            val cx = sumX / ring.size
            val cz = sumZ / ring.size
            if (sectorIndexFor(cx, cz) != (sx to sz)) continue

            val tags = el.optJSONObject("tags")
            val isPool = tags?.optString("leisure") == "swimming_pool"
            val id = el.optLong("id", i.toLong())
            processWayPolygon(id, tags, ring, cx, cz, isPool, monuments, resorts, rawBuildings, pools, namedPlaces)
        }

        val footprints = if (rawBuildings.size > 1200) {
            rawBuildings.sortedByDescending { it.first }.take(1200).map { it.second }
        } else rawBuildings.map { it.second }
        return Triple(footprints, pools, namedPlaces)
    }

    private fun splitHeroExclusions(heroBuildings: List<WorldBuilding>): Pair<List<WorldBuilding>, List<WorldBuilding>> {
        val monuments = heroBuildings.filter { b ->
            !b.isProceduralFill && (
                b.type == LandmarkType.PARROQUIA_CROWN ||
                b.type == LandmarkType.EL_CABALLITO ||
                b.type == LandmarkType.LOS_ARCOS_MALECON ||
                b.type == LandmarkType.MUELLE_LOS_MUERTOS ||
                b.type == LandmarkType.FARO_MARINA ||
                b.type == LandmarkType.BRIDGE_CUALE_PEDESTRIAN ||
                b.type == LandmarkType.BRIDGE_CUALE_VEHICULAR
            )
        }
        val resorts = heroBuildings.filter { b ->
            !b.isProceduralFill && (
                b.type.name.startsWith("RESORT_") ||
                b.type == LandmarkType.HOTEL_ROSITA ||
                b.type == LandmarkType.AIRPORT_TERMINAL ||
                b.type == LandmarkType.AIRPORT_TOWER
            )
        }
        return monuments to resorts
    }

    private fun processWayPolygon(
        id: Long,
        tags: JSONObject?,
        ring: List<Pair<Float, Float>>,
        cx: Float,
        cz: Float,
        isPool: Boolean,
        monuments: List<WorldBuilding>,
        resorts: List<WorldBuilding>,
        rawBuildings: MutableList<Pair<Float, OsmBuildingFootprint>>,
        pools: MutableList<OsmPoolFootprint>,
        namedPlaces: MutableList<OsmNamedPlace>
    ) {
        if (VallartaGeography.signedCoastDist(cx, cz) < -4f) return

        if (isPool) {
            if (pools.size < 200 && resorts.none { hypot(it.x - cx, it.z - cz) < 36f }) {
                pools += OsmPoolFootprint(id = id, centerX = cx, centerZ = cz, ring = ring)
            }
            return
        }

        if (monuments.any { hypot(it.x - cx, it.z - cz) < 24f }) return
        if (resorts.any { hypot(it.x - cx, it.z - cz) < 38f }) return

        val name = tags?.optString("name", "")?.takeIf { it.isNotBlank() }
        val levels = tags?.optString("building:levels", "")?.toFloatOrNull()
        val rawHeight = tags?.optString("height", "")?.replace("m", "")?.trim()?.toFloatOrNull()
        val bType = tags?.optString("building", "") ?: ""
        val tourism = tags?.optString("tourism", "") ?: ""
        val amenity = tags?.optString("amenity", "") ?: ""
        val buildingColour = tags?.optString("building:colour", "") ?: ""
        val roofColour = tags?.optString("roof:colour", "") ?: ""

        val isHotel = tourism == "hotel" || tourism == "resort" || bType == "hotel"
        val isCondo = bType == "apartments" || (bType == "residential" && (levels != null && levels >= 4f))
        val isCommercial = bType == "commercial" || bType == "retail" || bType == "supermarket" || amenity == "restaurant"
        val nearCoast = VallartaGeography.signedCoastDist(cx, cz) < 240f
        val idHash = abs((id xor (id ushr 16)).toInt())

        val height = when {
            rawHeight != null -> rawHeight.coerceIn(4.5f, 88f)
            levels != null -> (levels * 3.35f).coerceIn(4.5f, 84f)
            isHotel -> 22f + (idHash % 6) * 5.2f
            isCondo -> 15f + (idHash % 5) * 4.2f
            isCommercial -> 8.5f + (idHash % 3) * 3.0f
            nearCoast -> 10.5f + (idHash % 5) * 3.8f
            else -> 6.0f + (idHash % 4) * 2.2f
        }

        val matId = when {
            height >= 24f || (isCondo && height >= 16f) -> MaterialId.CONDO_FACADE
            isHotel || isCommercial || height >= 13f -> MaterialId.RESORT_FACADE
            else -> MaterialId.COLONIAL_FACADE
        }

        val parsedWallColor = parseOsmColor(buildingColour)
        val paletteCol = VALLARTA_WALL_PALETTE[idHash % VALLARTA_WALL_PALETTE.size]
        val colR = parsedWallColor?.get(0) ?: paletteCol[0]
        val colG = parsedWallColor?.get(1) ?: paletteCol[1]
        val colB = parsedWallColor?.get(2) ?: paletteCol[2]

        val parsedRoofColor = parseOsmColor(roofColour)
        val roofR = parsedRoofColor?.get(0) ?: if (matId == MaterialId.COLONIAL_FACADE) 0.72f + (idHash % 3) * 0.04f else 0.78f
        val roofG = parsedRoofColor?.get(1) ?: if (matId == MaterialId.COLONIAL_FACADE) 0.34f + (idHash % 3) * 0.03f else 0.80f
        val roofB = parsedRoofColor?.get(2) ?: if (matId == MaterialId.COLONIAL_FACADE) 0.22f + (idHash % 2) * 0.03f else 0.82f

        val fp = OsmBuildingFootprint(
            id = id,
            name = name,
            centerX = cx,
            centerZ = cz,
            height = height,
            materialId = matId,
            colorR = colR, colorG = colG, colorB = colB,
            roofR = roofR, roofG = roofG, roofB = roofB,
            ring = ring
        )
        val priority = height + (if (isHotel) 60f else 0f) + (if (name != null) 30f else 0f) + (if (nearCoast) 25f else 0f)
        rawBuildings += (priority to fp)

        if (name != null && (isHotel || height >= 15f || tourism.isNotBlank())) {
            val groundY = max(2.0f, VallartaGeography.terrainHeight(cx, cz))
            namedPlaces += OsmNamedPlace(
                id = "osm_poi_$id",
                name = name,
                x = cx,
                y = groundY + height,
                z = cz
            )
        }
    }

    private fun executeOverpassWithFailover(query: String): String? {
        val encoded = "data=" + java.net.URLEncoder.encode(query, "UTF-8")
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        for (url in OVERPASS_ENDPOINTS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "VallartaViceAndroid3D/5.0")
                    .post(encoded.toRequestBody(mediaType))
                    .build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string()
                        if (!bodyStr.isNullOrBlank() && bodyStr.trimStart().startsWith("{")) {
                            return bodyStr
                        }
                    }
                }
            } catch (_: Exception) {
                // Try next Overpass mirror
            }
        }
        return null
    }

    private fun parseOsmColor(raw: String): FloatArray? {
        val s = raw.trim().lowercase()
        if (s.isEmpty()) return null
        if (s.startsWith("#") && s.length == 7) {
            return try {
                val r = s.substring(1, 3).toInt(16) / 255f
                val g = s.substring(3, 5).toInt(16) / 255f
                val b = s.substring(5, 7).toInt(16) / 255f
                floatArrayOf(r.coerceIn(0.25f, 0.98f), g.coerceIn(0.25f, 0.98f), b.coerceIn(0.25f, 0.98f))
            } catch (_: Exception) {
                null
            }
        }
        return when (s) {
            "white" -> floatArrayOf(0.96f, 0.95f, 0.93f)
            "beige", "cream", "ivory" -> floatArrayOf(0.95f, 0.90f, 0.80f)
            "yellow", "ochre" -> floatArrayOf(0.95f, 0.84f, 0.52f)
            "orange", "terracotta", "peach", "salmon" -> floatArrayOf(0.92f, 0.64f, 0.48f)
            "red", "maroon" -> floatArrayOf(0.82f, 0.36f, 0.28f)
            "pink", "coral" -> floatArrayOf(0.93f, 0.68f, 0.66f)
            "brown", "tan" -> floatArrayOf(0.76f, 0.60f, 0.46f)
            "blue", "lightblue", "cyan" -> floatArrayOf(0.74f, 0.86f, 0.94f)
            "green", "lightgreen" -> floatArrayOf(0.80f, 0.90f, 0.82f)
            "grey", "gray", "silver" -> floatArrayOf(0.82f, 0.84f, 0.86f)
            else -> null
        }
    }
}
