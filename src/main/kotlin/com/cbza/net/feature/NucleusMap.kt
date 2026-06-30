package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import net.minecraft.client.Minecraft
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import com.cbza.net.feature.NucleusMap;

object NucleusMap {

    private const val NUCLEUS_CENTER_X = 512.0
    private const val NUCLEUS_CENTER_Z = 512.0
    private const val NUCLEUS_SIZE = 621.0
    private const val MAX_POI_SAMPLES = 20
    private const val UNKNOWN_REMOVE_DISTANCE = 15.0 // blocks

    @Volatile var inCrystalHollows = false

    private var lastAreaCheck = 0L
    private const val AREA_CHECK_INTERVAL_MS = 2000L
    private var lastArea = ""

    val poiColors = mapOf(
        "Jungle Temple"       to 0xFFAA00FF.toInt(),
        "Mines of Divan"      to 0xFFFFFF00.toInt(),
        "Goblin Queen's Den"  to 0xFF00FF00.toInt(),
        "Lost Precursor City" to 0xFFFFFFFF.toInt(),
        "Khazad-dûm"          to 0xFFFF0000.toInt()
    )

    val poiSizes = mapOf(
        "Jungle Temple"       to 8,
        "Mines of Divan"      to 12,
        "Goblin Queen's Den"  to 8,
        "Lost Precursor City" to 10,
        "Khazad-dûm"          to 8
    )

    val discoveredPois = mutableMapOf<String, Pair<Double, Double>>()
    private val poiPositionSamples = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

    // unknown markers: id -> (x, z)
    val unknownMarkers = mutableMapOf<String, Pair<Double, Double>>()
    private var unknownIdCounter = 0

    private val coordRegex = Regex("x:\\s*(-?\\d+).*?y:\\s*(-?\\d+).*?z:\\s*(-?\\d+)")

    fun onCoordsShared(text: String) {
        if (!ModConfig.get().NucleusMap) return
        if (!inCrystalHollows) return
        if (discoveredPois.size >= poiColors.size) return // all found, ignore

        val match = coordRegex.find(text) ?: return
        val x = match.groupValues[1].toDoubleOrNull() ?: return
        val z = match.groupValues[3].toDoubleOrNull() ?: return

        // don't add if too close to an existing unknown or discovered poi
        val tooClose = unknownMarkers.values.any { distance(it.first, it.second, x, z) < UNKNOWN_REMOVE_DISTANCE } ||
                discoveredPois.values.any { distance(it.first, it.second, x, z) < UNKNOWN_REMOVE_DISTANCE }
        if (tooClose) return

        val id = "unknown_${unknownIdCounter++}"
        unknownMarkers[id] = Pair(x, z)
    }

    private fun distance(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        val dx = x1 - x2
        val dz = z1 - z2
        return kotlin.math.sqrt(dx * dx + dz * dz)
    }

    fun tick() {
        if (!ModConfig.get().NucleusMap) return

        val mc = Minecraft.getInstance()
        val now = System.currentTimeMillis()

        if (now - lastAreaCheck > AREA_CHECK_INTERVAL_MS) {
            lastAreaCheck = now
            val area = LocationAPI.area.name

            inCrystalHollows = SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()

            if (area != lastArea) {
                lastArea = area
            }

            val matchedPoi = poiColors.keys.firstOrNull { area.contains(it) }
            if (matchedPoi != null) {
                val player = mc.player
                if (player != null) {
                    val samples = poiPositionSamples.getOrPut(matchedPoi) { mutableListOf() }
                    if (samples.size < MAX_POI_SAMPLES) {
                        samples.add(Pair(player.x, player.z))
                        val avgX = samples.map { it.first }.average()
                        val avgZ = samples.map { it.second }.average()
                        discoveredPois[matchedPoi] = Pair(avgX, avgZ)
                    }
                }
            }

            // clear all unknown markers once all pois are found
            if (discoveredPois.size >= poiColors.size) {
                unknownMarkers.clear()
            }
        }

        // remove unknown markers the player has walked near (whether or not a poi was found there)
        val player = mc.player
        if (player != null && unknownMarkers.isNotEmpty()) {
            val toRemove = unknownMarkers.entries.filter {
                distance(it.value.first, it.value.second, player.x, player.z) < UNKNOWN_REMOVE_DISTANCE
            }.map { it.key }
            toRemove.forEach { unknownMarkers.remove(it) }
        }
    }

    fun getPlayerMapPosition(mapSize: Int): Pair<Int, Int>? {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return null

        val relX = (player.x - (NUCLEUS_CENTER_X - NUCLEUS_SIZE / 2)) / NUCLEUS_SIZE
        val relZ = (player.z - (NUCLEUS_CENTER_Z - NUCLEUS_SIZE / 2)) / NUCLEUS_SIZE

        val clampedX = relX.coerceIn(0.0, 1.0)
        val clampedZ = relZ.coerceIn(0.0, 1.0)

        return Pair(
            (clampedX * mapSize).toInt(),
            (clampedZ * mapSize).toInt()
        )
    }

    fun getPoiMapPosition(x: Double, z: Double, mapSize: Int): Pair<Int, Int> {
        val relX = (x - (NUCLEUS_CENTER_X - NUCLEUS_SIZE / 2)) / NUCLEUS_SIZE
        val relZ = (z - (NUCLEUS_CENTER_Z - NUCLEUS_SIZE / 2)) / NUCLEUS_SIZE

        val clampedX = relX.coerceIn(0.0, 1.0)
        val clampedZ = relZ.coerceIn(0.0, 1.0)

        return Pair(
            (clampedX * mapSize).toInt(),
            (clampedZ * mapSize).toInt()
        )
    }

    fun reset() {
        inCrystalHollows = false
        discoveredPois.clear()
        poiPositionSamples.clear()
        unknownMarkers.clear()
        lastArea = ""
    }
}