package com.cbza.net.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    private void onAddMessage(Component message, CallbackInfo ci) {
        // You can add your chat filtering or detection logic here
    }
}