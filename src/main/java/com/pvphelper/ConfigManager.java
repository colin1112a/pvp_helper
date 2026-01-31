package com.pvphelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pvp_helper.json");
    private static PvpHelperConfig config = new PvpHelperConfig();

    private ConfigManager() {
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
                PvpHelperConfig loaded = GSON.fromJson(reader, PvpHelperConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException e) {
                System.err.println("[PvpHelper] Failed to load config: " + e.getMessage());
            }
        }

        config.clamp();
        save();
    }

    public static PvpHelperConfig get() {
        return config;
    }

    public static void save(PvpHelperConfig updated) {
        if (updated == null) {
            return;
        }
        updated.clamp();
        config = updated;
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            System.err.println("[PvpHelper] Failed to save config: " + e.getMessage());
        }
    }
}
