package com.example.game.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.simulation.PedestrianArchetype
import com.example.game.simulation.ProjectedLandmarkPin
import com.example.game.simulation.VallartaGameEngine
import com.example.game.simulation.VehicleEntity
import com.example.game.world.CameraMode
import com.example.game.world.LandmarkType
import com.example.game.world.OsmVallartaRepository
import com.example.game.world.TimeOfDay
import com.example.game.world.VallartaGeography
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

private class GpuHumanoid(set: HumanoidMeshSet) {
    val head = GpuMesh(set.headAndNeck)
    val pelvis = GpuMesh(set.pelvisAndHips)
    val upperTorso = GpuMesh(set.upperTorso)
    val leftUpperArm = GpuMesh(set.leftUpperArm)
    val leftForearm = GpuMesh(set.leftForearmAndHand)
    val rightUpperArm = GpuMesh(set.rightUpperArm)
    val rightForearm = GpuMesh(set.rightForearmAndHand)
    val leftThigh = GpuMesh(set.leftThigh)
    val leftCalf = GpuMesh(set.leftCalfAndFoot)
    val rightThigh = GpuMesh(set.rightThigh)
    val rightCalf = GpuMesh(set.rightCalfAndFoot)
}

/**
 * Next-Gen OpenGL ES 3.0 1:1 Puerto Vallarta Pure-3D Engine Renderer.
 * - Pure-3D PBR terrain (golden beaches, wet tidal sand, emerald Marina Golf fairways,
 *   urban earth, and Sierra Madre Occidental foothills up to 5 km inland — NO blurry flat satellite layer).
 * - Renders ALL 3,867 real OpenStreetMap streets, river-stone "empedrados" (Centro/Zona Romántica/5 de Diciembre),
 *   Marina Vallarta seawalls/piers wrapping Hotel Flamingo, and beach-access alleys ("accesos a playa").
 * - Renders 7,609 real extruded 1:1 OSM footprints (multi-wing hotels with 3D floor-by-floor balconies,
 *   Hacienda cupolas, thatched palapas, colonial tile roofs, and inland Mexican casas) + 128 real pools.
 * - Renders curated sculptural landmarks (Parroquia, Muelle Los Muertos, El Caballito, Los Arcos, Faro Marina),
 *   authentic Río Cuale bridges, 4-wave Gerstner Pacific Ocean, smooth-lofted vehicles, and articulated humanoids.
 */
class VallartaGLRenderer(
    private val engine: VallartaGameEngine
) : GLSurfaceView.Renderer {

    private var worldProgram = 0
    private var oceanProgram = 0
    private var skyProgram = 0
    private var skyVao = 0

    // World uniform locations
    private var uWorldMvp = 0; private var uWorldModel = 0; private var uWorldTint = 0
    private var uWorldCamPos = 0; private var uWorldSunDir = 0; private var uWorldSunCol = 0
    private var uWorldSkyTop = 0; private var uWorldSkyHor = 0; private var uWorldNight = 0; private var uWorldTime = 0

    // World 2D texture sampler uniform locations (9 active procedural PBR textures!)
    private var uTexRoad = 0
    private var uTexMalecon = 0
    private var uTexResort = 0
    private var uTexCondo = 0
    private var uTexColonial = 0
    private var uTexTerrain = 0
    private var uTexEmpedrado = 0
    private var uTexSand = 0
    private var uTexGrass = 0

    // Ocean uniform locations
    private var uOceanMvp = 0; private var uOceanTime = 0; private var uOceanCamPos = 0
    private var uOceanSunDir = 0; private var uOceanSunCol = 0
    private var uOceanSkyTop = 0; private var uOceanSkyHor = 0; private var uOceanNight = 0

    // Sky uniform locations
    private var uSkyInvVp = 0; private var uSkySunDir = 0; private var uSkySunCol = 0
    private var uSkyTop = 0; private var uSkyMid = 0; private var uSkyHor = 0
    private var uSkyNight = 0; private var uSkyTime = 0

    private class SectorGpuEntry(val mesh: GpuMesh, val centerX: Float, val centerZ: Float, val hasCenter: Boolean)

    // Dedicated NEVER-EVICTED GPU lists with spatial sector centers for ultra-fast distance culling at 60fps!
    private val roadSectorMeshes = ArrayList<SectorGpuEntry>()
    private val buildingSectorMeshes = ArrayList<SectorGpuEntry>()
    private val decorSectorMeshes = ArrayList<SectorGpuEntry>()

    // Procedural mipmapped architectural/road/empedrado/sand/grass texture atlas set
    private lateinit var textures: VallartaTextureSet

    // Core 1:1 Pure-3D Terrain, Marina Seawalls/Docks & Ocean Meshes
    private lateinit var terrainMesh: GpuMesh
    private lateinit var roadsMesh: GpuMesh
    private lateinit var oceanMesh: GpuMesh
    private lateinit var wheelMesh: GpuMesh

    // 19 Distinct Bespoke Puerto Vallarta Beachfront & Marina Resorts + 2 Authentic Río Cuale Bridges
    private lateinit var velasMesh: GpuMesh
    private lateinit var marriottMesh: GpuMesh
    private lateinit var westinMesh: GpuMesh
    private lateinit var meliaMesh: GpuMesh
    private lateinit var crownParadiseMesh: GpuMesh
    private lateinit var krystalMesh: GpuMesh
    private lateinit var peninsulaMesh: GpuMesh
    private lateinit var grandVenetianMesh: GpuMesh
    private lateinit var fiestaMesh: GpuMesh
    private lateinit var iconMesh: GpuMesh
    private lateinit var secretsDreamsMesh: GpuMesh
    private lateinit var sheratonMesh: GpuMesh
    private lateinit var buenaventuraMesh: GpuMesh
    private lateinit var rositaMesh: GpuMesh
    private lateinit var hyattZivaMesh: GpuMesh
    private lateinit var garzaBlancaMesh: GpuMesh
    private lateinit var barceloMesh: GpuMesh
    private lateinit var beachOpenMesh: GpuMesh
    private lateinit var flamingoMesh: GpuMesh
    private lateinit var cualePedBridgeMesh: GpuMesh
    private lateinit var cualeVehBridgeMesh: GpuMesh

    // Iconic Vallarta Landmarks & Supporting Architecture Units
    private lateinit var hotelUnitMesh: GpuMesh
    private lateinit var condoUnitMesh: GpuMesh
    private lateinit var colonialUnitMesh: GpuMesh
    private lateinit var elCaballitoMesh: GpuMesh
    private lateinit var parroquiaMesh: GpuMesh
    private lateinit var muelleMesh: GpuMesh
    private lateinit var arcosMaleconMesh: GpuMesh
    private lateinit var faroMesh: GpuMesh
    private lateinit var cruiseShipMesh: GpuMesh
    private lateinit var yachtMesh: GpuMesh
    private lateinit var airplaneMesh: GpuMesh
    private lateinit var arcosRocksMesh: GpuMesh
    private lateinit var palmMesh: GpuMesh
    private lateinit var umbrellaMesh: GpuMesh
    private lateinit var streetlampMesh: GpuMesh
    private lateinit var beaconMesh: GpuMesh
    private lateinit var shadowDiscMesh: GpuMesh

    // Vehicle meshes indexed by (VehicleType, colorVariant)
    private val vehicleMeshes = HashMap<Pair<VehicleType, Int>, GpuMesh>()

    // 9 Articulated 10-bone high-poly humanoids (Protagonist + Street Locals + 4 Authentic Beachwear Styles + Policía)
    private lateinit var protagonistMesh: GpuHumanoid
    private lateinit var policeMesh: GpuHumanoid
    private lateinit var touristCoralMesh: GpuHumanoid
    private lateinit var localDressMesh: GpuHumanoid
    private lateinit var localGuayaberaMesh: GpuHumanoid
    private lateinit var beachSwimShortsMesh: GpuHumanoid
    private lateinit var beachBikiniSarongMesh: GpuHumanoid
    private lateinit var beachHawaiianMesh: GpuHumanoid
    private lateinit var beachSundressMesh: GpuHumanoid

    // Reusable matrices & vectors (zero per-frame GC allocation)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val invVpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val torsoMatrix = FloatArray(16)
    private val limbMatrix = FloatArray(16)
    private val childLimbMatrix = FloatArray(16)
    private val projWorldVec = FloatArray(4)
    private val projClipVec = FloatArray(4)

    // Smoothly damped spherical orbit camera state
    private var camX = 0f
    private var camY = 5f
    private var camZ = -30f
    private var lookX = 0f
    private var lookY = 2.5f
    private var lookZ = -18f
    private var currentFov = 58f
    private var viewportAspect = 1.6f
    private var lastFrameNanos = 0L
    private var pinProjectFrameCounter = 0

    private val carPaintPalette = arrayOf(
        floatArrayOf(0.92f, 0.24f, 0.12f), // Rosso Corsa / Sunset Vallarta Red-Orange
        floatArrayOf(0.05f, 0.72f, 0.78f), // Banderas Metallic Turquoise
        floatArrayOf(0.86f, 0.12f, 0.38f), // Malecón Ruby Magenta
        floatArrayOf(0.94f, 0.94f, 0.92f), // Pearl Coastal White
        floatArrayOf(0.94f, 0.72f, 0.14f), // Jalisco Metallic Gold
        floatArrayOf(0.14f, 0.36f, 0.82f)  // Pacific Cobalt Metallic
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glDisable(GLES30.GL_CULL_FACE)
        GLES30.glClearColor(0.06f, 0.09f, 0.16f, 1f)

        worldProgram = GlShaders.compileProgram(GlShaders.WORLD_VERT, GlShaders.WORLD_FRAG)
        oceanProgram = GlShaders.compileProgram(GlShaders.OCEAN_VERT, GlShaders.OCEAN_FRAG)
        skyProgram = GlShaders.compileProgram(GlShaders.SKY_VERT, GlShaders.SKY_FRAG)

        cacheUniforms()
        initSkyQuad()

        textures = VallartaTextureSet()
        roadSectorMeshes.clear()
        buildingSectorMeshes.clear()
        decorSectorMeshes.clear()
        uploadAllHighPolyMeshes()

        // Restore any previously loaded road/building/decor sectors cleanly after EGL context recreation!
        for (chunk in engine.allLoadedOsmChunks) {
            uploadSectorChunkToGpu(chunk)
        }

        // Initialize camera cleanly behind player
        val yaw = engine.cameraYaw
        val p = engine.player
        camX = p.x - sin(yaw) * 6.0f
        camY = p.y + 3.2f
        camZ = p.z - cos(yaw) * 6.0f
        lookX = p.x + sin(yaw) * 4.0f
        lookY = p.y + 1.6f
        lookZ = p.z + cos(yaw) * 4.0f
        lastFrameNanos = System.nanoTime()
    }

    private fun uploadSectorChunkToGpu(chunk: OsmSectorChunk) {
        if (chunk.meshData.indices.isEmpty()) return
        val gpu = GpuMesh(chunk.meshData)
        val rawKey = chunk.sectorKey.removePrefix("road_").removePrefix("decor_")
        val parts = rawKey.split('_')
        val sx = parts.getOrNull(0)?.toIntOrNull()
        val sz = parts.getOrNull(1)?.toIntOrNull()
        val hasCenter = sx != null && sz != null
        val cx = if (hasCenter) (sx!! + 0.5f) * OsmVallartaRepository.SECTOR_SIZE_M else 0f
        val cz = if (hasCenter) (sz!! + 0.5f) * OsmVallartaRepository.SECTOR_SIZE_M else 0f
        val entry = SectorGpuEntry(gpu, cx, cz, hasCenter)
        when {
            chunk.sectorKey.startsWith("road_") -> roadSectorMeshes.add(entry)
            chunk.sectorKey.startsWith("decor_") -> decorSectorMeshes.add(entry)
            else -> buildingSectorMeshes.add(entry)
        }
    }

    private fun cacheUniforms() {
        // World shader uniforms
        uWorldMvp = GLES30.glGetUniformLocation(worldProgram, "uMVP")
        uWorldModel = GLES30.glGetUniformLocation(worldProgram, "uModel")
        uWorldTint = GLES30.glGetUniformLocation(worldProgram, "uTint")
        uWorldCamPos = GLES30.glGetUniformLocation(worldProgram, "uCameraPos")
        uWorldSunDir = GLES30.glGetUniformLocation(worldProgram, "uSunDir")
        uWorldSunCol = GLES30.glGetUniformLocation(worldProgram, "uSunColor")
        uWorldSkyTop = GLES30.glGetUniformLocation(worldProgram, "uSkyTopColor")
        uWorldSkyHor = GLES30.glGetUniformLocation(worldProgram, "uSkyHorizonColor")
        uWorldNight = GLES30.glGetUniformLocation(worldProgram, "uNightFactor")
        uWorldTime = GLES30.glGetUniformLocation(worldProgram, "uTime")

        uTexRoad = GLES30.glGetUniformLocation(worldProgram, "uTexRoad")
        uTexMalecon = GLES30.glGetUniformLocation(worldProgram, "uTexMalecon")
        uTexResort = GLES30.glGetUniformLocation(worldProgram, "uTexResort")
        uTexCondo = GLES30.glGetUniformLocation(worldProgram, "uTexCondo")
        uTexColonial = GLES30.glGetUniformLocation(worldProgram, "uTexColonial")
        uTexTerrain = GLES30.glGetUniformLocation(worldProgram, "uTexTerrain")
        uTexEmpedrado = GLES30.glGetUniformLocation(worldProgram, "uTexEmpedrado")
        uTexSand = GLES30.glGetUniformLocation(worldProgram, "uTexSand")
        uTexGrass = GLES30.glGetUniformLocation(worldProgram, "uTexGrass")

        // Ocean shader uniforms
        uOceanMvp = GLES30.glGetUniformLocation(oceanProgram, "uMVP")
        uOceanTime = GLES30.glGetUniformLocation(oceanProgram, "uTime")
        uOceanCamPos = GLES30.glGetUniformLocation(oceanProgram, "uCameraPos")
        uOceanSunDir = GLES30.glGetUniformLocation(oceanProgram, "uSunDir")
        uOceanSunCol = GLES30.glGetUniformLocation(oceanProgram, "uSunColor")
        uOceanSkyTop = GLES30.glGetUniformLocation(oceanProgram, "uSkyTopColor")
        uOceanSkyHor = GLES30.glGetUniformLocation(oceanProgram, "uSkyHorizonColor")
        uOceanNight = GLES30.glGetUniformLocation(oceanProgram, "uNightFactor")

        // Sky shader uniforms
        uSkyInvVp = GLES30.glGetUniformLocation(skyProgram, "uInvViewProj")
        uSkySunDir = GLES30.glGetUniformLocation(skyProgram, "uSunDir")
        uSkySunCol = GLES30.glGetUniformLocation(skyProgram, "uSunColor")
        uSkyTop = GLES30.glGetUniformLocation(skyProgram, "uSkyTopColor")
        uSkyMid = GLES30.glGetUniformLocation(skyProgram, "uSkyMidColor")
        uSkyHor = GLES30.glGetUniformLocation(skyProgram, "uSkyHorizonColor")
        uSkyNight = GLES30.glGetUniformLocation(skyProgram, "uNightFactor")
        uSkyTime = GLES30.glGetUniformLocation(skyProgram, "uTime")
    }

    private fun initSkyQuad() {
        val quad = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val vaos = IntArray(1); val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        skyVao = vaos[0]
        GLES30.glBindVertexArray(skyVao)
        val buf = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(quad).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, quad.size * 4, buf, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun uploadAllHighPolyMeshes() {
        val (terrainData, roadsData, oceanData) = CoastalTerrainBuilder.buildWorldGeometry()
        terrainMesh = GpuMesh(terrainData)
        roadsMesh = GpuMesh(roadsData)
        oceanMesh = GpuMesh(oceanData)
        wheelMesh = GpuMesh(HighPolyVehicleMeshes.buildAlloyWheel())

        // 18 Distinct Bespoke Puerto Vallarta Beachfront Resorts (open to the beach!) & Río Cuale Bridges
        velasMesh = GpuMesh(BespokeVallartaResorts.buildVelasVallartaComplex())
        marriottMesh = GpuMesh(BespokeVallartaResorts.buildMarriottResortComplex())
        westinMesh = GpuMesh(BespokeVallartaResorts.buildWestinResortComplex())
        meliaMesh = GpuMesh(BespokeVallartaResorts.buildMeliaVallartaComplex())
        crownParadiseMesh = GpuMesh(BespokeVallartaResorts.buildCrownParadiseComplex())
        krystalMesh = GpuMesh(BespokeVallartaResorts.buildKrystalVillageComplex())
        peninsulaMesh = GpuMesh(BespokeVallartaResorts.buildPeninsulaTowersComplex())
        grandVenetianMesh = GpuMesh(BespokeVallartaResorts.buildGrandVenetianComplex())
        fiestaMesh = GpuMesh(BespokeVallartaResorts.buildFiestaAmericanaComplex())
        iconMesh = GpuMesh(BespokeVallartaResorts.buildIconVallartaComplex())
        secretsDreamsMesh = GpuMesh(BespokeVallartaResorts.buildSecretsDreamsComplex())
        sheratonMesh = GpuMesh(BespokeVallartaResorts.buildSheratonComplex())
        buenaventuraMesh = GpuMesh(BespokeVallartaResorts.buildBuenaventuraComplex())
        rositaMesh = GpuMesh(BespokeVallartaResorts.buildHotelRositaComplex())
        hyattZivaMesh = GpuMesh(BespokeVallartaResorts.buildHyattZivaCoveComplex())
        garzaBlancaMesh = GpuMesh(BespokeVallartaResorts.buildGarzaBlancaTerraceComplex())
        barceloMesh = GpuMesh(BespokeVallartaResorts.buildBarceloMismaloyaComplex())
        beachOpenMesh = GpuMesh(BespokeVallartaResorts.buildBeachOpenBoutiqueResort())
        flamingoMesh = GpuMesh(BespokeVallartaResorts.buildFlamingoVallartaComplex())
        cualePedBridgeMesh = GpuMesh(BespokeVallartaResorts.buildCualeCablePedestrianBridge())
        cualeVehBridgeMesh = GpuMesh(BespokeVallartaResorts.buildCualeVehicularBridge())

        // Supporting architecture & iconic sculptural landmarks
        hotelUnitMesh = GpuMesh(HighPolyLandmarkMeshes.buildLuxuryHotelUnit())
        condoUnitMesh = GpuMesh(HighPolyLandmarkMeshes.buildGlassCondoUnit())
        colonialUnitMesh = GpuMesh(HighPolyLandmarkMeshes.buildColonialVillaUnit())
        elCaballitoMesh = GpuMesh(HighPolyLandmarkMeshes.buildElCaballito())
        parroquiaMesh = GpuMesh(HighPolyLandmarkMeshes.buildParroquiaCrown())
        muelleMesh = GpuMesh(HighPolyLandmarkMeshes.buildMuelleLosMuertos())
        arcosMaleconMesh = GpuMesh(HighPolyLandmarkMeshes.buildLosArcosMalecon())
        faroMesh = GpuMesh(HighPolyLandmarkMeshes.buildFaroMarina())
        cruiseShipMesh = GpuMesh(HighPolyLandmarkMeshes.buildCruiseShip())
        yachtMesh = GpuMesh(HighPolyLandmarkMeshes.buildLuxuryYacht())
        airplaneMesh = GpuMesh(HighPolyLandmarkMeshes.buildPassengerJet())
        arcosRocksMesh = GpuMesh(HighPolyLandmarkMeshes.buildLosArcosRocks())
        palmMesh = GpuMesh(HighPolyLandmarkMeshes.buildPalmTree())
        umbrellaMesh = GpuMesh(HighPolyLandmarkMeshes.buildBeachUmbrella())
        streetlampMesh = GpuMesh(HighPolyLandmarkMeshes.buildStreetlamp())
        beaconMesh = GpuMesh(HighPolyLandmarkMeshes.buildMissionBeacon())

        // Soft dark contact shadow disc for vehicles & humanoids
        val sm = MeshBuilder(64)
        sm.addCylinderY(0f, 0.015f, 0.03f, 0f, 1.0f, 1.0f, 0.05f, 0.06f, 0.08f, MaterialId.MATTE, slices = 20, capBottom = false)
        shadowDiscMesh = GpuMesh(sm.build())

        // Build smooth-lofted high-poly vehicles for each class & metallic finish
        vehicleMeshes.clear()
        for (type in VehicleType.entries) {
            if (type == VehicleType.TAXI || type == VehicleType.POLICE || type == VehicleType.BUS) {
                val mesh = GpuMesh(HighPolyVehicleMeshes.buildVehicleBody(type, 1f, 1f, 1f))
                for (c in carPaintPalette.indices) vehicleMeshes[type to c] = mesh
            } else {
                for (c in carPaintPalette.indices) {
                    val col = carPaintPalette[c]
                    vehicleMeshes[type to c] = GpuMesh(HighPolyVehicleMeshes.buildVehicleBody(type, col[0], col[1], col[2]))
                }
            }
        }

        // Build all 9 articulated 10-bone high-poly humanoid rigs (including 4 authentic Puerto Vallarta beachwear outfits!)
        protagonistMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.PROTAGONIST))
        policeMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.POLICE_OFFICER))
        touristCoralMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.TOURIST_CORAL))
        localDressMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.LOCAL_DRESS))
        localGuayaberaMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.LOCAL_GUAYABERA))
        beachSwimShortsMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.BEACH_SWIM_SHORTS))
        beachBikiniSarongMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.BEACH_BIKINI_SARONG))
        beachHawaiianMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.BEACH_HAWAIIAN_SHIRT))
        beachSundressMesh = GpuHumanoid(HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.BEACH_SUNDRESS))
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewportAspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
    }

    private fun bindWorldTextures() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texRoad)
        GLES30.glUniform1i(uTexRoad, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texMalecon)
        GLES30.glUniform1i(uTexMalecon, 1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texResort)
        GLES30.glUniform1i(uTexResort, 2)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texCondo)
        GLES30.glUniform1i(uTexCondo, 3)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE4)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texColonial)
        GLES30.glUniform1i(uTexColonial, 4)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE5)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texTerrain)
        GLES30.glUniform1i(uTexTerrain, 5)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE6)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texEmpedrado)
        GLES30.glUniform1i(uTexEmpedrado, 6)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE7)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texSand)
        GLES30.glUniform1i(uTexSand, 7)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE8)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures.texGrass)
        GLES30.glUniform1i(uTexGrass, 8)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = now

        engine.update(dt)
        updateCamera(dt)

        // Upload up to 12 pre-baked/streamed 3D road/building/decor sector chunks per frame (NEVER evict roads!)
        var uploadsThisFrame = 0
        while (uploadsThisFrame < 12) {
            val nextChunk = engine.pendingOsmChunks.poll() ?: break
            if (nextChunk.meshData.indices.isNotEmpty()) {
                uploadSectorChunkToGpu(nextChunk)
                uploadsThisFrame++
            }
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val lighting = currentLighting()

        // 1. Dynamic Tropical Sky Dome
        drawSky(lighting)

        // 2. PBR-inspired World Geometry Pass (Pure-3D Terrain, Streets/Empedrados, Hotels, Shops, Palapas, Vegetation, Vehicles, Humanoids)
        GLES30.glUseProgram(worldProgram)
        bindWorldTextures()
        GLES30.glUniform3f(uWorldCamPos, camX, camY, camZ)
        GLES30.glUniform3f(uWorldSunDir, lighting.sunX, lighting.sunY, lighting.sunZ)
        GLES30.glUniform3f(uWorldSunCol, lighting.sunR, lighting.sunG, lighting.sunB)
        GLES30.glUniform3f(uWorldSkyTop, lighting.topR, lighting.topG, lighting.topB)
        GLES30.glUniform3f(uWorldSkyHor, lighting.horR, lighting.horG, lighting.horB)
        GLES30.glUniform1f(uWorldNight, lighting.nightFactor)
        GLES30.glUniform1f(uWorldTime, engine.worldTimeSec)

        // Pure-3D PBR Coastal Terrain (3-Biome Golden Beach Sand, Emerald Sierra Madre/Golf Grass & Urban Earth)
        Matrix.setIdentityM(modelMatrix, 0)
        drawMeshWithCurrentModel(terrainMesh, 1f, 1f, 1f)

        val px = engine.player.x
        val pz = engine.player.z
        val sectorDrawDist = 1350f

        // Core Malecón/Runway + ALL 3,857 Real OSM Streets, Empedrados, Marina Boardwalks & Beach Alleys (ZERO overlapping roads!)
        GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
        GLES30.glPolygonOffset(-2.5f, -4.0f)
        Matrix.setIdentityM(modelMatrix, 0)
        drawMeshWithCurrentModel(roadsMesh, 1f, 1f, 1f)
        for (entry in roadSectorMeshes) {
            if (entry.hasCenter && (abs(entry.centerX - px) > sectorDrawDist || abs(entry.centerZ - pz) > sectorDrawDist)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            drawMeshWithCurrentModel(entry.mesh, 1f, 1f, 1f)
        }
        GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)

        // Pre-baked & Streamed 15,161 Real 1:1 OSM & Urban 3D Hotels, Small Shops with Awnings, Beach Palapa Clubs, Casas & 128 Pools
        for (entry in buildingSectorMeshes) {
            if (entry.hasCenter && (abs(entry.centerX - px) > sectorDrawDist || abs(entry.centerZ - pz) > sectorDrawDist)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            drawMeshWithCurrentModel(entry.mesh, 1f, 1f, 1f)
        }

        // Batched 3D World Decoration & Lush Tropical Vegetation (Palms, Lifeguard Towers, Beach Umbrellas, Loungers, Pangas, Kiosks & Forest)
        for (entry in decorSectorMeshes) {
            if (entry.hasCenter && (abs(entry.centerX - px) > sectorDrawDist || abs(entry.centerZ - pz) > sectorDrawDist)) continue
            Matrix.setIdentityM(modelMatrix, 0)
            drawMeshWithCurrentModel(entry.mesh, 1f, 1f, 1f)
        }

        // Curated Real Puerto Vallarta Beachfront & Marina Resorts, Iconic Landmarks & Río Cuale Bridges
        val drawDist = 920f
        for (b in engine.buildings) {
            if (abs(b.z - pz) > drawDist || abs(b.x - px) > drawDist) continue
            // Hide procedural fill blocks once real 1:1 OSM extruded polygons load for this sector!
            if (b.isProceduralFill && OsmVallartaRepository.sectorKey(b.x, b.z) in engine.loadedOsmSectors) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, b.x, b.y, b.z)
            Matrix.rotateM(modelMatrix, 0, b.rotationDeg, 0f, 1f, 0f)
            when (b.type) {
                LandmarkType.RESORT_VELAS -> drawMeshWithCurrentModel(velasMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_MARRIOTT -> drawMeshWithCurrentModel(marriottMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_WESTIN -> drawMeshWithCurrentModel(westinMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_MELIA -> drawMeshWithCurrentModel(meliaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_CROWN_PARADISE -> drawMeshWithCurrentModel(crownParadiseMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_KRYSTAL -> drawMeshWithCurrentModel(krystalMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_PENINSULA -> drawMeshWithCurrentModel(peninsulaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_GRAND_VENETIAN -> drawMeshWithCurrentModel(grandVenetianMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_FIESTA_AMERICANA -> drawMeshWithCurrentModel(fiestaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_ICON -> drawMeshWithCurrentModel(iconMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_SECRETS_DREAMS -> drawMeshWithCurrentModel(secretsDreamsMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_SHERATON -> drawMeshWithCurrentModel(sheratonMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_BUENAVENTURA -> drawMeshWithCurrentModel(buenaventuraMesh, 1f, 1f, 1f)
                LandmarkType.HOTEL_ROSITA -> drawMeshWithCurrentModel(rositaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_HYATT_ZIVA -> drawMeshWithCurrentModel(hyattZivaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_GARZA_BLANCA -> drawMeshWithCurrentModel(garzaBlancaMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_BARCELO -> drawMeshWithCurrentModel(barceloMesh, 1f, 1f, 1f)
                LandmarkType.RESORT_BEACH_OPEN -> drawMeshWithCurrentModel(beachOpenMesh, b.colorR, b.colorG, b.colorB)
                LandmarkType.RESORT_FLAMINGO -> drawMeshWithCurrentModel(flamingoMesh, 1f, 1f, 1f)
                LandmarkType.BRIDGE_CUALE_PEDESTRIAN -> drawMeshWithCurrentModel(cualePedBridgeMesh, 1f, 1f, 1f)
                LandmarkType.BRIDGE_CUALE_VEHICULAR -> drawMeshWithCurrentModel(cualeVehBridgeMesh, 1f, 1f, 1f)
                LandmarkType.PARROQUIA_CROWN -> drawMeshWithCurrentModel(parroquiaMesh, 1f, 1f, 1f)
                LandmarkType.MUELLE_LOS_MUERTOS -> drawMeshWithCurrentModel(muelleMesh, 1f, 1f, 1f)
                LandmarkType.LOS_ARCOS_MALECON -> drawMeshWithCurrentModel(arcosMaleconMesh, 1f, 1f, 1f)
                LandmarkType.FARO_MARINA -> drawMeshWithCurrentModel(faroMesh, 1f, 1f, 1f)
                LandmarkType.EL_CABALLITO -> drawMeshWithCurrentModel(elCaballitoMesh, 1f, 1f, 1f)
                LandmarkType.CRUISE_SHIP -> {
                    val bob = sin(engine.worldTimeSec * 0.9f) * 0.35f
                    Matrix.translateM(modelMatrix, 0, 0f, bob, 0f)
                    drawMeshWithCurrentModel(cruiseShipMesh, 1f, 1f, 1f)
                }
                LandmarkType.YACHT -> {
                    val bob = sin(engine.worldTimeSec * 1.6f + b.z * 0.05f) * 0.25f
                    val roll = cos(engine.worldTimeSec * 1.2f + b.z * 0.03f) * 2.2f
                    Matrix.translateM(modelMatrix, 0, 0f, bob, 0f)
                    Matrix.rotateM(modelMatrix, 0, roll, 0f, 0f, 1f)
                    drawMeshWithCurrentModel(yachtMesh, 1f, 1f, 1f)
                }
                LandmarkType.AIRPLANE -> drawMeshWithCurrentModel(airplaneMesh, 1f, 1f, 1f)
                LandmarkType.LOS_ARCOS_ROCKS -> drawMeshWithCurrentModel(arcosRocksMesh, 1f, 1f, 1f)
                LandmarkType.BUILDING_CONDO, LandmarkType.AIRPORT_TERMINAL, LandmarkType.AIRPORT_TOWER -> {
                    Matrix.scaleM(modelMatrix, 0, b.width, b.height, b.depth)
                    drawMeshWithCurrentModel(condoUnitMesh, b.colorR, b.colorG, b.colorB)
                }
                LandmarkType.BUILDING_COLONIAL -> {
                    Matrix.scaleM(modelMatrix, 0, b.width, b.height, b.depth)
                    drawMeshWithCurrentModel(colonialUnitMesh, b.colorR, b.colorG, b.colorB)
                }
                else -> {
                    Matrix.scaleM(modelMatrix, 0, b.width, b.height, b.depth)
                    drawMeshWithCurrentModel(hotelUnitMesh, b.colorR, b.colorG, b.colorB)
                }
            }
        }

        // Tropical Coconut Palm Trees, Beach Umbrellas & Malecón Streetlamps
        val propDist = 380f
        for (p in engine.props) {
            if (abs(p.z - pz) > propDist || abs(p.x - px) > propDist) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.rotateM(modelMatrix, 0, p.rotationDeg, 0f, 1f, 0f)
            when (p.type) {
                LandmarkType.PALM_TREE -> {
                    val sway = sin(engine.worldTimeSec * 1.3f + p.z * 0.08f) * 2.1f
                    Matrix.rotateM(modelMatrix, 0, sway, 0f, 0f, 1f)
                    Matrix.scaleM(modelMatrix, 0, p.scale, p.scale, p.scale)
                    drawMeshWithCurrentModel(palmMesh, 1f, 1f, 1f)
                }
                LandmarkType.UMBRELLA -> {
                    Matrix.scaleM(modelMatrix, 0, p.scale, p.scale, p.scale)
                    drawMeshWithCurrentModel(umbrellaMesh, p.colorR, p.colorG, p.colorB)
                }
                LandmarkType.STREETLAMP -> drawMeshWithCurrentModel(streetlampMesh, 1f, 1f, 1f)
                else -> {}
            }
        }

        // High-Polygon Smooth-Lofted Vehicles + 4 Spinning/Steering Alloy Wheels
        for (v in engine.vehicles) {
            if (abs(v.z - pz) > 460f || abs(v.x - px) > 460f) continue
            drawVehicle(v)
        }

        // Articulated 10-Bone High-Poly Humanoid Pedestrians (Street Locals + Authentic Beachgoers!)
        for (ped in engine.pedestrians) {
            if (abs(ped.z - pz) > 220f || abs(ped.x - px) > 220f) continue
            val (rig, isFemale) = when (ped.archetype) {
                PedestrianArchetype.POLICE_OFFICER -> policeMesh to false
                PedestrianArchetype.TOURIST_CORAL -> touristCoralMesh to false
                PedestrianArchetype.LOCAL_DRESS -> localDressMesh to true
                PedestrianArchetype.LOCAL_GUAYABERA -> localGuayaberaMesh to false
                PedestrianArchetype.BEACH_SWIM_SHORTS -> beachSwimShortsMesh to false
                PedestrianArchetype.BEACH_BIKINI_SARONG -> beachBikiniSarongMesh to true
                PedestrianArchetype.BEACH_HAWAIIAN_SHIRT -> beachHawaiianMesh to false
                PedestrianArchetype.BEACH_SUNDRESS -> beachSundressMesh to true
            }
            drawArticulatedHumanoid(
                rig = rig,
                x = ped.x, y = ped.y, z = ped.z,
                headingRad = ped.headingRad,
                walkPhase = ped.walkPhase,
                speedNorm = (if (ped.panicTimer > 0f) 0.96f else 0.42f),
                punchProgress = 0f,
                punchComboIndex = 0,
                isSeated = false,
                isFemale = isFemale,
                staggerNorm = (ped.staggerTimer / 0.55f).coerceIn(0f, 1f)
            )
        }

        // Protagonist Humanoid (on foot OR seated low inside the open-top Convertible!)
        val playerVeh = engine.vehicles.firstOrNull { it.id == engine.player.vehicleId }
        if (playerVeh == null) {
            val punchDuration = 0.46f
            val punchNorm = if (engine.player.punchTimer > 0f) {
                (1f - (engine.player.punchTimer / punchDuration)).coerceIn(0f, 1f)
            } else 0f
            drawArticulatedHumanoid(
                rig = protagonistMesh,
                x = engine.player.x,
                y = engine.player.y,
                z = engine.player.z,
                headingRad = engine.player.headingRad,
                walkPhase = engine.player.walkPhase,
                speedNorm = (engine.player.speed / 9.8f).coerceIn(0f, 1f),
                punchProgress = punchNorm,
                punchComboIndex = engine.player.punchComboIndex,
                isSeated = false,
                isAirborne = !engine.player.isGrounded && !engine.player.isClimbing && !engine.player.isMantling,
                isClimbing = engine.player.isClimbing || engine.player.isMantling,
                climbPhase = engine.player.climbPhase,
                leanRollDeg = engine.player.leanRoll,
                landingSquash = engine.player.landingSquash
            )
        } else if (playerVeh.type.isConvertible) {
            val seatOffX = -0.38f * cos(playerVeh.headingRad) - 0.14f * sin(playerVeh.headingRad)
            val seatOffZ = 0.38f * sin(playerVeh.headingRad) - 0.14f * cos(playerVeh.headingRad)
            drawArticulatedHumanoid(
                rig = protagonistMesh,
                x = playerVeh.x + seatOffX,
                y = playerVeh.y,
                z = playerVeh.z + seatOffZ,
                headingRad = playerVeh.headingRad,
                walkPhase = 0f,
                speedNorm = 0f,
                punchProgress = 0f,
                punchComboIndex = 0,
                isSeated = true
            )
        }

        // Active 3D Glowing Mission Target Beacon Pillar
        val activeMission = engine.getActiveMissionState()
        if (activeMission != null && abs(activeMission.targetZ - pz) < 950f && abs(activeMission.targetX - px) < 950f) {
            val groundY = CoastalTerrainBuilder.surfaceHeightAt(activeMission.targetX, activeMission.targetZ)
            val pulse = 1.0f + sin(engine.worldTimeSec * 4.5f) * 0.12f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, activeMission.targetX, groundY, activeMission.targetZ)
            Matrix.rotateM(modelMatrix, 0, engine.worldTimeSec * 55f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, pulse, 1f, pulse)
            drawMeshWithCurrentModel(beaconMesh, 1f, 1f, 1f)
        }

        // 3. AAA 4-Wave Gerstner Pacific Ocean + Marina Basin + Marina Golf Lagoons + Río Cuale
        drawOcean(lighting)

        // 4. Project nearby real hotels & landmarks into 2D screen coordinates for sleek floating HUD pins!
        pinProjectFrameCounter++
        if (pinProjectFrameCounter % 2 == 0) {
            projectVisibleLandmarkPins()
        }
    }

    /**
     * Projects nearby real Puerto Vallarta hotels, resorts, bridges, and OSM places into normalized
     * 2D screen coordinates for sleek, non-overlapping Google-Maps-3D / GTA architectural glass pins!
     */
    private fun projectVisibleLandmarkPins() {
        val px = engine.player.x
        val pz = engine.player.z
        val maxDist = 560f
        val candidates = ArrayList<ProjectedLandmarkPin>(16)

        fun tryProject(id: String, name: String, wx: Float, wy: Float, wz: Float, isIconic: Boolean) {
            val d = hypot(wx - px, wz - pz)
            if (d < 10f || d > maxDist) return
            projWorldVec[0] = wx
            projWorldVec[1] = wy
            projWorldVec[2] = wz
            projWorldVec[3] = 1f
            Matrix.multiplyMV(projClipVec, 0, vpMatrix, 0, projWorldVec, 0)
            val w = projClipVec[3]
            if (w <= 1.0f) return
            val ndcX = projClipVec[0] / w
            val ndcY = projClipVec[1] / w
            if (ndcX < -0.84f || ndcX > 0.84f || ndcY < -0.45f || ndcY > 0.78f) return
            val sx = (ndcX + 1f) * 0.5f
            val sy = (1f - ndcY) * 0.5f
            candidates += ProjectedLandmarkPin(
                id = id,
                name = name,
                screenXNorm = sx,
                screenYNorm = sy,
                distanceMeters = d.toInt(),
                isIconic = isIconic
            )
        }

        for (b in engine.buildings) {
            if (!b.showPin) continue
            val pinY = b.y + b.height.coerceAtLeast(10f) + 5.5f
            val iconic = b.type == LandmarkType.PARROQUIA_CROWN ||
                b.type == LandmarkType.MUELLE_LOS_MUERTOS ||
                b.type == LandmarkType.LOS_ARCOS_MALECON ||
                b.type == LandmarkType.FARO_MARINA ||
                b.type == LandmarkType.EL_CABALLITO ||
                b.type == LandmarkType.BRIDGE_CUALE_PEDESTRIAN ||
                b.type == LandmarkType.BRIDGE_CUALE_VEHICULAR ||
                b.type == LandmarkType.AIRPORT_TERMINAL
            tryProject(b.id, b.name, b.x, pinY, b.z, iconic)
        }

        for (p in engine.osmNamedPlaces) {
            tryProject("osm_${p.name}_${p.x.toInt()}", p.name, p.x, p.y + 4.0f, p.z, false)
        }

        candidates.sortWith(compareByDescending<ProjectedLandmarkPin> { it.isIconic && it.distanceMeters < 220 }.thenBy { it.distanceMeters })

        val kept = ArrayList<ProjectedLandmarkPin>(5)
        for (c in candidates) {
            if (kept.size >= 5) break
            val overlaps = kept.any { existing ->
                abs(existing.screenXNorm - c.screenXNorm) < 0.16f &&
                    abs(existing.screenYNorm - c.screenYNorm) < 0.085f
            }
            if (!overlaps) {
                kept += c
            }
        }
        engine.projectedPins = kept
    }

    private fun drawVehicle(v: VehicleEntity) {
        val deg = Math.toDegrees(v.headingRad.toDouble()).toFloat()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, v.x, v.y + 0.01f, v.z)
        Matrix.rotateM(modelMatrix, 0, deg, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, v.type.width * 0.56f, 1f, v.type.length * 0.53f)
        drawMeshWithCurrentModel(shadowDiscMesh, 1f, 1f, 1f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, v.x, v.y, v.z)
        Matrix.rotateM(modelMatrix, 0, deg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.bodyPitchRad.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.bodyRollRad.toDouble()).toFloat(), 0f, 0f, 1f)

        val bodyMesh = vehicleMeshes[v.type to (v.colorIndex % carPaintPalette.size)] ?: return
        drawMeshWithCurrentModel(bodyMesh, 1f, 1f, 1f)

        if (v.type.isPolice && (v.isPoliceChasing || engine.player.wantedStars > 0)) {
            val redPhase = sin(v.sirenPhase) > 0f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, v.x, v.y + 1.84f, v.z)
            Matrix.rotateM(modelMatrix, 0, deg, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, if (redPhase) -0.42f else 0.42f, 0f, 0.05f)
            Matrix.scaleM(modelMatrix, 0, 0.16f, 0.04f, 0.16f)
            drawMeshWithCurrentModel(
                beaconMesh,
                if (redPhase) 1.0f else 0.10f,
                if (redPhase) 0.10f else 0.55f,
                if (redPhase) 0.18f else 1.0f
            )
        }

        val wx = v.type.trackHalfX
        val wz = v.type.wheelBaseZ
        val r = v.type.wheelRadius
        val spinDeg = Math.toDegrees(v.wheelSpinRad.toDouble()).toFloat()
        val steerDeg = Math.toDegrees(v.steerAngle.toDouble()).toFloat()

        val wheelPositions = arrayOf(
            floatArrayOf(-wx, r, wz, 1f),
            floatArrayOf(wx, r, wz, 1f),
            floatArrayOf(-wx, r, -wz, 0f),
            floatArrayOf(wx, r, -wz, 0f)
        )
        for (wp in wheelPositions) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, v.x, v.y, v.z)
            Matrix.rotateM(modelMatrix, 0, deg, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, wp[0], wp[1], wp[2])
            if (wp[3] > 0.5f) {
                Matrix.rotateM(modelMatrix, 0, steerDeg, 0f, 1f, 0f)
            }
            Matrix.rotateM(modelMatrix, 0, spinDeg, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, r, r, r)
            drawMeshWithCurrentModel(wheelMesh, 1f, 1f, 1f)
        }
    }

    private fun drawArticulatedHumanoid(
        rig: GpuHumanoid,
        x: Float,
        y: Float,
        z: Float,
        headingRad: Float,
        walkPhase: Float,
        speedNorm: Float,
        punchProgress: Float,
        punchComboIndex: Int = 0,
        isSeated: Boolean,
        isAirborne: Boolean = false,
        isClimbing: Boolean = false,
        climbPhase: Float = 0f,
        isFemale: Boolean = false,
        leanRollDeg: Float = 0f,
        landingSquash: Float = 0f,
        staggerNorm: Float = 0f
    ) {
        val deg = Math.toDegrees(headingRad.toDouble()).toFloat()
        val shoulderX = if (isFemale) 0.212f else 0.252f

        if (!isSeated && !isClimbing && !isAirborne) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y + 0.02f, z)
            Matrix.scaleM(modelMatrix, 0, 0.44f, 1f, 0.44f)
            drawMeshWithCurrentModel(shadowDiscMesh, 1f, 1f, 1f)
        }

        // Smooth biomechanical 3-phase punch envelope: 0..0.24 coil wind-up, 0.24..0.52 explosive strike, 0.52..1.0 cubic recovery!
        val isPunching = punchProgress > 0.001f && !isSeated && !isClimbing
        var punchCoil = 0f
        var punchThrust = 0f
        if (isPunching) {
            when {
                punchProgress < 0.24f -> {
                    val t = punchProgress / 0.24f
                    punchCoil = sin(t * PI * 0.5).toFloat()
                }
                punchProgress < 0.52f -> {
                    val t = (punchProgress - 0.24f) / 0.28f
                    val snap = 1f - (1f - t) * (1f - t) * (1f - t)
                    punchCoil = (1f - snap) * 0.45f
                    punchThrust = snap
                }
                else -> {
                    val t = (punchProgress - 0.52f) / 0.48f
                    val ease = 1f - (t * t * (3f - 2f * t))
                    punchThrust = ease
                }
            }
        }
        val isLeftJab = (punchComboIndex % 3) == 0
        val isRightHook = (punchComboIndex % 3) == 2
        val strikeSign = if (isLeftJab) -1f else 1f

        val moving = !isSeated && !isClimbing && !isAirborne && staggerNorm <= 0.01f
        val strideSin = if (moving) sin(walkPhase) else 0f
        val strideCos = if (moving) cos(walkPhase) else 0f
        val idleBreath = sin(engine.worldTimeSec * 2.4f + x * 0.7f) * 0.012f * (1f - speedNorm)

        val strideSwing = strideSin * (38f * speedNorm)
        val bobY = if (moving) (abs(strideSin) * 0.068f * speedNorm) else 0f
        val punchDip = if (isPunching) (punchCoil * 0.05f + punchThrust * 0.07f) else 0f
        val squashDip = landingSquash * 0.14f
        val hipBaseY = if (isSeated) y + 0.42f else y + 0.90f + bobY + idleBreath - punchDip - squashDip

        val pelvisYaw = when {
            isPunching -> strikeSign * (-punchCoil * 16f + punchThrust * 24f)
            moving -> -strideSin * 9f * speedNorm
            else -> 0f
        }
        val pelvisPitch = when {
            staggerNorm > 0.01f -> -staggerNorm * 24f
            isPunching -> punchThrust * 10f
            isSeated -> -8f
            isClimbing -> 10f
            isAirborne -> 8f
            else -> speedNorm * 6.5f + landingSquash * 16f
        }

        // 1. Pelvis & Hips (Root bone)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, hipBaseY, z)
        Matrix.rotateM(modelMatrix, 0, deg + pelvisYaw, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, pelvisPitch, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, leanRollDeg, 0f, 0f, 1f)
        if (isSeated) {
            Matrix.scaleM(modelMatrix, 0, 0.94f, 0.94f, 0.94f)
        }
        drawMeshWithCurrentModel(rig.pelvis, 1f, 1f, 1f)

        // 2. Upper Torso (Independent waist twist + boxing torque + athletic forward lean!)
        System.arraycopy(modelMatrix, 0, torsoMatrix, 0, 16)
        Matrix.translateM(torsoMatrix, 0, 0f, 0.08f, 0f)
        val torsoTwistYaw = when {
            isPunching -> strikeSign * (-punchCoil * 26f + punchThrust * (if (isRightHook) 52f else 42f))
            moving -> strideSin * 14f * speedNorm
            else -> 0f
        }
        val torsoLeanPitch = when {
            staggerNorm > 0.01f -> -staggerNorm * 18f
            isPunching -> -punchCoil * 6f + punchThrust * 14f
            isClimbing -> 8f
            isAirborne -> 10f
            else -> speedNorm * 5.5f + landingSquash * 12f
        }
        Matrix.rotateM(torsoMatrix, 0, torsoTwistYaw, 0f, 1f, 0f)
        Matrix.rotateM(torsoMatrix, 0, torsoLeanPitch, 1f, 0f, 0f)
        drawMeshWithMatrix(rig.upperTorso, torsoMatrix)

        // 3. Sculpted Head & Neck (Tracks target naturally during boxing strikes!)
        System.arraycopy(torsoMatrix, 0, limbMatrix, 0, 16)
        Matrix.translateM(limbMatrix, 0, 0f, 0.50f, 0.01f)
        val headCounterYaw = when {
            isPunching -> -(pelvisYaw + torsoTwistYaw) * 0.72f
            moving -> -strideSin * 6f * speedNorm
            else -> 0f
        }
        val headPitch = when {
            staggerNorm > 0.01f -> -staggerNorm * 22f
            isClimbing -> -26f
            isPunching -> -8f * punchThrust
            else -> 0f
        }
        Matrix.rotateM(limbMatrix, 0, headCounterYaw, 0f, 1f, 0f)
        Matrix.rotateM(limbMatrix, 0, headPitch, 1f, 0f, 0f)
        drawMeshWithMatrix(rig.head, limbMatrix)

        // 4. Articulated Left Arm (Shoulder + Elbow joints!)
        val climbWave = sin(climbPhase)
        val lShoulderPitch: Float
        val lShoulderRoll: Float
        val lShoulderYaw: Float
        val lElbowBend: Float
        when {
            staggerNorm > 0.01f -> {
                lShoulderPitch = -75f * staggerNorm; lShoulderRoll = -35f * staggerNorm; lShoulderYaw = 0f; lElbowBend = -45f * staggerNorm
            }
            isSeated -> {
                lShoulderPitch = -48f; lShoulderRoll = 10f; lShoulderYaw = 12f; lElbowBend = -38f
            }
            isClimbing -> {
                lShoulderPitch = -132f + climbWave * 36f; lShoulderRoll = -8f; lShoulderYaw = 0f; lElbowBend = -48f - climbWave * 28f
            }
            isAirborne -> {
                lShoulderPitch = -92f; lShoulderRoll = -24f; lShoulderYaw = 0f; lElbowBend = -46f
            }
            isPunching && isLeftJab -> {
                lShoulderPitch = -42f * punchCoil - 94f * punchThrust
                lShoulderRoll = -10f - 8f * punchCoil + 6f * punchThrust
                lShoulderYaw = 14f * punchThrust
                lElbowBend = -106f * punchCoil - 12f * punchThrust - 22f * (1f - punchThrust)
            }
            isPunching && !isLeftJab -> {
                lShoulderPitch = -56f * (punchCoil + punchThrust).coerceAtMost(1f)
                lShoulderRoll = 12f * punchThrust
                lShoulderYaw = 24f * punchThrust
                lElbowBend = -112f * (punchCoil + punchThrust).coerceAtMost(1f)
            }
            else -> {
                lShoulderPitch = strideSwing * 0.82f
                lShoulderRoll = -5f - speedNorm * 4f
                lShoulderYaw = 0f
                lElbowBend = -(16f + speedNorm * 46f + max(0f, -strideSin) * 24f * speedNorm)
            }
        }
        System.arraycopy(torsoMatrix, 0, limbMatrix, 0, 16)
        Matrix.translateM(limbMatrix, 0, -shoulderX, 0.42f, 0f)
        Matrix.rotateM(limbMatrix, 0, lShoulderPitch, 1f, 0f, 0f)
        Matrix.rotateM(limbMatrix, 0, lShoulderYaw, 0f, 1f, 0f)
        Matrix.rotateM(limbMatrix, 0, lShoulderRoll, 0f, 0f, 1f)
        drawMeshWithMatrix(rig.leftUpperArm, limbMatrix)

        System.arraycopy(limbMatrix, 0, childLimbMatrix, 0, 16)
        Matrix.translateM(childLimbMatrix, 0, -0.012f, -0.29f, 0.010f)
        Matrix.rotateM(childLimbMatrix, 0, lElbowBend, 1f, 0f, 0f)
        drawMeshWithMatrix(rig.leftForearm, childLimbMatrix)

        // 5. Articulated Right Arm (Shoulder + Elbow joints!)
        val rShoulderPitch: Float
        val rShoulderRoll: Float
        val rShoulderYaw: Float
        val rElbowBend: Float
        when {
            staggerNorm > 0.01f -> {
                rShoulderPitch = -88f * staggerNorm; rShoulderRoll = 38f * staggerNorm; rShoulderYaw = 0f; rElbowBend = -55f * staggerNorm
            }
            isSeated -> {
                rShoulderPitch = -48f; rShoulderRoll = -10f; rShoulderYaw = -12f; rElbowBend = -38f
            }
            isClimbing -> {
                rShoulderPitch = -132f - climbWave * 36f; rShoulderRoll = 8f; rShoulderYaw = 0f; rElbowBend = -48f + climbWave * 28f
            }
            isAirborne -> {
                rShoulderPitch = -78f; rShoulderRoll = 22f; rShoulderYaw = 0f; rElbowBend = -54f
            }
            isPunching && !isLeftJab && !isRightHook -> {
                rShoulderPitch = 24f * punchCoil - 96f * punchThrust
                rShoulderRoll = 18f * punchCoil - 6f * punchThrust
                rShoulderYaw = -22f * punchThrust
                rElbowBend = -108f * punchCoil - 8f * punchThrust - 18f * (1f - punchThrust)
            }
            isPunching && isRightHook -> {
                rShoulderPitch = -64f * punchCoil - 84f * punchThrust
                rShoulderRoll = 54f * punchCoil + 68f * punchThrust
                rShoulderYaw = -34f * punchCoil + 46f * punchThrust
                rElbowBend = -94f * (punchCoil + punchThrust).coerceAtMost(1f)
            }
            isPunching && isLeftJab -> {
                rShoulderPitch = -54f * (punchCoil + punchThrust).coerceAtMost(1f)
                rShoulderRoll = -12f * punchThrust
                rShoulderYaw = -22f * punchThrust
                rElbowBend = -110f * (punchCoil + punchThrust).coerceAtMost(1f)
            }
            else -> {
                rShoulderPitch = -strideSwing * 0.82f
                rShoulderRoll = 5f + speedNorm * 4f
                rShoulderYaw = 0f
                rElbowBend = -(16f + speedNorm * 46f + max(0f, strideSin) * 24f * speedNorm)
            }
        }
        System.arraycopy(torsoMatrix, 0, limbMatrix, 0, 16)
        Matrix.translateM(limbMatrix, 0, shoulderX, 0.42f, 0f)
        Matrix.rotateM(limbMatrix, 0, rShoulderPitch, 1f, 0f, 0f)
        Matrix.rotateM(limbMatrix, 0, rShoulderYaw, 0f, 1f, 0f)
        Matrix.rotateM(limbMatrix, 0, rShoulderRoll, 0f, 0f, 1f)
        drawMeshWithMatrix(rig.rightUpperArm, limbMatrix)

        System.arraycopy(limbMatrix, 0, childLimbMatrix, 0, 16)
        Matrix.translateM(childLimbMatrix, 0, 0.012f, -0.29f, 0.010f)
        Matrix.rotateM(childLimbMatrix, 0, rElbowBend, 1f, 0f, 0f)
        drawMeshWithMatrix(rig.rightForearm, childLimbMatrix)

        // 6 & 7. Articulated Left & Right Legs (Hip + Knee joints with lifelike stride fold & combat lunge!)
        if (!isSeated) {
            val lThighPitch: Float
            val lKneeBend: Float
            val rThighPitch: Float
            val rKneeBend: Float
            when {
                isClimbing -> {
                    lThighPitch = -56f - climbWave * 30f
                    lKneeBend = 64f + climbWave * 28f
                    rThighPitch = -56f + climbWave * 30f
                    rKneeBend = 64f - climbWave * 28f
                }
                isAirborne -> {
                    lThighPitch = -54f; lKneeBend = 72f
                    rThighPitch = 18f; rKneeBend = 42f
                }
                isPunching -> {
                    lThighPitch = -24f * punchThrust - 10f * punchCoil
                    lKneeBend = 30f * punchThrust + 14f * punchCoil
                    rThighPitch = 22f * punchThrust + 8f * punchCoil
                    rKneeBend = 18f * punchThrust + 12f * punchCoil
                }
                staggerNorm > 0.01f -> {
                    lThighPitch = -16f * staggerNorm; lKneeBend = 24f * staggerNorm
                    rThighPitch = 22f * staggerNorm; rKneeBend = 18f * staggerNorm
                }
                else -> {
                    val squashKnee = landingSquash * 58f
                    val squashHip = -landingSquash * 32f
                    lThighPitch = -strideSwing + squashHip
                    rThighPitch = strideSwing + squashHip
                    lKneeBend = (max(0f, strideCos) * 56f * speedNorm + max(0f, -strideSin) * 18f * speedNorm + squashKnee).coerceIn(0f, 92f)
                    rKneeBend = (max(0f, -strideCos) * 56f * speedNorm + max(0f, strideSin) * 18f * speedNorm + squashKnee).coerceIn(0f, 92f)
                }
            }

            // Left Leg (Thigh -> Knee -> Calf/Foot)
            Matrix.setIdentityM(limbMatrix, 0)
            Matrix.translateM(limbMatrix, 0, x, hipBaseY, z)
            Matrix.rotateM(limbMatrix, 0, deg + pelvisYaw * 0.55f, 0f, 1f, 0f)
            Matrix.translateM(limbMatrix, 0, -0.104f, 0f, 0f)
            Matrix.rotateM(limbMatrix, 0, lThighPitch, 1f, 0f, 0f)
            drawMeshWithMatrix(rig.leftThigh, limbMatrix)

            System.arraycopy(limbMatrix, 0, childLimbMatrix, 0, 16)
            Matrix.translateM(childLimbMatrix, 0, 0f, -0.43f, 0.008f)
            Matrix.rotateM(childLimbMatrix, 0, lKneeBend, 1f, 0f, 0f)
            drawMeshWithMatrix(rig.leftCalf, childLimbMatrix)

            // Right Leg (Thigh -> Knee -> Calf/Foot)
            Matrix.setIdentityM(limbMatrix, 0)
            Matrix.translateM(limbMatrix, 0, x, hipBaseY, z)
            Matrix.rotateM(limbMatrix, 0, deg + pelvisYaw * 0.55f, 0f, 1f, 0f)
            Matrix.translateM(limbMatrix, 0, 0.104f, 0f, 0f)
            Matrix.rotateM(limbMatrix, 0, rThighPitch, 1f, 0f, 0f)
            drawMeshWithMatrix(rig.rightThigh, limbMatrix)

            System.arraycopy(limbMatrix, 0, childLimbMatrix, 0, 16)
            Matrix.translateM(childLimbMatrix, 0, 0f, -0.43f, 0.008f)
            Matrix.rotateM(childLimbMatrix, 0, rKneeBend, 1f, 0f, 0f)
            drawMeshWithMatrix(rig.rightCalf, childLimbMatrix)
        }
    }

    private fun drawMeshWithCurrentModel(mesh: GpuMesh, tintR: Float, tintG: Float, tintB: Float) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uWorldMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uWorldModel, 1, false, modelMatrix, 0)
        GLES30.glUniform3f(uWorldTint, tintR, tintG, tintB)
        mesh.draw()
    }

    private fun drawMeshWithMatrix(mesh: GpuMesh, mat: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, mat, 0)
        GLES30.glUniformMatrix4fv(uWorldMvp, 1, false, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(uWorldModel, 1, false, mat, 0)
        GLES30.glUniform3f(uWorldTint, 1f, 1f, 1f)
        mesh.draw()
    }

    private fun drawSky(l: LightingParams) {
        GLES30.glDepthMask(false)
        GLES30.glUseProgram(skyProgram)
        Matrix.invertM(invVpMatrix, 0, vpMatrix, 0)
        GLES30.glUniformMatrix4fv(uSkyInvVp, 1, false, invVpMatrix, 0)
        GLES30.glUniform3f(uSkySunDir, l.sunX, l.sunY, l.sunZ)
        GLES30.glUniform3f(uSkySunCol, l.sunR, l.sunG, l.sunB)
        GLES30.glUniform3f(uSkyTop, l.topR, l.topG, l.topB)
        GLES30.glUniform3f(uSkyMid, l.midR, l.midG, l.midB)
        GLES30.glUniform3f(uSkyHor, l.horR, l.horG, l.horB)
        GLES30.glUniform1f(uSkyNight, l.nightFactor)
        GLES30.glUniform1f(uSkyTime, engine.worldTimeSec)
        GLES30.glBindVertexArray(skyVao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDepthMask(true)
    }

    private fun drawOcean(l: LightingParams) {
        GLES30.glUseProgram(oceanProgram)
        GLES30.glUniformMatrix4fv(uOceanMvp, 1, false, vpMatrix, 0)
        GLES30.glUniform1f(uOceanTime, engine.worldTimeSec)
        GLES30.glUniform3f(uOceanCamPos, camX, camY, camZ)
        GLES30.glUniform3f(uOceanSunDir, l.sunX, l.sunY, l.sunZ)
        GLES30.glUniform3f(uOceanSunCol, l.sunR, l.sunG, l.sunB)
        GLES30.glUniform3f(uOceanSkyTop, l.topR, l.topG, l.topB)
        GLES30.glUniform3f(uOceanSkyHor, l.horR, l.horG, l.horB)
        GLES30.glUniform1f(uOceanNight, l.nightFactor)
        oceanMesh.draw()
    }

    private fun updateCamera(dt: Float) {
        val p = engine.player
        val inCar = p.vehicleId != null
        val yaw = engine.cameraYaw
        val pitch = engine.cameraPitch
        val isHood = engine.cameraMode == CameraMode.HOOD && inCar

        val desiredCamX: Float
        val desiredCamY: Float
        val desiredCamZ: Float
        val desiredLookX: Float
        val desiredLookY: Float
        val desiredLookZ: Float
        val targetFov: Float

        if (isHood) {
            val h = p.headingRad
            desiredCamX = p.x + sin(h) * 0.55f
            desiredCamZ = p.z + cos(h) * 0.55f
            desiredCamY = p.y + 1.42f
            desiredLookX = p.x + sin(h) * 24.0f
            desiredLookZ = p.z + cos(h) * 24.0f
            desiredLookY = p.y + 1.32f - sin(pitch - 0.18f) * 6.0f
            targetFov = 68f + (p.speed * 0.22f).coerceAtMost(12f)
        } else {
            val (dist, pivotHeight, lookAhead, fov) = when (engine.cameraMode) {
                CameraMode.CHASE -> if (inCar) {
                    CamSpec(7.6f, 1.48f, 5.4f, 58f + (p.speed * 0.24f).coerceAtMost(13f))
                } else {
                    CamSpec(4.9f, 1.56f, 2.8f, 56f)
                }
                CameraMode.WIDE -> if (inCar) {
                    CamSpec(12.8f, 2.05f, 8.8f, 62f + (p.speed * 0.20f).coerceAtMost(11f))
                } else {
                    CamSpec(8.2f, 1.88f, 4.4f, 60f)
                }
                CameraMode.HOOD -> {
                    CamSpec(3.3f, 1.62f, 4.5f, 60f)
                }
            }

            val horizDist = dist * cos(pitch)
            val vertOffset = dist * sin(pitch)
            desiredCamX = p.x - sin(yaw) * horizDist
            desiredCamZ = p.z - cos(yaw) * horizDist
            val groundAtCam = CoastalTerrainBuilder.surfaceHeightAt(desiredCamX, desiredCamZ)
            desiredCamY = max(p.y + pivotHeight + vertOffset, groundAtCam + 0.95f)

            desiredLookX = p.x + sin(yaw) * lookAhead
            desiredLookZ = p.z + cos(yaw) * lookAhead
            desiredLookY = p.y + pivotHeight + 0.12f - sin(pitch) * 1.45f
            targetFov = fov
        }

        val jumpDist = hypot(desiredCamX - camX, desiredCamZ - camZ)
        if (jumpDist > 35f) {
            camX = desiredCamX; camY = desiredCamY; camZ = desiredCamZ
            lookX = desiredLookX; lookY = desiredLookY; lookZ = desiredLookZ
            currentFov = targetFov
        } else {
            val posAlpha = 1f - exp(-(if (isHood) 24f else 11.5f) * dt)
            val lookAlpha = 1f - exp(-(if (isHood) 28f else 16.0f) * dt)
            val fovAlpha = 1f - exp(-6.0f * dt)
            camX += (desiredCamX - camX) * posAlpha
            camY += (desiredCamY - camY) * posAlpha
            camZ += (desiredCamZ - camZ) * posAlpha
            lookX += (desiredLookX - lookX) * lookAlpha
            lookY += (desiredLookY - lookY) * lookAlpha
            lookZ += (desiredLookZ - lookZ) * lookAlpha
            currentFov += (targetFov - currentFov) * fovAlpha
        }

        val shake = p.cameraPunchShake
        val shakeX = if (shake > 0.001f) sin(engine.worldTimeSec * 58f) * shake * 0.14f else 0f
        val shakeY = if (shake > 0.001f) cos(engine.worldTimeSec * 71f) * shake * 0.10f else 0f
        Matrix.perspectiveM(projMatrix, 0, currentFov, viewportAspect, 0.35f, 1550f)
        Matrix.setLookAtM(viewMatrix, 0, camX + shakeX, camY + shakeY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)
    }

    private data class CamSpec(val dist: Float, val pivotHeight: Float, val lookAhead: Float, val fov: Float)

    private data class LightingParams(
        val sunX: Float, val sunY: Float, val sunZ: Float,
        val sunR: Float, val sunG: Float, val sunB: Float,
        val topR: Float, val topG: Float, val topB: Float,
        val midR: Float, val midG: Float, val midB: Float,
        val horR: Float, val horG: Float, val horB: Float,
        val nightFactor: Float
    )

    private fun currentLighting(): LightingParams = when (engine.timeOfDay) {
        TimeOfDay.SUNSET -> LightingParams(
            sunX = -0.88f, sunY = 0.25f, sunZ = 0.22f,
            sunR = 1.00f, sunG = 0.66f, sunB = 0.36f,
            topR = 0.14f, topG = 0.18f, topB = 0.44f,
            midR = 0.68f, midG = 0.30f, midB = 0.48f,
            horR = 0.98f, horG = 0.58f, horB = 0.34f,
            nightFactor = 0.32f
        )
        TimeOfDay.DAY -> LightingParams(
            sunX = -0.45f, sunY = 0.82f, sunZ = 0.35f,
            sunR = 1.00f, sunG = 0.97f, sunB = 0.90f,
            topR = 0.10f, topG = 0.44f, topB = 0.86f,
            midR = 0.34f, midG = 0.68f, midB = 0.94f,
            horR = 0.74f, horG = 0.89f, horB = 0.98f,
            nightFactor = 0.0f
        )
        TimeOfDay.NIGHT -> LightingParams(
            sunX = -0.60f, sunY = 0.48f, sunZ = -0.25f,
            sunR = 0.44f, sunG = 0.56f, sunB = 0.86f,
            topR = 0.03f, topG = 0.05f, topB = 0.13f,
            midR = 0.07f, midG = 0.09f, midB = 0.22f,
            horR = 0.16f, horG = 0.12f, horB = 0.28f,
            nightFactor = 0.95f
        )
    }
}
