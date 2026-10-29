package com.cbza.net.mixin;

import com.cbza.net.event.EventBus;
import com.cbza.net.event.events.RenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.Vec3;

//? if <26.2 {
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
//?}
//? if >=26.2 {
/*import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
*///?}

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    //? if >=26.2 {
    /*@Inject(
            method = "submitFeatures",
            at = @At("TAIL")
    )
    private void onSubmitFeatures(
            LevelRenderState levelRenderState,
            SubmitNodeCollector submitNodeCollector,
            boolean renderOutline,
            CallbackInfo ci
    ) {
        Vec3 camPos = levelRenderState.cameraRenderState.pos;
        PoseStack poseStack = new PoseStack();

        EventBus.INSTANCE.post(new RenderEvent(submitNodeCollector, poseStack, camPos));
    }
    *///?} else {
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

        EventBus.INSTANCE.post(new RenderEvent(bufferSource, matrix, camPos));

        bufferSource.endBatch();
    }
    //?}
}