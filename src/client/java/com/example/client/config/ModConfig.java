package com.example.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {

    //   GSON & CONFIG FILE INSTANCE FIELDS
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("casualskyblockaddons.json");

    private static ModConfig instance;

    // --- settings go here ---
    public boolean showRarityBackgrounds = true;

    // --- Get Active Config Instance ---
    public static ModConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    // --- Load Settings from JSON File ---
    public static void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                instance = GSON.fromJson(json, ModConfig.class);
            } catch (IOException e) {
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
        }
        if (instance == null) instance = new ModConfig();
    }

    // --- Save Current Settings to JSON File ---
    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(instance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}