package com.cbza.net.utility

import com.cbza.net.config.ModConfig
import com.cbza.net.feature.NucleusMap
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI

object WishingCompassSolver {

    private const val TRAIL_WINDOW_MS = 250L
    private const val MIN_TRAIL_POINTS = 20
    private const val MAX_STEP_DISTANCE = 3.0 // blocks
    private const val MIN_BASELINE_DISTANCE = 5.0 // blocks
    private const val KINGS_CENT_DURATION_MS = 20 * 60 * 1000L

    private var kingsScentExpiresAt: Long = 0L

    data class Ray(
        val origin: Triple<Double, Double, Double>,
        val dir: Triple<Double, Double, Double>,
        val targetPoi: String
    )

    private var currentTrail = mutableListOf<Triple<Double, Double, Double>>()
    private var lastParticleTime = 0L
    private var trailOrigin: Triple<Double, Double, Double>? = null
    private var pendingRay: Ray? = null

    private val poiCrystalMap = mapOf(
        "King Yolkar" to "Amber",
        "Goblin Queen's Den" to "Amber",
        "Lost Precursor City" to "Sapphire",
        "Mines of Divan" to "Jade",
        "Jungle Temple" to "Amethyst",
        "Khazad-dûm" to "Topaz",
        "Key Guardian" to ""
    )

    private fun isCompassUnreliableFor(targetPoi: String): Boolean {
        val crystalName = poiCrystalMap[targetPoi] ?: return false
        val status = TabListReader.getCrystalStatus(crystalName) ?: return false
        return !status.contains("Not Found", ignoreCase = true)
    }

    fun handleParticle(x: Double, y: Double, z: Double) {
        if (!ModConfig.get().NucleusMap) return
        if (!NucleusMap.inCrystalHollows) return

        val now = System.currentTimeMillis()
        if (now - lastParticleTime > TRAIL_WINDOW_MS && currentTrail.isNotEmpty()) {
            finishTrail()
        }

        val last = currentTrail.lastOrNull()
        if (last != null) {
            val dx = x - last.first
            val dy = y - last.second
            val dz = z - last.third
            val stepDist = kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            if (stepDist > MAX_STEP_DISTANCE) {
                return // too far from the last real point - not part of this trail, drop it silently
            }
        }

        if (currentTrail.isEmpty()) {
            val player = Minecraft.getInstance().player
            trailOrigin = if (player != null) Triple(player.x, player.y, player.z) else Triple(x, y, z)
        }

        lastParticleTime = now
        currentTrail.add(Triple(x, y, z))
    }

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

        if (isCompassUnreliableFor(targetPoi)) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c] §fIgnoring compass reading for §e$targetPoi§f, its crystal is placed/found. So this reading can't be trusted.")
            )
            return
        }

        val ray = fitRay(points, origin, targetPoi) ?: return

        onRaySolved(ray)
    }

    private fun resolveTargetPoi(): String? {
        val zone = LocationAPI.area.name

        return when {
            zone.contains("Goblin Holdout")     -> if (hasKingsScent()) "Goblin Queen's Den" else "King Yolkar"
            zone.contains("Precursor Remnants") -> "Lost Precursor City"
            zone.contains("Mithril Deposits")   -> "Mines of Divan"
            zone.contains("Magma Fields")       -> "Khazad-dûm"
            zone.contains("Jungle")             -> {
                if (hasJungleKey()) {
                    "Jungle Temple"
                } else {
                    // Send the player a warning message!
                    Minecraft.getInstance().player?.sendSystemMessage(
                        Component.literal("§c[§6CasualSkyblockZAddons§c] §fIgnoring compass reading. You need a §5Jungle Key§f to find the Jungle Temple.")
                    )
                    null // Still returns null to stop the solver safely!
                }
            }
            else -> null
        }
    }

    private fun hasKingsScent(): Boolean {
        return System.currentTimeMillis() < kingsScentExpiresAt
    }
    fun onKingsScentGranted() {
        if (!ModConfig.get().NucleusMap) return
        if (!NucleusMap.inCrystalHollows) return

        kingsScentExpiresAt = System.currentTimeMillis() + KINGS_CENT_DURATION_MS
    }

    private fun hasJungleKey(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val inventory = player.inventory
        for (i in 0 until inventory.containerSize) {
            val stack = inventory.getItem(i)
            if (!stack.isEmpty && stack.hoverName.string == "Jungle Key") return true
        }
        return false
    }

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
        var vlen = kotlin.math.sqrt(vx * vx + vy * vy + vz * vz)
        if (vlen < 1e-6) { vx = 1.0; vy = 0.0; vz = 0.0 } else { vx /= vlen; vy /= vlen; vz /= vlen }

        repeat(25) {
            val nx = sxx * vx + sxy * vy + sxz * vz
            val ny = sxy * vx + syy * vy + syz * vz
            val nz = sxz * vx + syz * vy + szz * vz
            val nlen = kotlin.math.sqrt(nx * nx + ny * ny + nz * nz)
            if (nlen > 1e-9) {
                vx = nx / nlen; vy = ny / nlen; vz = nz / nlen
            }
        }

        val toCloudX = cx - origin.first
        val toCloudY = cy - origin.second
        val toCloudZ = cz - origin.third
        val dot = vx * toCloudX + vy * toCloudY + vz * toCloudZ
        if (dot < 0) { vx = -vx; vy = -vy; vz = -vz }

        return Ray(origin, Triple(vx, vy, vz), targetPoi)
    }

    private fun onRaySolved(ray: Ray) {
        val first = pendingRay

        if (first == null) {
            pendingRay = ray
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasuakSKyblockZAddons§c] §fDirection captured for §e${ray.targetPoi}§f. Move to a different spot and use another compass to solve it.")
            )
            return
        }

        if (first.targetPoi != ray.targetPoi) {
            pendingRay = ray
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c] §fTarget changed to §e${ray.targetPoi}§f, direction captured. Use it again elsewhere to solve.")
            )
            return
        }

        val baselineDist = distance3D(first.origin, ray.origin)
        if (baselineDist < MIN_BASELINE_DISTANCE) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c] §fToo close to your first use (§e${"%.1f".format(baselineDist)}§f blocks). Move further away and use the compass again.")
            )
            return
        }

        pendingRay = null
        val solved = closestPointBetweenRays(first, ray)
        if (solved == null) {
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c] §fRays too close to parallel to solve. Try again from a more different angle.")
            )
            return
        }

        val (x, y, z) = solved
        Minecraft.getInstance().player?.sendSystemMessage(
            Component.literal("§c[§6CasualSkyblockZAddons§c] §fSolved §e${ray.targetPoi}§f: x:${"%.1f".format(x)} z:${"%.1f".format(z)}")
        )

        NucleusMap.registerCompassSolvedPoi(ray.targetPoi, x, z)
    }

    private fun distance3D(a: Triple<Double, Double, Double>, b: Triple<Double, Double, Double>): Double {
        val dx = a.first - b.first
        val dy = a.second - b.second
        val dz = a.third - b.third
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

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
        if (kotlin.math.abs(denom) < 1e-6) return null

        val t1 = (a12 * b2 - a22 * b1) / denom
        val t2 = (a11 * b2 - a12 * b1) / denom

        val p1 = Triple(ox1 + dx1 * t1, oy1 + dy1 * t1, oz1 + dz1 * t1)
        val p2 = Triple(ox2 + dx2 * t2, oy2 + dy2 * t2, oz2 + dz2 * t2)

        return Triple((p1.first + p2.first) / 2, (p1.second + p2.second) / 2, (p1.third + p2.third) / 2)
    }

    fun reset() {
        currentTrail.clear()
        trailOrigin = null
        pendingRay = null
    }
}