package com.example.client.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {

    private final List<ConfigCategory> categories = new ArrayList<>();
    private static final int COLUMN_WIDTH = 150;
    private static final int ROW_HEIGHT = 22;
    private static final int COLUMN_GAP = 10;
    private static final int TOP_MARGIN = 30;

    private final Screen parent;

    // Define colors as ARGB hexadecimal (AARRGGBB)
    // Keep standard vanilla dark color for 'OFF' (matches default)
    private static final int COLOR_DISABLED_BOX = 0xFF353535; // Normal dark background
    // Vivid bright green box for 'ON'
    private static final int COLOR_ENABLED_BOX = 0xFF32A852;  // Hypixel Vivid Green

    public ConfigScreen(Screen parent) {
        super(Component.literal("Casual Skyblock Addons"));
        this.parent = parent;
        buildCategories();
    }

    private void buildCategories() {
        ModConfig cfg = ModConfig.get();

        ConfigCategory general = new ConfigCategory("General")
                .toggle("Rarity Backgrounds",
                        () -> cfg.showRarityBackgrounds,
                        value -> cfg.showRarityBackgrounds = value);

        categories.add(general);
    }

    // New helper method to generate dynamic button labels (ALWAYS FULL WHITE)
    private Component getButtonText(ConfigCategory.ToggleEntry entry) {
        boolean value = entry.getter.getAsBoolean();
        String suffix = value ? " [ON]" : " [OFF]";
        // and the text must be full white (by default with no formatting)
        return Component.literal(entry.label + suffix);
    }

    @Override
    protected void init() {
        super.init();
        int startX = (width - (categories.size() * (COLUMN_WIDTH + COLUMN_GAP))) / 2;

        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = startX + col * (COLUMN_WIDTH + COLUMN_GAP);
            for (int row = 0; row < category.entries.size(); row++) {
                ConfigCategory.ToggleEntry entry = category.entries.get(row);
                int y = TOP_MARGIN + 20 + row * ROW_HEIGHT;

                Button button = Button.builder(
                        // 1. Text must be white
                        getButtonText(entry),
                        btn -> {
                            boolean newValue = !entry.getter.getAsBoolean();
                            entry.setter.accept(newValue);
                            ModConfig.save();
                            // 2. Text must update
                            btn.setMessage(getButtonText(entry));
                        }
                ).bounds(x, y, COLUMN_WIDTH, ROW_HEIGHT - 2).build();

                addRenderableWidget(button);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Overlay background over the game scene so UI is readable
        context.fillGradient(0, 0, this.width, this.height, 0xD0101010, 0xE0151515);

        // --- Render CUSTOM BOX COLOR Around ON/OFF State ---
        int startX = (width - (categories.size() * (COLUMN_WIDTH + COLUMN_GAP))) / 2;

        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = startX + col * (COLUMN_WIDTH + COLUMN_GAP);

            for (int row = 0; row < category.entries.size(); row++) {
                ConfigCategory.ToggleEntry entry = category.entries.get(row);
                int y = TOP_MARGIN + 20 + row * ROW_HEIGHT;

                // 3. Determine color: keep default dark for OFF, use green for ON
                boolean isEnabled = entry.getter.getAsBoolean();
                int boxColor = isEnabled ? COLOR_ENABLED_BOX : COLOR_DISABLED_BOX;

                // Draw the colored container box (the 'frame') just outside the button bounds
                // Use fillGradient for a clean, professional looking box
                renderColoredButtonBox(context, x, y, COLUMN_WIDTH, ROW_HEIGHT - 2, boxColor);
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);

        // --- Render Headers ---
        startX = (width - (categories.size() * (COLUMN_WIDTH + COLUMN_GAP))) / 2;
        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = startX + col * (COLUMN_WIDTH + COLUMN_GAP);

            context.fill(x, TOP_MARGIN, x + COLUMN_WIDTH, TOP_MARGIN + 18, 0xFF202020);
            context.centeredText(font, category.name, x + COLUMN_WIDTH / 2, TOP_MARGIN + 5, 0xFFFFFF);
        }
    }

    // Professional helper method to render the professional looking frame box
    private static void renderColoredButtonBox(GuiGraphicsExtractor context, int x, int y, int width, int height, int colorARGB) {
        // Use fillGradient for a professionally colored rectangle frame around the button
        // We render it 1 pixel outside standard button bounds so it fully surrounds the default background
        int outerX = x - 1;
        int outerY = y - 1;
        int outerX2 = x + width + 1;
        int outerY2 = y + height + 1;

        // Draw the main color (slight gradient looks clean in Minecraft UI)
        context.fillGradient(outerX, outerY, outerX2, outerY2, colorARGB, colorARGB);

        // Add a clean 1-pixel dark border frame outside the colored box for definition
        context.fill(outerX - 1, outerY - 1, outerX2 + 1, outerY, 0xFF101010); // Top
        context.fill(outerX - 1, outerY2, outerX2 + 1, outerY2 + 1, 0xFF101010); // Bottom
        context.fill(outerX - 1, outerY, outerX, outerY2, 0xFF101010); // Left
        context.fill(outerX2, outerY, outerX2 + 1, outerY2, 0xFF101010); // Right
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}