package com.cbza.net.utility

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.resources.Identifier

object Render2D {
    private val loadedTextures = mutableSetOf<Identifier>()

    fun drawImage(ctx: GuiGraphicsExtractor, image: Identifier?, x: Int, y: Int, width: Int, height: Int) {
        if (image == null) return
        val mc = Minecraft.getInstance()

        if (!loadedTextures.contains(image)) {
            val resourceId = Identifier.fromNamespaceAndPath(image.namespace, "textures/${image.path}.png")
            val tex = SimpleTexture(resourceId)
            mc.textureManager.register(image, tex)
            try {
                val contents = tex.loadContents(mc.resourceManager)
                tex.apply(contents)
            } catch (e: Exception) {
                println("DEBUG Render2D: failed to load texture $resourceId: ${e.message}")
            }
            loadedTextures.add(image)
        }

        ctx.blit(RenderPipelines.GUI_TEXTURED, image, x, y, 0f, 0f, width, height, width, height, -1)
    }
}