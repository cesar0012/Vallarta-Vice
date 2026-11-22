package com.example.game.engine

import com.example.game.world.RoadPolyline
import com.example.game.world.VallartaGeography
import kotlin.math.*

/**
 * Pure-3D Procedural PBR Terrain, Exact 158-Vertex Marina Vallarta Seawalls & Docks,
 * El Malecón Seawall, Río Cuale Bridges, and Bahía de Banderas + Marina + Río Cuale Ocean Builder.
 *
 * v6 Major Upgrade:
 *  - COMPLETELY REPLACES the flat blurry Esri/Google satellite image ground layer with a crisp,
 *    high-resolution, vertex-colored PBR 3D terrain mesh featuring:
 *      * Sunlit golden Puerto Vallarta beach sand & wet tidal surf zone
 *      * Lush emerald Marina Vallarta Golf Course & tropical park greens
 *      * Clean warm coastal stone/earth urban ground for all 3,867 real 3D streets & empedrados
 *      * Dramatic emerald Sierra Madre Occidental mountain foothills bounding the city at 5 km inland!
 *  - Sculpted 3D concrete & stone Marina Vallarta seawalls wrapping ALL 158 real OSM vertices of
 *    the harbor basin around Hotel Flamingo Vallarta, El Faro, and Terminal Marítima cruise docks!
 *  - Dense 84x96 Marina harbor water + 4 real Marina Golf lagoons + exact 53-vertex Río Cuale water!
 */
object CoastalTerrainBuilder {

    fun surfaceHeightAt(x: Float, z: Float): Float {
        return VallartaGeography.drivableHeightAt(x, z) + 0.10f
    }

    fun buildWorldGeometry(): Triple<MeshData, MeshData, MeshData> {
        val terrain = buildPure3DTerrainMesh()
        val (roadsAndSeawalls, ocean) = buildTerrainAndRoads()
        return Triple(terrain, roadsAndSeawalls, ocean)
    }

    fun buildTerrainAndRoads(): Pair<MeshData, MeshData> {
        val roads = MeshBuilder(68000)
        val ocean = MeshBuilder(96000)

        // 1. Only render El Malecón pedestrian stone promenade + PVR Airport runway here;
        // ALL 3,857 real OSM city avenues, streets, cobblestones & Marina roads render ONCE from `pv_roads.json` (ZERO overlapping roads!)
        for (road in VallartaGeography.roadNetwork) {
            if (road.isPedestrianMalecon || road.id == "pvr_runway") {
                buildRoadRibbon(roads, road)
                if (road.isPedestrianMalecon) {
                    buildMaleconSeawall(roads, road)
                }
            }
        }

        // 2. Elevated 3D bridge road decks spanning cleanly across Río Cuale (Morelos & Vallarta)
        buildRioCualeBridgeDecks(roads)

        // 3. Exact 158-vertex Marina Vallarta concrete/stone seawalls, cruise ship docks & yacht finger piers!
        buildMarinaSeawallsAndDocks(roads)

        // 4. Exact 1:1 Bahía de Banderas Gerstner Ocean Mesh strictly hugging real OSM coastline
        buildBanderasOceanMesh(ocean)

        // 5. Dense 158-vertex Marina Vallarta harbor water + 4 Marina Golf lagoons + 53-vertex Río Cuale water!
        buildMarinaAndRioCualeWater(ocean)

        return roads.build() to ocean.build()
    }

    /**
     * Builds the pure-3D PBR Puerto Vallarta ground terrain mesh (NO flat blurry satellite image!).
     * Dynamically adapts horizontal grid density: tight 5..12m resolution along the beach & hotel
     * strip so shorelines, Marina channels, and Río Cuale banks are razor-sharp, transitioning
     * cleanly across the 5 km inland plain and up into the Sierra Madre Occidental mountain backdrop.
     */
    private fun buildPure3DTerrainMesh(): MeshData {
        val mb = MeshBuilder(148000)

        val zStart = VallartaGeography.WORLD_Z_NORTH
        val zEnd = VallartaGeography.WORLD_Z_SOUTH
        val zStep = 24f
        val zRows = ((zEnd - zStart) / zStep).toInt() + 1

        // Non-uniform cross-shore offsets relative to exact coastX(z):
        // Ultra-dense around waterline (-30..+120m) & urban plain so roads never clip terrain triangles!
        val xOffsets = floatArrayOf(
            -90f, -35f, -14f, -4f, 3f, 10f, 18f, 28f, 42f, 65f, 95f,
            135f, 185f, 245f, 320f, 410f, 520f, 660f, 820f, 1000f, 1220f, 1480f,
            1780f, 2120f, 2500f, 2900f, 3350f, 3800f, 4250f, 4650f, 5000f, 5400f, 5900f, 6400f
        )
        val cols = xOffsets.size

        // Marina Vallarta Golf Course bounds (emerald fairways behind Velas/Marriott/Flamingo!)
        val golfZMin = VallartaGeography.latToWorldZ(20.6738)
        val golfZMax = VallartaGeography.latToWorldZ(20.6595)
        val golfXMin = VallartaGeography.lonToWorldX(-105.2640)
        val golfXMax = VallartaGeography.lonToWorldX(-105.2465)

        val ys = FloatArray(zRows * cols)
        val xs = FloatArray(zRows * cols)
        val zs = FloatArray(zRows * cols)
        val sds = FloatArray(zRows * cols)

        for (r in 0 until zRows) {
            val z = zStart + r * zStep
            val cx = VallartaGeography.coastX(z)
            for (c in 0 until cols) {
                val x = cx + xOffsets[c]
                val idx = r * cols + c
                xs[idx] = x
                zs[idx] = z
                ys[idx] = VallartaGeography.terrainHeight(x, z)
                sds[idx] = VallartaGeography.signedCoastDist(x, z)
            }
        }

        for (r in 0 until zRows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val x = xs[idx]
                val y = ys[idx]
                val z = zs[idx]
                val sd = sds[idx]

                // Smooth central-difference terrain normal
                val cL = max(0, c - 1); val cR = min(cols - 1, c + 1)
                val rU = max(0, r - 1); val rD = min(zRows - 1, r + 1)
                val dX = (xs[r * cols + cR] - xs[r * cols + cL]).coerceAtLeast(1f)
                val dZ = (zs[rD * cols + c] - zs[rU * cols + c]).coerceAtLeast(1f)
                val hL = ys[r * cols + cL]; val hR = ys[r * cols + cR]
                val hU = ys[rU * cols + c]; val hD = ys[rD * cols + c]
                val nx = -(hR - hL) / dX
                val nz = -(hD - hU) / dZ

                // Natural Puerto Vallarta PBR biome color
                val inMarinaGolf = x in golfXMin..golfXMax && z in golfZMin..golfZMax &&
                    sd > 95f && !VallartaGeography.isInsideMarinaBasin(x, z)
                val noise = 0.5f + 0.5f * sin(x * 0.011f + cos(z * 0.009f))

                val cr: Float; val cg: Float; val cb: Float
                when {
                    y < -0.35f -> {
                        // Submerged seabed / Marina channel floor
                        cr = 0.42f; cg = 0.46f; cb = 0.38f
                    }
                    sd < 7f -> {
                        // Wet golden tidal surf sand
                        cr = 0.72f; cg = 0.62f; cb = 0.45f
                    }
                    sd < 34f -> {
                        // Sunlit golden Banderas Bay beach sand!
                        cr = 0.89f; cg = 0.80f; cb = 0.61f
                    }
                    inMarinaGolf -> {
                        // Manicured emerald Marina Vallarta Golf Club fairways!
                        cr = 0.24f + noise * 0.08f
                        cg = 0.54f + noise * 0.12f
                        cb = 0.20f + noise * 0.05f
                    }
                    y > 8f || sd > 1800f -> {
                        // Lush emerald Sierra Madre Occidental tropical jungle & inland vegetation belt!
                        val lushFactor = max(
                            VallartaGeography.smoothstep(8f, 36f, y),
                            VallartaGeography.smoothstep(1600f, 3600f, sd)
                        )
                        cr = 0.24f + (1f - lushFactor) * 0.22f + noise * 0.05f
                        cg = 0.48f + (1f - lushFactor) * 0.10f + noise * 0.11f
                        cb = 0.18f + (1f - lushFactor) * 0.16f
                    }
                    else -> {
                        // Warm sunlit Puerto Vallarta urban earth / park grass / courtyard sandstone
                        val greenPatch = smoothstepPatch(x, z) * 0.45f
                        cr = 0.72f - greenPatch * 0.44f + noise * 0.04f
                        cg = 0.69f - greenPatch * 0.12f + noise * 0.04f
                        cb = 0.59f - greenPatch * 0.36f + noise * 0.03f
                    }
                }

                mb.addVertex(x, y, z, nx, 1f, nz, cr, cg, cb, MaterialId.TERRAIN)
            }
        }

        // Marina bounding box — seamlessly stitched with high-res ~5.5m Marina quay & peninsula patch!
        val mBounds = VallartaGeography.getMarinaBoundsXZ()
        val mPadX0 = mBounds[0] - 56f
        val mPadX1 = mBounds[1] + 56f
        val mPadZ0 = mBounds[2] - 56f
        val mPadZ1 = mBounds[3] + 56f

        for (r in 0 until zRows - 1) {
            for (c in 0 until cols - 1) {
                val a = r * cols + c
                val b = a + 1
                val d = a + cols
                val e = d + 1
                // Skip coarse quads only when all 4 corners lie inside the Marina high-res patch (zero seam!)
                val inPatch =
                    xs[a] in mPadX0..mPadX1 && zs[a] in mPadZ0..mPadZ1 &&
                        xs[b] in mPadX0..mPadX1 && zs[b] in mPadZ0..mPadZ1 &&
                        xs[d] in mPadX0..mPadX1 && zs[d] in mPadZ0..mPadZ1 &&
                        xs[e] in mPadX0..mPadX1 && zs[e] in mPadZ0..mPadZ1
                if (inPatch) continue
                mb.addQuad(a, d, e, b)
            }
        }

        // High-resolution ~5.5m x 5.5m Marina Vallarta & Terminal Marítima Terrain Patch:
        // Keeps EVERY narrow dry quay, Hotel Flamingo waterfront, El Faro boardwalk, Club Regina breakwater
        // & Vamar peninsula crisp, flat, sunlit and 100% walkable right up to the stone seawalls!
        if (mPadX1 > mPadX0 && mPadZ1 > mPadZ0) {
            val mCols = 236
            val mRows = 264
            val mStride = mCols + 1
            val baseV = mb.vertexCount
            for (r in 0..mRows) {
                val z = mPadZ0 + (mPadZ1 - mPadZ0) * (r.toFloat() / mRows)
                for (c in 0..mCols) {
                    val x = mPadX0 + (mPadX1 - mPadX0) * (c.toFloat() / mCols)
                    val inWater = VallartaGeography.isInsideMarinaBasin(x, z)
                    val sd = VallartaGeography.signedCoastDist(x, z)
                    val isEdge = (r == 0 || r == mRows || c == 0 || c == mCols)
                    val rawY = when {
                        inWater -> -2.25f
                        sd < 12f -> VallartaGeography.terrainHeight(x, z)
                        else -> max(2.04f, VallartaGeography.terrainHeight(x, z))
                    }
                    // Tiny +0.02f interior lift prevents z-fighting on overlapping border apron while border stays flush
                    val y = if (isEdge || inWater) rawY else rawY + 0.02f
                    val inGolf = x in golfXMin..golfXMax && z in golfZMin..golfZMax && sd > 95f && !inWater
                    val cr: Float; val cg: Float; val cb: Float
                    when {
                        inWater -> { cr = 0.36f; cg = 0.42f; cb = 0.36f }
                        sd < 7f -> { cr = 0.72f; cg = 0.62f; cb = 0.45f }
                        sd < 32f -> { cr = 0.89f; cg = 0.80f; cb = 0.61f }
                        inGolf -> { cr = 0.24f; cg = 0.56f; cb = 0.21f }
                        else -> { cr = 0.78f; cg = 0.74f; cb = 0.65f } // Warm sunlit Marina stone & plaza ground!
                    }
                    mb.addVertex(x, y, z, 0f, 1f, 0f, cr, cg, cb, MaterialId.TERRAIN)
                }
            }
            for (r in 0 until mRows) {
                for (c in 0 until mCols) {
                    val a = baseV + r * mStride + c
                    val b = a + 1
                    val d = a + mStride
                    val e = d + 1
                    mb.addQuad(a, d, e, b)
                }
            }
        }

        return mb.build()
    }

    private fun smoothstepPatch(x: Float, z: Float): Float {
        val v = sin(x * 0.0085f) * cos(z * 0.0085f)
        return VallartaGeography.smoothstep(0.25f, 0.85f, v)
    }

    /**
     * Builds exact 3D stone/concrete seawalls along ALL 158 vertices of the Marina Vallarta +
     * Terminal Marítima harbor basin (`way 25709720`) — visibly sculpting the exact waterway
     * that enters from Banderas Bay and wraps cleanly around Hotel Flamingo Vallarta & El Faro —
     * PLUS the 3 concrete Terminal Marítima cruise ship pier docks and wooden yacht finger piers!
     */
    private fun buildMarinaSeawallsAndDocks(mb: MeshBuilder) {
        val (mx, mz) = VallartaGeography.getMarinaPolygonXZ()
        val n = mx.size
        if (n >= 6) {
            val wallTopY = 2.08f
            val wallBotY = -1.60f
            val capW = 1.65f
            for (i in 0 until n) {
                val x0 = mx[i]; val z0 = mz[i]
                val x1 = mx[(i + 1) % n]; val z1 = mz[(i + 1) % n]
                val dx = x1 - x0; val dz = z1 - z0
                val segLen = hypot(dx, dz)
                if (segLen < 0.8f) continue
                // Skip open ocean harbor mouth at the southern sea entrance so water flows straight in!
                val midX = (x0 + x1) * 0.5f; val midZ = (z0 + z1) * 0.5f
                if (VallartaGeography.signedCoastDist(midX, midZ) < 10f) continue

                val nx = dz / segLen
                val nz = -dx / segLen
                // Determine outward direction (pointing onto dry land quay vs into water)
                val probeLand = !VallartaGeography.isInsideMarinaBasin(midX + nx * 3f, midZ + nz * 3f)
                val landNx = if (probeLand) nx else -nx
                val landNz = if (probeLand) nz else -nz
                val waterNx = -landNx
                val waterNz = -landNz

                // Vertical stone/concrete seawall facing the marina water
                val v0 = mb.addVertex(x0, wallBotY, z0, waterNx, 0f, waterNz, 0.66f, 0.63f, 0.58f, MaterialId.MATTE)
                val v1 = mb.addVertex(x1, wallBotY, z1, waterNx, 0f, waterNz, 0.66f, 0.63f, 0.58f, MaterialId.MATTE)
                val v2 = mb.addVertex(x1, wallTopY, z1, waterNx, 0f, waterNz, 0.78f, 0.74f, 0.68f, MaterialId.MATTE)
                val v3 = mb.addVertex(x0, wallTopY, z0, waterNx, 0f, waterNz, 0.78f, 0.74f, 0.68f, MaterialId.MATTE)
                mb.addQuad(v0, v1, v2, v3)

                // Sunlit Malecón de la Marina stone promenade coping rim along the top of the seawall!
                mb.addFlatQuad(
                    x0, wallTopY + 0.04f, z0,
                    x1, wallTopY + 0.04f, z1,
                    x1 + landNx * capW, wallTopY + 0.04f, z1 + landNz * capW,
                    x0 + landNx * capW, wallTopY + 0.04f, z0 + landNz * capW,
                    0.88f, 0.82f, 0.73f, MaterialId.MALECON_STONE
                )

                // Mooring bollards & wooden yacht finger piers inside the inner Marina around Flamingo & El Faro!
                if (segLen > 12f && i % 3 == 0) {
                    mb.addCylinderY(midX + landNx * 0.45f, wallTopY, wallTopY + 0.55f, midZ + landNz * 0.45f, 0.16f, 0.14f, 0.24f, 0.28f, 0.34f, MaterialId.CHROME, slices = 6)
                }
                if (segLen > 18f && i % 5 == 0 && midZ < VallartaGeography.latToWorldZ(20.6592)) {
                    // Wooden yacht finger-pier extending 14m out into the Marina basin!
                    val px0 = midX; val pz0 = midZ
                    val px1 = midX + waterNx * 14f; val pz1 = midZ + waterNz * 14f
                    val tx = (dx / segLen) * 1.15f; val tz = (dz / segLen) * 1.15f
                    mb.addFlatQuad(
                        px0 - tx, 0.65f, pz0 - tz,
                        px0 + tx, 0.65f, pz0 + tz,
                        px1 + tx, 0.65f, pz1 + tz,
                        px1 - tx, 0.65f, pz1 - tz,
                        0.58f, 0.40f, 0.24f, MaterialId.MATTE
                    )
                }
            }
        }

        // Exact 3 real Terminal Marítima cruise ship concrete pier docks (`waterway=dock`)
        for (dock in VallartaGeography.getMarinaDocksXZ()) {
            if (dock.size < 3) continue
            val cx = dock.map { it.first }.average().toFloat()
            val cz = dock.map { it.second }.average().toFloat()
            val deckY = 1.95f
            val cIdx = mb.addVertex(cx, deckY, cz, 0f, 1f, 0f, 0.78f, 0.77f, 0.74f, MaterialId.MATTE)
            var prev = mb.addVertex(dock[0].first, deckY, dock[0].second, 0f, 1f, 0f, 0.76f, 0.75f, 0.72f, MaterialId.MATTE)
            val first = prev
            for (i in 1 until dock.size) {
                val cur = mb.addVertex(dock[i].first, deckY, dock[i].second, 0f, 1f, 0f, 0.76f, 0.75f, 0.72f, MaterialId.MATTE)
                mb.addTriangle(cIdx, cur, prev)
                prev = cur
            }
            mb.addTriangle(cIdx, first, prev)
        }
    }

    private fun buildRoadRibbon(mb: MeshBuilder, road: RoadPolyline) {
        val rawPts = road.points
        if (rawPts.size < 2) return

        val pts = ArrayList<Pair<Float, Float>>(rawPts.size * 8)
        for (i in 0 until rawPts.size - 1) {
            val (x0, z0) = rawPts[i]
            val (x1, z1) = rawPts[i + 1]
            val dist = hypot(x1 - x0, z1 - z0)
            val steps = max(1, ceil(dist / 16f).toInt())
            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                pts.add((x0 + (x1 - x0) * t) to (z0 + (z1 - z0) * t))
            }
        }
        pts.add(rawPts.last())

        val hw = road.halfWidth
        val mat = if (road.isPedestrianMalecon) MaterialId.MALECON_STONE else MaterialId.ASPHALT
        val r = if (road.isPedestrianMalecon) 0.92f else 0.22f
        val g = if (road.isPedestrianMalecon) 0.86f else 0.23f
        val b = if (road.isPedestrianMalecon) 0.76f else 0.27f

        var prevLx = 0f; var prevLz = 0f; var prevLy = 0f
        var prevRx = 0f; var prevRz = 0f; var prevRy = 0f
        var prevPx = 0f; var prevPz = 0f
        var prevCx = 0f; var prevCz = 0f
        var hasPrev = false

        for (i in pts.indices) {
            val (cx, cz) = pts[i]
            val (tx, tz) = when (i) {
                0 -> (pts[1].first - cx) to (pts[1].second - cz)
                pts.lastIndex -> (cx - pts[i - 1].first) to (cz - pts[i - 1].second)
                else -> (pts[i + 1].first - pts[i - 1].first) to (pts[i + 1].second - pts[i - 1].second)
            }
            val len = hypot(tx, tz).coerceAtLeast(1e-4f)
            val px = tz / len
            val pz = -tx / len

            val lx = cx - px * hw
            val lz = cz - pz * hw
            val rx = cx + px * hw
            val rz = cz + pz * hw

            val cy = VallartaGeography.drivableHeightAt(cx, cz) + 0.36f

            if (hasPrev) {
                mb.addFlatQuad(prevLx, prevLy, prevLz, prevRx, prevRy, prevRz, rx, cy, rz, lx, cy, lz, r, g, b, mat)
                val curbW = if (road.isPedestrianMalecon) 1.15f else 1.55f
                val curbH = 0.14f
                // Left sidewalk / curb
                mb.addFlatQuad(
                    prevLx - prevPx * curbW, prevLy + curbH, prevLz - prevPz * curbW,
                    prevLx, prevLy + curbH, prevLz,
                    lx, cy + curbH, lz,
                    lx - px * curbW, cy + curbH, lz - pz * curbW,
                    0.82f, 0.78f, 0.72f, MaterialId.MATTE
                )
                // Right sidewalk / curb
                mb.addFlatQuad(
                    prevRx, prevRy + curbH, prevRz,
                    prevRx + prevPx * curbW, prevRy + curbH, prevRz + prevPz * curbW,
                    rx + px * curbW, cy + curbH, rz + pz * curbW,
                    rx, cy + curbH, rz,
                    0.82f, 0.78f, 0.72f, MaterialId.MATTE
                )
                // Crisp yellow double-median stripe on wide Puerto Vallarta avenues
                if (!road.isPedestrianMalecon && hw >= 6.5f) {
                    val sw = 0.24f
                    mb.addFlatQuad(
                        prevCx - prevPx * sw, prevLy + 0.03f, prevCz - prevPz * sw,
                        prevCx + prevPx * sw, prevLy + 0.03f, prevCz + prevPz * sw,
                        cx + px * sw, cy + 0.03f, cz + pz * sw,
                        cx - px * sw, cy + 0.03f, cz - pz * sw,
                        0.97f, 0.79f, 0.15f, MaterialId.MATTE
                    )
                }
            }

            prevLx = lx; prevLz = lz; prevLy = cy
            prevRx = rx; prevRz = rz; prevRy = cy
            prevPx = px; prevPz = pz
            prevCx = cx; prevCz = cz
            hasPrev = true
        }
    }

    private fun buildRioCualeBridgeDecks(mb: MeshBuilder) {
        val bridgeSpecs = arrayOf(
            VallartaGeography.lonToWorldX(-105.2364) to VallartaGeography.latToWorldZ(20.6057),
            VallartaGeography.lonToWorldX(-105.2352) to VallartaGeography.latToWorldZ(20.6057)
        )
        for ((bx, bz) in bridgeSpecs) {
            val y = 2.86f
            mb.addFlatQuad(
                bx - 9.5f, y, bz - 36f,
                bx + 9.5f, y, bz - 36f,
                bx + 9.5f, y, bz + 36f,
                bx - 9.5f, y, bz + 36f,
                0.26f, 0.27f, 0.30f, MaterialId.ASPHALT
            )
            mb.addFlatQuad(
                bx - 0.24f, y + 0.03f, bz - 35f,
                bx + 0.24f, y + 0.03f, bz - 35f,
                bx + 0.24f, y + 0.03f, bz + 35f,
                bx - 0.24f, y + 0.03f, bz + 35f,
                0.96f, 0.78f, 0.16f, MaterialId.MATTE
            )
        }
    }

    private fun buildMaleconSeawall(mb: MeshBuilder, malecon: RoadPolyline) {
        val raw = malecon.points
        for (i in 0 until raw.size - 1) {
            val (x0, z0) = raw[i]
            val (x1, z1) = raw[i + 1]
            val dist = hypot(x1 - x0, z1 - z0)
            val steps = max(1, ceil(dist / 16f).toInt())
            val tx = (x1 - x0) / dist.coerceAtLeast(1f)
            val tz = (z1 - z0) / dist.coerceAtLeast(1f)
            val wx = -tz
            val wz = tx
            val offset = malecon.halfWidth + 0.6f

            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                val bx = x0 + (x1 - x0) * t + wx * offset
                val bz = z0 + (z1 - z0) * t + wz * offset
                if (VallartaGeography.isInsideRioCualeChannel(bx, bz)) continue
                val by = VallartaGeography.drivableHeightAt(bx, bz) + 0.18f
                mb.addCylinderY(bx, by, by + 0.85f, bz, 0.20f, 0.16f, 0.78f, 0.73f, 0.66f, MaterialId.MATTE, slices = 8)
                mb.addEllipsoid(bx, by + 0.90f, bz, 0.19f, 0.14f, 0.19f, 0.86f, 0.80f, 0.72f, MaterialId.MATTE, stacks = 4, slices = 8)
            }
        }
    }

    private fun buildBanderasOceanMesh(ocean: MeshBuilder) {
        val zStart = VallartaGeography.WORLD_Z_NORTH
        val zEnd = VallartaGeography.WORLD_Z_SOUTH
        val zStep = 32f
        val zRows = ((zEnd - zStart) / zStep).toInt() + 1

        // Extend east of coastX(z) up to +3800m so the bay behind Boca de Tomates spit peninsula
        // and the Terminal Marítima mouth have full ocean surface while dry inland quads are skipped!
        val oceanOffsets = floatArrayOf(
            -3800f, -2100f, -1100f, -580f, -310f, -170f, -95f, -52f, -26f, -12f, -4f, 4f,
            16f, 42f, 88f, 160f, 280f, 460f, 720f, 1080f, 1550f, 2150f, 2900f, 3800f
        )
        val oCols = oceanOffsets.size
        val sds = FloatArray(zRows * oCols)
        val baseVert = ocean.vertexCount

        for (r in 0 until zRows) {
            val z = zStart + r * zStep
            val cx = VallartaGeography.coastX(z)
            for (c in 0 until oCols) {
                val x = cx + oceanOffsets[c]
                val sd = VallartaGeography.signedCoastDist(x, z)
                sds[r * oCols + c] = sd
                val shoreNorm = ((-sd) / 900f).coerceIn(0f, 1f)
                val oceanY = if (sd <= 2f) 0.0f else -2.6f * VallartaGeography.smoothstep(2f, 10f, sd)
                ocean.addVertex(x, oceanY, z, 0f, 1f, 0f, shoreNorm, 0f, 0f, MaterialId.MATTE)
            }
        }
        for (r in 0 until zRows - 1) {
            for (c in 0 until oCols - 1) {
                val a = r * oCols + c
                val b = a + 1
                val d = a + oCols
                val e = d + 1
                if (sds[a] > 24f && sds[b] > 24f && sds[d] > 24f && sds[e] > 24f) continue
                ocean.addQuad(baseVert + a, baseVert + d, baseVert + e, baseVert + b)
            }
        }
    }

    private fun buildMarinaAndRioCualeWater(ocean: MeshBuilder) {
        // 1. Exact 158-vertex Marina Vallarta & Terminal Marítima harbor water via 2D ear-clipping
        // so ZERO water triangles bleed onto Vamar peninsula, Club Regina breakwater, or Flamingo quays!
        val (mxRaw, mzRaw) = VallartaGeography.getMarinaPolygonXZ()
        val polyPts = ArrayList<Pair<Float, Float>>(mxRaw.size)
        for (i in mxRaw.indices) {
            val pt = mxRaw[i] to mzRaw[i]
            if (polyPts.isEmpty() || hypot(pt.first - polyPts.last().first, pt.second - polyPts.last().second) > 0.25f) {
                polyPts.add(pt)
            }
        }
        if (polyPts.size >= 3 && hypot(polyPts.first().first - polyPts.last().first, polyPts.first().second - polyPts.last().second) < 0.25f) {
            polyPts.removeAt(polyPts.lastIndex)
        }
        if (polyPts.size >= 3) {
            earClipWaterPolygon(ocean, polyPts, waterY = -0.02f, shoreNorm = 0.14f)
        }

        // 2. Real Marina Vallarta Golf Course lagoons (emerald-turquoise calm water!)
        for (lagoon in VallartaGeography.getMarinaLagoonsXZ()) {
            if (lagoon.size < 3) continue
            val cx = lagoon.map { it.first }.average().toFloat()
            val cz = lagoon.map { it.second }.average().toFloat()
            val ly = max(1.68f, VallartaGeography.terrainHeight(cx, cz) + 0.06f)
            earClipWaterPolygon(ocean, lagoon, waterY = ly, shoreNorm = 0.10f)
        }

        // 3. Exact Río Cuale Water Ribbon following real 53-point OSM river centerline
        val (rcx, rcz) = VallartaGeography.getCualePointsXZ()
        if (rcx.size >= 2) {
            val mouthX = rcx.first()
            for (i in 0 until rcx.size - 1) {
                val x0 = rcx[i]; val z0 = rcz[i]
                val x1 = rcx[i + 1]; val z1 = rcz[i + 1]
                val halfW = if (x0 < mouthX + 520f) 24f else 17f
                ocean.addFlatQuad(
                    x0, -0.05f, z0 - halfW,
                    x0, -0.05f, z0 + halfW,
                    x1, -0.05f, z1 + halfW,
                    x1, -0.05f, z1 - halfW,
                    0.08f, 0f, 0f, MaterialId.MATTE
                )
            }
        }
    }

    /**
     * Triangulates an arbitrary concave simple polygon in XZ using 2D ear-clipping.
     */
    private fun earClipWaterPolygon(
        mb: MeshBuilder,
        rawPts: List<Pair<Float, Float>>,
        waterY: Float,
        shoreNorm: Float
    ) {
        val pts = ArrayList<Pair<Float, Float>>(rawPts.size)
        for (p in rawPts) {
            if (pts.isEmpty() || hypot(p.first - pts.last().first, p.second - pts.last().second) > 0.20f) {
                pts.add(p)
            }
        }
        if (pts.size >= 3 && hypot(pts.first().first - pts.last().first, pts.first().second - pts.last().second) < 0.20f) {
            pts.removeAt(pts.lastIndex)
        }
        val n = pts.size
        if (n < 3) return

        // Determine signed area so we orient CCW in XZ
        var area = 0f
        for (i in 0 until n) {
            val (x0, z0) = pts[i]
            val (x1, z1) = pts[(i + 1) % n]
            area += (x0 * z1 - x1 * z0)
        }
        if (area < 0f) pts.reverse()

        val vertIndices = IntArray(n) { i ->
            mb.addVertex(pts[i].first, waterY, pts[i].second, 0f, 1f, 0f, shoreNorm, 0f, 0f, MaterialId.MATTE)
        }
        val rem = MutableList(n) { it }

        fun cross2D(ax: Float, az: Float, bx: Float, bz: Float, cx: Float, cz: Float): Float =
            (bx - ax) * (cz - az) - (bz - az) * (cx - ax)

        fun pointInTri(px: Float, pz: Float, ax: Float, az: Float, bx: Float, bz: Float, cx: Float, cz: Float): Boolean {
            val c1 = cross2D(ax, az, bx, bz, px, pz)
            val c2 = cross2D(bx, bz, cx, cz, px, pz)
            val c3 = cross2D(cx, cz, ax, az, px, pz)
            return c1 >= -1e-5f && c2 >= -1e-5f && c3 >= -1e-5f
        }

        var guard = rem.size * rem.size + 16
        var idx = 0
        while (rem.size > 3 && guard-- > 0) {
            val m = rem.size
            val iPrev = rem[(idx - 1 + m) % m]
            val iCur = rem[idx % m]
            val iNext = rem[(idx + 1) % m]
            val (ax, az) = pts[iPrev]
            val (bx, bz) = pts[iCur]
            val (cx, cz) = pts[iNext]

            if (cross2D(ax, az, bx, bz, cx, cz) > 1e-4f) {
                var containsOther = false
                for (k in 0 until m) {
                    val ik = rem[k]
                    if (ik == iPrev || ik == iCur || ik == iNext) continue
                    val (px, pz) = pts[ik]
                    if (pointInTri(px, pz, ax, az, bx, bz, cx, cz)) {
                        containsOther = true
                        break
                    }
                }
                if (!containsOther) {
                    mb.addTriangle(vertIndices[iPrev], vertIndices[iCur], vertIndices[iNext])
                    rem.removeAt(idx % m)
                    if (rem.isNotEmpty()) idx %= rem.size
                    continue
                }
            }
            idx = (idx + 1) % rem.size
        }
        if (rem.size == 3) {
            mb.addTriangle(vertIndices[rem[0]], vertIndices[rem[1]], vertIndices[rem[2]])
        }
    }
}
