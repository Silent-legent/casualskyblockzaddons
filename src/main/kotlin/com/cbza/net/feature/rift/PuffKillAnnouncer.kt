package com.cbza.net.feature.rift

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.SoundPlayedEvent
import com.cbza.net.utility.ColorCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundEvents.CHICKEN_EGG
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockAreas
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object PuffKillAnnouncer {
    init {
        EventBus.subscribe<SoundPlayedEvent> { event ->
            if (event.sound == (CHICKEN_EGG)) {
                onPuffDying()
            }
        }
    }

    var inRift = false
    var onMountainTop = false

    var popupMessage: String? = null
    var popupExpireTime: Long = 0L
    const val POPUP_DURATION_MS = 3000L

    var lastPopupTime: Long = 0L
    const val POPUP_COOLDOWN_MS = 25000L


        fun onPuffDying() {
         if (!ModConfig.get().puffKillAnnouncer) return
            inRift = SkyBlockIsland.THE_RIFT.inIsland()
            onMountainTop = SkyBlockAreas.THE_MOUNTAINTOP.inArea()

            if (!inRift || !onMountainTop)
                return

            showPopup("Kill Puffs!")
        }

        fun showPopup(message: String) {
            val now = System.currentTimeMillis()
            if (message == popupMessage && now - lastPopupTime < POPUP_COOLDOWN_MS) return
            popupMessage = message
            lastPopupTime = now
            popupExpireTime = now + POPUP_DURATION_MS

            val mc = Minecraft.getInstance()
            mc.execute {
                mc.player?.playSound(
                    SoundEvents.NOTE_BLOCK_PLING.value(),
                    1.0f,
                    2.0f
                )
            }
        }

    fun getActivePopup(): String? {
        val msg = popupMessage ?: return null
        if (System.currentTimeMillis() > popupExpireTime) {
            popupMessage = null
            return null
        }
        return msg
    }

        fun render(graphics: GuiGraphicsExtractor) {
            val cfg = ModConfig.get()
            if (!cfg.puffKillAnnouncer) return

            val popup = getActivePopup() ?: return
            val mc = Minecraft.getInstance()
            val startX = cfg.puffKillAnnouncerX
            val startY = cfg.puffKillAnnouncerY
            val scale = cfg.puffKillAnnouncerScale

            graphics.pose().pushMatrix()
            graphics.pose().scale(scale, scale)

            val ux = (startX / scale).toInt()
            val uy = (startY / scale).toInt()

            val color = ColorCatalog.RED
            graphics.text(mc.font, popup, ux, uy, color, true)
        }
}