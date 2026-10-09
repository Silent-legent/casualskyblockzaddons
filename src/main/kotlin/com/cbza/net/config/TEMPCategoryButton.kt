package com.cbza.net.config

import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.rendering.TEMPRender2D
import com.cbza.net.utility.rendering.TEMPRender2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object TEMPCategoryButton {

    fun draw(ctx: GuiGraphicsExtractor,label: String, x: Int, y: Int, width: Int, height: Int, selected: Boolean, mouseX: Int, mouseY: Int) {

        val hovered = (mouseX in x..(x + width) && mouseY in y..(y + height))
        val bg = when {
            selected && hovered -> ColorCatalog.SELECTED_HOVER1
            selected -> ColorCatalog.SELECTED1
            hovered -> ColorCatalog.UNSELECTED_HOVER1
            else -> ColorCatalog.UNSELECTED1
            }

        TEMPRender2D.drawRect(ctx, x, y, width, height, Color(bg, true))

        if (selected) {
            TEMPRender2D.drawRect(ctx, x, y, 3, height, Color(ColorCatalog.LINECOLOR1, true))
        }

                val lineHeight = 6f
                val scale = height / 19f

                val scaledLineHeight = lineHeight * scale
                val textY = y + (height - scaledLineHeight) / 2

                val scaledTextWidth = label.width() * scale
                val textX = (x + 2) + ((width - 2) - scaledTextWidth) / 2

                TEMPRender2D.drawString(ctx, label, (textX / scale).toInt(), (textY / scale).toInt(), color = Color.WHITE, scale = scale)

    }
}