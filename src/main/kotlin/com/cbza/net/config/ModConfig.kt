package com.cbza.net.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

class ModConfig {
	@JvmField
	var showRarityBackgrounds = true
	var PowderChestSolver = true
	var MiningAbilityAnnouncer = true
	var NucleusMap = true
	var PowderChestYOffset = 0.0

	// HUD positions and scales
	var abilityAnnouncerX = -1 // -1 means default/centered
	var abilityAnnouncerY = -1
	var abilityAnnouncerScale = 3.5f

	var nucleusMapX = 10
	var nucleusMapY = 10
	var nucleusMapScale = 1.0f

	companion object {
		private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
		private val PATH: Path = FabricLoader.getInstance().configDir.resolve("casualskyblockaddons.json")

		@Volatile
		private var instance: ModConfig? = null

		fun get(): ModConfig {
			if (instance == null) load()
			return instance!!
		}

		fun load() {
			instance = if (Files.exists(PATH)) {
				try {
					val json = PATH.toFile().readText(Charsets.UTF_8)
					GSON.fromJson(json, ModConfig::class.java)
				} catch (e: Exception) {
					ModConfig()
				}
			} else {
				ModConfig()
			}
			if (instance == null) instance = ModConfig()
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