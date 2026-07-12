package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.BlockStrengths
import com.cbza.net.utility.TabListReader
import com.cbza.net.utility.PingTracker
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import tech.thatgravyboat.skyblockapi.api.area.mining.MiningBlock

object PingGlide {

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

    private var lastBreakingTime = 0L
    private const val STOP_MINING_GRACE_MS = 400L // tune this — too short brings back flicker, too long feels delayed

    private fun startMiningTimer(pos: BlockPos) {
        val mc = Minecraft.getInstance()
        val blockMatch = MiningBlock.currentlyActiveBlocks.firstOrNull {
            it.blocks.contains(mc.level?.getBlockState(pos)?.block)
        } ?: return
        val blockKey = blockMatch.name
        val miningSpeed = TabListReader.getMiningSpeed()
        if (miningSpeed == null) {
            val now = System.currentTimeMillis()
            if (now - lastMiningSpeedWarn > WARN_INTERVAL_MS) {
                lastMiningSpeedWarn = now
                mc.player?.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                        "§c[§6CasualSkyblockAddons§c] §fPingGlide needs §eMining Speed §fvisible in your tab list — enable it in your §eSkyBlock §fstats settings."
                    )
                )
            }
            return
        }
        val ticks = BlockStrengths.calculateTicks(blockKey, miningSpeed) ?: return
        val ms = BlockStrengths.ticksToMs(ticks)
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

    fun tick() {
        if (!ModConfig.get().PingGlide) return

        val mc = Minecraft.getInstance()
        val hit = mc.hitResult as? BlockHitResult
        val targetPos = hit?.blockPos
        val now = System.currentTimeMillis()

        if (isBreakingBlock()) {
            lastBreakingTime = now
            if (targetPos != null && lastTargetedPos != targetPos) {
                val blockMatch = MiningBlock.currentlyActiveBlocks.firstOrNull {
                    it.blocks.contains(mc.level?.getBlockState(targetPos)?.block)
                }
                if (blockMatch != null) {
                    startMiningTimer(targetPos)
                } else {
                    // switched to an untracked block — clear the old tracked one
                    _isCurrentlyMining = false
                    _currentBlockPos = null
                }
            }
            lastTargetedPos = targetPos?.immutable()
        } else {
            lastTargetedPos = null
            // stopped breaking entirely (not just a brief look-away) — clear after grace period
            if (_isCurrentlyMining && now - lastBreakingTime > STOP_MINING_GRACE_MS) {
                _isCurrentlyMining = false
                println("[PingGlide] Cleared mining state after grace period")
            }
        }

        // only reset if timer expired naturally
        if (_isCurrentlyMining && System.currentTimeMillis() >= (currentMineStartTime + (currentTotalMs ?: Long.MAX_VALUE) + 500)) {
            _isCurrentlyMining = false
        }

        val broken = MiningBlock.lastBrokenBlock
        if (broken != null && broken != lastSeenBlock) {
            lastSeenBlock = broken
            // only reset if the broken block is the one we were tracking
            if (broken.first == _currentBlockPos) {
                _isCurrentlyMining = false
            }
        }
    }

    private fun isBreakingBlock(): Boolean {
        val mc = Minecraft.getInstance()
        return mc.gameMode?.isDestroying() ?: false
    }

    fun getPing(): Int {
        return PingTracker.getPing()?.toInt() ?: ModConfig.get().manualPing
    }

    fun getElapsedMs(): Long {
        if (!_isCurrentlyMining) return 0L
        return System.currentTimeMillis() - currentMineStartTime
    }

    fun isSafeToMove(): Boolean {
        val safe = currentSafeToMoveMs ?: return false
        return getElapsedMs() >= safe
    }
}