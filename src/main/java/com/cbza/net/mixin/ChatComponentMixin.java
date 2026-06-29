package com.cbza.net.mixin;

import com.cbza.net.feature.MiningAbilityTracker;
import com.cbza.net.feature.PowderChestSolver;
import com.cbza.net.feature.NucleusMap;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(method = "addMessage", at = @At("HEAD"))
    private void onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        String text = message.getString();
        if (text.contains("You uncovered a treasure chest!") && !text.contains("[DEBUG]")) {
            PowderChestSolver.INSTANCE.onChestSpawn();
        }
        if (text.contains("Sending to server")) {
            PowderChestSolver.INSTANCE.clearChests();
            NucleusMap.INSTANCE.reset();
        }
        if (text.contains("You used your") && text.contains("Pickaxe Ability!")) {
            MiningAbilityTracker.INSTANCE.onAbilityUsed(text);
        }
        if (text.contains("is now available!")) {
            MiningAbilityTracker.INSTANCE.onAbilityReady(text);
        }
    }
}