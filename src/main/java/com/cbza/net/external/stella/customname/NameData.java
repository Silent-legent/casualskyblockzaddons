    /*
     * Inspired by Stella; https://github.com/Eclipse-5214/stella
     * Permission to use from Eclipse-5214
    */

package com.cbza.net.external.stella.customname;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.awt.Color;
import java.util.List;

public class NameData {
    private String text;
    private List<ExtraPart> extra;
    private List<String> gradient;

    public static class ExtraPart {
        private String text;
        private String color;

        public String getText() { return text; }
        public String getColor() { return color; }
    }

    public String getText() { return text; }
    public List<ExtraPart> getExtra() { return extra; }
    public List<String> getGradient() { return gradient; }

    // actualName = the real username as it appears in chat (correct case), used
    // as the fallback display text and as the string the gradient is spread across
    public MutableComponent getComponent(String actualName) {
        if (gradient != null && !gradient.isEmpty()) {
            return buildGradientComponent(actualName);
        }

        MutableComponent base = Component.literal(text != null ? text : actualName);
        if (extra != null) {
            for (ExtraPart part : extra) {
                base.append(Component.literal(part.getText())
                        .withColor(Color.decode(normalizeHex(part.getColor())).getRGB()));
            }
        }
        return base;
    }

    private MutableComponent buildGradientComponent(String actualName) {
        MutableComponent result = Component.empty();
        int len = actualName.length();
        for (int i = 0; i < len; i++) {
            int colorIndex = len <= 1
                    ? 0
                    : (int) Math.round(i * (gradient.size() - 1) / (double) (len - 1));
            String hex = gradient.get(colorIndex);
            result.append(Component.literal(String.valueOf(actualName.charAt(i)))
                    .withColor(Color.decode(normalizeHex(hex)).getRGB()));
        }
        return result;
    }

    private static String normalizeHex(String hex) {
        return hex.startsWith("#") ? hex : "#" + hex;
    }
}