package com.cbza.net

import com.cbza.net.commands.MainCommands
import com.cbza.net.commands.MiningCommands
import com.cbza.net.commands.ShortCommands
import com.cbza.net.commands.SlayerCommands
import com.cbza.net.config.HudEditorScreen
import com.cbza.net.config.HudLayers
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.external.stella.customname.Cosmetics
import com.cbza.net.feature.dungeons.MimicChest
import com.cbza.net.feature.mining.general.MiningAbilityTracker
import com.cbza.net.feature.mining.general.PingGlide
import com.cbza.net.feature.mining.hollows.PowderChestSolver
import com.cbza.net.feature.mining.hollows.map.NucleusMap
import com.cbza.net.feature.mining.hollows.map.WishingCompassSolver
import com.cbza.net.feature.slayers.SlayerXpTracker

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier

class CasualskyblockzaddonsClient : ClientModInitializer {
	override fun onInitializeClient() {
		Cosmetics.init()

		// Evaluated at startup to register EventBus subscribers
		PowderChestSolver
		NucleusMap
		MiningAbilityTracker
		WishingCompassSolver
		PingGlide
		MimicChest
		SlayerXpTracker

		MainCommands.register()
		MiningCommands.register()
		ShortCommands.register()
		SlayerCommands.register()

		ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			EventBus.post(ServerJoinEvent)
		}

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockzaddons", "main_hud")) { graphics, _ ->
			if (Minecraft.getInstance().screen is HudEditorScreen) return@addLast
			HudLayers.renderAll(graphics)
		}
	}
}