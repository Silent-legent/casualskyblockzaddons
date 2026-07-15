    package com.cbza.net.utility

    import net.minecraft.client.Minecraft
    import net.minecraft.network.chat.Component
    import javax.sound.sampled.Line

    object TabListReader {

        fun getLines(): List<String> {
            val mc = Minecraft.getInstance()
            return mc.connection?.getOnlinePlayers()
                ?.mapNotNull { it.getTabListDisplayName()?.string }
                ?: emptyList()
        }

        fun findLine(contains: String): String? {
            return getLines().firstOrNull { it.contains(contains) }
        }

        fun findLineAfter(header: String): String? {
            val lines = getLines()
            val index = lines.indexOfFirst { it.contains(header) }
            if (index == -1 || index + 1 >= lines.size) return null
            return lines[index + 1]
        }
            fun getMiningSpeed(): Int? {
                val line = findLine("Mining Speed:") ?: return null
                val match = Regex("Mining Speed:\\s*[^\\d]*(\\d+)").find(line) ?: return null
                return match.groupValues[1].toIntOrNull()
        }
        fun getServer(): String? {
            val line = findLine("Server:") ?: return null
            val match = Regex("Server:\\s*(\\w+)").find(line) ?: return null
            return match.groupValues[1]
        }
        fun getCrystalStatus(crystalName: String): String? {
            val line = findLine(crystalName) ?: return null
            val match = Regex("$crystalName:\\s*(.*)").find(line) ?: return null
            return match.groupValues[1]
        }
        fun getCommissionName(name: String): String? {
            val line = findLine(name) ?: return null
           return name
        }
        fun getCommissionProgress(commissionName: String): String? {
            val line = findLine(commissionName) ?: return null
            val match = Regex("$commissionName:\\s*(.*)").find(line) ?: return null
            return match.groupValues[1]
        }
    }