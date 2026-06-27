package com.cbza.net

import com.cbza.net.config.ConfigScreen
import com.cbza.net.feature.MiningAbilityTracker
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class CasualskyblockaddonsClient : ClientModInitializer {
	override fun onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(literal("csz")
				.executes {
					val client = Minecraft.getInstance()
					client.execute {
						client.setScreen(ConfigScreen(client.screen))
					}
					1
				})
		}

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockaddons", "ability_hud")) { graphics, _ ->
			val popup = MiningAbilityTracker.getActivePopup() ?: return@addLast
			val mc = Minecraft.getInstance()
			val screenWidth = mc.window.guiScaledWidth
			val screenHeight = mc.window.guiScaledHeight
			val scale = 3.5f
			val textWidth = mc.font.width(popup)
			val x = ((screenWidth - textWidth * scale) / 2).toInt()
			val y = screenHeight / 3
			val color = ARGB.opaque(0x55FF55)
			graphics.pose().pushMatrix()
			graphics.pose().scale(scale, scale)
			graphics.text(mc.font, popup, (x / scale).toInt(), (y / scale).toInt(), color, true)
			graphics.pose().popMatrix()
		}
	}
}