package com.cbza.net.commands

import com.cbza.net.config.ConfigScreen
import com.cbza.net.config.HudEditorScreen
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft

object MainCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("csz")
                    .executes {
                        openConfigScreen()
                        1
                    }
                    .then(
                        literal("hud")
                            .executes {
                                openHudEditor()
                                1
                            })
                    .then(
                        literal("help")
                            .executes {
                                sendHelpMessage()
                                1
                            }
                            .then(
                                literal("Mining")
                                    .executes {
                                        sendMiningCommands()
                                        1
                                    }
                            )
                            .then(
                                literal("Slayer")
                                    .executes {
                                        sendSlayerCommands()
                                        1
                                    }
                            )
                            .then(
                                literal("Short")
                                    .executes {
                                        sendShortCommands()
                                        1
                                    }
                            ))
            )
        }
    }

    private fun openConfigScreen() {
        val client = Minecraft.getInstance()
        client.execute {
            client.setScreen(ConfigScreen(client.screen))
        }
    }

    private fun openHudEditor() {
        val client = Minecraft.getInstance()
        client.execute {
            client.setScreen(HudEditorScreen())
        }
    }

    private fun sendHelpMessage() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fCommands:\n" +
                    "§e/csz §7- Opens the mod settings screen.\n" +
                    "§e/csz hud §7- Opens the HUD editor.\n" +
                    "§e/csz help §7- Shows this list.\n" +
                    "§e/csz help <skill>§7- Shows all the commands related to that skill.\n" +
                    "§e/csz help short§7- Shows every short command.\n" +
                    "§f══════════════§7"
        )
        client.player?.sendSystemMessage(msg)
    }
    private fun sendDungeonCommands() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fDungeon Commands:\n" +
                    "There are currently no §eDungeon§7 commands"
        )
        client.player?.sendSystemMessage(msg)
    }
    private fun sendForagingCommands() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fForaging Commands:\n" +
                    "There are currently no §Foraging§7 commands"
        )
        client.player?.sendSystemMessage(msg)
    }
    private fun sendMiningCommands() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fMining Commands:\n" +
                    "§e/sharelocation <poi> §7- Share a discovered Crystal Hollows POI.\n" +
                    "§e/calculatetick <miningSpeed> <block §7- Calculates ticks-to-break for a block at a given Mining Speed, + speed needed for the next tick.\n" +
                    "§f══════════════§7"
        )
        client.player?.sendSystemMessage(msg)
    }
    private fun sendSlayerCommands() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fSlayer Commands:\n" +
                    "§e/slayerxp <slayer> <level> <tier> [currentXp] §7- Calculates XP needed, bosses remaining, and spawn cost to reach the target level. (Skip currentXp if you already killed one boss.)\n" +
                    "§f══════════════§7"
        )
        client.player?.sendSystemMessage(msg)
    }
    private fun sendShortCommands() {
        val client = Minecraft.getInstance()
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fShort Commands:\n" +
                    "§e/dh, dn§7- Warps u to the Dungeon Hub.\n" +
                    "§e/garden§7- Warps u to the Garden.\n" +
                    "§e/galatea§7- Warps u to the Galatea.\n" +
                    "§e/torrhus§7- Warps u to the Torrhus Canyon.\n" +
                    "§e/isle§7- Warps u to the Crimson Isle.\n" +
                    "§e/mines, dwarven§7- Warps u to the Dwarven Mines.\n" +
                    "§e/camp§7- Warps u to the Dwarven Base Camp.\n"+
                    "§e/cn, nuc§7- Warps u to the Crystal Nucleus.\n" +
                    "§f══════════════§7"
        )
        client.player?.sendSystemMessage(msg)
    }
}