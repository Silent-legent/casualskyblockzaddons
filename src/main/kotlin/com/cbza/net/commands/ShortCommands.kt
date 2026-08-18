package com.cbza.net.commands

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import tech.thatgravyboat.skyblockapi.helpers.McClient

object ShortCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            // dungeons
            registerWarp(dispatcher, "dh", "dh", "dn")
            // farming
            registerWarp(dispatcher, "garden", "garden")
            // foraging
            registerWarp(dispatcher, "galatea", "galatea")
            registerWarp(dispatcher, "torrhus", "torrhus")
            // combat
            registerWarp(dispatcher, "isle", "isle")
            // mining
            registerWarp(dispatcher, "mines", "mines", "dwarven")
            registerWarp(dispatcher, "camp", "camp")
            registerWarp(dispatcher, "nucleus", "cn", "nuc")
        }
    }

    private fun registerWarp(
        dispatcher: CommandDispatcher<FabricClientCommandSource>,
        target: String,
        vararg aliases: String
    ) {
        for (name in aliases) {
            dispatcher.register(literal(name)
                .executes {
                    McClient.sendCommand("warp $target")
                    1
                })
        }
    }
}