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

	companion object {
		private const val COLUMN_WIDTH = 100
		private const val ROW_HEIGHT = 14
		private const val COLUMN_GAP = 10
		private const val TOP_MARGIN = 10
		private const val LEFT_MARGIN = 10
		private const val COLOR_ENABLED_BOX: Int = 0xFF32CD32.toInt()
		private const val COLOR_ENABLED_BORDER: Int = 0xFF1E8C1E.toInt()
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
				{ value -> cfg.showRarityBackgrounds = value }
			)

		val mining = ConfigCategory("Mining")
			.toggle(
				"Ability Announcer",
				{ cfg.MiningAbilityAnnouncer },
				{ value -> cfg.MiningAbilityAnnouncer = value }
			)
			.toggle(
				"Ping Glide",
				{ cfg.PingGlide },
				{ value -> cfg.PingGlide = value }
			)
			.toggle(
				"Chest Solver",
				{ cfg.PowderChestSolver },
				{ value -> cfg.PowderChestSolver = value }
			)
			.toggle(
				"Nucleus Map",
				{ cfg.NucleusMap },
				{ value -> cfg.NucleusMap = value }
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
				}
			}
		}

		super.extractRenderState(context, mouseX, mouseY, delta)
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