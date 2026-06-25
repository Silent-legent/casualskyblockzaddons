package com.example.client.mixin;

import com.example.client.feature.PowderChestSolver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
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

        List<Vec3> chestPositions = PowderChestSolver.getActiveChestPositions();
        if (chestPositions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.debugFilledBox());

        Vec3 camPos = camera.position();

        Matrix4f viewMatrix = new Matrix4f(mc.gameRenderer.getMainCamera().getViewRotationMatrix(new Matrix4f()));
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(viewMatrix);
        var matrix = poseStack.last().pose();

        int color = ARGB.colorFromFloat(1.0f, 0.0f, 1.0f, 0.0f);

        for (Vec3 targetPos : chestPositions) {
            double dx = targetPos.x - camPos.x;
            double dy = targetPos.y - camPos.y;
            double dz = targetPos.z - camPos.z;
            double s = 0.05;

            float x1 = (float)(dx - s), x2 = (float)(dx + s);
            float y1 = (float)(dy - s), y2 = (float)(dy + s);
            float z1 = (float)(dz - s), z2 = (float)(dz + s);

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

        bufferSource.endBatch(RenderTypes.debugFilledBox());
    }
}