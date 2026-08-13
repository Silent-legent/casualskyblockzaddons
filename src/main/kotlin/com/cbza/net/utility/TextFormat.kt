package com.cbza.net.utility

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object TextFormat {

    // Force '.' as grouping separator regardless of system locale
    private val dotFormatter = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
    })

    /**
     * Formats numbers into compact suffix notation.
     * Example: 1_500_000 -> "1.5m", 2_400 -> "2.4k"
     */
    fun formatCoins(amount: Long): String {
        return when {
            amount >= 1_000_000 -> "%.1fm".format(amount / 1_000_000.0)
            amount >= 1_000 -> "%.1fk".format(amount / 1_000.0)
            else -> amount.toString()
        }
    }

    fun formatCoins(amount: Int): String = formatCoins(amount.toLong())

    /**
     * Formats numbers with dot separators.
     * Example: 1000 -> "1.000", 1000000 -> "1.000.000"
     */
    fun formatWithDots(amount: Long): String {
        return dotFormatter.format(amount)
    }

    fun formatWithDots(amount: Int): String = formatWithDots(amount.toLong())
}