package com.cbza.net

import com.cbza.net.config.ConfigScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.Minecraft
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal

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
	}
}