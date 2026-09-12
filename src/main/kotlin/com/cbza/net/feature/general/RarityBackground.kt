package com.cbza.net.feature.general

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

object RarityBackground {

    private val rarityColorCache: MutableMap<List<String>, Int> = HashMap()

    private val lastColorBySlot: MutableMap<Int, Int> = HashMap()
    private val missStreakBySlot: MutableMap<Int, Int> = HashMap()
    private val lastWasDyedBySlot: MutableMap<Int, Boolean> = HashMap()

    private const val MISS_TOLERANCE = 35

    private fun slotKey(x: Int, y: Int): Int = x * 100000 + y

    fun getRarityColor(itemStack: ItemStack?, x: Int, y: Int): Int {
        if (!ModConfig.get().showRarityBackgrounds) return -1

        val key = slotKey(x, y)

        if (itemStack == null || itemStack.isEmpty) {
            lastColorBySlot.remove(key)
            missStreakBySlot.remove(key)
            lastWasDyedBySlot.remove(key)
            return -1
        }

        val lore = itemStack.get(DataComponents.LORE)
        if (lore == null) {
            return handleMiss(key)
        }

        val lines = lore.lines().map { it.string }
        if (lines.isEmpty()) {
            return handleMiss(key)
        }

        // Real, lore-bearing stack: resolve color normally, reset debounce state.
        val color = rarityColorCache.getOrPut(lines) { computeRarityColor(lines) }
        missStreakBySlot[key] = 0
        lastWasDyedBySlot[key] = isDyedLore(lines)
        if (color != -1) {
            lastColorBySlot[key] = color
        } else {
            lastColorBySlot.remove(key)
        }
        return color
    }

    private fun handleMiss(key: Int): Int {
        val wasDyed = lastWasDyedBySlot[key] == true

        if (!wasDyed) {
            lastColorBySlot.remove(key)
            missStreakBySlot.remove(key)
            return -1
        }

        val streak = (missStreakBySlot[key] ?: 0) + 1
        missStreakBySlot[key] = streak

        val remembered = lastColorBySlot[key]
        return if (remembered != null && streak <= MISS_TOLERANCE) {
            remembered
        } else {
            lastColorBySlot.remove(key)
            lastWasDyedBySlot.remove(key)
            -1
        }
    }

    private fun computeRarityColor(lines: List<String>): Int {
        val linesToCheck = minOf(8, lines.size)
        for (i in 0 until linesToCheck) {
            val rawLine = stripLeadingIcon(lines[lines.size - 1 - i].trim())

            val color = when {
                rawLine.startsWith("ADMIN") -> ColorCatalog.TRANSLUCENT_DARK_RED
                rawLine.startsWith("ULTIMATE") -> ColorCatalog.TRANSLUCENT_DARK_RED
                rawLine.startsWith("VERY SPECIAL") -> ColorCatalog.TRANSLUCENT_LIGHT_RED
                rawLine.startsWith("SPECIAL") -> ColorCatalog.TRANSLUCENT_LIGHT_RED
                rawLine.startsWith("DIVINE") -> ColorCatalog.TRANSLUCENT_CYAN
                rawLine.startsWith("MYTHIC") -> ColorCatalog.TRANSLUCENT_LIGHT_MAGENTA
                rawLine.startsWith("LEGENDARY") -> ColorCatalog.TRANSLUCENT_GOLD
                rawLine.startsWith("EPIC") -> ColorCatalog.TRANSLUCENT_DARK_PURPLE
                rawLine.startsWith("RARE") -> ColorCatalog.TRANSLUCENT_LIGHT_BLUE
                rawLine.startsWith("UNCOMMON") -> ColorCatalog.TRANSLUCENT_LIGHT_GREEN
                rawLine.startsWith("COMMON") -> ColorCatalog.TRANSLUCENT_WHITE
                else -> null
            }

            if (color != null) return color
        }

        return -1
    }

    private fun stripLeadingIcon(line: String): String {
        val index = line.indexOfFirst { it in 'A'..'Z' }
        return if (index != -1) line.substring(index) else line
    }

    private fun isDyedLore(lines: List<String>): Boolean =
        lines.any { it.contains("Dyed") }
}