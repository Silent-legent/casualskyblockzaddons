package com.cbza.net.mixin;

import com.cbza.net.feature.PingGlide;
import com.cbza.net.feature.PowderChestSolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
            method = "renderLevel",
            at = @At("RETURN")
    )
    private void onRenderWorld(CallbackInfo ci) {


        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 camPos = camera.position();

        Matrix4f viewMatrix = new Matrix4f(mc.gameRenderer.getMainCamera().getViewRotationMatrix(new Matrix4f()));
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(viewMatrix);
        var matrix = poseStack.last().pose();

        // Powder Chest Solver boxes
        List<Vec3> chestPositions = PowderChestSolver.INSTANCE.getActiveChestPositions();
        if (!chestPositions.isEmpty()) {
            VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.debugFilledBox());
            int color = ARGB.colorFromFloat(1.0f, 0.0f, 1.0f, 0.0f);

            for (Vec3 targetPos : chestPositions) {
                drawBox(buffer, matrix,
                        targetPos.x - camPos.x, targetPos.y - camPos.y, targetPos.z - camPos.z,
                        0.05, 0.05, 0.05, color);
            }

            bufferSource.endBatch(RenderTypes.debugFilledBox());
        }

        // Ping Glide block outline
        if (PingGlide.INSTANCE.isCurrentlyMining()) {
            BlockPos blockPos = PingGlide.INSTANCE.getCurrentBlockPos();
            if (blockPos != null) {
                boolean safe = PingGlide.INSTANCE.isSafeToMove();


                int outlineColor = safe
                        ? ARGB.colorFromFloat(1.0f, 0.0f, 1.0f, 0.0f)
                        : ARGB.colorFromFloat(1.0f, 1.0f, 0.0f, 0.0f);
                int fillColor = safe
                        ? ARGB.colorFromFloat(0.3f, 0.0f, 1.0f, 0.0f)
                        : ARGB.colorFromFloat(0.3f, 1.0f, 0.0f, 0.0f);

                VoxelShape shape = mc.level != null
                        ? mc.level.getBlockState(blockPos).getShape(mc.level, blockPos)
                        : Shapes.block();

                double dx = blockPos.getX() - camPos.x;
                double dy = blockPos.getY() - camPos.y;
                double dz = blockPos.getZ() - camPos.z;

                // filled faces
                VertexConsumer fillBuffer = bufferSource.getBuffer(RenderTypes.debugQuads());
                shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
                    drawBox(fillBuffer, matrix,
                            dx + (x1 + x2) / 2, dy + (y1 + y2) / 2, dz + (z1 + z2) / 2,
                            (x2 - x1) / 1.9, (y2 - y1) / 1.9, (z2 - z1) / 1.9,
                            fillColor);
                });
                bufferSource.endBatch(RenderTypes.debugQuads());

                // outline edges
                PoseStack outlinePoseStack = new PoseStack();
                outlinePoseStack.last().pose().set(matrix);
                VertexConsumer lineBuffer = bufferSource.getBuffer(RenderTypes.lines());
                ShapeRenderer.renderShape(outlinePoseStack, lineBuffer, shape, dx, dy, dz, outlineColor, 10.0f);
                bufferSource.endBatch(RenderTypes.lines());
            }
        }
    }

    private void drawBox(VertexConsumer buffer, Matrix4f matrix, double dx, double dy, double dz, double sx, double sy, double sz, int color) {
        float x1 = (float)(dx - sx), x2 = (float)(dx + sx);
        float y1 = (float)(dy - sy), y2 = (float)(dy + sy);
        float z1 = (float)(dz - sz), z2 = (float)(dz + sz);

        // bottom
        buffer.addVertex(matrix, x1, y1, z2).setColor(color);
        buffer.addVertex(matrix, x2, y1, z2).setColor(color);
        buffer.addVertex(matrix, x2, y1, z1).setColor(color);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        // top
        buffer.addVertex(matrix, x1, y2, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
        buffer.addVertex(matrix, x1, y2, z2).setColor(color);
        // north
        buffer.addVertex(matrix, x1, y2, z1).setColor(color);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z1).setColor(color);
        // south
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
        buffer.addVertex(matrix, x2, y1, z2).setColor(color);
        buffer.addVertex(matrix, x1, y1, z2).setColor(color);
        buffer.addVertex(matrix, x1, y2, z2).setColor(color);
        // west
        buffer.addVertex(matrix, x1, y2, z2).setColor(color);
        buffer.addVertex(matrix, x1, y1, z2).setColor(color);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        buffer.addVertex(matrix, x1, y2, z1).setColor(color);
        // east
        buffer.addVertex(matrix, x2, y2, z1).setColor(color);
        buffer.addVertex(matrix, x2, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y1, z2).setColor(color);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
    }
}