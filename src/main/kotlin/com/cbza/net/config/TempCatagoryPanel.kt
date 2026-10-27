package com.cbza.net.config

import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.rendering.TEMPRender2D
import com.cbza.net.utility.rendering.TEMPRender2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object TEMPCategoryPanel {

    fun rowY(panelY: Int, row: Int, titleH: Int, rowH: Int): Int =
        panelY + titleH + row * rowH

    fun draw(ctx: GuiGraphicsExtractor,category: ConfigCategory,x: Int, y: Int, width: Int, titleH: Int, rowH: Int, folded: Boolean, mouseX: Int, mouseY: Int, active: Boolean) {

        val titleHovered = active && mouseX in x..(x + width) && mouseY in y..(y + titleH)
        val titleColor = if (titleHovered) ColorCatalog.UNSELECTED_HOVER1 else ColorCatalog.UNSELECTED1

        TEMPRender2D.drawRect(ctx, x, y, width, titleH, Color(titleColor, true))

        for (row in category.entries.indices) {
            if (!folded) {
                val entry = category.entries[row]
                val top = rowY(y, row, titleH, rowH)
                val enabled = entry.getter()

                val hovered = active && mouseX in x..(x + width) && mouseY in top..(top + rowH)
                val color = when {
                    enabled && hovered -> ColorCatalog.SELECTED_HOVER1
                    enabled -> ColorCatalog.SELECTED1
                    hovered -> ColorCatalog.UNSELECTED_HOVER1
                    else -> ColorCatalog.UNSELECTED1
                }

                TEMPRender2D.drawRect(ctx, x, top, width, rowH, Color(color, true))

                if (enabled) {
                    TEMPRender2D.drawRect(ctx, x, top, 3, rowH, Color(ColorCatalog.LINECOLOR1, true))
                }
                val lineHeight = 6f
                val scale = rowH / 15f //19

                val scaledLineHeight = lineHeight * scale
                val textY = rowY(y, row, titleH, rowH) + (rowH - scaledLineHeight) / 2

                val scaledTextWidth = entry.label.width() * scale
                val textX = (x) + ((width) - scaledTextWidth) / 2
                TEMPRender2D.drawString(ctx, entry.label, (textX / scale).toInt(), (textY / scale).toInt(), color = Color.WHITE, scale = scale)
            }
        }

        val lineHeight = 6f
        val scale = rowH / 15f //19

        val scaledLineHeight = lineHeight * scale
        val textY = y + (titleH - scaledLineHeight) / 2

        val scaledTextWidth = category.name.width() * scale
        val textX = (x) + ((width) - scaledTextWidth) / 2

        TEMPRender2D.drawString(ctx, category.name, (textX / scale).toInt(), (textY / scale).toInt(), color = Color.WHITE, scale = scale)
    }
}