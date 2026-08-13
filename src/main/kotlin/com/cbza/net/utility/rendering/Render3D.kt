package com.cbza.net.utility.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import org.joml.Matrix4f

/**
 * 3D rendering utilities for drawing wireframes, bounding boxes, and filled region overlays.
 */
object Render3D {

    /**
     * Draws a 12-line wireframe box (24 vertices) using explicit coordinate bounds.
     */
    fun drawOutlinedBox(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        color: Int
    ) {
        fun line(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float) {
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
        }

        // Bottom Face
        line(minX, minY, minZ, maxX, minY, minZ)
        line(maxX, minY, minZ, maxX, minY, maxZ)
        line(maxX, minY, maxZ, minX, minY, maxZ)
        line(minX, minY, maxZ, minX, minY, minZ)

        // Top Face
        line(minX, maxY, minZ, maxX, maxY, minZ)
        line(maxX, maxY, minZ, maxX, maxY, maxZ)
        line(maxX, maxY, maxZ, minX, maxY, maxZ)
        line(minX, maxY, maxZ, minX, maxY, minZ)

        // Vertical Pillars
        line(minX, minY, minZ, minX, maxY, minZ)
        line(maxX, minY, minZ, maxX, maxY, minZ)
        line(maxX, minY, maxZ, maxX, maxY, maxZ)
        line(minX, minY, maxZ, minX, maxY, maxZ)
    }

    /**
     * Draws a 6-face filled box using Quads (24 vertices) using explicit coordinate bounds.
     */
    fun drawFilledBox(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        color: Int
    ) {
        fun quad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float
        ) {
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
            buffer.addVertex(matrix, cx, cy, cz).setColor(color)
            buffer.addVertex(matrix, dx, dy, dz).setColor(color)
        }

        quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ) // Bottom
        quad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ) // Top
        quad(minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ) // North
        quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ) // South
        quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ) // West
        quad(maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, minY, minZ) // East
    }

    /**
     * Draws a double-sided filled box using Quads (48 vertices) using explicit bounds.
     */
    fun drawDoubleSidedFilledBox(
        buffer: VertexConsumer,
        matrix: Matrix4f,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        color: Int
    ) {
        fun doubleQuad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float
        ) {
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
            buffer.addVertex(matrix, cx, cy, cz).setColor(color)
            buffer.addVertex(matrix, dx, dy, dz).setColor(color)

            buffer.addVertex(matrix, dx, dy, dz).setColor(color)
            buffer.addVertex(matrix, cx, cy, cz).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
        }

        doubleQuad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ)
        doubleQuad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ)
        doubleQuad(minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, minX, minY, minZ)
        doubleQuad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ)
        doubleQuad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ)
        doubleQuad(maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, maxX, minY, minZ)
    }

    // --- Convenience Overloads for BlockPos and AABB ---

    fun drawOutlinedBox(buffer: VertexConsumer, matrix: Matrix4f, pos: BlockPos, color: Int) {
        drawOutlinedBox(
            buffer, matrix,
            pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
            (pos.x + 1).toFloat(), (pos.y + 1).toFloat(), (pos.z + 1).toFloat(),
            color
        )
    }

    fun drawFilledBox(buffer: VertexConsumer, matrix: Matrix4f, pos: BlockPos, color: Int) {
        drawFilledBox(
            buffer, matrix,
            pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat(),
            (pos.x + 1).toFloat(), (pos.y + 1).toFloat(), (pos.z + 1).toFloat(),
            color
        )
    }

    fun drawOutlinedBox(buffer: VertexConsumer, matrix: Matrix4f, box: AABB, color: Int) {
        drawOutlinedBox(
            buffer, matrix,
            box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(),
            box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(),
            color
        )
    }

    // --- Backwards Compatibility Aliases (Keeps MimicChest and others working) ---

    fun drawBox(
        buffer: VertexConsumer, matrix: Matrix4f,
        dx: Double, dy: Double, dz: Double,
        sx: Double, sy: Double, sz: Double,
        color: Int
    ) {
        drawOutlinedBox(
            buffer, matrix,
            (dx - sx).toFloat(), (dy - sy).toFloat(), (dz - sz).toFloat(),
            (dx + sx).toFloat(), (dy + sy).toFloat(), (dz + sz).toFloat(),
            color
        )
    }

    fun drawBoxDoubleSided(
        buffer: VertexConsumer, matrix: Matrix4f,
        dx: Double, dy: Double, dz: Double,
        sx: Double, sy: Double, sz: Double,
        color: Int
    ) {
        drawDoubleSidedFilledBox(
            buffer, matrix,
            (dx - sx).toFloat(), (dy - sy).toFloat(), (dz - sz).toFloat(),
            (dx + sx).toFloat(), (dy + sy).toFloat(), (dz + sz).toFloat(),
            color
        )
    }
}