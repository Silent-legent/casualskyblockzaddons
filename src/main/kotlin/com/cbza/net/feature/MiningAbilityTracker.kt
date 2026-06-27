package com.cbza.net.feature

import com.cbza.net.config.ModConfig
import net.minecraft.client.Minecraft

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
    private const val STARTUP_GRACE_MS = 5000L

    @Volatile var popupMessage: String? = null
    @Volatile var popupExpireTime: Long = 0L
    private const val POPUP_DURATION_MS = 3000L

    @Volatile private var activeAbilityName: String = ""
    @Volatile private var waitingForReady: Boolean = false
    @Volatile private var readyTime: Long = 0L
    @Volatile private var gotTimeFromTab: Boolean = false
    @Volatile private var tabReadDeadline: Long = 0L

    fun onAbilityUsed(chatMessage: String) {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        val match = abilityNames.firstOrNull { chatMessage.contains(it) } ?: return
        activeAbilityName = match
        waitingForReady = true
        gotTimeFromTab = false
        tabReadDeadline = System.currentTimeMillis() + 3000L
        readyTime = Long.MAX_VALUE
    }

    fun onAbilityReady(chatMessage: String) {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (System.currentTimeMillis() - lastServerJoinTime < STARTUP_GRACE_MS) return
        if (!waitingForReady) return
        if (activeAbilityName.isEmpty()) return
        if (!chatMessage.contains(activeAbilityName)) return
        waitingForReady = false
        showPopup("$activeAbilityName Ready!")
    }

    fun tick() {
        if (!ModConfig.get().MiningAbilityAnnouncer) return
        if (!waitingForReady) return

        if (!gotTimeFromTab && System.currentTimeMillis() < tabReadDeadline) {
            val seconds = readTabListCooldownSeconds()
            if (seconds != null) {
                readyTime = System.currentTimeMillis() + (seconds * 1000L)
                gotTimeFromTab = true
            }
        }

        if (System.currentTimeMillis() >= readyTime) {
            waitingForReady = false
            showPopup("$activeAbilityName Ready!")
        }
    }

    private fun readTabListCooldownSeconds(): Long? {
        val mc = Minecraft.getInstance()
        val playerList = mc.connection?.getOnlinePlayers() ?: return null

        val lines = playerList.mapNotNull { it.getTabListDisplayName()?.string }

        val abilityLine = lines.firstOrNull { line ->
            abilityNames.any { abilityName -> line.contains(abilityName) }
        } ?: return null

        if (abilityLine.contains("Available")) return null

        val match = Regex("(\\d+)s").find(abilityLine) ?: return null
        return match.groupValues[1].toLongOrNull()
    }

    private fun showPopup(message: String) {
        popupMessage = message
        popupExpireTime = System.currentTimeMillis() + POPUP_DURATION_MS

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
        tabReadDeadline = 0L
        lastServerJoinTime = System.currentTimeMillis()
    }
}