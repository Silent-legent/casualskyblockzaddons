package com.cbza.net.commands

import com.cbza.net.feature.mining.hollows.map.NucleusMap
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft

object MiningCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("sharelocation")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument("poi", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                    .suggests { _, builder ->
                        NucleusMap.discoveredPois.keys.forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }
                    .executes { ctx ->
                        val name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "poi")
                        val client = Minecraft.getInstance()
                        val coords = NucleusMap.discoveredPois[name]
                        if (coords == null) {
                            client.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cPOI '$name' not discovered yet."))
                        } else {
                            val (x, z) = coords
                            val y = client.player?.blockY ?: 0
                            client.player?.connection?.sendCommand("ac x: ${x.toInt()} y: $y z: ${z.toInt()} $name")
                            client.player?.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aShared location for $name!"))
                        }
                        1
                    }))
        }
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(literal("calculatetick")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument(
                    "miningSpeed", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                    .then(net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument(
                        "block", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests { _, builder ->
                            com.cbza.net.utility.BlockStrength.strengths.keys.forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val miningSpeed = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "MiningSpeed")
                            val blockInput = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "Block")
                            val blockKey = blockInput.uppercase()
                            val client = Minecraft.getInstance()

                            val strength = com.cbza.net.utility.BlockStrength.strengths[blockKey]
                            if (strength == null) {
                                client.player?.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal(
                                        "§c[§6CasualSkyblockZAddons§c] §fUnknown block/gemstone '§e$blockInput§f'.\n" +
                                                "§7Valid options: §e${com.cbza.net.utility.BlockStrength.strengths.keys.joinToString(", ")}"
                                    )
                                )
                                return@executes 1
                            }

                            val ticks = com.cbza.net.utility.BlockStrength.calculateTicks(blockKey, miningSpeed)
                            if (ticks == null) {
                                client.player?.sendSystemMessage(
                                    net.minecraft.network.chat.Component.literal("§c[§6CasualSkyblockZAddons§c] §fInvalid mining speed.")
                                )
                                return@executes 1
                            }

                            val ms = com.cbza.net.utility.BlockStrength.ticksToMs(ticks)

                            val nextTickLine: String = com.cbza.net.utility.BlockStrength.speedForNextTick(strength, ticks)
                                ?.let { speedNeeded ->
                                    val moreNeeded = (speedNeeded - miningSpeed).coerceAtLeast(0)
                                    "§fNext tick §7(§a${ticks - 1} ticks§7): §fneed §a+$moreNeeded §fmore Mining Speed §7(§e$speedNeeded §ftotal§7)"
                                }
                                ?: "§7Already at the minimum tick count (4)."

                            client.player?.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                    "§c[§6CasualSkyblockZAddons§c]\n" +
                                            "§fTick calculation for §e$blockKey\n" +
                                            "§f@ §e$miningSpeed §fMining Speed:\n" +
                                            "§fBreaks in §a$ticks ticks §7(§a${ms}ms§7)\n" +
                                            nextTickLine
                                )
                            )
                            1
                        })))
        }
    }
}