package com.example

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.engine.VallartaGLRenderer
import com.example.game.world.VallartaGeography
import com.example.ui.*
import com.example.ui.theme.VallartaViceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load exact 586-point real OSM coastline, Marina harbor polygon & Río Cuale BEFORE ViewModel constructs!
        VallartaGeography.loadExactGeoAssets(assets)
        enableEdgeToEdge()
        setContent {
            VallartaViceTheme {
                VallartaViceApp()
            }
        }
    }
}

@Composable
private fun VallartaViceApp(
    viewModel: VallartaViewModel = viewModel()
) {
    val context = LocalContext.current
    val showTitle by viewModel.showTitleScreen.collectAsStateWithLifecycle()
    val overlay by viewModel.activeOverlay.collectAsStateWithLifecycle()
    val hud by viewModel.hudState.collectAsStateWithLifecycle()

    if (showTitle) {
        TitleScreen(
            onPlayClick = { spawnDistrict ->
                viewModel.startGame(context.assets, spawnDistrict)
            }
        )
    } else {
        val lifecycleOwner = LocalLifecycleOwner.current
        var glViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }

        DisposableEffect(lifecycleOwner, glViewRef) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                    Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Native 60fps OpenGL ES 3.0 3D Puerto Vallarta Engine
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(3)
                        setEGLConfigChooser(8, 8, 8, 8, 24, 0)
                        preserveEGLContextOnPause = true
                        setRenderer(VallartaGLRenderer(viewModel.engine))
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        glViewRef = this
                    }
                }
            )

            // Crisp Responsive GTA HUD & Touch Controls
            GameHudOverlay(
                hud = hud,
                input = viewModel.engine.input,
                onOpenMap = { viewModel.openOverlay(ActiveOverlay.MAP) },
                onOpenMissions = { viewModel.openOverlay(ActiveOverlay.MISSIONS) },
                onCycleCamera = { viewModel.engine.cycleCameraMode() },
                onCycleTime = { viewModel.engine.cycleTimeOfDay() },
                onCycleRadio = { viewModel.engine.cycleRadioStation() },
                onToggleMute = { viewModel.engine.toggleMute() },
                onSpawnCar = { viewModel.engine.spawnPlayerSupercar() }
            )

            // Interactive Full-Coastline Puerto Vallarta GPS Map Overlay
            if (overlay == ActiveOverlay.MAP) {
                VallartaMapOverlay(
                    hud = hud,
                    onTeleport = { viewModel.teleportTo(it) },
                    onSyncOsm = { viewModel.syncOpenStreetMap3D() },
                    onDismiss = { viewModel.closeOverlay() }
                )
            }

            // Missions & Vice Actions Overlay
            if (overlay == ActiveOverlay.MISSIONS) {
                MissionsOverlay(
                    hud = hud,
                    onStartMission = { viewModel.launchMission(it) },
                    onSpawnSupercar = { viewModel.engine.spawnPlayerSupercar() },
                    onBribePolice = { viewModel.engine.bribePoliceClearWanted() },
                    onSyncOsm = { viewModel.syncOpenStreetMap3D() },
                    onDismiss = { viewModel.closeOverlay() }
                )
            }
        }
    }
}
