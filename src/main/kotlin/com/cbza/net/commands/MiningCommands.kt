package com.cbza.net.commands

import com.cbza.net.feature.mining.hollows.map.NucleusMap
import com.cbza.net.utility.BlockStrength
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object MiningCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->

            // /sharelocation <poi>
            dispatcher.register(literal("sharelocation")
                .then(argument("poi", StringArgumentType.greedyString())
                    .suggests { _, builder ->
                        NucleusMap.discoveredPois.keys.forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }
                    .executes { ctx ->
                        val poiName = StringArgumentType.getString(ctx, "poi")
                        shareLocations(poiName)
                        1
                    }))

            // /calculatetick <miningSpeed> <block>
            dispatcher.register(literal("calculatetick")
                .then(argument("miningSpeed", IntegerArgumentType.integer(1))
                    .then(argument("block", StringArgumentType.word())
                        .suggests { _, builder ->
                            BlockStrength.strengths.keys.forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }
                        .executes { ctx ->
                            val miningSpeed = IntegerArgumentType.getInteger(ctx, "miningSpeed")
                            val blockInput = StringArgumentType.getString(ctx, "block")
                            val blockKey = blockInput.uppercase()

                            calculateTick(miningSpeed, blockKey)
                            1
                        })))
        }
    }

    private fun shareLocations(name: String) {
        val client = Minecraft.getInstance()
        val coords = NucleusMap.discoveredPois[name]

        if (coords == null) {
            client.player?.sendSystemMessage(Component.literal("§cPOI '$name' not discovered yet."))
        } else {
            val (x, z) = coords
            val y = client.player?.blockY ?: 0
            client.player?.connection?.sendCommand("ac x: ${x.toInt()} y: $y z: ${z.toInt()} $name")
            client.player?.sendSystemMessage(Component.literal("§aShared location for $name!"))
        }
    }

    private fun calculateTick(miningSpeed: Int, blockKey: String) {
        val client = Minecraft.getInstance()

        val strength = BlockStrength.strengths[blockKey]
        if (strength == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fUnknown block/gemstone '§e$blockKey§f'.\n" +
                            "§7Valid options: §e${BlockStrength.strengths.keys.joinToString(", ")}"
                )
            )
            return
        }

        val ticks = BlockStrength.calculateTicks(blockKey, miningSpeed)
        if (ticks == null) {
            client.player?.sendSystemMessage(
                Component.literal("§c[§6CasualSkyblockZAddons§c] §fInvalid mining speed.")
            )
            return
        }

        val ms = BlockStrength.ticksToMs(ticks)

        val nextTickLine: String = BlockStrength.speedForNextTick(strength, ticks)
            ?.let { speedNeeded ->
                val moreNeeded = (speedNeeded - miningSpeed).coerceAtLeast(0)
                "§fNext tick §7(§a${ticks - 1} ticks§7): §fneed §a+$moreNeeded §fmore Mining Speed §7(§e$speedNeeded §ftotal§7)"
            }
            ?: "§7Already at the minimum tick count (4)."

        client.player?.sendSystemMessage(
            Component.literal(
                "§f══════════════§7\n" +
                        "§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fTick calculation for §e$blockKey\n" +
                        "§f@ §e$miningSpeed §fMining Speed:\n" +
                        "§fBreaks in §a$ticks ticks §7(§a${ms}ms§7)\n" +
                        "$nextTickLine\n" +
                        "§f══════════════§7"
            )
        )
    }
}