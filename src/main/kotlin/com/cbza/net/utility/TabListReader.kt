package com.cbza.net.utility

import net.minecraft.client.Minecraft

/**
 * Reads text from the tab list (`PlayerInfo` entries) to extract Hypixel Skyblock stats,
 * server IDs, crystal states, and commissions.
 */
object TabListReader {

    private val MINING_SPEED_REGEX = Regex("""Mining Speed:\s*[^\d]*([\d,]+)""")
    private val SERVER_REGEX = Regex("""Server:\s*(\S+)""")
    private val COMMISSION_REGEX = Regex("""^.+:\s*(\d+(\.\d+)?%|DONE)$""")

    /**
     * Gets every line of text currently shown in the tab list grid.
     */
    fun getLines(): List<String> {
        val mc = Minecraft.getInstance()
        return mc.connection?.onlinePlayers
            ?.mapNotNull { it.tabListDisplayName?.string }
            ?: emptyList()
    }

    /**
     * Finds the first line containing the given substring.
     */
    fun findLine(contains: String): String? {
        return getLines().firstOrNull { it.contains(contains, ignoreCase = true) }
    }

    /**
     * Finds a line matching the given header/label, and returns the line immediately following it.
     */
    fun findLineAfter(header: String): String? {
        val lines = getLines()
        val index = lines.indexOfFirst { it.contains(header, ignoreCase = true) }
        if (index == -1 || index + 1 >= lines.size) return null
        return lines[index + 1]
    }

    /**
     * Reads the player's current Mining Speed stat from the tab list.
     * Correctly handles comma separators (e.g. "1,850" -> 1850).
     */
    fun getMiningSpeed(): Int? {
        val line = findLine("Mining Speed:") ?: return null
        val match = MINING_SPEED_REGEX.find(line) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }

    /**
     * Reads the current server instance ID (e.g., "mini123A").
     */
    fun getServer(): String? {
        val line = findLine("Server:") ?: return null
        val match = SERVER_REGEX.find(line) ?: return null
        return match.groupValues[1]
    }

    /**
     * Reads the status (e.g. "Found", "NOT FOUND") of a named crystal from the Crystal Hollows tab list.
     */
    fun getCrystalStatus(crystalName: String): String? {
        val line = findLine(crystalName) ?: return null
        val match = Regex("$crystalName:\\s*(.*)", RegexOption.IGNORE_CASE).find(line) ?: return null
        return match.groupValues[1].trim()
    }

    /**
     * Reads active Commission progress lines from the tab list.
     */
    fun getCommissionLines(): List<String> {
        return getLines()
            .map { it.trim() }
            .filter { COMMISSION_REGEX.matches(it) }
    }
}