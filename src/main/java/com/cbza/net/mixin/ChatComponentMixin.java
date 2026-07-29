package com.cbza.net.mixin;

import com.cbza.net.feature.mining.general.MiningAbilityTracker;
import com.cbza.net.feature.mining.hollows.PowderChestSolver;
import com.cbza.net.feature.mining.hollows.map.NucleusMap;
import com.cbza.net.feature.mining.hollows.map.WishingCompassSolver;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("Sending to server (\\S+)");

    @Inject(method = "addMessage", at = @At("HEAD"))
    private void onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        String text = message.getString();
        if (text.contains("You uncovered a treasure chest!") && !text.contains("[DEBUG]")) {
            PowderChestSolver.INSTANCE.onChestSpawn();
        }
        if (text.contains("Sending to server")) {
            PowderChestSolver.INSTANCE.clearChests();
            Matcher matcher = SERVER_ID_PATTERN.matcher(text);
            if (matcher.find()) {
                NucleusMap.INSTANCE.onServerSwitch(matcher.group(1));
            }
        }
        if (text.contains("You used your") && text.contains("Pickaxe Ability!")) {
            MiningAbilityTracker.INSTANCE.onAbilityUsed(text);
        }
        if (text.contains("is now available!")) {
            MiningAbilityTracker.INSTANCE.onAbilityReady(text);
        }
        if (text.contains("Your Wishing Compass shattered into pieces!")) {
            WishingCompassSolver.INSTANCE.onCompassUsed();
        }
        if (text.contains("Get to the Queen before my stench goes away and you'll be able to sneak past her imbecile Guards!"))  {
            WishingCompassSolver.INSTANCE.onKingsScentGranted();
        }
        NucleusMap.INSTANCE.onCoordsShared(text);
    }
}