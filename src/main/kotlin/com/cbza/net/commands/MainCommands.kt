package com.cbza.net.commands

import com.cbza.net.config.ConfigScreen
import com.cbza.net.config.HudEditorScreen
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft

object MainCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("csz")
                .executes {
                    val client = Minecraft.getInstance()
                    client.execute {
                        client.setScreen(ConfigScreen(client.screen))
                    }
                    1
                }
                .then(literal("hud")
                    .executes {
                        val client = Minecraft.getInstance()
                        client.execute {
                            client.setScreen(HudEditorScreen())
                        }
                        1
                    })
                .then(literal("help")
                    .executes {
                        val client = Minecraft.getInstance()
                        val msg = net.minecraft.network.chat.Component.literal(
                            "§c[§6CasualSkyblockZAddons§c]\n" +
                                    "§fCommands:\n" +
                                    "§e/csz §7- Open the mod settings screen.\n" +
                                    "§e/csz hud §7- Open the HUD editor.\n" +
                                    "§e/sharelocation <poi> §7- Share a discovered Crystal Hollows POI.\n" +
                                    "§e/calculatetick <miningSpeed> <block §7- Calculates ticks-to-break for a block at a given Mining Speed, + speed needed for the next tick.\n" +
                                    "§e/csz help §7- Show this list."
                        )
                        client.player?.sendSystemMessage(msg)
                        1
                    }))
        }
    }
}