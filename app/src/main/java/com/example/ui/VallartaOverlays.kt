package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.simulation.HudUiState
import com.example.game.world.DistrictInfo
import com.example.game.world.MissionDef
import com.example.game.world.VallartaGeography
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun VallartaMapOverlay(
    hud: HudUiState,
    onTeleport: (DistrictInfo) -> Unit,
    onSyncOsm: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEB070D1A))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.map_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = SandWhite
                    )
                    Text(
                        text = stringResource(R.string.map_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = BanderasTurquoise
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onSyncOsm,
                        enabled = !hud.isSyncingOsm,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BanderasTurquoise),
                        modifier = Modifier.testTag("osm_sync_button")
                    ) {
                        Icon(Icons.Rounded.Public, contentDescription = stringResource(R.string.cd_osm_sync), tint = BanderasTurquoise, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hud.isSyncingOsm) "CARGANDO..." else "OSM 3D (${hud.osmBuildingCount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = BanderasTurquoise
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CardSlateSurface)
                            .testTag("close_map_button")
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_overlay), tint = SandWhite)
                    }
                }
            }

            // Status strip
            Text(
                text = "Fuente 3D: ${hud.osmStatus} • Toca cualquier zona de Puerto Vallarta para Viaje Rápido",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedMist
            )

            // Main Split: Live 1:1 Coastline + 6km Inland Road Vector Map & 12-District Fast-Travel List
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Full 1:1 Puerto Vallarta Map (17.8 km Coastline + 6.2 km Inland to Pitillal)
                Surface(
                    color = Color(0xFF0A243C),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BanderasTurquoise.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        val xMin = -3200f
                        val xMax = 5200f
                        val zMin = VallartaGeography.WORLD_Z_NORTH
                        val zMax = VallartaGeography.WORLD_Z_SOUTH
                        fun mapX(wx: Float): Float = ((wx - xMin) / (xMax - xMin)) * size.width
                        fun mapY(wz: Float): Float = ((wz - zMin) / (zMax - zMin)) * size.height

                        // Real Banderas Bay Coastline & Sierra Madre land polygon
                        val landPath = Path()
                        var first = true
                        var z = zMin
                        while (z <= zMax) {
                            val cx = mapX(VallartaGeography.coastX(z))
                            val sy = mapY(z)
                            if (first) {
                                landPath.moveTo(cx, sy)
                                first = false
                            } else {
                                landPath.lineTo(cx, sy)
                            }
                            z += 120f
                        }
                        landPath.lineTo(size.width, size.height)
                        landPath.lineTo(size.width, 0f)
                        landPath.close()
                        drawPath(landPath, color = Color(0xFF1A362E))

                        // Marina Vallarta harbor basin
                        val mPt = Offset(
                            mapX(VallartaGeography.lonToWorldX(-105.2528)),
                            mapY(VallartaGeography.latToWorldZ(20.6632))
                        )
                        drawCircle(color = Color(0xFF0E4B70), radius = 8f, center = mPt)

                        // Río Cuale river channel cutting inland from the sea
                        val riverPath = Path()
                        var rx = VallartaGeography.coastX(250f)
                        var rFirst = true
                        while (rx <= 4800f) {
                            val rz = VallartaGeography.rioCualeCenterZ(rx)
                            if (rFirst) {
                                riverPath.moveTo(mapX(rx), mapY(rz))
                                rFirst = false
                            } else {
                                riverPath.lineTo(mapX(rx), mapY(rz))
                            }
                            rx += 180f
                        }
                        drawPath(riverPath, color = BanderasTurquoise.copy(alpha = 0.72f), style = Stroke(width = 3f, cap = StrokeCap.Round))

                        // ALL 14 real 1:1 GPS Puerto Vallarta roads (Coastal + Fluvial + Pitillal + Libramiento!)
                        for (road in VallartaGeography.roadNetwork) {
                            if (road.points.size < 2) continue
                            val p = Path()
                            road.points.forEachIndexed { i, pt ->
                                val sx = mapX(pt.first)
                                val sy = mapY(pt.second)
                                if (i == 0) p.moveTo(sx, sy) else p.lineTo(sx, sy)
                            }
                            val col = when {
                                road.isPedestrianMalecon -> SunsetCoral
                                road.halfWidth >= 10f -> TropicalGold.copy(alpha = 0.90f)
                                else -> Color(0xBB8AB0CC)
                            }
                            drawPath(
                                path = p,
                                color = col,
                                style = Stroke(width = if (road.halfWidth >= 10f) 3.2f else 1.8f, cap = StrokeCap.Round)
                            )
                        }

                        // All 12 Coastal & Inland District pins at exact 1:1 GPS coordinates!
                        for (d in VallartaGeography.districts) {
                            val pt = Offset(mapX(d.spawnX), mapY(d.spawnZ))
                            drawCircle(
                                color = if (d.isInland) BanderasTurquoise else SandWhite.copy(alpha = 0.92f),
                                radius = 4.2f,
                                center = pt
                            )
                        }

                        // Active Mission Beacon
                        hud.activeMission?.let { m ->
                            val mp = Offset(mapX(m.targetX), mapY(m.targetZ))
                            drawCircle(color = BanderasTurquoise, radius = 8.5f, center = mp)
                        }

                        // Player Live Pin
                        val pp = Offset(mapX(hud.playerX), mapY(hud.playerZ))
                        drawCircle(color = SunsetCoral.copy(alpha = 0.35f), radius = 13f, center = pp)
                        drawCircle(color = SunsetCoral, radius = 6f, center = pp)
                    }
                }

                // 12 Full-City Coastal & Inland Districts Fast-Travel List
                LazyColumn(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(VallartaGeography.districts) { dist ->
                        val isCurrent = dist.name == hud.districtName
                        Surface(
                            color = if (isCurrent) SunsetCoral.copy(alpha = 0.20f) else CardSlateSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isCurrent) SunsetCoral else SubtleBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTeleport(dist) }
                                .testTag("map_district_${dist.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = dist.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isCurrent) TropicalGold else SandWhite
                                        )
                                        if (dist.isInland) {
                                            Surface(
                                                color = BanderasTurquoise.copy(alpha = 0.18f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "INTERIOR",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                    color = BanderasTurquoise,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = dist.subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = MutedMist
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.NearMe,
                                    contentDescription = stringResource(R.string.map_teleport),
                                    tint = if (isCurrent) TropicalGold else BanderasTurquoise,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MissionsOverlay(
    hud: HudUiState,
    onStartMission: (MissionDef) -> Unit,
    onSpawnSupercar: () -> Unit,
    onBribePolice: () -> Unit,
    onSyncOsm: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEB070D1A))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.missions_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = SandWhite
                    )
                    Text(
                        text = "DINERO: $${String.format(Locale.US, "%,d", hud.cash)} MXN • COMPLETADAS: ${hud.completedMissionIds.size}/${VallartaGeography.missions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CashEmerald
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CardSlateSurface)
                        .testTag("close_missions_button")
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_overlay), tint = SandWhite)
                }
            }

            // Quick GTA Vice Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSpawnSupercar(); onDismiss() },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("deliver_supercar_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral, contentColor = PacificMidnight)
                ) {
                    Icon(Icons.Rounded.DirectionsCar, contentDescription = stringResource(R.string.cd_spawn_car), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SUPERCAR", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onBribePolice,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("bribe_police_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TropicalGold)
                ) {
                    Icon(Icons.Rounded.LocalPolice, contentDescription = stringResource(R.string.cd_clear_wanted), tint = TropicalGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("LIMPIAR ★ ($500)", style = MaterialTheme.typography.labelLarge, color = TropicalGold)
                }

                OutlinedButton(
                    onClick = onSyncOsm,
                    modifier = Modifier.height(48.dp).testTag("missions_osm_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BanderasTurquoise)
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = stringResource(R.string.cd_osm_sync), tint = BanderasTurquoise, modifier = Modifier.size(18.dp))
                }
            }

            // 5 Iconic Puerto Vallarta Coastal Missions
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(VallartaGeography.missions) { mission ->
                    val done = mission.id in hud.completedMissionIds
                    val isActive = hud.activeMission?.mission?.id == mission.id
                    Surface(
                        color = if (isActive) Color(0xFF102A3A) else CardSlateSurface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = when {
                                isActive -> BanderasTurquoise
                                done -> CashEmerald.copy(alpha = 0.55f)
                                else -> SubtleBorder
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mission.id}. ${mission.title}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                                    color = if (done) CashEmerald else TropicalGold
                                )
                                Surface(
                                    color = CashEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "+$${mission.rewardCash} MXN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CashEmerald,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = mission.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SandWhite.copy(alpha = 0.88f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${mission.startDistrict} → ${mission.targetName}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MutedMist
                                )
                                Button(
                                    onClick = { onStartMission(mission) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) BanderasTurquoise else SunsetCoral,
                                        contentColor = PacificMidnight
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("start_mission_${mission.id}")
                                ) {
                                    Text(
                                        text = if (isActive) "EN CURSO" else if (done) "REPETIR" else "INICIAR",
                                        style = MaterialTheme.typography.labelLarge
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
