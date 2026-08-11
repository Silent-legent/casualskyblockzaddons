package com.cbza.net.commands

import com.cbza.net.feature.slayers.SlayerCalc
import com.cbza.net.feature.slayers.SlayerXpTracker
import com.cbza.net.feature.slayers.slayerutil.SlayerBossXp
import com.cbza.net.feature.slayers.slayerutil.SlayerCost
import com.cbza.net.feature.slayers.slayerutil.SlayerType
import com.cbza.net.utility.TextFormat.formatCoins
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object SlayerCommands {

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("slayerxp")
                    .then(argument("slayer", StringArgumentType.word())
                        .suggests { _, builder ->
                            SlayerType.entries.forEach { builder.suggest(it.apiId) }
                            builder.buildFuture()
                        }
                        .then(argument("level", IntegerArgumentType.integer(1))
                            .then(argument("tier", IntegerArgumentType.integer(1))
                                // no currentXp typed — fall back to tracked value
                                .executes { ctx -> runSlayerCommand(ctx, typedXp = null) }
                                // currentXp typed — use it directly
                                .then(argument("currentXp", LongArgumentType.longArg(1))
                                    .executes { ctx ->
                                        val typedXp = LongArgumentType.getLong(ctx, "currentXp")
                                        runSlayerCommand(ctx, typedXp)
                                    }))))
            )
        }
    }

    private fun runSlayerCommand(ctx: CommandContext<FabricClientCommandSource>, typedXp: Long?): Int {
        val bossInput = StringArgumentType.getString(ctx, "slayer")
        val levelInput = IntegerArgumentType.getInteger(ctx, "level")
        val tierInput = IntegerArgumentType.getInteger(ctx, "tier")

        val client = Minecraft.getInstance()

        val slayer = SlayerType.entries.find { it.apiId == bossInput }
        if (slayer == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fUnknown slayertype '§e$bossInput§f'.\n" +
                            "§7Valid options: §e${SlayerType.entries.joinToString(", ") { it.apiId }}"
                )
            )
            return 1
        }

        // resolve currentXp: typed value first, tracked value as fallback
        val currentXp = typedXp ?: SlayerXpTracker.getCurrentXp(slayer)
        if (currentXp == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fNo tracked XP for §e${slayer.displayName}§f yet.\n" +
                            "§7Type it manually: §e/slayerxp $bossInput $levelInput $tierInput <currentXp>"
                )
            )
            return 1
        }

        val targetXp = SlayerCalc.xpForLevel(slayer, levelInput)
        if (targetXp == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fLevel §e$levelInput §fdoesn't exist for §e${slayer.displayName}§f."
                )
            )
            return 1
        }

        val xpPerBoss = SlayerBossXp.Level.valueOf(slayer.name).xp.getOrNull(tierInput - 1)
        if (xpPerBoss == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fTier §e$tierInput §fdoesn't exist for §e${slayer.displayName}§f."
                )
            )
            return 1
        }

        val pricePerBoss = SlayerCost.Cost.valueOf(slayer.name).prices.getOrNull(tierInput - 1)
        if (pricePerBoss == null) {
            client.player?.sendSystemMessage(
                Component.literal(
                    "§c[§6CasualSkyblockZAddons§c] §fSpawn cost for tier §e$tierInput §fdoesn't exist for §e${slayer.displayName}§f."
                )
            )
            return 1
        }

        val bossesLeft = SlayerCalc.calculateBosses(currentXp, targetXp, xpPerBoss)
        val totalCost = bossesLeft?.let { SlayerCalc.calcPrice(it, pricePerBoss) }
        val xpToNextLevel = targetXp - currentXp

        client.player?.sendSystemMessage(
            Component.literal(
                "§c[§6CasualSkyblockZAddons§c]\n" +
                        "§fSlayer §e${slayer.displayName} §7(§eT$tierInput§7)\n" +
                        "§fXP to next lvl: §a$xpToNextLevel\n" +
                        "§fBosses left: §a$bossesLeft\n" +
                        "§fSpawn cost: §a${formatCoins(totalCost ?: 0L)}"
            )
        )
        return 1
    }
}