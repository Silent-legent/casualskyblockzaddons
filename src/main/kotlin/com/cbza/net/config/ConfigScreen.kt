package com.cbza.net.config

import com.cbza.net.config.TEMPCategoryPanel.TITLE_HEIGHT
import com.cbza.net.config.TEMPCategoryPanel.rowY
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class ConfigScreen(parent: Screen?) : Screen(Component.literal("CasualSkyblockzAddons")) {

	private val categories = mutableListOf<ConfigCategory>()
	private val openCategories = mutableSetOf<Int>()
	private val panels = mutableListOf<PanelState>()

	private var hoveredKey: String? = null
	private var hoverStartTime: Long = 0L

	companion object {
		private const val ROW_HEIGHT = 14
		private const val HOVER_DELAY_MS = 1000L
		private const val DESC_BG_COLOR: Int = 0xE0101010.toInt()
		private const val DESC_BORDER_COLOR: Int = 0xFF555555.toInt()
		private const val DESC_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()
		private const val DESC_PADDING = 4
		private const val DESC_MAX_WIDTH = 150

		private const val PANEL_X = 5
		private const val PANEL_Y = 10
		private const val PANEL_WIDTH = 80
		private const val PANEL_GAP = 5

	}

	override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
		val clickX = event.x.toInt()
		val clickY = event.y.toInt()

		for (col in categories.indices) {
			if (col !in openCategories) continue
			val panel = panels[col]
			if (clickX in panel.x..(panel.x + PANEL_WIDTH) && clickY in panel.y..(panel.y + TITLE_HEIGHT)) {
				panel.folded = !panel.folded
				return true
			}
			if (!panel.folded) {
				for (row in categories[col].entries.indices) {
					val top = rowY(panel.y, row)
					if (clickX in panel.x..(panel.x + PANEL_WIDTH) && clickY in top..(top + ROW_HEIGHT)) {
						val entry = categories[col].entries[row]
						entry.setter(!entry.getter())
						ModConfig.save()
						return true
					}
				}
			}
		}
		return super.mouseClicked(event, doubleClick)
	}

	init {
		buildCategories()
	}

	private fun buildCategories() {
		val cfg = ModConfig.get()

		val general = ConfigCategory("General")
			.toggle(
				"Rarity Backgrounds",
				cfg::showRarityBackgrounds,
				"Colors item backgrounds based on their rarity."
			)
			.toggle(
				"Inventory overlay",
				cfg::PlayerInventory,
				"Overlays your inventory's content outside the inventory."
			)
			.toggle(
				"Action Bar stats",
				cfg::hideActionBarStats,
				"Removes the stats in your action bar."
			)

		val dungeons = ConfigCategory("Dungeons")
			.toggle(
				"Mimic Chest",
				cfg::mimicChest,
				"Outlines the mimic chest inside Dungeons."
			)

		val mining = ConfigCategory("Mining")
			.toggle(
				"Ability Announcer",
				cfg::miningAbilityAnnouncer,
				"Shows a popup and sound when your pickaxe ability is ready."
			)
			.toggle(
				"Ping Glide",
				cfg::pingGlide,
				"Outlines your block and shows when it's safe to move without losing progress"
			)
			.toggle(
				"Chest Solver",
				cfg::powderChestSolver,
				"Highlights the exact point to look while opening PowderChests."
			)
			.toggle(
				"Nucleus Map",
				cfg::nucleusMap,
				"Shows a mini-map of Crystal Hollows."
			)
			.toggle(
				"Commission Display",
				cfg::commissionsDisplay,
				"Show your curent commissions and there progression."
			)
		val rift = ConfigCategory("Rift")
			.toggle(
				"Puff Kill Announcer",
				cfg::puffKillAnnouncer,
				"Shows when to kill your puffs."
			)

		categories.add(general)
		categories.add(dungeons)
		categories.add(mining)
		categories.add(rift)

		for (col in categories.indices) {
			panels.add(PanelState(x = PANEL_X + col * (PANEL_WIDTH + PANEL_GAP), PANEL_Y, false))

		}

		openCategories.addAll(categories.indices)
	}

	override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		context.fillGradient(0, 0, width, height, 0x60101010, 0x60151515)

		var currentHoverKey: String? = null
		var currentHoverDescription: String? = null
		var currentHoverX = 0
		var currentHoverY = 0

		for (col in categories.indices) {
			val category = categories[col]
			val panel = panels[col]

			if (col !in openCategories || panel.folded) continue

			for (row in categories[col].entries.indices) {
				val top = rowY(panel.y, row)

				if (mouseX in panel.x..(panel.x + PANEL_WIDTH) && mouseY in top..(top + ROW_HEIGHT)) {
					val entry = categories[col].entries[row]

					if (entry.description.isNotEmpty()) {
						currentHoverKey = category.name + ":" + entry.label
						currentHoverDescription = entry.description
						currentHoverX = panel.x + PANEL_WIDTH + 5
						currentHoverY = top
					}
				}
			}
		}

		if (currentHoverKey != hoveredKey) {
			hoveredKey = currentHoverKey
			hoverStartTime = System.currentTimeMillis()
		}

		super.extractRenderState(context, mouseX, mouseY, delta)

		for (col in categories.indices) {
				val panel = panels[col]
				TEMPCategoryPanel.draw(context, categories[col], panel.x, panel.y, PANEL_WIDTH, panel.folded, mouseX, mouseY)
		}

		if (currentHoverKey != null && currentHoverDescription != null) {
			val elapsed = System.currentTimeMillis() - hoverStartTime
			if (elapsed >= HOVER_DELAY_MS) {
				context.nextStratum()
				drawDescriptionBox(context, currentHoverDescription, currentHoverX, currentHoverY)
			}
		}
	}

	private fun drawDescriptionBox(context: GuiGraphicsExtractor, description: String, x: Int, y: Int) {
		val wrappedLines = this.font.split(Component.literal(description), DESC_MAX_WIDTH)

		var textWidth = 0
		for (line in wrappedLines) {
			val w = this.font.width(line)
			if (w > textWidth) textWidth = w
		}

		val lineHeight = 9
		val textHeight = wrappedLines.size * lineHeight

		val boxWidth = textWidth + DESC_PADDING * 2
		val boxHeight = textHeight + DESC_PADDING * 2

		val boxX = if (x + boxWidth > width) (width - boxWidth).coerceAtLeast(0) else x
		val boxY = y

		context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, DESC_BG_COLOR)
		context.fill(boxX, boxY, boxX + boxWidth, boxY + 1, DESC_BORDER_COLOR)
		context.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, DESC_BORDER_COLOR)
		context.fill(boxX, boxY, boxX + 1, boxY + boxHeight, DESC_BORDER_COLOR)
		context.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, DESC_BORDER_COLOR)

		var lineY = boxY + DESC_PADDING
		for (line in wrappedLines) {
			context.text(this.font, line, boxX + DESC_PADDING, lineY, DESC_TEXT_COLOR, false)
			lineY += lineHeight
		}
	}

	override fun isPauseScreen(): Boolean = false
}
class PanelState(var x: Int, var y: Int, var folded: Boolean)