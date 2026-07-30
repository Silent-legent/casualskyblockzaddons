package com.cbza.net.feature.mining.general

import com.cbza.net.config.HudEditorScreen
import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.TabListReader
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

object CommissionsDisplay {

    private val miningIslands = setOf(
        SkyBlockIsland.DWARVEN_MINES,
        SkyBlockIsland.CRYSTAL_HOLLOWS,
        SkyBlockIsland.MINESHAFT,
    )
    private var wasOnMiningIsland = false

    val commissionPattern = Regex("""(.+): (\d+(?:\.\d+)?%|DONE)""")

    fun register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockzaddons", "commissions")) { graphics, _ ->
            val onMiningIsland = miningIslands.any { it.inIsland() }
            if (wasOnMiningIsland && !onMiningIsland) reset()
            wasOnMiningIsland = onMiningIsland
            if (!onMiningIsland) return@addLast

            val cfg = ModConfig.get()
            if (!cfg.CommissionsDisplay) return@addLast

            if (Minecraft.getInstance().screen is HudEditorScreen) return@addLast

            val lines = TabListReader.getCommissionLines()
            if (lines.isEmpty()) return@addLast

            val mc = Minecraft.getInstance()
            val startX = cfg.CommissionsDisplayX
            val startY = cfg.CommissionsDisplayY
            val scale = cfg.CommissionsDisplayScale
            val lineHeight = 10
            val nameColor = ColorCatalog.WHITE

            graphics.pose().pushMatrix()
            graphics.pose().scale(scale, scale)

            val ux = (startX / scale).toInt()
            val uy = (startY / scale).toInt()

            val validCommissions = lines.mapNotNull { line ->
                val match = commissionPattern.find(line) ?: return@mapNotNull null
                val name = match.groupValues[1]
                val value = match.groupValues[2]
                val endsWithLevelNumber = Regex("""\s\d+$""")
                if (endsWithLevelNumber.containsMatchIn(name)) return@mapNotNull null
                Pair(name, value)
            }

            for ((index, pair) in validCommissions.withIndex()) {
                val (name, value) = pair
                val y = uy + (index * lineHeight)

                val namePrefix = "$name: "

                graphics.text(mc.font, namePrefix, ux, y, nameColor, true)
                val valueX = ux + mc.font.width(namePrefix)
                val progress: Float = if (value == "DONE") {
                    1f
                } else {
                    val number = value.removeSuffix("%").toFloatOrNull() ?: 0f
                    number / 100f
                }
                val valueColor = ColorCatalog.lerpColor(ColorCatalog.RED, ColorCatalog.GREEN, progress)
                graphics.text(mc.font, value, valueX, y, valueColor, true)
            }

            graphics.pose().popMatrix()
        }
    }
    fun reset() {
    }
}
