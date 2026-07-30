/*
 * Feature ideah inspired by PingOfsetMiner created by Revvilon
 */

package com.cbza.net.feature.mining.general

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.BlockStrength
import com.cbza.net.utility.PingTracker
import com.cbza.net.utility.TabListReader
import com.cbza.net.utility.TpsTracker
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.TickEvent
import com.cbza.net.event.events.WorldRenderEvent
import com.cbza.net.utility.Render3D
import com.mojang.blaze3d.vertex.PoseStack

import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.util.ARGB
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.tags.ItemTags
import net.minecraft.world.phys.BlockHitResult

import tech.thatgravyboat.skyblockapi.api.area.mining.MiningBlock
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId

// Figures out, based on the block being mined and the player's ping/mining speed,
// how long it will actually take to break it. so the player knows the exact
// moment it's "safe" to move on to the next block without wasting the swing.
object PingGlide {
    init {
        EventBus.subscribe(TickEvent::class.java) {
            tick()
        }
        EventBus.subscribe(WorldRenderEvent::class.java) { event ->
            render(event)
        }
    }

    @Volatile var currentMineStartTime: Long = 0L
    @Volatile var currentSafeToMoveMs: Long? = null
    @Volatile var currentTotalMs: Long? = null
    @Volatile private var _currentBlockPos: BlockPos? = null
    @Volatile private var _isCurrentlyMining: Boolean = false

    private var lastSeenBlock: Pair<Any, MiningBlock>? = null
    private var lastTargetedPos: BlockPos? = null
    private var warnedNoMiningSpeed = false

    private var lastMiningSpeedWarn = 0L
    private const val WARN_INTERVAL_MS = 5000L

    private val eligibleIslands = setOf(
        SkyBlockIsland.CRYSTAL_HOLLOWS,
        SkyBlockIsland.DWARVEN_MINES,
        SkyBlockIsland.MINESHAFT,
        SkyBlockIsland.THE_END
    )

    private fun render(event: WorldRenderEvent) {
        if (!ModConfig.get().PingGlide) return
        if (!_isCurrentlyMining) return
        val blockPos = _currentBlockPos ?: return

        val mc = Minecraft.getInstance()
        val safe = isSafeToMove()
        val outlineColor = if (safe) ARGB.colorFromFloat(1.0f, 0.0f, 1.0f, 0.0f) else ARGB.colorFromFloat(1.0f, 1.0f, 0.0f, 0.0f)
        val fillColor = if (safe) ARGB.colorFromFloat(0.3f, 0.0f, 1.0f, 0.0f) else ARGB.colorFromFloat(0.3f, 1.0f, 0.0f, 0.0f)

        val level = mc.level
        val shape = level?.getBlockState(blockPos)?.getShape(level, blockPos) ?: Shapes.block()
        val camPos = event.camPos
        val dx = blockPos.x - camPos.x
        val dy = blockPos.y - camPos.y
        val dz = blockPos.z - camPos.z

        val fillBuffer = event.bufferSource.getBuffer(RenderTypes.debugQuads())
        shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
            Render3D.drawBox(fillBuffer, event.matrix,
                dx + (x1 + x2) / 2, dy + (y1 + y2) / 2, dz + (z1 + z2) / 2,
                (x2 - x1) / 1.9, (y2 - y1) / 1.9, (z2 - z1) / 1.9,
                fillColor)
        }
        event.bufferSource.endBatch(RenderTypes.debugQuads())

        val outlinePoseStack = PoseStack()
        outlinePoseStack.last().pose().set(event.matrix)
        val lineBuffer = event.bufferSource.getBuffer(RenderTypes.lines())
        ShapeRenderer.renderShape(outlinePoseStack, lineBuffer, shape, dx, dy, dz, outlineColor, 10.0f)
        event.bufferSource.endBatch(RenderTypes.lines())
    }

    // Only run this feature on islands where mining actually happens.
    private fun isEligibleIsland(): Boolean = eligibleIslands.any { it.inIsland() }

    // Checks whether the player is holding a pickaxe or one of the drills.
    fun isHoldingMiningTool(): Boolean {
        val heldItem = Minecraft.getInstance().player?.mainHandItem
        val isPickaxe = heldItem?.`is`(ItemTags.PICKAXES) == true
        val id = heldItem?.getSkyBlockId()?.skyblockId
        val drillIds = setOf("MITHRIL_DRILL_1", "MITHRIL_DRILL_2", "GEMSTONE_DRILL_1", "GEMSTONE_DRILL_2", "GEMSTONE_DRILL_3", "GEMSTONE_DRILL_4", "TITANIUM_DRILL_1", "TITANIUM_DRILL_2", "TITANIUM_DRILL_3", "TITANIUM_DRILL_4", "DIVAN_DRILL")
        val isDrill = id in drillIds
        return isPickaxe || isDrill
    }

    // Called when the player starts mining a new block. Works out how long the
    // block will take to break (using mining speed + server lag/ping) and stores
    // that so the rest of the code knows when it's safe to move on.
    private fun startMiningTimer(pos: BlockPos) {
        val mc = Minecraft.getInstance()
        val blockMatch = MiningBlock.currentlyActiveBlocks.firstOrNull {
            it.blocks.contains(mc.level?.getBlockState(pos)?.block)
        } ?: return
        val blockKey = blockMatch.name
        val miningSpeed = TabListReader.getMiningSpeed()
        if (miningSpeed == null) {
            // Can't calculate anything without mining speed. warn the player (but not too often).
            val now = System.currentTimeMillis()
            if (now - lastMiningSpeedWarn > WARN_INTERVAL_MS) {
                lastMiningSpeedWarn = now
                mc.player?.sendSystemMessage(
                    Component.literal(
                        "§c[§6CasualSkyblockZAddons§c]\n" +
                                "§fPingGlide needs §eMining Speed §fvisible in your tab list, enable it in your §eSkyBlock §fstats settings."
                    )
                )
            }
            return
        }
        val ticks = BlockStrength.calculateTicks(blockKey, miningSpeed) ?: return
        val tps = TpsTracker.getAverageTps() ?: 20.0
        val ms = BlockStrength.ticksToMs(ticks, tps)
        val ping = getPing()
        val safeToMoveMs = (ms - (ping / 2)).coerceAtLeast(0L)
        currentMineStartTime = System.currentTimeMillis()
        currentTotalMs = ms
        currentSafeToMoveMs = safeToMoveMs
        _currentBlockPos = pos
        _isCurrentlyMining = true
    }

    fun isCurrentlyMining(): Boolean = _isCurrentlyMining
    fun getCurrentBlockPos(): BlockPos? = _currentBlockPos

    // Runs every game tick. Watches what block the player is aiming at and
    // whether they're actively mining it, and kicks off/cancels the timer as needed.
    fun tick() {
        if (!ModConfig.get().PingGlide) return
        if (!isEligibleIsland()) {
            _isCurrentlyMining = false
            return
        }
        if (!isHoldingMiningTool()) {
            _isCurrentlyMining = false
            return
        }
        val mc = Minecraft.getInstance()
        val hit = mc.hitResult as? BlockHitResult
        val targetPos = hit?.blockPos

        if (isBreakingBlock()) {
            // Player is holding down attack on a block. start timing it if it's a new target.
            if (targetPos != null && lastTargetedPos != targetPos) {
                val blockMatch = MiningBlock.currentlyActiveBlocks.firstOrNull {
                    it.blocks.contains(mc.level?.getBlockState(targetPos)?.block)
                }
                if (blockMatch != null) {
                    startMiningTimer(targetPos)
                } else {
                    _isCurrentlyMining = false
                    _currentBlockPos = null
                }
            }
            lastTargetedPos = targetPos?.immutable()
        } else {
            // Not mining anything right now.
            lastTargetedPos = null
            _isCurrentlyMining = false
        }

        // Safety cutoff: if way more time has passed than the block should've taken, stop tracking it.
        if (_isCurrentlyMining && System.currentTimeMillis() >= (currentMineStartTime + (currentTotalMs ?: Long.MAX_VALUE) + 500)) {
            _isCurrentlyMining = false
        }

        // If the game tells us the block actually broke, stop tracking it right away.
        val broken = MiningBlock.lastBrokenBlock
        if (broken != null && broken != lastSeenBlock) {
            lastSeenBlock = broken
            if (broken.first == _currentBlockPos) {
                _isCurrentlyMining = false
            }
        }
    }

    // True while the player is holding down the attack button while aiming at a block.
    private fun isBreakingBlock(): Boolean {
        val mc = Minecraft.getInstance()
        return mc.options.keyAttack.isDown && mc.hitResult is BlockHitResult
    }

    // Gets the player's current ping (delay to the server), or a manually set fallback value.
    fun getPing(): Int {
        return PingTracker.getPing()?.toInt() ?: ModConfig.get().manualPing
    }

    // How long the player has been mining the current block, in milliseconds.
    fun getElapsedMs(): Long {
        if (!_isCurrentlyMining) return 0L
        return System.currentTimeMillis() - currentMineStartTime
    }

    // Whether enough time has passed that it's now safe to move on to the next block.
    fun isSafeToMove(): Boolean {
        val safe = currentSafeToMoveMs ?: return false
        return getElapsedMs() >= safe
    }
}