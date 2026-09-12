package com.cbza.net.feature.mining.hollows.map

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.TabListReader
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.utility.rendering.Render2D

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

import kotlin.math.sqrt

private val SERVER_ID_PATTERN = Regex("Sending to server (\\S+)")

object NucleusMap {
    init {
        EventBus.subscribe(ChatEvent::class.java) { event ->
            val text = event.text
            if (text.contains("Sending to server")) {
                val match = SERVER_ID_PATTERN.find(text)
                if (match != null) {
                    onServerSwitch(match.groupValues[1])
                }
            }
            onCoordsShared(text)
        }
        EventBus.subscribe(TickEvent::class.java) {
            tick()
        }
    }

    private val textureId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "nucleus_map")
    private val arrowId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "player_arrow")

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

    private val confirmedPois = mutableSetOf<String>()

    val poiColors = mapOf(
        "Jungle Temple"       to ColorCatalog.PURPLE,
        "Mines of Divan"      to ColorCatalog.YELLOW,
        "King Yolkar"         to ColorCatalog.ORANGE,
        "Goblin Queen's Den"  to ColorCatalog.GREEN,
        "Lost Precursor City" to ColorCatalog.WHITE,
        "Khazad-dûm"          to ColorCatalog.RED,
        "Odawa"               to ColorCatalog.DARK_GREEN
    )

    val poiSizes = mapOf(
        "Jungle Temple"       to 13,
        "Mines of Divan"      to 15,
        "Goblin Queen's Den"  to 15,
        "King Yolkar"         to 10,
        "Lost Precursor City" to 15,
        "Khazad-dûm"          to 12,
        "Odawa"               to 8
    )

    val discoveredPois = mutableMapOf<String, Pair<Double, Double>>()

    private val poiPositionSamples = mutableMapOf<String, MutableList<Pair<Double, Double>>>()

    val unknownMarkers = mutableMapOf<String, Pair<Double, Double>>()
    private var unknownIdCounter = 0

    private val coordRegex = Regex("x:\\s*(-?\\d+).*?y:\\s*(-?\\d+).*?z:\\s*(-?\\d+)")

    private val poiIndicatorEntities: Map<String, String> = mapOf(
        "Kalhuiki Door Guardian" to "Jungle Temple",
        "Keeper of Diamond"      to "Mines of Divan",
        "Keeper of Gold"         to "Mines of Divan",
        "Keeper of Lapis"        to "Mines of Divan",
        "Keeper of Emerald"      to "Mines of Divan",
        "Professor Robot"        to "Lost Precursor City",
        "King Yolkar"            to "King Yolkar",
        "Bal"                    to "Khazad-dûm",
        "Odawa"                  to "Odawa"
    )

    private val poiNpcOffsets: Map<String, Pair<Double, Double>> = mapOf(
        "Kalhuiki Door Guardian" to Pair(0.0, 0.0),
        "Keeper of Diamond"      to Pair(33.0, -3.0),
        "Keeper of Gold"         to Pair(3.0, -33.0),
        "Keeper of Lapis"        to Pair(-33.0, 3.0),
        "Keeper of Emerald"      to Pair(-3.0, 33.0),
        "Professor Robot"        to Pair(16.0, -21.0),
        "King Yolkar"            to Pair(0.0, 0.0),
        "Bal"                    to Pair(0.0, 0.0),
        "Odawa"                  to Pair(0.0, 0.0)
    )

    private val poiCompassOffsets: Map<String, Pair<Double, Double>> = mapOf(
        "Jungle Temple"       to Pair(-16.0, -23.0),
        "Mines of Divan"      to Pair(0.0, 0.0),
        "King Yolkar"         to Pair(2.0, -2.0),
        "Goblin Queen's Den"  to Pair(0.0, 0.0),
        "Lost Precursor City" to Pair(40.0, -41.0),
        "Khazad-dûm"          to Pair(2.0, 16.0),
        "Odawa"               to Pair(-4.0, -16.0)
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
        if (!ModConfig.get().nucleusMap) return
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
        return sqrt(dx * dx + dz * dz)
    }

    fun tick() {
        if (!ModConfig.get().nucleusMap) return

        val mc = Minecraft.getInstance()
        val now = System.currentTimeMillis()

        if (now - lastAreaCheck > AREA_CHECK_INTERVAL_MS) {
            lastAreaCheck = now
            val area = LocationAPI.area.name

            var currentServer = LocationAPI.serverId
            if (currentServer.isNullOrEmpty()) {
                currentServer = TabListReader.getServer()
            }

            if (currentServer != null && currentServer != currentServerId) {
                onServerSwitch(currentServer)
            }

            inCrystalHollows = SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()

            if (area != lastArea) {
                lastArea = area
            }

            val level = mc.level
            if (level != null && inCrystalHollows) {
                scanForPoiEntities(level)
            }

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

    fun isInJungleQuadrant(x: Double, z: Double): Boolean {
        return x < NUCLEUS_CENTER_X && z < NUCLEUS_CENTER_Z
    }

    data class ServerSnapshot(val pois: Map<String, Pair<Double, Double>>, val timestamp: Long)

    private val serverSnapshots = mutableMapOf<String, ServerSnapshot>()
    private var currentServerId: String? = null
    private const val SNAPSHOT_EXPIRY_MS = 30 * 60 * 1000L // 30 minutes

    private val odawaNotSpawnedServers = mutableMapOf<String, Long>()

    fun isOdawaNotSpawned(): Boolean {
        val server = currentServerId ?: return false
        return odawaNotSpawnedServers.containsKey(server)
    }

    fun markOdawaUnreliable(): Boolean {
        val server = currentServerId ?: return false
        if (odawaNotSpawnedServers.containsKey(server)) return false
        odawaNotSpawnedServers[server] = System.currentTimeMillis()
        return true
    }

    fun onServerSwitch(newServerId: String) {
        val oldId = currentServerId
        if (oldId != null && inCrystalHollows && discoveredPois.isNotEmpty()) {
            serverSnapshots[oldId] = ServerSnapshot(discoveredPois.toMap(), System.currentTimeMillis())
        }

        currentServerId = newServerId

        reset()

        val now = System.currentTimeMillis()
        serverSnapshots.entries.removeIf { now - it.value.timestamp > SNAPSHOT_EXPIRY_MS }
        odawaNotSpawnedServers.entries.removeIf { now - it.value > SNAPSHOT_EXPIRY_MS }

        val snapshot = serverSnapshots[newServerId]
        if (snapshot != null) {
            discoveredPois.putAll(snapshot.pois)
        }
    }

    fun render(graphics: GuiGraphicsExtractor) {
            if (!ModConfig.get().nucleusMap) return
            if (!inCrystalHollows) return

            val mc = Minecraft.getInstance()
            val cfg = ModConfig.get()
            val mapSize = (100 * cfg.nucleusMapScale).toInt()
            val arrowWidth = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)
            val arrowHeight = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)

            val mapX = cfg.nucleusMapX
            val mapY = cfg.nucleusMapY

            Render2D.drawImage(graphics, textureId, mapX, mapY, mapSize, mapSize)

            for ((name, coords) in discoveredPois) {
                val color = poiColors[name] ?: continue
                val size = ((poiSizes[name] ?: 6) * cfg.nucleusMapScale).toInt()
                val poiPos = getPoiMapPosition(coords.first, coords.second, mapSize)
                val px = mapX + poiPos.first
                val py = mapY + poiPos.second
                graphics.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, color)
            }

            for ((id, coords) in unknownMarkers) {
                val poiPos = getPoiMapPosition(coords.first, coords.second, mapSize)
                val px = mapX + poiPos.first
                val py = mapY + poiPos.second
                val size = (6 * cfg.nucleusMapScale).toInt()
                graphics.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, 0xFF808080.toInt())
            }

            val pos = getPlayerMapPosition(mapSize)
            if (pos != null) {
                val dotX = mapX + pos.first
                val dotY = mapY + pos.second
                val yaw = mc.player?.yRot ?: 0f

                graphics.pose().pushMatrix()
                graphics.pose().translate(dotX.toFloat(), dotY.toFloat())
                graphics.pose().rotate(Math.toRadians((yaw + 180.0)).toFloat())
                graphics.pose().translate(-(arrowWidth / 2).toFloat(), -(arrowHeight / 2).toFloat())
                Render2D.drawImage(graphics, arrowId, 0, 0, arrowWidth, arrowHeight)
                graphics.pose().popMatrix()
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
        WishingCompassSolver.reset()
    }
}