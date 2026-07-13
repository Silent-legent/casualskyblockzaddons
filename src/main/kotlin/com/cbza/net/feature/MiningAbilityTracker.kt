package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import net.minecraft.client.Minecraft
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object MiningAbilityTracker {

    private val abilityNames = listOf(
        "Pickobulus",
        "Mining Speed Boost",
        "Maniac Miner",
        "Tunnel Vision",
        "Sheer Force",
        "Gemstone Infusion"
    )

    @Volatile private var lastServerJoinTime = System.currentTimeMillis()
    private const val STARTUP_GRACE_MS = 15000L // ignore chat-history replay right after connecting

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

    fun onAbilityReady(chatMessage: String) {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        if (!waitingForReady) return
        if (activeAbilityName.isEmpty()) return
        if (!chatMessage.contains(activeAbilityName)) return
        if (gotTimeFromTab) return // tab already handling this cast, message is just a fallback
        // Ignore a message that arrives faster than any real cooldown allows -
        // it's a stale leftover from a previous cast, not this one.
        if (System.currentTimeMillis() - abilityUsedTime < MIN_PLAUSIBLE_SECONDS * 1000L) return
        waitingForReady = false
        showPopup("$activeAbilityName Ready!")
    }

    fun tick() {
        val onMiningIsland = miningIslands.any { it.inIsland() }
        if (wasOnMiningIsland && !onMiningIsland) reset()
        wasOnMiningIsland = onMiningIsland
        if (!onMiningIsland) return
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (!waitingForReady) return

        if (!gotTimeFromTab && System.currentTimeMillis() - abilityUsedTime > TAB_READ_DELAY_MS) {
            val seconds = readTabListCooldownSeconds()
            val now = System.currentTimeMillis()

            if (seconds == null || seconds <= MIN_PLAUSIBLE_SECONDS) {
                // Bad/missing reading - skip it, keep any existing candidate intact.
            } else {
                val pending = pendingTabSeconds
                if (pending == null) {
                    // First sighting - don't trust it yet, confirm next tick.
                    pendingTabSeconds = seconds
                    pendingTabReadTime = now
                } else {
                    // A real countdown drops by roughly the time that passed.
                    // If it doesn't line up, the tab was mid-update - restart confirmation.
                    val elapsedSec = (now - pendingTabReadTime) / 1000.0
                    val isConsistent = kotlin.math.abs((pending - elapsedSec) - seconds) <= 1.0

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
                    net.minecraft.network.chat.Component.literal(
                        "§c[§6CasualSkyblockAddons§c] §fAbilityAnnouncer works more accurately with the §ePickaxe Ability Widget §fPickaxe Ability Widget visible in your tab list."
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
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
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

    // Call from your actual "connected to server" event, not just mod load -
    // otherwise chat-history replay mods can trigger fake popups on join.
    fun onServerJoin() {
        reset()
    }
}