package com.cbza.net.utility.rendering

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.texture.SimpleTexture
import net.minecraft.resources.Identifier

/**
 * 2D rendering utilities for drawing custom GUI textures, map icons, and HUD overlays.
 */
object Render2D {

    // Keeps track of initialized textures to avoid redundant registration calls
    private val loadedTextures = mutableSetOf<Identifier>()

    /**
     * Draws a full image at the specified screen position and dimensions.
     */
    fun drawImage(
        ctx: GuiGraphicsExtractor,
        image: Identifier?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        color: Int = -1
    ) {
        if (image == null) return
        val mc = Minecraft.getInstance()

        ensureTextureLoaded(mc, image)

        // Draw full texture mapped across the bounding box
        ctx.blit(
            RenderPipelines.GUI_TEXTURED,
            image,
            x, y,
            0f, 0f,
            width, height,
            width, height,
            color
        )
    }

    /**
     * Draws a cropped region (UV cutout) of a texture sheet at screen coordinates [x], [y].
     * Useful for sprite sheets or multi-icon atlas textures.
     */
    fun drawTextureRegion(
        ctx: GuiGraphicsExtractor,
        image: Identifier?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        regionWidth: Int,
        regionHeight: Int,
        textureWidth: Int,
        textureHeight: Int,
        color: Int = -1
    ) {
        if (image == null) return
        val mc = Minecraft.getInstance()

        ensureTextureLoaded(mc, image)

        ctx.blit(
            RenderPipelines.GUI_TEXTURED,
            image,
            x, y,
            u, v,
            width, height,
            regionWidth, regionHeight,
            textureWidth, textureHeight,
            color
        )
    }

    /**
     * Ensures the given [image] identifier is registered with [TextureManager] and loaded.
     */
    private fun ensureTextureLoaded(mc: Minecraft, image: Identifier) {
        if (loadedTextures.contains(image)) return

        // Standardize file path extension
        val cleanPath = image.path.removePrefix("textures/").removeSuffix(".png")
        val resourceId = Identifier.fromNamespaceAndPath(image.namespace, "textures/$cleanPath.png")

        val tex = SimpleTexture(resourceId)
        mc.textureManager.register(image, tex)

        try {
            // Safely load and close native memory buffers
            tex.loadContents(mc.resourceManager).use { contents ->
                tex.apply(contents)
            }
        } catch (e: Exception) {
            println("DEBUG Render2D: failed to load texture $resourceId: ${e.message}")
        }

        loadedTextures.add(image)
    }

    /**
     * Clears all cached texture references. Call on resource reload or game disconnect.
     */
    fun clearCache() {
        loadedTextures.clear()
    }
}