package com.cbza.net.mixin;

import com.cbza.net.event.EventBus;
import com.cbza.net.event.events.RenderEvent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(
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
}