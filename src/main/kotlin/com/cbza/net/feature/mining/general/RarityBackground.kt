package com.cbza.net.feature.general

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import java.util.Collections
import java.util.WeakHashMap

object RarityBackground {

    private val rarityColorCache: MutableMap<ItemStack, Int> =
        Collections.synchronizedMap(WeakHashMap())

    fun getRarityColor(itemStack: ItemStack?): Int {
        if (!ModConfig.get().showRarityBackgrounds) return -1
        if (itemStack == null || itemStack.isEmpty) return -1

        // Kotlin's computeIfAbsent replaces the manual null check + put
        return rarityColorCache.computeIfAbsent(itemStack) { computeRarityColor(it) }
    }

    private fun computeRarityColor(itemStack: ItemStack): Int {
        val lore = itemStack.get(DataComponents.LORE) ?: return -1
        val lines = lore.lines()
        if (lines.isEmpty()) return -1

        val linesToCheck = minOf(8, lines.size)
        for (i in 0 until linesToCheck) {
            val rawLine = stripLeadingIcon(lines[lines.size - 1 - i].string.trim())

            // Idiomatic Kotlin 'when' block replaces 11 chained 'if' statements
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
}