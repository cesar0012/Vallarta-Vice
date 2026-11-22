package com.example.game.engine

import com.example.game.world.VallartaGeography
import kotlin.math.*

/**
 * Architectural 3D style codes driving authentic Google-Maps-3D-level Puerto Vallarta building geometry:
 *   0 = Mexican inland house / commercial building (flat roof + concrete parapet `pretil` + rooftop `tinaco`)
 *   1 = Historic Puerto Vallarta Colonial (Centro, Zona Romántica, 5 de Diciembre — sloped red clay tile roof + eaves)
 *   2 = Beachfront Luxury Resort (Westin, Sheraton, Meliá, Club Regina, Crown Paradise, Hyatt Ziva, Barceló —
 *       3D PROTRUDING FLOOR-BY-FLOOR CONCRETE BALCONY SLABS wrapping the exact real OSM multi-wing polygon!)
 *   3 = Modern Oceanfront Glass Skyscraper (Icon Vallarta, Peninsula, Grand Venetian, Secrets, Almar —
 *       reflective curtain-wall glass + 3D cantilevered white concrete slabs + glass penthouse crown!)
 *   4 = Traditional Mexican Hacienda Resort (Flamingo Vallarta Hotel & Marina, Hotel Rosita, Krystal —
 *       warm arched stucco + steep red terracotta pitched roofs + colonial cupola dome!)
 *   5 = Grand Mexican Beachfront Resort (Velas, Marriott — 3D balcony tiers + authentic conical thatched palapa roof!)
 */
object BuildingStyle {
    const val MEXICAN_FLAT = 0
    const val COLONIAL_TILE = 1
    const val RESORT_BALCONIES = 2
    const val GLASS_SKYSCRAPER = 3
    const val HACIENDA_RESORT = 4
    const val GRAND_PALAPA_RESORT = 5
    const val SMALL_SHOP_AWNING = 6
    const val BEACH_PALAPA_CLUB = 7
}

data class OsmBuildingFootprint(
    val id: Long,
    val name: String?,
    val centerX: Float,
    val centerZ: Float,
    val height: Float,
    val materialId: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val roofR: Float,
    val roofG: Float,
    val roofB: Float,
    val ring: List<Pair<Float, Float>>, // (worldX, worldZ) in exact 1:1 meters
    val style: Int = BuildingStyle.MEXICAN_FLAT
)

data class OsmPoolFootprint(
    val id: Long,
    val centerX: Float,
    val centerZ: Float,
    val ring: List<Pair<Float, Float>>
)

data class OsmSectorChunk(
    val sectorKey: String,
    val meshData: MeshData,
    val buildingCount: Int,
    val poolCount: Int = 0,
    val namedPlaces: List<OsmNamedPlace>,
    val colliders: List<com.example.game.world.ColliderBox> = emptyList()
)

data class OsmNamedPlace(
    val id: String,
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * High-Detail 1:1 Puerto Vallarta 3D Architecture Extruder.
 * Extrudes real OpenStreetMap building & hotel footprints + swimming pools into batched OpenGL ES 3.0
 * sector meshes with genuine 3D protruding balcony ledges, cantilevered skyscraper decks, steep
 * red terracotta hip roofs, hacienda cupolas (Flamingo Vallarta), thatched palapas, rooftop
 * Mexican pretiles/tinacos, and shimmering turquoise swimming pools!
 */
object OsmPolygonMeshBuilder {

    fun buildSectorMesh(
        footprints: List<OsmBuildingFootprint>,
        pools: List<OsmPoolFootprint> = emptyList()
    ): MeshData {
        val mb = MeshBuilder(footprints.size * 72 + pools.size * 18 + 64)

        for (fp in footprints) {
            val pts = fp.ring
            if (pts.size < 3) continue

            val signedArea = computeSignedAreaXZ(pts)
            val absArea = abs(signedArea)
            if (absArea < 4.0f) continue
            val ordered = if (signedArea < 0f) pts.asReversed() else pts
            val n = ordered.size

            // Anchor base cleanly beneath smooth local terrain so hillside buildings never float
            var minGroundY = VallartaGeography.terrainHeight(fp.centerX, fp.centerZ)
            var maxGroundY = minGroundY
            for ((px, pz) in ordered) {
                val gy = VallartaGeography.terrainHeight(px, pz)
                if (gy < minGroundY) minGroundY = gy
                if (gy > maxGroundY) maxGroundY = gy
            }
            val baseY = max(1.0f, minGroundY) - 1.1f
            val topY = max(1.6f, maxGroundY) + fp.height

            val cx = ordered.map { it.first }.average().toFloat()
            val cz = ordered.map { it.second }.average().toFloat()
            val style = fp.style

            // Special Style 7: Open-sided Beachfront Palapa Club / Marisquería right on the golden sand!
            if (style == BuildingStyle.BEACH_PALAPA_CLUB) {
                val deckY = max(1.65f, maxGroundY) + 0.28f
                val postTopY = deckY + 3.4f
                val apexY = postTopY + (2.6f + sqrt(absArea).coerceAtMost(14f) * 0.16f)
                // Warm elevated wooden plank beach deck
                val deckCenter = mb.addVertex(cx, deckY, cz, 0f, 1f, 0f, 0.56f, 0.39f, 0.24f, MaterialId.MATTE)
                val d0 = mb.addVertex(ordered[0].first, deckY, ordered[0].second, 0f, 1f, 0f, 0.50f, 0.34f, 0.20f, MaterialId.MATTE)
                var dPrev = d0
                for (i in 1 until n) {
                    val dCur = mb.addVertex(ordered[i].first, deckY, ordered[i].second, 0f, 1f, 0f, 0.50f, 0.34f, 0.20f, MaterialId.MATTE)
                    mb.addTriangle(deckCenter, dCur, dPrev)
                    dPrev = dCur
                }
                mb.addTriangle(deckCenter, d0, dPrev)
                // Rustic dark-wood palapa support posts at polygon corners
                for ((px, pz) in ordered) {
                    val ix = cx + (px - cx) * 0.84f
                    val iz = cz + (pz - cz) * 0.84f
                    mb.addCylinderY(ix, deckY - 0.8f, postTopY, iz, 0.16f, 0.14f, 0.36f, 0.22f, 0.12f, MaterialId.MATTE, slices = 5)
                }
                // Warm overhanging golden thatched palm-frond roof pyramid!
                val apexIdx = mb.addVertex(cx, apexY, cz, 0f, 1f, 0f, 0.82f, 0.66f, 0.38f, MaterialId.TERRAIN)
                val overhang = ordered.map { (px, pz) ->
                    (cx + (px - cx) * 1.18f) to (cz + (pz - cz) * 1.18f)
                }
                val r0 = mb.addVertex(overhang[0].first, postTopY - 0.25f, overhang[0].second, 0f, 0.7f, 0f, 0.68f, 0.50f, 0.26f, MaterialId.TERRAIN)
                var rPrev = r0
                for (i in 1 until n) {
                    val rCur = mb.addVertex(overhang[i].first, postTopY - 0.25f, overhang[i].second, 0f, 0.7f, 0f, 0.68f, 0.50f, 0.26f, MaterialId.TERRAIN)
                    mb.addTriangle(apexIdx, rCur, rPrev)
                    rPrev = rCur
                }
                mb.addTriangle(apexIdx, r0, rPrev)
                // Glowing warm string-light ceiling lantern under the palapa!
                mb.addEllipsoid(cx, postTopY - 0.35f, cz, 0.85f, 0.32f, 0.85f, 1.0f, 0.88f, 0.58f, MaterialId.EMISSIVE, stacks = 4, slices = 6)
                continue
            }

            val isTiledRoof = style == BuildingStyle.COLONIAL_TILE || style == BuildingStyle.HACIENDA_RESORT || style == BuildingStyle.SMALL_SHOP_AWNING
            val isResortBalcony = style == BuildingStyle.RESORT_BALCONIES || style == BuildingStyle.GRAND_PALAPA_RESORT
            val isGlassTower = style == BuildingStyle.GLASS_SKYSCRAPER
            val isSmallShop = style == BuildingStyle.SMALL_SHOP_AWNING
            val hasLobbyBand = fp.height >= 12.0f
            val lobbyTopY = max(1.8f, maxGroundY) + 3.8f

            // Precompute outward edge normals for crisp 3D protruding balconies, awnings, cornices & eaves
            val edgeNx = FloatArray(n)
            val edgeNz = FloatArray(n)
            val edgeLen = FloatArray(n)
            for (i in 0 until n) {
                val (x0, z0) = ordered[i]
                val (x1, z1) = ordered[(i + 1) % n]
                val dx = x1 - x0; val dz = z1 - z0
                val l = hypot(dx, dz)
                edgeLen[i] = l
                if (l > 0.05f) {
                    edgeNx[i] = dz / l
                    edgeNz[i] = -dx / l
                }
            }

            // 1. Extruded Architectural Facade Walls with world-scaled UVs
            for (i in 0 until n) {
                val segLen = edgeLen[i]
                if (segLen < 0.35f) continue
                val (x0, z0) = ordered[i]
                val (x1, z1) = ordered[(i + 1) % n]
                val nx = edgeNx[i]; val nz = edgeNz[i]
                val uSpan = (segLen / 10.5f).coerceAtLeast(0.35f)

                if (hasLobbyBand && lobbyTopY < topY - 2.0f) {
                    // Ground-floor recessed resort lobby / colonnade / glass storefront podium
                    val lobR = fp.colorR * 0.78f
                    val lobG = fp.colorG * 0.77f
                    val lobB = fp.colorB * 0.75f
                    val lobMat = if (isGlassTower) MaterialId.GLASS else MaterialId.COLONIAL_FACADE
                    val lv0 = mb.addVertex(x0, baseY, z0, nx, 0f, nz, lobR, lobG, lobB, lobMat, 0f, 0f)
                    val lv1 = mb.addVertex(x1, baseY, z1, nx, 0f, nz, lobR, lobG, lobB, lobMat, uSpan, 0f)
                    val lv2 = mb.addVertex(x1, lobbyTopY, z1, nx, 0f, nz, lobR, lobG, lobB, lobMat, uSpan, 0.42f)
                    val lv3 = mb.addVertex(x0, lobbyTopY, z0, nx, 0f, nz, lobR, lobG, lobB, lobMat, 0f, 0.42f)
                    mb.addQuad(lv0, lv1, lv2, lv3)

                    // Upper architectural facade
                    val upperH = (topY - lobbyTopY).coerceAtLeast(1f)
                    val vSpan = (upperH / 10.0f).coerceAtLeast(0.45f)
                    val v0 = mb.addVertex(x0, lobbyTopY, z0, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, 0f, 0f)
                    val v1 = mb.addVertex(x1, lobbyTopY, z1, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, uSpan, 0f)
                    val v2 = mb.addVertex(x1, topY, z1, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, uSpan, vSpan)
                    val v3 = mb.addVertex(x0, topY, z0, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, 0f, vSpan)
                    mb.addQuad(v0, v1, v2, v3)
                } else {
                    val vSpan = ((topY - baseY) / 9.5f).coerceAtLeast(0.45f)
                    val v0 = mb.addVertex(x0, baseY, z0, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, 0f, 0f)
                    val v1 = mb.addVertex(x1, baseY, z1, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, uSpan, 0f)
                    val v2 = mb.addVertex(x1, topY, z1, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, uSpan, vSpan)
                    val v3 = mb.addVertex(x0, topY, z0, nx, 0f, nz, fp.colorR, fp.colorG, fp.colorB, fp.materialId, 0f, vSpan)
                    mb.addQuad(v0, v1, v2, v3)
                }

                // 1b. SMALL LOCAL BUSINESSES (Taquerías, Cafés, OXXO, Galerías, Tiendas — 3D colorful sloped awnings + shopfront glass!)
                if (isSmallShop && segLen >= 4.2f && i % 2 == 0) {
                    val shopFloorY = max(1.8f, maxGroundY)
                    val awningTopY = (shopFloorY + 3.15f).coerceAtMost(topY - 0.55f)
                    val awningBotY = awningTopY - 0.65f
                    val awOut = 1.65f
                    // Vibrant Vallarta business awning color (red, turquoise, marigold, emerald, magenta, cobalt)
                    val (ar, ag, ab) = when ((fp.id % 6L).toInt()) {
                        0 -> Triple(0.88f, 0.20f, 0.22f) // Taquería / OXXO crimson red
                        1 -> Triple(0.06f, 0.66f, 0.72f) // Seafood marisquería turquoise
                        2 -> Triple(0.94f, 0.64f, 0.12f) // Café / Panadería marigold gold
                        3 -> Triple(0.14f, 0.58f, 0.30f) // Mexican cantina emerald green
                        4 -> Triple(0.82f, 0.16f, 0.46f) // Art gallery bougainvillea pink
                        else -> Triple(0.16f, 0.38f, 0.78f) // Surf shop / Boutique cobalt
                    }
                    // Protruding glass shopfront window beneath awning
                    val gx0 = x0 + nx * 0.14f; val gz0 = z0 + nz * 0.14f
                    val gx1 = x1 + nx * 0.14f; val gz1 = z1 + nz * 0.14f
                    val w0 = mb.addVertex(gx0, shopFloorY + 0.35f, gz0, nx, 0f, nz, 0.16f, 0.26f, 0.34f, MaterialId.GLASS)
                    val w1 = mb.addVertex(gx1, shopFloorY + 0.35f, gz1, nx, 0f, nz, 0.16f, 0.26f, 0.34f, MaterialId.GLASS)
                    val w2 = mb.addVertex(gx1, awningBotY + 0.15f, gz1, nx, 0f, nz, 0.24f, 0.38f, 0.48f, MaterialId.GLASS)
                    val w3 = mb.addVertex(gx0, awningBotY + 0.15f, gz0, nx, 0f, nz, 0.24f, 0.38f, 0.48f, MaterialId.GLASS)
                    mb.addQuad(w0, w1, w2, w3)

                    // 3D sloped colorful fabric awning (`toldo`) projecting over the sidewalk!
                    val ax0 = x0 + nx * awOut; val az0 = z0 + nz * awOut
                    val ax1 = x1 + nx * awOut; val az1 = z1 + nz * awOut
                    val a0 = mb.addVertex(x0, awningTopY, z0, nx * 0.45f, 0.89f, nz * 0.45f, ar, ag, ab, MaterialId.MATTE)
                    val a1 = mb.addVertex(x1, awningTopY, z1, nx * 0.45f, 0.89f, nz * 0.45f, ar, ag, ab, MaterialId.MATTE)
                    val a2 = mb.addVertex(ax1, awningBotY, az1, nx * 0.45f, 0.89f, nz * 0.45f, ar * 0.90f, ag * 0.90f, ab * 0.90f, MaterialId.MATTE)
                    val a3 = mb.addVertex(ax0, awningBotY, az0, nx * 0.45f, 0.89f, nz * 0.45f, ar * 0.90f, ag * 0.90f, ab * 0.90f, MaterialId.MATTE)
                    mb.addQuad(a0, a1, a2, a3)
                    // Bright white/cream front valance lip on the awning
                    val v0 = mb.addVertex(ax0, awningBotY - 0.24f, az0, nx, 0f, nz, 0.97f, 0.95f, 0.90f, MaterialId.MATTE)
                    val v1 = mb.addVertex(ax1, awningBotY - 0.24f, az1, nx, 0f, nz, 0.97f, 0.95f, 0.90f, MaterialId.MATTE)
                    val v2 = mb.addVertex(ax1, awningBotY, az1, nx, 0f, nz, 0.97f, 0.95f, 0.90f, MaterialId.MATTE)
                    val v3 = mb.addVertex(ax0, awningBotY, az0, nx, 0f, nz, 0.97f, 0.95f, 0.90f, MaterialId.MATTE)
                    mb.addQuad(v0, v1, v2, v3)
                }

                // 2. REAL 3D PROTRUDING FLOOR-BY-FLOOR BALCONY SLABS on Beachfront Resorts & Glass Towers!
                if ((isResortBalcony || isGlassTower) && fp.height >= 13f && segLen >= 3.2f) {
                    val floorStep = if (isGlassTower) 6.4f else 3.6f
                    val out = if (isGlassTower) 0.72f else 0.88f
                    val slabThick = if (isGlassTower) 0.46f else 0.38f
                    val railH = if (isGlassTower) 0.76f else 0.72f
                    val sr = if (isGlassTower) 0.96f else (fp.colorR * 0.99f).coerceAtMost(1f)
                    val sg = if (isGlassTower) 0.97f else (fp.colorG * 0.98f).coerceAtMost(1f)
                    val sb = if (isGlassTower) 0.99f else (fp.colorB * 0.96f).coerceAtMost(1f)

                    val ox0 = x0 + nx * out; val oz0 = z0 + nz * out
                    val ox1 = x1 + nx * out; val oz1 = z1 + nz * out

                    var fy = lobbyTopY + 1.2f
                    var tiers = 0
                    val maxTiers = if (n > 18) 8 else 14
                    while (fy < topY - 1.2f && tiers < maxTiers) {
                        // Top horizontal concrete terrace ledge
                        mb.addFlatQuad(
                            x0, fy + slabThick, z0,
                            x1, fy + slabThick, z1,
                            ox1, fy + slabThick, oz1,
                            ox0, fy + slabThick, oz0,
                            sr, sg, sb, MaterialId.MATTE
                        )
                        // Outer white/cream balcony fascia rim catching sunlight
                        val b0 = mb.addVertex(ox0, fy, oz0, nx, 0f, nz, sr, sg, sb, MaterialId.MATTE)
                        val b1 = mb.addVertex(ox1, fy, oz1, nx, 0f, nz, sr, sg, sb, MaterialId.MATTE)
                        val b2 = mb.addVertex(ox1, fy + slabThick, oz1, nx, 0f, nz, sr, sg, sb, MaterialId.MATTE)
                        val b3 = mb.addVertex(ox0, fy + slabThick, oz0, nx, 0f, nz, sr, sg, sb, MaterialId.MATTE)
                        mb.addQuad(b0, b1, b2, b3)

                        // Tinted glass balcony railing panel above the slab on tall beachfront resorts
                        if (isGlassTower || tiers % 2 == 0) {
                            val r0 = mb.addVertex(ox0, fy + slabThick, oz0, nx, 0f, nz, 0.22f, 0.56f, 0.74f, MaterialId.GLASS)
                            val r1 = mb.addVertex(ox1, fy + slabThick, oz1, nx, 0f, nz, 0.22f, 0.56f, 0.74f, MaterialId.GLASS)
                            val r2 = mb.addVertex(ox1, fy + slabThick + railH, oz1, nx, 0f, nz, 0.30f, 0.66f, 0.84f, MaterialId.GLASS)
                            val r3 = mb.addVertex(ox0, fy + slabThick + railH, oz0, nx, 0f, nz, 0.30f, 0.66f, 0.84f, MaterialId.GLASS)
                            mb.addQuad(r0, r1, r2, r3)
                        }

                        fy += floorStep
                        tiers++
                    }
                }

                // 3. Architectural Roof Parapet (`Pretil` on Mexican houses / Cornice on resorts / Eaves on colonial)
                if (!isTiledRoof) {
                    val parapetH = if (style == BuildingStyle.MEXICAN_FLAT) 0.72f else 0.80f
                    val out = if (isResortBalcony || isGlassTower) 0.42f else 0.18f
                    val ox0 = x0 + nx * out; val oz0 = z0 + nz * out
                    val ox1 = x1 + nx * out; val oz1 = z1 + nz * out
                    val pr = (fp.colorR * 0.94f).coerceIn(0f, 1f)
                    val pg = (fp.colorG * 0.93f).coerceIn(0f, 1f)
                    val pb = (fp.colorB * 0.92f).coerceIn(0f, 1f)
                    val c0 = mb.addVertex(ox0, topY - 0.15f, oz0, nx, 0.08f, nz, pr, pg, pb, MaterialId.MATTE)
                    val c1 = mb.addVertex(ox1, topY - 0.15f, oz1, nx, 0.08f, nz, pr, pg, pb, MaterialId.MATTE)
                    val c2 = mb.addVertex(ox1, topY + parapetH, oz1, nx, 0.08f, nz, pr, pg, pb, MaterialId.MATTE)
                    val c3 = mb.addVertex(ox0, topY + parapetH, oz0, nx, 0.08f, nz, pr, pg, pb, MaterialId.MATTE)
                    mb.addQuad(c0, c1, c2, c3)
                } else {
                    // Authentic Puerto Vallarta overhanging red-clay terracotta roof eaves (`teja roja`)!
                    val eaveOut = 0.55f
                    val ox0 = x0 + nx * eaveOut; val oz0 = z0 + nz * eaveOut
                    val ox1 = x1 + nx * eaveOut; val oz1 = z1 + nz * eaveOut
                    val e0 = mb.addVertex(ox0, topY - 0.25f, oz0, nx, -0.3f, nz, 0.54f, 0.26f, 0.15f, MaterialId.MATTE)
                    val e1 = mb.addVertex(ox1, topY - 0.25f, oz1, nx, -0.3f, nz, 0.54f, 0.26f, 0.15f, MaterialId.MATTE)
                    val e2 = mb.addVertex(ox1, topY + 0.18f, oz1, nx, 0.4f, nz, fp.roofR, fp.roofG, fp.roofB, MaterialId.MATTE)
                    val e3 = mb.addVertex(ox0, topY + 0.18f, oz0, nx, 0.4f, nz, fp.roofR, fp.roofG, fp.roofB, MaterialId.MATTE)
                    mb.addQuad(e0, e1, e2, e3)
                }
            }

            // 4. Real Polygon Roof Cap (steep terracotta hipped roof on Colonial & Hacienda, crisp deck on modern)
            val roofApexY = when (style) {
                BuildingStyle.HACIENDA_RESORT -> topY + (2.4f + sqrt(absArea).coerceAtMost(28f) * 0.09f)
                BuildingStyle.COLONIAL_TILE -> topY + (1.55f + sqrt(absArea).coerceAtMost(18f) * 0.07f)
                BuildingStyle.GLASS_SKYSCRAPER -> topY + 0.30f
                else -> topY + 0.18f
            }
            val centerIdx = mb.addVertex(cx, roofApexY, cz, 0f, 1f, 0f, fp.roofR, fp.roofG, fp.roofB, MaterialId.MATTE)
            val rimStart = mb.addVertex(
                ordered[0].first, topY + 0.12f, ordered[0].second, 0f, 1f, 0f,
                fp.roofR * 0.86f, fp.roofG * 0.86f, fp.roofB * 0.86f, MaterialId.MATTE
            )
            var prevIdx = rimStart
            for (i in 1 until n) {
                val (px, pz) = ordered[i]
                val curIdx = mb.addVertex(
                    px, topY + 0.12f, pz, 0f, 1f, 0f,
                    fp.roofR * 0.86f, fp.roofG * 0.86f, fp.roofB * 0.86f, MaterialId.MATTE
                )
                mb.addTriangle(centerIdx, curIdx, prevIdx)
                prevIdx = curIdx
            }
            mb.addTriangle(centerIdx, rimStart, prevIdx)

            // 5. Signature Architectural Roof Crowns per Hotel / Building Style!
            if (style == BuildingStyle.HACIENDA_RESORT && absArea > 400f) {
                // Iconic Hacienda Cupola Dome (e.g. Flamingo Vallarta Hotel & Marina + Hotel Rosita!)
                val domeBase = roofApexY - 0.35f
                mb.addCylinderY(cx, domeBase, domeBase + 2.2f, cz, 3.6f, 3.4f, 0.96f, 0.90f, 0.78f, MaterialId.COLONIAL_FACADE, slices = 10)
                mb.addEllipsoid(cx, domeBase + 2.4f, cz, 3.7f, 2.2f, 3.7f, 0.78f, 0.30f, 0.16f, MaterialId.MATTE, stacks = 6, slices = 12)
                mb.addCylinderY(cx, domeBase + 4.3f, domeBase + 5.6f, cz, 0.22f, 0.08f, 0.94f, 0.78f, 0.32f, MaterialId.CHROME, slices = 6)
            } else if (style == BuildingStyle.GRAND_PALAPA_RESORT && absArea > 700f) {
                // Iconic Conical Thatched Mexican Palapa Roof Crown (Velas, Marriott!)
                val pBase = topY + 0.2f
                mb.addCylinderY(cx, pBase, pBase + 4.6f, cz, 7.8f, 0.4f, 0.76f, 0.62f, 0.36f, MaterialId.TERRAIN, slices = 12)
                mb.addCylinderY(cx, pBase - 0.1f, pBase + 0.4f, cz, 8.2f, 7.8f, 0.64f, 0.49f, 0.27f, MaterialId.TERRAIN, slices = 12)
            } else if ((isGlassTower || isResortBalcony) && fp.height >= 24f && n in 4..16) {
                // Stepped Illuminated Glass Penthouse Terrace / Mechanical Crown on tall resorts & skyscrapers!
                val inset = 0.62f
                val phBaseY = topY + 0.10f
                val phTopY = topY + (3.2f + (fp.id % 3L) * 1.1f)
                val phRing = ordered.map { (px, pz) ->
                    (cx + (px - cx) * inset) to (cz + (pz - cz) * inset)
                }
                for (i in 0 until n) {
                    val (x0, z0) = phRing[i]
                    val (x1, z1) = phRing[(i + 1) % n]
                    val dx = x1 - x0; val dz = z1 - z0
                    val segLen = hypot(dx, dz)
                    if (segLen < 0.3f) continue
                    val nx = dz / segLen; val nz = -dx / segLen
                    val pv0 = mb.addVertex(x0, phBaseY, z0, nx, 0f, nz, fp.colorR * 0.94f, fp.colorG * 0.95f, fp.colorB * 0.98f, MaterialId.CONDO_FACADE, 0f, 0f)
                    val pv1 = mb.addVertex(x1, phBaseY, z1, nx, 0f, nz, fp.colorR * 0.94f, fp.colorG * 0.95f, fp.colorB * 0.98f, MaterialId.CONDO_FACADE, segLen / 8f, 0f)
                    val pv2 = mb.addVertex(x1, phTopY, z1, nx, 0f, nz, fp.colorR * 0.94f, fp.colorG * 0.95f, fp.colorB * 0.98f, MaterialId.CONDO_FACADE, segLen / 8f, 0.35f)
                    val pv3 = mb.addVertex(x0, phTopY, z0, nx, 0f, nz, fp.colorR * 0.94f, fp.colorG * 0.95f, fp.colorB * 0.98f, MaterialId.CONDO_FACADE, 0f, 0.35f)
                    mb.addQuad(pv0, pv1, pv2, pv3)
                }
                val phCenter = mb.addVertex(cx, phTopY + 0.14f, cz, 0f, 1f, 0f, fp.roofR, fp.roofG, fp.roofB, MaterialId.MATTE)
                val phRim0 = mb.addVertex(phRing[0].first, phTopY, phRing[0].second, 0f, 1f, 0f, fp.roofR * 0.94f, fp.roofG * 0.94f, fp.roofB * 0.94f, MaterialId.MATTE)
                var phPrev = phRim0
                for (i in 1 until n) {
                    val (px, pz) = phRing[i]
                    val phCur = mb.addVertex(px, phTopY, pz, 0f, 1f, 0f, fp.roofR * 0.94f, fp.roofG * 0.94f, fp.roofB * 0.94f, MaterialId.MATTE)
                    mb.addTriangle(phCenter, phCur, phPrev)
                    phPrev = phCur
                }
                mb.addTriangle(phCenter, phRim0, phPrev)
            } else if (style == BuildingStyle.MEXICAN_FLAT && absArea in 35f..260f && fp.id % 2L == 0L) {
                // Authentic Mexican neighborhood rooftop water tank (`tinaco`)!
                val tx = cx + ((fp.id % 5L) - 2L) * 0.7f
                val tz = cz + (((fp.id / 5L) % 5L) - 2L) * 0.7f
                mb.addCylinderY(tx, topY + 0.15f, topY + 1.35f, tz, 0.56f, 0.52f, 0.14f, 0.15f, 0.17f, MaterialId.MATTE, slices = 7)
            }
        }

        // 6. Real OpenStreetMap Swimming Pools (`leisure=swimming_pool`) — shimmering turquoise water + travertine deck!
        for (pool in pools) {
            val pts = pool.ring
            if (pts.size < 3) continue
            val signedArea = computeSignedAreaXZ(pts)
            if (abs(signedArea) < 4.0f) continue
            val ordered = if (signedArea < 0f) pts.asReversed() else pts
            val n = ordered.size

            val cx = ordered.map { it.first }.average().toFloat()
            val cz = ordered.map { it.second }.average().toFloat()
            val groundY = max(1.85f, VallartaGeography.terrainHeight(cx, cz))
            val copingY = groundY + 0.15f
            val waterY = groundY + 0.22f

            val copingCenter = mb.addVertex(cx, copingY, cz, 0f, 1f, 0f, 0.93f, 0.91f, 0.87f, MaterialId.MATTE)
            val copingOuter = ordered.map { (px, pz) ->
                val dx = px - cx; val dz = pz - cz
                val len = hypot(dx, dz).coerceAtLeast(0.5f)
                (px + (dx / len) * 1.25f) to (pz + (dz / len) * 1.25f)
            }
            val cRim0 = mb.addVertex(copingOuter[0].first, copingY, copingOuter[0].second, 0f, 1f, 0f, 0.93f, 0.91f, 0.87f, MaterialId.MATTE)
            var cPrev = cRim0
            for (i in 1 until n) {
                val (px, pz) = copingOuter[i]
                val cCur = mb.addVertex(px, copingY, pz, 0f, 1f, 0f, 0.93f, 0.91f, 0.87f, MaterialId.MATTE)
                mb.addTriangle(copingCenter, cCur, cPrev)
                cPrev = cCur
            }
            mb.addTriangle(copingCenter, cRim0, cPrev)

            val waterCenter = mb.addVertex(cx, waterY, cz, 0f, 1f, 0f, 0.08f, 0.80f, 0.94f, MaterialId.POOL_WATER)
            val wRim0 = mb.addVertex(ordered[0].first, waterY, ordered[0].second, 0f, 1f, 0f, 0.06f, 0.74f, 0.90f, MaterialId.POOL_WATER)
            var wPrev = wRim0
            for (i in 1 until n) {
                val (px, pz) = ordered[i]
                val wCur = mb.addVertex(px, waterY, pz, 0f, 1f, 0f, 0.06f, 0.74f, 0.90f, MaterialId.POOL_WATER)
                mb.addTriangle(waterCenter, wCur, wPrev)
                wPrev = wCur
            }
            mb.addTriangle(waterCenter, wRim0, wPrev)
        }

        return mb.build()
    }

    private fun computeSignedAreaXZ(pts: List<Pair<Float, Float>>): Float {
        var area = 0f
        for (i in pts.indices) {
            val (x0, z0) = pts[i]
            val (x1, z1) = pts[(i + 1) % pts.size]
            area += (x0 * z1 - x1 * z0)
        }
        return area * 0.5f
    }
}
