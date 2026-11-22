package com.example.game.engine

import kotlin.math.*

/**
 * Authentic high-polygon 3D Puerto Vallarta Landmarks, Hotels, Bridge, Pier & Tropical Foliage
 * modeled after real Puerto Vallarta architecture (Google Maps 3D / coastal references).
 */
object HighPolyLandmarkMeshes {

    /**
     * AUTHENTIC PUENTE RÍO CUALE (between Centro Histórico & Zona Romántica):
     * Open-air colonial stone-and-iron bridge spanning above the sparkling Río Cuale river!
     * ZERO obstruction across the road — only side balustrades, colonial globe lamps,
     * and arched masonry piers descending down into the river below!
     */
    fun buildPuenteRioCuale(): MeshData {
        val m = MeshBuilder(950)
        val halfRoad = 10.6f // Safely outside the 9.8m half-roadway so vehicles drive cleanly over!
        val bridgeLen = 58f

        // Left & right colonial stone bridge parapets + wrought-iron balustrades (y = 0 is road level)
        for (sx in floatArrayOf(-halfRoad, halfRoad)) {
            // Stone curb base wall
            m.addBox(sx, 0.45f, 0f, 0.85f, 0.95f, bridgeLen, 0.90f, 0.84f, 0.74f, MaterialId.MALECON_STONE)
            // Upper stone cap rail
            m.addBox(sx, 1.38f, 0f, 0.95f, 0.20f, bridgeLen, 0.94f, 0.88f, 0.78f, MaterialId.MATTE)
            // Stone bridge piers & colonial wrought-iron rail segments + glowing bridge lanterns
            for (k in -3..3) {
                val pz = k * 9.0f
                m.addBox(sx, 0.85f, pz, 1.15f, 1.75f, 1.15f, 0.93f, 0.86f, 0.75f, MaterialId.MATTE)
                if (k % 2 == 0) {
                    // Ornate colonial bridge streetlamp post on top of parapet
                    m.addCylinderY(sx, 1.72f, 4.3f, pz, 0.10f, 0.07f, 0.14f, 0.15f, 0.18f, MaterialId.CHROME, slices = 8)
                    m.addEllipsoid(sx, 4.55f, pz, 0.30f, 0.34f, 0.30f, 1.0f, 0.90f, 0.62f, MaterialId.EMISSIVE, stacks = 6, slices = 10)
                }
            }
            // Underneath structural bridge arches plunging down into Río Cuale!
            for (az in floatArrayOf(-18f, 0f, 18f)) {
                m.addArchRibbon(sx * 0.95f, -3.4f, az, 8.4f, 2.8f, 1.4f, 0.75f, alongZ = true, r = 0.82f, g = 0.76f, b = 0.68f, matId = MaterialId.MATTE, segments = 10)
            }
        }

        // Solid concrete piers descending from deck underside into river water
        for (pz in floatArrayOf(-26f, -9f, 9f, 26f)) {
            m.addBox(0f, -2.0f, pz, halfRoad * 2.0f, 3.6f, 2.2f, 0.74f, 0.70f, 0.64f, MaterialId.MATTE)
        }
        return m.build()
    }

    /**
     * AUTHENTIC MUELLE DE PLAYA DE LOS MUERTOS (Zona Romántica):
     * Faithfully inspired by the real Puerto Vallarta Los Muertos Pier —
     * long sweeping boardwalk walkway extending over Banderas Bay into a wide circular viewing
     * deck crowned by the famous TWISTING WHITE SAIL SCULPTURE with luminous cyan/magenta lights!
     */
    fun buildMuelleLosMuertos(): MeshData {
        val m = MeshBuilder(1750)
        // Walkable boardwalk deck extending from beach ramp (+36m) out to the circular sea plaza (-16m)
        // Top of main walkway at local y = 0.72f (world y = 2.67m with yOverride = 1.95f)
        m.addBox(10f, 0.36f, 0f, 52f, 0.72f, 7.6f, 0.92f, 0.88f, 0.82f, MaterialId.MALECON_STONE)

        // Sloped/stepped beach approach ramp from local x = +36m (top 0.72f) down to x = +53m (top -0.10f -> buried seamlessly in Playa Los Muertos sand!)
        m.addFlatQuad(
            36f, 0.72f, -3.8f,
            53f, -0.10f, -3.8f,
            53f, -0.10f, 3.8f,
            36f, 0.72f, 3.8f,
            0.90f, 0.85f, 0.78f, MaterialId.MALECON_STONE
        )
        // Solid stone foundation under approach ramp so it never floats on sloped sand
        m.addBox(44.5f, -0.45f, 0f, 17f, 0.90f, 7.6f, 0.80f, 0.75f, 0.68f, MaterialId.MATTE)

        // Side glass & stainless-steel pier safety railings along the main boardwalk
        for (sz in floatArrayOf(-3.6f, 3.6f)) {
            m.addBox(10f, 1.15f, sz, 52f, 0.85f, 0.14f, 0.18f, 0.36f, 0.48f, MaterialId.GLASS)
            m.addBox(10f, 1.60f, sz, 52f, 0.10f, 0.22f, 0.90f, 0.92f, 0.96f, MaterialId.CHROME)
            for (px in -14..34 step 8) {
                m.addCylinderY(px.toFloat(), 0.70f, 2.65f, sz, 0.08f, 0.06f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, slices = 8)
                m.addEllipsoid(px.toFloat(), 2.80f, sz, 0.20f, 0.20f, 0.20f, 1.0f, 0.92f, 0.65f, MaterialId.EMISSIVE, stacks = 5, slices = 8)
            }
        }
        // Submerged concrete support pilings
        for (px in -28..34 step 8) {
            m.addCylinderY(px.toFloat(), -4.0f, 0.4f, -2.9f, 0.48f, 0.48f, 0.62f, 0.60f, 0.58f, MaterialId.MATTE, slices = 8)
            m.addCylinderY(px.toFloat(), -4.0f, 0.4f, 2.9f, 0.48f, 0.48f, 0.62f, 0.60f, 0.58f, MaterialId.MATTE, slices = 8)
        }

        // Wide circular ocean plaza at sea end (center at x = -28m)
        val cx = -28f
        m.addCylinderY(cx, -1.6f, 0.82f, 0f, 13.5f, 14.0f, 0.94f, 0.92f, 0.88f, MaterialId.MALECON_STONE, slices = 24)
        // Concentric glowing turquoise plaza floor ring
        m.addCylinderY(cx, 0.83f, 0.86f, 0f, 9.2f, 9.2f, 0.08f, 0.86f, 0.94f, MaterialId.EMISSIVE, slices = 24, capBottom = false)
        m.addCylinderY(cx, 0.87f, 0.90f, 0f, 8.4f, 8.4f, 0.92f, 0.90f, 0.86f, MaterialId.MALECON_STONE, slices = 24, capBottom = false)

        // WORLD-FAMOUS TWISTING WHITE SAIL SCULPTURE (sweeping spiraling ribs converging to a 20m spire!)
        val ribs = 10
        val steps = 10
        for (r in 0 until ribs) {
            val baseAng = (2.0 * PI * r / ribs).toFloat()
            val isAccent = r % 3 == 0
            var px = cx + cos(baseAng) * 10.6f
            var py = 0.88f
            var pz = sin(baseAng) * 10.6f
            for (s in 1..steps) {
                val t = s / steps.toFloat()
                // Graceful hyperbolic twist rising and spiraling into the iconic sail apex!
                val ang = baseAng + t * 1.45f
                val rad = 10.6f * (1f - t * 0.94f) + sin(t * PI).toFloat() * 2.1f
                val nx = cx + cos(ang) * rad
                val ny = 0.88f + t * 18.5f
                val nz = sin(ang) * rad
                m.addTaperedLimb(
                    px, py, pz, 0.34f * (1f - t * 0.45f), 0.34f * (1f - t * 0.45f),
                    nx, ny, nz, 0.30f * (1f - t * 0.45f), 0.30f * (1f - t * 0.45f),
                    if (isAccent) 0.10f else 0.97f,
                    if (isAccent) 0.90f else 0.98f,
                    if (isAccent) 0.98f else 0.99f,
                    if (isAccent) MaterialId.EMISSIVE else MaterialId.GLOSSY_PAINT,
                    slices = 6, rings = 1, capBottom = false, capTop = false
                )
                px = nx; py = ny; pz = nz
            }
        }
        // Glowing magenta-gold crown spire tip at the top of the sail
        m.addEllipsoid(cx, 19.8f, 0f, 1.0f, 1.4f, 1.0f, 1.0f, 0.28f, 0.66f, MaterialId.EMISSIVE, stacks = 6, slices = 10)
        return m.build()
    }

    /**
     * AUTHENTIC EL CABALLITO DEL MALECÓN (Iconic bronze Seahorse sculpture on Puerto Vallarta's boardwalk!)
     */
    fun buildElCaballito(): MeshData {
        val m = MeshBuilder(420)
        // Stepped coastal stone pedestal
        m.addBox(0f, 0.45f, 0f, 3.2f, 0.9f, 3.2f, 0.86f, 0.82f, 0.74f, MaterialId.MALECON_STONE)
        m.addCylinderY(0f, 0.9f, 2.2f, 0f, 1.25f, 1.05f, 0.88f, 0.84f, 0.76f, MaterialId.MATTE, slices = 12)
        // Sculpted curved bronze seahorse body & tail
        val br = 0.72f; val bg = 0.56f; val bb = 0.26f
        m.addTaperedLimb(0f, 2.2f, -0.35f, 0.34f, 0.34f, 0f, 3.6f, 0.25f, 0.48f, 0.56f, br, bg, bb, MaterialId.CHROME, slices = 10, rings = 3)
        m.addTaperedLimb(0f, 3.6f, 0.25f, 0.42f, 0.48f, 0f, 4.9f, 0.05f, 0.28f, 0.34f, br, bg, bb, MaterialId.CHROME, slices = 10, rings = 3)
        // Seahorse curved head & snout facing Banderas Bay (-X)
        m.addEllipsoid(-0.18f, 5.15f, 0.10f, 0.36f, 0.32f, 0.28f, br, bg, bb, MaterialId.CHROME, stacks = 7, slices = 10)
        m.addTaperedLimb(-0.35f, 5.05f, 0.10f, 0.14f, 0.14f, -0.82f, 4.90f, 0.10f, 0.08f, 0.08f, br, bg, bb, MaterialId.CHROME, slices = 8, rings = 2)
        // Sculpted rider silhouette holding hat aloft
        m.addTaperedLimb(0.15f, 4.15f, -0.10f, 0.20f, 0.20f, 0.15f, 5.25f, -0.10f, 0.22f, 0.20f, br, bg, bb, MaterialId.CHROME, slices = 8, rings = 2)
        m.addEllipsoid(0.15f, 5.55f, -0.10f, 0.18f, 0.20f, 0.18f, br, bg, bb, MaterialId.CHROME, stacks = 6, slices = 8)
        return m.build()
    }

    /**
     * AUTHENTIC PARROQUIA DE NUESTRA SEÑORA DE GUADALUPE (Centro Histórico)
     */
    fun buildParroquiaCrown(): MeshData {
        val m = MeshBuilder(1600)
        // Reddish-terracotta textured brick basilica nave
        m.addBox(0f, 8.8f, 0f, 20f, 17.6f, 28f, 0.88f, 0.46f, 0.32f, MaterialId.COLONIAL_FACADE)
        m.addBox(0f, 18.4f, 13.8f, 18f, 3.2f, 1.8f, 0.94f, 0.86f, 0.74f, MaterialId.MATTE)
        m.addArchRibbon(0f, 0f, 14.3f, 3.6f, 5.6f, 1.5f, 0.95f, alongZ = false, r = 0.94f, g = 0.88f, b = 0.78f)
        // Central square terracotta bell tower
        m.addBox(0f, 23.0f, 7.5f, 9.8f, 13.5f, 9.8f, 0.84f, 0.41f, 0.27f, MaterialId.MATTE)
        // Illuminated golden bell chamber arches
        m.addBox(0f, 25.0f, 7.5f, 6.4f, 5.4f, 6.4f, 1.0f, 0.84f, 0.48f, MaterialId.EMISSIVE)
        // Octagonal drum supporting the Crown of Guadalupe
        m.addCylinderY(0f, 29.6f, 34.2f, 7.5f, 4.5f, 4.1f, 0.93f, 0.85f, 0.72f, MaterialId.MATTE, slices = 8)
        // Iconic 8-ribbed baroque iron Crown of Guadalupe!
        for (i in 0 until 8) {
            val ang = (2.0 * PI * i / 8.0).toFloat()
            val ca = cos(ang); val sa = sin(ang)
            var px = ca * 3.9f; var py = 34.2f; var pz = 7.5f + sa * 3.9f
            for (step in 1..6) {
                val t = step / 6f
                val rad = 3.9f + sin(t * PI).toFloat() * 1.55f - t * 3.7f
                val ny = 34.2f + t * 8.2f
                val nx = ca * rad
                val nz = 7.5f + sa * rad
                m.addTaperedLimb(px, py, pz, 0.34f, 0.34f, nx, ny, nz, 0.28f, 0.28f, 0.94f, 0.84f, 0.64f, MaterialId.CHROME, slices = 6, rings = 1, capBottom = false, capTop = false)
                px = nx; py = ny; pz = nz
            }
        }
        m.addEllipsoid(0f, 43.0f, 7.5f, 1.1f, 1.1f, 1.1f, 0.96f, 0.84f, 0.36f, MaterialId.CHROME, stacks = 7, slices = 10)
        m.addBox(0f, 45.4f, 7.5f, 0.44f, 3.4f, 0.44f, 1.0f, 0.90f, 0.44f, MaterialId.EMISSIVE)
        m.addBox(0f, 45.8f, 7.5f, 2.2f, 0.44f, 0.44f, 1.0f, 0.90f, 0.44f, MaterialId.EMISSIVE)
        return m.build()
    }

    /**
     * AUTHENTIC LOS ARCOS DEL MALECÓN — 4 neoclassical cantera stone arches on the seafront plaza
     */
    fun buildLosArcosMalecon(): MeshData {
        val m = MeshBuilder(1020)
        // Stepped stone apron so players can walk smoothly up onto the amphitheater stage
        m.addBox(0f, 0.21f, 0f, 8.8f, 0.42f, 30.4f, 0.88f, 0.82f, 0.72f, MaterialId.MALECON_STONE)
        m.addBox(0f, 0.41f, 0f, 7.6f, 0.82f, 29.2f, 0.90f, 0.84f, 0.74f, MaterialId.MALECON_STONE)
        m.addBox(0f, 0.6f, 0f, 6.4f, 1.2f, 28f, 0.92f, 0.86f, 0.76f, MaterialId.MALECON_STONE)
        for (i in 0..4) {
            val pz = -12f + i * 6f
            m.addBox(0f, 4.4f, pz, 1.9f, 6.6f, 1.55f, 0.93f, 0.86f, 0.75f, MaterialId.MATTE)
            m.addBox(0f, 7.8f, pz, 2.3f, 0.65f, 2.0f, 0.96f, 0.90f, 0.80f, MaterialId.MATTE)
        }
        for (i in 0 until 4) {
            val cz = -9f + i * 6f
            m.addArchRibbon(0f, 7.4f, cz, 2.65f, 3.5f, 1.65f, 0.88f, alongZ = true, r = 0.94f, g = 0.87f, b = 0.76f, matId = MaterialId.MATTE, segments = 12)
        }
        m.addBox(0f, 11.7f, 0f, 2.2f, 0.85f, 27.2f, 0.96f, 0.90f, 0.80f, MaterialId.MATTE)
        return m.build()
    }

    fun buildFaroMarina(): MeshData {
        val m = MeshBuilder(800)
        m.addCylinderY(0f, 0f, 3.6f, 0f, 5.5f, 5.1f, 0.92f, 0.88f, 0.80f, MaterialId.MALECON_STONE, slices = 16)
        val bands = 6
        val hBand = 4.6f
        for (i in 0 until bands) {
            val y0 = 3.6f + i * hBand
            val y1 = y0 + hBand
            val r0 = 4.5f - i * 0.25f
            val r1 = 4.5f - (i + 1) * 0.25f
            val isRed = i % 2 == 1
            m.addCylinderY(0f, y0, y1, 0f, r0, r1, if (isRed) 0.86f else 0.97f, if (isRed) 0.19f else 0.96f, if (isRed) 0.17f else 0.94f, MaterialId.MATTE, slices = 16)
        }
        m.addCylinderY(0f, 31.2f, 32.8f, 0f, 4.8f, 5.1f, 0.96f, 0.95f, 0.92f, MaterialId.CHROME, slices = 16)
        m.addCylinderY(0f, 32.8f, 35.8f, 0f, 2.7f, 2.7f, 1.0f, 0.92f, 0.56f, MaterialId.EMISSIVE, slices = 16)
        m.addEllipsoid(0f, 35.8f, 0f, 3.0f, 2.4f, 3.0f, 0.84f, 0.19f, 0.17f, MaterialId.GLOSSY_PAINT, stacks = 8, slices = 14)
        return m.build()
    }

    fun buildCruiseShip(): MeshData {
        val m = MeshBuilder(1400)
        val hullStations = listOf(
            MeshBuilder.LoftStation(-52f, -1.5f, 2.0f, 5.2f, 8.6f, 8.5f, 10.6f, 11.8f, 12.2f),
            MeshBuilder.LoftStation(-28f, -1.5f, 2.0f, 5.5f, 9.0f, 11.5f, 12.4f, 12.6f, 12.8f),
            MeshBuilder.LoftStation(24f, -1.5f, 2.0f, 5.6f, 9.2f, 11.5f, 12.4f, 12.6f, 12.8f),
            MeshBuilder.LoftStation(44f, -1.0f, 2.5f, 6.2f, 10.2f, 5.5f, 6.8f, 7.8f, 8.6f),
            MeshBuilder.LoftStation(56f, 0.5f, 3.5f, 7.0f, 11.0f, 0.5f, 0.9f, 1.2f, 1.5f)
        )
        m.addSmoothLoftedBody(hullStations, 0.09f, 0.20f, 0.42f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 13.5f, -4f, 22f, 9f, 84f, 0.98f, 0.98f, 0.99f, MaterialId.RESORT_FACADE)
        m.addBox(0f, 20.5f, -6f, 19f, 5.5f, 70f, 0.98f, 0.98f, 0.99f, MaterialId.RESORT_FACADE)
        m.addBox(0f, 21.5f, 32f, 28f, 3.2f, 8f, 0.12f, 0.24f, 0.36f, MaterialId.GLASS)
        m.addCylinderY(0f, 23f, 31f, -16f, 3.6f, 2.8f, 0.12f, 0.44f, 0.78f, MaterialId.GLOSSY_PAINT, slices = 14)
        m.addCylinderY(0f, 30.5f, 32.5f, -16f, 2.9f, 2.6f, 0.96f, 0.36f, 0.16f, MaterialId.GLOSSY_PAINT, slices = 14)
        m.addBox(-11.2f, 10.2f, -2f, 0.5f, 1.0f, 78f, 1.0f, 0.92f, 0.68f, MaterialId.EMISSIVE)
        m.addBox(11.2f, 10.2f, -2f, 0.5f, 1.0f, 78f, 1.0f, 0.92f, 0.68f, MaterialId.EMISSIVE)
        return m.build()
    }

    fun buildLuxuryYacht(): MeshData {
        val m = MeshBuilder(900)
        val hullStations = listOf(
            MeshBuilder.LoftStation(-10.5f, -0.5f, 0.6f, 1.6f, 2.4f, 2.8f, 3.2f, 3.4f, 3.4f),
            MeshBuilder.LoftStation(1.5f, -0.5f, 0.7f, 1.7f, 2.6f, 3.0f, 3.4f, 3.6f, 3.6f),
            MeshBuilder.LoftStation(7.5f, -0.2f, 0.8f, 1.9f, 2.9f, 1.6f, 2.0f, 2.3f, 2.5f),
            MeshBuilder.LoftStation(11.2f, 0.4f, 1.2f, 2.2f, 3.2f, 0.2f, 0.3f, 0.4f, 0.5f)
        )
        m.addSmoothLoftedBody(hullStations, 0.98f, 0.98f, 0.99f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 0.7f, -0.5f, 6.8f, 0.55f, 16f, 0.08f, 0.26f, 0.56f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 3.6f, -1.5f, 4.8f, 1.9f, 9.5f, 0.08f, 0.16f, 0.24f, MaterialId.GLASS)
        m.addBox(0f, 4.7f, -2.0f, 4.6f, 0.45f, 8.2f, 0.97f, 0.98f, 0.99f, MaterialId.GLOSSY_PAINT)
        m.addArchRibbon(0f, 4.8f, -3.5f, 2.1f, 2.0f, 0.6f, 0.4f, alongZ = false, r = 0.96f, g = 0.97f, b = 0.99f, matId = MaterialId.CHROME, segments = 10)
        return m.build()
    }

    fun buildPassengerJet(): MeshData {
        val m = MeshBuilder(1000)
        val fuseStations = listOf(
            MeshBuilder.LoftStation(-16.5f, 2.4f, 2.9f, 3.6f, 4.2f, 0.3f, 0.45f, 0.5f, 0.4f),
            MeshBuilder.LoftStation(-10.5f, 1.6f, 2.4f, 3.8f, 5.0f, 1.2f, 1.8f, 1.9f, 1.3f),
            MeshBuilder.LoftStation(10.5f, 1.6f, 2.4f, 3.8f, 5.0f, 1.2f, 1.8f, 1.9f, 1.3f),
            MeshBuilder.LoftStation(14.5f, 1.9f, 2.5f, 3.5f, 4.4f, 0.8f, 1.2f, 1.3f, 0.8f),
            MeshBuilder.LoftStation(16.8f, 2.4f, 2.6f, 2.9f, 3.2f, 0.2f, 0.35f, 0.4f, 0.2f)
        )
        m.addSmoothLoftedBody(fuseStations, 0.97f, 0.97f, 0.99f, MaterialId.GLOSSY_PAINT)
        m.addFlatQuad(-1.8f, 2.5f, 2.8f, -15.5f, 3.2f, -4.8f, -15.5f, 3.2f, -7.2f, -1.8f, 2.5f, -3.2f, 0.90f, 0.92f, 0.95f, MaterialId.CHROME)
        m.addFlatQuad(1.8f, 2.5f, -3.2f, 15.5f, 3.2f, -7.2f, 15.5f, 3.2f, -4.8f, 1.8f, 2.5f, 2.8f, 0.90f, 0.92f, 0.95f, MaterialId.CHROME)
        for (ex in floatArrayOf(-5.6f, 5.6f)) {
            m.addEllipsoid(ex, 1.8f, 0.6f, 1.0f, 1.0f, 2.4f, 0.14f, 0.42f, 0.80f, MaterialId.GLOSSY_PAINT, stacks = 6, slices = 10)
        }
        m.addBox(0f, 7.2f, -13.6f, 0.42f, 6.2f, 4.4f, 0.12f, 0.44f, 0.82f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 3.6f, -14.4f, 10.2f, 0.35f, 2.4f, 0.92f, 0.93f, 0.96f, MaterialId.CHROME)
        m.addBox(0f, 3.9f, 14.0f, 1.8f, 0.65f, 1.4f, 0.08f, 0.14f, 0.22f, MaterialId.GLASS)
        return m.build()
    }

    fun buildLosArcosRocks(): MeshData {
        val m = MeshBuilder(950)
        m.addCylinderY(-12f, -3f, 24f, -10f, 11f, 6.5f, 0.50f, 0.46f, 0.41f, MaterialId.TERRAIN, slices = 10)
        m.addCylinderY(10f, -3f, 30f, 8f, 13f, 7.5f, 0.48f, 0.44f, 0.39f, MaterialId.TERRAIN, slices = 10)
        m.addArchRibbon(-1f, 2f, -1f, 12f, 14f, 8.5f, 5.2f, alongZ = false, r = 0.52f, g = 0.48f, b = 0.42f, matId = MaterialId.TERRAIN, segments = 10)
        m.addEllipsoid(-12f, 24.5f, -10f, 7.2f, 4.2f, 7.2f, 0.16f, 0.44f, 0.19f, MaterialId.TERRAIN, stacks = 6, slices = 10)
        m.addEllipsoid(10f, 30.5f, 8f, 8.4f, 4.8f, 8.4f, 0.14f, 0.42f, 0.17f, MaterialId.TERRAIN, stacks = 6, slices = 10)
        return m.build()
    }

    fun buildPalmTree(): MeshData {
        val m = MeshBuilder(1150)
        val segs = 8
        var px = 0f; var py = 0f; var pz = 0f
        for (i in 1..segs) {
            val t = i.toFloat() / segs
            val nx = sin(t * 1.15f) * 1.25f
            val ny = t * 10.6f
            val nz = (1f - cos(t * 0.9f)) * 0.55f
            val r0 = 0.36f * (1f - (i - 1) * 0.055f)
            val r1 = 0.36f * (1f - i * 0.055f)
            val shade = if (i % 2 == 0) 0.90f else 1.0f
            m.addTaperedLimb(px, py, pz, r0, r0, nx, ny, nz, r1, r1, 0.46f * shade, 0.34f * shade, 0.22f * shade, MaterialId.MATTE, slices = 8, rings = 1, capBottom = false, capTop = false)
            px = nx; py = ny; pz = nz
        }
        for (c in 0 until 4) {
            val ang = (2.0 * PI * c / 4.0).toFloat()
            m.addEllipsoid(px + cos(ang) * 0.32f, py - 0.25f, pz + sin(ang) * 0.32f, 0.22f, 0.26f, 0.22f, 0.36f, 0.24f, 0.12f, MaterialId.MATTE, stacks = 5, slices = 7)
        }
        val frondCount = 9
        for (f in 0 until frondCount) {
            val yaw = (2.0 * PI * f / frondCount).toFloat() + (f % 2) * 0.15f
            val dirX = cos(yaw); val dirZ = sin(yaw)
            val sideX = -dirZ; val sideZ = dirX
            val pitchBoost = if (f % 2 == 0) 1.35f else 0.72f
            val length = if (f % 2 == 0) 4.4f else 3.8f
            var prevX = px; var prevY = py; var prevZ = pz; var prevW = 0.14f
            for (s in 1..5) {
                val u = s / 5f
                val dist = u * length
                val archY = py + sin(u * PI * 0.78).toFloat() * (1.25f + pitchBoost * 0.65f) - u * u * 1.85f
                val cx = px + dirX * dist; val cz = pz + dirZ * dist
                val w = sin(u * PI).toFloat() * 0.72f + 0.08f
                val gCol = 0.44f + u * 0.16f
                val droop = w * 0.32f
                m.addFlatQuad(
                    prevX, prevY, prevZ,
                    cx, archY, cz,
                    cx + sideX * w, archY - droop, cz + sideZ * w,
                    prevX + sideX * prevW, prevY - prevW * 0.25f, prevZ + sideZ * prevW,
                    0.15f, gCol, 0.16f, MaterialId.MATTE
                )
                m.addFlatQuad(
                    cx, archY, cz,
                    prevX, prevY, prevZ,
                    prevX - sideX * prevW, prevY - prevW * 0.25f, prevZ - sideZ * prevW,
                    cx - sideX * w, archY - droop, cz - sideZ * w,
                    0.12f, gCol * 0.90f, 0.13f, MaterialId.MATTE
                )
                prevX = cx; prevY = archY; prevZ = cz; prevW = w
            }
        }
        return m.build()
    }

    /**
     * AUTHENTIC BEACHFRONT PUERTO VALLARTA RESORT (Marriott, Sheraton, Fiesta Americana, Barceló, Krystal):
     * Textured recessed balcony facade + stepped luxury wings + Mexican palapa/porte-cochère entrance + pool!
     */
    fun buildLuxuryHotelUnit(): MeshData {
        val m = MeshBuilder(820)
        // Main textured beachfront resort tower
        m.addBox(0f, 0.48f, 0f, 0.92f, 0.96f, 0.92f, 1f, 1f, 1f, MaterialId.RESORT_FACADE)
        // Recessed upper penthouse terrace setback
        m.addBox(0f, 0.98f, 0f, 0.70f, 0.12f, 0.72f, 0.98f, 0.95f, 0.90f, MaterialId.RESORT_FACADE)
        // Projecting architectural sun-shade eaves & balcony cornices
        for (i in 1..4) {
            val by = i * 0.22f
            m.addBox(0f, by, 0f, 1.03f, 0.022f, 1.03f, 0.96f, 0.93f, 0.88f, MaterialId.MATTE)
        }
        // Grand Puerto Vallarta resort lobby porte-cochère + terracotta accent roof trim
        m.addBox(0f, 0.09f, 0f, 1.14f, 0.032f, 1.14f, 0.86f, 0.46f, 0.30f, MaterialId.MATTE)
        // Rooftop pergola & infinity pool
        m.addBox(0f, 1.05f, 0f, 0.56f, 0.02f, 0.56f, 0.06f, 0.78f, 0.86f, MaterialId.GLASS)
        return m.build()
    }

    /**
     * AUTHENTIC MODERN GLASS CONDO SKYSCRAPER (Grand Venetian, Peninsula Towers, Icon Vallarta, Mousai):
     * Sleek cantilevered travertine balconies + reflective floor-to-ceiling ocean glass!
     */
    fun buildGlassCondoUnit(): MeshData {
        val m = MeshBuilder(760)
        m.addBox(0f, 0.50f, 0f, 0.90f, 1.0f, 0.90f, 1f, 1f, 1f, MaterialId.CONDO_FACADE)
        for (i in 1..5) {
            val by = i * 0.17f
            m.addBox(0f, by, 0f, 1.06f, 0.020f, 1.06f, 0.96f, 0.97f, 0.98f, MaterialId.MATTE)
        }
        // Glass rooftop sky-lounge crown
        m.addBox(0f, 1.04f, 0f, 0.72f, 0.08f, 0.72f, 0.14f, 0.36f, 0.54f, MaterialId.GLASS)
        m.addBox(0f, 1.09f, 0f, 0.78f, 0.02f, 0.78f, 0.96f, 0.97f, 0.99f, MaterialId.CHROME)
        return m.build()
    }

    /**
     * AUTHENTIC PUERTO VALLARTA COLONIAL VILLA (Centro Histórico, Hotel Rosita, Gringo Gulch, Olas Altas):
     * Textured arched-window Mexican stucco + wrought-iron balconies + pitched terracotta barrel-tile roof!
     */
    fun buildColonialVillaUnit(): MeshData {
        val m = MeshBuilder(480)
        m.addBox(0f, 0.44f, 0f, 0.94f, 0.88f, 0.94f, 1f, 1f, 1f, MaterialId.COLONIAL_FACADE)
        m.addBox(0f, 0.48f, 0f, 1.04f, 0.036f, 1.04f, 0.24f, 0.17f, 0.14f, MaterialId.MATTE)
        val yEave = 0.87f; val yPeak = 1.13f
        val hw = 0.57f; val hd = 0.57f; val ridge = 0.24f
        val tr = 0.76f; val tg = 0.29f; val tb = 0.17f
        m.addFlatQuad(-hw, yEave, hd, hw, yEave, hd, ridge, yPeak, 0f, -ridge, yPeak, 0f, tr, tg, tb, MaterialId.MATTE)
        m.addFlatQuad(hw, yEave, -hd, -hw, yEave, -hd, -ridge, yPeak, 0f, ridge, yPeak, 0f, tr * 0.90f, tg * 0.90f, tb * 0.90f, MaterialId.MATTE)
        m.addFlatQuad(hw, yEave, hd, hw, yEave, -hd, ridge, yPeak, 0f, ridge, yPeak, 0f, tr * 0.95f, tg * 0.95f, tb * 0.95f, MaterialId.MATTE)
        m.addFlatQuad(-hw, yEave, -hd, -hw, yEave, hd, -ridge, yPeak, 0f, -ridge, yPeak, 0f, tr * 0.95f, tg * 0.95f, tb * 0.95f, MaterialId.MATTE)
        return m.build()
    }

    fun buildBeachUmbrella(): MeshData {
        val m = MeshBuilder(260)
        m.addCylinderY(0f, 0f, 2.55f, 0f, 0.06f, 0.05f, 0.56f, 0.38f, 0.22f, MaterialId.MATTE, slices = 8)
        val slices = 10
        val apex = m.addVertex(0f, 2.75f, 0f, 0f, 1f, 0f, 1f, 1f, 1f, MaterialId.MATTE)
        val ringStart = m.addVertex(1.65f, 2.15f, 0f, 0.5f, 0.8f, 0f, 1f, 1f, 1f, MaterialId.MATTE)
        for (i in 1..slices) {
            val a = (2.0 * PI * i / slices).toFloat()
            val c = if (i % 2 == 0) 0.98f else 0.84f
            m.addVertex(cos(a) * 1.65f, 2.15f, sin(a) * 1.65f, cos(a) * 0.5f, 0.8f, sin(a) * 0.5f, c, c, c, MaterialId.MATTE)
        }
        for (i in 0 until slices) m.addTriangle(apex, ringStart + i + 1, ringStart + i)
        m.addBox(1.1f, 0.35f, 0.2f, 0.80f, 0.14f, 1.85f, 0.96f, 0.94f, 0.90f, MaterialId.MATTE)
        return m.build()
    }

    fun buildStreetlamp(): MeshData {
        val m = MeshBuilder(240)
        m.addCylinderY(0f, 0f, 0.7f, 0f, 0.26f, 0.18f, 0.16f, 0.18f, 0.22f, MaterialId.CHROME, slices = 8)
        m.addCylinderY(0f, 0.7f, 6.8f, 0f, 0.12f, 0.08f, 0.18f, 0.20f, 0.24f, MaterialId.CHROME, slices = 8)
        m.addBox(0.85f, 6.75f, 0f, 1.7f, 0.10f, 0.10f, 0.20f, 0.22f, 0.26f, MaterialId.CHROME)
        m.addEllipsoid(1.65f, 6.55f, 0f, 0.30f, 0.34f, 0.30f, 1.0f, 0.88f, 0.58f, MaterialId.EMISSIVE, stacks = 6, slices = 10)
        return m.build()
    }

    fun buildMissionBeacon(): MeshData {
        val m = MeshBuilder(240)
        m.addCylinderY(0f, 0f, 28f, 0f, 2.6f, 1.2f, 0.06f, 0.94f, 0.88f, MaterialId.EMISSIVE, slices = 16, capTop = false, capBottom = false)
        m.addCylinderY(0f, 0.2f, 1.0f, 0f, 4.4f, 4.4f, 1.0f, 0.76f, 0.18f, MaterialId.EMISSIVE, slices = 20, capTop = false, capBottom = false)
        return m.build()
    }
}
