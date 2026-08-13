package com.cbza.net.feature.mining.hollows

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.ParticleEvent
import com.cbza.net.event.events.WorldRenderEvent
import com.cbza.net.utility.rendering.Render3D

import net.minecraft.util.ARGB
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3

import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

import java.util.concurrent.ConcurrentHashMap

object PowderChestSolver {
	init {
		EventBus.subscribe(ChatEvent::class.java) { event ->
			val text = event.text
			if (text.contains("You uncovered a treasure chest!") && !text.contains("[DEBUG]")) {
				onChestSpawn()
			}
			if (text.contains("Sending to server")) {
				clearChests()
			}
		}
		EventBus.subscribe(ParticleEvent::class.java) { event ->
			if (event.options.type == ParticleTypes.CRIT) {
				handleParticle(event.x, event.y, event.z)
			}
		}
		EventBus.subscribe(WorldRenderEvent::class.java) { event ->
			render(event)
		}
	}

	private val chestExpireTimes = ConcurrentHashMap<String, Long>()
	private val activeChests = ConcurrentHashMap<String, Vec3>()

	@Volatile
	private var expectingChest = false

	@Volatile
	private var expectingChestTime = 0L

	private const val PARTICLE_POINT_LIFETIME_MS = 250L
	private const val PARTICLE_WINDOW_MS = 60000L

	private fun render(event: WorldRenderEvent) {
		if (!ModConfig.get().powderChestSolver) return
		val positions = getActiveChestPositions()
		if (positions.isEmpty()) return

		val buffer = event.bufferSource.getBuffer(RenderTypes.debugFilledBox())
		val color = ARGB.colorFromFloat(1.0f, 0.0f, 1.0f, 0.0f)
		val camPos = event.camPos

		for (targetPos in positions) {
			Render3D.drawBox(buffer, event.matrix,
				targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z,
				0.05, 0.05, 0.05, color)
		}
		event.bufferSource.endBatch(RenderTypes.debugFilledBox())
	}

	private fun isRecentlyMining(): Boolean {
		val mc = Minecraft.getInstance()
		return mc.options.keyAttack.isDown
	}

	fun onChestSpawn() {
		if (!ModConfig.get().powderChestSolver) return
		if (!SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()) return
		expectingChest = true
		expectingChestTime = System.currentTimeMillis()
	}

	fun handleParticle(x: Double, y: Double, z: Double) {
		if (!ModConfig.get().powderChestSolver) return
		if (!SkyBlockIsland.CRYSTAL_HOLLOWS.inIsland()) return
		if (!expectingChest) return

		val now = System.currentTimeMillis()
		if (now - expectingChestTime > PARTICLE_WINDOW_MS) {
			expectingChest = false
			return
		}

		if (isRecentlyMining()) return // actively swinging - almost certainly a mining crit, not a chest hint

		val adjustedY = y + ModConfig.get().powderChestYOffset
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