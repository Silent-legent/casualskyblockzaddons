package com.cbza.net.utility

import kotlin.math.roundToInt

/**
 * Calculates block breaking duration and speed thresholds based on Hypixel Skyblock's
 * mining formulas.
 */
object BlockStrength {

    /**
     * Hardness values for Hypixel Skyblock ores, gemstones, and custom blocks.
     */
    val strengths = mapOf(
        "RUBY" to 2300,
        "AMBER" to 3000,
        "SAPPHIRE" to 3000,
        "JADE" to 3000,
        "AMETHYST" to 3000,
        "OPAL" to 3000,
        "TOPAZ" to 3800,
        "JASPER" to 4800,
        "ONYX" to 5200,
        "AQUAMARINE" to 5200,
        "CITRINE" to 5200,
        "PERIDOT" to 5200,
        "GLACITE" to 6000,
        "TUNGSTEN" to 5600,
        "UMBER" to 5600,
        "TITANIUM" to 2000,
        "OBSIDIAN" to 445,
        "LOW_TIER_MITHRIL" to 500,
        "MID_TIER_MITHRIL" to 800,
        "HIGH_TIER_MITHRIL" to 1500
    )

    /**
     * Calculates breaking duration in ticks for a given block key and mining speed.
     * Enforces the 4-tick (0.2s) soft floor.
     */
    fun calculateTicks(blockKey: String, miningSpeed: Int): Int? {
        val strength = strengths[blockKey.uppercase()] ?: return null
        return calculateTicks(strength, miningSpeed)
    }

    /**
     * Calculates breaking duration in ticks for a raw strength value.
     */
    fun calculateTicks(strength: Int, miningSpeed: Int): Int? {
        if (miningSpeed <= 0 || strength <= 0) return null

        val rawTicks = (30.0 * strength / miningSpeed).roundToInt()
        return maxOf(4, rawTicks) // Enforces the 4-tick minimum breaking speed
    }

    /**
     * Converts server ticks into milliseconds (default 20 TPS = 50ms per tick).
     */
    fun ticksToMs(ticks: Int, tps: Double = 20.0): Long {
        if (tps <= 0.0) return (ticks * 50L)
        return (ticks * (1000.0 / tps)).toLong()
    }

    /**
     * Calculates the exact Mining Speed required to reach the next lower tick threshold
     * (e.g. going from 6 ticks down to 5 ticks).
     */
    fun speedForNextTick(strength: Int, currentTicks: Int): Int? {
        if (currentTicks <= 4) return null // Already at the 4-tick floor
        val denominator = 2 * currentTicks - 1
        return (60 * strength) / denominator + 1
    }

    fun speedForNextTick(blockKey: String, currentTicks: Int): Int? {
        val strength = strengths[blockKey.uppercase()] ?: return null
        return speedForNextTick(strength, currentTicks)
    }
}