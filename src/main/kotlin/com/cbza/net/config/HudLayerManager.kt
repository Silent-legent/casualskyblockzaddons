package com.cbza.net.config

object HudLayerManager {
    fun hitOrder(): List<String> = ModConfig.get().hudLayerOrder.reversed()

    fun moveLayer(name: String, up: Boolean) {
            val cfg = ModConfig.get()
            val list = cfg.hudLayerOrder
            val index = list.indexOf(name)
            val newIndex = if (up) index + 1 else index - 1

            if (newIndex in list.indices) {
                val temp = list[index]

                list[index] = list[newIndex]
                list[newIndex] = temp
            }
        }
    }