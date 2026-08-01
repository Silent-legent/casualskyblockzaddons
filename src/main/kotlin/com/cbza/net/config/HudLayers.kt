package com.cbza.net.config

import com.cbza.net.feature.mining.general.CommissionsDisplay
import com.cbza.net.feature.mining.general.MiningAbilityTracker
import com.cbza.net.feature.mining.hollows.map.NucleusMap
import net.minecraft.client.gui.GuiGraphicsExtractor

object HudLayers {
    private val renderers: Map<String, (GuiGraphicsExtractor) -> Unit> = mapOf(
        "ability_announcer"  to { g -> MiningAbilityTracker.render(g) },
        "Commission_Display" to { g -> CommissionsDisplay.render(g) },
        "nucleus_map"        to { g -> NucleusMap.render(g) }
    )

    fun renderAll(graphics: GuiGraphicsExtractor) {
        for (name in ModConfig.get().hudLayerOrder) {
            renderers[name]?.invoke(graphics)
        }
    }
}