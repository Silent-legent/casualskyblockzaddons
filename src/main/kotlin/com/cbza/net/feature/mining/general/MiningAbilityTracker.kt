package com.cbza.net.feature.mining.general

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.event.events.TickEvent
import com.cbza.net.utility.ColorCatalog

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents

import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

import kotlin.math.abs

object MiningAbilityTracker {
    init {
        EventBus.subscribe<ChatEvent> { event ->
            if (event.text.contains("You used your") && event.text.contains("Pickaxe Ability!")) {
                onAbilityUsed(event.text)
            }
            if (event.text.contains("is now available!")) {
                onAbilityReady(event.text)
            }
        }
        EventBus.subscribe<TickEvent> {
            tick()
        }
        EventBus.subscribe<ServerJoinEvent> {
            onServerJoin()
        }
    }

    private val abilityNames = listOf(
        "Pickobulus",
        "Mining Speed Boost",
        "Maniac Miner",
        "Tunnel Vision",
        "Sheer Force",
        "Gemstone Infusion",
    )

    @Volatile private var lastServerJoinTime = System.currentTimeMillis()
    private const val STARTUP_GRACE_MS = 3000L

    @Volatile var popupMessage: String? = null
    @Volatile var popupExpireTime: Long = 0L
    private const val POPUP_DURATION_MS = 3000L

    @Volatile private var activeAbilityName: String = ""
    @Volatile private var waitingForReady: Boolean = false
    @Volatile private var readyTime: Long = 0L
    @Volatile private var gotTimeFromTab: Boolean = false

    @Volatile private var lastPopupTime: Long = 0L
    @Volatile private var lastPopupMessage: String? = null
    private const val POPUP_COOLDOWN_MS = 500L

    @Volatile private var abilityUsedTime: Long = 0L
    private const val TAB_READ_DELAY_MS = 500L

    private const val MIN_PLAUSIBLE_SECONDS = 10L

    @Volatile private var pendingTabSeconds: Long? = null
    @Volatile private var pendingTabReadTime: Long = 0L

    @Volatile private var hasWarnedTabMissingThisIsland: Boolean = false

    private val miningIslands = setOf(
        SkyBlockIsland.DWARVEN_MINES,
        SkyBlockIsland.CRYSTAL_HOLLOWS,
        SkyBlockIsland.MINESHAFT,
        SkyBlockIsland.CRIMSON_ISLE
    )
    private var wasOnMiningIsland = false

    fun onAbilityUsed(chatMessage: String) {
        if (!ModConfig.get().miningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        val match = abilityNames.firstOrNull { chatMessage.contains(it) } ?: return
        activeAbilityName = match
        waitingForReady = true
        gotTimeFromTab = false
        readyTime = Long.MAX_VALUE
        pendingTabSeconds = null
        abilityUsedTime = System.currentTimeMillis()
    }

    fun onAbilityReady(chatMessage: String) {
        if (!ModConfig.get().miningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        if (!waitingForReady) return
        if (activeAbilityName.isEmpty()) return
        if (!chatMessage.contains(activeAbilityName)) return
        if (gotTimeFromTab) return
        if (System.currentTimeMillis() - abilityUsedTime < MIN_PLAUSIBLE_SECONDS * 1000L) return
        waitingForReady = false
        showPopup("$activeAbilityName Ready!")
    }

    fun tick() {
        val onMiningIsland = miningIslands.any { it.inIsland() }
        if (wasOnMiningIsland && !onMiningIsland) reset()
        wasOnMiningIsland = onMiningIsland
        if (!onMiningIsland) return
        if (!ModConfig.get().miningAbilityAnnouncer) return
        if (!waitingForReady) return

        if (!gotTimeFromTab && System.currentTimeMillis() - abilityUsedTime > TAB_READ_DELAY_MS) {
            val seconds = readTabListCooldownSeconds()
            val now = System.currentTimeMillis()

            if (seconds == null || seconds <= MIN_PLAUSIBLE_SECONDS) {
            } else {
                val pending = pendingTabSeconds
                if (pending == null) {
                    pendingTabSeconds = seconds
                    pendingTabReadTime = now
                } else {
                    val elapsedSec = (now - pendingTabReadTime) / 1000.0
                    val isConsistent = abs((pending - elapsedSec) - seconds) <= 1.0

                    if (isConsistent) {
                        readyTime = now + (seconds * 1000L)
                        gotTimeFromTab = true
                        pendingTabSeconds = null
                    } else {
                        pendingTabSeconds = seconds
                        pendingTabReadTime = now
                    }
                }
            }
        }

        if (System.currentTimeMillis() >= readyTime) {
            waitingForReady = false
            showPopup("$activeAbilityName Ready!")
        }
    }

    private fun readTabListCooldownSeconds(): Long? {
        val mc = Minecraft.getInstance()
        val lines = mc.connection?.getOnlinePlayers()
            ?.mapNotNull { it.getTabListDisplayName()?.string }
            ?: emptyList()

        val abilityLine = lines.firstOrNull { line ->
            abilityNames.any { abilityName -> line.contains(abilityName) }
        }

        if (abilityLine == null) {
            if (!hasWarnedTabMissingThisIsland) {
                hasWarnedTabMissingThisIsland = true
                mc.player?.sendSystemMessage(
                    Component.literal(
                        "§c[§6CasualSkyblockZAddons§c]\n" +
                                "§fAbilityAnnouncer works more accurately with the §ePickaxe Ability Widget §fvisible in your tab list."
                    )
                )
            }
            return null
        }

        if (abilityLine.contains("Available")) return null

        val match = Regex("(\\d+)s").find(abilityLine) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    private fun showPopup(message: String) {
        val now = System.currentTimeMillis()
        if (message == lastPopupMessage && now - lastPopupTime < POPUP_COOLDOWN_MS) return
        lastPopupMessage = message
        lastPopupTime = now
        popupMessage = message
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
        val popup = getActivePopup() ?: return
        val mc = Minecraft.getInstance()
        val cfg = ModConfig.get()
        val screenWidth = mc.window.guiScaledWidth
        val screenHeight = mc.window.guiScaledHeight
        val scale = cfg.abilityAnnouncerScale
        val textWidth = mc.font.width(popup)
        val x = if (cfg.abilityAnnouncerX == -1) ((screenWidth - textWidth * scale) / 2).toInt() else cfg.abilityAnnouncerX
        val y = if (cfg.abilityAnnouncerY == -1) screenHeight / 3 else cfg.abilityAnnouncerY
        val color = ColorCatalog.GREEN
        graphics.pose().pushMatrix()
        graphics.pose().scale(scale, scale)
        graphics.text(mc.font, popup, (x / scale).toInt(), (y / scale).toInt(), color, true)
        graphics.pose().popMatrix()
    }

    fun reset() {
        waitingForReady = false
        activeAbilityName = ""
        popupMessage = null
        gotTimeFromTab = false
        readyTime = Long.MAX_VALUE
        lastServerJoinTime = System.currentTimeMillis()
        lastPopupTime = 0L
        lastPopupMessage = null
        pendingTabSeconds = null
        hasWarnedTabMissingThisIsland = false
    }

    fun onServerJoin() {
        reset()
    }
}