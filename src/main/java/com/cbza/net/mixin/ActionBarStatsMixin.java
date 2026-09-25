package com.cbza.net.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.cbza.net.config.ModConfig;

@Mixin(Gui.class)
public abstract class ActionBarStatsMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetOverlayMessage(Component message, boolean tinted, CallbackInfo ci) {
        if (ModConfig.get().getHideActionBarStats()) {
            ci.cancel();
        }
    }
}