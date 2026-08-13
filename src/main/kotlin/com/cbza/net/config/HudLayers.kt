package com.cbza.net.config

import com.cbza.net.feature.mining.general.CommissionsDisplay
import com.cbza.net.feature.mining.general.MiningAbilityTracker
import com.cbza.net.feature.mining.hollows.map.NucleusMap
import net.minecraft.client.gui.GuiGraphicsExtractor

object HudLayers {
    private val renderers: Map<String, (GuiGraphicsExtractor) -> Unit> = mapOf(
        "ability_announcer"  to MiningAbilityTracker::render,
        "commission_display" to CommissionsDisplay::render,
        "nucleus_map"        to NucleusMap::render
    )

    fun renderAll(graphics: GuiGraphicsExtractor) {
        for (name in ModConfig.get().hudLayerOrder) {
            renderers[name]?.invoke(graphics)
        }
    }
}