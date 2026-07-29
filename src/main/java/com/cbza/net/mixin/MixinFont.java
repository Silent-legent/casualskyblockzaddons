    /*
    * Originaly from Stella; https://github.com/Eclipse-5214/stella
    * Permission to use from Eclipse-5214
    */

package com.cbza.net.mixin;

import com.cbza.net.external.stella.customname.Cosmetics;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class MixinFont {
    @ModifyVariable(method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;", at = @At("HEAD"), argsOnly = true, name = "text")
    private FormattedCharSequence onPrepareText(FormattedCharSequence text) {
        return Cosmetics.handleCharSequence(text);
    }

    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), argsOnly = true, name = "text")
    private FormattedCharSequence onTextWidth(FormattedCharSequence text) {
        return Cosmetics.handleCharSequence(text);
    }
}