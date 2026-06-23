package com.example.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Mixin(GuiGraphicsExtractor.class)
public class RarityBackgroundMixin {

    //   COLOR LOGIC (Reads item lore and determines background hex colors)
    private static int getRarityColor(ItemStack itemStack) {
        if (!com.example.client.config.ModConfig.get().showRarityBackgrounds) return -1;
        if (itemStack == null || itemStack.isEmpty()) return -1;

        // --- Configuration Check ---
        ItemLore lore = itemStack.get(DataComponents.LORE);
        if (lore == null) return -1;

        List<Component> lines = lore.lines();
        if (lines == null || lines.isEmpty()) return -1;

        // --- Read Last Line of Lore ---
        String lastLine = lines.get(lines.size() - 1).getString().toUpperCase();

        // --- Color Mapping Settings ---
        if (lastLine.contains("ADMIN"))        return 0x60AA0000;
        if (lastLine.contains("ULTIMATE"))     return 0x60AA0000;
        if (lastLine.contains("VERY SPECIAL")) return 0x60FF5555;
        if (lastLine.contains("SPECIAL"))      return 0x60FF5555;
        if (lastLine.contains("DIVINE"))       return 0x6055FFFF;
        if (lastLine.contains("MYTHIC"))       return 0x60FF55FF;
        if (lastLine.contains("LEGENDARY"))    return 0x60FFAA00;
        if (lastLine.contains("EPIC"))         return 0x60AA00AA;
        if (lastLine.contains("RARE"))         return 0x605555FF;
        if (lastLine.contains("UNCOMMON"))     return 0x6055FF55;
        if (lastLine.contains("COMMON"))       return 0x60FFFFFF;

        return -1;
    }

    // =========================================================================
    //   MIXIN INJECTIONS (Hooks into Minecraft's rendering to draw backgrounds)

    // --- Context 1: Items held/owned by entities ---
    @Inject(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundWithOwner(@Nullable LivingEntity owner, ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = getRarityColor(itemStack);
        if (color == -1) return;
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        graphics.fill(x, y, x + 16, y + 16, color);
    }
    // --- Context 2: Standard standalone inventory items ---
    @Inject(
            method = "item(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundWithSeed(ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = getRarityColor(itemStack);
        if (color == -1) return;
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        graphics.fill(x, y, x + 16, y + 16, color);
    }
    // --- Context 3: Ghost/Fake items (recipes, background previews, etc) ---
    @Inject(
            method = "fakeItem(Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD")
    )
    private void drawRarityBackgroundFake(ItemStack itemStack, int x, int y, int seed, CallbackInfo ci) {
        int color = getRarityColor(itemStack);
        if (color == -1) return;
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        graphics.fill(RenderPipelines.GUI, x, y, x + 16, y + 16, color);
    }
}