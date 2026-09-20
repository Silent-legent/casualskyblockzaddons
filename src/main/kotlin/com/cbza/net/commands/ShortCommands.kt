package com.cbza.net.commands

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import tech.thatgravyboat.skyblockapi.helpers.McClient

object ShortCommands {
    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            // --- dungeons ---
            registerWarp(dispatcher, "dh", "dh", "dn")
            // --- farming ---
            registerWarp(dispatcher, "barn", "barn")
            registerWarp(dispatcher, "garden", "garden")
            // --- foraging ---
            registerWarp(dispatcher, "park", "park")
            registerWarp(dispatcher, "galatea", "galatea", "moonglade")
            registerWarp(dispatcher, "torrhus", "torrhus")
            // --- combat ---
            registerWarp(dispatcher, "spider", "spider", "spiders")
            registerWarp(dispatcher, "end", "end")
            registerWarp(dispatcher, "isle", "isle", "nether")
            // --- mining ---
            registerWarp(dispatcher, "gold", "gold")
            registerWarp(dispatcher, "deep", "deep", "cavern", "caverns")
            registerWarp(dispatcher, "mines", "mines", "dwarven")
            registerWarp(dispatcher, "camp", "camp")
            registerWarp(dispatcher, "nucleus", "cn", "nuc")
            // --- Fishing ---
            registerWarp(dispatcher, "atoll", "lotus", "atoll")
            // --- Extra ---
            registerWarp(dispatcher, "tower", "tower")
            registerWarp(dispatcher, "rift", "rift")
            registerWarp(dispatcher, "jerry", "jerry", "workshop")
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