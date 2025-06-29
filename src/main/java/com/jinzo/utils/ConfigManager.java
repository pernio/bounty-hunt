package com.jinzo.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        this.config = plugin.getConfig();
    }

    public int getBountyTaxPercentage() {
        return config.getInt("bounty-tax-percentage", 10);
    }

    public int getBountyCooldownSeconds() {
        return config.getInt("bounty-cooldown-seconds", 60);
    }

    public int getBountyMinimumAmount() {
        return config.getInt("bounty-minimum-amount", 1);
    }

    public int getBountyMaximumAmount() {
        return config.getInt("bounty-max-amount", 1_000_000);
    }
}
