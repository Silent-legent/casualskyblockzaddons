package com.cbza.net.Info

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.feature.mining.general.MiningAbilityTracker.onServerJoin
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object FirstLogin {
    init {
        EventBus.subscribe<ServerJoinEvent> {
            firstTime()
        }
    }

    fun firstTime() {
        if (ModConfig.get().firstTimeUsing) return

        Thread {
            Thread.sleep(3000)

            val client = Minecraft.getInstance()
            client.execute {
                client.player?.sendSystemMessage(
                    Component.literal(
                        "§f════════════════════§7\n" +
                                "§fWelcome to:§7 §c[§6CasualSkyblockZAddons§c]§7\n" +
                                "\n" +
                                "§fUse§7 §e/csz§7, §e/csz hud§7 §fto use our mod.§7\n" +
                                "§fAnd use§7 §e/csz help§7 §ffor more commands.§7\n" +
                                "\n" +
                                "§f════════════════════§7"
                    )
                )
                ModConfig.get().firstTimeUsing = true
                ModConfig.save()
            }
        }.start()
    }
}