package com.cbza.net.mixin;

import com.cbza.net.feature.mining.general.MiningAbilityTracker;
import com.cbza.net.feature.mining.hollows.map.NucleusMap;
import com.cbza.net.feature.mining.general.PingGlide;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        MiningAbilityTracker.INSTANCE.tick();
        NucleusMap.INSTANCE.tick();
        PingGlide.INSTANCE.tick();
    }
}