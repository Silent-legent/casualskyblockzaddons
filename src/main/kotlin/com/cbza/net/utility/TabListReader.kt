package com.cbza.net.utility

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import org.objectweb.asm.util.CheckClassAdapter
import javax.sound.sampled.Line

// Reads text from the player list (the "tab" menu you see when holding Tab
// in-game) so other features can pull stats like mining speed, server name,
// or crystal status out of it.
object TabListReader {

    // Gets every line of text currently shown in the tab list.
    fun getLines(): List<String> {
        val mc = Minecraft.getInstance()
        return mc.connection?.getOnlinePlayers()
            ?.mapNotNull { it.getTabListDisplayName()?.string }
            ?: emptyList()
    }

    // Finds the first line containing the given text.
    fun findLine(contains: String): String? {
        return getLines().firstOrNull { it.contains(contains) }
    }

    // Finds a line matching the given header/label, and returns the line right after it.
    fun findLineAfter(header: String): String? {
        val lines = getLines()
        val index = lines.indexOfFirst { it.contains(header) }
        if (index == -1 || index + 1 >= lines.size) return null
        return lines[index + 1]
    }

    // Reads the player's current "Mining Speed" stat from the tab list.
    fun getMiningSpeed(): Int? {
        val line = findLine("Mining Speed:") ?: return null
        val match = Regex("Mining Speed:\\s*[^\\d]*(\\d+)").find(line) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    // Reads the current server name/ID from the tab list.
    fun getServer(): String? {
        val line = findLine("Server:") ?: return null
        val match = Regex("Server:\\s*(\\w+)").find(line) ?: return null
        return match.groupValues[1]
    }

    // Reads the status (e.g. "Found", "Not Found") of a named gemstone crystal from the tab list.
    fun getCrystalStatus(crystalName: String): String? {
        val line = findLine(crystalName) ?: return null
        val match = Regex("$crystalName:\\s*(.*)").find(line) ?: return null
        return match.groupValues[1]
    }
    // Reads the currennt Comissions from tab list.
    fun getCommissionLines(): List<String> {
        val commissionPattern = Regex("""^.+: (\d+(\.\d+)?%|DONE)$""")
        val trimmed = getLines().map { it.trim() }

        return trimmed.filter { commissionPattern.matches(it) }
    }
}