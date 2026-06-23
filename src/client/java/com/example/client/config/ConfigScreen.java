package com.example.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
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

        // Add more categories the same way:
        // ConfigCategory dungeons = new ConfigCategory("Dungeons")
        //         .toggle("Some Feature", () -> cfg.someFeature, v -> cfg.someFeature = v);
        // categories.add(dungeons);
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
                        Component.literal(entry.label),
                        btn -> {
                            boolean newValue = !entry.getter.getAsBoolean();
                            entry.setter.accept(newValue);
                            ModConfig.save();
                            btn.setMessage(Component.literal(entry.label));
                        }
                ).bounds(x, y, COLUMN_WIDTH, ROW_HEIGHT - 2).build();

                addRenderableWidget(button);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int startX = (width - (categories.size() * (COLUMN_WIDTH + COLUMN_GAP))) / 2;

        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = startX + col * (COLUMN_WIDTH + COLUMN_GAP);

            context.fill(x, TOP_MARGIN, x + COLUMN_WIDTH, TOP_MARGIN + 18, 0xFF202020);
            context.centeredText(font, category.name, x + COLUMN_WIDTH / 2, TOP_MARGIN + 5, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}