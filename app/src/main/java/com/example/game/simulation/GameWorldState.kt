package com.example.game.simulation

import com.example.game.engine.VehicleType
import com.example.game.world.CameraMode
import com.example.game.world.MissionDef
import com.example.game.world.RadioStation
import com.example.game.world.TimeOfDay
import com.example.game.world.VallartaGeography

data class VehicleEntity(
    val id: Int,
    val type: VehicleType,
    val colorIndex: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    var headingRad: Float,
    var speed: Float = 0f,
    var steerAngle: Float = 0f,
    var wheelSpinRad: Float = 0f,
    var bodyRollRad: Float = 0f,
    var bodyPitchRad: Float = 0f,
    var laneOffset: Float = 4.6f,
    var directionSign: Float = 1f,
    var isPlayerControlled: Boolean = false,
    var isPoliceChasing: Boolean = false,
    var isParked: Boolean = false,
    var occupiedByNpc: Boolean = true,
    var sirenPhase: Float = 0f
)

enum class PedestrianArchetype {
    TOURIST_CORAL,
    LOCAL_DRESS,
    LOCAL_GUAYABERA,
    POLICE_OFFICER,
    BEACH_SWIM_SHORTS,
    BEACH_BIKINI_SARONG,
    BEACH_HAWAIIAN_SHIRT,
    BEACH_SUNDRESS
}

enum class PedestrianZone {
    BEACH_SHORE,
    MALECON_MARINA,
    BOULEVARD_SIDEWALK
}

data class PedestrianEntity(
    val id: Int,
    val archetype: PedestrianArchetype,
    var x: Float,
    var y: Float,
    var z: Float,
    var headingRad: Float,
    var speed: Float,
    var walkPhase: Float,
    var sideOffset: Float,
    var directionSign: Float,
    var panicTimer: Float = 0f,
    val zone: PedestrianZone = PedestrianZone.BOULEVARD_SIDEWALK,
    var staggerTimer: Float = 0f,
    var knockVelX: Float = 0f,
    var knockVelZ: Float = 0f
)

/**
 * Sleek 3D-to-2D projected architectural landmark marker (replaces bulky 3D signboards!).
 */
data class ProjectedLandmarkPin(
    val id: String,
    val name: String,
    val screenXNorm: Float, // 0f left .. 1f right
    val screenYNorm: Float, // 0f top .. 1f bottom
    val distanceMeters: Int,
    val isIconic: Boolean = true
)

data class PlayerState(
    // Spawn right on El Malecón facing South toward Los Arcos, Parroquia & Puentes del Río Cuale!
    var x: Float = VallartaGeography.lonToWorldX(-105.2351),
    var y: Float = 2.35f,
    var z: Float = VallartaGeography.latToWorldZ(20.6102),
    var headingRad: Float = 0.20f,
    var speed: Float = 0f,
    var velX: Float = 0f,
    var velZ: Float = 0f,
    var leanRoll: Float = 0f,
    var landingSquash: Float = 0f,
    var walkPhase: Float = 0f,
    var punchTimer: Float = 0f,
    var punchComboIndex: Int = 0,
    var punchHitDelivered: Boolean = false,
    var punchComboResetTimer: Float = 0f,
    var cameraPunchShake: Float = 0f,
    var velY: Float = 0f,
    var isGrounded: Boolean = true,
    var isClimbing: Boolean = false,
    var climbPhase: Float = 0f,
    var climbWallTopY: Float = 0f,
    var climbLandX: Float = 0f,
    var climbLandZ: Float = 0f,
    var climbNormalX: Float = 0f,
    var climbNormalZ: Float = 0f,
    var isMantling: Boolean = false,
    var mantleProgress: Float = 0f,
    var mantleStartX: Float = 0f,
    var mantleStartY: Float = 0f,
    var mantleStartZ: Float = 0f,
    var mantleEndX: Float = 0f,
    var mantleEndY: Float = 0f,
    var mantleEndZ: Float = 0f,
    var vehicleId: Int? = null,
    var health: Float = 100f,
    var nitro: Float = 100f,
    var cash: Int = 2500,
    var wantedStars: Int = 0,
    var wantedCooldown: Float = 0f
)

data class ActiveMissionState(
    val mission: MissionDef,
    var timeRemaining: Float,
    val targetX: Float,
    val targetZ: Float
)

data class MinimapBlip(
    val x: Float,
    val z: Float,
    val isPolice: Boolean,
    val isVehicle: Boolean
)

/**
 * Immutable UI snapshot pushed smoothly (~20Hz) from the 60fps simulation engine to Jetpack Compose HUD.
 */
data class HudUiState(
    val playerX: Float = VallartaGeography.lonToWorldX(-105.2351),
    val playerZ: Float = VallartaGeography.latToWorldZ(20.6102),
    val playerHeadingRad: Float = 0.20f,
    val speedKmh: Int = 0,
    val inVehicle: Boolean = false,
    val currentVehicleName: String = "",
    val nearbyVehicleName: String? = null,
    val health: Int = 100,
    val nitro: Int = 100,
    val cash: Int = 2500,
    val wantedStars: Int = 0,
    val districtName: String = "El Malecón y Centro",
    val districtSubtitle: String = "Hotel Rosita • El Caballito • Parroquia • Los Arcos",
    val currentStreetName: String = "Paseo Díaz Ordaz (El Malecón)",
    val nearbyLandmarkLabel: String? = null,
    val gpsLat: Double = 20.6102,
    val gpsLon: Double = -105.2351,
    val timeOfDay: TimeOfDay = TimeOfDay.SUNSET,
    val cameraMode: CameraMode = CameraMode.CHASE,
    val radioStation: RadioStation = RadioStation.BAHIA_SYNTH,
    val activeMission: ActiveMissionState? = null,
    val missionDistanceMeters: Int = 0,
    val completedMissionIds: Set<Int> = emptySet(),
    val toastBanner: String? = null,
    val toastAccentGold: Boolean = false,
    val osmStatus: String = "PV 3D 1:1 • Calles y Hoteles Reales",
    val osmBuildingCount: Int = 0,
    val isSyncingOsm: Boolean = false,
    val soundMuted: Boolean = false,
    val canClimbAhead: Boolean = false,
    val isClimbing: Boolean = false,
    val isAirborne: Boolean = false,
    val nearbyBlips: List<MinimapBlip> = emptyList(),
    val projectedPins: List<ProjectedLandmarkPin> = emptyList()
)

/**
 * Lock-free volatile touch input state written by Compose touch handlers and read by the 60fps engine.
 */
class GameInputState {
    @Volatile var joyX: Float = 0f
    @Volatile var joyY: Float = 0f
    @Volatile var steerLeft: Boolean = false
    @Volatile var steerRight: Boolean = false
    @Volatile var accelerate: Boolean = false
    @Volatile var brakeReverse: Boolean = false
    @Volatile var drift: Boolean = false
    @Volatile var nitro: Boolean = false
    @Volatile var sprint: Boolean = false
    @Volatile var climbHold: Boolean = false
    @Volatile var cameraYawDelta: Float = 0f
    @Volatile var cameraPitchDelta: Float = 0f

    @Volatile var pendingEnterExit: Boolean = false
    @Volatile var pendingAttack: Boolean = false
    @Volatile var pendingJump: Boolean = false
    @Volatile var pendingHorn: Boolean = false
}
