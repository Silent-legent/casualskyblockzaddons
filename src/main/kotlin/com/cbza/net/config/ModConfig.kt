package com.cbza.net.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import net.fabricmc.loader.api.FabricLoader

import java.nio.file.Files
import java.nio.file.Path

class ModConfig {

	var firstTimeUsing = false

	var misc = Misc()
	var general = General()
	var mining = Mining()
	var dungeons = Dungeons()
	var rift = Rift()

	class Misc {
		var panels: MutableMap<String, PanelState> = mutableMapOf()
		var panelOrder: MutableList<String> = mutableListOf()
		var hudLayerOrder: MutableList<String> = mutableListOf(
			"ability_announcer",
			"puff_kill_announcer",
			"commission_display",
			"nucleus_map",
			"player_inventory",
		)
	}

	class General {
		var showRarityBackgrounds = true

		var playerInventory = false
		var playerInventoryPos = HudPosition(-1,  -1, 1.0f)

		var hideActionBarStats = true
	}

	class Mining {
		var nucleusMap = true
		var nucleusMapPos = HudPosition(0,  0, 1.0f)

		var miningAbilityAnnouncer = true
		var miningAbilityAnnouncerPos = HudPosition(-1,  -1, 3.5f)

		var commissionsDisplay = true
		var commissionDisplayPos = HudPosition(0,  100, 1.0f)

		var pingGlide = true
		var manualPing = 0

		var powderChestSolver = true
		var powderChestYOffset = 0.0
	}

	class Dungeons {
		var mimicChest = true
	}

	class Rift {
		var puffKillAnnouncer = false
		var puffKillAnnouncerPos = HudPosition(-1,  -1, 1.0f)
	}

	companion object {
		private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
		private val PATH: Path = FabricLoader.getInstance().configDir.resolve("casualskyblockzaddons.json")

		@Volatile
		private var instance: ModConfig? = null

		@JvmStatic
		fun get(): ModConfig {
			return instance ?: load()
		}

		fun load(): ModConfig {
			val loaded = if (Files.exists(PATH)) {
				try {
					val json = PATH.toFile().readText(Charsets.UTF_8)
					GSON.fromJson(json, ModConfig::class.java) ?: ModConfig()
				} catch (e: Exception) {
					ModConfig()
				}
			} else {
				ModConfig()
			}

			val defaults = ModConfig().misc.hudLayerOrder
			for (layer in defaults) {
				if (!loaded.misc.hudLayerOrder.contains(layer)) {
					loaded.misc.hudLayerOrder.add(layer)
				}
			}

			instance = loaded
			return loaded
		}

		fun save() {
			try {
				if (!Files.exists(PATH.parent)) {
					Files.createDirectories(PATH.parent)
				}
				PATH.toFile().writeText(GSON.toJson(get()), Charsets.UTF_8)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}
	class HudPosition(var x: Int = -1, var y: Int = -1, var scale: Float = 1f)
}
val config get() = ModConfig.get()
