package com.example.ui

import android.content.res.AssetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.engine.VallartaDecorationBuilder
import com.example.game.simulation.HudUiState
import com.example.game.simulation.VallartaGameEngine
import com.example.game.world.DistrictInfo
import com.example.game.world.MissionDef
import com.example.game.world.OsmVallartaRepository
import com.example.game.world.PrebakedCoastOsmLoader
import com.example.game.world.PrebakedRoadNetworkLoader
import com.example.game.world.VallartaGeography
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ActiveOverlay {
    NONE,
    MAP,
    MISSIONS
}

class VallartaViewModel : ViewModel() {

    val engine = VallartaGameEngine()
    private val osmRepository = OsmVallartaRepository()

    val hudState: StateFlow<HudUiState> = engine.hudState

    private val _showTitleScreen = MutableStateFlow(true)
    val showTitleScreen: StateFlow<Boolean> = _showTitleScreen.asStateFlow()

    private val _activeOverlay = MutableStateFlow(ActiveOverlay.NONE)
    val activeOverlay: StateFlow<ActiveOverlay> = _activeOverlay.asStateFlow()

    private var autoStreamJob: Job? = null
    private var prebakedLoaded = false

    // Ordered spiral offsets for 3x3 inland/live sector streaming (1.65km x 1.65km around player)
    private val neighborSpiral = listOf(
        0 to 0,
        0 to -1,
        0 to 1,
        1 to 0,
        -1 to 0,
        1 to -1,
        1 to 1,
        -1 to -1,
        -1 to 1
    )

    fun startGame(assets: AssetManager, initialDistrict: DistrictInfo? = null) {
        if (initialDistrict != null) {
            engine.teleportToDistrict(initialDistrict)
        }
        _showTitleScreen.value = false
        loadPrebakedCoastalCityIfNeeded(assets)
        startAutoSectorStreaming()
    }

    private fun sortByPlayerProximity(chunks: List<com.example.game.engine.OsmSectorChunk>): List<com.example.game.engine.OsmSectorChunk> {
        val (pSx, pSz) = OsmVallartaRepository.sectorIndexFor(engine.player.x, engine.player.z)
        return chunks.sortedBy { ch ->
            val rawKey = ch.sectorKey.removePrefix("road_").removePrefix("decor_")
            val parts = rawKey.split("_")
            val sx = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val sz = parts.getOrNull(1)?.toIntOrNull() ?: 0
            (sx - pSx) * (sx - pSx) + (sz - pSz) * (sz - pSz)
        }
    }

    private fun loadPrebakedCoastalCityIfNeeded(assets: AssetManager) {
        if (prebakedLoaded) return
        prebakedLoaded = true
        viewModelScope.launch {
            // 1. Load ALL real OSM hotels, small local shops with awnings, beach palapa clubs, urban infill & 128 pools FIRST
            // so engine.colliderGrid has every building footprint registered before roads are clipped!
            engine.setOsmSyncState(syncing = true, status = "Cargando hoteles, locales y palapas 3D...")
            val buildingChunks = sortByPlayerProximity(
                PrebakedCoastOsmLoader.loadPrebakedCoastalSectors(
                    assets = assets,
                    heroBuildings = engine.buildings.toList()
                )
            )
            for (ch in buildingChunks) {
                osmRepository.markSectorPrebaked(ch.sectorKey)
            }
            engine.enqueuePrebakedOsmChunks(buildingChunks)

            // 2. Load ALL real OSM streets, empedrados, Marina walkways & real street names — strictly clipped outside every building!
            engine.setOsmSyncState(syncing = true, status = "Cargando calles y empedrados 1:1...")
            val roadChunks = sortByPlayerProximity(
                PrebakedRoadNetworkLoader.loadPrebakedRoadSectors(
                    assets = assets,
                    colliderGrid = engine.colliderGrid,
                    heroBuildings = engine.buildings.toList()
                )
            )
            engine.enqueuePrebakedOsmChunks(roadChunks)

            // 3. Load batched 3D palms, beach umbrellas, sun loungers, plaza kiosks & dense Sierra Madre vegetation with full colliders!
            engine.setOsmSyncState(syncing = true, status = "Cargando palmeras, kioscos y selva tropical...")
            val decorChunks = sortByPlayerProximity(
                VallartaDecorationBuilder.buildBatchedDecorationSectors(buildingColliderGrid = engine.colliderGrid)
            )
            engine.enqueuePrebakedOsmChunks(decorChunks)

            syncOpenStreetMap3D()
        }
    }

    private fun startAutoSectorStreaming() {
        if (autoStreamJob?.isActive == true) return
        autoStreamJob = viewModelScope.launch {
            while (isActive) {
                delay(750L)
                if (!_showTitleScreen.value) {
                    val (centerSx, centerSz) = OsmVallartaRepository.sectorIndexFor(engine.player.x, engine.player.z)
                    val nextMissing = neighborSpiral.firstOrNull { (dx, dz) ->
                        val tsx = centerSx + dx
                        val tsz = centerSz + dz
                        val midX = (tsx + 0.5f) * OsmVallartaRepository.SECTOR_SIZE_M
                        val midZ = (tsz + 0.5f) * OsmVallartaRepository.SECTOR_SIZE_M
                        val inland = VallartaGeography.signedCoastDist(midX, midZ)
                        inland <= VallartaGeography.MAX_INLAND_DIST_M &&
                            !osmRepository.isSectorIndexLoadedOrLoading(tsx, tsz)
                    }
                    if (nextMissing != null) {
                        fetchSector(centerSx + nextMissing.first, centerSz + nextMissing.second)
                    }
                }
            }
        }
    }

    fun openOverlay(overlay: ActiveOverlay) {
        _activeOverlay.value = overlay
    }

    fun closeOverlay() {
        _activeOverlay.value = ActiveOverlay.NONE
    }

    fun teleportTo(district: DistrictInfo) {
        engine.teleportToDistrict(district)
        _activeOverlay.value = ActiveOverlay.NONE
        syncOpenStreetMap3D()
    }

    fun launchMission(mission: MissionDef) {
        engine.startMission(mission)
        _activeOverlay.value = ActiveOverlay.NONE
        syncOpenStreetMap3D()
    }

    fun syncOpenStreetMap3D() {
        viewModelScope.launch {
            val (sx, sz) = OsmVallartaRepository.sectorIndexFor(engine.player.x, engine.player.z)
            fetchSector(sx, sz)
        }
    }

    private suspend fun fetchSector(sx: Int, sz: Int) {
        if (osmRepository.isSectorIndexLoadedOrLoading(sx, sz)) return
        engine.setOsmSyncState(syncing = true, status = "Cargando OSM 3D 1:1...")
        val result = osmRepository.fetchSectorByIndex(
            sx = sx,
            sz = sz,
            heroBuildings = engine.buildings.toList()
        )
        engine.setOsmSyncState(
            syncing = false,
            status = result.statusText,
            chunk = result.chunk
        )
    }
}
