package com.example

import com.example.game.engine.BespokeVallartaResorts
import com.example.game.engine.BuildingStyle
import com.example.game.engine.CoastalTerrainBuilder
import com.example.game.engine.HighPolyCharacterMeshes
import com.example.game.engine.HighPolyLandmarkMeshes
import com.example.game.engine.HighPolyVehicleMeshes
import com.example.game.engine.MaterialId
import com.example.game.engine.OsmBuildingFootprint
import com.example.game.engine.OsmPolygonMeshBuilder
import com.example.game.engine.OsmPoolFootprint
import com.example.game.engine.VallartaDecorationBuilder
import com.example.game.engine.VehicleType
import com.example.game.simulation.PedestrianArchetype
import com.example.game.simulation.PedestrianZone
import com.example.game.simulation.VallartaGameEngine
import com.example.game.world.LandmarkType
import com.example.game.world.PrebakedRoadNetworkLoader
import com.example.game.world.VallartaGeography
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class ExampleUnitTest {

    @Test
    fun highPolyMeshesAndAll18BespokeResortsHaveRichDistinctGeometry() {
        val hero = HighPolyCharacterMeshes.buildHumanoid(HighPolyCharacterMeshes.PROTAGONIST)
        val heroTris = hero.totalTriangleCount
        assertTrue("10-bone protagonist humanoid should be high-poly (>2000 tris), was $heroTris", heroTris > 2000)

        // Verify all 4 authentic beachwear pedestrian outfits build rich 10-bone smooth rigs
        val beachOutfits = listOf(
            "BEACH_SWIM_SHORTS" to HighPolyCharacterMeshes.BEACH_SWIM_SHORTS,
            "BEACH_BIKINI_SARONG" to HighPolyCharacterMeshes.BEACH_BIKINI_SARONG,
            "BEACH_HAWAIIAN_SHIRT" to HighPolyCharacterMeshes.BEACH_HAWAIIAN_SHIRT,
            "BEACH_SUNDRESS" to HighPolyCharacterMeshes.BEACH_SUNDRESS
        )
        for ((name, outfit) in beachOutfits) {
            val tris = HighPolyCharacterMeshes.buildHumanoid(outfit).totalTriangleCount
            assertTrue("Beachwear rig $name should be smooth high-poly (>1800 tris), was $tris", tris > 1800)
        }

        val supercar = HighPolyVehicleMeshes.buildVehicleBody(VehicleType.SUPERCAR, 1f, 0.4f, 0.1f)
        val wheel = HighPolyVehicleMeshes.buildAlloyWheel()
        val totalCarTris = supercar.triangleCount + wheel.triangleCount * 4
        assertTrue("Supercar + 4 alloy wheels should be high-poly (>2000 tris), was $totalCarTris", totalCarTris > 2000)

        // Verify ALL 18 distinct bespoke beachfront resorts + both authentic Río Cuale bridges
        val distinctResorts = listOf(
            "Velas" to BespokeVallartaResorts.buildVelasVallartaComplex(),
            "Marriott" to BespokeVallartaResorts.buildMarriottResortComplex(),
            "Westin" to BespokeVallartaResorts.buildWestinResortComplex(),
            "Melia" to BespokeVallartaResorts.buildMeliaVallartaComplex(),
            "CrownParadise" to BespokeVallartaResorts.buildCrownParadiseComplex(),
            "Krystal" to BespokeVallartaResorts.buildKrystalVillageComplex(),
            "Peninsula" to BespokeVallartaResorts.buildPeninsulaTowersComplex(),
            "GrandVenetian" to BespokeVallartaResorts.buildGrandVenetianComplex(),
            "FiestaAmericana" to BespokeVallartaResorts.buildFiestaAmericanaComplex(),
            "Icon" to BespokeVallartaResorts.buildIconVallartaComplex(),
            "SecretsDreams" to BespokeVallartaResorts.buildSecretsDreamsComplex(),
            "Sheraton" to BespokeVallartaResorts.buildSheratonComplex(),
            "Buenaventura" to BespokeVallartaResorts.buildBuenaventuraComplex(),
            "HotelRosita" to BespokeVallartaResorts.buildHotelRositaComplex(),
            "HyattZiva" to BespokeVallartaResorts.buildHyattZivaCoveComplex(),
            "GarzaBlanca" to BespokeVallartaResorts.buildGarzaBlancaTerraceComplex(),
            "Barcelo" to BespokeVallartaResorts.buildBarceloMismaloyaComplex(),
            "BeachOpen" to BespokeVallartaResorts.buildBeachOpenBoutiqueResort(),
            "FlamingoVallarta" to BespokeVallartaResorts.buildFlamingoVallartaComplex()
        )
        for ((name, mesh) in distinctResorts) {
            assertTrue("Bespoke resort $name should have rich 3D geometry (>140 tris), was ${mesh.triangleCount}", mesh.triangleCount > 140)
        }
        assertTrue(BespokeVallartaResorts.buildCualeCablePedestrianBridge().triangleCount > 200)
        assertTrue(BespokeVallartaResorts.buildCualeVehicularBridge().triangleCount > 200)

        val parroquia = HighPolyLandmarkMeshes.buildParroquiaCrown()
        val muelle = HighPolyLandmarkMeshes.buildMuelleLosMuertos()
        val caballito = HighPolyLandmarkMeshes.buildElCaballito()
        val palm = HighPolyLandmarkMeshes.buildPalmTree()
        assertTrue(parroquia.triangleCount > 300)
        assertTrue(muelle.triangleCount > 250)
        assertTrue(caballito.triangleCount > 100)
        assertTrue(palm.triangleCount > 200)

        // Verify Pure-3D PBR terrain mesh, Marina seawalls/docks/roads, and ocean/Marina/Cuale water
        val (terrainMesh, roadsMesh, oceanMesh) = CoastalTerrainBuilder.buildWorldGeometry()
        assertTrue("Pure-3D PBR terrain should have >20,000 tris, was ${terrainMesh.triangleCount}", terrainMesh.triangleCount > 20_000)
        assertTrue("Roads, Malecón & Marina seawalls/docks should have >3,000 tris, was ${roadsMesh.triangleCount}", roadsMesh.triangleCount > 3_000)
        assertTrue("Pacific Ocean + Marina + Lagoons + Cuale should have >10,000 tris, was ${oceanMesh.triangleCount}", oceanMesh.triangleCount > 10_000)

        // Verify real OSM polygon + 3D balconies + Hacienda cupola + skyscraper + pool extrusion builder produces rich architectural geometry
        val osmMesh = OsmPolygonMeshBuilder.buildSectorMesh(
            footprints = listOf(
                OsmBuildingFootprint(
                    id = 1L,
                    name = "Hotel Flamingo Vallarta",
                    centerX = 0f,
                    centerZ = 0f,
                    height = 18f,
                    materialId = MaterialId.RESORT_FACADE,
                    colorR = 0.96f, colorG = 0.90f, colorB = 0.80f,
                    roofR = 0.78f, roofG = 0.32f, roofB = 0.18f,
                    style = BuildingStyle.HACIENDA_RESORT,
                    ring = listOf(-14f to -10f, 14f to -10f, 14f to 10f, -14f to 10f)
                ),
                OsmBuildingFootprint(
                    id = 2L,
                    name = "Westin Resort Vallarta",
                    centerX = 45f,
                    centerZ = 0f,
                    height = 42f,
                    materialId = MaterialId.RESORT_FACADE,
                    colorR = 0.94f, colorG = 0.54f, colorB = 0.44f,
                    roofR = 0.82f, roofG = 0.34f, roofB = 0.22f,
                    style = BuildingStyle.RESORT_BALCONIES,
                    ring = listOf(30f to -12f, 60f to -12f, 60f to 12f, 30f to 12f)
                ),
                OsmBuildingFootprint(
                    id = 3L,
                    name = "Icon Vallarta Tower",
                    centerX = 95f,
                    centerZ = 0f,
                    height = 75f,
                    materialId = MaterialId.CONDO_FACADE,
                    colorR = 0.88f, colorG = 0.93f, colorB = 0.97f,
                    roofR = 0.65f, roofG = 0.70f, roofB = 0.76f,
                    style = BuildingStyle.GLASS_SKYSCRAPER,
                    ring = listOf(84f to -10f, 106f to -10f, 106f to 10f, 84f to 10f)
                ),
                OsmBuildingFootprint(
                    id = 5L,
                    name = "Taquería El Malecón",
                    centerX = 135f,
                    centerZ = 0f,
                    height = 5.5f,
                    materialId = MaterialId.COLONIAL_FACADE,
                    colorR = 0.96f, colorG = 0.78f, colorB = 0.42f,
                    roofR = 0.82f, roofG = 0.24f, roofB = 0.22f,
                    style = BuildingStyle.SMALL_SHOP_AWNING,
                    ring = listOf(130f to -5f, 140f to -5f, 140f to 5f, 130f to 5f)
                ),
                OsmBuildingFootprint(
                    id = 6L,
                    name = "Palapa Beach Club",
                    centerX = -60f,
                    centerZ = 0f,
                    height = 5.2f,
                    materialId = MaterialId.MATTE,
                    colorR = 0.86f, colorG = 0.70f, colorB = 0.44f,
                    roofR = 0.80f, roofG = 0.65f, roofB = 0.36f,
                    style = BuildingStyle.BEACH_PALAPA_CLUB,
                    ring = listOf(-68f to -6f, -52f to -6f, -52f to 6f, -68f to 6f)
                )
            ),
            pools = listOf(
                OsmPoolFootprint(
                    id = 4L,
                    centerX = -26f,
                    centerZ = 0f,
                    ring = listOf(-34f to -6f, -18f to -6f, -18f to 6f, -34f to 6f)
                )
            )
        )
        assertTrue("Styled 3D OSM hotels, awning shop, beach palapa club & pool should exceed 320 tris, was ${osmMesh.triangleCount}", osmMesh.triangleCount > 320)

        val decorSectors = runBlocking { VallartaDecorationBuilder.buildBatchedDecorationSectors() }
        assertTrue("Batched decoration & lush inland vegetation should generate >40 sector chunks, got ${decorSectors.size}", decorSectors.size > 40)
        val totalDecorTris = decorSectors.sumOf { it.meshData.triangleCount }
        assertTrue("Batched decoration & Sierra Madre vegetation should exceed 25,000 tris, was $totalDecorTris", totalDecorTris > 25_000)
    }

    @Test
    fun trueOneToOnePuertoVallartaGeographyHasExactShorelineSmoothTerrainAndCloseBeachfrontHotels() {
        assertEquals(12, VallartaGeography.districts.size)

        val airport = VallartaGeography.districts.first { it.id == "airport" }
        val mismaloya = VallartaGeography.districts.first { it.id == "mismaloya" }
        val pitillal = VallartaGeography.districts.first { it.id == "pitillal" }
        val fluvial = VallartaGeography.districts.first { it.id == "fluvial" }

        // True 1:1 GPS scale: Airport PVR -> Mismaloya is > 15,000 real meters!
        val northSouthSpan = abs(mismaloya.spawnZ - airport.spawnZ)
        assertTrue("Airport-to-Mismaloya 1:1 span should exceed 15,000m, was $northSouthSpan", northSouthSpan > 15_000f)

        // True inland depth: Fluvial (> 1.5 km) and El Pitillal (> 3.5 km) east of coastline!
        val fluvialInland = fluvial.spawnX - VallartaGeography.coastX(fluvial.spawnZ)
        val pitillalInland = pitillal.spawnX - VallartaGeography.coastX(pitillal.spawnZ)
        assertTrue("Fluvial Vallarta should be > 1,500m inland, was $fluvialInland", fluvialInland > 1500f)
        assertTrue("El Pitillal should be > 3,500m inland, was $pitillalInland", pitillalInland > 3500f)

        // Verify exact OSM signedCoastDist: deep Pacific Ocean is negative, dry land landmarks are positive!
        val deepOceanSd = VallartaGeography.signedCoastDist(
            VallartaGeography.lonToWorldX(-105.242),
            VallartaGeography.latToWorldZ(20.610)
        )
        assertTrue("Pacific Ocean west of Malecón should have negative signedCoastDist, was $deepOceanSd", deepOceanSd < -150f)

        val dryLandChecks = listOf(
            "Parroquia de Guadalupe" to (20.6095 to -105.2335),
            "Hotel Rosita" to (20.6149 to -105.2320),
            "Sheraton Buganvilias" to (20.6251 to -105.2314),
            "Hyatt Ziva" to (20.5831 to -105.2442),
            "Garza Blanca" to (20.5477 to -105.2654),
            "Barceló Mismaloya" to (20.5313 to -105.2916)
        )
        for ((label, coords) in dryLandChecks) {
            val sd = VallartaGeography.signedCoastDist(
                VallartaGeography.lonToWorldX(coords.second),
                VallartaGeography.latToWorldZ(coords.first)
            )
            assertTrue("$label should be on dry land (signedCoastDist > 0), was $sd", sd > 0f)
        }

        // Verify smooth flat coastal topography across Hotel Zone North & Malecón (zero sudden cliff spikes!)
        val hzZ = VallartaGeography.latToWorldZ(20.638)
        val coastX = VallartaGeography.coastX(hzZ)
        val h1 = VallartaGeography.terrainHeight(coastX + 30f, hzZ)
        val h2 = VallartaGeography.terrainHeight(coastX + 80f, hzZ)
        assertTrue("Hotel Zone North coastal plain should stay smooth & level (|h2-h1| < 1.5m), was ${abs(h2 - h1)}", abs(h2 - h1) < 1.5f)
        assertTrue("Coastal plain should be cleanly above sea level (> 1.4m), was $h1", h1 > 1.4f)

        // Verify Puente Río Cuale deck at exact OSM coordinates stays cleanly elevated above the river channel
        val bridgeZ = VallartaGeography.latToWorldZ(20.6057)
        val bridgeX = VallartaGeography.lonToWorldX(-105.2364)
        val bridgeSurfaceY = CoastalTerrainBuilder.surfaceHeightAt(bridgeX, bridgeZ)
        assertTrue("Puente Río Cuale road surface should remain above river (> 2.0m), was $bridgeSurfaceY", bridgeSurfaceY > 2.0f)

        val (buildings, props) = VallartaGeography.buildStaticCity()
        val bespokeResorts = buildings.filter { it.type.name.startsWith("RESORT_") || it.type == LandmarkType.HOTEL_ROSITA }
        assertTrue("Should generate 36+ continuous beachfront resorts along the coast, had ${bespokeResorts.size}", bespokeResorts.size >= 36)

        // CRITICAL: ZERO resorts may ever sit in the ocean! Every single resort must have signedCoastDist > 10m!
        val wetResorts = bespokeResorts.filter { VallartaGeography.signedCoastDist(it.x, it.z) <= 10f }
        assertTrue(
            "Zero beachfront resorts may sit in ocean/waterline! Found wet resorts: ${wetResorts.map { it.name }}",
            wetResorts.isEmpty()
        )

        // Verify Flamingo Vallarta (East Marina shore) AND Vamar Vallarta (Marina Peninsula) are present, dry, and elevated!
        val flamingo = buildings.firstOrNull { it.id == "flamingo_vallarta" }
        assertNotNull("Flamingo Vallarta Hotel & Marina should exist in static city", flamingo)
        assertTrue(
            "Flamingo Vallarta must sit on dry land outside Marina basin (sd=${VallartaGeography.signedCoastDist(flamingo!!.x, flamingo.z)})",
            VallartaGeography.signedCoastDist(flamingo.x, flamingo.z) > 50f && !VallartaGeography.isInsideMarinaBasin(flamingo.x, flamingo.z)
        )
        assertTrue(
            "Flamingo Vallarta terrain surface should be dry & walkable (>1.8m)",
            CoastalTerrainBuilder.surfaceHeightAt(flamingo.x, flamingo.z) > 1.8f
        )

        val vamar = buildings.firstOrNull { it.id == "vamar_vallarta" }
        assertNotNull("Vamar Vallarta Marina & Beach should exist on the Marina peninsula", vamar)
        assertTrue(
            "Vamar Vallarta peninsula must be dry & outside Marina water",
            VallartaGeography.signedCoastDist(vamar!!.x, vamar.z) > 50f && !VallartaGeography.isInsideMarinaBasin(vamar.x, vamar.z)
        )
        assertTrue(
            "Vamar Vallarta peninsula terrain surface should be dry & walkable (>1.8m)",
            CoastalTerrainBuilder.surfaceHeightAt(vamar.x, vamar.z) > 1.8f
        )

        // Verify Marina basin water sits cleanly between beachfront hotels and Flamingo/Vamar!
        val marinaWaterX = VallartaGeography.lonToWorldX(-105.2502)
        val marinaWaterZ = VallartaGeography.latToWorldZ(20.6636)
        assertTrue("Marina Vallarta inner yacht harbor must be water", VallartaGeography.isInsideMarinaBasin(marinaWaterX, marinaWaterZ))

        // Verify strict 5 km inland boundary constant
        assertEquals(5000f, VallartaGeography.MAX_INLAND_DIST_M, 0.1f)

        // Verify beachfront resorts hug the dry sand shoreline and ZERO resort corners sit in the ocean!
        val wetResortCorners = mutableListOf<String>()
        for (r in bespokeResorts) {
            val rad = Math.toRadians(r.rotationDeg.toDouble())
            val cosR = kotlin.math.cos(rad).toFloat()
            val sinR = kotlin.math.sin(rad).toFloat()
            val hw = r.width * 0.48f
            val hd = r.depth * 0.48f
            val corners = listOf(-hw to -hd, hw to -hd, hw to hd, -hw to hd)
            for ((lx, lz) in corners) {
                val wx = r.x + lx * cosR + lz * sinR
                val wz = r.z - lx * sinR + lz * cosR
                val sd = VallartaGeography.signedCoastDist(wx, wz)
                if (sd < 6f) {
                    wetResortCorners += "${r.name} (sd=$sd)"
                    break
                }
            }
        }
        assertTrue(
            "Zero resort footprints may overhang into the ocean! Wet corners: $wetResortCorners",
            wetResortCorners.isEmpty()
        )

        // Verify Av. Francisco Medina Ascencio, Paseo de la Marina Norte, and Paseo de la Marina Sur (Vamar Peninsula) stay 100% dry!
        for (roadId in listOf("medina_ascencio", "paseo_marina", "paseo_marina_sur")) {
            val road = VallartaGeography.roadNetwork.firstOrNull { it.id == roadId }
            assertNotNull("Road $roadId must exist", road)
            for ((wx, wz) in road!!.points) {
                assertTrue("Road $roadId point ($wx,$wz) must stay outside Marina water", !VallartaGeography.isInsideMarinaBasin(wx, wz))
                val sd = VallartaGeography.signedCoastDist(wx, wz)
                assertTrue("Road $roadId point ($wx,$wz) must stay dry (sd > 20m), was $sd", sd > 20f)
            }
        }

        val closeBeachResorts = bespokeResorts.count { r ->
            val distFromSand = VallartaGeography.signedCoastDist(r.x, r.z)
            distFromSand in 20f..115f
        }
        assertTrue("Nearly all beachfront resorts should hug the dry sand (20..115m from coast), got $closeBeachResorts / ${bespokeResorts.size}", closeBeachResorts >= 34)
        assertTrue("Should populate palm trees, streetlamps, and umbrellas", props.size > 150)
    }

    @Test
    fun simulationEngineSupportsDrivingSteeringCameraAndMissions() {
        val engine = VallartaGameEngine()
        engine.sound.muted = true

        // Verify lively 84-person coastal crowd including 30+ beachfront walkers wearing authentic beachwear
        assertTrue("Should spawn 80+ lively coastal pedestrians, got ${engine.pedestrians.size}", engine.pedestrians.size >= 80)
        val beachWalkers = engine.pedestrians.filter { it.zone == PedestrianZone.BEACH_SHORE }
        assertTrue("Should spawn 30+ beachfront walkers on the golden sand, got ${beachWalkers.size}", beachWalkers.size >= 30)
        val beachwearTypes = setOf(
            PedestrianArchetype.BEACH_SWIM_SHORTS,
            PedestrianArchetype.BEACH_BIKINI_SARONG,
            PedestrianArchetype.BEACH_HAWAIIAN_SHIRT,
            PedestrianArchetype.BEACH_SUNDRESS
        )
        assertTrue(
            "All beach shore walkers must wear authentic beachwear outfits",
            beachWalkers.all { it.archetype in beachwearTypes }
        )
        assertEquals(
            "All 4 distinct beachwear styles should appear on the beach",
            4,
            beachWalkers.map { it.archetype }.toSet().size
        )

        // Verify live real Puerto Vallarta street / beach names lookup
        val maleconStreet = PrebakedRoadNetworkLoader.lookupStreetName(
            VallartaGeography.lonToWorldX(-105.2344),
            VallartaGeography.latToWorldZ(20.6104)
        )
        assertTrue("Malecón should resolve to Paseo Díaz Ordaz, was '$maleconStreet'", maleconStreet.contains("Díaz Ordaz") || maleconStreet.contains("Malecón"))
        val losMuertosBeach = PrebakedRoadNetworkLoader.lookupStreetName(
            VallartaGeography.coastX(VallartaGeography.latToWorldZ(20.5988)) + 14f,
            VallartaGeography.latToWorldZ(20.5988)
        )
        assertTrue("Los Muertos beach shore should resolve to Playa Los Muertos, was '$losMuertosBeach'", losMuertosBeach.contains("Los Muertos"))

        // Verify free-look camera yaw & pitch respond to drag deltas
        val initialYaw = engine.cameraYaw
        val initialPitch = engine.cameraPitch
        engine.input.cameraYawDelta = 0.25f
        engine.input.cameraPitchDelta = 0.10f
        engine.update(0.033f)
        assertTrue("Camera yaw should orbit cleanly", engine.cameraYaw != initialYaw)
        assertTrue("Camera pitch should tilt cleanly", engine.cameraPitch != initialPitch)

        // Start Mission 1 (Airport PVR -> Marina Vallarta)
        engine.startMission(VallartaGeography.missions.first())
        assertTrue("Player should start mission inside a vehicle", engine.player.vehicleId != null)

        // Accelerate forward and steer with dedicated driving controls
        engine.input.accelerate = true
        repeat(30) { engine.update(0.033f) }
        assertTrue("Vehicle should accelerate along coastal boulevard", engine.player.speed > 8f)

        val headingBeforeSteer = engine.player.headingRad
        engine.input.steerRight = true
        repeat(10) { engine.update(0.033f) }
        engine.input.steerRight = false
        assertTrue("Dedicated steerRight should turn the car", engine.player.headingRad < headingBeforeSteer)

        // Verify proximity landmark detection at El Malecón y Centro
        val malecon = VallartaGeography.districts.first { it.id == "malecon" }
        engine.teleportToDistrict(malecon)
        repeat(5) { engine.update(0.033f) }
        assertNotNull("Player near Malecón should see nearby landmark/hotel label", engine.hudState.value.nearbyLandmarkLabel)

        // Verify strict 5 km inland boundary prevents walking past MAX_INLAND_DIST_M
        engine.player.vehicleId = null
        val testZ = VallartaGeography.latToWorldZ(20.640)
        val coastX = VallartaGeography.coastX(testZ)
        engine.player.x = coastX + VallartaGeography.MAX_INLAND_DIST_M - 1f
        engine.player.z = testZ
        engine.cameraYaw = (Math.PI / 2.0).toFloat() // Facing straight East (+X inland)
        engine.input.joyX = 0f
        engine.input.joyY = 1f
        engine.input.sprint = true
        repeat(20) { engine.update(0.033f) }
        val finalSd = VallartaGeography.signedCoastDist(engine.player.x, engine.player.z)
        assertTrue("Player must not cross 5 km inland boundary (was $finalSd m)", finalSd <= VallartaGeography.MAX_INLAND_DIST_M + 1f)
    }

    @Test
    fun solidBuildingCollisionsJumpAndWallClimbingWorkCorrectly() {
        val engine = VallartaGameEngine()
        engine.sound.muted = true
        engine.player.vehicleId = null

        // Place player on flat open coastal plain away from traffic
        val testZ = VallartaGeography.latToWorldZ(20.640)
        val baseX = VallartaGeography.coastX(testZ) + 120f
        val groundY = CoastalTerrainBuilder.surfaceHeightAt(baseX, testZ)
        engine.player.x = baseX
        engine.player.z = testZ
        engine.player.y = groundY
        engine.player.isGrounded = true

        // 1. Register a solid 10m x 10m x 7m building directly East (+X) of the player
        val wallBox = com.example.game.world.ColliderBox(
            cx = baseX + 6.5f,
            cz = testZ,
            halfW = 5f,
            halfD = 5f,
            cosR = 1f,
            sinR = 0f,
            baseY = groundY - 0.5f,
            topY = groundY + 7.0f
        )
        engine.colliderGrid.addBox(wallBox)

        // Attempt to walk straight into the building (+X) — MUST be blocked solidly before x = baseX + 1.5f
        engine.cameraYaw = (Math.PI / 2.0).toFloat() // Forward = +X
        engine.input.joyX = 0f
        engine.input.joyY = 1f
        engine.input.sprint = true
        repeat(35) { engine.update(0.033f) }
        assertTrue(
            "Solid building wall must block player from passing through (player.x=${engine.player.x}, wallFront=${baseX + 1.5f})",
            engine.player.x < baseX + 1.45f
        )
        engine.input.joyY = 0f
        engine.input.sprint = false

        // 2. Test Jump: pressing pendingJump lifts player clearly into the air and lands back on ground
        engine.player.x = baseX - 2f
        engine.player.z = testZ
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        val yBeforeJump = engine.player.y
        engine.input.pendingJump = true
        var maxJumpY = yBeforeJump
        repeat(40) {
            engine.update(0.033f)
            if (engine.player.y > maxJumpY) maxJumpY = engine.player.y
        }
        assertTrue("Jump must lift player > 1.3m off ground, gained ${maxJumpY - yBeforeJump}m", maxJumpY - yBeforeJump > 1.3f)
        assertTrue("Player must land back on ground after jump", engine.player.isGrounded)

        // 3. Test Wall Climbing & Rooftop Mantling: stand facing wallBox (+X) and hold climbHold + forward
        engine.player.x = baseX + 0.8f
        engine.player.z = testZ
        engine.player.y = groundY
        engine.player.headingRad = (Math.PI / 2.0).toFloat() // Facing +X into wall
        engine.cameraYaw = (Math.PI / 2.0).toFloat()
        engine.input.climbHold = true
        engine.input.joyY = 1f
        var enteredClimb = false
        repeat(90) {
            engine.update(0.033f)
            if (engine.player.isClimbing || engine.player.isMantling) enteredClimb = true
        }
        engine.input.climbHold = false
        engine.input.joyY = 0f
        repeat(15) { engine.update(0.033f) }

        assertTrue("Player facing solid wall with climbHold should enter climb/mantle state", enteredClimb)
        assertTrue(
            "Player should climb and mantle onto the 7m building rooftop (player.y=${engine.player.y}, expected >= ${groundY + 6.5f})",
            engine.player.y >= groundY + 6.5f
        )
        assertTrue("Player should stand stably on top of the building roof", engine.player.isGrounded)

        // 4. Test 3-hit realistic boxing combo (Left Jab -> Right Cross -> Right Hook) & forward weight lunge
        engine.player.x = baseX - 8f
        engine.player.z = testZ
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        engine.player.headingRad = 0f
        engine.player.punchTimer = 0f
        engine.player.punchComboResetTimer = 0f

        val startZ = engine.player.z
        engine.input.pendingAttack = true
        engine.update(0.033f)
        assertEquals("First punch should start combo 0 (Left Jab)", 0, engine.player.punchComboIndex)
        repeat(15) { engine.update(0.033f) }
        assertTrue("Boxing punch should produce realistic forward combat lunge", engine.player.z > startZ + 0.15f)
        assertTrue("Peak-extension impact flag should trigger cleanly", engine.player.punchHitDelivered)

        // Chain 2nd and 3rd combo punches!
        engine.input.pendingAttack = true
        engine.update(0.033f)
        assertEquals("Second punch should chain into combo 1 (Right Cross)", 1, engine.player.punchComboIndex)
        repeat(15) { engine.update(0.033f) }
        engine.input.pendingAttack = true
        engine.update(0.033f)
        assertEquals("Third punch should chain into combo 2 (Right Hook)", 2, engine.player.punchComboIndex)
    }

    @Test
    fun muelleLosMuertosWalkabilityPeninsulasAndOrnamentalPropCollisionsWork() {
        val engine = VallartaGameEngine()
        engine.sound.muted = true
        engine.player.vehicleId = null
        engine.vehicles.clear()

        // 1. Verify Boca de Tomates spit peninsula is DRY while estuary bay east of spit & Marina water are WATER,
        //    and all Marina peninsulas/hotels stay strictly dry!
        val spitX = VallartaGeography.lonToWorldX(-105.2884)
        val spitZ = VallartaGeography.latToWorldZ(20.6720)
        assertTrue(
            "Boca de Tomates sand spit peninsula should be dry land (sd=${VallartaGeography.signedCoastDist(spitX, spitZ)})",
            VallartaGeography.signedCoastDist(spitX, spitZ) > 0f
        )
        val bayBehindSpitX = VallartaGeography.lonToWorldX(-105.2840)
        val bayBehindSpitZ = VallartaGeography.latToWorldZ(20.6705)
        assertTrue(
            "Lagoon/estuary bay behind Boca de Tomates spit should be ocean water (sd=${VallartaGeography.signedCoastDist(bayBehindSpitX, bayBehindSpitZ)})",
            VallartaGeography.signedCoastDist(bayBehindSpitX, bayBehindSpitZ) < 0f
        )
        for (hotelId in listOf("flamingo_vallarta", "vamar_vallarta", "club_regina", "westin_pv", "melia_pv")) {
            val b = engine.buildings.first { it.id == hotelId }
            assertTrue(
                "Hotel $hotelId must be dry and outside Marina basin",
                VallartaGeography.signedCoastDist(b.x, b.z) > 10f && !VallartaGeography.isInsideMarinaBasin(b.x, b.z)
            )
        }

        // 2. Verify player can walk straight from Playa Los Muertos beach entrance up the Muelle ramp,
        //    across the boardwalk over ocean waves (sd < 0), onto the circular ocean plaza!
        val muelle = engine.buildings.first { it.id == "muelle_muertos" }
        val rad = Math.toRadians(muelle.rotationDeg.toDouble())
        val cosR = kotlin.math.cos(rad).toFloat()
        val sinR = kotlin.math.sin(rad).toFloat()
        // Start on dry sand just East of ramp lip (local x = +54m, z = 0)
        engine.player.x = muelle.x + 54f * cosR
        engine.player.z = muelle.z - 54f * sinR
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        // Walk along local -X direction: (-cosR, +sinR)
        val dirX = -cosR
        val dirZ = sinR
        engine.cameraYaw = kotlin.math.atan2(dirX, dirZ)
        engine.input.joyX = 0f
        engine.input.joyY = 1f
        engine.input.sprint = true
        repeat(280) { engine.update(0.033f) }
        engine.input.joyY = 0f
        engine.input.sprint = false

        val muelleLocalX = (engine.player.x - muelle.x) * cosR - (engine.player.z - muelle.z) * sinR
        assertTrue(
            "Player must walk seamlessly up ramp and onto Muelle ocean plaza (localX=$muelleLocalX, expected <= -15f, y=${engine.player.y})",
            muelleLocalX <= -15f
        )
        assertTrue(
            "Player walking on Muelle pier over the ocean must stay elevated on deck (y=${engine.player.y}, expected in 2.5..3.1m)",
            engine.player.y in 2.5f..3.1f
        )
        assertTrue("Player must remain grounded on Muelle ocean plaza", engine.player.isGrounded)

        // 3. Verify Los Arcos del Malecón amphitheater stepped stage is walkable & stone piers are solid!
        val arcos = engine.buildings.first { it.id == "los_arcos_malecon" }
        val aRad = Math.toRadians(arcos.rotationDeg.toDouble())
        val aCos = kotlin.math.cos(aRad).toFloat()
        val aSin = kotlin.math.sin(aRad).toFloat()
        // Stand slightly East of center arch gap (local x = +5.2m, z = +3.0m between piers at pz=0 and pz=6)
        engine.player.x = arcos.x + 5.2f * aCos + 3.0f * aSin
        engine.player.z = arcos.z - 5.2f * aSin + 3.0f * aCos
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        engine.cameraYaw = kotlin.math.atan2(-aCos, aSin)
        engine.input.joyY = 1f
        repeat(45) { engine.update(0.033f) }
        engine.input.joyY = 0f
        assertTrue(
            "Player must walk cleanly up steps onto Los Arcos stage (player.y=${engine.player.y}, expected >= ${arcos.y + 1.0f})",
            engine.player.y >= arcos.y + 1.0f
        )

        // 4. Verify Plaza de Armas Kiosco floor is walkable via steps while its 8 iron columns and palm trunks block!
        val decorChunks = runBlocking { VallartaDecorationBuilder.buildBatchedDecorationSectors(engine.colliderGrid) }
        engine.enqueuePrebakedOsmChunks(decorChunks)
        val totalDecorColliders = decorChunks.sumOf { it.colliders.size }
        assertTrue("All palms, trees, shrubs, planters, benches & kiosks must register colliders (>1500), got $totalDecorColliders", totalDecorColliders > 1500)

        val kioskX = VallartaGeography.lonToWorldX(-105.2356)
        val kioskZ = VallartaGeography.latToWorldZ(20.6083)
        val kioskGy = kotlin.math.max(2.0f, VallartaGeography.terrainHeight(kioskX, kioskZ))
        // Walking straight along z = kioskZ steps up all 3 octagonal steps onto the kiosk deck AND is blocked by the West column (cx = kioskX - 6.8)
        engine.player.x = kioskX - 11.2f
        engine.player.z = kioskZ
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        engine.cameraYaw = (Math.PI / 2.0).toFloat() // Walk East (+X)
        engine.input.joyY = 1f
        repeat(45) { engine.update(0.033f) }
        engine.input.joyY = 0f
        assertTrue(
            "Player walking at z=kioskZ should climb onto deck (y=${engine.player.y}) and stop cleanly at West column (x=${engine.player.x})",
            engine.player.y >= kioskGy + 0.85f && engine.player.x < kioskX - 6.5f
        )

        // Walking through the open colonnade gap at z = kioskZ + 2.4f walks smoothly all the way to kiosk center!
        engine.player.x = kioskX - 11.2f
        engine.player.z = kioskZ + 2.4f
        engine.player.y = CoastalTerrainBuilder.surfaceHeightAt(engine.player.x, engine.player.z)
        engine.player.isGrounded = true
        engine.cameraYaw = (Math.PI / 2.0).toFloat()
        engine.input.joyY = 1f
        repeat(65) { engine.update(0.033f) }
        engine.input.joyY = 0f
        assertTrue(
            "Player walking between columns must reach octagonal Plaza Kiosk center (x=${engine.player.x} near $kioskX, y=${engine.player.y} >= ${kioskGy + 0.85f})",
            abs(engine.player.x - kioskX) < 3.0f && engine.player.y >= kioskGy + 0.85f
        )

        // 5. Verify PALM_TREE static prop trunk has solid collision
        val palm = engine.props.first { it.type == LandmarkType.PALM_TREE }
        val palmSupport = engine.colliderGrid.collidesHorizontally(palm.x, palm.z, palm.y, palm.y + 1.85f, 0.3f)
        assertTrue("Palm tree trunk must have solid horizontal collision", palmSupport)
    }
}


