package com.cbza.net.feature.general

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

object RarityBackground {

    private val rarityColorCache: MutableMap<List<String>, Int> = HashMap()

    // Debounce state, keyed by slot position (x,y packed into one Int).
    // Remembers the last real color shown at a slot, how many consecutive
    // frames in a row that slot has come back "loreless", and whether the
    // last real item seen there was dyed - only dyed items get the smoothing,
    // since only they actually cause the animation-glitch placeholder frames.
    private val lastColorBySlot: MutableMap<Int, Int> = HashMap()
    private val missStreakBySlot: MutableMap<Int, Int> = HashMap()
    private val lastWasDyedBySlot: MutableMap<Int, Boolean> = HashMap()

    // How many consecutive loreless frames we tolerate before trusting
    // it (i.e. treating the slot as genuinely empty/changed) rather than
    // a transient glitch frame. Bump this up if the flicker survives more
    // than a couple frames on your machine; keep it low so unequipping
    // gear still clears the background quickly.
    private const val MISS_TOLERANCE = 35

    private fun slotKey(x: Int, y: Int): Int = x * 100000 + y

    fun getRarityColor(itemStack: ItemStack?, x: Int, y: Int): Int {
        if (!ModConfig.get().showRarityBackgrounds) return -1

        val key = slotKey(x, y)

        // A genuinely empty slot (nothing there at all - e.g. crafting grid,
        // an unequipped armor slot) is trusted immediately, no debounce.
        // Debounce only exists to smooth over the animation glitch, where a
        // *real, non-empty* placeholder stack briefly shows up in place of
        // the actual item - it should never bleed into slots that are
        // legitimately empty.
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
            // Never a dyed item at this slot - no reason to expect the
            // animation glitch, so trust the miss immediately.
            lastColorBySlot.remove(key)
            missStreakBySlot.remove(key)
            return -1
        }

        val streak = (missStreakBySlot[key] ?: 0) + 1
        missStreakBySlot[key] = streak

        val remembered = lastColorBySlot[key]
        return if (remembered != null && streak <= MISS_TOLERANCE) {
            // Dyed item, probably a transient glitch frame (mid-animation
            // placeholder stack) - keep showing the last known-good color.
            remembered
        } else {
            // Either no prior color, or we've missed too many frames in a row -
            // trust it, the slot is actually empty/changed now.
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

    // Hypixel-style items that get an animated dye applied show a line like
    // "Oasis Dyed" (color name + "Dyed") near the top of their lore. Checking
    // the already-fetched lore for this is free - no extra component lookup -
    // and matches how these items actually mark themselves, rather than
    // relying on vanilla's DYED_COLOR component, which these server-driven
    // items may not carry at all.
    private fun isDyedLore(lines: List<String>): Boolean =
        lines.any { it.contains("Dyed") }
}