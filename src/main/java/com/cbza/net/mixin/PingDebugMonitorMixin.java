    /*
     * Code given to me by Revvilon Creator of PingOfsetMiner
     */

package com.cbza.net.mixin;

import com.cbza.net.utility.PingTracker;
import net.minecraft.client.multiplayer.PingDebugMonitor;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PingDebugMonitor.class)
public class PingDebugMonitorMixin {
    @Inject(method = "onPongReceived", at = @At("TAIL"))
    private void onPongReceivedEvent(ClientboundPongResponsePacket packet, CallbackInfo ci) {
        long delta = Util.getMillis() - packet.time();
        PingTracker.INSTANCE.onPongReceived(delta);
    }
}