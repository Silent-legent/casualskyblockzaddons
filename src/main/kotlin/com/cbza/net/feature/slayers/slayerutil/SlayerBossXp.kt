package com.cbza.net.feature.slayers.slayerutil

object  SlayerBossXp {

    private val Default = listOf(5L, 25L, 100L, 500L)
    private val ZombieSpider = listOf(5L, 25L, 100L, 500L, 1500L)
    private val Vampire = listOf(10L, 25L, 60L, 120L, 150L)

    enum class Level(val xp: List<Long>) {
        WOLF(Default),
        ENDERMAN(Default),
        BLAZE(Default),
        ZOMBIE(ZombieSpider),
        SPIDER(ZombieSpider),
        VAMPIRE(Vampire)
    }
}