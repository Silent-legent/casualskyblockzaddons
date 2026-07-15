package com.cbza.net

import com.cbza.net.config.ConfigScreen
import com.cbza.net.config.HudEditorScreen
import com.cbza.net.config.ModConfig
import com.cbza.net.feature.MiningAbilityTracker
import com.cbza.net.feature.NucleusMap
import com.cbza.net.utility.Render2D
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class CasualskyblockaddonsClient : ClientModInitializer {
	override fun onInitializeClient() {
		val textureId = Identifier.fromNamespaceAndPath("casualskyblockaddons", "nucleus_map")
		val arrowId = Identifier.fromNamespaceAndPath("casualskyblockaddons", "player_arrow")

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			MiningAbilityTracker.onServerJoin()
		}

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(literal("csz")
				.executes {
					val client = Minecraft.getInstance()
					client.execute {
						client.setScreen(ConfigScreen(client.screen))
					}
					1
				}
				.then(literal("hud")
					.executes {
						val client = Minecraft.getInstance()
						client.execute {
							client.setScreen(HudEditorScreen())
						}
						1
					})
				.then(literal("help")
					.executes {
						val client = Minecraft.getInstance()
						val msg = net.minecraft.network.chat.Component.literal(
							"§6§lCasualSkyblockAddons §7- §fCommands:\n" +
									"§e/csz §7- Open the mod settings screen.\n" +
									"§e/csz hud §7- Open the HUD editor.\n" +
									"§e/sharelocation <poi> §7- Share a discovered Crystal Hollows POI.\n" +
									"§e/csz help §7- Show this list."
						)
						client.player?.sendSystemMessage(msg)
						1
					}))
		}
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

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockaddons", "ability_hud")) { graphics, _ ->
			val popup = MiningAbilityTracker.getActivePopup() ?: return@addLast
			val mc = Minecraft.getInstance()
			val cfg = ModConfig.get()
			val screenWidth = mc.window.guiScaledWidth
			val screenHeight = mc.window.guiScaledHeight
			val scale = cfg.abilityAnnouncerScale
			val textWidth = mc.font.width(popup)
			val x = if (cfg.abilityAnnouncerX == -1) ((screenWidth - textWidth * scale) / 2).toInt() else cfg.abilityAnnouncerX
			val y = if (cfg.abilityAnnouncerY == -1) screenHeight / 3 else cfg.abilityAnnouncerY
			val color = ARGB.opaque(0x55FF55)
			graphics.pose().pushMatrix()
			graphics.pose().scale(scale, scale)
			graphics.text(mc.font, popup, (x / scale).toInt(), (y / scale).toInt(), color, true)
			graphics.pose().popMatrix()
		}

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockaddons", "nucleus_map")) { graphics, _ ->
			if (!ModConfig.get().NucleusMap) return@addLast
			if (!NucleusMap.inCrystalHollows) return@addLast

			val mc = Minecraft.getInstance()
			val cfg = ModConfig.get()
			val mapSize = (100 * cfg.nucleusMapScale).toInt()
			val arrowWidth = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)
			val arrowHeight = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)

			val mapX = cfg.nucleusMapX
			val mapY = cfg.nucleusMapY

			Render2D.drawImage(graphics, textureId, mapX, mapY, mapSize, mapSize)

			for ((name, coords) in NucleusMap.discoveredPois) {
				val color = NucleusMap.poiColors[name] ?: continue
				val size = ((NucleusMap.poiSizes[name] ?: 6) * cfg.nucleusMapScale).toInt()
				val poiPos = NucleusMap.getPoiMapPosition(coords.first, coords.second, mapSize)
				val px = mapX + poiPos.first
				val py = mapY + poiPos.second
				graphics.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, color)
			}

			for ((id, coords) in NucleusMap.unknownMarkers) {
				val poiPos = NucleusMap.getPoiMapPosition(coords.first, coords.second, mapSize)
				val px = mapX + poiPos.first
				val py = mapY + poiPos.second
				val size = (6 * cfg.nucleusMapScale).toInt()
				graphics.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, 0xFF808080.toInt())
			}

			val pos = NucleusMap.getPlayerMapPosition(mapSize)
			if (pos != null) {
				val dotX = mapX + pos.first
				val dotY = mapY + pos.second
				val yaw = mc.player?.yRot ?: 0f

				graphics.pose().pushMatrix()
				graphics.pose().translate(dotX.toFloat(), dotY.toFloat())
				graphics.pose().rotate(Math.toRadians((yaw + 180.0)).toFloat())
				graphics.pose().translate(-(arrowWidth / 2).toFloat(), -(arrowHeight / 2).toFloat())
				Render2D.drawImage(graphics, arrowId, 0, 0, arrowWidth, arrowHeight)
				graphics.pose().popMatrix()
			}
		}
	}
}
