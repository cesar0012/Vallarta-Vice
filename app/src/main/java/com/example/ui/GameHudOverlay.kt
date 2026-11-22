package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.simulation.GameInputState
import com.example.game.simulation.HudUiState
import com.example.game.simulation.ProjectedLandmarkPin
import com.example.game.world.TimeOfDay
import com.example.game.world.VallartaGeography
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun GameHudOverlay(
    hud: HudUiState,
    input: GameInputState,
    onOpenMap: () -> Unit,
    onOpenMissions: () -> Unit,
    onCycleCamera: () -> Unit,
    onCycleTime: () -> Unit,
    onCycleRadio: () -> Unit,
    onToggleMute: () -> Unit,
    onSpawnCar: () -> Unit
) {
    var useAnalogForDriving by remember { mutableStateOf(false) }

    // Ensure steering flags never stick when toggling control mode or exiting a vehicle
    DisposableEffect(hud.inVehicle, useAnalogForDriving) {
        onDispose {
            input.steerLeft = false
            input.steerRight = false
            input.climbHold = false
            input.joyX = 0f
            input.joyY = 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ==================== ELEGANT FLOATING ARCHITECTURAL LANDMARK PINS ====================
        // Replaces clunky 3D signboards with sleek Google-Maps-3D / GTA projected glass pills!
        ProjectedLandmarkPinLayer(pins = hud.projectedPins)

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Open viewport touch drag orbits camera smoothly in both Yaw (360°) and Pitch!
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        input.cameraYawDelta += -dragAmount.x * 0.0052f
                        input.cameraPitchDelta += dragAmount.y * 0.0038f
                    }
                }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ==================== TOP BAR: MINIMAP + DISTRICT (LEFT) & STATS + TOOLS (RIGHT) ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Live Circular Puerto Vallarta Radar + District / GPS Banner
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VallartaRadarMinimap(
                    hud = hud,
                    onClick = onOpenMap
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.widthIn(max = 185.dp)
                ) {
                    Surface(
                        color = DarkGlass,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenMap)
                            .testTag("district_badge")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                text = hud.districtName.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                                color = TropicalGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = String.format(Locale.US, "%.4f°N  %.4f°W", hud.gpsLat, hud.gpsLon),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                color = MutedMist
                            )
                        }
                    }

                    // Live Real Puerto Vallarta Street / Beach Name Indicator
                    Surface(
                        color = Color(0xE0141A24),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TropicalGold.copy(alpha = 0.72f)),
                        modifier = Modifier
                            .widthIn(max = 195.dp)
                            .testTag("current_street_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Signpost,
                                contentDescription = "Calle actual",
                                tint = TropicalGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = hud.currentStreetName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = SandWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Live Nearby Hotel / Real Puerto Vallarta Landmark Proximity Pill
                    AnimatedVisibility(
                        visible = hud.nearbyLandmarkLabel != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = Color(0xDD101C2E),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BanderasTurquoise.copy(alpha = 0.8f)),
                            modifier = Modifier.testTag("nearby_landmark_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationCity,
                                    contentDescription = null,
                                    tint = BanderasTurquoise,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = hud.nearbyLandmarkLabel.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SandWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Active Mission Objective Tracker
                    hud.activeMission?.let { active ->
                        Surface(
                            color = Color(0xDD0B1E28),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, BanderasTurquoise)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null,
                                        tint = BanderasTurquoise,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = active.mission.targetName,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SandWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${hud.missionDistanceMeters}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BanderasTurquoise
                                    )
                                    Text(
                                        text = "${active.timeRemaining.toInt().coerceAtLeast(0)}s",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active.timeRemaining < 15f) PoliceRed else TropicalGold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right: GTA Cash, Wanted Stars, Health/Nitro Bars & Quick Action Pills
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = DarkGlass,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Cash Counter ($MXN)
                        Text(
                            text = "$${String.format(Locale.US, "%,d", hud.cash)} MXN",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp, lineHeight = 22.sp),
                            color = CashEmerald
                        )

                        // 5 GTA Wanted Stars ★
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (i in 1..5) {
                                val lit = i <= hud.wantedStars
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = stringResource(R.string.hud_wanted),
                                    tint = when {
                                        !lit -> Color(0x33FFFFFF)
                                        (System.currentTimeMillis() / 280L) % 2L == 0L -> PoliceRed
                                        else -> PoliceBlue
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Compact Health & Nitro progress bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HudMiniBar(progress = hud.health / 100f, color = CashEmerald, width = 54.dp)
                            HudMiniBar(progress = hud.nitro / 100f, color = BanderasTurquoise, width = 44.dp)
                        }
                    }
                }

                // Compact Icon Toolbar: Missions, Camera, Time of Day, Radio, Sound
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HudSmallIconButton(
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        contentDescription = stringResource(R.string.cd_missions_button),
                        tint = TropicalGold,
                        tag = "missions_button",
                        onClick = onOpenMissions
                    )
                    HudSmallIconButton(
                        icon = Icons.Rounded.Videocam,
                        contentDescription = stringResource(R.string.cd_camera_toggle),
                        tint = SandWhite,
                        tag = "camera_button",
                        onClick = onCycleCamera
                    )
                    HudSmallIconButton(
                        icon = when (hud.timeOfDay) {
                            TimeOfDay.SUNSET -> Icons.Rounded.WbTwilight
                            TimeOfDay.DAY -> Icons.Rounded.WbSunny
                            TimeOfDay.NIGHT -> Icons.Rounded.NightsStay
                        },
                        contentDescription = stringResource(R.string.cd_time_toggle),
                        tint = SunsetCoral,
                        tag = "time_button",
                        onClick = onCycleTime
                    )
                    HudSmallIconButton(
                        icon = Icons.Rounded.Radio,
                        contentDescription = stringResource(R.string.cd_radio_toggle),
                        tint = BanderasTurquoise,
                        tag = "radio_button",
                        onClick = onCycleRadio
                    )
                    HudSmallIconButton(
                        icon = if (hud.soundMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = stringResource(R.string.action_radio),
                        tint = MutedMist,
                        tag = "mute_button",
                        onClick = onToggleMute
                    )
                }
            }
        }

        // ==================== CENTER TOP CINEMATIC GTA TOAST BANNER ====================
        AnimatedVisibility(
            visible = hud.toastBanner != null,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 86.dp)
        ) {
            Surface(
                color = Color(0xE60A101E),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (hud.toastAccentGold) TropicalGold else SunsetCoral
                )
            ) {
                Text(
                    text = hud.toastBanner.orEmpty(),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 20.sp),
                    color = if (hud.toastAccentGold) TropicalGold else SandWhite,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp)
                )
            }
        }

        // ==================== BOTTOM CONTROLS: ANALOG STICK (LEFT) & ACTION DECK (RIGHT) ====================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: Crisp Driving Steering Arrows (IZQ/DER + Stick toggle) OR 360° On-Foot Analog Stick
            if (hud.inVehicle && !useAnalogForDriving) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Surface(
                        color = DarkGlass,
                        shape = RoundedCornerShape(100.dp),
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { useAnalogForDriving = true }
                            .testTag("toggle_steer_mode")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SportsEsports,
                                contentDescription = null,
                                tint = BanderasTurquoise,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "STICK",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = SandWhite
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HoldActionButton(
                            label = "IZQ",
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            color = BanderasTurquoise,
                            size = 68.dp,
                            tag = "steer_left_button",
                            onPressedChange = { input.steerLeft = it }
                        )
                        HoldActionButton(
                            label = "DER",
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            color = BanderasTurquoise,
                            size = 68.dp,
                            tag = "steer_right_button",
                            onPressedChange = { input.steerRight = it }
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (hud.inVehicle) {
                        Surface(
                            color = DarkGlass,
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { useAnalogForDriving = false }
                                .testTag("toggle_steer_mode")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Gamepad,
                                    contentDescription = null,
                                    tint = TropicalGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "FLECHAS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SandWhite
                                )
                            }
                        }
                    }
                    VirtualAnalogJoystick(
                        modifier = Modifier.testTag("virtual_joystick"),
                        onStickChange = { x, y ->
                            input.joyX = x
                            input.joyY = y
                        }
                    )
                }
            }

            // Right: Context-Sensitive GTA Controls (Driving vs. On Foot)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Vehicle Status / Speedometer / Enter-Vehicle Prompt Pill
                if (hud.inVehicle) {
                    Surface(
                        color = DarkGlass,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = hud.currentVehicleName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = TropicalGold
                                )
                                Text(
                                    text = hud.radioStation.stationName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = BanderasTurquoise
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${hud.speedKmh}",
                                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp, lineHeight = 30.sp),
                                    color = SandWhite
                                )
                                Text(
                                    text = " KM/H",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = SunsetCoral,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                        }
                    }

                    // Secondary Driving Row: Exit, Horn, Drift, Nitro
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TapActionChip(
                            label = stringResource(R.string.action_exit_vehicle),
                            icon = Icons.AutoMirrored.Rounded.VolumeOff, // overridden below
                            overrideIcon = Icons.Rounded.MeetingRoom,
                            accent = MutedMist,
                            tag = "exit_vehicle_button",
                            onClick = { input.pendingEnterExit = true }
                        )
                        TapActionChip(
                            label = stringResource(R.string.action_horn),
                            overrideIcon = Icons.Rounded.Campaign,
                            accent = TropicalGold,
                            tag = "horn_button",
                            onClick = { input.pendingHorn = true }
                        )
                        HoldActionButton(
                            label = stringResource(R.string.action_drift),
                            icon = Icons.Rounded.Sync,
                            color = MaleconMagenta,
                            size = 54.dp,
                            tag = "drift_button",
                            onPressedChange = { input.drift = it }
                        )
                        HoldActionButton(
                            label = stringResource(R.string.action_nitro),
                            icon = Icons.Rounded.Bolt,
                            color = BanderasTurquoise,
                            size = 54.dp,
                            tag = "nitro_button",
                            onPressedChange = { input.nitro = it }
                        )
                    }

                    // Primary Driving Pedals: Brake/Reverse + Accelerate
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HoldActionButton(
                            label = "FRENO",
                            icon = Icons.Rounded.ArrowDownward,
                            color = PoliceRed,
                            size = 64.dp,
                            tag = "brake_button",
                            onPressedChange = { input.brakeReverse = it }
                        )
                        HoldActionButton(
                            label = "GAS",
                            icon = Icons.Rounded.Speed,
                            color = SunsetCoral,
                            size = 74.dp,
                            tag = "gas_button",
                            onPressedChange = { input.accelerate = it }
                        )
                    }
                } else {
                    // On-Foot Controls: Hijack/Enter Vehicle CTA (highlighted when near any car!), Spawn Car, Sprint, Attack
                    hud.nearbyVehicleName?.let { vehName ->
                        Button(
                            onClick = { input.pendingEnterExit = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SunsetCoral,
                                contentColor = PacificMidnight
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("enter_vehicle_button")
                        ) {
                            Icon(Icons.Rounded.DirectionsCar, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SUBIR • ${vehName.uppercase()}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // On-Foot Controls Row 1: Spawn Car + Contextual ESCALAR (Wall Climb / Ledge Mantle)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TapActionChip(
                            label = "AUTO",
                            overrideIcon = Icons.Rounded.DirectionsCar,
                            accent = TropicalGold,
                            tag = "spawn_car_button",
                            onClick = onSpawnCar
                        )
                        HoldActionButton(
                            label = stringResource(R.string.action_climb),
                            icon = Icons.Rounded.Terrain,
                            color = if (hud.isClimbing || hud.canClimbAhead) CashEmerald else MutedMist,
                            size = 58.dp,
                            tag = "climb_button",
                            onPressedChange = { input.climbHold = it }
                        )
                    }

                    // On-Foot Controls Row 2: Sprint + SALTAR (Jump / Vault) + Attack
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        HoldActionButton(
                            label = stringResource(R.string.action_sprint),
                            icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                            color = BanderasTurquoise,
                            size = 58.dp,
                            tag = "sprint_button",
                            onPressedChange = { input.sprint = it }
                        )
                        Surface(
                            color = TropicalGold.copy(alpha = 0.24f),
                            shape = CircleShape,
                            border = BorderStroke(2.dp, TropicalGold),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable { input.pendingJump = true }
                                .testTag("jump_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = stringResource(R.string.action_jump),
                                    tint = SandWhite,
                                    modifier = Modifier.size(23.dp)
                                )
                                Text(
                                    text = stringResource(R.string.action_jump),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = SandWhite
                                )
                            }
                        }
                        Surface(
                            color = SunsetCoral.copy(alpha = 0.25f),
                            shape = CircleShape,
                            border = BorderStroke(2.dp, SunsetCoral),
                            modifier = Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .clickable { input.pendingAttack = true }
                                .testTag("attack_button")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.SportsMma, contentDescription = stringResource(R.string.action_attack), tint = SandWhite, modifier = Modifier.size(24.dp))
                                Text(
                                    text = stringResource(R.string.action_attack),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = SandWhite
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ProjectedLandmarkPinLayer(pins: List<ProjectedLandmarkPin>) {
    if (pins.isEmpty()) return
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        for (pin in pins) {
            val alpha = (1.0f - (pin.distanceMeters - 160f).coerceAtLeast(0f) / 480f).coerceIn(0.45f, 0.96f)
            val accent = if (pin.isIconic) TropicalGold else BanderasTurquoise
            val xPos = (w * pin.screenXNorm - 76.dp).coerceIn(12.dp, (w - 164.dp).coerceAtLeast(12.dp))
            val yPos = (h * pin.screenYNorm - 42.dp).coerceIn(66.dp, (h - 140.dp).coerceAtLeast(66.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = xPos, y = yPos)
                    .widthIn(min = 96.dp, max = 164.dp)
            ) {
                Surface(
                    color = Color(0xCC091322).copy(alpha = alpha),
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, accent.copy(alpha = alpha * 0.85f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = alpha))
                        )
                        Text(
                            text = pin.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = SandWhite.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "${pin.distanceMeters}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = accent.copy(alpha = alpha)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(1.2.dp)
                        .height(10.dp)
                        .background(accent.copy(alpha = alpha * 0.72f))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun VallartaRadarMinimap(
    hud: HudUiState,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(102.dp)
            .clip(CircleShape)
            .background(Color(0xE6081426))
            .border(2.dp, Brush.sweepGradient(listOf(BanderasTurquoise, SunsetCoral, TropicalGold, BanderasTurquoise)), CircleShape)
            .clickable(onClick = onClick)
            .testTag("minimap_radar"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f
            val range = 420f // 1:1 meters visible radius
            val px = hud.playerX
            val pz = hud.playerZ

            fun toScreen(wx: Float, wz: Float): Offset {
                val sx = cx + ((wx - px) / range) * r
                val sy = cy + ((wz - pz) / range) * r
                return Offset(sx, sy)
            }

            // Fill land vs Pacific Ocean along real 1:1 VallartaGeography.coastX(z)
            val landPath = Path()
            var first = true
            var dz = -range
            while (dz <= range) {
                val wz = pz + dz
                val coastPt = toScreen(VallartaGeography.coastX(wz), wz)
                if (first) {
                    landPath.moveTo(coastPt.x, coastPt.y)
                    first = false
                } else {
                    landPath.lineTo(coastPt.x, coastPt.y)
                }
                dz += 32f
            }
            landPath.lineTo(size.width + 24f, size.height + 24f)
            landPath.lineTo(size.width + 24f, -24f)
            landPath.close()

            // Ocean background + Land mass
            drawCircle(color = Color(0xFF0A2E4D), radius = r)
            drawPath(path = landPath, color = Color(0xFF1D342C))

            // Marina Vallarta harbor water basin (if in range)
            val marinaCenter = toScreen(
                VallartaGeography.lonToWorldX(-105.2528),
                VallartaGeography.latToWorldZ(20.6632)
            )
            if (hypot(marinaCenter.x - cx, marinaCenter.y - cy) < r + 40f) {
                drawCircle(color = Color(0xFF0C486C), radius = (220f / range) * r, center = marinaCenter)
            }

            // Río Cuale river ribbon (if in range)
            if (abs(pz - 250f) < range + 140f) {
                val riverPath = Path()
                var rx = VallartaGeography.coastX(250f) - 20f
                var rFirst = true
                while (rx <= px + range + 60f) {
                    val rz = VallartaGeography.rioCualeCenterZ(rx)
                    val pt = toScreen(rx, rz)
                    if (rFirst) { riverPath.moveTo(pt.x, pt.y); rFirst = false } else riverPath.lineTo(pt.x, pt.y)
                    rx += 35f
                }
                drawPath(riverPath, color = Color(0xFF116D88), style = Stroke(width = 4.5f, cap = StrokeCap.Round))
            }

            // ALL 14 real 1:1 GPS Puerto Vallarta roads from VallartaGeography.roadNetwork!
            for (road in VallartaGeography.roadNetwork) {
                if (road.points.size < 2) continue
                val path = Path()
                var anyInRange = false
                for ((idx, pt) in road.points.withIndex()) {
                    val sc = toScreen(pt.first, pt.second)
                    if (abs(pt.first - px) < range * 1.6f && abs(pt.second - pz) < range * 1.6f) {
                        anyInRange = true
                    }
                    if (idx == 0) path.moveTo(sc.x, sc.y) else path.lineTo(sc.x, sc.y)
                }
                if (anyInRange) {
                    val strokeW = when {
                        road.isPedestrianMalecon -> 3.0f
                        road.halfWidth >= 10f -> 4.2f
                        else -> 2.4f
                    }
                    val col = when {
                        road.isPedestrianMalecon -> TropicalGold.copy(alpha = 0.85f)
                        road.halfWidth >= 10f -> Color(0xFF8EA4BE)
                        else -> Color(0xAA6A819C)
                    }
                    drawPath(path = path, color = col, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                }
            }

            // Nearby traffic & police blips
            for (b in hud.nearbyBlips) {
                val pt = toScreen(b.x, b.z)
                if (hypot(pt.x - cx, pt.y - cy) < r - 5f) {
                    drawCircle(
                        color = if (b.isPolice) PoliceRed else Color(0xAAFFFFFF),
                        radius = if (b.isPolice) 4.2f else 2.2f,
                        center = pt
                    )
                }
            }

            // Active Mission Waypoint Beacon (clamped to radar edge if far!)
            hud.activeMission?.let { m ->
                var dx = (m.targetX - px) / range * r
                var dy = (m.targetZ - pz) / range * r
                val d = hypot(dx, dy)
                if (d > r - 8f) {
                    dx = dx / d * (r - 8f)
                    dy = dy / d * (r - 8f)
                }
                drawCircle(color = BanderasTurquoise, radius = 6f, center = Offset(cx + dx, cy + dy))
                drawCircle(color = TropicalGold, radius = 3f, center = Offset(cx + dx, cy + dy))
            }

            // Player directional chevron at radar center
            val h = hud.playerHeadingRad
            val tip = Offset(cx + sin(h) * 8.5f, cy + cos(h) * 8.5f)
            val left = Offset(cx + sin(h + 2.4f) * 6.5f, cy + cos(h + 2.4f) * 6.5f)
            val right = Offset(cx + sin(h - 2.4f) * 6.5f, cy + cos(h - 2.4f) * 6.5f)
            val arrow = Path().apply {
                moveTo(tip.x, tip.y); lineTo(left.x, left.y); lineTo(right.x, right.y); close()
            }
            drawPath(arrow, color = SunsetCoral)
        }

        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TropicalGold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun VirtualAnalogJoystick(
    modifier: Modifier = Modifier,
    onStickChange: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 110f

    Box(
        modifier = modifier
            .size(134.dp)
            .clip(CircleShape)
            .background(Color(0x660A101E))
            .border(1.5.dp, Color(0x55FFFFFF), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    fun updateFrom(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadiusPx) {
                            Offset(delta.x / dist * maxRadiusPx, delta.y / dist * maxRadiusPx)
                        } else delta
                        knobOffset = clamped
                        // +X = right, +Y = forward (up on screen is negative delta.y)
                        onStickChange(
                            (clamped.x / maxRadiusPx).coerceIn(-1f, 1f),
                            (-clamped.y / maxRadiusPx).coerceIn(-1f, 1f)
                        )
                    }
                    updateFrom(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val ptr = event.changes.firstOrNull { it.id == down.id }
                        if (ptr != null && ptr.pressed) {
                            ptr.consume()
                            updateFrom(ptr.position)
                        } else {
                            break
                        }
                    } while (true)
                    knobOffset = Offset.Zero
                    onStickChange(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = Color(0x22FFFFFF), radius = size.minDimension * 0.34f, center = c, style = Stroke(width = 1.5f))
            val kPos = c + knobOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SunsetCoral, MaleconMagenta.copy(alpha = 0.85f)),
                    center = kPos,
                    radius = 34f
                ),
                radius = 32f,
                center = kPos
            )
            drawCircle(color = SandWhite.copy(alpha = 0.65f), radius = 32f, center = kPos, style = Stroke(width = 2.5f))
        }
    }
}

@Composable
private fun HoldActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    size: Dp,
    tag: String,
    onPressedChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Surface(
        color = if (pressed) color.copy(alpha = 0.72f) else DarkGlass,
        shape = CircleShape,
        border = BorderStroke(2.dp, color),
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .testTag(tag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    onPressedChange(true)
                    do {
                        val ev = awaitPointerEvent()
                        val ptr = ev.changes.firstOrNull { it.id == down.id }
                        if (ptr == null || !ptr.pressed) break
                        ptr.consume()
                    } while (true)
                    pressed = false
                    onPressedChange(false)
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (pressed) PacificMidnight else SandWhite, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = if (pressed) PacificMidnight else SandWhite
            )
        }
    }
}

@Composable
private fun TapActionChip(
    label: String,
    icon: ImageVector? = null,
    overrideIcon: ImageVector,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = DarkGlass,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(overrideIcon, contentDescription = label, tint = accent, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                color = SandWhite
            )
        }
    }
}

@Composable
private fun HudSmallIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = DarkGlass,
        shape = CircleShape,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun HudMiniBar(progress: Float, color: Color, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0x33FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(color)
        )
    }
}
