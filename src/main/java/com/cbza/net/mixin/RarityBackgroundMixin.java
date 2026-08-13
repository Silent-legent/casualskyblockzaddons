package com.cbza.net.mixin;

import com.cbza.net.feature.general.RarityBackground;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(GuiGraphicsExtractor.class)
public class RarityBackgroundMixin {

    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundWithOwner(@Nullable LivingEntity owner, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = RarityBackground.INSTANCE.getRarityColor(itemStack);
        if (color != -1) {
            ((GuiGraphicsExtractor) (Object) this).fill(x, y, x + 16, y + 16, color);
        }
    }

    @Inject(
            method = "item(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundWithSeed(ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = RarityBackground.INSTANCE.getRarityColor(itemStack);
        if (color != -1) {
            ((GuiGraphicsExtractor) (Object) this).fill(x, y, x + 16, y + 16, color);
        }
    }

    @Inject(
            method = "fakeItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundFake(ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = RarityBackground.INSTANCE.getRarityColor(itemStack);
        if (color != -1) {
            ((GuiGraphicsExtractor) (Object) this).fill(RenderPipelines.GUI, x, y, x + 16, y + 16, color);
        }
    }
}