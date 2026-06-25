package com.example.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends Screen {

    private final List<ConfigCategory> categories = new ArrayList<>();
    private final Map<String, Boolean> collapsed = new HashMap<>();
    private static final int COLUMN_WIDTH = 100;
    private static final int ROW_HEIGHT = 14;
    private static final int COLUMN_GAP = 10;
    private static final int TOP_MARGIN = 10;
    private static final int LEFT_MARGIN = 10;

    private final Screen parent;

    private static final int COLOR_DISABLED_BOX = 0xFF353535;
    private static final int COLOR_ENABLED_BOX = 0xFF32A852;
    private static final int COLOR_HEADER_NORMAL = 0xFF202020;
    private static final int COLOR_HEADER_COLLAPSED = 0xFF00BFFF;

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

        ConfigCategory mining = new ConfigCategory("Mining")
                .toggle("Chest Solver",
                        () -> cfg.PowderChestSolver,
                        value -> cfg.PowderChestSolver = value);

        categories.add(general);
        categories.add(mining);

        // all start open
        for (ConfigCategory cat : categories) {
            collapsed.put(cat.name, false);
        }
    }

    private Component getButtonText(ConfigCategory.ToggleEntry entry) {
        boolean value = entry.getter.getAsBoolean();
        String suffix = value ? " [ON]" : " [OFF]";
        return Component.literal(entry.label + suffix);
    }

    @Override
    protected void init() {
        super.init();

        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = LEFT_MARGIN + col * (COLUMN_WIDTH + COLUMN_GAP);
            boolean isCollapsed = collapsed.getOrDefault(category.name, false);

            // header button
            String catName = category.name;
            Button header = Button.builder(
                    Component.literal(category.name),
                    btn -> {
                        collapsed.put(catName, !collapsed.getOrDefault(catName, false));
                        rebuildWidgets();
                    }
            ).bounds(x, TOP_MARGIN, COLUMN_WIDTH, ROW_HEIGHT - 2).build();
            addRenderableWidget(header);

            // only show toggle buttons if not collapsed
            if (!isCollapsed) {
                for (int row = 0; row < category.entries.size(); row++) {
                    ConfigCategory.ToggleEntry entry = category.entries.get(row);
                    int y = TOP_MARGIN + 20 + row * ROW_HEIGHT;

                    Button button = Button.builder(
                            getButtonText(entry),
                            btn -> {
                                boolean newValue = !entry.getter.getAsBoolean();
                                entry.setter.accept(newValue);
                                ModConfig.save();
                                btn.setMessage(getButtonText(entry));
                            }
                    ).bounds(x, y, COLUMN_WIDTH, ROW_HEIGHT - 2).build();

                    addRenderableWidget(button);
                }
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fillGradient(0, 0, this.width, this.height, 0x60101010, 0x60151515);

        // render colored boxes for toggle buttons
        for (int col = 0; col < categories.size(); col++) {
            ConfigCategory category = categories.get(col);
            int x = LEFT_MARGIN + col * (COLUMN_WIDTH + COLUMN_GAP);
            boolean isCollapsed = collapsed.getOrDefault(category.name, false);

            // header box color
            int headerColor = isCollapsed ? COLOR_HEADER_COLLAPSED : COLOR_HEADER_NORMAL;
            renderColoredButtonBox(context, x, TOP_MARGIN, COLUMN_WIDTH, ROW_HEIGHT - 2, headerColor);

            if (!isCollapsed) {
                for (int row = 0; row < category.entries.size(); row++) {
                    ConfigCategory.ToggleEntry entry = category.entries.get(row);
                    int y = TOP_MARGIN + 20 + row * ROW_HEIGHT;

                    boolean isEnabled = entry.getter.getAsBoolean();
                    int boxColor = isEnabled ? COLOR_ENABLED_BOX : COLOR_DISABLED_BOX;
                    renderColoredButtonBox(context, x, y, COLUMN_WIDTH, ROW_HEIGHT - 2, boxColor);
                }
            }
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private static void renderColoredButtonBox(GuiGraphicsExtractor context, int x, int y, int width, int height, int colorARGB) {
        int outerX = x - 1;
        int outerY = y - 1;
        int outerX2 = x + width + 1;
        int outerY2 = y + height + 1;

        context.fillGradient(outerX, outerY, outerX2, outerY2, colorARGB, colorARGB);

        context.fill(outerX - 1, outerY - 1, outerX2 + 1, outerY, 0xFF101010);
        context.fill(outerX - 1, outerY2, outerX2 + 1, outerY2 + 1, 0xFF101010);
        context.fill(outerX - 1, outerY, outerX, outerY2, 0xFF101010);
        context.fill(outerX2, outerY, outerX2 + 1, outerY2, 0xFF101010);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}