package com.cbza.net.feature.dungeons

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.BlockInteractEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.event.events.WorldRenderEvent
import com.cbza.net.utility.Render3D

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.core.BlockPos
import net.minecraft.util.ARGB
import net.minecraft.world.level.block.Blocks

import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object MimicChest {
    init {
        EventBus.subscribe(TickEvent::class.java) {
            tick()
        }
        EventBus.subscribe(WorldRenderEvent::class.java) { event ->
            render(event)
        }
        EventBus.subscribe(BlockInteractEvent::class.java) { event ->
            interact(event)
        }
    }

    private val trackedChests = mutableSetOf<BlockPos>()
    private val openedChests = mutableSetOf<BlockPos>()
    private var tickCounter = 0

    private fun render(event: WorldRenderEvent) {
        if (!ModConfig.get().MimicChest) return
        val positions = getMimicChestPositions()
        if (positions.isEmpty()) return

        val buffer = event.bufferSource.getBuffer(RenderTypes.debugFilledBox())
        val color = ARGB.colorFromFloat(1.0f, 1.0f, 0.0f, 0.0f)
        val camPos = event.camPos

        for (targetPos in positions) {
            val dx = targetPos.x + 0.5 - camPos.x
            val dy = targetPos.y + 0.5 - camPos.y
            val dz = targetPos.z + 0.5 - camPos.z
            Render3D.drawBoxDoubleSided(buffer, event.matrix, dx, dy, dz, 0.5, 0.5, 0.5, color)
        }
        event.bufferSource.endBatch(RenderTypes.debugFilledBox())
    }

    fun getMimicChestPositions(): List<BlockPos> {
        return trackedChests.toList()
    }

    fun tick() {
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
    }
    fun interact(event: BlockInteractEvent) {
        removeChest(event.blockPos)
    }

    fun removeChest(pos: BlockPos) {
        trackedChests.remove(pos)
        openedChests.add(pos)
    }
}