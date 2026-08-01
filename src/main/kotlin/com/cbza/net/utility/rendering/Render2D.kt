package com.cbza.net.utility.rendering

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.resources.Identifier

// Helper for drawing custom images (like map icons) on screen. Loads each
// image file the first time it's needed and reuses it after that.
object Render2D {
    // Keeps track of which images have already been loaded, so we don't reload the same one twice.
    private val loadedTextures = mutableSetOf<Identifier>()

    // Draws an image at the given position and size. Loads the image file from
    // disk first if it hasn't been used before.
    fun drawImage(ctx: GuiGraphicsExtractor, image: Identifier?, x: Int, y: Int, width: Int, height: Int) {
        if (image == null) return
        val mc = Minecraft.getInstance()

        if (!loadedTextures.contains(image)) {
            // First time seeing this image. load it into the game and remember it for next time.
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

        // Actually draw the image on screen.
        ctx.blit(RenderPipelines.GUI_TEXTURED, image, x, y, 0f, 0f, width, height, width, height, -1)
    }
}