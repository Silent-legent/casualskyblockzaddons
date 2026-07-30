package com.cbza.net.mixin;

import com.cbza.net.event.EventBus;
import com.cbza.net.event.events.ChatMessageEvent;
import com.cbza.net.feature.mining.hollows.map.WishingCompassSolver;
import com.cbza.net.feature.mining.hollows.PowderChestSolver;
import com.cbza.net.event.events.ParticleSpawnEvent;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD")
    )
    private void onSpawnParticle(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<?> ci) {
        EventBus.INSTANCE.post(new ParticleSpawnEvent(options, x, y, z));
    }

}