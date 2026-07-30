package com.cbza.net.mixin;

import com.cbza.net.config.ModConfig;

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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.cbza.net.utility.ColorCatalog.*;

@Mixin(GuiGraphicsExtractor.class)
public class RarityBackgroundMixin {

    private static final Map<ItemStack, Integer> RARITY_COLOR_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static String stripLeadingIcon(String line) {
        int i = 0;
        while (i < line.length() && !(line.charAt(i) >= 'A' && line.charAt(i) <= 'Z')) {
            i++;
        }
        return line.substring(i);
    }

    private static int getRarityColor(ItemStack itemStack) {
        if (!ModConfig.Companion.get().showRarityBackgrounds) return -1;
        if (itemStack == null || itemStack.isEmpty()) return -1;

        Integer cached = RARITY_COLOR_CACHE.get(itemStack);
        if (cached != null) return cached;

        int color = computeRarityColor(itemStack);
        RARITY_COLOR_CACHE.put(itemStack, color);
        return color;
    }

    private static int computeRarityColor(ItemStack itemStack) {
        ItemLore lore = itemStack.get(DataComponents.LORE);
        if (lore == null) return -1;

        List<Component> lines = lore.lines();
        if (lines == null || lines.isEmpty()) return -1;

        int linesToCheck = Math.min(8, lines.size());
        for (int i = 0; i < linesToCheck; i++) {
            String rawLine = stripLeadingIcon(lines.get(lines.size() - 1 - i).getString().trim());

            if (rawLine.startsWith("ADMIN"))        return TRANSLUCENT_DARK_RED;
            if (rawLine.startsWith("ULTIMATE"))     return TRANSLUCENT_DARK_RED;
            if (rawLine.startsWith("VERY SPECIAL")) return TRANSLUCENT_LIGHT_RED;
            if (rawLine.startsWith("SPECIAL"))      return TRANSLUCENT_LIGHT_RED;
            if (rawLine.startsWith("DIVINE"))       return TRANSLUCENT_CYAN;
            if (rawLine.startsWith("MYTHIC"))       return TRANSLUCENT_LIGHT_MAGENTA;
            if (rawLine.startsWith("LEGENDARY"))    return TRANSLUCENT_GOLD;
            if (rawLine.startsWith("EPIC"))         return TRANSLUCENT_DARK_PURPLE;
            if (rawLine.startsWith("RARE"))         return TRANSLUCENT_LIGHT_BLUE;
            if (rawLine.startsWith("UNCOMMON"))     return TRANSLUCENT_LIGHT_GREEN;
            if (rawLine.startsWith("COMMON"))       return TRANSLUCENT_WHITE;

        }

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