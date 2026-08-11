package com.cbza.net.feature.slayers

import com.cbza.net.feature.slayers.slayerutil.SlayerLevelXp
import com.cbza.net.feature.slayers.slayerutil.SlayerType

object SlayerCalc {

    fun xpForLevel(slayer: SlayerType, level: Int): Long? {
        val table = SlayerLevelXp.Level.valueOf(slayer.name).xp
        return table.getOrNull(level - 1)
    }

    fun calculateBosses(currentXp: Long, targetXp: Long, xpPerBoss: Long): Long? {
        if (xpPerBoss <= 0) return null

        val remaining = targetXp - currentXp
        if (remaining <= 0) return 0L

        return (remaining + xpPerBoss - 1) / xpPerBoss

    }

    fun calcPrice(bosses: Long, pricePerBoss: Long ): Long? {
        if (pricePerBoss <= 0) return null

        val price = bosses * pricePerBoss

        return price
    }
}