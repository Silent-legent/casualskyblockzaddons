package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.util.concurrent.ConcurrentHashMap

object PowderChestSolver {

	private val chestExpireTimes = ConcurrentHashMap<String, Long>()
	private val activeChests = ConcurrentHashMap<String, Vec3>()

	@Volatile
	private var expectingChest = false

	@Volatile
	private var expectingChestTime = 0L

	private const val PARTICLE_POINT_LIFETIME_MS = 250L
	private const val PARTICLE_WINDOW_MS = 60000L

	private fun isRecentlyMining(): Boolean {
		val mc = Minecraft.getInstance()
		return mc.options.keyAttack.isDown
	}

	fun onChestSpawn() {
		if (!ModConfig.get().PowderChestSolver) return
		if (!SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()) return
		expectingChest = true
		expectingChestTime = System.currentTimeMillis()
	}

	fun handleParticle(x: Double, y: Double, z: Double) {
		if (!ModConfig.get().PowderChestSolver) return
		if (!SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()) return
		if (!expectingChest) return

		val now = System.currentTimeMillis()
		if (now - expectingChestTime > PARTICLE_WINDOW_MS) {
			expectingChest = false
			return
		}

		if (isRecentlyMining()) return // actively swinging - almost certainly a mining crit, not a chest hint

		val adjustedY = y + ModConfig.get().PowderChestYOffset
		val key = "${Math.round(x)},${Math.round(adjustedY)},${Math.round(z)}"
		activeChests[key] = Vec3(x, adjustedY, z)
		chestExpireTimes[key] = now + PARTICLE_POINT_LIFETIME_MS
	}

	fun getActiveChestPositions(): List<Vec3> {
		val now = System.currentTimeMillis()
		chestExpireTimes.entries.removeIf { entry ->
			if (now > entry.value) {
				activeChests.remove(entry.key)
				true
			} else {
				false
			}
		}
		return ArrayList(activeChests.values)
	}

	fun clearChests() {
		activeChests.clear()
		chestExpireTimes.clear()
		expectingChest = false
	}
}