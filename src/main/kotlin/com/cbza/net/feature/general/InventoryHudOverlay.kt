package com.cbza.net.feature.general

import com.cbza.net.config.ModConfig
import com.cbza.net.config.config
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
        //? if >=26.2 {
        //return mc.level != null && (mc.gui.screen() == null || mc.gui.screen() is ChatScreen)
        //?} else {
        return mc.level != null && (mc.screen == null || mc.screen is ChatScreen)
        //?}
    }

    fun getDefaultPosition(): Pair<Int, Int> {
        val mc = Minecraft.getInstance()

        val screenWidth = mc.window.guiScaledWidth
        val screenHeight = mc.window.guiScaledHeight
        val scale = config.general.playerInventoryPos.scale

        val x = if (config.general.playerInventoryPos.x == -1)
            (screenWidth / 2 - ((COLUMS * SLOT_SIZE + MARGIN) * scale) / 2).toInt()
        else config.general.playerInventoryPos.x

        val y = if (config.general.playerInventoryPos.y == -1)
            (screenHeight - 40 - (ROWS * SLOT_SIZE + MARGIN) * scale).toInt()
        else config.general.playerInventoryPos.y

        return Pair(x, y)
    }

    fun render(graphics: GuiGraphicsExtractor) {
        if (!ModConfig.get().general.playerInventory) return
        if (!isInGameUi()) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val scale = config.general.playerInventoryPos.scale

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