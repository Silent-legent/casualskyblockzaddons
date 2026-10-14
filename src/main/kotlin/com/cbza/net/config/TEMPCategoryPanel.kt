package com.cbza.net.config

import com.cbza.net.utility.ColorCatalog
import com.cbza.net.utility.rendering.TEMPRender2D
import com.cbza.net.utility.rendering.TEMPRender2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

object TEMPCategoryPanel {
    const val TITLE_HEIGHT = 14
    private const val ROW_HEIGHT = 14

    fun rowY(panelY: Int, row: Int): Int = panelY + 14 + row * ROW_HEIGHT

    fun draw(ctx: GuiGraphicsExtractor,category: ConfigCategory,x: Int, y: Int, width: Int, folded: Boolean, mouseX: Int, mouseY: Int) {

        val titleHovered = mouseX in x..(x + width) && mouseY in y..(y + TITLE_HEIGHT)
        val titleColor = if (titleHovered) ColorCatalog.UNSELECTED_HOVER1 else ColorCatalog.UNSELECTED1

        TEMPRender2D.drawRect(ctx, x, y, width, TITLE_HEIGHT, Color(titleColor, true))

        for (row in category.entries.indices) {
            if (!folded) {
                val entry = category.entries[row]
                val top = rowY(y, row)
                val enabled = entry.getter()

                val hovered = mouseX in x..(x + width) && mouseY in top..(top + ROW_HEIGHT)
                val color = when {
                    enabled && hovered -> ColorCatalog.SELECTED_HOVER1
                    enabled -> ColorCatalog.SELECTED1
                    hovered -> ColorCatalog.UNSELECTED_HOVER1
                    else -> ColorCatalog.UNSELECTED1
                }

                TEMPRender2D.drawRect(ctx, x, top, width, ROW_HEIGHT, Color(color, true))

                if (enabled) {
                    TEMPRender2D.drawRect(ctx, x, top, 3, ROW_HEIGHT, Color(ColorCatalog.LINECOLOR1, true))
                }
                val lineHeight = 6f
                val scale = ROW_HEIGHT / 15f //19

                val scaledLineHeight = lineHeight * scale
                val textY = rowY(y, row) + (ROW_HEIGHT - scaledLineHeight) / 2

                val scaledTextWidth = entry.label.width() * scale
                val textX = (x) + ((width) - scaledTextWidth) / 2
                TEMPRender2D.drawString(ctx, entry.label, (textX / scale).toInt(), (textY / scale).toInt(), color = Color.WHITE, scale = scale)
            }
        }

        val lineHeight = 6f
        val scale = TITLE_HEIGHT / 15f //19

        val scaledLineHeight = lineHeight * scale
        val textY = y + (TITLE_HEIGHT - scaledLineHeight) / 2

        val scaledTextWidth = category.name.width() * scale
        val textX = (x) + ((width) - scaledTextWidth) / 2

        TEMPRender2D.drawString(ctx, category.name, (textX / scale).toInt(), (textY / scale).toInt(), color = Color.WHITE, scale = scale)
    }
}