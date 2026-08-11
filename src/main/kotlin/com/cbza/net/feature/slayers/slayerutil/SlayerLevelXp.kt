package com.cbza.net.feature.slayers.slayerutil

object SlayerLevelXp {

    private val Default = listOf(5L, 15L, 200L, 1000L, 5000L, 20000L, 100000L, 400000L, 1000000L)
    private val Vampire = listOf(20L, 75L, 240L, 840L, 2400L)

    enum class Level(val xp: List<Long>) {
        ZOMBIE(Default),
        SPIDER(Default),
        WOLF(Default),
        ENDERMAN(Default),
        BLAZE(Default),
        VAMPIRE(Vampire)
    }
}