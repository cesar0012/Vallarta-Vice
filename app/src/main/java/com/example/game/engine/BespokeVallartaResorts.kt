package com.example.game.engine

import kotlin.math.*

/**
 * Complete library of 18+ DISTINCT, NON-REPEATED 3D architectural complexes modeled after REAL
 * Puerto Vallarta beachfront resorts and authentic Río Cuale bridges!
 *
 * Every single beachfront resort opens West (-X toward Bahía de Banderas) revealing its sunlit
 * open-air lobby, animated shimmering turquoise infinity pools (MaterialId.POOL_WATER), thatched
 * palapas, swim-up bars, and beach cabanas directly from the sandy beach — with ZERO repeated
 * generic templates across the skyline!
 */
object BespokeVallartaResorts {

    private fun addOpenPalapa(m: MeshBuilder, cx: Float, cy: Float, cz: Float, radius: Float, roofH: Float, postH: Float) {
        val posts = 8
        for (i in 0 until posts) {
            val a = (2.0 * PI * i / posts).toFloat()
            val px = cx + cos(a) * (radius * 0.78f)
            val pz = cz + sin(a) * (radius * 0.78f)
            m.addCylinderY(px, cy, cy + postH, pz, 0.24f, 0.20f, 0.42f, 0.27f, 0.16f, MaterialId.MATTE, slices = 6)
        }
        m.addCylinderY(cx, cy + postH - 0.3f, cy + postH + roofH, cz, radius, 0.28f, 0.74f, 0.58f, 0.34f, MaterialId.TERRAIN, slices = 16)
        m.addCylinderY(cx, cy + postH - 0.35f, cy + postH - 0.15f, cz, radius * 0.65f, radius * 0.65f, 1.0f, 0.88f, 0.62f, MaterialId.EMISSIVE, slices = 12)
    }

    private fun addBeachfrontPoolCourt(
        m: MeshBuilder,
        cx: Float,
        cz: Float,
        deckW: Float,
        deckD: Float,
        poolW: Float,
        poolD: Float
    ) {
        // Travertine terrace deck sitting directly adjacent to the sandy beach
        m.addBox(cx, 0.42f, cz, deckW, 0.84f, deckD, 0.91f, 0.86f, 0.78f, MaterialId.MALECON_STONE)
        // Crisp white pool coping rim
        m.addBox(cx - 1.5f, 0.88f, cz, poolW + 1.4f, 0.14f, poolD + 1.4f, 0.96f, 0.96f, 0.95f, MaterialId.MATTE)
        // Animated turquoise shimmering infinity pool water!
        m.addFlatQuad(
            cx - 1.5f - poolW * 0.5f, 0.97f, cz - poolD * 0.5f,
            cx - 1.5f - poolW * 0.5f, 0.97f, cz + poolD * 0.5f,
            cx - 1.5f + poolW * 0.5f, 0.97f, cz + poolD * 0.5f,
            cx - 1.5f + poolW * 0.5f, 0.97f, cz - poolD * 0.5f,
            0.08f, 0.78f, 0.86f, MaterialId.POOL_WATER
        )
        // Beachfront sun loungers & open white cabanas lining the -X oceanfront edge
        val westEdge = cx - deckW * 0.42f
        val count = max(3, (deckD / 11f).toInt())
        for (i in 0 until count) {
            val lz = cz - deckD * 0.38f + (i / (count - 1).coerceAtLeast(1).toFloat()) * (deckD * 0.76f)
            m.addBox(westEdge, 1.10f, lz, 2.2f, 0.30f, 0.95f, 0.96f, 0.96f, 0.97f, MaterialId.MATTE)
            if (i % 2 == 0) {
                val cabX = westEdge - 2.2f
                for (dx in floatArrayOf(-1.1f, 1.1f)) {
                    for (dz in floatArrayOf(-1.1f, 1.1f)) {
                        m.addCylinderY(cabX + dx, 0.84f, 3.2f, lz + dz, 0.08f, 0.08f, 0.95f, 0.94f, 0.92f, MaterialId.MATTE, slices = 5)
                    }
                }
                m.addBox(cabX, 3.25f, lz, 2.5f, 0.14f, 2.5f, 0.98f, 0.97f, 0.95f, MaterialId.MATTE)
            }
        }
    }

    /** 1. VELAS VALLARTA (Marina North): Grand white arched Mediterranean colonnade wings + lush lagoon courtyard. */
    fun buildVelasVallartaComplex(): MeshData {
        val m = MeshBuilder(2400)
        val wR = 0.97f; val wG = 0.96f; val wB = 0.93f
        // Sweeping double-wing white Mediterranean resort with grand arched center portal
        m.addBox(16f, 14f, -24f, 24f, 28f, 36f, wR, wG, wB, MaterialId.RESORT_FACADE)
        m.addBox(16f, 14f, 24f, 24f, 28f, 36f, wR, wG, wB, MaterialId.RESORT_FACADE)
        // Monumental center bridge arch connecting North & South wings with open ocean breezeway underneath!
        m.addBox(16f, 22f, 0f, 22f, 12f, 22f, wR, wG, wB, MaterialId.RESORT_FACADE)
        m.addBox(16f, 28.8f, 0f, 25f, 1.6f, 86f, 0.78f, 0.36f, 0.24f, MaterialId.MATTE) // Terracotta crest
        // Grand white breezeway pillars framing the ocean view right through the lobby
        for (cz in floatArrayOf(-10f, -5f, 5f, 10f)) {
            m.addCylinderY(8f, 0.4f, 16f, cz, 0.85f, 0.80f, 0.98f, 0.97f, 0.95f, MaterialId.MATTE, slices = 10)
        }
        m.addBox(14f, 3.2f, 0f, 10f, 5.2f, 18f, 1.0f, 0.90f, 0.66f, MaterialId.EMISSIVE)
        addBeachfrontPoolCourt(m, cx = -12f, cz = 0f, deckW = 34f, deckD = 72f, poolW = 18f, poolD = 54f)
        addOpenPalapa(m, cx = -16f, cy = 0.85f, cz = 0f, radius = 6.8f, roofH = 4.2f, postH = 4.0f)
        return m.build()
    }

    /** 2. MARRIOTT PUERTO VALLARTA: Iconic U-shaped hacienda-modern resort opening wide onto Banderas Bay! */
    fun buildMarriottResortComplex(): MeshData {
        val m = MeshBuilder(2600)
        val r = 0.97f; val g = 0.94f; val b = 0.89f
        val roofR = 0.76f; val roofG = 0.34f; val roofB = 0.22f
        m.addBox(22f, 15.5f, 0f, 18f, 25f, 64f, r, g, b, MaterialId.RESORT_FACADE)
        m.addBox(22f, 28.6f, 0f, 19.5f, 1.4f, 65.5f, roofR, roofG, roofB, MaterialId.MATTE)
        m.addBox(15f, 2.6f, 0f, 4.5f, 4.6f, 36f, 1.0f, 0.88f, 0.62f, MaterialId.EMISSIVE)
        for (cz in -26..26 step 6) {
            m.addCylinderY(13.2f, 0.2f, 6.0f, cz.toFloat(), 0.55f, 0.50f, 0.96f, 0.94f, 0.90f, MaterialId.MATTE, slices = 8)
        }
        m.addBox(0f, 13.0f, -33f, 48f, 26f, 15f, r, g, b, MaterialId.RESORT_FACADE)
        m.addBox(0f, 26.6f, -33f, 49.5f, 1.4f, 16.5f, roofR, roofG, roofB, MaterialId.MATTE)
        m.addBox(0f, 13.0f, 33f, 48f, 26f, 15f, r, g, b, MaterialId.RESORT_FACADE)
        m.addBox(0f, 26.6f, 33f, 49.5f, 1.4f, 16.5f, roofR, roofG, roofB, MaterialId.MATTE)
        addBeachfrontPoolCourt(m, cx = -8f, cz = 0f, deckW = 40f, deckD = 48f, poolW = 20f, poolD = 32f)
        addOpenPalapa(m, cx = -18f, cy = 0.85f, cz = -16f, radius = 7.0f, roofH = 4.4f, postH = 4.0f)
        addOpenPalapa(m, cx = -6f, cy = 0.85f, cz = 18f, radius = 5.4f, roofH = 3.5f, postH = 3.8f)
        return m.build()
    }

    /** 3. THE WESTIN RESORT & SPA (Javier Sordo Madaleno): Iconic stepped crimson-terracotta pyramid & ochre pergolas! */
    fun buildWestinResortComplex(): MeshData {
        val m = MeshBuilder(2400)
        val tr = 0.86f; val tg = 0.36f; val tb = 0.24f
        val ocR = 0.94f; val ocG = 0.64f; val ocB = 0.36f
        m.addBox(14f, 6.0f, 0f, 24f, 12f, 74f, tr, tg, tb, MaterialId.RESORT_FACADE)
        m.addBox(18f, 17.0f, 0f, 18f, 11f, 56f, tr * 1.04f, tg * 1.06f, tb * 1.05f, MaterialId.RESORT_FACADE)
        m.addBox(21f, 27.0f, 0f, 14f, 10f, 38f, ocR, ocG, ocB, MaterialId.RESORT_FACADE)
        for (kz in -16..16 step 8) {
            m.addBox(21f, 32.8f, kz.toFloat(), 16f, 1.0f, 1.4f, ocR, ocG, ocB, MaterialId.MATTE)
        }
        m.addBox(-4f, 5.5f, -33f, 32f, 11f, 13f, ocR, ocG, ocB, MaterialId.RESORT_FACADE)
        m.addBox(-4f, 5.5f, 33f, 32f, 11f, 13f, ocR, ocG, ocB, MaterialId.RESORT_FACADE)
        m.addBox(3.5f, 3.2f, 0f, 4.5f, 6.0f, 24f, 1.0f, 0.86f, 0.58f, MaterialId.EMISSIVE)
        for (cz in -12..12 step 6) {
            m.addBox(1.5f, 3.2f, cz.toFloat(), 1.2f, 6.4f, 1.2f, tr, tg, tb, MaterialId.MATTE)
        }
        addBeachfrontPoolCourt(m, cx = -12f, cz = 0f, deckW = 34f, deckD = 50f, poolW = 18f, poolD = 34f)
        addOpenPalapa(m, cx = -23f, cy = 0.85f, cz = 0f, radius = 6.0f, roofH = 3.9f, postH = 3.7f)
        return m.build()
    }

    /** 4. MELIÁ PUERTO VALLARTA: Broad warm cream tropical garden resort + twin angled beachfront wings & bridge pool! */
    fun buildMeliaVallartaComplex(): MeshData {
        val m = MeshBuilder(2200)
        val cr = 0.96f; val cg = 0.89f; val cb = 0.76f
        m.addBox(18f, 13f, 0f, 20f, 26f, 68f, cr, cg, cb, MaterialId.RESORT_FACADE)
        // Stepped tropical atrium roof & terracotta trim
        m.addBox(18f, 26.8f, 0f, 22f, 1.6f, 70f, 0.80f, 0.44f, 0.24f, MaterialId.MATTE)
        // Projecting horizontal balcony bands (signature Meliá look)
        for (fy in floatArrayOf(6f, 12f, 18f, 24f)) {
            m.addBox(7.5f, fy, 0f, 1.8f, 0.7f, 68f, 0.98f, 0.96f, 0.92f, MaterialId.MATTE)
        }
        // Central open garden breezeway + footbridge across massive lagoon pool!
        addBeachfrontPoolCourt(m, cx = -10f, cz = 0f, deckW = 34f, deckD = 62f, poolW = 20f, poolD = 46f)
        m.addBox(-11f, 1.45f, 0f, 22f, 0.45f, 3.4f, 0.62f, 0.42f, 0.24f, MaterialId.MATTE) // Timber pool bridge!
        addOpenPalapa(m, cx = -16f, cy = 0.85f, cz = -20f, radius = 5.2f, roofH = 3.6f, postH = 3.6f)
        return m.build()
    }

    /** 5. CROWN PARADISE CLUB & GOLDEN: Twin 8-story oceanfront slabs + beachfront pirate waterpark tower! */
    fun buildCrownParadiseComplex(): MeshData {
        val m = MeshBuilder(2300)
        // North Golden Adult Tower (crisp ocean-blue & white)
        m.addBox(14f, 13f, -24f, 22f, 26f, 34f, 0.95f, 0.96f, 0.98f, MaterialId.RESORT_FACADE)
        m.addBox(14f, 26.7f, -24f, 23f, 1.4f, 35f, 0.14f, 0.56f, 0.78f, MaterialId.MATTE)
        // South Family Club Wing (warm yellow-ochre accent)
        m.addBox(12f, 12f, 22f, 24f, 24f, 38f, 0.97f, 0.90f, 0.70f, MaterialId.RESORT_FACADE)
        m.addBox(12f, 24.7f, 22f, 25f, 1.4f, 39f, 0.88f, 0.46f, 0.18f, MaterialId.MATTE)
        // Distinctive beachfront waterpark tower & turquoise slide pool!
        addBeachfrontPoolCourt(m, cx = -12f, cz = 0f, deckW = 30f, deckD = 68f, poolW = 16f, poolD = 50f)
        m.addBox(-14f, 5.5f, 18f, 6f, 9f, 6f, 0.72f, 0.46f, 0.24f, MaterialId.MATTE)
        m.addCylinderY(-14f, 10f, 13.5f, 18f, 4.2f, 0.3f, 0.88f, 0.24f, 0.20f, MaterialId.MATTE, slices = 10)
        return m.build()
    }

    /** 6. KRYSTAL PUERTO VALLARTA: Iconic Mexican colonial stone aqueduct arches + red-tile villas + central tower! */
    fun buildKrystalVillageComplex(): MeshData {
        val m = MeshBuilder(2400)
        // Tall recessed cream hotel tower behind village
        m.addBox(24f, 15f, 0f, 20f, 30f, 52f, 0.96f, 0.91f, 0.82f, MaterialId.RESORT_FACADE)
        m.addBox(24f, 30.8f, 0f, 21.5f, 1.6f, 53.5f, 0.76f, 0.32f, 0.20f, MaterialId.MATTE)
        // Iconic Krystal Mexican Village low colonial red-tile villas flanking the gardens!
        for (vz in floatArrayOf(-30f, 30f)) {
            m.addBox(2f, 4.5f, vz, 30f, 9f, 14f, 0.97f, 0.92f, 0.84f, MaterialId.COLONIAL_FACADE)
            m.addBox(2f, 9.7f, vz, 31.5f, 1.5f, 15.5f, 0.78f, 0.30f, 0.18f, MaterialId.MATTE)
        }
        // Iconic Krystal Decorative Colonial Stone Aqueduct Arches spanning the central beachfront pool!
        for (az in -18..18 step 9) {
            m.addBox(-4f, 4.2f, az.toFloat(), 1.8f, 7.6f, 2.2f, 0.94f, 0.86f, 0.72f, MaterialId.MATTE)
        }
        m.addBox(-4f, 8.2f, 0f, 2.2f, 1.4f, 40f, 0.92f, 0.82f, 0.68f, MaterialId.MATTE)
        addBeachfrontPoolCourt(m, cx = -14f, cz = 0f, deckW = 30f, deckD = 54f, poolW = 16f, poolD = 40f)
        addOpenPalapa(m, cx = -18f, cy = 0.85f, cz = 0f, radius = 5.8f, roofH = 3.8f, postH = 3.6f)
        return m.build()
    }

    /** 7. PENINSULA VALLARTA: 3 razor-sharp dark sapphire reflective glass slabs with bold white floor ribbons! */
    fun buildPeninsulaTowersComplex(): MeshData {
        val m = MeshBuilder(2500)
        val zOffsets = floatArrayOf(-34f, 0f, 34f)
        for (tz in zOffsets) {
            // Sleek dark-blue glass rectangular slab (distinctly darker & sharper than Grand Venetian!)
            m.addBox(16f, 3.0f, tz, 16f, 6f, 22f, 0.95f, 0.90f, 0.76f, MaterialId.EMISSIVE)
            m.addBox(16f, 34f, tz, 17f, 56f, 24f, 0.54f, 0.76f, 0.92f, MaterialId.CONDO_FACADE)
            // Bold cantilevered white architectural balcony ribbons every 11m
            for (fy in floatArrayOf(12f, 23f, 34f, 45f, 56f, 62.5f)) {
                m.addBox(15f, fy, tz, 20f, 0.9f, 26f, 0.97f, 0.98f, 0.99f, MaterialId.MATTE)
            }
        }
        addBeachfrontPoolCourt(m, cx = -10f, cz = 0f, deckW = 28f, deckD = 84f, poolW = 14f, poolD = 68f)
        return m.build()
    }

    /** 8. GRAND VENETIAN: 3 towering sculpted white-and-cyan oceanfront curved towers + cascading lagoon pools! */
    fun buildGrandVenetianComplex(): MeshData {
        val m = MeshBuilder(2700)
        val specs = arrayOf(
            floatArrayOf(16f, -36f, 20f, 60f, 24f),
            floatArrayOf(22f, 0f,   24f, 70f, 26f),
            floatArrayOf(16f, 36f,  20f, 58f, 24f)
        )
        for (sp in specs) {
            val tx = sp[0]; val tz = sp[1]; val tw = sp[2]; val th = sp[3]; val td = sp[4]
            m.addBox(tx, 3.0f, tz, tw * 0.92f, 6.0f, td * 0.92f, 0.98f, 0.92f, 0.76f, MaterialId.EMISSIVE)
            // Sculpted curved-prow oceanfront tower (main shaft + rounded glass front nose!)
            m.addBox(tx, 6f + (th - 6f) * 0.5f, tz, tw, th - 6f, td, 0.96f, 0.97f, 0.99f, MaterialId.CONDO_FACADE)
            m.addCylinderY(tx - tw * 0.45f, 6f, th, tz, td * 0.42f, td * 0.42f, 0.88f, 0.96f, 1.0f, MaterialId.CONDO_FACADE, slices = 12)
            // Penthouse crown + turquoise LED halo
            m.addBox(tx, th + 0.9f, tz, tw + 3.2f, 1.6f, td + 3.2f, 0.97f, 0.98f, 0.99f, MaterialId.MATTE)
            m.addBox(tx, th + 1.9f, tz, tw + 2.0f, 0.45f, td + 2.0f, 0.08f, 0.88f, 0.98f, MaterialId.EMISSIVE)
        }
        addBeachfrontPoolCourt(m, cx = -12f, cz = 0f, deckW = 32f, deckD = 88f, poolW = 18f, poolD = 70f)
        m.addBox(-4f, 2.8f, 0f, 9f, 3.8f, 18f, 0.22f, 0.52f, 0.68f, MaterialId.GLASS)
        m.addBox(-4f, 4.9f, 0f, 11f, 0.45f, 20f, 0.96f, 0.97f, 0.98f, MaterialId.MATTE)
        return m.build()
    }

    /** 9. FIESTA AMERICANA PUERTO VALLARTA: Unmistakable GIANT CONICAL THATCHED PALAPA LOBBY + sandstone wing! */
    fun buildFiestaAmericanaComplex(): MeshData {
        val m = MeshBuilder(2400)
        val sr = 0.94f; val sg = 0.78f; val sb = 0.62f
        m.addBox(18f, 17.5f, 0f, 22f, 35f, 56f, sr, sg, sb, MaterialId.RESORT_FACADE)
        m.addBox(18f, 35.8f, 0f, 23.5f, 1.6f, 57.5f, 0.76f, 0.34f, 0.22f, MaterialId.MATTE)
        m.addBox(6f, 13.0f, -26f, 24f, 26f, 15f, sr * 0.97f, sg * 0.96f, sb * 0.95f, MaterialId.RESORT_FACADE)
        m.addBox(6f, 13.0f, 26f, 24f, 26f, 15f, sr * 0.97f, sg * 0.96f, sb * 0.95f, MaterialId.RESORT_FACADE)
        // GIANT ICONIC PALAPA GRAND ATRIUM LOBBY visible from half a kilometer down the beach!
        addOpenPalapa(m, cx = 0f, cy = 0.8f, cz = 0f, radius = 14.5f, roofH = 12.0f, postH = 7.5f)
        addBeachfrontPoolCourt(m, cx = -18f, cz = 0f, deckW = 28f, deckD = 52f, poolW = 15f, poolD = 36f)
        addOpenPalapa(m, cx = -18f, cy = 0.85f, cz = 18f, radius = 5.0f, roofH = 3.5f, postH = 3.6f)
        return m.build()
    }

    /** 10. ICON VALLARTA (Philippe Starck): 3 sleek stark-white slender towers with deep recessed charcoal balconies! */
    fun buildIconVallartaComplex(): MeshData {
        val m = MeshBuilder(2400)
        for (tz in floatArrayOf(-30f, 0f, 30f)) {
            // Stark white minimalist vertical fins framing deep recessed dark glass bays!
            m.addBox(14f, 34f, tz, 15f, 68f, 18f, 0.34f, 0.42f, 0.50f, MaterialId.CONDO_FACADE)
            // Sculptural white side fins (signature Icon Vallarta silhouette!)
            m.addBox(14f, 35f, tz - 9.4f, 17.5f, 70f, 1.4f, 0.98f, 0.98f, 0.99f, MaterialId.MATTE)
            m.addBox(14f, 35f, tz + 9.4f, 17.5f, 70f, 1.4f, 0.98f, 0.98f, 0.99f, MaterialId.MATTE)
            m.addBox(14f, 70.5f, tz, 18.5f, 1.6f, 21f, 0.98f, 0.98f, 0.99f, MaterialId.MATTE)
        }
        // Long sleek Philippe Starck linear beachfront infinity pool & white cube cabanas
        addBeachfrontPoolCourt(m, cx = -10f, cz = 0f, deckW = 26f, deckD = 76f, poolW = 12f, poolD = 62f)
        return m.build()
    }

    /** 11. SECRETS & DREAMS VALLARTA BAY: Twin sweeping 15-story golden-sand & glass contemporary curved towers! */
    fun buildSecretsDreamsComplex(): MeshData {
        val m = MeshBuilder(2500)
        val sandR = 0.95f; val sandG = 0.88f; val sandB = 0.76f
        // Secrets (North) & Dreams (South) twin angled golden-cream luxury wings
        m.addBox(16f, 22f, -24f, 20f, 44f, 36f, sandR, sandG, sandB, MaterialId.RESORT_FACADE)
        m.addBox(16f, 22f, 24f, 20f, 44f, 36f, sandR * 0.98f, sandG * 0.96f, sandB * 0.94f, MaterialId.RESORT_FACADE)
        // Central connecting grand glass sky-bridge atrium
        m.addBox(20f, 10f, 0f, 14f, 20f, 16f, 0.86f, 0.94f, 0.98f, MaterialId.CONDO_FACADE)
        // Warm golden rooftop accent cornices
        m.addBox(16f, 44.8f, -24f, 21.5f, 1.6f, 37.5f, 0.96f, 0.80f, 0.48f, MaterialId.MATTE)
        m.addBox(16f, 44.8f, 24f, 21.5f, 1.6f, 37.5f, 0.96f, 0.80f, 0.48f, MaterialId.MATTE)
        // Expansive dual beachfront infinity pools (Secrets Adults + Dreams Family!)
        addBeachfrontPoolCourt(m, cx = -12f, cz = -20f, deckW = 28f, deckD = 34f, poolW = 16f, poolD = 24f)
        addBeachfrontPoolCourt(m, cx = -12f, cz = 20f, deckW = 28f, deckD = 34f, poolW = 16f, poolD = 24f)
        addOpenPalapa(m, cx = -14f, cy = 0.85f, cz = 0f, radius = 6.2f, roofH = 4.0f, postH = 3.8f)
        return m.build()
    }

    /** 12. SHERATON BUGANVILIAS RESORT: Unmistakable huge SWEEPING SEMI-CIRCULAR CRESCENT coral-sandstone wing! */
    fun buildSheratonComplex(): MeshData {
        val m = MeshBuilder(2600)
        val r = 0.96f; val g = 0.78f; val b = 0.64f
        // True segmented semi-circular crescent hugging Playa Tranquila (5 angled curved facade segments!)
        val segs = 5
        val radius = 48f
        for (i in 0 until segs) {
            val a = (-0.65f + (i / (segs - 1).toFloat()) * 1.30f)
            val cx = 24f - cos(a) * 14f
            val cz = sin(a) * radius
            m.addBox(cx, 17f, cz, 18f, 34f, 20f, r, g, b, MaterialId.RESORT_FACADE)
            m.addBox(cx, 34.7f, cz, 19.5f, 1.4f, 21.5f, 0.80f, 0.36f, 0.24f, MaterialId.MATTE)
        }
        // Open sea-facing ground-level atrium colonnade
        m.addBox(4f, 3.0f, 0f, 8f, 5.2f, 40f, 1.0f, 0.88f, 0.66f, MaterialId.EMISSIVE)
        for (cz in -18..18 step 6) {
            m.addCylinderY(0.2f, 0.4f, 5.8f, cz.toFloat(), 0.45f, 0.42f, 0.96f, 0.93f, 0.88f, MaterialId.MATTE, slices = 8)
        }
        addBeachfrontPoolCourt(m, cx = -14f, cz = 0f, deckW = 32f, deckD = 66f, poolW = 18f, poolD = 46f)
        addOpenPalapa(m, cx = -16f, cy = 0.85f, cz = -22f, radius = 6.0f, roofH = 4.0f, postH = 3.8f)
        addOpenPalapa(m, cx = -16f, cy = 0.85f, cz = 22f, radius = 6.0f, roofH = 4.0f, postH = 3.8f)
        return m.build()
    }

    /** 13. BUENAVENTURA GRAND & VILLA PREMIERE: Warm ochre-terracotta hacienda arches + sleek boutique beachfront wing! */
    fun buildBuenaventuraComplex(): MeshData {
        val m = MeshBuilder(2000)
        // Warm Jalisco ochre-orange hacienda facade
        m.addBox(12f, 11f, 0f, 18f, 22f, 46f, 0.95f, 0.72f, 0.48f, MaterialId.RESORT_FACADE)
        m.addBox(12f, 22.8f, 0f, 19.5f, 1.6f, 47.5f, 0.78f, 0.30f, 0.18f, MaterialId.MATTE)
        // Open beachfront arcade pillars
        for (cz in -18..18 step 6) {
            m.addCylinderY(2.2f, 0.4f, 5.2f, cz.toFloat(), 0.42f, 0.40f, 0.98f, 0.94f, 0.88f, MaterialId.MATTE, slices = 8)
        }
        m.addBox(3.5f, 2.8f, 0f, 3.2f, 4.4f, 34f, 1.0f, 0.88f, 0.62f, MaterialId.EMISSIVE)
        addBeachfrontPoolCourt(m, cx = -10f, cz = 0f, deckW = 24f, deckD = 42f, poolW = 13f, poolD = 28f)
        addOpenPalapa(m, cx = -12f, cy = 0.85f, cz = -14f, radius = 5.0f, roofH = 3.4f, postH = 3.5f)
        return m.build()
    }

    /** 14. HOTEL ROSITA (Historic 1948 Landmark at North Start of El Malecón): Whitewashed arches & terracotta tile roof! */
    fun buildHotelRositaComplex(): MeshData {
        val m = MeshBuilder(1800)
        m.addBox(10f, 6.5f, 0f, 16f, 13f, 44f, 0.98f, 0.96f, 0.92f, MaterialId.COLONIAL_FACADE)
        m.addBox(10f, 13.6f, 0f, 17.5f, 1.6f, 45.5f, 0.78f, 0.30f, 0.20f, MaterialId.MATTE)
        m.addBox(-4f, 5.5f, -18f, 22f, 11f, 12f, 0.98f, 0.96f, 0.92f, MaterialId.COLONIAL_FACADE)
        m.addBox(-4f, 11.6f, -18f, 23.5f, 1.4f, 13.5f, 0.78f, 0.30f, 0.20f, MaterialId.MATTE)
        addBeachfrontPoolCourt(m, cx = -8f, cz = 6f, deckW = 22f, deckD = 28f, poolW = 11f, poolD = 18f)
        addOpenPalapa(m, cx = -12f, cy = 0.85f, cz = 18f, radius = 4.5f, roofH = 3.2f, postH = 3.4f)
        return m.build()
    }

    /** 15. HYATT ZIVA (Playa Las Estacas): Private cove resort with twin stepped white beachfront towers & infinity pools! */
    fun buildHyattZivaCoveComplex(): MeshData {
        val m = MeshBuilder(2300)
        // North Main Club Tower (stepped white contemporary tower)
        m.addBox(14f, 18f, -20f, 20f, 36f, 28f, 0.97f, 0.96f, 0.94f, MaterialId.RESORT_FACADE)
        m.addBox(16f, 37.5f, -20f, 16f, 3f, 22f, 0.94f, 0.95f, 0.97f, MaterialId.CONDO_FACADE)
        // South Curved Beachfront Wing curving around the cove
        m.addBox(8f, 12f, 22f, 26f, 24f, 18f, 0.96f, 0.94f, 0.90f, MaterialId.RESORT_FACADE)
        // Multi-level infinity pools right on Playa Las Estacas sand
        addBeachfrontPoolCourt(m, cx = -12f, cz = -4f, deckW = 28f, deckD = 54f, poolW = 16f, poolD = 40f)
        addOpenPalapa(m, cx = -16f, cy = 0.85f, cz = 16f, radius = 6.0f, roofH = 4.0f, postH = 3.8f)
        return m.build()
    }

    /** 16. GARZA BLANCA & HOTEL MOUSAI: Stepped hillside glass-and-limestone resort rising above Playa Gemelas! */
    fun buildGarzaBlancaTerraceComplex(): MeshData {
        val m = MeshBuilder(2300)
        // Lower beachfront white-and-glass terrace wing
        m.addBox(4f, 11f, 0f, 18f, 22f, 54f, 0.97f, 0.96f, 0.94f, MaterialId.CONDO_FACADE)
        // Upper hillside Mousai glass tower rising dramatically behind it
        m.addBox(24f, 26f, -8f, 20f, 52f, 36f, 0.86f, 0.92f, 0.98f, MaterialId.CONDO_FACADE)
        m.addBox(24f, 52.8f, -8f, 22f, 1.6f, 38f, 0.98f, 0.24f, 0.42f, MaterialId.EMISSIVE) // Iconic Mousai rooftop glow!
        addBeachfrontPoolCourt(m, cx = -14f, cz = 0f, deckW = 24f, deckD = 48f, poolW = 14f, poolD = 34f)
        return m.build()
    }

    /** 17. BARCELÓ PUERTO VALLARTA (Playa Mismaloya): Grand stepped golden-cream amphitheater facing Los Arcos! */
    fun buildBarceloMismaloyaComplex(): MeshData {
        val m = MeshBuilder(2300)
        val r = 0.97f; val g = 0.88f; val b = 0.72f
        // Central grand stepped amphitheater block + flanking Mismaloya bay wings
        m.addBox(20f, 17f, 0f, 22f, 34f, 60f, r, g, b, MaterialId.RESORT_FACADE)
        m.addBox(20f, 34.8f, 0f, 24f, 1.6f, 62f, 0.78f, 0.34f, 0.20f, MaterialId.MATTE)
        m.addBox(2f, 12f, -32f, 34f, 24f, 16f, r, g, b, MaterialId.RESORT_FACADE)
        m.addBox(2f, 12f, 32f, 34f, 24f, 16f, r, g, b, MaterialId.RESORT_FACADE)
        addBeachfrontPoolCourt(m, cx = -10f, cz = 0f, deckW = 34f, deckD = 48f, poolW = 18f, poolD = 32f)
        addOpenPalapa(m, cx = -18f, cy = 0.85f, cz = 0f, radius = 7.0f, roofH = 4.6f, postH = 4.0f)
        return m.build()
    }

    /** 18. OPEN-BEACHFRONT BOUTIQUE RESORT (Sunscape, Friendly Fun, Villa del Palmar, Canto del Sol, Tropicana, Blue Chairs): */
    fun buildBeachOpenBoutiqueResort(): MeshData {
        val m = MeshBuilder(1800)
        m.addBox(12f, 11.5f, 0f, 18f, 23f, 40f, 0.96f, 0.93f, 0.88f, MaterialId.RESORT_FACADE)
        m.addBox(12f, 23.6f, 0f, 19.2f, 1.2f, 41.2f, 0.82f, 0.42f, 0.26f, MaterialId.MATTE)
        m.addBox(-1f, 8.5f, -17f, 20f, 17f, 12f, 0.96f, 0.93f, 0.88f, MaterialId.RESORT_FACADE)
        m.addBox(2.5f, 2.5f, 4f, 4f, 4.4f, 22f, 1.0f, 0.88f, 0.64f, MaterialId.EMISSIVE)
        addBeachfrontPoolCourt(m, cx = -10f, cz = 6f, deckW = 22f, deckD = 28f, poolW = 12f, poolD = 18f)
        addOpenPalapa(m, cx = -14f, cy = 0.85f, cz = -12f, radius = 4.6f, roofH = 3.3f, postH = 3.4f)
        return m.build()
    }

    /** 18b. FLAMINGO VALLARTA HOTEL & MARINA: Iconic Mexican Hacienda on the Marina peninsula — white/ochre arched wings, red terracotta hipped roofs, colonial cupola dome & marina-front pool + palapa bar! */
    fun buildFlamingoVallartaComplex(): MeshData {
        val m = MeshBuilder(2400)
        val wallR = 0.98f; val wallG = 0.92f; val wallB = 0.81f
        val tileR = 0.78f; val tileG = 0.30f; val tileB = 0.16f
        // Main East Hacienda Wing + North & South Marina-embracing wings (opening West toward the yachts!)
        m.addBox(16f, 7.2f, 0f, 15f, 14.4f, 54f, wallR, wallG, wallB, MaterialId.COLONIAL_FACADE)
        m.addBox(-4f, 6.4f, -24f, 34f, 12.8f, 13f, wallR, wallG, wallB, MaterialId.COLONIAL_FACADE)
        m.addBox(-4f, 6.4f, 24f, 34f, 12.8f, 13f, wallR, wallG, wallB, MaterialId.COLONIAL_FACADE)
        // Overhanging steep red-clay terracotta Hacienda pitched roofs (`teja roja`)
        m.addBox(16f, 15.1f, 0f, 17.5f, 1.5f, 56.5f, tileR, tileG, tileB, MaterialId.MATTE)
        m.addBox(16f, 16.5f, 0f, 11.0f, 1.4f, 50.0f, tileR * 0.94f, tileG * 0.94f, tileB * 0.94f, MaterialId.MATTE)
        m.addBox(-4f, 13.4f, -24f, 36.2f, 1.4f, 15.2f, tileR, tileG, tileB, MaterialId.MATTE)
        m.addBox(-4f, 13.4f, 24f, 36.2f, 1.4f, 15.2f, tileR, tileG, tileB, MaterialId.MATTE)
        // Iconic Central Colonial Cupola Dome & Gold Finial overlooking Marina Vallarta!
        m.addCylinderY(16f, 16.8f, 19.6f, 0f, 4.2f, 4.0f, wallR, wallG, wallB, MaterialId.COLONIAL_FACADE, slices = 10)
        m.addEllipsoid(16f, 20.0f, 0f, 4.4f, 2.6f, 4.4f, tileR, tileG, tileB, MaterialId.MATTE, stacks = 6, slices = 12)
        m.addCylinderY(16f, 22.4f, 24.0f, 0f, 0.24f, 0.08f, 0.95f, 0.78f, 0.30f, MaterialId.CHROME, slices = 6)
        // Ground-floor arched veranda columns & warm glowing Hacienda lobby facing the yacht slips
        for (cz in -20..20 step 5) {
            m.addCylinderY(7.8f, 0.3f, 4.8f, cz.toFloat(), 0.42f, 0.38f, 0.98f, 0.96f, 0.92f, MaterialId.MATTE, slices = 8)
        }
        m.addBox(9.2f, 2.4f, 0f, 3.2f, 4.2f, 30f, 1.0f, 0.88f, 0.60f, MaterialId.EMISSIVE)
        // Marina-facing travertine courtyard, turquoise swimming pool & thatched palapa swim-up bar!
        addBeachfrontPoolCourt(m, cx = -8f, cz = 0f, deckW = 28f, deckD = 34f, poolW = 14f, poolD = 22f)
        addOpenPalapa(m, cx = -15f, cy = 0.85f, cz = -10f, radius = 5.2f, roofH = 3.6f, postH = 3.5f)
        return m.build()
    }

    /** 19. ICONIC PUENTE PEATONAL COLGANTE DEL MALECÓN OVER RÍO CUALE (Cable-stayed inclined white pylon!). */
    fun buildCualeCablePedestrianBridge(): MeshData {
        val m = MeshBuilder(1400)
        val halfW = 5.4f
        val bridgeLen = 68f
        m.addBox(0f, -0.35f, 0f, halfW * 2.0f, 0.70f, bridgeLen, 0.90f, 0.86f, 0.78f, MaterialId.MALECON_STONE)
        for (sx in floatArrayOf(-halfW + 0.25f, halfW - 0.25f)) {
            m.addBox(sx, 0.62f, 0f, 0.14f, 0.95f, bridgeLen, 0.20f, 0.42f, 0.56f, MaterialId.GLASS)
            m.addBox(sx, 1.15f, 0f, 0.22f, 0.12f, bridgeLen, 0.92f, 0.94f, 0.96f, MaterialId.CHROME)
        }
        val mastX = halfW + 1.4f
        m.addTaperedLimb(
            mastX, -3.5f, 0f, 0.95f, 0.95f,
            mastX + 3.2f, 19.5f, 0f, 0.38f, 0.38f,
            0.97f, 0.98f, 0.99f, MaterialId.GLOSSY_PAINT,
            slices = 12, rings = 6
        )
        m.addEllipsoid(mastX + 3.2f, 20.0f, 0f, 0.55f, 0.75f, 0.55f, 0.10f, 0.90f, 0.98f, MaterialId.EMISSIVE, stacks = 5, slices = 8)
        val anchorZs = floatArrayOf(-28f, -21f, -14f, -7f, 7f, 14f, 21f, 28f)
        for ((idx, az) in anchorZs.withIndex()) {
            val anchorY = 12.5f + (idx % 4) * 1.8f
            val ax = mastX + (anchorY / 19.5f) * 3.2f
            m.addTaperedLimb(ax, anchorY, 0f, 0.07f, 0.07f, halfW, 0.4f, az, 0.07f, 0.07f, 0.88f, 0.92f, 0.96f, MaterialId.CHROME, slices = 5, rings = 1)
            m.addTaperedLimb(ax, anchorY, 0f, 0.07f, 0.07f, -halfW, 0.4f, az, 0.07f, 0.07f, 0.88f, 0.92f, 0.96f, MaterialId.CHROME, slices = 5, rings = 1)
        }
        for (pz in floatArrayOf(-22f, 0f, 22f)) {
            m.addCylinderY(-3.0f, -4.0f, -0.35f, pz, 0.85f, 0.85f, 0.78f, 0.76f, 0.74f, MaterialId.MATTE, slices = 10)
            m.addCylinderY(3.0f, -4.0f, -0.35f, pz, 0.85f, 0.85f, 0.78f, 0.76f, 0.74f, MaterialId.MATTE, slices = 10)
        }
        return m.build()
    }

    /** 20. AUTHENTIC PUENTES VEHICULARES DEL RÍO CUALE (Morelos & Ignacio L. Vallarta — open sky, colonial lamps!). */
    fun buildCualeVehicularBridge(): MeshData {
        val m = MeshBuilder(1100)
        val halfRoad = 11.4f
        val bridgeLen = 74f
        for (sx in floatArrayOf(-halfRoad, halfRoad)) {
            m.addBox(sx, 0.35f, 0f, 0.75f, 0.72f, bridgeLen, 0.86f, 0.82f, 0.76f, MaterialId.MATTE)
            m.addBox(sx, 1.15f, 0f, 0.16f, 0.12f, bridgeLen, 0.16f, 0.18f, 0.22f, MaterialId.CHROME)
            for (k in -4..4) {
                val pz = k * 8.5f
                m.addBox(sx, 0.75f, pz, 0.35f, 1.1f, 0.35f, 0.20f, 0.22f, 0.26f, MaterialId.CHROME)
                if (k % 2 == 0) {
                    m.addCylinderY(sx, 1.2f, 4.4f, pz, 0.10f, 0.07f, 0.15f, 0.17f, 0.20f, MaterialId.CHROME, slices = 8)
                    m.addEllipsoid(sx, 4.65f, pz, 0.28f, 0.32f, 0.28f, 1.0f, 0.90f, 0.62f, MaterialId.EMISSIVE, stacks = 5, slices = 8)
                }
            }
            m.addBox(sx * 0.92f, -0.75f, 0f, 0.95f, 1.15f, bridgeLen, 0.72f, 0.70f, 0.66f, MaterialId.MATTE)
        }
        for (pz in floatArrayOf(-26f, -8f, 8f, 26f)) {
            for (px in floatArrayOf(-7.5f, 0f, 7.5f)) {
                m.addCylinderY(px, -4.2f, -0.6f, pz, 0.95f, 0.95f, 0.74f, 0.72f, 0.68f, MaterialId.MATTE, slices = 10)
            }
        }
        return m.build()
    }
}
