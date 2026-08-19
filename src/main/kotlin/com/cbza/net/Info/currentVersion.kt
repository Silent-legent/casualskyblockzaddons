package com.cbza.net.Info

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.feature.mining.general.MiningAbilityTracker.onServerJoin
import com.google.gson.Gson
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object currentVersion {
    init {
        EventBus.subscribe<ServerJoinEvent> {
            onServerJoin()
            checkForUpdate()
        }
    }

    var updateMessageSent = false

    val gameVersion = "26.1.2"
    val currentVersionNumber = 6
    val currentVersionName = "1.0.4"

    val latestVersion = "https://raw.githubusercontent.com/Silent-legent/casualskyblockzaddons/refs/heads/main/Version"

    val CLIENT: HttpClient = HttpClient.newHttpClient()
    val GSON: Gson = Gson()

    fun checkForUpdate() {
        if (updateMessageSent) return

        val request: HttpRequest = HttpRequest.newBuilder()
            .uri(URI.create(latestVersion))
            .GET()
            .build()

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept { response ->
                if (response.statusCode() != 200) {
                    println("Failed to fetch version info: HTTP ${response.statusCode()}")
                    return@thenAccept
                }

                val data = GSON.fromJson(response.body(), VersionInfo::class.java)
                val versioncheck = currentVersionNumber - data.latestVersionNumber

                // If versioncheck is 0 or positive, your version is up to date or newer -> skip
                if (versioncheck >= 0) return@thenAccept

                updateMessageSent = true

                Thread.sleep(2500)

                // If we get here, an update exists! Safely send the message on the main thread.
                val client = Minecraft.getInstance()
                client.execute {
                    client.player?.sendSystemMessage(
                        Component.literal(
                            "§f══════════════§7\n" +
                                    "§c[§6CasualSkyblockZAddons§c]§7\n" +
                                    "There is a new update! §e${data.latestVersionName}§7\n" +
                                    "You're currently on §e$currentVersionName§7\n" +
                                    "§f══════════════§7"
                        )
                    )
                }
            }
    }
}

data class VersionInfo(
    val latestVersionNumber: Int,
    val latestVersionName: String
)