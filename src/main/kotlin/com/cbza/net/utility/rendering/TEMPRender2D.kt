/*
* Originaly from Stella; https://github.com/Eclipse-5214/stella
* Permission to use from Eclipse-5214
*
*/

package com.cbza.net.utility.rendering

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import java.awt.Color
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.splitLines
import kotlin.math.*

object TEMPRender2D {
    private val formattingRegex = "(?<!\\\\\\\\)&(?=[0-9a-fk-or])".toRegex()
    val client = Minecraft.getInstance()

    fun drawImage(ctx: GuiGraphicsExtractor, image: Identifier?, x: Int, y: Int, width: Int, height: Int, color: Color) {
        drawImage(ctx, image, x, y, width, height, color.rgb)
    }

    fun drawImage(ctx: GuiGraphicsExtractor, image: Identifier?, x: Int, y: Int, width: Int, height: Int, color: Int = -1) {
        if (image == null) return
        ctx.blit(RenderPipelines.GUI_TEXTURED, image, x, y, 0f, 0f, width, height, width, height, width, height, color)
    }

    fun drawSprite(ctx: GuiGraphicsExtractor, sprite: Identifier, x: Int, y: Int, width: Int, height: Int, color: Color) {
        drawSprite(ctx, sprite, x, y, width, height, color.rgb)
    }

    fun drawSprite(ctx: GuiGraphicsExtractor, sprite: Identifier, x: Int, y: Int, width: Int, height: Int, color: Int = -1) {
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, color)
    }

    @JvmOverloads
    fun drawRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
    }

    @JvmOverloads
    fun drawHollowRect(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, thickness: Int, color: Color = Color.WHITE) {
        if (thickness <= 0) return
        val rgb = color.rgb
        ctx.fill(RenderPipelines.GUI, x, y, x + width, y + thickness, rgb)
        ctx.fill(RenderPipelines.GUI, x, y + height - thickness, x + width, y + height, rgb)
        ctx.fill(RenderPipelines.GUI, x, y + thickness, x + thickness, y + height - thickness, rgb)
        ctx.fill(RenderPipelines.GUI, x + width - thickness, y + thickness, x + width, y + height - thickness, rgb)
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.text(
            client.font,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: Component, x: Int, y: Int, scale: Float = 1f, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.text(
            client.font,
            str,
            x,
            y,
            -1,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    @JvmOverloads
    fun drawString(ctx: GuiGraphicsExtractor, str: String, x: Int, y: Int, scale: Float = 1f, color: Color, shadow: Boolean = true) {
        val matrices = ctx.pose()
        if (scale != 1f) {
            matrices.pushMatrix()
            matrices.scale(scale, scale)
        }

        ctx.text(
            client.font,
            str.replace(formattingRegex, "${ChatFormatting.PREFIX_CODE}"),
            x,
            y,
            color.rgb,
            shadow
        )

        if (scale != 1f) matrices.popMatrix()
    }

    fun renderItem(context: GuiGraphicsExtractor, item: ItemStack, x: Float, y: Float, scale: Float) {
        context.pose().pushMatrix()
        context.pose().translate(x, y)
        context.pose().scale(scale, scale)
        context.item(item, 0, 0)
        context.itemDecorations(client.font, item, 0, 0)
        context.pose().popMatrix()
    }

    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { client.font.width(it.stripColor()) }
    }

    fun MutableComponent.width(): Int {
        val lines = splitLines()
        return lines.maxOf { client.font.width(it)}
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return client.font.lineHeight * lineCount
    }

    fun MutableComponent.height(): Int {
        val lineCount = this.string.count { it == '\n' } + 1
        return client.font.lineHeight * lineCount
    }

    fun renderScrolled(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, scrollOffset: Float, block: () -> Unit) {
        ctx.enableScissor(x, y, x + width, y + height)
        ctx.pushPop {
            ctx.pose().translate(x.toFloat(), y.toFloat())
            ctx.pose().translate(0f, scrollOffset)
            block()
        }

        ctx.disableScissor()
    }

    fun calculateScroll(currentTarget: Float, amount: Float, totalHeight: Int, viewportHeight: Int, speed: Float = 20f): Float {
        val maxScroll = (totalHeight - viewportHeight).toFloat().coerceAtLeast(0f)
        return (currentTarget + amount * speed).coerceIn(-maxScroll, 0f)
    }

    fun drawScrollbar(ctx: GuiGraphicsExtractor, x: Int, y: Int, viewportHeight: Int, scrollOffset: Float, totalHeight: Int, color: Color) {
        if (totalHeight <= viewportHeight) return
        val barHeight = (viewportHeight.toFloat() / totalHeight) * viewportHeight
        val barY = (-scrollOffset / totalHeight) * viewportHeight
        drawRect(ctx, x, y + barY.toInt(), 1, barHeight.toInt(), color)
    }

    fun drawLine(
        ctx: GuiGraphicsExtractor,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        color: Color,
        thickness: Float = 1f
    ) {
        val dx = x2 - x1
        val dy = y2 - y1
        val len = hypot(dx, dy)
        if (len == 0f) return

        val angle = atan2(dy, dx)
        val halfWidth = thickness * 0.5f

        ctx.pushPop {
            val pose = ctx.pose()
            pose.translate(x1, y1)
            pose.rotate(angle)
            ctx.fill(RenderPipelines.GUI, 0, (-halfWidth).toInt(), ceil(len).toInt(), ceil(halfWidth).toInt(), color.rgb)
        }
    }
}