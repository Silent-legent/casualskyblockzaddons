package com.cbza.net.feature.rift

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.SoundPlayedEvent
import com.cbza.net.event.events.TickEvent
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
        EventBus.subscribe<ChatEvent> { event ->
            if (event.text.contains("BUFF! A vending machine splashed you with Gravity I!")) {
                hasGravityI()
            }
        }
        EventBus.subscribe<TickEvent> {
            tick()
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
        if (now - lastPopupTime < POPUP_COOLDOWN_MS) return

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

    var gravityTime = 0

    fun hasGravityI() {
        gravityTime = 611
    }

    var tickCounter = 0

    fun tick() {
            tickCounter++
            if (tickCounter >= 20) {
                tickCounter = 0

                if (gravityTime > 0) {
                    gravityTime--
                    if (gravityTime == 60) sendMINwarning()
                    if (gravityTime in 1..10) sendCountdownMessage(gravityTime)
                    if (gravityTime == 0) sendExpiredMessage()
            }
        }
        return
    }

    fun sendMINwarning() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§5Gravity I§c §fexpires in 1min!"
        )
        client.player?.sendSystemMessage(msg)
    }

    fun sendCountdownMessage(seconds: Int) {
        val client = Minecraft.getInstance()
        val mc = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§5Gravity I§c §fexpires in ${seconds}sec!"
        )
        client.player?.sendSystemMessage(msg)
        mc.execute {
            mc.player?.playSound(
                SoundEvents.NOTE_BLOCK_PLING.value(),
                1.0f,
                2.0f,
            )
        }
    }

    fun sendExpiredMessage() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§5Gravity I§c §fhas expired!"
        )
        client.player?.sendSystemMessage(msg)

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
