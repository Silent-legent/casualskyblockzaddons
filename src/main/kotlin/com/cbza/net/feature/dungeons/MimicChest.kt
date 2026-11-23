package com.cbza.net.feature.dungeons

import com.cbza.net.api.astrum.Astrum
import com.cbza.net.api.cushion.mc
import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.event.events.RenderEvent

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.awt.Color

object MimicChest {
    init {
        EventBus.subscribe<TickEvent> {
            tick()
        }
        EventBus.subscribe<RenderEvent> { event ->
            render()
        }
        EventBus.subscribe<ChatEvent> { event ->
            if (event.text.contains("The Catacombs")) {
                clearChests()
            }
        }
    }

    private val trackedChests = mutableSetOf<BlockPos>()
    private var tickCounter = 0
    private var clearChestsDelay = 0

    private fun render() {
        if (!ModConfig.get().dungeons.mimicChest) return
        val positions = getMimicChestPositions()
        if (positions.isEmpty()) return

        for (targetPos in positions) {
            Astrum.queueBox(AABB(targetPos), Color(255, 0, 0), true, true, 1f)
        }
    }

        fun getMimicChestPositions(): List<BlockPos> {
        return trackedChests.toList()
    }

    fun tick() {
        if (!ModConfig.get().dungeons.mimicChest) {
            reset()
            return
        }

        if (clearChestsDelay > 0) {
            clearChestsDelay--
            if (clearChestsDelay == 0) {
                trackedChests.clear()
            }
        }

        if (!SkyBlockIsland.THE_CATACOMBS.inIsland()) {
            reset()
            return
        }

        tickCounter++
        if (tickCounter < ModConfig.get().dungeons.mimicChestCheckTimer) return
        tickCounter = 0

        val player = mc.player ?: return
        val level = mc.level ?: return
        val radius = ModConfig.get().dungeons.mimicChestRadius
        val center = player.blockPosition()

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val pos = center.offset(x, y, z)
                    if (level.getBlockState(pos).block == Blocks.TRAPPED_CHEST) {
                        trackedChests.add(pos)
                    }
                }
            }
        }
        trackedChests.removeIf { chestPos -> level.getBlockState(chestPos).block != Blocks.TRAPPED_CHEST }
    }
    fun reset() {
        trackedChests.clear()
        clearChestsDelay = 0
        tickCounter = 0
    }

    fun clearChests() {
        clearChestsDelay = 60
    }
}