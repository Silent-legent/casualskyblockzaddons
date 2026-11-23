package com.cbza.net.commands

import com.cbza.net.api.cushion.mc
import com.cbza.net.config.ModConfig
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

object FarmingCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(
                literal("PestCooldown")
                    .then(argument("cooldown", IntegerArgumentType.integer(1))
                    .executes { ctx -> setPestTimer(ctx)
                        1
                    }))
            }
        }

    // maybe add a min at sum point.
    fun setPestTimer(ctx: CommandContext<FabricClientCommandSource>) {
        val cooldownInput = IntegerArgumentType.getInteger(ctx, "cooldown")

        ModConfig.get().farming.spawnIntervalSeconds = cooldownInput
        ModConfig.save()

        val msg = net.minecraft.network.chat.Component.literal(
            "§c[§6CasualSkyblockZAddons§c] " +
            "§fPestCooldown set to '§e$cooldownInput§f' seconds..\n"
        )
        mc.player?.sendSystemMessage(msg)
    }

}