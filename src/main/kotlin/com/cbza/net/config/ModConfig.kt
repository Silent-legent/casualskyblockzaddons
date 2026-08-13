package com.cbza.net.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

class ModConfig {

	// --- Toggles ---
	var showRarityBackgrounds = true
	var powderChestSolver = true
	var miningAbilityAnnouncer = true
	var nucleusMap = true
	var pingGlide = true
	var commissionsDisplay = true
	var mimicChest = true

	// --- Settings ---
	var powderChestYOffset = 0.0
	var manualPing = 0

	// --- HUD Positioning ---
	var hudLayerOrder: MutableList<String> = mutableListOf(
		"ability_announcer",
		"commission_display",
		"nucleus_map"
	)

	var abilityAnnouncerX = -1
	var abilityAnnouncerY = -1
	var abilityAnnouncerScale = 3.5f

	var nucleusMapX = 10
	var nucleusMapY = 10
	var nucleusMapScale = 1.0f

	var commissionsDisplayX = 0
	var commissionsDisplayY = 100
	var commissionsDisplayScale = 1.0f

	companion object {
		private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
		private val PATH: Path = FabricLoader.getInstance().configDir.resolve("casualskyblockzaddons.json")

		@Volatile
		private var instance: ModConfig? = null

		fun get(): ModConfig {
			return instance ?: load()
		}

		fun load(): ModConfig {
			val config = if (Files.exists(PATH)) {
				try {
					val json = PATH.toFile().readText(Charsets.UTF_8)
					GSON.fromJson(json, ModConfig::class.java) ?: ModConfig()
				} catch (e: Exception) {
					ModConfig()
				}
			} else {
				ModConfig()
			}
			instance = config
			return config
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
}