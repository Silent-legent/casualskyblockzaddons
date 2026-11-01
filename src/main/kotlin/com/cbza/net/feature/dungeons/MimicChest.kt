package com.cbza.net.feature.dungeons

import com.cbza.net.api.astrum.Astrum
import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.event.events.RenderEvent

import net.minecraft.client.Minecraft
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
    private val openedChests = mutableSetOf<BlockPos>()
    private var tickCounter = 0
    private var clearChestsDelay = 0

    private fun render() {
        if (!ModConfig.get().mimicChest) return
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
        if (clearChestsDelay > 0) {
            clearChestsDelay--
            if (clearChestsDelay == 0) {
                trackedChests.clear()
                openedChests.clear()
            }
        }

        if (!SkyBlockIsland.THE_CATACOMBS.inIsland()) {
            trackedChests.clear()
            openedChests.clear()
            tickCounter = 0
            return
        }

        tickCounter++
        if (tickCounter < 10) return
        tickCounter = 0

        val player = Minecraft.getInstance().player ?: return
        val level = Minecraft.getInstance().level ?: return
        val radius = 16

        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val pos = player.blockPosition().offset(x, y, z)
                    if (pos in openedChests) continue
                    if (level.getBlockState(pos).block == Blocks.TRAPPED_CHEST) {
                        trackedChests.add(pos)
                    }
                }
            }
        }
        trackedChests.removeIf { chestPos -> level.getBlockState(chestPos).block != Blocks.TRAPPED_CHEST }
    }

    fun clearChests() {
        clearChestsDelay = 60 // 3s
    }
}