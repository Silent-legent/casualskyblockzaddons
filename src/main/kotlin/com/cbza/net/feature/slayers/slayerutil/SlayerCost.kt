package com.cbza.net.feature.slayers.slayerutil

object SlayerCost {

    private val Default = listOf(1980L, 7200L, 19200L, 48000L, 96000L)
    private val Inferno = listOf(9600L, 24000L, 57600L, 144000L)
    private val Vampire = listOf(3840L, 7680L, 9600L, 13440L, 19200L)

    enum class Cost(val prices: List<Long>) {
        ZOMBIE(Default),
        SPIDER(Default),
        WOLF(Default),
        ENDERMAN(Default),
        BLAZE(Inferno),
        VAMPIRE(Vampire)
    }
}