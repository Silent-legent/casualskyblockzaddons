package com.cbza.net.feature.slayers

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ChatEvent
import com.cbza.net.feature.slayers.slayerutil.SlayerType
import net.minecraft.client.Minecraft

object SlayerXpTracker {

    // in-memory only for now — resets on game restart. Saving to disk is a separate task for later.
    private val currentXpBySlayer = mutableMapOf<SlayerType, Long>()

    init {
        EventBus.subscribe(ChatEvent::class.java) { event ->
            onChatMessage(event.text)
        }
    }

    private val questCompleteRegex = Regex("(\\w+) Slayer LVL (\\d+) - Next LVL in ([\\d,]+) XP!")

    private fun onChatMessage(text: String) {
        val match = questCompleteRegex.find(text) ?: return

        val slayerWord = match.groupValues[1]     // e.g. "Zombie"
        val currentLevel = match.groupValues[2].toIntOrNull() ?: return
        val remainingXp = match.groupValues[3].replace(",", "").toLongOrNull() ?: return

        val slayer = SlayerType.entries.find { it.apiId == slayerWord.lowercase() } ?: return

        val xpForNextLevel = SlayerCalc.xpForLevel(slayer, currentLevel + 1) ?: return
        val currentXp = xpForNextLevel - remainingXp

        if (currentXp != null) {
            currentXpBySlayer[slayer] = currentXp
            currentXpBySlayer[slayer] = currentXp


        }
    }

    fun getCurrentXp(slayer: SlayerType): Long? = currentXpBySlayer[slayer]
}