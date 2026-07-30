package com.cbza.net

import com.cbza.net.config.ConfigScreen
import com.cbza.net.config.HudEditorScreen
import com.cbza.net.config.ModConfig
import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent
import com.cbza.net.feature.mining.general.MiningAbilityTracker
import com.cbza.net.feature.mining.hollows.map.NucleusMap
import com.cbza.net.utility.Render2D
import com.cbza.net.feature.mining.hollows.PowderChestSolver
import com.cbza.net.feature.mining.hollows.map.WishingCompassSolver
import com.cbza.net.feature.mining.general.PingGlide
import com.cbza.net.feature.dungeons.MimicChest
import com.cbza.net.feature.mining.general.CommissionsDisplay
import com.cbza.net.external.stella.customname.Cosmetics

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class CasualskyblockzaddonsClient : ClientModInitializer {
	override fun onInitializeClient() {
		Cosmetics.init()
		CommissionsDisplay.register()

		// calls run at startup instead of whenever something else happens to touch them first.
		PowderChestSolver
		NucleusMap
		MiningAbilityTracker
		WishingCompassSolver
		PingGlide
		MimicChest

		val textureId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "nucleus_map")
		val arrowId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "player_arrow")

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			EventBus.post(ServerJoinEvent())
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
							"§6§lCasualSkyblockZAddons\n" +
									"§fCommands:\n" +
									"§e/csz §7- Open the mod settings screen.\n" +
									"§e/csz hud §7- Open the HUD editor.\n" +
									"§e/sharelocation <poi> §7- Share a discovered Crystal Hollows POI.\n" +
									"§e/calculatetick <miningSpeed> <block §7- Calculates ticks-to-break for a block at a given Mining Speed, + speed needed for the next tick.\n" +
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
							val miningSpeed = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "miningSpeed")
							val blockInput = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "block")
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
									"§6§lCasualSkyblockZAddons\n" +
											"§fTick calculation for §e$blockKey\n" +
											"§f@ §e$miningSpeed §fMining Speed:\n" +
											"§fBreaks in §a$ticks ticks §7(§a${ms}ms§7)\n" +
											nextTickLine
								)
							)
							1
						})))
		}


		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockzaddons", "ability_hud")) { graphics, _ ->
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

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockzaddons", "nucleus_map")) { graphics, _ ->
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
