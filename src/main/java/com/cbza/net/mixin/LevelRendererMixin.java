package com.cbza.net.mixin;

import com.cbza.net.event.EventBus;
import com.cbza.net.event.events.WorldRenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

        Matrix4f viewMatrix = new Matrix4f(camera.getViewRotationMatrix(new Matrix4f()));
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(viewMatrix);
        var matrix = poseStack.last().pose();

        // 1. Fire event so features can submit draw calls
        EventBus.INSTANCE.post(new WorldRenderEvent(bufferSource, matrix, camPos));

        // 2. Flush the buffer so queued feature render calls actually draw!
        bufferSource.endBatch();
    }
}