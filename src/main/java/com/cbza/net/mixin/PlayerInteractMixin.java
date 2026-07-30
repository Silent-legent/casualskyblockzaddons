package com.cbza.net.mixin;

import com.cbza.net.event.EventBus;
import com.cbza.net.event.events.BlockInteractEvent;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class PlayerInteractMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOn(net.minecraft.client.player.LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> ci) {
        BlockPos pos = hitResult.getBlockPos();
        EventBus.INSTANCE.post(new BlockInteractEvent(pos));
    }
}