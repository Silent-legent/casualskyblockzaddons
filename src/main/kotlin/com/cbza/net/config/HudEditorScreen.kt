package com.cbza.net.config

import com.cbza.net.feature.NucleusMap
import com.cbza.net.utility.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB

class HudEditorScreen : Screen(Component.literal("HUD Editor")) {

    private val textureId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "nucleus_map")
    private val arrowId = Identifier.fromNamespaceAndPath("casualskyblockzaddons", "player_arrow")

    private var draggingElement: String? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    override fun isPauseScreen(): Boolean = false

    override fun init() {
        super.init()
        val resetButton = Button.builder(Component.literal("Reset Positions")) {
            val cfg = ModConfig.get()
            cfg.abilityAnnouncerX = -1
            cfg.abilityAnnouncerY = -1
            cfg.abilityAnnouncerScale = 3.5f
            cfg.nucleusMapX = 10
            cfg.nucleusMapY = 10
            cfg.nucleusMapScale = 1.0f
            ModConfig.save()
        }.bounds(width / 2 - 50, height - 30, 100, 20).build()
        addRenderableWidget(resetButton)
    }

    override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0x90101010.toInt(), 0x90151515.toInt())

        val cfg = ModConfig.get()

        if (cfg.MiningAbilityAnnouncer) {
            val text = "Maniac Miner Ready!"
            val scale = cfg.abilityAnnouncerScale
            val x = if (cfg.abilityAnnouncerX == -1) (width - this.font.width(text) * scale).toInt() / 2 else cfg.abilityAnnouncerX
            val y = if (cfg.abilityAnnouncerY == -1) height / 3 else cfg.abilityAnnouncerY

            drawEditorBox(context, x, y, (this.font.width(text) * scale).toInt(), (10 * scale).toInt())

            context.pose().pushMatrix()
            context.pose().scale(scale, scale)
            context.text(this.font, text, (x / scale).toInt(), (y / scale).toInt(), ARGB.opaque(0x55FF55), true)
            context.pose().popMatrix()
        }

        if (cfg.NucleusMap) {
            val mapSize = (100 * cfg.nucleusMapScale).toInt()
            val x = cfg.nucleusMapX
            val y = cfg.nucleusMapY

            drawEditorBox(context, x, y, mapSize, mapSize)

            Render2D.drawImage(context, textureId, x, y, mapSize, mapSize)

            // fake POI dots using real colors and real sizes scaled with the map
            val fakePoiOrder = listOf("Jungle Temple", "Mines of Divan", "Goblin Queen's Den")
            val fakeOffsets = listOf(Pair(0.25, 0.25), Pair(0.75, 0.25), Pair(0.25, 0.75))
            for (i in fakePoiOrder.indices) {
                val name = fakePoiOrder[i]
                val color = NucleusMap.poiColors[name] ?: continue
                val size = ((NucleusMap.poiSizes[name] ?: 6) * cfg.nucleusMapScale).toInt()
                val (offX, offY) = fakeOffsets[i]
                val px = x + (mapSize * offX).toInt()
                val py = y + (mapSize * offY).toInt()
                context.fill(px - size / 2, py - size / 2, px + size / 2, py + size / 2, color)
            }

            val centerX = x + mapSize / 2
            val centerY = y + mapSize / 2
            val arrowSize = (9 * cfg.nucleusMapScale).toInt().coerceAtLeast(3)
            context.pose().pushMatrix()
            context.pose().translate(centerX.toFloat(), centerY.toFloat())
            context.pose().translate(-(arrowSize / 2).toFloat(), -(arrowSize / 2).toFloat())
            Render2D.drawImage(context, arrowId, 0, 0, arrowSize, arrowSize)
            context.pose().popMatrix()
        }

        context.text(this.font, "Drag to move, scroll to resize. Press ESC to close and save.", 10, height - 40, 0xFFFFFFFF.toInt(), true)

        super.extractRenderState(context, mouseX, mouseY, delta)
    }

    private fun drawEditorBox(context: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        context.fill(x - 1, y - 1, x + w + 1, y, 0x80FFFFFF.toInt())
        context.fill(x - 1, y + h, x + w + 1, y + h + 1, 0x80FFFFFF.toInt())
        context.fill(x - 1, y, x, y + h, 0x80FFFFFF.toInt())
        context.fill(x + w, y, x + w + 1, y + h, 0x80FFFFFF.toInt())
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        val cfg = ModConfig.get()
        val mx = event.x().toInt()
        val my = event.y().toInt()

        val mapSize = (100 * cfg.nucleusMapScale).toInt()
        if (mx in cfg.nucleusMapX..(cfg.nucleusMapX + mapSize) && my in cfg.nucleusMapY..(cfg.nucleusMapY + mapSize)) {
            draggingElement = "nucleus_map"
            dragOffsetX = mx - cfg.nucleusMapX
            dragOffsetY = my - cfg.nucleusMapY
            return true
        }

        val text = "Maniac Miner Ready!"
        val scale = cfg.abilityAnnouncerScale
        val aw = (this.font.width(text) * scale).toInt()
        val ah = (10 * scale).toInt()
        val ax = if (cfg.abilityAnnouncerX == -1) (width - aw) / 2 else cfg.abilityAnnouncerX
        val ay = if (cfg.abilityAnnouncerY == -1) height / 3 else cfg.abilityAnnouncerY
        if (mx in ax..(ax + aw) && my in ay..(ay + ah)) {
            draggingElement = "ability_announcer"
            dragOffsetX = mx - ax
            dragOffsetY = my - ay
            cfg.abilityAnnouncerX = ax
            cfg.abilityAnnouncerY = ay
            return true
        }

        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        val cfg = ModConfig.get()
        val mx = event.x().toInt()
        val my = event.y().toInt()
        when (draggingElement) {
            "nucleus_map" -> {
                cfg.nucleusMapX = mx - dragOffsetX
                cfg.nucleusMapY = my - dragOffsetY
            }
            "ability_announcer" -> {
                cfg.abilityAnnouncerX = mx - dragOffsetX
                cfg.abilityAnnouncerY = my - dragOffsetY
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

        val mapSize = (100 * cfg.nucleusMapScale).toInt()
        if (mx in cfg.nucleusMapX..(cfg.nucleusMapX + mapSize) && my in cfg.nucleusMapY..(cfg.nucleusMapY + mapSize)) {
            cfg.nucleusMapScale = (cfg.nucleusMapScale + scrollY.toFloat() * 0.1f).coerceIn(0.3f, 3.0f)
            return true
        }

        val text = "Maniac Miner Ready!"
        val scale = cfg.abilityAnnouncerScale
        val aw = (this.font.width(text) * scale).toInt()
        val ah = (10 * scale).toInt()
        val ax = if (cfg.abilityAnnouncerX == -1) (width - aw) / 2 else cfg.abilityAnnouncerX
        val ay = if (cfg.abilityAnnouncerY == -1) height / 3 else cfg.abilityAnnouncerY
        if (mx in ax..(ax + aw) && my in ay..(ay + ah)) {
            cfg.abilityAnnouncerScale = (cfg.abilityAnnouncerScale + scrollY.toFloat() * 0.1f).coerceIn(1.0f, 6.0f)
            return true
        }

        return false
    }

    override fun onClose() {
        ModConfig.save()
        super.onClose()
    }
}