package com.example.game.engine

import com.example.game.world.ColliderBox
import com.example.game.world.OsmVallartaRepository
import com.example.game.world.VallartaGeography
import com.example.game.world.WorldColliderGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * High-Performance Batched 3D World Decoration & Lush Tropical Vegetation Sector Builder.
 * Generates THOUSANDS of detailed 3D Puerto Vallarta props & lush Sierra Madre / inland tropical
 * trees batched into 550m GPU sector chunks (`decor_sx_sz`) with solid colliders for ALL props:
 *  - Dense coconut palm groves lining beaches, Marina boardwalk, medians & hotel courtyards
 *  - Rich coastal life: lifeguard towers (`torres salvavidas`), panga fishing boats, surfboards,
 *    colorful beach umbrellas, white resort loungers (`camastros`) & coco-frío beach stands
 *  - Urban street life: Malecón & plaza colonial benches, terracotta bougainvillea planters,
 *    streetlamps, and 4 walkable stepped Mexican plaza kiosks & fountains
 *  - Dense Lush Inland & Sierra Madre Tropical Vegetation across the 0.2..5.0 km inland belt!
 */
object VallartaDecorationBuilder {

    suspend fun buildBatchedDecorationSectors(
        buildingColliderGrid: WorldColliderGrid? = null
    ): List<OsmSectorChunk> = withContext(Dispatchers.IO) {
        val builders = HashMap<String, MeshBuilder>(160)
        val collidersBySector = HashMap<String, MutableList<ColliderBox>>(64)
        fun keyFor(x: Float, z: Float): String = "decor_" + OsmVallartaRepository.sectorKey(x, z)
        fun mbFor(x: Float, z: Float): MeshBuilder {
            return builders.getOrPut(keyFor(x, z)) { MeshBuilder(1600) }
        }
        fun addSolidCollider(
            x: Float,
            z: Float,
            halfW: Float,
            halfD: Float,
            baseY: Float,
            topY: Float,
            rotDeg: Float = 0f,
            climbable: Boolean = true
        ) {
            val rad = Math.toRadians(rotDeg.toDouble())
            collidersBySector.getOrPut(keyFor(x, z)) { ArrayList(24) }.add(
                ColliderBox(
                    cx = x,
                    cz = z,
                    halfW = halfW,
                    halfD = halfD,
                    cosR = cos(rad).toFloat(),
                    sinR = sin(rad).toFloat(),
                    baseY = baseY,
                    topY = topY,
                    climbable = climbable
                )
            )
        }
        fun isNearMajorRoad(x: Float, z: Float, margin: Float = 4f): Boolean {
            for (road in VallartaGeography.roadNetwork) {
                val rHalf = road.halfWidth + margin
                val pts = road.points
                for (i in 0 until pts.size - 1) {
                    val (ax, az) = pts[i]
                    val (bx, bz) = pts[i + 1]
                    val dx = bx - ax; val dz = bz - az
                    val l2 = dx * dx + dz * dz
                    if (l2 < 0.01f) continue
                    val t = (((x - ax) * dx + (z - az) * dz) / l2).coerceIn(0f, 1f)
                    val px = ax + t * dx; val pz = az + t * dz
                    if (hypot(x - px, z - pz) < rHalf) return true
                }
            }
            return false
        }
        fun registerPalmCollider(px: Float, gy: Float, pz: Float, scale: Float) {
            val r = (0.34f * scale).coerceAtLeast(0.26f)
            addSolidCollider(px, pz, r, r, gy - 0.5f, gy + 6.8f * scale, climbable = false)
        }
        fun registerBroadleafCollider(tx: Float, gy: Float, tz: Float, scale: Float) {
            val r = (0.38f * scale).coerceAtLeast(0.28f)
            addSolidCollider(tx, tz, r, r, gy - 0.5f, gy + 3.4f * scale, climbable = false)
        }
        fun registerShrubCollider(sx: Float, gy: Float, sz: Float, scale: Float) {
            val r = (1.05f * scale).coerceAtLeast(0.65f)
            addSolidCollider(sx, sz, r, r, gy - 0.3f, gy + 1.45f * scale, climbable = false)
        }
        fun registerPlanterCollider(px: Float, gy: Float, pz: Float) {
            addSolidCollider(px, pz, 0.70f, 0.70f, gy - 0.3f, gy + 1.65f, climbable = false)
        }
        fun registerBenchAndLampCollider(bx: Float, gy: Float, bz: Float) {
            addSolidCollider(bx, bz, 0.38f, 0.98f, gy - 0.3f, gy + 0.95f, climbable = false)
            addSolidCollider(bx, bz + 1.4f, 0.24f, 0.24f, gy - 0.3f, gy + 4.5f, climbable = false)
        }
        fun registerUmbrellaSetCollider(ux: Float, gy: Float, uz: Float) {
            addSolidCollider(ux, uz, 0.24f, 0.24f, gy - 0.3f, gy + 2.4f, climbable = false)
            addSolidCollider(ux - 0.85f, uz + 0.75f, 0.88f, 0.35f, gy - 0.2f, gy + 0.45f, climbable = false)
            addSolidCollider(ux - 0.85f, uz - 0.75f, 0.88f, 0.35f, gy - 0.2f, gy + 0.45f, climbable = false)
        }

        // ==================== 1. DENSE BEACHFRONT PALM GROVES, UMBRELLAS, LOUNGERS & STREET DECOR ====================
        var z = VallartaGeography.latToWorldZ(20.6730)
        val zEnd = VallartaGeography.latToWorldZ(20.5290)
        var step = 0
        while (z < zEnd) {
            val lat = VallartaGeography.worldToLat(z)
            val cx = VallartaGeography.coastX(z)
            val (coastPt, landN, _) = VallartaGeography.coastPointAndTangentAt(lat, VallartaGeography.worldToLon(cx))

            // A. Lush double-row beach coconut palms hugging the real shoreline every 18m!
            val palmOff = 13f + (step % 4) * 3.5f
            val px = coastPt.first + landN.first * palmOff
            val pz = coastPt.second + landN.second * palmOff
            if (VallartaGeography.signedCoastDist(px, pz) > 7f &&
                !VallartaGeography.isInsideMarinaBasin(px, pz) &&
                !VallartaGeography.isInsideRioCualeChannel(px, pz)
            ) {
                val gy = max(1.55f, VallartaGeography.terrainHeight(px, pz))
                val scale = 0.85f + (step % 5) * 0.11f
                addFastPalm(mbFor(px, pz), px, gy, pz, scale = scale, seed = step)
                registerPalmCollider(px, gy, pz, scale)
            }

            // B. Beachfront umbrella + white lounger (`camastro`) clusters, panga boats, surfboards & lifeguard towers!
            if (step % 2 == 0) {
                val uOff = 9.0f + (step % 3) * 2.6f
                val ux = coastPt.first + landN.first * uOff
                val uz = coastPt.second + landN.second * uOff
                if (VallartaGeography.signedCoastDist(ux, uz) > 5.5f &&
                    !VallartaGeography.isInsideMarinaBasin(ux, uz) &&
                    !VallartaGeography.isInsideRioCualeChannel(ux, uz)
                ) {
                    val gy = max(1.25f, VallartaGeography.terrainHeight(ux, uz))
                    addBeachUmbrellaSet(mbFor(ux, uz), ux, gy, uz, seed = step)
                    registerUmbrellaSetCollider(ux, gy, uz)
                }
            }
            // Lifeguard Tower (`Torre Salvavidas`) with solid climbable structure every ~250m along the beach!
            if (step % 14 == 3) {
                val tx = coastPt.first + landN.first * 16.5f
                val tz = coastPt.second + landN.second * 16.5f
                if (VallartaGeography.signedCoastDist(tx, tz) > 12f &&
                    !VallartaGeography.isInsideMarinaBasin(tx, tz) &&
                    !VallartaGeography.isInsideRioCualeChannel(tx, tz)
                ) {
                    val gy = max(1.65f, VallartaGeography.terrainHeight(tx, tz))
                    addLifeguardTower(mbFor(tx, tz), tx, gy, tz)
                    addSolidCollider(tx, tz, 1.55f, 1.55f, gy, gy + 5.2f)
                }
            } else if (step % 9 == 5) {
                // Authentic Mexican wooden Panga fishing boat & colorful surfboards resting on the sand!
                val bx = coastPt.first + landN.first * 7.8f
                val bz = coastPt.second + landN.second * 7.8f
                if (VallartaGeography.signedCoastDist(bx, bz) > 5.5f &&
                    !VallartaGeography.isInsideMarinaBasin(bx, bz) &&
                    !VallartaGeography.isInsideRioCualeChannel(bx, bz)
                ) {
                    val gy = max(1.15f, VallartaGeography.terrainHeight(bx, bz))
                    addBeachPangaAndSurfboards(mbFor(bx, bz), bx, gy, bz, seed = step)
                    addSolidCollider(bx, bz, 2.3f, 1.0f, gy, gy + 1.0f)
                }
            } else if (step % 11 == 7) {
                // Coastal Coco-Frío / Palapa Beach Stand (solid climbable roof!)
                val sx = coastPt.first + landN.first * 20.5f
                val sz = coastPt.second + landN.second * 20.5f
                if (VallartaGeography.signedCoastDist(sx, sz) > 15f &&
                    !VallartaGeography.isInsideMarinaBasin(sx, sz) &&
                    !VallartaGeography.isInsideRioCualeChannel(sx, sz)
                ) {
                    val gy = max(1.85f, VallartaGeography.terrainHeight(sx, sz))
                    addBeachCocoStand(mbFor(sx, sz), sx, gy, sz, seed = step)
                    addSolidCollider(sx, sz, 1.9f, 1.5f, gy, gy + 3.2f)
                }
            }

            // C. Boulevard palms, colonial benches & vibrant magenta bougainvillea terracotta planters!
            val blvdX = VallartaGeography.mainBoulevardX(z)
            val sideSign = if (step % 2 == 0) -1f else 1f
            val decX = blvdX + sideSign * 13.8f
            if (VallartaGeography.signedCoastDist(decX, z) > 8f &&
                !VallartaGeography.isInsideMarinaBasin(decX, z) &&
                !VallartaGeography.isInsideRioCualeChannel(decX, z)
            ) {
                val gy = VallartaGeography.drivableHeightAt(decX, z)
                val mb = mbFor(decX, z)
                if (step % 3 == 0) {
                    val scale = 0.92f + (step % 3) * 0.10f
                    addFastPalm(mb, decX, gy, z, scale = scale, seed = step + 17)
                    registerPalmCollider(decX, gy, z, scale)
                } else if (step % 3 == 1) {
                    addBougainvilleaPlanter(mb, decX, gy, z, seed = step)
                    registerPlanterCollider(decX, gy, z)
                } else {
                    addColonialBenchAndLamp(mb, decX, gy, z, facingWest = sideSign > 0f)
                    registerBenchAndLampCollider(decX, gy, z)
                }
            }

            z += 18f
            step++
        }

        // ==================== 2. MARINA VALLARTA BOARDWALK PALMS & BOUGAINVILLEA AROUND HOTEL FLAMINGO ====================
        val (mx, mz) = VallartaGeography.getMarinaPolygonXZ()
        for (i in mx.indices step 2) {
            val x0 = mx[i]; val z0 = mz[i]
            val x1 = mx[(i + 1) % mx.size]; val z1 = mz[(i + 1) % mx.size]
            val dx = x1 - x0; val dz = z1 - z0
            val len = hypot(dx, dz)
            if (len < 6f) continue
            val midX = (x0 + x1) * 0.5f; val midZ = (z0 + z1) * 0.5f
            if (VallartaGeography.signedCoastDist(midX, midZ) < 16f) continue
            val nx = dz / len; val nz = -dx / len
            val landSide = if (!VallartaGeography.isInsideMarinaBasin(midX + nx * 4f, midZ + nz * 4f)) 1f else -1f
            val px = midX + nx * landSide * 3.8f
            val pz = midZ + nz * landSide * 3.8f
            if (!VallartaGeography.isInsideMarinaBasin(px, pz)) {
                val mb = mbFor(px, pz)
                if (i % 4 == 0) {
                    addFastPalm(mb, px, 2.08f, pz, scale = 0.95f, seed = i + 90)
                    registerPalmCollider(px, 2.08f, pz, 0.95f)
                } else {
                    addBougainvilleaPlanter(mb, px, 2.08f, pz, seed = i)
                    registerPlanterCollider(px, 2.08f, pz)
                }
            }
        }

        // ==================== 3. FOUR ICONIC PUERTO VALLARTA PLAZA KIOSKS & FOUNTAINS ====================
        val plazas = listOf(
            20.6083 to -105.2356, // Plaza de Armas (Malecón / Parroquia)
            20.6018 to -105.2384, // Parque Lázaro Cárdenas (Zona Romántica — mosaic tiles & gazebo!)
            20.6142 to -105.2314, // Parque Hidalgo (5 de Diciembre)
            20.6484 to -105.2063  // Plaza Principal El Pitillal (4.5 km inland!)
        )
        for ((pIdx, coords) in plazas.withIndex()) {
            val px = VallartaGeography.lonToWorldX(coords.second)
            val pz = VallartaGeography.latToWorldZ(coords.first)
            val gy = max(2.0f, VallartaGeography.terrainHeight(px, pz))
            addMexicanPlazaKiosk(mbFor(px, pz), px, gy, pz, seed = pIdx)

            // Stepped walkable octagonal platform (3 smooth 0.32m steps) + 8 iron columns + overhead canopy!
            addSolidCollider(px, pz, 9.1f, 9.1f, gy - 0.5f, gy + 0.42f, rotDeg = 0f, climbable = false)
            addSolidCollider(px, pz, 9.1f, 9.1f, gy - 0.5f, gy + 0.42f, rotDeg = 45f, climbable = false)
            addSolidCollider(px, pz, 8.4f, 8.4f, gy - 0.5f, gy + 0.70f, rotDeg = 0f, climbable = false)
            addSolidCollider(px, pz, 8.4f, 8.4f, gy - 0.5f, gy + 0.70f, rotDeg = 45f, climbable = false)
            addSolidCollider(px, pz, 7.8f, 7.8f, gy - 0.5f, gy + 0.96f, rotDeg = 0f, climbable = true)
            addSolidCollider(px, pz, 7.8f, 7.8f, gy - 0.5f, gy + 0.96f, rotDeg = 45f, climbable = true)
            for (c in 0 until 8) {
                val a = (2.0 * PI * c / 8.0).toFloat()
                addSolidCollider(px + cos(a) * 6.8f, pz + sin(a) * 6.8f, 0.28f, 0.28f, gy + 0.92f, gy + 5.2f, climbable = false)
            }
            addSolidCollider(px, pz, 7.2f, 7.2f, gy + 5.05f, gy + 8.6f, rotDeg = 0f, climbable = true)
            // Colliders for the 4 framing plaza palms & 4 bougainvillea planters
            for (k in 0 until 4) {
                val a = (k * PI * 0.5 + PI * 0.25).toFloat()
                registerPalmCollider(px + cos(a) * 12.5f, gy, pz + sin(a) * 12.5f, 1.05f)
                registerPlanterCollider(px + cos(a) * 9.8f, gy, pz + sin(a) * 9.8f)
            }
        }

        // ==================== 4. LUSH INLAND & SIERRA MADRE TROPICAL VEGETATION (0.25 km .. 5.0 km INLAND!) ====================
        // Dense Parota, Ceiba, Mango, golden Primavera trees & tropical jungle shrubs across inland Vallarta & foothills!
        var iz = VallartaGeography.latToWorldZ(20.6760)
        val izEnd = VallartaGeography.latToWorldZ(20.5260)
        var row = 0
        while (iz < izEnd) {
            val coastX = VallartaGeography.coastX(iz)
            // Band 1: Urban parks, boulevard medians & residential tree canopy (160m .. 1,400m inland)
            var dInland = 165f
            var col = 0
            while (dInland <= 4920f) {
                val hash = ((row * 73856093) xor (col * 19349663)) and 0x7FFFFFFF
                val jx = ((hash % 47) - 23).toFloat()
                val jz = (((hash / 47) % 47) - 23).toFloat()
                val tx = coastX + dInland + jx
                val tz = iz + jz

                if (!VallartaGeography.isInsideMarinaBasin(tx, tz) &&
                    !VallartaGeography.isInsideRioCualeChannel(tx, tz) &&
                    !isNearMajorRoad(tx, tz, margin = 4f)
                ) {
                    val sd = VallartaGeography.signedCoastDist(tx, tz)
                    if (sd in 120f..4960f) {
                        val gy = VallartaGeography.terrainHeight(tx, tz)
                        val clearOfBuildings = buildingColliderGrid?.collidesHorizontally(tx, tz, gy + 0.2f, gy + 3.0f, 2.2f) != true
                        val isHillOrJungle = gy > 8.5f || sd > 1650f || tz > VallartaGeography.latToWorldZ(20.5920)
                        // Denser lush forest on hills, south shore cliffs & >1.5 km inland; pleasant park trees in urban grid!
                        val shouldSpawn = clearOfBuildings && (if (isHillOrJungle) (hash % 10 < 7) else (hash % 10 < 4))
                        if (shouldSpawn) {
                            val mb = mbFor(tx, tz)
                            val scale = (if (isHillOrJungle) 1.10f else 0.85f) + (hash % 7) * 0.14f
                            if (sd < 900f && hash % 4 == 0) {
                                addFastPalm(mb, tx, gy, tz, scale = scale * 0.90f, seed = hash)
                                registerPalmCollider(tx, gy, tz, scale * 0.90f)
                            } else {
                                addTropicalBroadleafTree(mb, tx, gy, tz, scale = scale, seed = hash)
                                registerBroadleafCollider(tx, gy, tz, scale)
                                // Add companion tropical undergrowth bush in lush jungle & hillside zones!
                                val sx = tx + 4.2f; val sz = tz + 3.4f
                                if (isHillOrJungle && hash % 3 == 0 &&
                                    !isNearMajorRoad(sx, sz, margin = 2.5f) &&
                                    buildingColliderGrid?.collidesHorizontally(sx, sz, gy + 0.2f, gy + 2.0f, 1.5f) != true
                                ) {
                                    val shrubScale = 0.9f + (hash % 4) * 0.2f
                                    addTropicalShrub(mb, sx, gy - 0.1f, sz, scale = shrubScale, seed = hash)
                                    registerShrubCollider(sx, gy - 0.1f, sz, shrubScale)
                                }
                            }
                        }
                    }
                }

                // Step denser in hills / inland jungle, slightly wider across flat residential grids
                dInland += when {
                    dInland < 1200f -> 115f
                    dInland < 2600f -> 135f
                    else -> 110f
                }
                col++
            }
            iz += 92f
            row++
        }

        val chunks = ArrayList<OsmSectorChunk>(builders.size)
        for ((key, mb) in builders) {
            val meshData = mb.build()
            if (meshData.indices.isNotEmpty()) {
                chunks += OsmSectorChunk(
                    sectorKey = key,
                    meshData = meshData,
                    buildingCount = 0,
                    poolCount = 0,
                    namedPlaces = emptyList(),
                    colliders = collidersBySector[key].orEmpty()
                )
            }
        }
        chunks
    }

    /** Fast, graceful curved Puerto Vallarta coconut palm (low-poly batched for 1,500+ palms at 60fps!). */
    private fun addFastPalm(mb: MeshBuilder, x: Float, y: Float, z: Float, scale: Float, seed: Int) {
        val h = 7.6f * scale
        val leanAngle = (seed * 1.37f) % (2f * PI.toFloat())
        val lean = 0.85f * scale
        val topX = x + cos(leanAngle) * lean
        val topZ = z + sin(leanAngle) * lean
        val midX = x + cos(leanAngle) * (lean * 0.42f)
        val midZ = z + sin(leanAngle) * (lean * 0.42f)
        // Curved palm trunk in 2 tapered segments
        mb.addTaperedLimb(x, y - 0.3f, z, 0.24f * scale, 0.24f * scale, midX, y + h * 0.52f, midZ, 0.17f * scale, 0.17f * scale, 0.46f, 0.35f, 0.24f, MaterialId.MATTE, slices = 5, rings = 1)
        mb.addTaperedLimb(midX, y + h * 0.52f, midZ, 0.17f * scale, 0.17f * scale, topX, y + h, topZ, 0.12f * scale, 0.12f * scale, 0.50f, 0.39f, 0.27f, MaterialId.MATTE, slices = 5, rings = 1)
        // 6 arched tropical palm fronds radiating from crown
        val frondLen = 3.2f * scale
        for (f in 0 until 6) {
            val a = leanAngle + f * (PI.toFloat() / 3f)
            val fx = cos(a); val fz = sin(a)
            val px = -fz; val pz = fx
            val tipX = topX + fx * frondLen
            val tipZ = topZ + fz * frondLen
            val midFX = topX + fx * (frondLen * 0.52f)
            val midFZ = topZ + fz * (frondLen * 0.52f)
            val w = 0.44f * scale
            val g = 0.48f + (f % 2) * 0.08f
            val r = 0.14f + (f % 2) * 0.04f
            val vBase = mb.addVertex(topX, y + h, topZ, 0f, 1f, 0f, r, g, 0.12f, MaterialId.MATTE)
            val vL = mb.addVertex(midFX - px * w, y + h + 0.52f * scale, midFZ - pz * w, 0f, 0.9f, 0f, r + 0.04f, g + 0.08f, 0.14f, MaterialId.MATTE)
            val vR = mb.addVertex(midFX + px * w, y + h + 0.52f * scale, midFZ + pz * w, 0f, 0.9f, 0f, r + 0.04f, g + 0.08f, 0.14f, MaterialId.MATTE)
            val vTip = mb.addVertex(tipX, y + h - 0.68f * scale, tipZ, fx * 0.3f, 0.7f, fz * 0.3f, r + 0.08f, g + 0.12f, 0.16f, MaterialId.MATTE)
            mb.addTriangle(vBase, vL, vR)
            mb.addTriangle(vL, vTip, vR)
        }
    }

    /** Lush Sierra Madre / Vallarta Broadleaf Tropical Tree (Parota, Mango, Ceiba, or Golden Primavera!). */
    private fun addTropicalBroadleafTree(mb: MeshBuilder, x: Float, y: Float, z: Float, scale: Float, seed: Int) {
        val trunkH = 3.2f * scale
        val rCrown = 3.1f * scale
        mb.addCylinderY(x, y - 0.4f, y + trunkH, z, 0.34f * scale, 0.22f * scale, 0.36f, 0.25f, 0.16f, MaterialId.MATTE, slices = 5)

        // Authentic Puerto Vallarta tree foliage palette (emerald Parota, deep Ceiba, sunlit Mango, golden Primavera blossoms!)
        val (cr, cg, cb) = when (seed % 7) {
            0 -> Triple(0.14f, 0.48f, 0.16f) // Deep emerald Parota
            1 -> Triple(0.20f, 0.56f, 0.18f) // Vibrant tropical mango green
            2 -> Triple(0.11f, 0.38f, 0.14f) // Rich Sierra Madre jungle canopy
            3 -> Triple(0.28f, 0.58f, 0.20f) // Sunlit olive-lime foliage
            4 -> Triple(0.16f, 0.44f, 0.19f) // Coastal Ceiba emerald
            5 -> Triple(0.92f, 0.74f, 0.16f) // Iconic Golden-Yellow Vallarta Primavera tree in bloom!
            else -> Triple(0.86f, 0.22f, 0.42f) // Vibrant Bougainvillea / Flamboyán tree!
        }

        // Layered multi-cluster canopy so every tree has natural organic silhouette
        mb.addEllipsoid(x, y + trunkH + rCrown * 0.52f, z, rCrown, rCrown * 0.68f, rCrown, cr, cg, cb, MaterialId.MATTE, stacks = 4, slices = 7)
        mb.addEllipsoid(
            x + rCrown * 0.42f, y + trunkH + rCrown * 0.34f, z + rCrown * 0.25f,
            rCrown * 0.68f, rCrown * 0.52f, rCrown * 0.68f,
            (cr * 1.10f).coerceAtMost(1f), (cg * 1.08f).coerceAtMost(1f), (cb * 1.05f).coerceAtMost(1f),
            MaterialId.MATTE, stacks = 3, slices = 6
        )
    }

    private fun addTropicalShrub(mb: MeshBuilder, x: Float, y: Float, z: Float, scale: Float, seed: Int) {
        val g = 0.40f + (seed % 3) * 0.08f
        mb.addEllipsoid(x, y + 0.75f * scale, z, 1.45f * scale, 0.95f * scale, 1.45f * scale, 0.15f, g, 0.14f, MaterialId.MATTE, stacks = 3, slices = 6)
    }

    /** Beachfront colorful umbrella + pair of white resort lounge chairs (`camastros`). */
    private fun addBeachUmbrellaSet(mb: MeshBuilder, x: Float, y: Float, z: Float, seed: Int) {
        val (ur, ug, ub) = when (seed % 5) {
            0 -> Triple(0.06f, 0.78f, 0.86f) // Banderas turquoise
            1 -> Triple(0.96f, 0.38f, 0.22f) // Sunset coral
            2 -> Triple(0.96f, 0.80f, 0.18f) // Marigold gold
            3 -> Triple(0.16f, 0.48f, 0.86f) // Pacific blue
            else -> Triple(0.96f, 0.95f, 0.93f) // Resort pearl white
        }
        mb.addCylinderY(x, y - 0.1f, y + 2.35f, z, 0.05f, 0.05f, 0.85f, 0.82f, 0.76f, MaterialId.CHROME, slices = 5)
        mb.addCylinderY(x, y + 2.05f, y + 2.55f, z, 1.65f, 0.08f, ur, ug, ub, MaterialId.MATTE, slices = 8)
        // Pair of white resort sun loungers flanking the umbrella
        mb.addBox(x - 0.85f, y + 0.22f, z + 0.75f, 1.75f, 0.24f, 0.68f, 0.96f, 0.95f, 0.94f, MaterialId.MATTE)
        mb.addBox(x - 0.85f, y + 0.22f, z - 0.75f, 1.75f, 0.24f, 0.68f, 0.96f, 0.95f, 0.94f, MaterialId.MATTE)
    }

    /** Mexican terracotta planter overflowing with lush green leaves & bright magenta bougainvillea blossoms! */
    private fun addBougainvilleaPlanter(mb: MeshBuilder, x: Float, y: Float, z: Float, seed: Int) {
        mb.addCylinderY(x, y, y + 0.78f, z, 0.56f, 0.72f, 0.76f, 0.36f, 0.22f, MaterialId.MATTE, slices = 7)
        mb.addEllipsoid(x, y + 1.15f, z, 0.85f, 0.58f, 0.85f, 0.16f, 0.52f, 0.18f, MaterialId.MATTE, stacks = 3, slices = 6)
        val br = if (seed % 2 == 0) 0.92f else 0.96f
        val bg = if (seed % 2 == 0) 0.14f else 0.34f
        val bb = if (seed % 2 == 0) 0.48f else 0.18f
        mb.addEllipsoid(x + 0.25f, y + 1.48f, z - 0.18f, 0.48f, 0.36f, 0.48f, br, bg, bb, MaterialId.MATTE, stacks = 3, slices = 6)
    }

    /** Colonial wrought-iron streetlamp + wooden park bench along Malecón & avenues. */
    private fun addColonialBenchAndLamp(mb: MeshBuilder, x: Float, y: Float, z: Float, facingWest: Boolean) {
        mb.addBox(x, y + 0.46f, z, 0.65f, 0.14f, 1.95f, 0.44f, 0.28f, 0.16f, MaterialId.MATTE)
        val backOff = if (facingWest) 0.28f else -0.28f
        mb.addBox(x + backOff, y + 0.82f, z, 0.12f, 0.52f, 1.95f, 0.44f, 0.28f, 0.16f, MaterialId.MATTE)
        mb.addCylinderY(x, y, y + 4.4f, z + 1.4f, 0.11f, 0.07f, 0.16f, 0.18f, 0.20f, MaterialId.CHROME, slices = 6)
        mb.addEllipsoid(x, y + 4.65f, z + 1.4f, 0.26f, 0.32f, 0.26f, 1.0f, 0.90f, 0.62f, MaterialId.EMISSIVE, stacks = 4, slices = 6)
    }

    /** Authentic Octagonal Mexican Plaza Kiosk (`Kiosco`) + Stepped Cantera Stone Pavilion + Palms (Plaza de Armas, Pitillal, Lázaro Cárdenas!). */
    private fun addMexicanPlazaKiosk(mb: MeshBuilder, x: Float, y: Float, z: Float, seed: Int) {
        // Stepped octagonal cantera stone plaza platform so players can walk smoothly up onto the gazebo floor
        mb.addCylinderY(x, y - 0.3f, y + 0.42f, z, 10.4f, 10.2f, 0.84f, 0.78f, 0.68f, MaterialId.MALECON_STONE, slices = 8)
        mb.addCylinderY(x, y + 0.40f, y + 0.70f, z, 9.6f, 9.4f, 0.86f, 0.80f, 0.70f, MaterialId.MALECON_STONE, slices = 8)
        mb.addCylinderY(x, y + 0.68f, y + 0.96f, z, 8.8f, 8.5f, 0.88f, 0.82f, 0.72f, MaterialId.MALECON_STONE, slices = 8)
        // 8 ornate colonial iron columns
        for (i in 0 until 8) {
            val a = (2.0 * PI * i / 8.0).toFloat()
            val cx = x + cos(a) * 6.8f
            val cz = z + sin(a) * 6.8f
            mb.addCylinderY(cx, y + 0.95f, y + 5.2f, cz, 0.22f, 0.18f, 0.18f, 0.26f, 0.24f, MaterialId.CHROME, slices = 6)
        }
        // Sculpted Mexican wrought-iron dome roof & finial
        mb.addCylinderY(x, y + 5.1f, y + 7.4f, z, 7.6f, 1.6f, 0.16f, 0.44f, 0.38f, MaterialId.MATTE, slices = 8)
        mb.addEllipsoid(x, y + 7.4f, z, 2.2f, 1.8f, 2.2f, 0.78f, 0.32f, 0.18f, MaterialId.MATTE, stacks = 4, slices = 8)
        mb.addCylinderY(x, y + 9.0f, y + 10.6f, z, 0.16f, 0.05f, 0.96f, 0.80f, 0.32f, MaterialId.CHROME, slices = 6)
        // Warm glowing interior kiosk lantern
        mb.addEllipsoid(x, y + 4.4f, z, 0.9f, 0.7f, 0.9f, 1.0f, 0.90f, 0.62f, MaterialId.EMISSIVE, stacks = 4, slices = 6)
        // 4 framing plaza palms & bougainvillea planters
        for (k in 0 until 4) {
            val a = (k * PI * 0.5 + PI * 0.25).toFloat()
            addFastPalm(mb, x + cos(a) * 12.5f, y, z + sin(a) * 12.5f, scale = 1.05f, seed = seed * 10 + k)
            addBougainvilleaPlanter(mb, x + cos(a) * 9.8f, y, z + sin(a) * 9.8f, seed = k)
        }
    }

    /** Iconic Vallarta Coastal Lifeguard Tower (`Torre Salvavidas`) on timber stilts overlooking Banderas Bay! */
    private fun addLifeguardTower(mb: MeshBuilder, x: Float, y: Float, z: Float) {
        // 4 timber support legs
        for (dx in floatArrayOf(-1.2f, 1.2f)) {
            for (dz in floatArrayOf(-1.2f, 1.2f)) {
                mb.addCylinderY(x + dx, y - 0.2f, y + 2.8f, z + dz, 0.12f, 0.10f, 0.54f, 0.38f, 0.24f, MaterialId.MATTE, slices = 5)
            }
        }
        // Wooden deck platform & railing
        mb.addBox(x, y + 2.85f, z, 3.1f, 0.18f, 3.1f, 0.62f, 0.44f, 0.28f, MaterialId.MATTE)
        // Bright white & rescue-orange lifeguard cabin
        mb.addBox(x, y + 3.85f, z, 2.3f, 1.85f, 2.3f, 0.96f, 0.96f, 0.95f, MaterialId.MATTE)
        mb.addBox(x, y + 3.30f, z, 2.38f, 0.46f, 2.38f, 0.96f, 0.36f, 0.12f, MaterialId.MATTE)
        // Sloped coastal palm/shingle shade roof
        mb.addCylinderY(x, y + 4.78f, y + 5.55f, z, 2.2f, 0.25f, 0.86f, 0.32f, 0.14f, MaterialId.MATTE, slices = 4)
    }

    /** Authentic Puerto Vallarta wooden fishing panga boat + pair of colorful tropical surfboards on the sand! */
    private fun addBeachPangaAndSurfboards(mb: MeshBuilder, x: Float, y: Float, z: Float, seed: Int) {
        val (br, bg, bb) = when (seed % 3) {
            0 -> Triple(0.10f, 0.56f, 0.84f)
            1 -> Triple(0.94f, 0.42f, 0.18f)
            else -> Triple(0.08f, 0.72f, 0.68f)
        }
        // Hull & gunwale
        mb.addBox(x, y + 0.36f, z, 4.4f, 0.62f, 1.65f, 0.95f, 0.95f, 0.94f, MaterialId.MATTE)
        mb.addBox(x, y + 0.68f, z, 4.55f, 0.16f, 1.72f, br, bg, bb, MaterialId.MATTE)
        mb.addBox(x + 2.4f, y + 0.45f, z, 0.95f, 0.55f, 0.90f, 0.95f, 0.95f, 0.94f, MaterialId.MATTE)
        // Pair of colorful upright surfboards planted in the golden sand beside the boat
        mb.addEllipsoid(x - 1.2f, y + 1.05f, z + 1.65f, 0.10f, 1.05f, 0.32f, 0.98f, 0.82f, 0.16f, MaterialId.GLOSSY_PAINT, stacks = 4, slices = 6)
        mb.addEllipsoid(x - 0.7f, y + 1.02f, z + 1.85f, 0.10f, 0.98f, 0.30f, 0.08f, 0.80f, 0.86f, MaterialId.GLOSSY_PAINT, stacks = 4, slices = 6)
    }

    /** Vibrant Puerto Vallarta Beachfront Coco-Frío / Ceviche Palapa Stand! */
    private fun addBeachCocoStand(mb: MeshBuilder, x: Float, y: Float, z: Float, seed: Int) {
        val (sr, sg, sb) = if (seed % 2 == 0) Triple(0.96f, 0.52f, 0.18f) else Triple(0.12f, 0.68f, 0.76f)
        // Painted counter base
        mb.addBox(x, y + 0.62f, z, 3.2f, 1.18f, 2.2f, sr, sg, sb, MaterialId.MATTE)
        mb.addBox(x, y + 1.24f, z, 3.45f, 0.12f, 2.45f, 0.96f, 0.92f, 0.84f, MaterialId.MATTE)
        // 4 bamboo corner posts
        for (dx in floatArrayOf(-1.4f, 1.4f)) {
            for (dz in floatArrayOf(-0.95f, 0.95f)) {
                mb.addCylinderY(x + dx, y + 1.2f, y + 2.75f, z + dz, 0.09f, 0.08f, 0.62f, 0.48f, 0.28f, MaterialId.MATTE, slices = 5)
            }
        }
        // Thatched palapa roof
        mb.addCylinderY(x, y + 2.65f, y + 3.85f, z, 2.65f, 0.18f, 0.78f, 0.62f, 0.34f, MaterialId.MATTE, slices = 8)
    }
}
