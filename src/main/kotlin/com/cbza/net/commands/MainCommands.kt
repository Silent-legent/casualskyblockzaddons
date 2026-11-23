package com.cbza.net.commands

import com.cbza.net.api.cushion.mc
import com.cbza.net.api.cushion.screen
import com.cbza.net.api.cushion.setScreen
import com.cbza.net.config.ConfigScreen
import com.cbza.net.config.HudEditorScreen
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal

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
                                    })
                            .then(literal("Farming")
                                .executes {
                                    sendFarmingCommands()
                                    1
                                })
                            .then(
                                literal("Slayer")
                                    .executes {
                                        sendSlayerCommands()
                                        1
                                    })
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
        mc.execute {
            setScreen(ConfigScreen(screen))
        }
    }

    private fun openHudEditor() {
        mc.execute {
            setScreen(HudEditorScreen())
        }
    }

    private fun sendHelpMessage() {
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
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendDungeonCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fDungeon Commands:\n" +
                    "There are currently no §eDungeon§7 commands"
        )
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendFarmingCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§Farming Commands:\n" +
                    "§e/PestCooldown <cooldown(seconds)> §7- Sets the cooldown on the pest spawn warning.\n"
        )
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendForagingCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fForaging Commands:\n" +
                    "There are currently no §Foraging§7 commands"
        )
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendMiningCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fMining Commands:\n" +
                    "§e/sharelocation <poi> §7- Share a discovered Crystal Hollows POI.\n" +
                    "§e/calculatetick <miningSpeed> <block §7- Calculates ticks-to-break for a block at a given Mining Speed, + speed needed for the next tick.\n" +
                    "§f══════════════§7"
        )
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendSlayerCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fSlayer Commands:\n" +
                    "§e/slayerxp <slayer> <level> <tier> [currentXp] §7- Calculates XP needed, bosses remaining, and spawn cost to reach the target level. (Skip currentXp if you already killed one boss.)\n" +
                    "§f══════════════§7"
        )
        mc.player?.sendSystemMessage(msg)
    }
    private fun sendShortCommands() {
        val msg = net.minecraft.network.chat.Component.literal(
            "§f══════════════§7\n" +
                    "§c[§6CasualSkyblockZAddons§c]\n" +
                    "§fShort Commands:\n" +
                    "§e/dh, dn§7- Warps u to the Dungeon Hub.\n" +
                    "§e/barn§7- Warps u to the Barn.\n" +
                    "§e/garden§7- Warps u to the Garden.\n" +
                    "§e/park§7- Warps u to the Park.\n" +
                    "§e/galatea, /moonglade§7- Warps u to the Galatea.\n" +
                    "§e/torrhus§7- Warps u to the Torrhus Canyon.\n" +
                    "§e/spider, /spiders§7- Warps u to the Spiders Den.\n" +
                    "§e/end§7- Warps u to the End.\n" +
                    "§e/isle, /nether§7- Warps u to the Crimson Isle.\n" +
                    "§e/lotus, /atoll§7- Warps u to the Lotus Atoll.\n" +
                    "§e/gold§7- Warps u to the Gold Mines.\n" +
                    "§e/deep, /cavern, /caverns§7- Warps u to the Deep Caverns.\n" +
                    "§e/mines, dwarven§7- Warps u to the Dwarven Mines.\n" +
                    "§e/camp§7- Warps u to the Dwarven Base Camp.\n"+
                    "§e/cn, nuc§7- Warps u to the Crystal Nucleus.\n" +
                    "§e/tower§7- Warps u to the Rift Tower.\n" +
                    "§e/rift§7- Warps u to the Rift.\n" +
                    "§e/jerry, /workshop§7- Warps u to the Jerry's Workshop.\n" +
                    "§f══════════════§7"
        )
        mc.player?.sendSystemMessage(msg)
    }
}