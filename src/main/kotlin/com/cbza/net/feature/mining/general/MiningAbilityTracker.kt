package com.cbza.net.feature.mining.general

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatMessageEvent
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.event.events.TickEvent

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.ARGB

import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

import kotlin.math.abs

// Watches chat and the player list to detect when a mining ability was used,
// figures out when it will be ready again, and shows a popup + sound when it is.
object MiningAbilityTracker {
    init {
        EventBus.subscribe(ChatMessageEvent::class.java) { event ->
            val text = event.text
            if (text.contains("You used your") && text.contains("Pickaxe Ability!")) {
                onAbilityUsed(text)
            }
            if (text.contains("is now available!")) {
                onAbilityReady(text)
            }
        }
        EventBus.subscribe(TickEvent::class.java) {
            tick()
        }
        EventBus.subscribe(ServerJoinEvent::class.java) {
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
    private const val STARTUP_GRACE_MS = 3000L // ignore chat-history replay right after connecting

    @Volatile var popupMessage: String? = null
    @Volatile var popupExpireTime: Long = 0L
    private const val POPUP_DURATION_MS = 3000L

    @Volatile private var activeAbilityName: String = ""
    @Volatile private var waitingForReady: Boolean = false
    @Volatile private var readyTime: Long = 0L
    @Volatile private var gotTimeFromTab: Boolean = false

    @Volatile private var lastPopupTime: Long = 0L
    @Volatile private var lastPopupMessage: String? = null
    private const val POPUP_COOLDOWN_MS = 500L // dedupe only the same message firing twice in a row

    @Volatile private var abilityUsedTime: Long = 0L
    private const val TAB_READ_DELAY_MS = 500L // wait before first attempting to read tab

    private const val MIN_PLAUSIBLE_SECONDS = 10L // shorter than this = stale leftover, not a real reading

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

    // Called when chat suggests an ability was just activated. Starts tracking it.
    fun onAbilityUsed(chatMessage: String) {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        val match = abilityNames.firstOrNull { chatMessage.contains(it) } ?: return
        activeAbilityName = match
        waitingForReady = true
        gotTimeFromTab = false
        readyTime = Long.MAX_VALUE
        pendingTabSeconds = null
        abilityUsedTime = System.currentTimeMillis()
    }

    // Called when chat suggests the tracked ability is ready again (fallback if the tab-list check below doesn't catch it first).
    fun onAbilityReady(chatMessage: String) {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        if (!waitingForReady) return
        if (activeAbilityName.isEmpty()) return
        if (!chatMessage.contains(activeAbilityName)) return
        if (gotTimeFromTab) return // tab already handling this cast, message is just a fallback
        // Ignore a message that arrives faster than any real cooldown allows
        // it's a stale leftover from a previous cast, not this one.
        if (System.currentTimeMillis() - abilityUsedTime < MIN_PLAUSIBLE_SECONDS * 1000L) return
        waitingForReady = false
        showPopup("$activeAbilityName Ready!")
    }

    // Runs every game tick. Handles leaving/entering mining islands, and reads the
    // player list to pin down exactly when the tracked ability becomes ready.
    fun tick() {
        val onMiningIsland = miningIslands.any { it.inIsland() }
        if (wasOnMiningIsland && !onMiningIsland) reset()
        wasOnMiningIsland = onMiningIsland
        if (!onMiningIsland) return
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (!waitingForReady) return

        // Try to read the cooldown from the tab list, requiring two consistent
        // readings in a row before trusting it (avoids acting on a glitchy value).
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

    // Looks for the tracked ability's line in the player list and reads its
    // remaining cooldown in seconds. Returns null if it can't find/read one.
    private fun readTabListCooldownSeconds(): Long? {
        val mc = Minecraft.getInstance()
        val lines = mc.connection?.getOnlinePlayers()
            ?.mapNotNull { it.getTabListDisplayName()?.string }
            ?: emptyList()

        val abilityLine = lines.firstOrNull { line ->
            abilityNames.any { abilityName -> line.contains(abilityName) }
        }

        if (abilityLine == null) {
            // No ability widget visible in tab list. warn the player once per island.
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

    // Shows the on-screen popup text and plays a notification sound.
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

    // Returns the popup that should be showing right now, or null if there isn't one / it expired.
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
        val color = ARGB.opaque(0x55FF55)
        graphics.pose().pushMatrix()
        graphics.pose().scale(scale, scale)
        graphics.text(mc.font, popup, (x / scale).toInt(), (y / scale).toInt(), color, true)
        graphics.pose().popMatrix()
    }

    // Clears all tracking state back to a clean slate.
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