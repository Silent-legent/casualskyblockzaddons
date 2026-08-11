package com.cbza.net.utility

object TextFormat {

    fun formatCoins(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "%.1fm".format(amount / 1_000_000.0)
            amount >= 1_000 -> "%.1fk".format(amount / 1_000.0)
            else -> amount.toString()
        }
    }
}