package com.cbza.net.utility

object ColorCatalog {

    // Solid
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val RED = 0xFFFF0000.toInt()
    const val GREEN = 0xFF00FF00.toInt()
    const val BLUE = 0xFF0000FF.toInt()
    const val ORANGE = 0xFFFFA500.toInt()
    const val YELLOW = 0xFFFFFF00.toInt()
    const val PURPLE = 0xFFAA00FF.toInt()
    const val CYAN = 0xFF00FFFF.toInt()
    const val MAGENTA = 0xFFFF00FF.toInt()
    const val GRAY = 0xFF808080.toInt()
    const val BROWN = 0xFFA52A2A.toInt()
    const val PINK = 0xFFFFC0CB.toInt()
    const val NAVY_BLUE = 0xFF000080.toInt()
    const val DARK_GREEN = 0xFF008000.toInt()
    const val DARK_RED = 0xFF800000.toInt()

    // 38% Translucent (0x60 Alpha)
    const val TRANSLUCENT_DARK_RED = 0x60AA0000.toInt()
    const val TRANSLUCENT_LIGHT_RED = 0x60FF5555.toInt()
    const val TRANSLUCENT_CYAN = 0x6055FFFF.toInt()
    const val TRANSLUCENT_LIGHT_MAGENTA = 0x60FF55FF.toInt()
    const val TRANSLUCENT_GOLD = 0x60FFAA00.toInt()
    const val TRANSLUCENT_DARK_PURPLE = 0x60AA00AA.toInt()
    const val TRANSLUCENT_LIGHT_BLUE = 0x605555FF.toInt()
    const val TRANSLUCENT_LIGHT_GREEN = 0x6055FF55.toInt()
    const val TRANSLUCENT_WHITE = 0x60FFFFFF.toInt()

    /**
     * Interpolates smoothly between two ARGB colors (including alpha channel).
     * @param fraction 0.0f returns 'from', 1.0f returns 'to'.
     */
    fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)

        val fa = (from ushr 24) and 0xFF
        val fr = (from ushr 16) and 0xFF
        val fg = (from ushr 8) and 0xFF
        val fb = from and 0xFF

        val ta = (to ushr 24) and 0xFF
        val tr = (to ushr 16) and 0xFF
        val tg = (to ushr 8) and 0xFF
        val tb = to and 0xFF

        val a = (fa + (ta - fa) * f).toInt()
        val r = (fr + (tr - fr) * f).toInt()
        val g = (fg + (tg - fg) * f).toInt()
        val b = (fb + (tb - fb) * f).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Overrides the alpha channel of an ARGB color.
     * @param alpha Int value between 0 (transparent) and 255 (opaque).
     */
    fun withAlpha(color: Int, alpha: Int): Int {
        val a = alpha.coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (a shl 24)
    }

    /**
     * Overrides the alpha channel of an ARGB color using a float percentage (0.0f - 1.0f).
     */
    fun withAlpha(color: Int, alphaFloat: Float): Int {
        return withAlpha(color, (alphaFloat.coerceIn(0f, 1f) * 255).toInt())
    }
}