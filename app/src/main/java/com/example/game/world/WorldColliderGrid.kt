package com.example.game.world

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*

/**
 * Oriented 3D Bounding Box (OBB) for a solid building, resort wing, shop, house, or monument.
 * Supports:
 *  - Exact rotated horizontal collision (no walking/driving through walls or hotels!)
 *  - Exact rooftop support elevation (`topY`) so players can jump and climb ONTO roofs!
 *  - Climbable facade detection with outward wall normal and rooftop landing position!
 */
data class ColliderBox(
    val cx: Float,
    val cz: Float,
    val halfW: Float,
    val halfD: Float,
    val cosR: Float,
    val sinR: Float,
    val baseY: Float,
    val topY: Float,
    val climbable: Boolean = true
) {
    // Axis-aligned bounding radius in XZ for fast broadphase culling
    val boundX: Float = abs(halfW * cosR) + abs(halfD * sinR)
    val boundZ: Float = abs(halfW * sinR) + abs(halfD * cosR)

    fun toLocal(wx: Float, wz: Float): Pair<Float, Float> {
        val dx = wx - cx
        val dz = wz - cz
        // Inverse of R_y(rot): x' = dx*cosR - dz*sinR, z' = dx*sinR + dz*cosR
        val lx = dx * cosR - dz * sinR
        val lz = dx * sinR + dz * cosR
        return lx to lz
    }

    fun toWorldDir(lx: Float, lz: Float): Pair<Float, Float> {
        // Forward R_y(rot): wx = lx*cosR + lz*sinR, wz = -lx*sinR + lz*cosR
        return (lx * cosR + lz * sinR) to (-lx * sinR + lz * cosR)
    }

    fun containsCircleXZ(wx: Float, wz: Float, radius: Float): Boolean {
        if (abs(wx - cx) > boundX + radius || abs(wz - cz) > boundZ + radius) return false
        val (lx, lz) = toLocal(wx, wz)
        val clampedX = lx.coerceIn(-halfW, halfW)
        val clampedZ = lz.coerceIn(-halfD, halfD)
        val ex = lx - clampedX
        val ez = lz - clampedZ
        return (ex * ex + ez * ez) < (radius * radius)
    }

    companion object {
        fun fromCenterRot(
            cx: Float,
            cz: Float,
            halfW: Float,
            halfD: Float,
            rotDeg: Float,
            baseY: Float,
            topY: Float,
            climbable: Boolean = true
        ): ColliderBox {
            val rad = Math.toRadians(rotDeg.toDouble())
            return ColliderBox(
                cx = cx,
                cz = cz,
                halfW = halfW.coerceAtLeast(0.6f),
                halfD = halfD.coerceAtLeast(0.6f),
                cosR = cos(rad).toFloat(),
                sinR = sin(rad).toFloat(),
                baseY = baseY,
                topY = max(baseY + 0.8f, topY),
                climbable = climbable
            )
        }

        /**
         * Computes a tight Oriented Bounding Box (OBB) for an arbitrary real OSM building polygon
         * by aligning the box axis with the polygon's longest exterior wall edge!
         */
        fun fromPolygonRing(
            ring: List<Pair<Float, Float>>,
            baseY: Float,
            height: Float,
            climbable: Boolean = true
        ): ColliderBox? {
            if (ring.size < 3) return null
            var bestLen2 = 0f
            var edgeDx = 1f
            var edgeDz = 0f
            for (i in ring.indices) {
                val (x0, z0) = ring[i]
                val (x1, z1) = ring[(i + 1) % ring.size]
                val dx = x1 - x0
                val dz = z1 - z0
                val l2 = dx * dx + dz * dz
                if (l2 > bestLen2) {
                    bestLen2 = l2
                    edgeDx = dx
                    edgeDz = dz
                }
            }
            if (bestLen2 < 0.25f) return null
            val elen = sqrt(bestLen2)
            // Local +X axis along dominant edge (ux, uz): R_y(rot)*(1,0) = (cosR, -sinR) => cosR = ux, sinR = -uz
            val cosR = edgeDx / elen
            val sinR = -edgeDz / elen

            var minLx = Float.POSITIVE_INFINITY
            var maxLx = Float.NEGATIVE_INFINITY
            var minLz = Float.POSITIVE_INFINITY
            var maxLz = Float.NEGATIVE_INFINITY
            for ((wx, wz) in ring) {
                val lx = wx * cosR - wz * sinR
                val lz = wx * sinR + wz * cosR
                if (lx < minLx) minLx = lx
                if (lx > maxLx) maxLx = lx
                if (lz < minLz) minLz = lz
                if (lz > maxLz) maxLz = lz
            }
            val midLx = (minLx + maxLx) * 0.5f
            val midLz = (minLz + maxLz) * 0.5f
            val halfW = ((maxLx - minLx) * 0.5f * 0.94f).coerceAtLeast(1.0f)
            val halfD = ((maxLz - minLz) * 0.5f * 0.94f).coerceAtLeast(1.0f)
            // Back to world space: wx = midLx*cosR + midLz*sinR, wz = -midLx*sinR + midLz*cosR
            val cx = midLx * cosR + midLz * sinR
            val cz = -midLx * sinR + midLz * cosR
            return ColliderBox(
                cx = cx,
                cz = cz,
                halfW = halfW,
                halfD = halfD,
                cosR = cosR,
                sinR = sinR,
                baseY = baseY,
                topY = baseY + height.coerceAtLeast(2.2f),
                climbable = climbable
            )
        }
    }
}

data class ClimbTarget(
    val box: ColliderBox,
    val wallTopY: Float,
    val roofLandX: Float,
    val roofLandZ: Float,
    val normalX: Float,
    val normalZ: Float,
    val attachX: Float,
    val attachZ: Float
)

/**
 * High-speed O(1) 32m Spatial-Hash 3D Collision & Parkour/Climbing Grid.
 * Covers ALL static landmarks, ALL 19 multi-wing bespoke Puerto Vallarta resorts, and ALL ~7,000+
 * pre-baked & live-streamed real OpenStreetMap buildings, shops, houses & palapas!
 */
class WorldColliderGrid {

    private val cellSize = 32f
    private val cells = ConcurrentHashMap<Long, CopyOnWriteArrayList<ColliderBox>>(2048)

    private fun cellKey(gx: Int, gz: Int): Long {
        return (gx.toLong() shl 32) xor (gz.toLong() and 0xFFFFFFFFL)
    }

    fun addBox(box: ColliderBox) {
        val x0 = floor((box.cx - box.boundX) / cellSize).toInt()
        val x1 = floor((box.cx + box.boundX) / cellSize).toInt()
        val z0 = floor((box.cz - box.boundZ) / cellSize).toInt()
        val z1 = floor((box.cz + box.boundZ) / cellSize).toInt()
        for (gx in x0..x1) {
            for (gz in z0..z1) {
                cells.computeIfAbsent(cellKey(gx, gz)) { CopyOnWriteArrayList() }.add(box)
            }
        }
    }

    fun addAll(boxes: List<ColliderBox>) {
        for (b in boxes) addBox(b)
    }

    /**
     * Registers accurate rotated multi-wing solid colliders for our 19 bespoke resorts and
     * monuments so every tower, wing, and lobby wall is solid & climbable while open beachfront
     * pool courtyards remain walkable!
     */
    fun registerStaticBuilding(b: WorldBuilding) {
        when (b.type) {
            LandmarkType.BRIDGE_CUALE_PEDESTRIAN -> {
                // Walkable pedestrian bridge deck + side glass balustrades + cable mast
                addLocalSubBox(b, 0f, 0f, 5.4f, 34f, height = 0.0f, baseOffset = -0.8f, topOffset = 0.02f)
                addLocalSubBox(b, -5.2f, 0f, 0.35f, 34f, height = 1.25f, baseOffset = 0.0f, topOffset = 1.25f, climbable = false)
                addLocalSubBox(b, 5.2f, 0f, 0.35f, 34f, height = 1.25f, baseOffset = 0.0f, topOffset = 1.25f, climbable = false)
                addLocalSubBox(b, 6.8f, 0f, 1.1f, 1.1f, height = 19.5f, baseOffset = -3.5f, topOffset = 19.5f)
                return
            }
            LandmarkType.BRIDGE_CUALE_VEHICULAR -> {
                // Walkable bridge roadway + outer colonial stone parapets (outside 10.6m road)
                addLocalSubBox(b, 0f, 0f, 10.8f, 37f, height = 0.14f, baseOffset = -0.9f, topOffset = 0.14f)
                addLocalSubBox(b, -11.4f, 0f, 0.45f, 37f, height = 1.35f, baseOffset = 0.0f, topOffset = 1.35f, climbable = false)
                addLocalSubBox(b, 11.4f, 0f, 0.45f, 37f, height = 1.35f, baseOffset = 0.0f, topOffset = 1.35f, climbable = false)
                return
            }
            LandmarkType.MUELLE_LOS_MUERTOS -> {
                // Stepped beach approach ramp connecting Playa Los Muertos sand (x = +53m) onto pier deck (x = +36m)
                val rampX = floatArrayOf(51.8f, 49.0f, 46.2f, 43.4f, 40.6f, 37.8f)
                val rampTop = floatArrayOf(-0.08f, 0.08f, 0.24f, 0.38f, 0.52f, 0.66f)
                for (i in rampX.indices) {
                    addLocalSubBox(b, rampX[i], 0f, 2.0f, 3.8f, height = 1.8f, baseOffset = -1.2f, topOffset = rampTop[i], climbable = false)
                }
                // Main flat boardwalk deck extending over Banderas Bay
                addLocalSubBox(b, 10f, 0f, 26f, 3.8f, height = 2.9f, baseOffset = -2.2f, topOffset = 0.72f)
                // Thin side safety railings along the boardwalk
                addLocalSubBox(b, 10f, -3.65f, 26f, 0.22f, height = 1.0f, baseOffset = 0.68f, topOffset = 1.66f, climbable = false)
                addLocalSubBox(b, 10f, 3.65f, 26f, 0.22f, height = 1.0f, baseOffset = 0.68f, topOffset = 1.66f, climbable = false)
                // Circular ocean viewing plaza at sea end (0° and 45° boxes closely match r = 13.8m disc)
                addLocalSubBox(b, -28f, 0f, 11.8f, 11.8f, height = 3.1f, baseOffset = -2.2f, topOffset = 0.88f, rotOffsetDeg = 0f)
                addLocalSubBox(b, -28f, 0f, 11.8f, 11.8f, height = 3.1f, baseOffset = -2.2f, topOffset = 0.88f, rotOffsetDeg = 45f)
                // Central base of twisting sail spire sculpture
                addLocalSubBox(b, -28f, 0f, 2.2f, 2.2f, height = 18.5f, baseOffset = 0.85f, topOffset = 19.5f, climbable = true)
                return
            }
            LandmarkType.YACHT,
            LandmarkType.CRUISE_SHIP,
            LandmarkType.LOS_ARCOS_ROCKS -> return

            LandmarkType.EL_CABALLITO -> {
                addLocalSubBox(b, 0f, 0f, 1.6f, 1.6f, 4.5f)
                return
            }
            LandmarkType.AIRPLANE -> {
                addLocalSubBox(b, 0f, 0f, 4.2f, 16.0f, 6.2f)
                return
            }
            LandmarkType.PARROQUIA_CROWN -> {
                addLocalSubBox(b, 0f, 0f, 11f, 16f, 26f)
                return
            }
            LandmarkType.FARO_MARINA -> {
                addLocalSubBox(b, 0f, 0f, 4.5f, 4.5f, 36f)
                return
            }
            LandmarkType.LOS_ARCOS_MALECON -> {
                // Walkable stepped cantera stone amphitheater stage + 5 stone arch piers + upper lintel
                addLocalSubBox(b, 0f, 0f, 4.4f, 15.2f, height = 0.9f, baseOffset = -0.4f, topOffset = 0.42f, climbable = false)
                addLocalSubBox(b, 0f, 0f, 3.8f, 14.6f, height = 1.3f, baseOffset = -0.4f, topOffset = 0.82f, climbable = false)
                addLocalSubBox(b, 0f, 0f, 3.2f, 14.0f, height = 1.6f, baseOffset = -0.4f, topOffset = 1.20f)
                for (i in 0..4) {
                    val pz = -12f + i * 6f
                    addLocalSubBox(b, 0f, pz, 1.05f, 0.88f, height = 7.0f, baseOffset = 1.15f, topOffset = 8.15f)
                }
                addLocalSubBox(b, 0f, 0f, 1.15f, 13.6f, height = 1.0f, baseOffset = 11.2f, topOffset = 12.15f)
                return
            }
            LandmarkType.RESORT_VELAS -> {
                addLocalSubBox(b, 16f, -24f, 12f, 18f, 29f)
                addLocalSubBox(b, 16f, 24f, 12f, 18f, 29f)
                return
            }
            LandmarkType.RESORT_MARRIOTT -> {
                addLocalSubBox(b, 22f, 0f, 9.5f, 32f, 29f)
                addLocalSubBox(b, 0f, -33f, 24f, 7.8f, 27f)
                addLocalSubBox(b, 0f, 33f, 24f, 7.8f, 27f)
                return
            }
            LandmarkType.RESORT_WESTIN -> {
                addLocalSubBox(b, 16f, 0f, 12f, 37f, 32f)
                addLocalSubBox(b, -4f, -33f, 16f, 6.8f, 11.5f)
                addLocalSubBox(b, -4f, 33f, 16f, 6.8f, 11.5f)
                return
            }
            LandmarkType.RESORT_MELIA -> {
                addLocalSubBox(b, 18f, 0f, 10.5f, 34f, 27f)
                return
            }
            LandmarkType.RESORT_CROWN_PARADISE -> {
                addLocalSubBox(b, 14f, -24f, 11f, 17f, 27f)
                addLocalSubBox(b, 12f, 22f, 12f, 19f, 25f)
                return
            }
            LandmarkType.RESORT_KRYSTAL -> {
                addLocalSubBox(b, 24f, 0f, 10.5f, 26f, 31f)
                addLocalSubBox(b, 2f, -30f, 15f, 7.5f, 10.5f)
                addLocalSubBox(b, 2f, 30f, 15f, 7.5f, 10.5f)
                return
            }
            LandmarkType.RESORT_PENINSULA -> {
                for (tz in floatArrayOf(-34f, 0f, 34f)) {
                    addLocalSubBox(b, 16f, tz, 9f, 12.5f, 63f)
                }
                return
            }
            LandmarkType.RESORT_GRAND_VENETIAN -> {
                addLocalSubBox(b, 16f, -36f, 11f, 12.5f, 61f)
                addLocalSubBox(b, 22f, 0f, 13f, 13.5f, 71f)
                addLocalSubBox(b, 16f, 36f, 11f, 12.5f, 59f)
                return
            }
            LandmarkType.RESORT_FIESTA_AMERICANA -> {
                addLocalSubBox(b, 18f, 0f, 11.5f, 28f, 36f)
                addLocalSubBox(b, 6f, -26f, 12f, 7.8f, 26f)
                addLocalSubBox(b, 6f, 26f, 12f, 7.8f, 26f)
                return
            }
            LandmarkType.RESORT_ICON -> {
                for (tz in floatArrayOf(-30f, 0f, 30f)) {
                    addLocalSubBox(b, 14f, tz, 9f, 10.5f, 71f)
                }
                return
            }
            LandmarkType.RESORT_FLAMINGO -> {
                addLocalSubBox(b, 16f, 0f, 8.5f, 30f, 14f)
                addLocalSubBox(b, 2f, -24f, 18f, 6.8f, 12f)
                addLocalSubBox(b, 2f, 24f, 18f, 6.8f, 12f)
                return
            }
            LandmarkType.RESORT_SECRETS_DREAMS,
            LandmarkType.RESORT_SHERATON,
            LandmarkType.RESORT_BUENAVENTURA,
            LandmarkType.HOTEL_ROSITA,
            LandmarkType.RESORT_HYATT_ZIVA,
            LandmarkType.RESORT_GARZA_BLANCA,
            LandmarkType.RESORT_BARCELO,
            LandmarkType.RESORT_BEACH_OPEN -> {
                addLocalSubBox(b, 16f, 0f, 11f, 28f, b.height.coerceAtLeast(14f))
                addLocalSubBox(b, 2f, -26f, 13f, 7f, (b.height * 0.65f).coerceAtLeast(10f))
                addLocalSubBox(b, 2f, 26f, 13f, 7f, (b.height * 0.65f).coerceAtLeast(10f))
                return
            }
            else -> {
                addBox(
                    ColliderBox.fromCenterRot(
                        cx = b.x,
                        cz = b.z,
                        halfW = (b.width * 0.48f).coerceAtLeast(2.5f),
                        halfD = (b.depth * 0.48f).coerceAtLeast(2.5f),
                        rotDeg = b.rotationDeg,
                        baseY = b.y,
                        topY = b.y + b.height.coerceAtLeast(4f),
                        climbable = true
                    )
                )
            }
        }
    }

    private fun addLocalSubBox(
        b: WorldBuilding,
        localX: Float,
        localZ: Float,
        halfW: Float,
        halfD: Float,
        height: Float,
        baseOffset: Float = 0f,
        topOffset: Float = height,
        rotOffsetDeg: Float = 0f,
        climbable: Boolean = true
    ) {
        val parentRad = Math.toRadians(b.rotationDeg.toDouble())
        val pCosR = cos(parentRad).toFloat()
        val pSinR = sin(parentRad).toFloat()
        val wx = b.x + localX * pCosR + localZ * pSinR
        val wz = b.z - localX * pSinR + localZ * pCosR
        val boxRad = Math.toRadians((b.rotationDeg + rotOffsetDeg).toDouble())
        addBox(
            ColliderBox(
                cx = wx,
                cz = wz,
                halfW = halfW,
                halfD = halfD,
                cosR = cos(boxRad).toFloat(),
                sinR = sin(boxRad).toFloat(),
                baseY = b.y + baseOffset,
                topY = b.y + topOffset,
                climbable = climbable
            )
        )
    }

    /**
     * Checks whether a vertical cylinder at `(x, z)` spanning `[footY, headY]` with `radius`
     * horizontally hits ANY solid building, shop, house, or resort wing.
     * Note: If the player is standing ON TOP of a building (`footY >= box.topY - 0.45f`), it does NOT block!
     */
    fun collidesHorizontally(
        x: Float,
        z: Float,
        footY: Float,
        headY: Float,
        radius: Float
    ): Boolean {
        val gx0 = floor((x - radius - 2f) / cellSize).toInt()
        val gx1 = floor((x + radius + 2f) / cellSize).toInt()
        val gz0 = floor((z - radius - 2f) / cellSize).toInt()
        val gz1 = floor((z + radius + 2f) / cellSize).toInt()
        for (gx in gx0..gx1) {
            for (gz in gz0..gz1) {
                val list = cells[cellKey(gx, gz)] ?: continue
                for (i in 0 until list.size) {
                    val box = list[i]
                    if (footY >= box.topY - 0.45f || headY <= box.baseY + 0.15f) continue
                    if (box.containsCircleXZ(x, z, radius)) return true
                }
            }
        }
        return false
    }

    /**
     * Returns the highest walkable rooftop elevation (`topY`) directly beneath `(x, z)` that is
     * at or below `currentFootY + 0.58f`, or `Float.NEGATIVE_INFINITY` if none.
     */
    fun rooftopSupportY(x: Float, z: Float, currentFootY: Float, radius: Float = 0.40f): Float {
        var bestY = Float.NEGATIVE_INFINITY
        val gx0 = floor((x - radius) / cellSize).toInt()
        val gx1 = floor((x + radius) / cellSize).toInt()
        val gz0 = floor((z - radius) / cellSize).toInt()
        val gz1 = floor((z + radius) / cellSize).toInt()
        for (gx in gx0..gx1) {
            for (gz in gz0..gz1) {
                val list = cells[cellKey(gx, gz)] ?: continue
                for (i in 0 until list.size) {
                    val box = list[i]
                    if (box.topY > currentFootY + 0.58f) continue
                    if (box.topY > bestY && box.containsCircleXZ(x, z, radius)) {
                        bestY = box.topY
                    }
                }
            }
        }
        return bestY
    }

    /**
     * Probes slightly ahead of the player `(x, z)` along unit direction `(dirX, dirZ)` for a
     * climbable building wall or roof ledge! Returns wall attach position, outward normal, and
     * rooftop landing spot for seamless wall-climbing & ledge mantling!
     */
    fun findClimbableWallAhead(
        x: Float,
        z: Float,
        footY: Float,
        dirX: Float,
        dirZ: Float,
        reach: Float = 1.75f
    ): ClimbTarget? {
        val probeX = x + dirX * reach
        val probeZ = z + dirZ * reach
        val gx0 = floor((min(x, probeX) - 2f) / cellSize).toInt()
        val gx1 = floor((max(x, probeX) + 2f) / cellSize).toInt()
        val gz0 = floor((min(z, probeZ) - 2f) / cellSize).toInt()
        val gz1 = floor((max(z, probeZ) + 2f) / cellSize).toInt()

        var bestTarget: ClimbTarget? = null
        var bestDist = Float.POSITIVE_INFINITY

        for (gx in gx0..gx1) {
            for (gz in gz0..gz1) {
                val list = cells[cellKey(gx, gz)] ?: continue
                for (i in 0 until list.size) {
                    val box = list[i]
                    if (!box.climbable) continue
                    // Wall must rise above player's feet and extend down near player's reach
                    if (box.topY <= footY + 0.35f || box.baseY > footY + 1.8f) continue
                    if (abs(x - box.cx) > box.boundX + reach + 1.5f) continue
                    if (abs(z - box.cz) > box.boundZ + reach + 1.5f) continue

                    val (lx, lz) = box.toLocal(x, z)
                    val clampedX = lx.coerceIn(-box.halfW, box.halfW)
                    val clampedZ = lz.coerceIn(-box.halfD, box.halfD)
                    val dxL = lx - clampedX
                    val dzL = lz - clampedZ
                    val dist = hypot(dxL, dzL)
                    if (dist < 0.05f || dist > reach) continue

                    // Outward normal in local space pointing from wall toward player
                    val lnx = dxL / dist
                    val lnz = dzL / dist
                    val (wnx, wnz) = box.toWorldDir(lnx, lnz)
                    // Ensure player is generally facing the wall (dot < 0.15)
                    val facingDot = dirX * wnx + dirZ * wnz
                    if (facingDot > 0.25f) continue

                    if (dist < bestDist) {
                        bestDist = dist
                        // Closest point on wall in world space
                        val (wEdgeOffX, wEdgeOffZ) = box.toWorldDir(clampedX, clampedZ)
                        val edgeX = box.cx + wEdgeOffX
                        val edgeZ = box.cz + wEdgeOffZ
                        val attachX = edgeX + wnx * 0.52f
                        val attachZ = edgeZ + wnz * 0.52f
                        // Rooftop landing point 0.85m inward onto the roof from the ledge edge
                        val landX = edgeX - wnx * 0.85f
                        val landZ = edgeZ - wnz * 0.85f
                        bestTarget = ClimbTarget(
                            box = box,
                            wallTopY = box.topY,
                            roofLandX = landX,
                            roofLandZ = landZ,
                            normalX = wnx,
                            normalZ = wnz,
                            attachX = attachX,
                            attachZ = attachZ
                        )
                    }
                }
            }
        }
        return bestTarget
    }
}
