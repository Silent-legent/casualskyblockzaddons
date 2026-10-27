package com.cbza.net.feature.general

import com.cbza.net.config.ModConfig
import com.cbza.net.utility.ColorCatalog
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.ChatScreen

object InventoryHudOverlay {

    val SLOT_SIZE = 20
    val COLUMS = 9
    val ROWS = 3
    val MARGIN = 4

    private fun isInGameUi(): Boolean {
        val mc = Minecraft.getInstance()
        return mc.level != null && (mc.gui.screen() == null || mc.gui.screen() is ChatScreen)
    }

    fun getDefaultPosition(): Pair<Int, Int> {
        val mc = Minecraft.getInstance()
        val cfg = ModConfig.get()

        val screenWidth = mc.window.guiScaledWidth
        val screenHeight = mc.window.guiScaledHeight
        val scale = cfg.playerInventoryDisplayScale

        val x = if (cfg.playerInventoryDisplayX == -1)
            (screenWidth / 2 - ((COLUMS * SLOT_SIZE + MARGIN) * scale) / 2).toInt()
        else cfg.playerInventoryDisplayX

        val y = if (cfg.playerInventoryDisplayY == -1)
            (screenHeight - 40 - (ROWS * SLOT_SIZE + MARGIN) * scale).toInt()
        else cfg.playerInventoryDisplayY

        return Pair(x, y)
    }

    fun render(graphics: GuiGraphicsExtractor) {
        if (!ModConfig.get().PlayerInventory) return
        if (!isInGameUi()) return

        val mc = Minecraft.getInstance()
        val cfg = ModConfig.get()
        val player = mc.player ?: return

        val scale = cfg.playerInventoryDisplayScale

        val (x, y) = getDefaultPosition()

        graphics.pose().pushMatrix()
        graphics.pose().translate(x.toFloat(), y.toFloat())
        graphics.pose().scale(scale, scale)

        // todo: add option to change background to dif color * Isue for a future update.
        graphics.fill(0, 0, COLUMS * SLOT_SIZE + MARGIN, ROWS * SLOT_SIZE + MARGIN, ColorCatalog.TRANSLUCENT_BLACK)

        for (row in 0 until ROWS) {
            for (col in 0 until COLUMS) {
                val slotIndex = 9 + (row * COLUMS) + col
                val stack = player.inventory.getItem(slotIndex)

                val slotX = col * SLOT_SIZE + MARGIN
                val slotY = row * SLOT_SIZE + MARGIN

                graphics.item(stack, slotX, slotY, 0)
                graphics.itemDecorations(mc.font, stack, slotX, slotY)
            }
        }

        graphics.pose().popMatrix()
    }
}