package com.cbza.net.feature.farming

import com.cbza.net.api.cushion.RenderContext
import com.cbza.net.api.cushion.mc
import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.TabListReader.getPestCooldown
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ARGB
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

enum class PestState { UNKNOWN, COUNTING, READY }

object PestKillTimer {

    private var state = PestState.UNKNOWN
    private var ticksLeft = 0
    private var warnedSoon = false
    private var lastCountdownSecond = -1
    private var tabCooldown = 0

    private var popupMessage: String? = null
    private var popupExpireTime = 0L

    init {
        EventBus.subscribe<ChatEvent> { event ->
            val clean = event.text.replace(Regex("§."), "")
            if (Regex("Pests? (have|has) spawned").containsMatchIn(clean))
                onPestsSpawned()
        }
        EventBus.subscribe<TickEvent> {
            tick()
        }
    }

    private fun onGarden() = SkyBlockIsland.GARDEN.inIsland()

    private fun tick() {
        if (!onGarden()) {
            state = PestState.UNKNOWN
            return
        }
        if (tabCooldown > 0) tabCooldown--
        if (tabCooldown == 0) {
            val isReady = getPestCooldown("Cooldown")?.contains("READY") == true
            if (isReady && state != PestState.READY) {
                state = PestState.READY
                showPopup("Pests can spawn now!", 3000L)
            }
        }

        if (state != PestState.COUNTING) return

        if (ticksLeft > 0) ticksLeft--
        val secondsLeft = (ticksLeft + 19) / 20

        if (secondsLeft <= 10 && secondsLeft > 5 && !warnedSoon) {
            warnedSoon = true
            showPopup("Pests spawn soon, switch gear!", 3000L)
        }
        if (secondsLeft in 1..5 && secondsLeft != lastCountdownSecond) {
            lastCountdownSecond = secondsLeft
            showPopup("Pests in $secondsLeft...", 1000L)
        }
    }

    private fun onPestsSpawned() {
        if (!onGarden()) return
        state = PestState.COUNTING
        ticksLeft = ModConfig.get().farming.spawnIntervalSeconds * 20
        warnedSoon = false
        lastCountdownSecond = -1
        tabCooldown = 100
    }

    private fun showPopup(text: String, durationMs: Long) {
        if (!ModConfig.get().farming.PestKillTimer) return

        popupMessage = text
        popupExpireTime = System.currentTimeMillis() + durationMs

        mc.execute {
            mc.player?.playSound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                1.0f, 2.0f
            )
        }
    }

    private fun getActivePopup(): String? {
        val msg = popupMessage ?: return null
        if (System.currentTimeMillis() > popupExpireTime) {
            popupMessage = null
            return null
        }
        return msg
    }

    fun render(graphics: RenderContext) {
        val cfg = ModConfig.get().farming
        if (!cfg.PestKillTimer) return

        val popup = getActivePopup() ?: return
        val pos = cfg.pestKillTimerPos
        val scale = pos.scale

        val textWidth = (mc.font.width(popup) * scale).toInt()
        val x = if (pos.x == -1) (mc.window.guiScaledWidth - textWidth) / 2 else pos.x
        val y = if (pos.y == -1) mc.window.guiScaledHeight / 3 else pos.y

        graphics.pose().pushMatrix()
        graphics.pose().scale(scale, scale)
        graphics.text(mc.font, popup, (x / scale).toInt(), (y / scale).toInt(), ARGB.opaque(ColorCatalog.RED), true)
        graphics.pose().popMatrix()
    }
}