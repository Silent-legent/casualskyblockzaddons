package com.cbza.net.utility.rendering

import com.mojang.blaze3d.vertex.VertexConsumer
import org.joml.Matrix4f

object Render3D {

    fun drawBox(buffer: VertexConsumer, matrix: Matrix4f, dx: Double, dy: Double, dz: Double, sx: Double, sy: Double, sz: Double, color: Int) {
        val x1 = (dx - sx).toFloat(); val x2 = (dx + sx).toFloat()
        val y1 = (dy - sy).toFloat(); val y2 = (dy + sy).toFloat()
        val z1 = (dz - sz).toFloat(); val z2 = (dz + sz).toFloat()

        buffer.addVertex(matrix, x1, y1, z2).setColor(color)
        buffer.addVertex(matrix, x2, y1, z2).setColor(color)
        buffer.addVertex(matrix, x2, y1, z1).setColor(color)
        buffer.addVertex(matrix, x1, y1, z1).setColor(color)
        buffer.addVertex(matrix, x1, y2, z1).setColor(color)
        buffer.addVertex(matrix, x2, y2, z1).setColor(color)
        buffer.addVertex(matrix, x2, y2, z2).setColor(color)
        buffer.addVertex(matrix, x1, y2, z2).setColor(color)
        buffer.addVertex(matrix, x1, y2, z1).setColor(color)
        buffer.addVertex(matrix, x1, y1, z1).setColor(color)
        buffer.addVertex(matrix, x2, y1, z1).setColor(color)
        buffer.addVertex(matrix, x2, y2, z1).setColor(color)
        buffer.addVertex(matrix, x2, y2, z2).setColor(color)
        buffer.addVertex(matrix, x2, y1, z2).setColor(color)
        buffer.addVertex(matrix, x1, y1, z2).setColor(color)
        buffer.addVertex(matrix, x1, y2, z2).setColor(color)
        buffer.addVertex(matrix, x1, y2, z2).setColor(color)
        buffer.addVertex(matrix, x1, y1, z2).setColor(color)
        buffer.addVertex(matrix, x1, y1, z1).setColor(color)
        buffer.addVertex(matrix, x1, y2, z1).setColor(color)
        buffer.addVertex(matrix, x2, y2, z1).setColor(color)
        buffer.addVertex(matrix, x2, y1, z1).setColor(color)
        buffer.addVertex(matrix, x2, y1, z2).setColor(color)
        buffer.addVertex(matrix, x2, y2, z2).setColor(color)
    }

    fun drawBoxDoubleSided(buffer: VertexConsumer, matrix: Matrix4f, dx: Double, dy: Double, dz: Double, sx: Double, sy: Double, sz: Double, color: Int) {
        val x1 = (dx - sx).toFloat(); val x2 = (dx + sx).toFloat()
        val y1 = (dy - sy).toFloat(); val y2 = (dy + sy).toFloat()
        val z1 = (dz - sz).toFloat(); val z2 = (dz + sz).toFloat()

        fun quad(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, cx: Float, cy: Float, cz: Float, dx2: Float, dy2: Float, dz2: Float) {
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
            buffer.addVertex(matrix, cx, cy, cz).setColor(color)
            buffer.addVertex(matrix, dx2, dy2, dz2).setColor(color)

            buffer.addVertex(matrix, dx2, dy2, dz2).setColor(color)
            buffer.addVertex(matrix, cx, cy, cz).setColor(color)
            buffer.addVertex(matrix, bx, by, bz).setColor(color)
            buffer.addVertex(matrix, ax, ay, az).setColor(color)
        }

        quad(x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1) // bottom
        quad(x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2) // top
        quad(x1, y2, z1, x1, y1, z1, x2, y1, z1, x2, y2, z1) // north
        quad(x2, y2, z2, x2, y1, z2, x1, y1, z2, x1, y2, z2) // south
        quad(x1, y2, z2, x1, y1, z2, x1, y1, z1, x1, y2, z1) // west
        quad(x2, y2, z1, x2, y1, z1, x2, y1, z2, x2, y2, z2) // east
    }
}