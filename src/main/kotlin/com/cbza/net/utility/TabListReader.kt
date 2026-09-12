package com.cbza.net.utility

import net.minecraft.client.Minecraft

object TabListReader {

    private val MINING_SPEED_REGEX = Regex("""Mining Speed:\s*[^\d]*([\d,]+)""")
    private val SERVER_REGEX = Regex("""Server:\s*(\S+)""")
    private val COMMISSION_REGEX = Regex("""^.+:\s*(\d+(\.\d+)?%|DONE)$""")

    fun getLines(): List<String> {
        val mc = Minecraft.getInstance()
        return mc.connection?.onlinePlayers
            ?.mapNotNull { it.tabListDisplayName?.string }
            ?: emptyList()
    }

    fun findLine(contains: String): String? {
        return getLines().firstOrNull { it.contains(contains, ignoreCase = true) }
    }

    fun findLineAfter(header: String): String? {
        val lines = getLines()
        val index = lines.indexOfFirst { it.contains(header, ignoreCase = true) }
        if (index == -1 || index + 1 >= lines.size) return null
        return lines[index + 1]
    }

    fun getMiningSpeed(): Int? {
        val line = findLine("Mining Speed:") ?: return null
        val match = MINING_SPEED_REGEX.find(line) ?: return null
        return match.groupValues[1].replace(",", "").toIntOrNull()
    }

    fun getServer(): String? {
        val line = findLine("Server:") ?: return null
        val match = SERVER_REGEX.find(line) ?: return null
        return match.groupValues[1]
    }

    fun getCrystalStatus(crystalName: String): String? {
        val line = findLine(crystalName) ?: return null
        val match = Regex("$crystalName:\\s*(.*)", RegexOption.IGNORE_CASE).find(line) ?: return null
        return match.groupValues[1].trim()
    }

    fun getCommissionLines(): List<String> {
        return getLines()
            .map { it.trim() }
            .filter { COMMISSION_REGEX.matches(it) }
    }
}