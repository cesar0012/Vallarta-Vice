package com.example.game.engine

import kotlin.math.*

enum class VehicleType(
    val displayName: String,
    val maxSpeed: Float,
    val accel: Float,
    val grip: Float,
    val length: Float,
    val width: Float,
    val wheelBaseZ: Float,
    val trackHalfX: Float,
    val wheelRadius: Float,
    val isConvertible: Boolean = false,
    val isPolice: Boolean = false
) {
    SUPERCAR("Vallarta GT V12", 52f, 26f, 0.94f, 4.6f, 2.08f, 1.42f, 0.95f, 0.37f),
    CONVERTIBLE("Malecón Spyder", 46f, 23f, 0.92f, 4.5f, 1.98f, 1.38f, 0.91f, 0.36f, isConvertible = true),
    MUSCLE("Banderas V8 Coupé", 44f, 22f, 0.87f, 4.7f, 2.02f, 1.44f, 0.92f, 0.38f),
    TAXI("Taxi Puerto Vallarta", 36f, 17f, 0.90f, 4.4f, 1.88f, 1.34f, 0.86f, 0.34f),
    POLICE("Policía Municipal PV", 48f, 24f, 0.93f, 4.8f, 2.04f, 1.48f, 0.94f, 0.40f, isPolice = true),
    SUV("Sierra 4x4 Costera", 39f, 18f, 0.89f, 4.9f, 2.10f, 1.50f, 0.96f, 0.43f),
    BUS("Camión Urbano PV", 29f, 11f, 0.88f, 8.6f, 2.48f, 2.75f, 1.15f, 0.47f)
}

object HighPolyVehicleMeshes {

    /**
     * High-polygon 5-spoke metallic alloy wheel with toroidal rubber tire, rim lip,
     * radial spokes, center lug cap, and red brake caliper. Radius = 1.0 (scaled by wheelRadius).
     */
    fun buildAlloyWheel(): MeshData {
        val m = MeshBuilder(1100)
        // Outer sculpted black rubber tire (torus)
        m.addWheelTorus(0f, 0f, 0f, majorR = 0.72f, minorR = 0.28f, r = 0.08f, g = 0.08f, b = 0.09f, MaterialId.MATTE, majorSegs = 24, minorSegs = 12)
        // Chrome outer rim lip
        m.addWheelTorus(0f, 0f, 0f, majorR = 0.51f, minorR = 0.07f, r = 0.88f, g = 0.90f, b = 0.94f, MaterialId.CHROME, majorSegs = 18, minorSegs = 6)
        // Dark inner wheel drum
        m.addEllipsoid(0f, 0f, 0f, 0.16f, 0.46f, 0.46f, 0.14f, 0.15f, 0.17f, MaterialId.MATTE, stacks = 6, slices = 12)
        // Center metallic hub cap
        m.addEllipsoid(0f, 0f, 0f, 0.22f, 0.17f, 0.17f, 0.86f, 0.88f, 0.92f, MaterialId.CHROME, stacks = 6, slices = 10)
        // 5 sculpted twin-style alloy spokes on both faces
        for (i in 0 until 5) {
            val ang = (2.0 * PI * i / 5.0).toFloat()
            val cy = cos(ang) * 0.24f; val cz = sin(ang) * 0.24f
            val sy = cos(ang) * 0.49f; val sz = sin(ang) * 0.49f
            val py = -sin(ang) * 0.062f; val pz = cos(ang) * 0.062f
            m.addFlatQuad(
                0.20f, cy - py, cz - pz,
                0.20f, cy + py, cz + pz,
                0.18f, sy + py * 0.65f, sz + pz * 0.65f,
                0.18f, sy - py * 0.65f, sz - pz * 0.65f,
                0.92f, 0.94f, 0.97f, MaterialId.CHROME
            )
            m.addFlatQuad(
                -0.20f, cy + py, cz + pz,
                -0.20f, cy - py, cz - pz,
                -0.18f, sy - py * 0.65f, sz - pz * 0.65f,
                -0.18f, sy + py * 0.65f, sz + pz * 0.65f,
                0.92f, 0.94f, 0.97f, MaterialId.CHROME
            )
        }
        // Red performance brake caliper visible inside spokes
        m.addBox(0.06f, 0.22f, 0.25f, 0.15f, 0.22f, 0.16f, 0.92f, 0.12f, 0.10f, MaterialId.GLOSSY_PAINT)
        return m.build()
    }

    fun buildVehicleBody(type: VehicleType, r: Float, g: Float, b: Float): MeshData {
        return when (type) {
            VehicleType.SUPERCAR -> buildSupercar(r, g, b)
            VehicleType.CONVERTIBLE -> buildConvertible(r, g, b)
            VehicleType.MUSCLE -> buildMuscle(r, g, b)
            VehicleType.TAXI -> buildTaxi()
            VehicleType.POLICE -> buildPoliceInterceptor()
            VehicleType.SUV -> buildSierraSuv(r, g, b)
            VehicleType.BUS -> buildUrbanBus()
        }
    }

    private fun addDarkUnderbodyAndWheelWells(m: MeshBuilder, halfW: Float, wbZ: Float, halfLen: Float, sillY: Float) {
        // Planted dark chassis floorpan (eliminates "floating shell" look!)
        m.addBox(0f, sillY + 0.06f, 0f, halfW * 1.82f, 0.14f, halfLen * 1.84f, 0.06f, 0.06f, 0.08f, MaterialId.MATTE)
        // 4 dark recessed wheel well arches behind the tires
        for (sx in floatArrayOf(-halfW * 0.88f, halfW * 0.88f)) {
            for (sz in floatArrayOf(wbZ, -wbZ)) {
                m.addEllipsoid(sx, sillY + 0.22f, sz, 0.16f, 0.42f, 0.44f, 0.05f, 0.05f, 0.06f, MaterialId.MATTE, stacks = 5, slices = 8)
            }
        }
        // Jalisco white-and-blue rear license plate
        m.addBox(0f, sillY + 0.34f, -halfLen - 0.02f, 0.34f, 0.16f, 0.03f, 0.95f, 0.96f, 0.98f, MaterialId.MATTE)
    }

    private fun buildSupercar(r: Float, g: Float, b: Float): MeshData {
        val m = MeshBuilder(1800)
        // Low-slung, planted smooth-shaded exotic supercar hull (sill sits realistically at y = 0.16m!)
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.32f, 0.22f, 0.48f, 0.76f, 0.92f, 0.84f, 0.98f, 1.02f, 0.86f),
            MeshBuilder.LoftStation(-1.72f, 0.16f, 0.46f, 0.78f, 0.96f, 0.96f, 1.05f, 1.04f, 0.90f),
            MeshBuilder.LoftStation(-0.85f, 0.15f, 0.44f, 0.76f, 0.92f, 0.94f, 1.01f, 1.00f, 0.86f),
            MeshBuilder.LoftStation(0.65f, 0.15f, 0.44f, 0.72f, 0.84f, 0.94f, 1.02f, 1.01f, 0.86f),
            MeshBuilder.LoftStation(1.55f, 0.16f, 0.42f, 0.64f, 0.72f, 0.92f, 1.00f, 0.96f, 0.80f),
            MeshBuilder.LoftStation(2.14f, 0.17f, 0.34f, 0.48f, 0.56f, 0.84f, 0.90f, 0.84f, 0.66f),
            MeshBuilder.LoftStation(2.34f, 0.20f, 0.28f, 0.38f, 0.44f, 0.62f, 0.68f, 0.62f, 0.48f)
        )
        m.addSmoothLoftedBody(bodyStations, r, g, b, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.96f, wbZ = 1.42f, halfLen = 2.30f, sillY = 0.15f)

        // Smooth curved teardrop glass cockpit
        val cabinStations = listOf(
            MeshBuilder.LoftStation(-1.42f, 0.90f, 0.96f, 1.04f, 1.08f, 0.78f, 0.74f, 0.66f, 0.50f),
            MeshBuilder.LoftStation(-0.52f, 0.88f, 1.02f, 1.18f, 1.24f, 0.82f, 0.78f, 0.70f, 0.54f),
            MeshBuilder.LoftStation(0.32f, 0.84f, 0.98f, 1.16f, 1.22f, 0.82f, 0.78f, 0.69f, 0.52f),
            MeshBuilder.LoftStation(1.16f, 0.74f, 0.78f, 0.84f, 0.88f, 0.76f, 0.72f, 0.64f, 0.48f)
        )
        m.addSmoothLoftedBody(cabinStations, 0.06f, 0.11f, 0.18f, MaterialId.GLASS)

        // Sculpted painted roof panel & rear engine deck slats
        m.addBox(0f, 1.24f, -0.14f, 0.98f, 0.05f, 0.92f, r, g, b, MaterialId.GLOSSY_PAINT)
        for (k in 0..3) {
            m.addBox(0f, 1.00f - k * 0.025f, -1.04f - k * 0.22f, 0.84f, 0.03f, 0.12f, 0.10f, 0.11f, 0.13f, MaterialId.MATTE)
        }

        // Carbon splitter, side aero vents, skirts & rear diffuser
        m.addBox(0f, 0.15f, 2.22f, 1.84f, 0.05f, 0.34f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(-1.00f, 0.17f, -0.08f, 0.10f, 0.08f, 2.10f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(1.00f, 0.17f, -0.08f, 0.10f, 0.08f, 2.10f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(0f, 0.24f, -2.29f, 1.66f, 0.18f, 0.14f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)

        // Side air intakes (dark recessed supercar scoops)
        m.addBox(-1.01f, 0.56f, -0.78f, 0.06f, 0.26f, 0.62f, 0.06f, 0.07f, 0.09f, MaterialId.MATTE)
        m.addBox(1.01f, 0.56f, -0.78f, 0.06f, 0.26f, 0.62f, 0.06f, 0.07f, 0.09f, MaterialId.MATTE)

        // GT Carbon Rear Wing
        m.addBox(-0.58f, 1.02f, -2.06f, 0.05f, 0.26f, 0.22f, 0.10f, 0.11f, 0.13f, MaterialId.MATTE)
        m.addBox(0.58f, 1.02f, -2.06f, 0.05f, 0.26f, 0.22f, 0.10f, 0.11f, 0.13f, MaterialId.MATTE)
        m.addBox(0f, 1.17f, -2.10f, 1.92f, 0.05f, 0.30f, 0.11f, 0.12f, 0.15f, MaterialId.GLOSSY_PAINT)
        m.addBox(-0.96f, 1.17f, -2.10f, 0.05f, 0.18f, 0.34f, r, g, b, MaterialId.GLOSSY_PAINT)
        m.addBox(0.96f, 1.17f, -2.10f, 0.05f, 0.18f, 0.34f, r, g, b, MaterialId.GLOSSY_PAINT)

        // Sleek side mirrors
        m.addBox(-1.02f, 0.88f, 0.58f, 0.20f, 0.08f, 0.15f, r, g, b, MaterialId.GLOSSY_PAINT)
        m.addBox(1.02f, 0.88f, 0.58f, 0.20f, 0.08f, 0.15f, r, g, b, MaterialId.GLOSSY_PAINT)

        // Sharp LED headlamps & full-width glowing brake bar + quad chrome exhausts
        m.addBox(-0.64f, 0.50f, 2.22f, 0.38f, 0.08f, 0.12f, 1.0f, 0.98f, 0.88f, MaterialId.EMISSIVE)
        m.addBox(0.64f, 0.50f, 2.22f, 0.38f, 0.08f, 0.12f, 1.0f, 0.98f, 0.88f, MaterialId.EMISSIVE)
        m.addBox(0f, 0.72f, -2.32f, 1.72f, 0.08f, 0.06f, 1.0f, 0.08f, 0.10f, MaterialId.EMISSIVE)
        for (ex in floatArrayOf(-0.44f, -0.30f, 0.30f, 0.44f)) {
            m.addEllipsoid(ex, 0.30f, -2.35f, 0.052f, 0.052f, 0.09f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, stacks = 5, slices = 8)
        }
        return m.build()
    }

    private fun buildConvertible(r: Float, g: Float, b: Float): MeshData {
        val m = MeshBuilder(1600)
        // Sleek low-slung open roadster body (smooth normals!)
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.22f, 0.20f, 0.46f, 0.72f, 0.84f, 0.82f, 0.94f, 0.96f, 0.84f),
            MeshBuilder.LoftStation(-1.35f, 0.16f, 0.44f, 0.72f, 0.86f, 0.90f, 0.98f, 0.98f, 0.86f),
            MeshBuilder.LoftStation(0.68f, 0.16f, 0.44f, 0.70f, 0.82f, 0.90f, 0.97f, 0.97f, 0.84f),
            MeshBuilder.LoftStation(1.62f, 0.17f, 0.42f, 0.62f, 0.72f, 0.88f, 0.95f, 0.92f, 0.78f),
            MeshBuilder.LoftStation(2.22f, 0.20f, 0.36f, 0.50f, 0.58f, 0.74f, 0.80f, 0.76f, 0.62f)
        )
        m.addSmoothLoftedBody(bodyStations, r, g, b, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.92f, wbZ = 1.38f, halfLen = 2.20f, sillY = 0.16f)

        // Recessed saddle-tan leather cockpit recessed cleanly inside beltline
        m.addBox(0f, 0.56f, -0.16f, 1.46f, 0.24f, 1.42f, 0.64f, 0.42f, 0.26f, MaterialId.MATTE)
        for (sx in floatArrayOf(-0.42f, 0.42f)) {
            m.addBox(sx, 0.74f, -0.58f, 0.44f, 0.44f, 0.14f, 0.62f, 0.40f, 0.24f, MaterialId.MATTE)
            m.addEllipsoid(sx, 1.00f, -0.60f, 0.14f, 0.11f, 0.09f, 0.62f, 0.40f, 0.24f, MaterialId.MATTE, stacks = 5, slices = 8)
            m.addArchRibbon(sx, 0.84f, -0.78f, 0.18f, 0.24f, 0.05f, 0.045f, alongZ = false, r = 0.88f, g = 0.90f, b = 0.94f, matId = MaterialId.CHROME, segments = 8)
        }
        // Dashboard & steering wheel
        m.addBox(0f, 0.80f, 0.52f, 1.42f, 0.16f, 0.30f, 0.12f, 0.12f, 0.14f, MaterialId.MATTE)
        m.addWheelTorus(-0.42f, 0.88f, 0.32f, 0.14f, 0.024f, 0.10f, 0.10f, 0.12f, MaterialId.MATTE, majorSegs = 12, minorSegs = 6)

        // Raked glass windshield + chrome A-pillar surround (reaches y = 1.28m so seated driver fits cleanly inside!)
        m.addFlatQuad(
            -0.82f, 0.80f, 0.78f,
            0.82f, 0.80f, 0.78f,
            0.74f, 1.28f, 0.30f,
            -0.74f, 1.28f, 0.30f,
            0.12f, 0.22f, 0.32f, MaterialId.GLASS
        )
        m.addBox(0f, 1.29f, 0.30f, 1.52f, 0.045f, 0.055f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME)
        m.addTaperedLimb(-0.82f, 0.80f, 0.78f, 0.03f, 0.03f, -0.75f, 1.29f, 0.30f, 0.028f, 0.028f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, slices = 6, rings = 1, capBottom = false, capTop = false)
        m.addTaperedLimb(0.82f, 0.80f, 0.78f, 0.03f, 0.03f, 0.75f, 1.29f, 0.30f, 0.028f, 0.028f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, slices = 6, rings = 1, capBottom = false, capTop = false)

        // Grille, headlights, taillights & chrome exhausts
        m.addBox(0f, 0.42f, 2.22f, 1.00f, 0.15f, 0.06f, 0.14f, 0.15f, 0.18f, MaterialId.CHROME)
        m.addBox(-0.62f, 0.50f, 2.16f, 0.34f, 0.11f, 0.12f, 1.0f, 0.98f, 0.88f, MaterialId.EMISSIVE)
        m.addBox(0.62f, 0.50f, 2.16f, 0.34f, 0.11f, 0.12f, 1.0f, 0.98f, 0.88f, MaterialId.EMISSIVE)
        m.addBox(-0.62f, 0.68f, -2.23f, 0.42f, 0.10f, 0.07f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(0.62f, 0.68f, -2.23f, 0.42f, 0.10f, 0.07f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addEllipsoid(-0.54f, 0.28f, -2.26f, 0.055f, 0.055f, 0.09f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, stacks = 5, slices = 8)
        m.addEllipsoid(0.54f, 0.28f, -2.26f, 0.055f, 0.055f, 0.09f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME, stacks = 5, slices = 8)
        return m.build()
    }

    private fun buildMuscle(r: Float, g: Float, b: Float): MeshData {
        val m = MeshBuilder(1400)
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.34f, 0.22f, 0.52f, 0.78f, 0.92f, 0.88f, 0.98f, 0.99f, 0.88f),
            MeshBuilder.LoftStation(-1.35f, 0.18f, 0.50f, 0.80f, 0.94f, 0.92f, 1.01f, 1.00f, 0.90f),
            MeshBuilder.LoftStation(0.70f, 0.18f, 0.50f, 0.78f, 0.90f, 0.92f, 1.00f, 0.99f, 0.88f),
            MeshBuilder.LoftStation(1.85f, 0.20f, 0.48f, 0.72f, 0.84f, 0.90f, 0.98f, 0.96f, 0.84f),
            MeshBuilder.LoftStation(2.34f, 0.22f, 0.42f, 0.62f, 0.74f, 0.84f, 0.90f, 0.88f, 0.78f)
        )
        m.addSmoothLoftedBody(bodyStations, r, g, b, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.94f, wbZ = 1.44f, halfLen = 2.32f, sillY = 0.18f)

        val cabinStations = listOf(
            MeshBuilder.LoftStation(-1.58f, 0.92f, 1.02f, 1.12f, 1.18f, 0.82f, 0.78f, 0.72f, 0.60f),
            MeshBuilder.LoftStation(-0.52f, 0.92f, 1.12f, 1.28f, 1.36f, 0.84f, 0.80f, 0.74f, 0.64f),
            MeshBuilder.LoftStation(0.42f, 0.90f, 1.10f, 1.26f, 1.34f, 0.84f, 0.80f, 0.74f, 0.64f),
            MeshBuilder.LoftStation(1.06f, 0.86f, 0.92f, 0.96f, 1.00f, 0.82f, 0.78f, 0.72f, 0.62f)
        )
        m.addSmoothLoftedBody(cabinStations, 0.06f, 0.10f, 0.16f, MaterialId.GLASS)
        m.addBox(0f, 1.36f, -0.06f, 1.20f, 0.07f, 1.04f, r, g, b, MaterialId.GLOSSY_PAINT)

        // Racing stripes & chrome hood blower
        for (sx in floatArrayOf(-0.22f, 0.22f)) {
            m.addBox(sx, 0.88f, 1.56f, 0.15f, 0.03f, 1.30f, 0.96f, 0.96f, 0.98f, MaterialId.GLOSSY_PAINT)
        }
        m.addBox(0f, 0.96f, 1.48f, 0.42f, 0.16f, 0.54f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME)
        m.addBox(0f, 0.34f, 2.38f, 1.84f, 0.12f, 0.12f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME)
        m.addBox(0f, 0.34f, -2.38f, 1.86f, 0.12f, 0.12f, 0.88f, 0.90f, 0.94f, MaterialId.CHROME)
        m.addBox(0f, 0.60f, 2.34f, 1.68f, 0.20f, 0.06f, 0.08f, 0.09f, 0.11f, MaterialId.MATTE)
        for (hx in floatArrayOf(-0.72f, -0.52f, 0.52f, 0.72f)) {
            m.addEllipsoid(hx, 0.60f, 2.36f, 0.08f, 0.08f, 0.045f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE, stacks = 5, slices = 8)
        }
        for (tx in floatArrayOf(-0.72f, -0.56f, -0.40f, 0.40f, 0.56f, 0.72f)) {
            m.addBox(tx, 0.68f, -2.36f, 0.09f, 0.18f, 0.06f, 1.0f, 0.08f, 0.10f, MaterialId.EMISSIVE)
        }
        return m.build()
    }

    private fun buildTaxi(): MeshData {
        val m = MeshBuilder(1300)
        // Iconic Puerto Vallarta yellow-and-white sedan taxi
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.18f, 0.20f, 0.48f, 0.74f, 0.88f, 0.82f, 0.90f, 0.92f, 0.82f),
            MeshBuilder.LoftStation(-1.28f, 0.18f, 0.48f, 0.74f, 0.90f, 0.86f, 0.92f, 0.92f, 0.84f),
            MeshBuilder.LoftStation(0.85f, 0.18f, 0.48f, 0.72f, 0.86f, 0.86f, 0.92f, 0.92f, 0.84f),
            MeshBuilder.LoftStation(1.85f, 0.20f, 0.44f, 0.64f, 0.76f, 0.84f, 0.90f, 0.88f, 0.78f),
            MeshBuilder.LoftStation(2.20f, 0.22f, 0.38f, 0.52f, 0.62f, 0.76f, 0.82f, 0.80f, 0.70f)
        )
        m.addSmoothLoftedBody(bodyStations, 0.98f, 0.76f, 0.06f, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.88f, wbZ = 1.34f, halfLen = 2.18f, sillY = 0.18f)

        val cabinStations = listOf(
            MeshBuilder.LoftStation(-1.32f, 0.88f, 1.10f, 1.28f, 1.36f, 0.80f, 0.76f, 0.70f, 0.62f),
            MeshBuilder.LoftStation(-0.62f, 0.88f, 1.18f, 1.34f, 1.42f, 0.82f, 0.78f, 0.72f, 0.64f),
            MeshBuilder.LoftStation(0.45f, 0.86f, 1.16f, 1.32f, 1.40f, 0.82f, 0.78f, 0.72f, 0.64f),
            MeshBuilder.LoftStation(1.08f, 0.84f, 0.90f, 0.94f, 0.98f, 0.80f, 0.76f, 0.70f, 0.62f)
        )
        m.addSmoothLoftedBody(cabinStations, 0.08f, 0.13f, 0.20f, MaterialId.GLASS)
        m.addBox(0f, 1.42f, -0.10f, 1.22f, 0.07f, 1.20f, 0.97f, 0.97f, 0.98f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 1.54f, 0.08f, 0.54f, 0.15f, 0.20f, 1.0f, 0.92f, 0.26f, MaterialId.EMISSIVE)
        m.addBox(-0.92f, 0.56f, -0.05f, 0.04f, 0.16f, 2.30f, 0.97f, 0.97f, 0.98f, MaterialId.GLOSSY_PAINT)
        m.addBox(0.92f, 0.56f, -0.05f, 0.04f, 0.16f, 2.30f, 0.97f, 0.97f, 0.98f, MaterialId.GLOSSY_PAINT)

        m.addBox(0f, 0.50f, 2.21f, 0.90f, 0.16f, 0.06f, 0.14f, 0.15f, 0.17f, MaterialId.CHROME)
        m.addBox(-0.60f, 0.54f, 2.18f, 0.28f, 0.14f, 0.10f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(0.60f, 0.54f, 2.18f, 0.28f, 0.14f, 0.10f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(-0.60f, 0.66f, -2.19f, 0.32f, 0.14f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(0.60f, 0.66f, -2.19f, 0.32f, 0.14f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        return m.build()
    }

    private fun buildPoliceInterceptor(): MeshData {
        val m = MeshBuilder(1400)
        // Policía Municipal de Puerto Vallarta tactical SUV
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.36f, 0.28f, 0.58f, 0.86f, 1.02f, 0.92f, 0.99f, 1.00f, 0.92f),
            MeshBuilder.LoftStation(-1.40f, 0.24f, 0.58f, 0.86f, 1.04f, 0.94f, 1.01f, 1.01f, 0.94f),
            MeshBuilder.LoftStation(0.95f, 0.24f, 0.58f, 0.84f, 1.02f, 0.94f, 1.01f, 1.00f, 0.92f),
            MeshBuilder.LoftStation(1.95f, 0.26f, 0.56f, 0.78f, 0.94f, 0.92f, 0.98f, 0.96f, 0.88f),
            MeshBuilder.LoftStation(2.40f, 0.28f, 0.48f, 0.66f, 0.80f, 0.86f, 0.92f, 0.90f, 0.80f)
        )
        m.addSmoothLoftedBody(bodyStations, 0.08f, 0.12f, 0.22f, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.96f, wbZ = 1.48f, halfLen = 2.36f, sillY = 0.24f)

        m.addBox(-1.01f, 0.68f, -0.10f, 0.05f, 0.52f, 2.02f, 0.96f, 0.97f, 0.99f, MaterialId.GLOSSY_PAINT)
        m.addBox(1.01f, 0.68f, -0.10f, 0.05f, 0.52f, 2.02f, 0.96f, 0.97f, 0.99f, MaterialId.GLOSSY_PAINT)

        val cabinStations = listOf(
            MeshBuilder.LoftStation(-2.14f, 1.02f, 1.32f, 1.54f, 1.66f, 0.90f, 0.86f, 0.82f, 0.74f),
            MeshBuilder.LoftStation(-1.00f, 1.04f, 1.36f, 1.58f, 1.70f, 0.92f, 0.88f, 0.84f, 0.76f),
            MeshBuilder.LoftStation(0.55f, 1.02f, 1.34f, 1.56f, 1.68f, 0.92f, 0.88f, 0.84f, 0.76f),
            MeshBuilder.LoftStation(1.22f, 0.98f, 1.04f, 1.08f, 1.12f, 0.88f, 0.84f, 0.80f, 0.72f)
        )
        m.addSmoothLoftedBody(cabinStations, 0.06f, 0.10f, 0.16f, MaterialId.GLASS)
        m.addBox(0f, 1.71f, -0.78f, 1.48f, 0.08f, 2.66f, 0.96f, 0.97f, 0.99f, MaterialId.GLOSSY_PAINT)

        // Glowing Red + Blue Police Roof Lightbar
        m.addBox(0f, 1.79f, 0.06f, 1.28f, 0.07f, 0.24f, 0.12f, 0.13f, 0.16f, MaterialId.MATTE)
        m.addBox(-0.40f, 1.85f, 0.06f, 0.46f, 0.10f, 0.20f, 1.0f, 0.08f, 0.16f, MaterialId.EMISSIVE)
        m.addBox(0.40f, 1.85f, 0.06f, 0.46f, 0.10f, 0.20f, 0.08f, 0.46f, 1.0f, MaterialId.EMISSIVE)
        m.addBox(0f, 1.85f, 0.06f, 0.24f, 0.09f, 0.20f, 1.0f, 1.0f, 1.0f, MaterialId.EMISSIVE)

        // Tactical steel bullbar
        m.addBox(0f, 0.50f, 2.50f, 1.36f, 0.09f, 0.09f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(0f, 0.78f, 2.48f, 1.22f, 0.08f, 0.08f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(-0.45f, 0.62f, 2.46f, 0.09f, 0.46f, 0.11f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)
        m.addBox(0.45f, 0.62f, 2.46f, 0.09f, 0.46f, 0.11f, 0.09f, 0.10f, 0.12f, MaterialId.MATTE)

        m.addBox(-0.68f, 0.66f, 2.38f, 0.34f, 0.14f, 0.10f, 1.0f, 0.98f, 0.90f, MaterialId.EMISSIVE)
        m.addBox(0.68f, 0.66f, 2.38f, 0.34f, 0.14f, 0.10f, 1.0f, 0.98f, 0.90f, MaterialId.EMISSIVE)
        m.addBox(-0.76f, 0.82f, -2.38f, 0.20f, 0.32f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(0.76f, 0.82f, -2.38f, 0.20f, 0.32f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        return m.build()
    }

    private fun buildSierraSuv(r: Float, g: Float, b: Float): MeshData {
        val m = MeshBuilder(1350)
        val bodyStations = listOf(
            MeshBuilder.LoftStation(-2.40f, 0.30f, 0.62f, 0.90f, 1.08f, 0.94f, 1.02f, 1.03f, 0.95f),
            MeshBuilder.LoftStation(1.05f, 0.28f, 0.62f, 0.90f, 1.08f, 0.96f, 1.03f, 1.03f, 0.95f),
            MeshBuilder.LoftStation(2.05f, 0.30f, 0.60f, 0.84f, 1.00f, 0.92f, 0.99f, 0.98f, 0.90f),
            MeshBuilder.LoftStation(2.44f, 0.32f, 0.52f, 0.72f, 0.88f, 0.86f, 0.92f, 0.90f, 0.82f)
        )
        m.addSmoothLoftedBody(bodyStations, r, g, b, MaterialId.GLOSSY_PAINT)
        addDarkUnderbodyAndWheelWells(m, halfW = 0.98f, wbZ = 1.50f, halfLen = 2.40f, sillY = 0.28f)

        val cabinStations = listOf(
            MeshBuilder.LoftStation(-2.18f, 1.08f, 1.42f, 1.66f, 1.78f, 0.92f, 0.88f, 0.84f, 0.76f),
            MeshBuilder.LoftStation(0.60f, 1.08f, 1.42f, 1.66f, 1.78f, 0.92f, 0.88f, 0.84f, 0.76f),
            MeshBuilder.LoftStation(1.24f, 1.02f, 1.08f, 1.12f, 1.16f, 0.88f, 0.86f, 0.82f, 0.74f)
        )
        m.addSmoothLoftedBody(cabinStations, 0.08f, 0.13f, 0.20f, MaterialId.GLASS)
        m.addBox(0f, 1.80f, -0.78f, 1.50f, 0.08f, 2.74f, r, g, b, MaterialId.GLOSSY_PAINT)
        // Surfboard on roof rack!
        m.addEllipsoid(0f, 1.96f, -0.60f, 0.34f, 0.055f, 1.28f, 0.06f, 0.82f, 0.82f, MaterialId.GLOSSY_PAINT, stacks = 6, slices = 10)
        m.addWheelTorus(0f, 1.00f, -2.54f, 0.29f, 0.11f, 0.10f, 0.10f, 0.12f, MaterialId.MATTE, majorSegs = 14, minorSegs = 7)
        m.addBox(-0.70f, 0.72f, 2.43f, 0.34f, 0.15f, 0.10f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(0.70f, 0.72f, 2.43f, 0.34f, 0.15f, 0.10f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(-0.80f, 0.88f, -2.42f, 0.20f, 0.32f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(0.80f, 0.88f, -2.42f, 0.20f, 0.32f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        return m.build()
    }

    private fun buildUrbanBus(): MeshData {
        val m = MeshBuilder(980)
        m.addBox(0f, 1.10f, 0f, 2.44f, 1.44f, 8.40f, 0.96f, 0.96f, 0.97f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 0.72f, 0f, 2.48f, 0.56f, 8.44f, 0.08f, 0.52f, 0.72f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 2.24f, 0.08f, 2.42f, 0.96f, 8.15f, 0.08f, 0.15f, 0.23f, MaterialId.GLASS)
        m.addBox(0f, 2.82f, 0f, 2.38f, 0.22f, 8.32f, 0.96f, 0.97f, 0.98f, MaterialId.GLOSSY_PAINT)
        m.addBox(0f, 3.00f, -0.80f, 1.56f, 0.18f, 2.20f, 0.88f, 0.90f, 0.92f, MaterialId.MATTE)
        m.addBox(0f, 2.70f, 4.18f, 1.48f, 0.24f, 0.08f, 1.0f, 0.76f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(-0.88f, 0.70f, 4.24f, 0.36f, 0.18f, 0.08f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(0.88f, 0.70f, 4.24f, 0.36f, 0.18f, 0.08f, 1.0f, 0.96f, 0.84f, MaterialId.EMISSIVE)
        m.addBox(-0.92f, 0.88f, -4.24f, 0.26f, 0.36f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        m.addBox(0.92f, 0.88f, -4.24f, 0.26f, 0.36f, 0.08f, 1.0f, 0.10f, 0.12f, MaterialId.EMISSIVE)
        return m.build()
    }
}
