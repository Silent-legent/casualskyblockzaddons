package com.cbza.net.mixin;

import com.cbza.net.feature.PowderChestSolver;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(
            method = "createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD")
    )
    private void onSpawnParticle(ParticleOptions options, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, CallbackInfoReturnable<?> ci) {
        if (options.getType() == ParticleTypes.CRIT) {
            System.out.println("[PowderChest] CRIT particle at " + x + ", " + y + ", " + z);

            // FORCE THE SOLVER AWAKE FOR TESTING
            PowderChestSolver.INSTANCE.onChestSpawn();

            // Send the coordinates over to the solver class
            PowderChestSolver.INSTANCE.handleParticle(x, y, z);
        }
    }
}