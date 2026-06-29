package com.cbza.net

import com.cbza.net.config.ConfigScreen
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
		val arrowId = Identifier.fromNamespaceAndPath("casualskyblockaddons", "untitled")

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			dispatcher.register(literal("csz")
				.executes {
					val client = Minecraft.getInstance()
					client.execute {
						client.setScreen(ConfigScreen(client.screen))
					}
					1
				})
		}

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("casualskyblockaddons", "ability_hud")) { graphics, _ ->
			val popup = MiningAbilityTracker.getActivePopup() ?: return@addLast
			val mc = Minecraft.getInstance()
			val screenWidth = mc.window.guiScaledWidth
			val screenHeight = mc.window.guiScaledHeight
			val scale = 3.5f
			val textWidth = mc.font.width(popup)
			val x = ((screenWidth - textWidth * scale) / 2).toInt()
			val y = screenHeight / 3
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
			val mapSize = 100
			val margin = 10
			val arrowWidth = 9
			val arrowHeight = 9

			val mapX = margin
			val mapY = margin

			Render2D.drawImage(graphics, textureId, mapX, mapY, mapSize, mapSize)

			// draw POI dots
			for ((name, coords) in NucleusMap.discoveredPois) {
				val color = NucleusMap.poiColors[name] ?: continue
				val size = NucleusMap.poiSizes[name] ?: 6
				val poiPos = NucleusMap.getPoiMapPosition(coords.first, coords.second, mapSize)
				val px = mapX + poiPos.first
				val py = mapY + poiPos.second
				graphics.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, color)
			}

			// draw player arrow
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