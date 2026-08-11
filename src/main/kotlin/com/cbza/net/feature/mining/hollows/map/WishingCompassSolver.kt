package com.cbza.net.feature.mining.hollows.map

import com.cbza.net.config.ModConfig
import com.cbza.net.feature.mining.hollows.map.NucleusMap.markOdawaUnreliable
import com.cbza.net.utility.TabListReader
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.ParticleEvent

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.core.particles.ParticleTypes

import tech.thatgravyboat.skyblockapi.api.location.LocationAPI

import kotlin.math.abs
import kotlin.math.sqrt

// Solves the exact location of a Crystal Hollows POI using the "Wishing Compass"
// item. The compass leaves a trail of particles pointing toward the target; this
// figures out that direction, and by using the compass twice from two different
// spots, works out where the two directions cross to get an exact location.
object WishingCompassSolver {
    init {
        EventBus.subscribe(ChatEvent::class.java) { event ->
            val text = event.text
            if (text.contains("Your Wishing Compass shattered into pieces!")) {
                onCompassUsed()
            }
            if (text.contains("Get to the Queen before my stench goes away and you'll be able to sneak past her imbecile Guards!")) {
                onKingsScentGranted()
            }
        }
        EventBus.subscribe(ParticleEvent::class.java) { event ->
            if (event.options.type == ParticleTypes.HAPPY_VILLAGER) {
                handleParticle(event.x, event.y, event.z)
            }
        }
    }

    private const val TRAIL_WINDOW_MS = 250L
    private const val MIN_TRAIL_POINTS = 20
    private const val MAX_STEP_DISTANCE = 3.0 // blocks
    private const val MIN_BASELINE_DISTANCE = 5.0 // blocks
    private const val KINGS_CENT_DURATION_MS = 20 * 60 * 1000L

    private var kingsScentExpiresAt: Long = 0L

    // A "direction line" from one compass use: where the player stood, which way
    // it pointed, and which POI it was aimed at.
    data class Ray(
        val origin: Triple<Double, Double, Double>,
        val dir: Triple<Double, Double, Double>,
        val targetPoi: String
    )

    private var currentTrail = mutableListOf<Triple<Double, Double, Double>>()
    private var lastParticleTime = 0L
    private var trailOrigin: Triple<Double, Double, Double>? = null
    private var pendingRay: Ray? = null

    // Which gemstone crystal corresponds to each POI (used to check if a reading can be trusted).
    private val poiCrystalMap = mapOf(
        "King Yolkar" to "Amber",
        "Goblin Queen's Den" to "Amber",
        "Lost Precursor City" to "Sapphire",
        "Mines of Divan" to "Jade",
        "Jungle Temple" to "Amethyst",
        "Khazad-dûm" to "Topaz",
    )

    // If the relevant crystal has already been placed/found, the compass reading
    // for that POI can no longer be trusted.
    private fun isCompassUnreliableFor(targetPoi: String): Boolean {
        val crystalName = poiCrystalMap[targetPoi] ?: return false
        val status = TabListReader.getCrystalStatus(crystalName) ?: return false
        return !status.contains("Not Found", ignoreCase = true)
    }
    private var compassJustShattered = false

    // Called when the player actually uses (breaks) a compass.
    fun onCompassUsed() {
        compassJustShattered = true
    }

    // Called for every particle spawned by the compass. Collects them into a
    // "trail" that we'll later use to figure out the pointing direction.
    fun handleParticle(x: Double, y: Double, z: Double) {
        if (!ModConfig.get().NucleusMap) return
        if (!NucleusMap.inCrystalHollows) return

        val now = System.currentTimeMillis()
        if (now - lastParticleTime > TRAIL_WINDOW_MS && currentTrail.isNotEmpty()) {
            finishTrail()
        }

        // Ignore particles that jumped too far from the last one - likely unrelated/noise.
        val last = currentTrail.lastOrNull()
        if (last != null) {
            val dx = x - last.first
            val dy = y - last.second
            val dz = z - last.third
            val stepDist = sqrt(dx * dx + dy * dy + dz * dz)
            if (stepDist > MAX_STEP_DISTANCE) {
                return
            }
        }

        if (currentTrail.isEmpty()) {
            val player = Minecraft.getInstance().player
            trailOrigin = if (player != null) Triple(player.x, player.y, player.z) else Triple(x, y, z)
        }

        lastParticleTime = now
        currentTrail.add(Triple(x, y, z))
    }

    // Called once a particle trail stops coming in. Turns the collected trail
    // into a direction ("ray") pointing at the target POI, if possible.
    private fun finishTrail() {

        val points = currentTrail.toList()
        val origin = trailOrigin
        currentTrail.clear()
        trailOrigin = null
        if (points.size < MIN_TRAIL_POINTS || origin == null) {
            return
        }

        val targetPoi = resolveTargetPoi()
        if (targetPoi == null) {
            return
        }

        if (targetPoi == "Odawa" && NucleusMap.isOdawaNotSpawned()) {
            if (compassJustShattered) {
                compassJustShattered = false
                pendingRay = null
                Minecraft.getInstance().player?.sendSystemMessage(
                    Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                            "§fOdawa isn't in this lobby. Make sure you have a §5Jungle Key§f before searching for the Jungle Temple.")
                )
            }
            return
        }

        if (isCompassUnreliableFor(targetPoi)) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fIgnoring compass reading for §e$targetPoi§f, its crystal is placed/found. So this reading can't be trusted.")
            )
            return
        }

        val ray = fitRay(points, origin, targetPoi) ?: return

        onRaySolved(ray)

    }

    // Works out which POI the compass is currently pointing towards, based on what
    // zone of the Hollows the player is standing in.
    private fun resolveTargetPoi(): String? {
        val zone = LocationAPI.area.name

        return when {
            zone.contains("Goblin Holdout")     -> if (hasKingsScent()) "Goblin Queen's Den" else "King Yolkar"
            zone.contains("Precursor Remnants") -> "Lost Precursor City"
            zone.contains("Mithril Deposits")   -> "Mines of Divan"
            zone.contains("Magma Fields")       -> "Khazad-dûm"
            zone.contains("Jungle")             -> if (hasJungleKey()) "Jungle Temple" else "Odawa"

            else -> null
        }
    }

    private fun hasKingsScent(): Boolean {
        return System.currentTimeMillis() < kingsScentExpiresAt
    }

    // Called when the player picks up the "King's Scent" buff, which changes which POI a compass targets.
    fun onKingsScentGranted() {
        if (!ModConfig.get().NucleusMap) return
        if (!NucleusMap.inCrystalHollows) return

        kingsScentExpiresAt = System.currentTimeMillis() + KINGS_CENT_DURATION_MS
    }

    // Checks the player's inventory for a "Jungle Key" item.
    private fun hasJungleKey(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            val stack = inventory.getItem(i)
            if (!stack.isEmpty && stack.hoverName.string == "Jungle Key") return true
        }
        return false
    }

    // Takes the collected particle trail points and works out the best-fit
    // straight-line direction they're travelling in (math: like drawing the
    // straightest possible line through a cloud of dots).
    private fun fitRay(
        points: List<Triple<Double, Double, Double>>,
        origin: Triple<Double, Double, Double>,
        targetPoi: String
    ): Ray? {
        val cx = points.map { it.first }.average()
        val cy = points.map { it.second }.average()
        val cz = points.map { it.third }.average()

        var sxx = 0.0; var sxy = 0.0; var sxz = 0.0
        var syy = 0.0; var syz = 0.0; var szz = 0.0
        for (p in points) {
            val dx = p.first - cx
            val dy = p.second - cy
            val dz = p.third - cz
            sxx += dx * dx; sxy += dx * dy; sxz += dx * dz
            syy += dy * dy; syz += dy * dz; szz += dz * dz
        }

        var vx = points.last().first - points.first().first
        var vy = points.last().second - points.first().second
        var vz = points.last().third - points.first().third
        var vlen = sqrt(vx * vx + vy * vy + vz * vz)
        if (vlen < 1e-6) { vx = 1.0; vy = 0.0; vz = 0.0 } else { vx /= vlen; vy /= vlen; vz /= vlen }

        // Refine the direction estimate repeatedly until it stabilizes.
        repeat(25) {
            val nx = sxx * vx + sxy * vy + sxz * vz
            val ny = sxy * vx + syy * vy + syz * vz
            val nz = sxz * vx + syz * vy + szz * vz
            val nlen = sqrt(nx * nx + ny * ny + nz * nz)
            if (nlen > 1e-9) {
                vx = nx / nlen; vy = ny / nlen; vz = nz / nlen
            }
        }

        // Make sure the direction points toward the particle cloud, not away from it.
        val toCloudX = cx - origin.first
        val toCloudY = cy - origin.second
        val toCloudZ = cz - origin.third
        val dot = vx * toCloudX + vy * toCloudY + vz * toCloudZ
        if (dot < 0) { vx = -vx; vy = -vy; vz = -vz }

        return Ray(origin, Triple(vx, vy, vz), targetPoi)
    }

    // Called once we have a fresh direction ("ray") from a compass use. Needs two
    // rays (from two different spots) aimed at the same POI to actually solve its
    // location, so this stores the first one and waits for a second.
    private fun onRaySolved(ray: Ray) {
        if (!compassJustShattered) return
        compassJustShattered = false

        val first = pendingRay

        if (first == null) {
            // This is the first reading. store it and ask the player to try again elsewhere.
            pendingRay = ray
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasuakSKyblockZAddons§c]\n" +
                        "§fDirection captured for §e${ray.targetPoi}§f. Move to a different spot and use another compass to solve it.")
            )
            if (ray.targetPoi == "Odawa" && !hasJungleKey()) {
                Minecraft.getInstance().player?.sendSystemMessage(
                    Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                            "§fHINT!. You need a §5Jungle Key§f to find the Jungle Temple.")
                )
            }
            return
        }

        if (first.targetPoi != ray.targetPoi) {
            // Player is now aiming at a different POI than before. start over with this new one.
            pendingRay = ray
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fTarget changed to §e${ray.targetPoi}§f, direction captured. Use it again elsewhere to solve.")
            )
            if (ray.targetPoi == "Odawa" && !hasJungleKey()) {
                Minecraft.getInstance().player?.sendSystemMessage(
                    Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                            "§fHINT!. You need a §5Jungle Key§f to find the Jungle Temple.")
                )
            }
            return
        }

        // Two readings need to come from sufficiently different spots, or the
        // triangulation math becomes too inaccurate to trust.
        val baselineDist = distance3D(first.origin, ray.origin)
        if (baselineDist < MIN_BASELINE_DISTANCE) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fToo close to your first use (§e${"%.1f".format(baselineDist)}§f blocks). Move further away and use the compass again.")
            )
            return
        }

        pendingRay = null
        val solved = closestPointBetweenRays(first, ray)
        if (solved == null) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fRays too close to parallel to solve. Try again from a more different angle.")
            )
            return
        }

        val (x, y, z) = solved

        // Special case: if the solved spot for Odawa isn't even in the right
        // part of the map, it means Odawa didn't spawn in this world at all.
        if (ray.targetPoi == "Odawa" && !NucleusMap.isInJungleQuadrant(x, z)) {
            val firstTime = markOdawaUnreliable()
            if (firstTime) {
                Minecraft.getInstance().player?.sendSystemMessage(
                    Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                            "§fOdawa didn't spawn in this lobby.")
                )
            }
            return
        }

        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fSolved §e${ray.targetPoi}§f: x:${"%.1f".format(x)} z:${"%.1f".format(z)}")
        )

        NucleusMap.registerCompassSolvedPoi(ray.targetPoi, x, z)
    }

    private fun distance3D(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        val dz = a.third - b.third
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    // Given two direction lines (rays) from two different spots, finds the point
    // in space where they come closest to crossing - that's the solved location.
    private fun closestPointBetweenRays(a: Ray, b: Ray): Triple<Double, Double, Double>? {
        val (ox1, oy1, oz1) = a.origin
        val (dx1, dy1, dz1) = a.dir
        val (ox2, oy2, oz2) = b.origin
        val (dx2, dy2, dz2) = b.dir

        val rx = ox1 - ox2
        val ry = oy1 - oy2
        val rz = oz1 - oz2

        val a11 = dx1 * dx1 + dy1 * dy1 + dz1 * dz1
        val a12 = dx1 * dx2 + dy1 * dy2 + dz1 * dz2
        val a22 = dx2 * dx2 + dy2 * dy2 + dz2 * dz2
        val b1 = dx1 * rx + dy1 * ry + dz1 * rz
        val b2 = dx2 * rx + dy2 * ry + dz2 * rz

        val denom = a11 * a22 - a12 * a12
        if (abs(denom) < 1e-6) return null // rays are too close to parallel to find a clear crossing point

        val t1 = (a12 * b2 - a22 * b1) / denom
        val t2 = (a11 * b2 - a12 * b1) / denom

        val p1 = Triple(ox1 + dx1 * t1, oy1 + dy1 * t1, oz1 + dz1 * t1)
        val p2 = Triple(ox2 + dx2 * t2, oy2 + dy2 * t2, oz2 + dz2 * t2)

        return Triple((p1.first + p2.first) / 2, (p1.second + p2.second) / 2, (p1.third + p2.third) / 2)
    }

    // Clears all in-progress compass tracking data.
    fun reset() {
        currentTrail.clear()
        trailOrigin = null
        pendingRay = null
    }
}