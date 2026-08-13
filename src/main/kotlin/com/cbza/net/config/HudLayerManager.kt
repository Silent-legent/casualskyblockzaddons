package com.cbza.net.config

import java.util.Collections

object HudLayerManager {
    fun hitOrder(): List<String> = ModConfig.get().hudLayerOrder.reversed()

    fun moveLayer(name: String, up: Boolean) {
        val list = ModConfig.get().hudLayerOrder
        val index = list.indexOf(name)
        if (index == -1) return

        val newIndex = if (up) index + 1 else index - 1

        if (newIndex in list.indices) {
            Collections.swap(list, index, newIndex)
            ModConfig.save() // Save changes when reordering!
        }
    }
}