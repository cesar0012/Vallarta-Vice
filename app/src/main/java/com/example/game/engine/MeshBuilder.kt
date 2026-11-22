package com.example.game.engine

import kotlin.math.*

/**
 * Material & Texture IDs interpreted by our PBR-inspired OpenGL ES 3.0 shaders:
 *   0.0 = Matte plaster / skin / cloth / foliage (uses subtle noise)
 *   1.0 = Glossy clearcoat car paint (high specular + Fresnel sky reflection)
 *   2.0 = Brushed chrome / metallic alloy / rims / bronze sculpture
 *   3.0 = Tinted reflective glass / car windshields / hotel glass towers
 *   4.0 = Emissive glow (headlights, taillights, neon sails, night beacons)
 *   5.0 = Textured Asphalt Road (uTexRoad)
 *   6.0 = Textured Beachfront Luxury Resort Facade (uTexResort)
 *   7.0 = Textured Malecón Pebble Mosaic & Cantera Stone (uTexMalecon)
 *   8.0 = Textured Tropical Sand & Sierra Madre Jungle Terrain (uTexTerrain)
 *   9.0 = Textured Modern Glass Condo Tower (uTexCondo)
 *  10.0 = Textured Colonial Vallarta Stucco & Terracotta (uTexColonial)
 *  11.0 = Textured Illuminated 3D Hotel / Landmark Signboard (uTexSigns)
 */
object MaterialId {
    const val MATTE = 0f
    const val GLOSSY_PAINT = 1f
    const val CHROME = 2f
    const val GLASS = 3f
    const val EMISSIVE = 4f
    const val ASPHALT = 5f
    const val RESORT_FACADE = 6f
    const val MALECON_STONE = 7f
    const val TERRAIN = 8f
    const val CONDO_FACADE = 9f
    const val COLONIAL_FACADE = 10f
    const val SIGNBOARD = 11f
    const val POOL_WATER = 11f
    const val EMPEDRADO = 12f
}

/**
 * Packed 12-float interleaved vertex buffer:
 *   [x, y, z, nx, ny, nz, u, v, r, g, b, matId]
 */
data class MeshData(
    val vertices: FloatArray,
    val indices: IntArray
) {
    val vertexCount: Int get() = vertices.size / 12
    val triangleCount: Int get() = indices.size / 3
}

/**
 * High-polygon procedural 3D mesh builder with real UV mapping, smooth-normal lofted automotive
 * bodywork, curved architectural surfaces, wheel tori, and 3D illuminated signboards.
 */
class MeshBuilder(initialVerts: Int = 1024) {
    private var verts = FloatArray(initialVerts * 12)
    private var vCount = 0
    private var inds = IntArray(initialVerts * 4)
    private var iCount = 0

    val vertexCount: Int get() = vCount

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        u: Float = 0f,
        v: Float = 0f
    ): Int {
        val needed = (vCount + 1) * 12
        if (needed > verts.size) {
            verts = verts.copyOf(max(verts.size * 2, needed))
        }
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(1e-5f)
        val o = vCount * 12
        verts[o] = x
        verts[o + 1] = y
        verts[o + 2] = z
        verts[o + 3] = nx / len
        verts[o + 4] = ny / len
        verts[o + 5] = nz / len
        verts[o + 6] = u
        verts[o + 7] = v
        verts[o + 8] = r
        verts[o + 9] = g
        verts[o + 10] = b
        verts[o + 11] = matId
        return vCount++
    }

    fun addTriangle(a: Int, b: Int, c: Int) {
        if (iCount + 3 > inds.size) {
            inds = inds.copyOf(inds.size * 2)
        }
        inds[iCount++] = a
        inds[iCount++] = b
        inds[iCount++] = c
    }

    fun addQuad(a: Int, b: Int, c: Int, d: Int) {
        addTriangle(a, b, c)
        addTriangle(a, c, d)
    }

    fun addFlatQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        u0: Float = 0f, v0: Float = 0f,
        u1: Float = 1f, v1: Float = 0f,
        u2: Float = 1f, v2: Float = 1f,
        u3: Float = 0f, v3: Float = 1f
    ) {
        val ux = x1 - x0; val uy = y1 - y0; val uz = z1 - z0
        val vx = x3 - x0; val vy = y3 - y0; val vz = z3 - z0
        val nx = uy * vz - uz * vy
        val ny = uz * vx - ux * vz
        val nz = ux * vy - uy * vx
        val i0 = addVertex(x0, y0, z0, nx, ny, nz, r, g, b, matId, u0, v0)
        val i1 = addVertex(x1, y1, z1, nx, ny, nz, r, g, b, matId, u1, v1)
        val i2 = addVertex(x2, y2, z2, nx, ny, nz, r, g, b, matId, u2, v2)
        val i3 = addVertex(x3, y3, z3, nx, ny, nz, r, g, b, matId, u3, v3)
        addQuad(i0, i1, i2, i3)
    }

    fun addEllipsoid(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        stacks: Int = 10,
        slices: Int = 14
    ) {
        val base = vCount
        for (i in 0..stacks) {
            val phi = PI * i / stacks
            val sinP = sin(phi).toFloat()
            val cosP = cos(phi).toFloat()
            val v = i.toFloat() / stacks
            for (j in 0..slices) {
                val theta = 2.0 * PI * j / slices
                val sinT = sin(theta).toFloat()
                val cosT = cos(theta).toFloat()
                val u = j.toFloat() / slices
                val nx = sinP * cosT
                val ny = cosP
                val nz = sinP * sinT
                addVertex(
                    cx + nx * rx, cy + ny * ry, cz + nz * rz,
                    nx / rx.coerceAtLeast(0.01f), ny / ry.coerceAtLeast(0.01f), nz / rz.coerceAtLeast(0.01f),
                    r, g, b, matId, u, v
                )
            }
        }
        val stride = slices + 1
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = base + i * stride + j
                val bIdx = a + stride
                val c = bIdx + 1
                val d = a + 1
                addQuad(a, d, c, bIdx)
            }
        }
    }

    fun addTaperedLimb(
        cx0: Float, y0: Float, cz0: Float, rx0: Float, rz0: Float,
        cx1: Float, y1: Float, cz1: Float, rx1: Float, rz1: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        slices: Int = 12,
        rings: Int = 4,
        capBottom: Boolean = true,
        capTop: Boolean = true
    ) {
        val base = vCount
        for (k in 0..rings) {
            val t = k.toFloat() / rings
            val bulge = 1f + 0.065f * sin(t * PI).toFloat()
            val cx = cx0 + (cx1 - cx0) * t
            val cy = y0 + (y1 - y0) * t
            val cz = cz0 + (cz1 - cz0) * t
            val rx = (rx0 + (rx1 - rx0) * t) * bulge
            val rz = (rz0 + (rz1 - rz0) * t) * bulge
            for (j in 0..slices) {
                val ang = (2.0 * PI * j / slices).toFloat()
                val cosA = cos(ang)
                val sinA = sin(ang)
                addVertex(
                    cx + cosA * rx, cy, cz + sinA * rz,
                    cosA / rx.coerceAtLeast(0.01f), 0f, sinA / rz.coerceAtLeast(0.01f),
                    r, g, b, matId, j.toFloat() / slices, t
                )
            }
        }
        val stride = slices + 1
        for (k in 0 until rings) {
            for (j in 0 until slices) {
                val a = base + k * stride + j
                val bIdx = a + stride
                val c = bIdx + 1
                val d = a + 1
                addQuad(a, d, c, bIdx)
            }
        }
        if (capBottom) {
            addEllipsoid(cx0, y0, cz0, rx0, min(rx0, rz0) * 0.85f, rz0, r, g, b, matId, stacks = 5, slices = slices)
        }
        if (capTop) {
            addEllipsoid(cx1, y1, cz1, rx1, min(rx1, rz1) * 0.85f, rz1, r, g, b, matId, stacks = 5, slices = slices)
        }
    }

    fun addWheelTorus(
        cx: Float, cy: Float, cz: Float,
        majorR: Float, minorR: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        majorSegs: Int = 24,
        minorSegs: Int = 12
    ) {
        val base = vCount
        for (i in 0..majorSegs) {
            val u = (2.0 * PI * i / majorSegs).toFloat()
            val cosU = cos(u)
            val sinU = sin(u)
            for (j in 0..minorSegs) {
                val v = (2.0 * PI * j / minorSegs).toFloat()
                val cosV = cos(v)
                val sinV = sin(v)
                val ring = majorR + minorR * cosV
                val x = cx + minorR * sinV
                val y = cy + ring * cosU
                val z = cz + ring * sinU
                val nx = sinV
                val ny = cosV * cosU
                val nz = cosV * sinU
                addVertex(x, y, z, nx, ny, nz, r, g, b, matId, i.toFloat() / majorSegs, j.toFloat() / minorSegs)
            }
        }
        val stride = minorSegs + 1
        for (i in 0 until majorSegs) {
            for (j in 0 until minorSegs) {
                val a = base + i * stride + j
                val bIdx = a + stride
                val c = bIdx + 1
                val d = a + 1
                addQuad(a, bIdx, c, d)
            }
        }
    }

    /**
     * High-detail symmetrical cross-section station for smooth-normal lofted automotive hulls,
     * yacht hulls, and aircraft fuselages!
     */
    data class LoftStation(
        val z: Float,
        val yBottom: Float,
        val yLower: Float,
        val yWaist: Float,
        val yTop: Float,
        val wBottom: Float,
        val wLower: Float,
        val wWaist: Float,
        val wTop: Float
    )

    /**
     * Smooth-shaded curved loft! Computes true averaged smooth vertex normals across adjacent
     * panels so cars, yachts, and fuselages catch glossy specular reflections like real curved metal!
     */
    fun addSmoothLoftedBody(
        stations: List<LoftStation>,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.GLOSSY_PAINT,
        capEnds: Boolean = true
    ) {
        if (stations.size < 2) return

        // 11-point smooth sculpted cross-section around each station
        fun ringPts(s: LoftStation): Array<FloatArray> {
            val crownY = s.yTop + (s.yTop - s.yWaist).coerceAtLeast(0.05f) * 0.14f
            return arrayOf(
                floatArrayOf(-s.wBottom, s.yBottom, s.z),
                floatArrayOf(0f, s.yBottom - 0.01f, s.z),
                floatArrayOf(s.wBottom, s.yBottom, s.z),
                floatArrayOf(s.wLower, s.yLower, s.z),
                floatArrayOf(s.wWaist, s.yWaist, s.z),
                floatArrayOf(s.wTop, s.yTop, s.z),
                floatArrayOf(0f, crownY, s.z),
                floatArrayOf(-s.wTop, s.yTop, s.z),
                floatArrayOf(-s.wWaist, s.yWaist, s.z),
                floatArrayOf(-s.wLower, s.yLower, s.z)
            )
        }

        val rings = stations.map { ringPts(it) }
        val numStations = rings.size
        val ptsPerRing = rings[0].size

        // Compute smooth vertex normals via central differences along ring (u) and station (v)
        val gridIndices = Array(numStations) { IntArray(ptsPerRing) }
        for (i in 0 until numStations) {
            val iPrev = (i - 1).coerceAtLeast(0)
            val iNext = (i + 1).coerceAtMost(numStations - 1)
            for (j in 0 until ptsPerRing) {
                val jPrev = (j - 1 + ptsPerRing) % ptsPerRing
                val jNext = (j + 1) % ptsPerRing
                val p = rings[i][j]
                // Tangent along ring
                val tx = rings[i][jNext][0] - rings[i][jPrev][0]
                val ty = rings[i][jNext][1] - rings[i][jPrev][1]
                val tz = rings[i][jNext][2] - rings[i][jPrev][2]
                // Tangent along Z length
                val bx = rings[iNext][j][0] - rings[iPrev][j][0]
                val by = rings[iNext][j][1] - rings[iPrev][j][1]
                val bz = rings[iNext][j][2] - rings[iPrev][j][2]
                // Outward normal = T x B
                var nx = ty * bz - tz * by
                var ny = tz * bx - tx * bz
                var nz = tx * by - ty * bx
                // Ensure outward orientation from station center
                val cy = (stations[i].yBottom + stations[i].yTop) * 0.5f
                if (nx * p[0] + ny * (p[1] - cy) < 0f) {
                    nx = -nx; ny = -ny; nz = -nz
                }
                gridIndices[i][j] = addVertex(
                    p[0], p[1], p[2],
                    nx, ny, nz,
                    r, g, b, matId,
                    u = j.toFloat() / ptsPerRing,
                    v = i.toFloat() / (numStations - 1)
                )
            }
        }

        for (i in 0 until numStations - 1) {
            for (j in 0 until ptsPerRing) {
                val jn = (j + 1) % ptsPerRing
                addQuad(gridIndices[i][j], gridIndices[i][jn], gridIndices[i + 1][jn], gridIndices[i + 1][j])
            }
        }

        if (capEnds) {
            fun capRing(ring: Array<FloatArray>, flip: Boolean) {
                var cx = 0f; var cy = 0f; var cz = 0f
                for (p in ring) { cx += p[0]; cy += p[1]; cz += p[2] }
                cx /= ring.size; cy /= ring.size; cz /= ring.size
                val nz = if (flip) 1f else -1f
                val center = addVertex(cx, cy, cz, 0f, 0f, nz, r, g, b, matId, 0.5f, 0.5f)
                val idxs = ring.map { addVertex(it[0], it[1], it[2], 0f, 0f, nz, r, g, b, matId, 0.5f, 0.5f) }
                for (j in ring.indices) {
                    val jn = (j + 1) % ring.size
                    if (flip) addTriangle(center, idxs[j], idxs[jn])
                    else addTriangle(center, idxs[jn], idxs[j])
                }
            }
            capRing(rings.first(), flip = false)
            capRing(rings.last(), flip = true)
        }
    }

    /**
     * UV-mapped oriented box (tiled cleanly in U/V for architectural facades & rooftops!).
     */
    fun addBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        uvScaleX: Float = 1f,
        uvScaleY: Float = 1f,
        uvScaleZ: Float = 1f
    ) {
        val hx = sx * 0.5f; val hy = sy * 0.5f; val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz
        // +Z front
        addFlatQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, matId, 0f, uvScaleY, uvScaleX, uvScaleY, uvScaleX, 0f, 0f, 0f)
        // -Z back
        addFlatQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, r, g, b, matId, 0f, uvScaleY, uvScaleX, uvScaleY, uvScaleX, 0f, 0f, 0f)
        // +X right
        addFlatQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, r, g, b, matId, 0f, uvScaleY, uvScaleZ, uvScaleY, uvScaleZ, 0f, 0f, 0f)
        // -X left
        addFlatQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, matId, 0f, uvScaleY, uvScaleZ, uvScaleY, uvScaleZ, 0f, 0f, 0f)
        // +Y top (rooftop uses matte/concrete color)
        val topMat = if (matId == MaterialId.RESORT_FACADE || matId == MaterialId.CONDO_FACADE || matId == MaterialId.COLONIAL_FACADE) MaterialId.MATTE else matId
        addFlatQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, r * 0.92f, g * 0.92f, b * 0.92f, topMat, 0f, 0f, uvScaleX, 0f, uvScaleX, uvScaleZ, 0f, uvScaleZ)
        // -Y bottom
        addFlatQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r * 0.8f, g * 0.8f, b * 0.8f, MaterialId.MATTE)
    }

    fun addCylinderY(
        cx: Float, y0: Float, y1: Float, cz: Float,
        rBottom: Float, rTop: Float,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        slices: Int = 16,
        capTop: Boolean = true,
        capBottom: Boolean = true
    ) {
        val base = vCount
        for (j in 0..slices) {
            val a = (2.0 * PI * j / slices).toFloat()
            val ca = cos(a); val sa = sin(a)
            val u = j.toFloat() / slices
            addVertex(cx + ca * rBottom, y0, cz + sa * rBottom, ca, 0f, sa, r, g, b, matId, u, 1f)
            addVertex(cx + ca * rTop, y1, cz + sa * rTop, ca, 0f, sa, r, g, b, matId, u, 0f)
        }
        for (j in 0 until slices) {
            val b0 = base + j * 2
            val t0 = b0 + 1
            val b1 = b0 + 2
            val t1 = b0 + 3
            addQuad(b0, b1, t1, t0)
        }
        if (capTop && rTop > 0.001f) {
            val cIdx = addVertex(cx, y1, cz, 0f, 1f, 0f, r, g, b, matId, 0.5f, 0.5f)
            val ringStart = vCount
            for (j in 0..slices) {
                val a = (2.0 * PI * j / slices).toFloat()
                addVertex(cx + cos(a) * rTop, y1, cz + sin(a) * rTop, 0f, 1f, 0f, r, g, b, matId, 0.5f + cos(a) * 0.5f, 0.5f + sin(a) * 0.5f)
            }
            for (j in 0 until slices) addTriangle(cIdx, ringStart + j + 1, ringStart + j)
        }
        if (capBottom && rBottom > 0.001f) {
            val cIdx = addVertex(cx, y0, cz, 0f, -1f, 0f, r, g, b, matId, 0.5f, 0.5f)
            val ringStart = vCount
            for (j in 0..slices) {
                val a = (2.0 * PI * j / slices).toFloat()
                addVertex(cx + cos(a) * rBottom, y0, cz + sin(a) * rBottom, 0f, -1f, 0f, r, g, b, matId, 0.5f, 0.5f)
            }
            for (j in 0 until slices) addTriangle(cIdx, ringStart + j, ringStart + j + 1)
        }
    }

    fun addArchRibbon(
        cx: Float, baseY: Float, cz: Float,
        spanHalf: Float, archHeight: Float,
        thicknessX: Float, thicknessY: Float,
        alongZ: Boolean,
        r: Float, g: Float, b: Float,
        matId: Float = MaterialId.MATTE,
        segments: Int = 14
    ) {
        var prevInner = FloatArray(3)
        var prevOuter = FloatArray(3)
        for (i in 0..segments) {
            val ang = PI * i / segments
            val s = cos(ang).toFloat() * spanHalf
            val h = sin(ang).toFloat() * archHeight
            val sOut = cos(ang).toFloat() * (spanHalf + thicknessY)
            val hOut = sin(ang).toFloat() * (archHeight + thicknessY)

            val inner = if (alongZ) floatArrayOf(cx, baseY + h, cz + s) else floatArrayOf(cx + s, baseY + h, cz)
            val outer = if (alongZ) floatArrayOf(cx, baseY + hOut, cz + sOut) else floatArrayOf(cx + sOut, baseY + hOut, cz)

            if (i > 0) {
                val dx = if (alongZ) thicknessX * 0.5f else 0f
                val dz = if (alongZ) 0f else thicknessX * 0.5f
                addFlatQuad(
                    prevInner[0] + dx, prevInner[1], prevInner[2] + dz,
                    inner[0] + dx, inner[1], inner[2] + dz,
                    outer[0] + dx, outer[1], outer[2] + dz,
                    prevOuter[0] + dx, prevOuter[1], prevOuter[2] + dz,
                    r, g, b, matId
                )
                addFlatQuad(
                    inner[0] - dx, inner[1], inner[2] - dz,
                    prevInner[0] - dx, prevInner[1], prevInner[2] - dz,
                    prevOuter[0] - dx, prevOuter[1], prevOuter[2] - dz,
                    outer[0] - dx, outer[1], outer[2] - dz,
                    r, g, b, matId
                )
                addFlatQuad(
                    prevOuter[0] - dx, prevOuter[1], prevOuter[2] - dz,
                    prevOuter[0] + dx, prevOuter[1], prevOuter[2] + dz,
                    outer[0] + dx, outer[1], outer[2] + dz,
                    outer[0] - dx, outer[1], outer[2] - dz,
                    r, g, b, matId
                )
                addFlatQuad(
                    prevInner[0] + dx, prevInner[1], prevInner[2] + dz,
                    prevInner[0] - dx, prevInner[1], prevInner[2] - dz,
                    inner[0] - dx, inner[1], inner[2] - dz,
                    inner[0] + dx, inner[1], inner[2] + dz,
                    r * 0.88f, g * 0.88f, b * 0.88f, matId
                )
            }
            prevInner = inner
            prevOuter = outer
        }
    }

    /**
     * Double-sided 3D illuminated signboard quad mapped to a specific slot in our Hotel Sign Atlas!
     */
    fun addSignboardQuad(
        width: Float, height: Float,
        u0: Float, v0: Float, u1: Float, v1: Float
    ) {
        val hw = width * 0.5f
        val hh = height * 0.5f
        // Front face (+X ocean/road facing or +Z)
        addFlatQuad(
            -hw, -hh, 0.14f,
            hw, -hh, 0.14f,
            hw, hh, 0.14f,
            -hw, hh, 0.14f,
            1f, 1f, 1f, MaterialId.SIGNBOARD,
            u0, v1, u1, v1, u1, v0, u0, v0
        )
        // Back face
        addFlatQuad(
            hw, -hh, -0.14f,
            -hw, -hh, -0.14f,
            -hw, hh, -0.14f,
            hw, hh, -0.14f,
            1f, 1f, 1f, MaterialId.SIGNBOARD,
            u0, v1, u1, v1, u1, v0, u0, v0
        )
        // Architectural dark metallic frame around the sign
        addBox(0f, 0f, 0f, width + 0.35f, height + 0.35f, 0.22f, 0.12f, 0.14f, 0.18f, MaterialId.CHROME)
    }

    fun build(): MeshData {
        return MeshData(
            vertices = verts.copyOf(vCount * 12),
            indices = inds.copyOf(iCount)
        )
    }
}
