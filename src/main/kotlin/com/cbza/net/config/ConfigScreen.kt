package com.cbza.net.config

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import java.util.ArrayList
import java.util.HashMap

class ConfigScreen(parent: Screen?) : Screen(Component.literal("Casual Skyblock Addons")) {

	private val categories = ArrayList<ConfigCategory>()
	private val collapsed = HashMap<String, Boolean>()

	// hover-delay tooltip tracking
	private var hoveredKey: String? = null
	private var hoverStartTime: Long = 0L

	companion object {
		private const val COLUMN_WIDTH = 100
		private const val ROW_HEIGHT = 14
		private const val COLUMN_GAP = 10
		private const val TOP_MARGIN = 10
		private const val LEFT_MARGIN = 10
		private const val COLOR_ENABLED_BOX: Int = 0xFF32CD32.toInt()
		private const val COLOR_ENABLED_BORDER: Int = 0xFF1E8C1E.toInt()
		private const val HOVER_DELAY_MS = 1000L
		private const val DESC_BG_COLOR: Int = 0xE0101010.toInt()
		private const val DESC_BORDER_COLOR: Int = 0xFF555555.toInt()
		private const val DESC_TEXT_COLOR: Int = 0xFFFFFFFF.toInt()
		private const val DESC_PADDING = 4
		private const val DESC_MAX_WIDTH = 150
	}

	init {
		buildCategories()
	}

	private fun buildCategories() {
		val cfg = ModConfig.get()

		val general = ConfigCategory("General")
			.toggle(
				"Rarity Backgrounds",
				{ cfg.showRarityBackgrounds },
				{ value -> cfg.showRarityBackgrounds = value },
				"Colors item backgrounds based on their rarity."
			)

		val mining = ConfigCategory("Mining")
			.toggle(
				"Ability Announcer",
				{ cfg.MiningAbilityAnnouncer },
				{ value -> cfg.MiningAbilityAnnouncer = value },
				"Shows a popup and sound when your pickaxe ability is ready."
			)
			.toggle(
				"Ping Glide",
				{ cfg.PingGlide },
				{ value -> cfg.PingGlide = value },
				"Outlines your block and shows when it's safe to move without losing progress"
			)
			.toggle(
				"Chest Solver",
				{ cfg.PowderChestSolver },
				{ value -> cfg.PowderChestSolver = value },
				"Highlights the exact point to look while opening PowderChests."
			)
			.toggle(
				"Nucleus Map",
				{ cfg.NucleusMap },
				{ value -> cfg.NucleusMap = value },
				"Shows a mini-map of Crystal Hollows."
			)

		categories.add(general)
		categories.add(mining)

		for (cat in categories) {
			collapsed[cat.name] = false
		}
	}

	private fun getButtonText(entry: ConfigCategory.ToggleEntry): Component {
		val value = entry.getter.asBoolean
		val suffix = if (value) " [ON]" else " [OFF]"
		return Component.literal(entry.label + suffix)
	}

	override fun init() {
		super.init()
		for (col in categories.indices) {
			val category = categories[col]
			val x = LEFT_MARGIN + col * (COLUMN_WIDTH + COLUMN_GAP)
			val isCollapsed = collapsed.getOrDefault(category.name, false)
			val catName = category.name

			val header = Button.builder(
				Component.literal(category.name)
			) { _ ->
				collapsed[catName] = !collapsed.getOrDefault(catName, false)
				rebuildWidgets()
			}.bounds(x, TOP_MARGIN, COLUMN_WIDTH, ROW_HEIGHT - 2).build()
			addRenderableWidget(header)

			if (!isCollapsed) {
				for (row in category.entries.indices) {
					val entry = category.entries[row]
					val y = TOP_MARGIN + 20 + row * (ROW_HEIGHT + 4)
					val button = Button.builder(
						getButtonText(entry)
					) { btn ->
						val newValue = !entry.getter.asBoolean
						entry.setter.accept(newValue)
						ModConfig.save()
						btn.message = getButtonText(entry)
					}.bounds(x, y, COLUMN_WIDTH, ROW_HEIGHT - 2).build()

					addRenderableWidget(button)
				}
			}
		}
	}

	override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
		context.fillGradient(0, 0, width, height, 0x60101010, 0x60151515)

		var currentHoverKey: String? = null
		var currentHoverDescription: String? = null
		var currentHoverX = 0
		var currentHoverY = 0

		for (col in categories.indices) {
			val category = categories[col]
			val x = LEFT_MARGIN + col * (COLUMN_WIDTH + COLUMN_GAP)
			val isCollapsed = collapsed.getOrDefault(category.name, false)

			if (!isCollapsed) {
				for (row in category.entries.indices) {
					val entry = category.entries[row]
					val y = TOP_MARGIN + 20 + row * (ROW_HEIGHT + 4)
					val isEnabled = entry.getter.asBoolean
					if (isEnabled) {
						renderColoredButtonBox(context, x, y, COLUMN_WIDTH, ROW_HEIGHT - 2, COLOR_ENABLED_BOX, COLOR_ENABLED_BORDER)
					}

					if (entry.description.isNotEmpty() &&
						mouseX in x..(x + COLUMN_WIDTH) &&
						mouseY in y..(y + ROW_HEIGHT)
					) {
						currentHoverKey = category.name + ":" + entry.label
						currentHoverDescription = entry.description
						currentHoverX = x + 110
						currentHoverY = y + ROW_HEIGHT + -15
					}
				}
			}
		}

		if (currentHoverKey != hoveredKey) {
			hoveredKey = currentHoverKey
			hoverStartTime = System.currentTimeMillis()
		}

		// draw all real widgets (buttons, etc) FIRST
		super.extractRenderState(context, mouseX, mouseY, delta)

		// THEN draw our description box, on its own top layer, so it always renders above everything else
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

	private fun renderColoredButtonBox(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, fillColor: Int, borderColor: Int) {
		val outerX = x - 1
		val outerY = y - 1
		val outerX2 = x + width + 1
		val outerY2 = y + height + 1
		context.fillGradient(outerX, outerY, outerX2, outerY2, fillColor, fillColor)
		context.fill(outerX - 1, outerY - 1, outerX2 + 1, outerY, borderColor)
		context.fill(outerX - 1, outerY2, outerX2 + 1, outerY2 + 1, borderColor)
		context.fill(outerX - 1, outerY, outerX, outerY2, borderColor)
		context.fill(outerX2, outerY, outerX2 + 1, outerY2, borderColor)
	}

	override fun isPauseScreen(): Boolean = false
}