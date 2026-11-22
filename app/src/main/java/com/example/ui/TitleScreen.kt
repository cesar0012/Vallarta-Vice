package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.world.DistrictInfo
import com.example.game.world.VallartaGeography
import com.example.ui.theme.*

@Composable
fun TitleScreen(
    onPlayClick: (DistrictInfo?) -> Unit
) {
    val quickSpawns = remember {
        listOf(
            VallartaGeography.districts.first { it.id == "malecon" },
            VallartaGeography.districts.first { it.id == "hotel_north" },
            VallartaGeography.districts.first { it.id == "marina" },
            VallartaGeography.districts.first { it.id == "romantica" },
            VallartaGeography.districts.first { it.id == "fluvial" },
            VallartaGeography.districts.first { it.id == "airport" },
            VallartaGeography.districts.first { it.id == "mismaloya" }
        )
    }
    var selectedSpawn by remember { mutableStateOf(quickSpawns.first()) }

    val pulseAnim = rememberInfiniteTransition(label = "titlePulse")
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PacificMidnight)
    ) {
        // Full-bleed GTA-style Puerto Vallarta illustrated cover art
        Image(
            painter = painterResource(id = R.drawable.img_vallarta_splash),
            contentDescription = stringResource(R.string.cd_splash_art),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Cinematic dark sunset vignette scrim for razor-sharp typography legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0xAA0A101E),
                        0.32f to Color(0x330A101E),
                        0.62f to Color(0xBB0A101E),
                        1.0f to Color(0xF50A101E)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top editorial badge + coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = DarkGlass,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, SunsetCoral.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = SunsetCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.title_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = SandWhite
                        )
                    }
                }

                Surface(
                    color = DarkGlass,
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, BanderasTurquoise.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "ESCALA 1:1 • CALLES, EMPEDRADOS Y HOTELES 3D",
                        style = MaterialTheme.typography.labelSmall,
                        color = BanderasTurquoise,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }

            // Bottom Asymmetric Title Lockup & Launch Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "VALLARTA",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 68.sp,
                            lineHeight = 64.sp,
                            letterSpacing = 4.sp
                        ),
                        color = SandWhite
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "VICE",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 54.sp,
                                lineHeight = 52.sp,
                                letterSpacing = 6.sp
                            ),
                            color = SunsetCoral.copy(alpha = glowAlpha)
                        )
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .weight(1f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(SunsetCoral, MaleconMagenta, BanderasTurquoise)
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Puerto Vallarta 1:1 con vista satelital real, 17.8 km de costa y 6 km tierra adentro: Aeropuerto PVR, Marina, Zona Hotelera, Fluvial, Pitillal, Malecón, Río Cuale y Mismaloya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SandWhite.copy(alpha = 0.88f)
                    )
                }

                // Starting District Selector Strip
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "PUNTO DE INICIO EN LA COSTA",
                        style = MaterialTheme.typography.labelSmall,
                        color = TropicalGold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickSpawns) { dist ->
                            val selected = dist.id == selectedSpawn.id
                            Surface(
                                color = if (selected) SunsetCoral.copy(alpha = 0.24f) else DarkGlass,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) SunsetCoral else SubtleBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedSpawn = dist }
                                    .testTag("spawn_chip_${dist.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = dist.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) TropicalGold else SandWhite
                                    )
                                    Text(
                                        text = dist.subtitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = MutedMist,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Primary Action CTA
                Button(
                    onClick = { onPlayClick(selectedSpawn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("play_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SunsetCoral,
                        contentColor = PacificMidnight
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_play),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
