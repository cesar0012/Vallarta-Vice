package com.example.game.simulation

import com.example.game.engine.CoastalTerrainBuilder
import com.example.game.engine.OsmNamedPlace
import com.example.game.engine.OsmSectorChunk
import com.example.game.engine.VehicleType
import com.example.game.world.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*
import kotlin.random.Random

/**
 * Core 60fps open-world GTA simulation engine for Puerto Vallarta.
 * Features independent GTA-style 360° camera yaw/pitch (no wild spinning on foot!),
 * crisp driving physics planted flush on road/bridge surfaces, autonomous coastal traffic,
 * articulated pedestrian AI, Policía Municipal wanted pursuits, 5 coastal missions,
 * and live proximity landmark identification.
 */
class VallartaGameEngine {

    val input = GameInputState()
    val sound = VallartaSoundEngine()

    val buildings = CopyOnWriteArrayList<WorldBuilding>()
    val props = CopyOnWriteArrayList<WorldProp>()
    val vehicles = CopyOnWriteArrayList<VehicleEntity>()
    val pedestrians = CopyOnWriteArrayList<PedestrianEntity>()
    val osmNamedPlaces = CopyOnWriteArrayList<OsmNamedPlace>()
    val pendingOsmChunks = ConcurrentLinkedQueue<OsmSectorChunk>()
    val allLoadedOsmChunks = CopyOnWriteArrayList<OsmSectorChunk>()
    val loadedOsmSectors: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val colliderGrid = WorldColliderGrid()
    val player = PlayerState()

    @Volatile var timeOfDay: TimeOfDay = TimeOfDay.SUNSET
    @Volatile var cameraMode: CameraMode = CameraMode.CHASE
    @Volatile var radioStation: RadioStation = RadioStation.BAHIA_SYNTH

    // Independent GTA V-style free camera angles
    @Volatile var cameraYaw: Float = 0.20f
    @Volatile var cameraPitch: Float = 0.22f
    @Volatile var freeLookHoldTimer: Float = 0f
    @Volatile var worldTimeSec: Float = 0f
    @Volatile var projectedPins: List<ProjectedLandmarkPin> = emptyList()

    private var activeMission: ActiveMissionState? = null
    private val completedMissions = mutableSetOf<Int>()
    private var toastText: String? = null
    private var toastTimer: Float = 0f
    private var toastGold: Boolean = false
    private var osmStatusText: String = "PV 3D 1:1 • Calles, Empedrados y Hoteles"
    private var osmAddedCount: Int = 0
    private var loadedRoadSectorCount: Int = 0
    private var isOsmSyncing: Boolean = false
    private var boundaryToastCooldown: Float = 0f
    private var climbSfxAccum: Float = 0f
    private var canClimbAheadNow: Boolean = false
    private var hudPushAccum: Float = 0f
    private var nextVehicleId = 100
    private var sirenChirpAccum = 0f

    private val _hudState = MutableStateFlow(HudUiState())
    val hudState: StateFlow<HudUiState> = _hudState.asStateFlow()

    init {
        val (staticBuildings, staticProps) = VallartaGeography.buildStaticCity()
        buildings.addAll(staticBuildings)
        props.addAll(staticProps)
        for (b in staticBuildings) {
            colliderGrid.registerStaticBuilding(b)
        }
        for (p in staticProps) {
            when (p.type) {
                LandmarkType.PALM_TREE -> {
                    val r = (0.36f * p.scale).coerceAtLeast(0.26f)
                    colliderGrid.addBox(
                        ColliderBox(p.x, p.z, r, r, 1f, 0f, p.y - 0.5f, p.y + 7.5f * p.scale, climbable = false)
                    )
                }
                LandmarkType.STREETLAMP -> {
                    colliderGrid.addBox(
                        ColliderBox(p.x, p.z, 0.26f, 0.26f, 1f, 0f, p.y - 0.4f, p.y + 6.5f, climbable = false)
                    )
                }
                LandmarkType.UMBRELLA -> {
                    colliderGrid.addBox(
                        ColliderBox(p.x, p.z, 0.24f, 0.24f, 1f, 0f, p.y - 0.3f, p.y + 2.4f, climbable = false)
                    )
                    val rad = Math.toRadians(p.rotationDeg.toDouble())
                    val cosR = cos(rad).toFloat()
                    val sinR = sin(rad).toFloat()
                    val lx = 1.1f * cosR + 0.2f * sinR
                    val lz = -1.1f * sinR + 0.2f * cosR
                    colliderGrid.addBox(
                        ColliderBox(p.x + lx, p.z + lz, 0.44f, 0.95f, cosR, sinR, p.y - 0.2f, p.y + 0.48f, climbable = false)
                    )
                }
                else -> {}
            }
        }
        player.y = CoastalTerrainBuilder.surfaceHeightAt(player.x, player.z)
        cameraYaw = player.headingRad
        spawnInitialFleetAndCrowd()
        pushHudSnapshot()
    }

    private fun spawnInitialFleetAndCrowd() {
        vehicles.clear()
        pedestrians.clear()

        // Hero parked showcase vehicles on verified dry-land boulevards (El Malecón, Marina, Airport PVR, Los Muertos, Hotel Zone North)
        spawnHeroVehicleAtGps(VehicleType.SUPERCAR, colorIdx = 0, lat = 20.6104, lon = -105.2344)
        spawnHeroVehicleAtGps(VehicleType.CONVERTIBLE, colorIdx = 1, lat = 20.6110, lon = -105.2342)
        spawnHeroVehicleAtGps(VehicleType.MUSCLE, colorIdx = 2, lat = 20.6800, lon = -105.2508)
        spawnHeroVehicleAtGps(VehicleType.SUPERCAR, colorIdx = 3, lat = 20.5988, lon = -105.2374)
        spawnHeroVehicleAtGps(VehicleType.CONVERTIBLE, colorIdx = 4, lat = 20.6612, lon = -105.2546)

        val types = arrayOf(
            VehicleType.TAXI, VehicleType.CONVERTIBLE, VehicleType.SUV,
            VehicleType.MUSCLE, VehicleType.SUPERCAR, VehicleType.TAXI,
            VehicleType.POLICE, VehicleType.BUS, VehicleType.SUV
        )
        for (i in 0 until 24) {
            var offset = -520f + i * 44f
            if (abs(offset) < 38f) offset += 48f
            val z = (player.z + offset)
                .coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
            val dir = if (i % 2 == 0) 1f else -1f
            val lane = if (dir > 0f) 4.4f else -4.4f
            val bx = findClearBoulevardX(z, lane) ?: (VallartaGeography.mainBoulevardX(z) + lane)
            val t = types[i % types.size]
            val h = VallartaGeography.coastHeadingRad(z) + if (dir < 0f) PI.toFloat() else 0f
            vehicles += VehicleEntity(
                id = nextVehicleId++,
                type = t,
                colorIndex = i % 6,
                x = bx,
                y = CoastalTerrainBuilder.surfaceHeightAt(bx, z),
                z = z,
                headingRad = h,
                speed = 11f + (i % 4) * 2.5f,
                laneOffset = lane,
                directionSign = dir,
                isParked = false,
                occupiedByNpc = true
            )
        }

        // Lively 84-person coastal crowd across Golden Beaches (36), Malecón & Marina Boardwalk (22), and Boulevard Sidewalks (26)
        val streetArchs = arrayOf(
            PedestrianArchetype.TOURIST_CORAL,
            PedestrianArchetype.LOCAL_DRESS,
            PedestrianArchetype.LOCAL_GUAYABERA
        )
        val beachArchs = arrayOf(
            PedestrianArchetype.BEACH_SWIM_SHORTS,
            PedestrianArchetype.BEACH_BIKINI_SARONG,
            PedestrianArchetype.BEACH_HAWAIIAN_SHIRT,
            PedestrianArchetype.BEACH_SUNDRESS
        )
        val maleconArchs = arrayOf(
            PedestrianArchetype.BEACH_HAWAIIAN_SHIRT,
            PedestrianArchetype.BEACH_SUNDRESS,
            PedestrianArchetype.TOURIST_CORAL,
            PedestrianArchetype.LOCAL_GUAYABERA,
            PedestrianArchetype.LOCAL_DRESS,
            PedestrianArchetype.BEACH_SWIM_SHORTS
        )
        for (i in 0 until 84) {
            val zone = when {
                i < 36 -> PedestrianZone.BEACH_SHORE
                i < 58 -> PedestrianZone.MALECON_MARINA
                else -> PedestrianZone.BOULEVARD_SIDEWALK
            }
            val side = when (zone) {
                PedestrianZone.BEACH_SHORE -> 9.5f + (i % 6) * 2.4f          // 9.5m..21.5m onto dry golden sand!
                PedestrianZone.MALECON_MARINA -> 6.0f + (i % 4) * 2.1f       // Malecón stone promenade & Marina quay
                PedestrianZone.BOULEVARD_SIDEWALK -> if (i % 2 == 0) -11.4f else 11.4f
            }
            val dir = if (i % 2 == 0) 1f else -1f
            val arch = when {
                zone == PedestrianZone.BEACH_SHORE -> beachArchs[i % beachArchs.size]
                zone == PedestrianZone.MALECON_MARINA && i % 10 == 0 -> PedestrianArchetype.POLICE_OFFICER
                zone == PedestrianZone.MALECON_MARINA -> maleconArchs[i % maleconArchs.size]
                zone == PedestrianZone.BOULEVARD_SIDEWALK && i % 8 == 0 -> PedestrianArchetype.POLICE_OFFICER
                else -> streetArchs[i % streetArchs.size]
            }
            val spreadZ = when (zone) {
                PedestrianZone.BEACH_SHORE -> player.z - 220f + (i * 12.5f)
                PedestrianZone.MALECON_MARINA -> player.z - 210f + ((i - 36) * 19.5f)
                PedestrianZone.BOULEVARD_SIDEWALK -> player.z - 220f + ((i - 58) * 17.2f)
            }.coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
            val ped = PedestrianEntity(
                id = i + 1,
                archetype = arch,
                x = player.x,
                y = player.y,
                z = spreadZ,
                headingRad = VallartaGeography.coastHeadingRad(spreadZ) + if (dir < 0f) PI.toFloat() else 0f,
                speed = when (zone) {
                    PedestrianZone.BEACH_SHORE -> 1.35f + (i % 4) * 0.35f
                    PedestrianZone.MALECON_MARINA -> 1.55f + (i % 3) * 0.30f
                    PedestrianZone.BOULEVARD_SIDEWALK -> 1.70f + (i % 3) * 0.35f
                },
                walkPhase = i * 0.95f,
                sideOffset = side,
                directionSign = dir,
                zone = zone
            )
            placePedestrianCleanly(ped, spreadZ, i)
            pedestrians += ped
        }
    }

    /**
     * Verified dry-land Marina Vallarta boardwalk anchor spots (El Faro quay, Flamingo Vallarta shore,
     * and the Vamar Vallarta peninsula access promenade) for lively Marina pedestrians!
     */
    private val marinaBoardwalkSpots: List<Pair<Float, Float>> by lazy {
        val gps = listOf(
            // North quay near El Faro lighthouse & Marina boardwalk shops
            20.6656 to -105.2528,
            20.6654 to -105.2512,
            20.6652 to -105.2496,
            20.6650 to -105.2484,
            // East shore promenade right in front of Hotel Flamingo Vallarta
            20.6648 to -105.2478,
            20.6644 to -105.2479,
            20.6638 to -105.2478,
            20.6631 to -105.2474,
            // South access road & dry peninsula of Hotel Vamar Vallarta
            20.6621 to -105.2454,
            20.6618 to -105.2462,
            20.6615 to -105.2470,
            20.6612 to -105.2474
        )
        gps.map { (lat, lon) ->
            VallartaGeography.lonToWorldX(lon) to VallartaGeography.latToWorldZ(lat)
        }
    }

    private fun isNearMarinaZone(z: Float): Boolean {
        val northZ = VallartaGeography.latToWorldZ(20.673)
        val southZ = VallartaGeography.latToWorldZ(20.654)
        return z in northZ..southZ
    }

    private fun findClearBoulevardX(z: Float, preferredOffset: Float, radius: Float = 2.6f): Float? {
        val center = VallartaGeography.mainBoulevardX(z)
        val candidates = floatArrayOf(
            preferredOffset,
            preferredOffset * 0.5f,
            0f,
            -preferredOffset * 0.5f,
            -preferredOffset,
            if (preferredOffset >= 0f) 6.5f else -6.5f
        )
        for (off in candidates) {
            val cx = center + off
            if (VallartaGeography.signedCoastDist(cx, z) > 8f &&
                !VallartaGeography.isInsideMarinaBasin(cx, z)
            ) {
                val gy = CoastalTerrainBuilder.surfaceHeightAt(cx, z)
                if (!collidesWithBuilding(cx, z, radius, gy)) {
                    return cx
                }
            }
        }
        return null
    }

    private fun placePedestrianCleanly(p: PedestrianEntity, targetZ: Float, slotIndex: Int) {
        val safeZ = targetZ.coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
        if (p.zone == PedestrianZone.MALECON_MARINA && isNearMarinaZone(player.z) && slotIndex % 2 == 0) {
            val (mx, mz) = marinaBoardwalkSpots[slotIndex % marinaBoardwalkSpots.size]
            val jx = ((slotIndex * 7) % 5 - 2) * 1.1f
            val jz = ((slotIndex * 11) % 5 - 2) * 1.1f
            val tx = mx + jx
            val tz = mz + jz
            if (VallartaGeography.signedCoastDist(tx, tz) > 4f &&
                !VallartaGeography.isInsideMarinaBasin(tx, tz) &&
                !collidesWithBuilding(tx, tz, 0.8f)
            ) {
                p.x = tx
                p.z = tz
                p.y = CoastalTerrainBuilder.surfaceHeightAt(tx, tz)
                p.headingRad = if (p.directionSign > 0f) 0.4f else -2.7f
                p.panicTimer = 0f
                return
            }
        }

        val heading = VallartaGeography.coastHeadingRad(safeZ)
        val normX = cos(heading)
        val normZ = -sin(heading)

        var chosenX: Float
        var chosenZ: Float
        when (p.zone) {
            PedestrianZone.BEACH_SHORE -> {
                val cx = VallartaGeography.coastX(safeZ)
                chosenX = cx + normX * p.sideOffset
                chosenZ = safeZ + normZ * p.sideOffset
                // Nudge slightly seaward or landward if hitting a beachfront palapa/hotel edge
                if (collidesWithBuilding(chosenX, chosenZ, 0.85f) ||
                    VallartaGeography.signedCoastDist(chosenX, chosenZ) < 5.5f ||
                    VallartaGeography.isInsideMarinaBasin(chosenX, chosenZ)
                ) {
                    val altOffs = floatArrayOf(8.5f, 12.0f, 16.0f, 6.5f, 21.0f)
                    for (ao in altOffs) {
                        val ax = cx + normX * ao
                        val az = safeZ + normZ * ao
                        if (VallartaGeography.signedCoastDist(ax, az) >= 5.0f &&
                            !VallartaGeography.isInsideMarinaBasin(ax, az) &&
                            !collidesWithBuilding(ax, az, 0.85f)
                        ) {
                            chosenX = ax
                            chosenZ = az
                            break
                        }
                    }
                }
            }
            PedestrianZone.MALECON_MARINA -> {
                val cx = VallartaGeography.coastX(safeZ)
                chosenX = cx + normX * p.sideOffset
                chosenZ = safeZ + normZ * p.sideOffset
                if (collidesWithBuilding(chosenX, chosenZ, 0.85f) ||
                    VallartaGeography.signedCoastDist(chosenX, chosenZ) < 5.0f ||
                    VallartaGeography.isInsideMarinaBasin(chosenX, chosenZ)
                ) {
                    chosenX = VallartaGeography.mainBoulevardX(safeZ) - 10.5f
                    chosenZ = safeZ
                }
            }
            PedestrianZone.BOULEVARD_SIDEWALK -> {
                val bx = VallartaGeography.mainBoulevardX(safeZ)
                chosenX = bx + p.sideOffset
                chosenZ = safeZ
                if (collidesWithBuilding(chosenX, chosenZ, 0.85f) ||
                    VallartaGeography.isInsideMarinaBasin(chosenX, chosenZ)
                ) {
                    chosenX = bx - p.sideOffset
                    if (collidesWithBuilding(chosenX, chosenZ, 0.85f)) {
                        chosenX = bx + (if (p.sideOffset > 0f) 7.5f else -7.5f)
                    }
                }
            }
        }
        p.x = chosenX
        p.z = chosenZ.coerceIn(VallartaGeography.WORLD_Z_NORTH + 35f, VallartaGeography.WORLD_Z_SOUTH - 35f)
        p.y = CoastalTerrainBuilder.surfaceHeightAt(p.x, p.z)
        p.headingRad = heading + if (p.directionSign < 0f) PI.toFloat() else 0f
        p.panicTimer = 0f
    }

    private fun spawnHeroVehicleAtGps(type: VehicleType, colorIdx: Int, lat: Double, lon: Double) {
        val x = VallartaGeography.lonToWorldX(lon)
        val z = VallartaGeography.latToWorldZ(lat)
        val y = CoastalTerrainBuilder.surfaceHeightAt(x, z)
        vehicles += VehicleEntity(
            id = nextVehicleId++,
            type = type,
            colorIndex = colorIdx,
            x = x,
            y = y,
            z = z,
            headingRad = VallartaGeography.coastHeadingRad(z),
            speed = 0f,
            laneOffset = 0f,
            directionSign = 1f,
            isParked = true,
            occupiedByNpc = false
        )
    }

    fun showToast(message: String, gold: Boolean = false, duration: Float = 3.2f) {
        toastText = message
        toastGold = gold
        toastTimer = duration
    }

    fun cycleTimeOfDay() {
        val vals = TimeOfDay.entries
        timeOfDay = vals[(timeOfDay.ordinal + 1) % vals.size]
        showToast("HORA: ${timeOfDay.label.uppercase()}")
        pushHudSnapshot()
    }

    fun cycleCameraMode() {
        val vals = CameraMode.entries
        cameraMode = vals[(cameraMode.ordinal + 1) % vals.size]
        showToast("CÁMARA: ${cameraMode.label.uppercase()}")
        pushHudSnapshot()
    }

    fun cycleRadioStation() {
        val vals = RadioStation.entries
        radioStation = vals[(radioStation.ordinal + 1) % vals.size]
        showToast("RADIO: ${radioStation.stationName}", gold = true)
        pushHudSnapshot()
    }

    fun toggleMute() {
        sound.muted = !sound.muted
        pushHudSnapshot()
    }

    fun setOsmSyncState(syncing: Boolean, status: String, chunk: OsmSectorChunk? = null) {
        isOsmSyncing = syncing
        osmStatusText = status
        if (chunk != null && loadedOsmSectors.add(chunk.sectorKey)) {
            allLoadedOsmChunks.add(chunk)
            pendingOsmChunks.add(chunk)
            if (chunk.colliders.isNotEmpty()) colliderGrid.addAll(chunk.colliders)
            osmNamedPlaces.addAll(chunk.namedPlaces)
            osmAddedCount += chunk.buildingCount
            showToast("OSM 3D 1:1: +${chunk.buildingCount} EDIFICIOS REALES", gold = true)
        }
        pushHudSnapshot()
    }

    fun enqueuePrebakedOsmChunks(chunks: List<OsmSectorChunk>) {
        if (chunks.isEmpty()) {
            isOsmSyncing = false
            osmStatusText = "PV 3D 1:1 • Ciudad activa"
            pushHudSnapshot()
            return
        }
        var totalPools = 0
        var addedBuildingsNow = 0
        var addedRoadSectorsNow = 0
        var addedDecorSectorsNow = 0
        for (chunk in chunks) {
            if (loadedOsmSectors.add(chunk.sectorKey)) {
                allLoadedOsmChunks.add(chunk)
                pendingOsmChunks.add(chunk)
                if (chunk.colliders.isNotEmpty()) colliderGrid.addAll(chunk.colliders)
                when {
                    chunk.sectorKey.startsWith("road_") -> {
                        loadedRoadSectorCount++
                        addedRoadSectorsNow++
                    }
                    chunk.sectorKey.startsWith("decor_") -> {
                        addedDecorSectorsNow++
                    }
                    else -> {
                        osmNamedPlaces.addAll(chunk.namedPlaces)
                        osmAddedCount += chunk.buildingCount
                        addedBuildingsNow += chunk.buildingCount
                        totalPools += chunk.poolCount
                    }
                }
            }
        }
        isOsmSyncing = false
        osmStatusText = if (osmAddedCount > 0) {
            "PV 3D 1:1 • 3,867 calles y +$osmAddedCount edificios"
        } else {
            "PV 3D 1:1 • $loadedRoadSectorCount sectores viales cargados"
        }
        if (addedBuildingsNow > 0) {
            showToast("PUERTO VALLARTA 3D 1:1: +$osmAddedCount EDIFICIOS, LOCALES Y $totalPools ALBERCAS", gold = true, duration = 4.0f)
        } else if (addedRoadSectorsNow > 0 && osmAddedCount == 0) {
            showToast("CALLES Y EMPEDRADOS 1:1 DE PUERTO VALLARTA ACTIVOS", gold = true, duration = 3.0f)
        } else if (addedDecorSectorsNow > 0 && osmAddedCount > 0) {
            showToast("PALMERAS, KIOSCOS Y VEGETACIÓN TROPICAL CARGADOS", gold = true, duration = 3.0f)
        }
        pushHudSnapshot()
    }

    fun teleportToDistrict(district: DistrictInfo) {
        val targetX = district.spawnX
        val targetZ = district.spawnZ
        val targetY = CoastalTerrainBuilder.surfaceHeightAt(targetX, targetZ)
        val heading = VallartaGeography.coastHeadingRad(targetZ)

        val currentVeh = vehicles.firstOrNull { it.id == player.vehicleId }
        if (currentVeh != null) {
            currentVeh.x = targetX
            currentVeh.y = targetY
            currentVeh.z = targetZ
            currentVeh.headingRad = heading
            currentVeh.speed = 0f
        }
        player.x = targetX
        player.y = targetY
        player.z = targetZ
        player.velY = 0f
        player.isGrounded = true
        player.isClimbing = false
        player.isMantling = false
        player.headingRad = heading
        cameraYaw = heading
        cameraPitch = 0.22f
        freeLookHoldTimer = 0f

        recenterWorldEntitiesAround(targetZ)
        showToast("VIAJE RÁPIDO: ${district.name.uppercase()}", gold = true)
        pushHudSnapshot()
    }

    fun startMission(mission: MissionDef) {
        activeMission = ActiveMissionState(
            mission = mission,
            timeRemaining = mission.timeLimitSec,
            targetX = mission.targetX,
            targetZ = mission.targetZ
        )
        val startX = mission.startX
        val startZ = mission.startZ
        val startY = CoastalTerrainBuilder.surfaceHeightAt(startX, startZ)
        val heading = VallartaGeography.coastHeadingRad(startZ)
        val currentVeh = vehicles.firstOrNull { it.id == player.vehicleId }
        if (currentVeh != null) {
            currentVeh.x = startX; currentVeh.y = startY; currentVeh.z = startZ
            currentVeh.headingRad = heading; currentVeh.speed = 0f
        } else {
            val spawned = VehicleEntity(
                id = nextVehicleId++,
                type = VehicleType.SUPERCAR,
                colorIndex = 0,
                x = startX, y = startY, z = startZ,
                headingRad = heading,
                isPlayerControlled = true,
                isParked = false,
                occupiedByNpc = false
            )
            vehicles.add(spawned)
            player.vehicleId = spawned.id
        }
        player.x = startX; player.y = startY; player.z = startZ; player.headingRad = heading
        cameraYaw = heading
        cameraPitch = 0.22f
        freeLookHoldTimer = 0f
        if (mission.triggerWantedStars > player.wantedStars) {
            player.wantedStars = mission.triggerWantedStars
            player.wantedCooldown = 28f
            sound.playPoliceChirp()
        }
        recenterWorldEntitiesAround(startZ)
        showToast("MISIÓN: ${mission.title}", gold = true, duration = 4.2f)
        pushHudSnapshot()
    }

    fun spawnPlayerSupercar() {
        val aheadX = player.x + sin(cameraYaw) * 5.2f
        val aheadZ = player.z + cos(cameraYaw) * 5.2f
        val y = CoastalTerrainBuilder.surfaceHeightAt(aheadX, aheadZ)
        val car = VehicleEntity(
            id = nextVehicleId++,
            type = if (Random.nextBoolean()) VehicleType.SUPERCAR else VehicleType.CONVERTIBLE,
            colorIndex = Random.nextInt(0, 6),
            x = aheadX, y = y, z = aheadZ,
            headingRad = cameraYaw,
            isParked = true,
            occupiedByNpc = false
        )
        vehicles.add(car)
        sound.playDoorLatch()
        showToast("${car.type.displayName.uppercase()} ENTREGADO", gold = true)
    }

    fun bribePoliceClearWanted() {
        if (player.wantedStars == 0) {
            showToast("NO TIENES ESTRELLAS DE BÚSQUEDA")
            return
        }
        val cost = 500
        if (player.cash >= cost) {
            player.cash -= cost
            player.wantedStars = 0
            player.wantedCooldown = 0f
            vehicles.forEach { it.isPoliceChasing = false }
            sound.playMissionComplete()
            showToast("POLICÍA SOBORNADA (-$500 MXN)", gold = true)
        } else {
            showToast("NECESITAS $500 MXN")
        }
        pushHudSnapshot()
    }

    private fun recenterWorldEntitiesAround(centerZ: Float) {
        var idx = 0
        for (v in vehicles) {
            if (v.isPlayerControlled || v.isParked) continue
            var offset = -460f + idx * 42f
            if (abs(offset) < 38f) {
                offset = if (offset >= 0f) offset + 52f else offset - 52f
            }
            val newZ = (centerZ + offset).coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
            val newX = findClearBoulevardX(newZ, v.laneOffset) ?: (VallartaGeography.mainBoulevardX(newZ) + v.laneOffset)
            v.z = newZ
            v.x = newX
            v.y = CoastalTerrainBuilder.surfaceHeightAt(newX, newZ)
            v.headingRad = VallartaGeography.coastHeadingRad(newZ) + if (v.directionSign < 0f) PI.toFloat() else 0f
            idx++
        }
        var pIdx = 0
        for (p in pedestrians) {
            val newZ = (centerZ - 210f + (pIdx % 36) * 12f)
                .coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
            placePedestrianCleanly(p, newZ, pIdx)
            pIdx++
        }
    }

    fun update(rawDt: Float) {
        val dt = rawDt.coerceIn(0.001f, 0.05f)
        worldTimeSec += dt

        if (toastTimer > 0f) {
            toastTimer -= dt
            if (toastTimer <= 0f) toastText = null
        }
        if (boundaryToastCooldown > 0f) {
            boundaryToastCooldown = (boundaryToastCooldown - dt).coerceAtLeast(0f)
        }

        // Smooth free-look camera orbit (yaw + vertical pitch!)
        val yawDelta = input.cameraYawDelta
        val pitchDelta = input.cameraPitchDelta
        if (abs(yawDelta) > 0.0001f || abs(pitchDelta) > 0.0001f) {
            cameraYaw += yawDelta
            cameraPitch = (cameraPitch + pitchDelta).coerceIn(-0.18f, 0.62f)
            input.cameraYawDelta = 0f
            input.cameraPitchDelta = 0f
            freeLookHoldTimer = 2.0f
        } else if (freeLookHoldTimer > 0f) {
            freeLookHoldTimer = (freeLookHoldTimer - dt).coerceAtLeast(0f)
        }

        handleOneShotActions()

        val activeVehicle = vehicles.firstOrNull { it.id == player.vehicleId }
        if (activeVehicle != null) {
            updateDrivingPlayer(activeVehicle, dt)
            // When driving and not actively dragging camera, smoothly auto-follow behind the car!
            if (freeLookHoldTimer <= 0f && abs(activeVehicle.speed) > 1.2f) {
                val followTargetYaw = if (activeVehicle.speed < -2f) {
                    activeVehicle.headingRad + PI.toFloat()
                } else {
                    activeVehicle.headingRad
                }
                cameraYaw = lerpAngle(cameraYaw, followTargetYaw, (dt * 4.5f).coerceAtMost(1f))
                cameraPitch += (0.22f - cameraPitch) * (dt * 2.5f).coerceAtMost(1f)
            }
        } else {
            updateOnFootPlayer(dt)
            // On foot: camera stays STABLE! Does NOT wildly spin when you strafe left/right!
        }

        updateTrafficAndPolice(dt)
        updatePedestrians(dt)
        updateWantedSystem(dt)
        updateMissionProgress(dt)

        hudPushAccum += dt
        if (hudPushAccum >= 0.055f) {
            hudPushAccum = 0f
            pushHudSnapshot()
        }
    }

    private fun handleOneShotActions() {
        if (input.pendingHorn) {
            input.pendingHorn = false
            sound.playHorn()
            for (p in pedestrians) {
                if (hypot(p.x - player.x, p.z - player.z) < 16f) {
                    p.panicTimer = max(p.panicTimer, 1.5f)
                }
            }
        }

        if (input.pendingAttack) {
            input.pendingAttack = false
            if (player.vehicleId == null && !player.isClimbing && !player.isMantling && player.punchTimer <= 0.06f) {
                player.punchComboIndex = if (player.punchComboResetTimer > 0f) {
                    (player.punchComboIndex + 1) % 3
                } else {
                    0
                }
                player.punchTimer = 0.46f
                player.punchHitDelivered = false
                player.punchComboResetTimer = 1.25f
                sound.playPunch()
            }
        }

        if (input.pendingEnterExit) {
            input.pendingEnterExit = false
            val currentVeh = vehicles.firstOrNull { it.id == player.vehicleId }
            if (currentVeh != null) {
                currentVeh.isPlayerControlled = false
                currentVeh.isParked = true
                currentVeh.occupiedByNpc = false
                currentVeh.speed *= 0.2f
                val sideX = -cos(currentVeh.headingRad) * 2.6f
                val sideZ = sin(currentVeh.headingRad) * 2.6f
                player.x = currentVeh.x + sideX
                player.z = currentVeh.z + sideZ
                player.y = CoastalTerrainBuilder.surfaceHeightAt(player.x, player.z)
                player.vehicleId = null
                player.speed = 0f
                player.velX = 0f
                player.velZ = 0f
                player.leanRoll = 0f
                sound.playDoorLatch()
            } else {
                val nearest = findNearestVehicle(7.2f)
                if (nearest != null) {
                    val wasOccupied = nearest.occupiedByNpc
                    val wasPolice = nearest.type.isPolice
                    nearest.isPlayerControlled = true
                    nearest.isParked = false
                    nearest.isPoliceChasing = false
                    nearest.occupiedByNpc = false
                    player.vehicleId = nearest.id
                    player.x = nearest.x
                    player.z = nearest.z
                    player.velX = 0f
                    player.velY = 0f
                    player.velZ = 0f
                    player.leanRoll = 0f
                    player.isGrounded = true
                    player.isClimbing = false
                    player.isMantling = false
                    player.headingRad = nearest.headingRad
                    cameraYaw = nearest.headingRad
                    freeLookHoldTimer = 0f
                    sound.playDoorLatch()
                    if (wasPolice) {
                        raiseWanted(3, "¡PATRULLA ROBADA! ★ ★ ★")
                    } else if (wasOccupied) {
                        raiseWanted(1, "¡VEHÍCULO ROBADO! ★")
                    } else {
                        showToast("CONDUCIENDO: ${nearest.type.displayName.uppercase()}")
                    }
                }
            }
        }
    }

    private fun vehicleRoofSupportY(x: Float, z: Float, currentFootY: Float): Float {
        var bestY = Float.NEGATIVE_INFINITY
        for (v in vehicles) {
            if (v.isPlayerControlled) continue
            val dx = x - v.x
            val dz = z - v.z
            if (abs(dx) > 4.2f || abs(dz) > 4.2f) continue
            val c = cos(-v.headingRad); val s = sin(-v.headingRad)
            val lx = dx * c - dz * s
            val lz = dx * s + dz * c
            val roofY = v.y + when (v.type) {
                VehicleType.BUS -> 2.75f
                VehicleType.SUV -> 1.75f
                else -> 1.38f
            }
            if (abs(lx) <= v.type.width * 0.56f && abs(lz) <= v.type.length * 0.52f && roofY <= currentFootY + 0.62f) {
                if (roofY > bestY) bestY = roofY
            }
        }
        return bestY
    }

    private fun supportSurfaceY(x: Float, z: Float, currentFootY: Float): Float {
        val groundY = CoastalTerrainBuilder.surfaceHeightAt(x, z)
        val roofY = colliderGrid.rooftopSupportY(x, z, currentFootY, 0.44f)
        val carY = vehicleRoofSupportY(x, z, currentFootY)
        return max(groundY, max(roofY, carY))
    }

    private fun startMantle(targetX: Float, targetY: Float, targetZ: Float) {
        player.isMantling = true
        player.isClimbing = false
        player.isGrounded = false
        player.velY = 0f
        player.mantleProgress = 0f
        player.mantleStartX = player.x
        player.mantleStartY = player.y
        player.mantleStartZ = player.z
        player.mantleEndX = targetX
        player.mantleEndY = targetY + 0.04f
        player.mantleEndZ = targetZ
        sound.playClimbGrab()
    }

    private fun canStandAtXZ(x: Float, z: Float, footY: Float): Boolean {
        if (z < VallartaGeography.WORLD_Z_NORTH + 25f || z > VallartaGeography.WORLD_Z_SOUTH - 25f) return false
        if (x > VallartaGeography.WORLD_X_INLAND - 40f) return false
        val onWalkableStructure = colliderGrid.rooftopSupportY(x, z, footY, 0.44f) > -5f
        val minCoastDist = if (onWalkableStructure) -120f else -2.5f
        val sd = VallartaGeography.signedCoastDist(x, z)
        if (sd > VallartaGeography.MAX_INLAND_DIST_M) {
            if (boundaryToastCooldown <= 0f) {
                showToast("LÍMITE DE PUERTO VALLARTA (5 KM TIERRA ADENTRO)", gold = true)
                boundaryToastCooldown = 3.5f
            }
            return false
        }
        if (sd < minCoastDist || (!onWalkableStructure && VallartaGeography.isInsideMarinaBasin(x, z))) return false
        if (collidesWithBuilding(x, z, 0.58f, footY)) return false
        // Also prevent walking straight through solid parked/moving cars (unless jumping ON TOP of them!)
        for (v in vehicles) {
            val dx = x - v.x; val dz = z - v.z
            if (abs(dx) > 4.2f || abs(dz) > 4.2f) continue
            val roofY = v.y + if (v.type == VehicleType.BUS) 2.5f else 1.22f
            if (footY >= roofY - 0.35f) continue
            val c = cos(-v.headingRad); val s = sin(-v.headingRad)
            val lx = dx * c - dz * s
            val lz = dx * s + dz * c
            if (abs(lx) < v.type.width * 0.52f + 0.35f && abs(lz) < v.type.length * 0.50f + 0.35f) {
                return false
            }
        }
        return true
    }

    /**
     * Smooth GTA-style 3D on-foot movement with Jumping, Ledge Mantling, Wall Climbing,
     * Walkable Rooftops, and Solid Wall Sliding!
     */
    private fun updateOnFootPlayer(dt: Float) {
        if (player.punchComboResetTimer > 0f) {
            player.punchComboResetTimer = (player.punchComboResetTimer - dt).coerceAtLeast(0f)
        }
        if (player.cameraPunchShake > 0f) {
            player.cameraPunchShake = (player.cameraPunchShake - dt * 5.2f).coerceAtLeast(0f)
        }
        if (player.landingSquash > 0f) {
            player.landingSquash = (player.landingSquash - dt * 4.5f).coerceAtLeast(0f)
        }

        // Biomechanical 3-hit boxing combo: forward weight-transfer lunge + peak-extension impact physics!
        if (player.punchTimer > 0f) {
            val punchNorm = (1f - player.punchTimer / 0.46f).coerceIn(0f, 1f)
            player.punchTimer = (player.punchTimer - dt).coerceAtLeast(0f)

            val fwdX = sin(player.headingRad)
            val fwdZ = cos(player.headingRad)

            if (punchNorm in 0.16f..0.44f && player.isGrounded) {
                val lungeSpeed = if (player.punchComboIndex == 2) 4.4f else 3.2f
                val lx = player.x + fwdX * lungeSpeed * dt
                val lz = player.z + fwdZ * lungeSpeed * dt
                if (canStandAtXZ(lx, lz, player.y)) {
                    player.x = lx
                    player.z = lz
                }
            }

            if (punchNorm >= 0.35f && !player.punchHitDelivered) {
                player.punchHitDelivered = true
                val knockPower = if (player.punchComboIndex == 2) 8.4f else 6.5f
                var landedHit = false
                for (p in pedestrians) {
                    val dx = p.x - player.x
                    val dz = p.z - player.z
                    val dist = hypot(dx, dz)
                    if (dist < 2.85f) {
                        val invD = if (dist > 0.05f) 1f / dist else 1f
                        val dot = (dx * invD) * fwdX + (dz * invD) * fwdZ
                        if (dot > 0.22f || dist < 1.35f) {
                            landedHit = true
                            val kx = (fwdX * 0.65f + dx * invD * 0.35f)
                            val kz = (fwdZ * 0.65f + dz * invD * 0.35f)
                            val kLen = hypot(kx, kz).coerceAtLeast(0.01f)
                            p.knockVelX = (kx / kLen) * knockPower
                            p.knockVelZ = (kz / kLen) * knockPower
                            p.staggerTimer = if (player.punchComboIndex == 2) 0.62f else 0.48f
                            p.panicTimer = 8f
                            if (p.archetype == PedestrianArchetype.POLICE_OFFICER) {
                                raiseWanted(2, "¡AGRESIÓN A LA POLICÍA!")
                            } else {
                                player.cash += 120
                                raiseWanted(1, "¡ALERTA EN LA VÍA PÚBLICA!")
                            }
                        }
                    }
                }
                if (landedHit) {
                    player.cameraPunchShake = if (player.punchComboIndex == 2) 1.0f else 0.72f
                }
            }
        }

        val jumpPressed = input.pendingJump
        if (jumpPressed) input.pendingJump = false

        // 1. Active Ledge Mantle / Vault onto rooftop!
        if (player.isMantling) {
            player.velX = 0f
            player.velZ = 0f
            player.leanRoll = 0f
            player.mantleProgress = (player.mantleProgress + dt / 0.28f).coerceAtMost(1f)
            val t = player.mantleProgress
            val smooth = t * t * (3f - 2f * t)
            // Vertical lift completes first, then forward step over the parapet
            val yT = (t * 1.35f).coerceAtMost(1f)
            val ySmooth = yT * yT * (3f - 2f * yT)
            player.x = player.mantleStartX + (player.mantleEndX - player.mantleStartX) * smooth
            player.z = player.mantleStartZ + (player.mantleEndZ - player.mantleStartZ) * smooth
            player.y = player.mantleStartY + (player.mantleEndY - player.mantleStartY) * ySmooth
            player.climbPhase += dt * 11f
            player.speed = 3.2f
            canClimbAheadNow = false
            if (player.mantleProgress >= 1f) {
                player.isMantling = false
                player.isGrounded = true
                player.velY = 0f
                player.landingSquash = 0.35f
            }
            return
        }

        // 2. Active Wall Climbing!
        if (player.isClimbing) {
            player.velX = 0f
            player.velZ = 0f
            player.leanRoll = 0f
            canClimbAheadNow = true
            val jx = input.joyX
            val jy = input.joyY
            // Press Jump while climbing: mantle if near rooftop, else athletic wall-kick!
            if (jumpPressed) {
                if (player.y >= player.climbWallTopY - 2.3f) {
                    startMantle(player.climbLandX, player.climbWallTopY, player.climbLandZ)
                } else {
                    player.isClimbing = false
                    player.isGrounded = false
                    player.velY = 8.4f
                    player.x += player.climbNormalX * 0.75f
                    player.z += player.climbNormalZ * 0.75f
                    player.headingRad = atan2(player.climbNormalX, player.climbNormalZ)
                    sound.playJump()
                }
                return
            }

            // Climb up when holding ESCALAR or pushing forward, climb down when pulling back
            val verticalInput = when {
                jy < -0.25f -> jy
                input.climbHold -> max(0.85f, jy)
                jy > 0.20f -> jy
                else -> 0f
            }

            if (!input.climbHold && abs(jy) < 0.15f && abs(jx) < 0.15f) {
                // Release climb button & stick neutral -> drop smoothly off wall
                player.isClimbing = false
                player.isGrounded = false
                player.velY = -1.2f
                return
            }

            val climbSpeed = 4.4f
            player.y += verticalInput * climbSpeed * dt
            if (abs(verticalInput) > 0.1f || abs(jx) > 0.1f) {
                player.climbPhase += dt * 7.8f
                climbSfxAccum += dt
                if (climbSfxAccum >= 0.42f) {
                    climbSfxAccum = 0f
                    sound.playClimbGrab()
                }
            }

            // Lateral shimmy along wall tangent
            if (abs(jx) > 0.20f) {
                val tanX = -player.climbNormalZ
                val tanZ = player.climbNormalX
                val sx = player.x + tanX * jx * 2.2f * dt
                val sz = player.z + tanZ * jx * 2.2f * dt
                val probe = colliderGrid.findClimbableWallAhead(sx, sz, player.y, -player.climbNormalX, -player.climbNormalZ, 1.35f)
                if (probe != null) {
                    player.x = probe.attachX
                    player.z = probe.attachZ
                    player.climbWallTopY = probe.wallTopY
                    player.climbLandX = probe.roofLandX
                    player.climbLandZ = probe.roofLandZ
                }
            }

            // Reached rooftop edge -> smooth mantle onto the roof!
            if (player.y >= player.climbWallTopY - 1.45f) {
                startMantle(player.climbLandX, player.climbWallTopY, player.climbLandZ)
                return
            }
            // Climbed all the way back down to ground
            val groundY = CoastalTerrainBuilder.surfaceHeightAt(player.x, player.z)
            if (player.y <= groundY + 0.08f && verticalInput < 0f) {
                player.y = groundY
                player.isClimbing = false
                player.isGrounded = true
                player.velY = 0f
            }
            player.speed = abs(verticalInput * climbSpeed)
            return
        }

        // 3. Smooth Ground & Airborne Movement with Momentum Inertia, Turn Banking & Wall Sliding
        val jx = input.joyX
        val jy = input.joyY
        val mag = hypot(jx, jy).coerceAtMost(1f)
        val punchSlowdown = if (player.punchTimer > 0.08f) 0.35f else 1f

        var targetVx = 0f
        var targetVz = 0f
        var targetLeanRoll = 0f

        if (mag > 0.08f) {
            val baseSpeed = (if (input.sprint) 9.8f else 5.1f) * punchSlowdown
            val maxMoveSpeed = if (player.isGrounded) baseSpeed else baseSpeed * 0.86f
            val forwardX = sin(cameraYaw); val forwardZ = cos(cameraYaw)
            val rightX = -cos(cameraYaw); val rightZ = sin(cameraYaw)
            val moveX = forwardX * jy + rightX * jx
            val moveZ = forwardZ * jy + rightZ * jx
            val targetHeading = atan2(moveX, moveZ)

            var angleDiff = (targetHeading - player.headingRad) % (2f * PI.toFloat())
            if (angleDiff > PI) angleDiff -= 2f * PI.toFloat()
            if (angleDiff < -PI) angleDiff += 2f * PI.toFloat()

            player.headingRad = lerpAngle(player.headingRad, targetHeading, (dt * 11.5f).coerceAtMost(1f))

            val spd = mag * maxMoveSpeed
            targetVx = sin(targetHeading) * spd
            targetVz = cos(targetHeading) * spd
            // Natural athletic body banking into turns (stronger when sprinting)
            targetLeanRoll = (angleDiff * spd * 1.45f).coerceIn(-11.5f, 11.5f)
        }

        val accelRate = when {
            !player.isGrounded -> 5.8f
            mag > 0.08f -> 14.5f
            else -> 12.0f
        }
        val blend = (dt * accelRate).coerceAtMost(1f)
        player.velX += (targetVx - player.velX) * blend
        player.velZ += (targetVz - player.velZ) * blend
        player.leanRoll += (targetLeanRoll - player.leanRoll) * (dt * 9.5f).coerceAtMost(1f)

        val horizSpeed = hypot(player.velX, player.velZ)
        if (horizSpeed < 0.05f && mag <= 0.08f) {
            player.velX = 0f
            player.velZ = 0f
            player.speed = 0f
        } else {
            player.speed = horizSpeed
            val nx = player.x + player.velX * dt
            val nz = player.z + player.velZ * dt
            when {
                canStandAtXZ(nx, nz, player.y) -> {
                    player.x = nx
                    player.z = nz
                }
                canStandAtXZ(nx, player.z, player.y) -> {
                    player.x = nx
                    player.velZ = 0f
                }
                canStandAtXZ(player.x, nz, player.y) -> {
                    player.z = nz
                    player.velX = 0f
                }
                else -> {
                    player.velX *= 0.25f
                    player.velZ *= 0.25f
                }
            }
            if (player.isGrounded) {
                player.walkPhase += dt * horizSpeed * 2.15f
            }
        }

        // Probe climbable walls/ledges ahead (checks both character heading and camera forward!)
        val climbTarget = colliderGrid.findClimbableWallAhead(player.x, player.z, player.y, sin(player.headingRad), cos(player.headingRad), 1.80f)
            ?: colliderGrid.findClimbableWallAhead(player.x, player.z, player.y, sin(cameraYaw), cos(cameraYaw), 1.65f)
        canClimbAheadNow = climbTarget != null

        // Initiate Climb or Quick Ledge Vault when holding ESCALAR!
        if (input.climbHold && climbTarget != null) {
            val heightToTop = climbTarget.wallTopY - player.y
            if (heightToTop <= 3.1f) {
                player.headingRad = atan2(-climbTarget.normalX, -climbTarget.normalZ)
                startMantle(climbTarget.roofLandX, climbTarget.wallTopY, climbTarget.roofLandZ)
                return
            } else {
                player.isClimbing = true
                player.isGrounded = false
                player.velX = 0f
                player.velY = 0f
                player.velZ = 0f
                player.x = climbTarget.attachX
                player.z = climbTarget.attachZ
                player.climbWallTopY = climbTarget.wallTopY
                player.climbLandX = climbTarget.roofLandX
                player.climbLandZ = climbTarget.roofLandZ
                player.climbNormalX = climbTarget.normalX
                player.climbNormalZ = climbTarget.normalZ
                player.headingRad = atan2(-climbTarget.normalX, -climbTarget.normalZ)
                climbSfxAccum = 0f
                sound.playClimbGrab()
                return
            }
        }

        // Jump action!
        if (jumpPressed) {
            if (climbTarget != null && (climbTarget.wallTopY - player.y) in 0.4f..2.6f && mag > 0.1f) {
                // Smooth parkour vault directly onto low roof/ledge!
                player.headingRad = atan2(-climbTarget.normalX, -climbTarget.normalZ)
                startMantle(climbTarget.roofLandX, climbTarget.wallTopY, climbTarget.roofLandZ)
                return
            } else if (player.isGrounded) {
                player.velY = 9.6f
                player.isGrounded = false
                sound.playJump()
            } else if (climbTarget != null) {
                // Mid-air jump press next to a wall latches straight into a climb!
                player.isClimbing = true
                player.velX = 0f
                player.velY = 0f
                player.velZ = 0f
                player.x = climbTarget.attachX
                player.z = climbTarget.attachZ
                player.climbWallTopY = climbTarget.wallTopY
                player.climbLandX = climbTarget.roofLandX
                player.climbLandZ = climbTarget.roofLandZ
                player.climbNormalX = climbTarget.normalX
                player.climbNormalZ = climbTarget.normalZ
                player.headingRad = atan2(-climbTarget.normalX, -climbTarget.normalZ)
                sound.playClimbGrab()
                return
            }
        }

        // Auto-mantle assist when airborne & pushing forward right at a rooftop edge!
        if (!player.isGrounded && climbTarget != null && jy > 0.25f) {
            val hDiff = climbTarget.wallTopY - player.y
            if (hDiff in 0.2f..1.95f) {
                player.headingRad = atan2(-climbTarget.normalX, -climbTarget.normalZ)
                startMantle(climbTarget.roofLandX, climbTarget.wallTopY, climbTarget.roofLandZ)
                return
            }
        }

        // Vertical Gravity & Landing onto Terrain, Bridges, Building Rooftops, or Car Roofs!
        val supportY = supportSurfaceY(player.x, player.z, player.y)
        if (!player.isGrounded || player.y > supportY + 0.14f) {
            player.isGrounded = false
            player.velY = (player.velY - 24.5f * dt).coerceAtLeast(-28f)
            player.y += player.velY * dt
            val landY = supportSurfaceY(player.x, player.z, player.y)
            if (player.y <= landY) {
                val impactVel = abs(player.velY)
                if (impactVel > 3.5f) {
                    player.landingSquash = (impactVel / 13.5f).coerceIn(0.25f, 1.0f)
                }
                player.y = landY
                player.velY = 0f
                player.isGrounded = true
            }
        } else {
            player.y = supportY
            player.velY = 0f
            player.isGrounded = true
        }

        player.nitro = (player.nitro + dt * 10f).coerceAtMost(100f)
    }

    private fun updateDrivingPlayer(car: VehicleEntity, dt: Float) {
        val accelInput = (if (input.accelerate) 1f else 0f) + input.joyY.coerceAtLeast(0f) * 0.7f
        val brakeInput = (if (input.brakeReverse) 1f else 0f) + (-input.joyY).coerceAtLeast(0f) * 0.7f

        // Crisp combined steering: dedicated Left/Right mobile driving buttons OR analog stick!
        val buttonSteer = (if (input.steerLeft) 1f else 0f) + (if (input.steerRight) -1f else 0f)
        val steerInput = (buttonSteer - input.joyX).coerceIn(-1f, 1f)

        val usingNitro = input.nitro && player.nitro > 4f && (accelInput > 0.1f || car.speed > 4f)
        if (usingNitro) {
            if (player.nitro > 95f) sound.playNitroBurst()
            player.nitro = (player.nitro - dt * 28f).coerceAtLeast(0f)
        } else {
            player.nitro = (player.nitro + dt * 9f).coerceAtMost(100f)
        }

        val maxSpd = car.type.maxSpeed * (if (usingNitro) 1.38f else 1.0f)
        val accelRate = car.type.accel * (if (usingNitro) 1.85f else 1.0f)

        var pitchTarget = 0f
        if (accelInput > 0.05f) {
            car.speed += accelRate * accelInput.coerceAtMost(1f) * dt
            pitchTarget = if (usingNitro) -0.045f else -0.022f
        } else if (brakeInput > 0.05f) {
            if (car.speed > 1.5f) {
                car.speed -= 36f * brakeInput.coerceAtMost(1f) * dt
                pitchTarget = 0.036f
            } else {
                car.speed -= 14f * brakeInput.coerceAtMost(1f) * dt
            }
        } else {
            car.speed *= (1f - dt * 1.45f).coerceIn(0f, 1f)
            if (abs(car.speed) < 0.15f) car.speed = 0f
        }

        if (input.drift && abs(car.speed) > 6f) {
            car.speed *= (1f - dt * 0.80f).coerceIn(0f, 1f)
        }
        car.speed = car.speed.coerceIn(-16f, maxSpd)

        val targetWheelSteer = steerInput * 0.50f
        car.steerAngle += (targetWheelSteer - car.steerAngle) * (dt * 12f).coerceAtMost(1f)

        val speedFactor = (abs(car.speed) / 8f).coerceIn(0f, 1f)
        val driftBoost = if (input.drift) 1.45f else 1.0f
        val dirSign = if (car.speed >= 0f) 1f else -1f
        val turnRate = 1.95f * driftBoost * speedFactor * (1f - (abs(car.speed) / 88f).coerceAtMost(0.45f))
        car.headingRad += steerInput * turnRate * dirSign * dt

        val rollTarget = -steerInput * speedFactor * (if (input.drift) 0.085f else 0.048f)
        car.bodyRollRad += (rollTarget - car.bodyRollRad) * (dt * 8f).coerceAtMost(1f)
        car.bodyPitchRad += (pitchTarget - car.bodyPitchRad) * (dt * 7f).coerceAtMost(1f)

        val nx = car.x + sin(car.headingRad) * car.speed * dt
        val nz = (car.z + cos(car.headingRad) * car.speed * dt)
            .coerceIn(VallartaGeography.WORLD_Z_NORTH + 25f, VallartaGeography.WORLD_Z_SOUTH - 25f)

        val clampedX = nx.coerceAtMost(VallartaGeography.WORLD_X_INLAND - 40f)
        val sd = VallartaGeography.signedCoastDist(clampedX, nz)
        val beyondInlandLimit = sd > VallartaGeography.MAX_INLAND_DIST_M
        val inWater = sd < 2.0f || VallartaGeography.isInsideMarinaBasin(clampedX, nz)
        if (beyondInlandLimit) {
            car.speed = -car.speed * 0.25f
            if (boundaryToastCooldown <= 0f) {
                showToast("LÍMITE DE PUERTO VALLARTA (5 KM TIERRA ADENTRO)", gold = true)
                boundaryToastCooldown = 3.5f
            }
        } else if (inWater || collidesWithBuilding(clampedX, nz, 2.2f)) {
            car.speed = -car.speed * 0.35f
            car.bodyPitchRad = 0.06f
        } else {
            car.x = clampedX
            car.z = nz
        }

        for (other in vehicles) {
            if (other.id == car.id) continue
            val dx = car.x - other.x
            val dz = car.z - other.z
            val dist = hypot(dx, dz)
            if (dist < 3.6f && dist > 0.01f) {
                val push = (3.6f - dist) * 0.55f
                car.x += (dx / dist) * push
                car.z += (dz / dist) * push
                other.x -= (dx / dist) * push
                other.z -= (dz / dist) * push
                if (abs(car.speed) > 14f) {
                    if (other.type.isPolice) raiseWanted(2, "¡CHOQUE CONTRA PATRULLA!")
                    else if (player.wantedStars == 0 && abs(car.speed) > 26f) raiseWanted(1, "¡CONDUCCIÓN TEMERARIA!")
                }
                car.speed *= 0.62f
                other.speed *= 0.4f
            }
        }

        car.y = CoastalTerrainBuilder.surfaceHeightAt(car.x, car.z)
        car.wheelSpinRad += (car.speed / car.type.wheelRadius) * dt
        if (car.type.isPolice) car.sirenPhase += dt * 10f

        player.x = car.x
        player.y = car.y
        player.z = car.z
        player.headingRad = car.headingRad
        player.speed = abs(car.speed)
    }

    private fun updateTrafficAndPolice(dt: Float) {
        for (v in vehicles) {
            if (v.isPlayerControlled || v.isParked) continue

            val dzPlayer = v.z - player.z
            if (abs(dzPlayer) > 580f) {
                val respawnZ = (player.z + (if (dzPlayer > 0f) -520f else 520f))
                    .coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
                val clearX = findClearBoulevardX(respawnZ, v.laneOffset)
                if (clearX != null) {
                    v.z = respawnZ
                    v.x = clearX
                    v.y = CoastalTerrainBuilder.surfaceHeightAt(v.x, v.z)
                    v.headingRad = VallartaGeography.coastHeadingRad(respawnZ) + if (v.directionSign < 0f) PI.toFloat() else 0f
                }
                continue
            }

            if (v.type.isPolice && player.wantedStars > 0) {
                v.isPoliceChasing = true
                v.sirenPhase += dt * 12f
                val toPlayerX = player.x - v.x
                val toPlayerZ = player.z - v.z
                val dist = hypot(toPlayerX, toPlayerZ)
                val targetAngle = atan2(toPlayerX, toPlayerZ)
                v.headingRad = lerpAngle(v.headingRad, targetAngle, (dt * 3.4f).coerceAtMost(1f))
                val chaseSpeed = (22f + player.wantedStars * 5.2f).coerceAtMost(44f)
                v.speed += ((if (dist > 8f) chaseSpeed else 8f) - v.speed) * (dt * 3f).coerceAtMost(1f)
                val nx = v.x + sin(v.headingRad) * v.speed * dt
                val nz = v.z + cos(v.headingRad) * v.speed * dt
                val gy = CoastalTerrainBuilder.surfaceHeightAt(nx, nz)
                if (!collidesWithBuilding(nx, nz, 2.5f, gy) &&
                    VallartaGeography.signedCoastDist(nx, nz) > 5f &&
                    !VallartaGeography.isInsideMarinaBasin(nx, nz)
                ) {
                    v.x = nx; v.z = nz
                } else {
                    v.speed *= 0.45f
                }
                v.y = CoastalTerrainBuilder.surfaceHeightAt(v.x, v.z)
                v.wheelSpinRad += (v.speed / v.type.wheelRadius) * dt

                if (dist < 4.5f && player.speed < 3f) {
                    player.health = (player.health - dt * 8f).coerceAtLeast(15f)
                }
                continue
            } else {
                v.isPoliceChasing = false
                if (v.type.isPolice) v.sirenPhase += dt * 3f
            }

            val aheadZ = (v.z + v.directionSign * 14f)
                .coerceIn(VallartaGeography.WORLD_Z_NORTH + 30f, VallartaGeography.WORLD_Z_SOUTH - 30f)
            val clearTargetX = findClearBoulevardX(aheadZ, v.laneOffset, 2.6f)
                ?: VallartaGeography.mainBoulevardX(aheadZ)
            val targetAngle = atan2(clearTargetX - v.x, aheadZ - v.z)
            v.headingRad = lerpAngle(v.headingRad, targetAngle, (dt * 5.2f).coerceAtMost(1f))

            // Look-ahead obstacle avoidance so NPC traffic NEVER hits or clips into hotels/buildings!
            val probeX = v.x + sin(v.headingRad) * 5.2f
            val probeZ = v.z + cos(v.headingRad) * 5.2f
            val probeGy = CoastalTerrainBuilder.surfaceHeightAt(probeX, probeZ)
            val obstacleAhead = collidesWithBuilding(probeX, probeZ, 2.4f, probeGy) ||
                VallartaGeography.isInsideMarinaBasin(probeX, probeZ) ||
                VallartaGeography.signedCoastDist(probeX, probeZ) < 6f

            val distPlayer = hypot(player.x - v.x, player.z - v.z)
            val cruiseSpeed = when (v.type) {
                VehicleType.BUS -> 10.5f
                VehicleType.SUPERCAR -> 18.5f
                else -> 14.5f
            }
            val desired = when {
                distPlayer < 12f -> 0f
                obstacleAhead -> 3.5f
                else -> cruiseSpeed
            }
            v.speed += (desired - v.speed) * (dt * 3.8f).coerceAtMost(1f)

            val nx = v.x + sin(v.headingRad) * v.speed * dt
            val nz = (v.z + cos(v.headingRad) * v.speed * dt)
                .coerceIn(VallartaGeography.WORLD_Z_NORTH + 25f, VallartaGeography.WORLD_Z_SOUTH - 25f)
            val gy = CoastalTerrainBuilder.surfaceHeightAt(nx, nz)
            if (!collidesWithBuilding(nx, nz, 2.5f, gy) &&
                VallartaGeography.signedCoastDist(nx, nz) > 6f &&
                !VallartaGeography.isInsideMarinaBasin(nx, nz)
            ) {
                v.x = nx
                v.z = nz
            } else {
                // Steer firmly onto verified clear boulevard center or cleanly recycle behind traffic
                val safeX = findClearBoulevardX(v.z, 0f, 2.6f)
                if (safeX != null) {
                    v.x += (safeX - v.x) * (dt * 6f).coerceAtMost(1f)
                    v.speed *= 0.45f
                } else {
                    val recycleZ = (player.z + (if (v.directionSign > 0f) -420f else 420f))
                        .coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
                    val recX = findClearBoulevardX(recycleZ, v.laneOffset)
                    if (recX != null) {
                        v.x = recX
                        v.z = recycleZ
                    }
                }
            }
            v.y = CoastalTerrainBuilder.surfaceHeightAt(v.x, v.z)
            v.wheelSpinRad += (v.speed / v.type.wheelRadius) * dt

            if (v.z <= VallartaGeography.WORLD_Z_NORTH + 40f || v.z >= VallartaGeography.WORLD_Z_SOUTH - 40f) {
                v.directionSign = -v.directionSign
                v.laneOffset = -v.laneOffset
            }
        }
    }

    private fun updatePedestrians(dt: Float) {
        var idx = 0
        for (p in pedestrians) {
            val dzPlayer = p.z - player.z
            val dxPlayer = p.x - player.x
            if (abs(dzPlayer) > 240f || abs(dxPlayer) > 520f) {
                val newZ = (player.z + (if (dzPlayer > 0f) -205f else 205f))
                    .coerceIn(VallartaGeography.WORLD_Z_NORTH + 40f, VallartaGeography.WORLD_Z_SOUTH - 40f)
                placePedestrianCleanly(p, newZ, idx)
                idx++
                continue
            }

            val distToPlayer = hypot(dxPlayer, dzPlayer)
            if (distToPlayer < 9f && player.speed > 12f) {
                p.panicTimer = 4.5f
            }

            if (p.staggerTimer > 0f) {
                p.staggerTimer = (p.staggerTimer - dt).coerceAtLeast(0f)
                val nx = p.x + p.knockVelX * dt
                val nz = p.z + p.knockVelZ * dt
                if (!collidesWithBuilding(nx, nz, 0.8f) &&
                    VallartaGeography.signedCoastDist(nx, nz) > 3.5f &&
                    !VallartaGeography.isInsideMarinaBasin(nx, nz)
                ) {
                    p.x = nx
                    p.z = nz
                } else {
                    p.knockVelX *= -0.25f
                    p.knockVelZ *= -0.25f
                }
                val damp = exp(-6.8f * dt)
                p.knockVelX *= damp
                p.knockVelZ *= damp
                // Face roughly toward attacker while reeling backwards
                val facePlayer = atan2(player.x - p.x, player.z - p.z)
                p.headingRad = lerpAngle(p.headingRad, facePlayer, (dt * 7f).coerceAtMost(1f))
                p.y = CoastalTerrainBuilder.surfaceHeightAt(p.x, p.z)
                idx++
                continue
            }

            if (p.panicTimer > 0f) {
                p.panicTimer -= dt
                val awayAngle = atan2(p.x - player.x, p.z - player.z)
                p.headingRad = lerpAngle(p.headingRad, awayAngle, (dt * 6f).coerceAtMost(1f))
                val runSpd = 5.4f
                val nx = p.x + sin(p.headingRad) * runSpd * dt
                val nz = p.z + cos(p.headingRad) * runSpd * dt
                if (!collidesWithBuilding(nx, nz, 0.8f) &&
                    VallartaGeography.signedCoastDist(nx, nz) > 3.5f &&
                    !VallartaGeography.isInsideMarinaBasin(nx, nz)
                ) {
                    p.x = nx; p.z = nz
                }
                p.walkPhase += dt * runSpd * 2.2f
            } else {
                val aheadZ = (p.z + p.directionSign * 8f)
                    .coerceIn(VallartaGeography.WORLD_Z_NORTH + 35f, VallartaGeography.WORLD_Z_SOUTH - 35f)
                val targetX: Float
                val targetZ: Float
                when (p.zone) {
                    PedestrianZone.BEACH_SHORE -> {
                        val h = VallartaGeography.coastHeadingRad(aheadZ)
                        targetX = VallartaGeography.coastX(aheadZ) + cos(h) * p.sideOffset
                        targetZ = aheadZ - sin(h) * p.sideOffset
                    }
                    PedestrianZone.MALECON_MARINA -> {
                        if (isNearMarinaZone(p.z) && p.x > VallartaGeography.coastX(p.z) + 180f) {
                            // Stroll gently along Marina Vallarta boardwalk / Flamingo quay / Vamar peninsula
                            targetX = p.x + sin(p.headingRad) * 5f
                            targetZ = p.z + cos(p.headingRad) * 5f
                        } else {
                            val h = VallartaGeography.coastHeadingRad(aheadZ)
                            targetX = VallartaGeography.coastX(aheadZ) + cos(h) * p.sideOffset
                            targetZ = aheadZ - sin(h) * p.sideOffset
                        }
                    }
                    PedestrianZone.BOULEVARD_SIDEWALK -> {
                        targetX = VallartaGeography.mainBoulevardX(aheadZ) + p.sideOffset
                        targetZ = aheadZ
                    }
                }
                val desiredHeading = atan2(targetX - p.x, targetZ - p.z)
                p.headingRad = lerpAngle(p.headingRad, desiredHeading, (dt * 4.5f).coerceAtMost(1f))

                val nx = p.x + sin(p.headingRad) * p.speed * dt
                val nz = p.z + cos(p.headingRad) * p.speed * dt
                val dry = VallartaGeography.signedCoastDist(nx, nz) > 4.2f &&
                    !VallartaGeography.isInsideMarinaBasin(nx, nz)
                if (dry && !collidesWithBuilding(nx, nz, 0.72f)) {
                    p.x = nx
                    p.z = nz
                } else {
                    // Turn smoothly around obstacle or water edge instead of ever clipping through buildings/water
                    p.headingRad += 1.65f
                    p.directionSign = -p.directionSign
                }
                p.walkPhase += dt * p.speed * 2.3f
            }
            p.y = CoastalTerrainBuilder.surfaceHeightAt(p.x, p.z)
            idx++
        }
    }

    private fun updateWantedSystem(dt: Float) {
        if (player.wantedStars <= 0) return
        sirenChirpAccum += dt
        if (sirenChirpAccum > 6.5f) {
            sirenChirpAccum = 0f
            sound.playPoliceChirp()
        }
        val policeNearby = vehicles.any {
            it.type.isPolice && !it.isPlayerControlled && hypot(it.x - player.x, it.z - player.z) < 75f
        }
        if (policeNearby) {
            player.wantedCooldown = 18f
        } else {
            player.wantedCooldown -= dt
            if (player.wantedCooldown <= 0f) {
                player.wantedStars = (player.wantedStars - 1).coerceAtLeast(0)
                player.wantedCooldown = if (player.wantedStars > 0) 14f else 0f
                if (player.wantedStars == 0) {
                    showToast("PERSECUCIÓN EVADIDA", gold = true)
                }
            }
        }
    }

    private fun updateMissionProgress(dt: Float) {
        val m = activeMission ?: return
        m.timeRemaining -= dt
        val dist = hypot(player.x - m.targetX, player.z - m.targetZ)
        if (dist < 22f) {
            player.cash += m.mission.rewardCash
            completedMissions.add(m.mission.id)
            sound.playMissionComplete()
            showToast("¡MISIÓN CUMPLIDA! +$${m.mission.rewardCash} MXN", gold = true, duration = 4.5f)
            activeMission = null
            pushHudSnapshot()
        } else if (m.timeRemaining <= 0f) {
            showToast("TIEMPO AGOTADO — MISIÓN FALLIDA", gold = false, duration = 3.8f)
            activeMission = null
            pushHudSnapshot()
        }
    }

    private fun raiseWanted(minStars: Int, reason: String) {
        val prev = player.wantedStars
        player.wantedStars = max(player.wantedStars, minStars).coerceAtMost(5)
        player.wantedCooldown = 22f
        if (player.wantedStars > prev) {
            sound.playPoliceChirp()
        }
        showToast(reason, gold = false)
    }

    private fun findNearestVehicle(maxDist: Float): VehicleEntity? {
        var best: VehicleEntity? = null
        var bestD = maxDist
        for (v in vehicles) {
            if (v.isPlayerControlled) continue
            val d = hypot(v.x - player.x, v.z - player.z)
            if (d < bestD) {
                bestD = d
                best = v
            }
        }
        return best
    }

    private fun findNearestNamedLandmark(): String? {
        var bestName: String? = null
        var bestDist = 175f
        for (b in buildings) {
            if (!b.showPin) continue
            val d = hypot(b.x - player.x, b.z - player.z)
            if (d < bestDist) {
                bestDist = d
                bestName = "${b.name} • ${d.toInt()}m"
            }
        }
        for (p in osmNamedPlaces) {
            val d = hypot(p.x - player.x, p.z - player.z)
            if (d < bestDist) {
                bestDist = d
                bestName = "${p.name} • ${d.toInt()}m"
            }
        }
        return bestName
    }

    private fun collidesWithBuilding(x: Float, z: Float, margin: Float, footY: Float = CoastalTerrainBuilder.surfaceHeightAt(x, z)): Boolean {
        return colliderGrid.collidesHorizontally(
            x = x,
            z = z,
            footY = footY,
            headY = footY + 1.85f,
            radius = margin
        )
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = (b - a) % (2f * PI.toFloat())
        if (diff > PI) diff -= 2f * PI.toFloat()
        if (diff < -PI) diff += 2f * PI.toFloat()
        return a + diff * t
    }

    fun getActiveMissionState(): ActiveMissionState? = activeMission

    private fun pushHudSnapshot() {
        val district = VallartaGeography.districtAt(player.x, player.z)
        val curVeh = vehicles.firstOrNull { it.id == player.vehicleId }
        val nearbyVeh = if (curVeh == null) findNearestVehicle(7.2f) else null
        val m = activeMission
        val mDist = if (m != null) hypot(player.x - m.targetX, player.z - m.targetZ).toInt() else 0

        val blips = mutableListOf<MinimapBlip>()
        for (v in vehicles) {
            if (v.isPlayerControlled) continue
            if (abs(v.z - player.z) < 340f && abs(v.x - player.x) < 340f) {
                blips += MinimapBlip(v.x, v.z, isPolice = v.type.isPolice, isVehicle = true)
            }
        }

        _hudState.value = HudUiState(
            playerX = player.x,
            playerZ = player.z,
            playerHeadingRad = player.headingRad,
            speedKmh = (player.speed * 4.4f).toInt(),
            inVehicle = curVeh != null,
            currentVehicleName = curVeh?.type?.displayName ?: "",
            nearbyVehicleName = nearbyVeh?.type?.displayName,
            health = player.health.toInt(),
            nitro = player.nitro.toInt(),
            cash = player.cash,
            wantedStars = player.wantedStars,
            districtName = district.name,
            districtSubtitle = district.subtitle,
            currentStreetName = PrebakedRoadNetworkLoader.lookupStreetName(player.x, player.z),
            nearbyLandmarkLabel = findNearestNamedLandmark(),
            gpsLat = VallartaGeography.worldToLat(player.z),
            gpsLon = VallartaGeography.worldToLon(player.x),
            timeOfDay = timeOfDay,
            cameraMode = cameraMode,
            radioStation = radioStation,
            activeMission = m?.copy(),
            missionDistanceMeters = mDist,
            completedMissionIds = completedMissions.toSet(),
            toastBanner = toastText,
            toastAccentGold = toastGold,
            osmStatus = osmStatusText,
            osmBuildingCount = osmAddedCount,
            isSyncingOsm = isOsmSyncing,
            soundMuted = sound.muted,
            canClimbAhead = canClimbAheadNow,
            isClimbing = player.isClimbing || player.isMantling,
            isAirborne = !player.isGrounded && player.vehicleId == null,
            nearbyBlips = blips,
            projectedPins = projectedPins
        )
    }
}
