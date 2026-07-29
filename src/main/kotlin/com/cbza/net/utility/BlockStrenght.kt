package com.cbza.net.utility

// Holds how "tough" each type of block/gemstone is to mine, and does the math
// to convert that toughness + the player's mining speed into an actual time
// (in ticks/milliseconds) it should take to break.
object BlockStrength {

    // How tough each block type is. higher number means it takes longer to mine.
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
     * Calculates mining time in ticks for a given block and mining speed.
     * Formula: ticks = round(30 * block strength / mining speed)
     * Clamped to minimum 4 ticks if result is between 1 and 3.
     */
    fun calculateTicks(blockKey: String, miningSpeed: Int): Int? {
        val strength = strengths[blockKey] ?: return null
        if (miningSpeed <= 0) return null

        var ticks = Math.round(30.0 * strength / miningSpeed).toInt()
        if (ticks in 1..3) ticks = 4
        return ticks
    }

    /**
     * Converts ticks to milliseconds (20 ticks per second).
     */
    fun ticksToMs(ticks: Int, tps: Double = 20.0): Long {
        return (ticks * (1000.0 / tps)).toLong()
    }

    // Works out how much mining speed the player would need to shave one more
    // tick off the current mining time (used to suggest gear/speed upgrades).
    fun speedForNextTick(strength: Int, currentTicks: Int): Int? {
        if (currentTicks <= 4) return null // 4 is the effective floor (clamp)
        val denominator = 2 * currentTicks - 1
        return (60 * strength) / denominator + 1
    }
}