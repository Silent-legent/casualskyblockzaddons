package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.TabListReader
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

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

    private val sharedUnconfirmedPois = mutableSetOf<String>()

    // POIs whose position came from an indicator NPC or a solved Wishing Compass triangulation
    // (walk-in fallback won't touch these)
    private val confirmedPois = mutableSetOf<String>()

    val poiColors = mapOf(
        "Jungle Temple"       to 0xFFAA00FF.toInt(),
        "Mines of Divan"      to 0xFFFFFF00.toInt(),
        "King Yolkar"         to 0xFFFFA500.toInt(),
        "Goblin Queen's Den"  to 0xFF00FF00.toInt(),
        "Lost Precursor City" to 0xFFFFFFFF.toInt(),
        "Khazad-dûm"          to 0xFFFF0000.toInt(),
    )

    val poiSizes = mapOf(
        "Jungle Temple"       to 13,
        "Mines of Divan"      to 15,
        "Goblin Queen's Den"  to 15,
        "King Yolkar"         to 8,
        "Lost Precursor City" to 15,
        "Khazad-dûm"          to 12
    )

    val discoveredPois = mutableMapOf<String, Pair<Double, Double>>()

    // walk-in fallback samples - only used for POIs not yet confirmed by NPC or compass solve
    private val poiPositionSamples = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

    // unknown markers: id -> (x, z)
    val unknownMarkers = mutableMapOf<String, Pair<Double, Double>>()
    private var unknownIdCounter = 0

    private val coordRegex = Regex("x:\\s*(-?\\d+).*?y:\\s*(-?\\d+).*?z:\\s*(-?\\d+)")

    // --- NPC-based instant POI detection ---
    // Maps an indicator NPC's exact custom name to the POI it confirms.
    private val poiIndicatorEntities: Map<String, String> = mapOf(
        "Kalhuiki Door Guardian" to "Jungle Temple",
        "Keeper of Diamond"      to "Mines of Divan",
        "Keeper of Gold"         to "Mines of Divan",
        "Keeper of Lapis"        to "Mines of Divan",
        "Keeper of Emerald"      to "Mines of Divan",
        "Professor Robot"        to "Lost Precursor City",
        "King Yolkar"            to "King Yolkar",
        "Bal"                    to "Khazad-dûm"
    )

    // Manually-measured offset from EACH INDIVIDUAL NPC's position to the POI's true center.
    // Keyed per-NPC (not per-POI) since e.g. the four Mines of Divan Keepers each stand
    // in a different spot relative to the mine's true center.
    // Format: NPC name -> Pair(offsetX, offsetZ), where trueCenter = npcPos + offset
    private val poiNpcOffsets: Map<String, Pair<Double, Double>> = mapOf(
        "Kalhuiki Door Guardian" to Pair(0.0, 0.0),
        "Keeper of Diamond"      to Pair(33.0, -3.0),
        "Keeper of Gold"         to Pair(3.0, -33.0),
        "Keeper of Lapis"        to Pair(-33.0, 3.0),
        "Keeper of Emerald"      to Pair(-3.0, 33.0),
        "Professor Robot"        to Pair(16.0, -21.0),
        "King Yolkar"            to Pair(0.0, 0.0),
        "Bal"                    to Pair(0.0, 0.0)
    )

    // Manually-measured offset from the Wishing Compass triangulation's solved point to the
    // POI's true center. Keyed per-POI (not per-NPC) since the compass consistently resolves
    // to the same relative spot within a given POI regardless of which NPC lives there.
    // Format: POI name -> Pair(offsetX, offsetZ), where trueCenter = solvedPos + offset
    private val poiCompassOffsets: Map<String, Pair<Double, Double>> = mapOf(
        "Jungle Temple"       to Pair(-16.0, -23.0),
        "Mines of Divan"      to Pair(0.0, 0.0),
        "King Yolkar"         to Pair(2.0, -2.0),
        "Goblin Queen's Den"  to Pair(0.0, 0.0),
        "Lost Precursor City" to Pair(40.0, -41.0),
        "Khazad-dûm"          to Pair(2.0, 16.0)
    )

    private fun scanForPoiEntities(level: ClientLevel) {
        for (entity in level.entitiesForRendering()) {
            val name = entity.customName?.string ?: continue
            val poi = poiIndicatorEntities[name] ?: continue
            if (confirmedPois.contains(poi)) continue

            val offset = poiNpcOffsets[name] ?: Pair(0.0, 0.0)
            val realX = entity.x + offset.first
            val realZ = entity.z + offset.second

            discoveredPois[poi] = Pair(realX, realZ)
            confirmedPois.add(poi)
            poiPositionSamples.remove(poi)
        }
    }

    fun onCoordsShared(text: String) {
        if (!ModConfig.get().NucleusMap) return
        if (!inCrystalHollows) return

        val match = coordRegex.find(text) ?: return
        val x = match.groupValues[1].toDoubleOrNull() ?: return
        val z = match.groupValues[3].toDoubleOrNull() ?: return

        val afterCoords = text.substring(match.range.last + 1)
        val namedPoi = poiColors.keys.firstOrNull { afterCoords.contains(it) }
        if (namedPoi != null) {
            if (!discoveredPois.containsKey(namedPoi)) {
                discoveredPois[namedPoi] = Pair(x, z)
                sharedUnconfirmedPois.add(namedPoi)
            }
            return
        }

        if (discoveredPois.size >= poiColors.size) return

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

            var currentServer = LocationAPI.serverId
            if (currentServer.isNullOrEmpty()) {
                currentServer = TabListReader.getServer()
            }

            // server-switch detection FIRST, so its reset() doesn't wipe inCrystalHollows right after we set it
            if (currentServer != null && currentServer != currentServerId) {
                onServerSwitch(currentServer)
            }

            inCrystalHollows = SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()

            if (area != lastArea) {
                lastArea = area
            }

            // instant NPC-based detection - authoritative, runs first so it can overwrite fallback data
            val level = mc.level
            if (level != null && inCrystalHollows) {
                scanForPoiEntities(level)
            }

            // walk-in fallback - only applies to POIs not yet confirmed by NPC or compass solve
            val matchedPoi = poiColors.keys.firstOrNull { area.contains(it) }
            if (matchedPoi != null && !confirmedPois.contains(matchedPoi)) {
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

            if (discoveredPois.size >= poiColors.size) {
                unknownMarkers.clear()
            }
        }

        val player = mc.player
        if (player != null && unknownMarkers.isNotEmpty()) {
            val toRemove = unknownMarkers.entries.filter {
                distance(it.value.first, it.value.second, player.x, player.z) < UNKNOWN_REMOVE_DISTANCE
            }.map { it.key }
            toRemove.forEach { unknownMarkers.remove(it) }
        }

        if (player != null && sharedUnconfirmedPois.isNotEmpty()) {
            val checked = mutableListOf<String>()
            for (name in sharedUnconfirmedPois) {
                val pos = discoveredPois[name] ?: continue
                if (distance(pos.first, pos.second, player.x, player.z) < UNKNOWN_REMOVE_DISTANCE) {
                    val confirmed = confirmedPois.contains(name)
                    if (!confirmed) {
                        discoveredPois.remove(name)
                    }
                    checked.add(name)
                }
            }
            checked.forEach { sharedUnconfirmedPois.remove(it) }
        }
    }

    // Called by WishingCompassSolver once a triangulated solve resolves to a tracked POI.
    // x/z here are the raw triangulated point - we correct it to the POI's true center
    // using poiCompassOffsets before storing it.
    fun registerCompassSolvedPoi(poi: String, x: Double, z: Double) {
        if (confirmedPois.contains(poi)) return // NPC (or an earlier compass solve) already confirmed this one
        val offset = poiCompassOffsets[poi] ?: Pair(0.0, 0.0)
        discoveredPois[poi] = Pair(x + offset.first, z + offset.second)
        confirmedPois.add(poi)
        poiPositionSamples.remove(poi)
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

    data class ServerSnapshot(val pois: Map<String, Pair<Double, Double>>, val timestamp: Long)

    private val serverSnapshots = mutableMapOf<String, ServerSnapshot>()
    private var currentServerId: String? = null
    private const val SNAPSHOT_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes

    fun onServerSwitch(newServerId: String) {
        val oldId = currentServerId
        if (oldId != null && inCrystalHollows && discoveredPois.isNotEmpty()) {
            serverSnapshots[oldId] = ServerSnapshot(discoveredPois.toMap(), System.currentTimeMillis())
        }

        currentServerId = newServerId

        reset()

        val now = System.currentTimeMillis()
        serverSnapshots.entries.removeIf { now - it.value.timestamp > SNAPSHOT_EXPIRY_MS }

        val snapshot = serverSnapshots[newServerId]
        if (snapshot != null) {
            discoveredPois.putAll(snapshot.pois)
        }
    }

    fun reset() {
        inCrystalHollows = false
        discoveredPois.clear()
        poiPositionSamples.clear()
        confirmedPois.clear()
        unknownMarkers.clear()
        sharedUnconfirmedPois.clear()
        lastArea = ""
    }
}