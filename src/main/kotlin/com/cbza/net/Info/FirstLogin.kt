package com.cbza.net.Info

import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.feature.mining.general.MiningAbilityTracker.onServerJoin
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

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
                val discordUrl = "https://discord.gg/UVN99sypv"

                val discordLink = Component.literal(discordUrl)
                    .withStyle(
                        Style.EMPTY
                            .withColor(net.minecraft.ChatFormatting.BLUE)
                            .withClickEvent(ClickEvent.OpenUrl(java.net.URI.create(discordUrl)))
                            .withHoverEvent(HoverEvent.ShowText(Component.literal("Click to join the Discord")))
                    )

                val message = Component.literal(
                    "§f════════════════════§7\n" +
                            "§fWelcome to:§7 §c[§6CasualSkyblockZAddons§c]§7\n" +
                            "\n" +
                            "§fUse§7 §e/csz§7, §e/csz hud§7 §fto use our mod.§7\n" +
                            "§fAnd use§7 §e/csz help§7 §ffor more commands.§7\n" +
                            "\n" +
                            "§fNeed more suport? Join our discord!§7\n"
                )
                    .append(discordLink)
                    .append(
                        Component.literal(
                            "§7\n" +
                                    "\n" +
                                    "§f════════════════════§7"
                        )
                    )

                client.player?.sendSystemMessage(message)

                ModConfig.get().firstTimeUsing = true
                ModConfig.save()
            }
        }.start()
    }
}