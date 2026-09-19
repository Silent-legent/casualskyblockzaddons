package com.cbza.net.config

import com.cbza.net.feature.general.InventoryHudOverlay
import com.cbza.net.feature.general.InventoryHudOverlay.COLUMS
import com.cbza.net.feature.general.InventoryHudOverlay.ROWS
import com.cbza.net.feature.general.InventoryHudOverlay.SLOT_SIZE
import com.cbza.net.feature.general.InventoryHudOverlay.MARGIN

import com.cbza.net.feature.mining.general.CommissionsDisplay
import com.cbza.net.feature.mining.hollows.map.NucleusMap
import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.rendering.Render2D
import net.minecraft.client.Minecraft

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class HudEditorScreen : Screen(Component.literal("HUD Editor")) {

    val mc = Minecraft.getInstance()

    private data class HudBounds(val x: Int, val y: Int, val w: Int, val h: Int) {
        fun contains(mx: Int, my: Int): Boolean = mx in x..(x + w) && my in y..(y + h)
    }

    private fun getBounds(name: String): HudBounds? {
        val cfg = ModConfig.get()
        return when (name) {
            // --- Gnereal ---
            "player_inventory" -> {
                val scale = cfg.playerInventoryDisplayScale
                val inventorysizeWidth = ((COLUMS * SLOT_SIZE + MARGIN) * scale).toInt()
                val inventorySizeHight = ((ROWS * SLOT_SIZE + MARGIN) * scale).toInt()
                val (x, y) = InventoryHudOverlay.getDefaultPosition()
                HudBounds(x, y, inventorysizeWidth, inventorySizeHight)
            }
            // --- Mining ---
            "nucleus_map" -> {
                val mapSize = (100 * cfg.nucleusMapScale).toInt()
                HudBounds(cfg.nucleusMapX, cfg.nucleusMapY, mapSize, mapSize)
            }
            "ability_announcer" -> {
                val text = "Maniac Miner Ready!"
                val scale = cfg.abilityAnnouncerScale
                val w = (this.font.width(text) * scale).toInt()
                val h = (10 * scale).toInt()
                val x = if (cfg.abilityAnnouncerX == -1) (width - w) / 2 else cfg.abilityAnnouncerX
                val y = if (cfg.abilityAnnouncerY == -1) height / 3 else cfg.abilityAnnouncerY
                HudBounds(x, y, w, h)
            }
            "commission_display" -> {
                val fakeCommissions = listOf(
                    "Sludge slayer: 74.5% ",
                    "Yog slayer: 25%",
                    "Boss corleon slayer: DONE",
                    "Amber Crystal Hunter: 0%"
                )
                val scale = cfg.commissionsDisplayScale
                val lineHeight = 10
                val w = (fakeCommissions.maxOf { this.font.width(it) } * scale).toInt()
                val h = (fakeCommissions.size * lineHeight * scale).toInt()
                val x = if (cfg.commissionsDisplayX == -1) (width - w) / 2 else cfg.commissionsDisplayX
                val y = if (cfg.commissionsDisplayY == -1) height / 3 else cfg.commissionsDisplayY
                HudBounds(x, y, w, h)
            }
            // --- Rift ---
            "puff_kill_announcer" -> {
                val text = "Kill Puffs!"
                val scale = cfg.puffKillAnnouncerScale
                val w = (this.font.width(text) * scale).toInt()
                val h = (10 * scale).toInt()
                val x = if (cfg.puffKillAnnouncerX == -1) (width - w) / 2 else cfg.puffKillAnnouncerX
                val y = if (cfg.puffKillAnnouncerY == -1) height / 3 else cfg.puffKillAnnouncerY
                HudBounds(x, y, w, h)
            }
            else -> null
        }
    }

    private val textureId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "nucleus_map")
    private val arrowId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "player_arrow")

    private var lastMouseX = 0
    private var lastMouseY = 0

    private var draggingElement: String? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun isPauseScreen(): Boolean = false

    override fun init() {
        super.init()
        val resetButton = Button.builder(Component.literal("Reset Positions")) {
            val cfg = ModConfig.get()

            cfg.hudLayerOrder = mutableListOf("commission_display", "puff_kill_announcer", "ability_announcer", "nucleus_map", "player_inventory")

            // --- General ---
            cfg.playerInventoryDisplayX = -1
            cfg.playerInventoryDisplayY = -1
            cfg.playerInventoryDisplayScale = 1.0f

            // --- Mining ---
            cfg.nucleusMapX = 0
            cfg.nucleusMapY = 0
            cfg.nucleusMapScale = 1.0f

            cfg.abilityAnnouncerX = -1
            cfg.abilityAnnouncerY = -1
            cfg.abilityAnnouncerScale = 3.5f

            cfg.commissionsDisplayX = 0
            cfg.commissionsDisplayY = 100
            cfg.commissionsDisplayScale = 1.0f

            // --- Rift ---
            cfg.puffKillAnnouncerX = -1
            cfg.puffKillAnnouncerY = -1
            cfg.puffKillAnnouncerScale = 3.5f

            ModConfig.save()
        }.bounds(width / 2 - 50, height - 30, 100, 20).build()
        addRenderableWidget(resetButton)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0x90101010.toInt(), 0x90151515.toInt())

        lastMouseX = mouseX
        lastMouseY = mouseY

        val cfg = ModConfig.get()

        for (name in cfg.hudLayerOrder) {
            val bounds = getBounds(name) ?: continue

            when (name) {
                // --- General ---
                "player_inventory" -> {
                    if (cfg.PlayerInventory) {
                        val player = Minecraft.getInstance().player ?: continue
                        val scale = cfg.playerInventoryDisplayScale

                        drawEditorBox(context, bounds.x, bounds.y, bounds.w, bounds.h)

                        context.pose().pushMatrix()
                        context.pose().translate(bounds.x.toFloat(), bounds.y.toFloat())
                        context.pose().scale(scale, scale)

                        context.fill(0, 0, COLUMS * SLOT_SIZE + MARGIN, ROWS * SLOT_SIZE + MARGIN, ColorCatalog.TRANSLUCENT_BLACK)

                        for (row in 0 until ROWS) {
                            for (col in 0 until COLUMS) {
                                val slotIndex = 9 + (row * COLUMS) + col
                                val stack = player.inventory.getItem(slotIndex)

                                val slotX = col * SLOT_SIZE + MARGIN
                                val slotY = row * SLOT_SIZE + MARGIN

                                context.item(stack, slotX, slotY, 0)
                                context.itemDecorations(this.font, stack, slotX, slotY)
                            }
                        }

                        context.pose().popMatrix()
                    }
                }
                // Inventory Display alr rendered.

                // --- Mining ---
                "nucleus_map" -> {
                    if (cfg.nucleusMap) {
                        drawEditorBox(context, bounds.x, bounds.y, bounds.w, bounds.h)

                        Render2D.drawImage(context, textureId, bounds.x, bounds.y, bounds.w, bounds.h)

                        // Fake POI dots using real colors and real sizes scaled with the map
                        val fakePoiOrder = listOf("Jungle Temple", "Mines of Divan", "Goblin Queen's Den")
                        val fakeOffsets = listOf(Pair(0.25, 0.25), Pair(0.75, 0.25), Pair(0.25, 0.75))
                        for (i in fakePoiOrder.indices) {
                            val poiName = fakePoiOrder[i]
                            val color = NucleusMap.poiColors[poiName] ?: continue
                            val size = ((NucleusMap.poiSizes[poiName] ?: 6) * cfg.nucleusMapScale).toInt()
                            val (offX, offY) = fakeOffsets[i]
                            val px = bounds.x + (bounds.w * offX).toInt()
                            val py = bounds.y + (bounds.h * offY).toInt()
                            context.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, color)
                        }

                        val centerX = bounds.x + bounds.w / 2
                        val centerY = bounds.y + bounds.h / 2
                        val arrowSize = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)
                        context.pose().pushMatrix()
                        context.pose().translate(centerX.toFloat(), centerY.toFloat())
                        context.pose().translate(-(arrowSize / 2).toFloat(), -(arrowSize / 2).toFloat())
                        Render2D.drawImage(context, arrowId, 0, 0, arrowSize, arrowSize)
                        context.pose().popMatrix()
                    }
                }
                "ability_announcer" -> {
                    if (cfg.miningAbilityAnnouncer) {
                        val text = "Maniac Miner Ready!"
                        val scale = cfg.abilityAnnouncerScale

                        drawEditorBox(context, bounds.x, bounds.y, bounds.w, bounds.h)

                        context.pose().pushMatrix()
                        context.pose().scale(scale, scale)
                        context.text(this.font, text, (bounds.x / scale).toInt(), (bounds.y / scale).toInt(), ARGB.opaque(ColorCatalog.GREEN), true)
                        context.pose().popMatrix()
                    }
                }
                "commission_display" -> {
                    if (cfg.commissionsDisplay) {
                        val fakeCommissions = listOf(
                            "Sludge slayer: 74.5% ",
                            "Yog slayer: 25%",
                            "Boss corleon slayer: DONE",
                            "Amber Crystal Hunter: 0%"
                        )
                        val scale = cfg.commissionsDisplayScale
                        val lineHeight = 10
                        val nameColor = ColorCatalog.WHITE

                        drawEditorBox(context, bounds.x, bounds.y, bounds.w, bounds.h)

                        context.pose().pushMatrix()
                        context.pose().scale(scale, scale)

                        val ux = (bounds.x / scale).toInt()
                        val uy = (bounds.y / scale).toInt()

                        for ((index, line) in fakeCommissions.withIndex()) {
                            val match = CommissionsDisplay.commissionPattern.find(line) ?: continue
                            val lineName = match.groupValues[1]
                            val value = match.groupValues[2]

                            val lineY = uy + (index * lineHeight)
                            val namePrefix = "$lineName: "

                            context.text(this.font, namePrefix, ux, lineY, nameColor, true)
                            val valueX = ux + this.font.width(namePrefix)
                            val progress: Float = if (value == "DONE") {
                                1f
                            } else {
                                val number = value.removeSuffix("%").toFloatOrNull() ?: 0f
                                number / 100f
                            }
                            val valueColor = ColorCatalog.lerpColor(ColorCatalog.RED, ColorCatalog.GREEN, progress)
                            context.text(this.font, value, valueX, lineY, valueColor, true)
                        }
                        context.pose().popMatrix()
                    }
                }
                // --- Rift ---
                "puff_kill_announcer" -> {
                    if (cfg.puffKillAnnouncer) {
                        val text = "Kill Puff!"
                        val scale = cfg.puffKillAnnouncerScale

                        drawEditorBox(context, bounds.x, bounds.y, bounds.w, bounds.h)

                        context.pose().pushMatrix()
                        context.pose().scale(scale, scale)
                        context.text(this.font, text, (bounds.x / scale).toInt(), (bounds.y / scale).toInt(), ARGB.opaque(ColorCatalog.RED), true)
                        context.pose().popMatrix()
                    }
                }
            }
        }

        context.text(this.font, "Drag to move, scroll to resize. Hover + Shift/Ctrl to reorder layers. ESC to close and save.", 10, height - 40, 0xFFFFFFFF.toInt(), true)

        super.extractRenderState(context, mouseX, mouseY, delta)
    }

    private fun elementAt(mx: Int, my: Int): String? {
        return HudLayerManager.hitOrder().firstOrNull { name ->
            getBounds(name)?.contains(mx, my) == true
        }
    }

    private fun drawEditorBox(context: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        context.fill(x - 1, y - 1, x + w + 1, y, 0x80FFFFFF.toInt())
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, 0x80FFFFFF.toInt())
        context.fill(x - 1, y, x, y + h, 0x80FFFFFF.toInt())
        context.fill(x + w, y, x + w + 1, y + h, 0x80FFFFFF.toInt())
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        val target = draggingElement ?: elementAt(lastMouseX, lastMouseY)

        if (target != null) {
            if (event.hasShiftDown()) {
                HudLayerManager.moveLayer(target, up = true)
            } else if (event.hasControlDown()) {
                HudLayerManager.moveLayer(target, up = false)
            }
        }

        return super.keyPressed(event)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val cfg = ModConfig.get()
        val mx = event.x().toInt()
        val my = event.y().toInt()

        for (name in HudLayerManager.hitOrder()) {
            val bounds = getBounds(name) ?: continue
            if (bounds.contains(mx, my)) {
                draggingElement = name
                dragOffsetX = mx - bounds.x
                dragOffsetY = my - bounds.y

                when (name) {
                    "player_inventory" -> { cfg.playerInventoryDisplayX = bounds.x; cfg.playerInventoryDisplayY = bounds.y }
                    "nucleus_map" -> { cfg.nucleusMapX = bounds.x; cfg.nucleusMapY = bounds.y }
                    "ability_announcer" -> { cfg.abilityAnnouncerX = bounds.x; cfg.abilityAnnouncerY = bounds.y }
                    "puff_kill_announcer" -> { cfg.puffKillAnnouncerX = bounds.x; cfg.puffKillAnnouncerY = bounds.y }
                    "commission_display" -> { cfg.commissionsDisplayX = bounds.x; cfg.commissionsDisplayY = bounds.y }

                }
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val cfg = ModConfig.get()
        val mx = event.x().toInt()
        val my = event.y().toInt()
        when (draggingElement) {
            "player_inventory" -> {
                cfg.playerInventoryDisplayX = mx - dragOffsetX
                cfg.playerInventoryDisplayY = my - dragOffsetY
            }
            "nucleus_map" -> {
                cfg.nucleusMapX = mx - dragOffsetX
                cfg.nucleusMapY = my - dragOffsetY
            }
            "ability_announcer" -> {
                cfg.abilityAnnouncerX = mx - dragOffsetX
                cfg.abilityAnnouncerY = my - dragOffsetY
            }
            "puff_kill_announcer" -> {
                cfg.puffKillAnnouncerX = mx - dragOffsetX
                cfg.puffKillAnnouncerY = my - dragOffsetY
            }
            "commission_display" -> {
                cfg.commissionsDisplayX = mx - dragOffsetX
                cfg.commissionsDisplayY = my - dragOffsetY
            }
        }
        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        draggingElement = null
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val cfg = ModConfig.get()
        val mx = mouseX.toInt()
        val my = mouseY.toInt()

        val target = elementAt(mx, my) ?: return false
        val delta = scrollY.toFloat() * 0.1f

        when (target) {
            "nucleus_map" -> cfg.nucleusMapScale = (cfg.nucleusMapScale + delta).coerceIn(0.3f, 3.0f)
            "ability_announcer" -> cfg.abilityAnnouncerScale = (cfg.abilityAnnouncerScale + delta).coerceIn(1.0f, 6.0f)
            "puff_kill_announcer" -> cfg.puffKillAnnouncerScale = (cfg.puffKillAnnouncerScale + delta).coerceIn(1.0f, 6.0f)
            "commission_display" -> cfg.commissionsDisplayScale = (cfg.commissionsDisplayScale + delta).coerceIn(1.0f, 6.0f)
            "player_inventory" -> cfg.playerInventoryDisplayScale = (cfg.playerInventoryDisplayScale + delta).coerceIn(0.5f, 6.0f)
        }
        return true
    }

    override fun onClose() {
        ModConfig.save()
        super.onClose()
    }
}