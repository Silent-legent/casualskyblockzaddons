package com.example.client;

import com.example.client.config.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CasualskyblockaddonsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Registers the "/csz" command safely.
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(literal("csz")
						.executes(context -> {
							Minecraft client = Minecraft.getInstance();

							// FORCE execution on the main game/render thread
							client.execute(() -> {
								// Pass client.screen to keep track of the parent menu context
								client.setScreen(new ConfigScreen(client.screen));
							});

							return 1;
						}))
		);
	}
}