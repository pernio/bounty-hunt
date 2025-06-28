package com.jinzo.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jinzo.BountyHunt;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class BountyManager {
    private final Map<UUID, Integer> bounties = new HashMap<>();
    private final Gson gson = new Gson();
    private final File dataFile;
    private final File logFile;
    private final Economy economy;

    public BountyManager(BountyHunt plugin, Economy economy) {
        this.economy = economy;
        this.dataFile = new File(plugin.getDataFolder(), "bounties.json");
        this.logFile = new File(plugin.getDataFolder(), "bounty.log");

        try {
            plugin.getDataFolder().mkdirs();
            if (!dataFile.exists()) dataFile.createNewFile();
            if (!logFile.exists()) logFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create bounty files.");
        }
    }

    public void loadBounties() {
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, Integer>>() {}.getType();
            Map<UUID, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                bounties.clear();
                bounties.putAll(loaded);
            }
        } catch (IOException e) {
            BountyHunt.getInstance().getLogger().warning("Failed to load bounties!");
        }
    }

    public void saveBounties() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(bounties, writer);
        } catch (IOException e) {
            BountyHunt.getInstance().getLogger().warning("Failed to save bounties!");
        }
    }

    public void log(String message) {
        FileUtil.logToFile(logFile, message);
    }

    public Map<UUID, Integer> getBounties() {
        return bounties;
    }

    public int getBounty(UUID uuid) {
        return bounties.getOrDefault(uuid, 0);
    }

    public void addBounty(UUID uuid, int amount) {
        bounties.put(uuid, getBounty(uuid) + amount);
        saveBounties();
    }

    public int claimBounty(UUID uuid) {
        int amount = bounties.remove(uuid);
        saveBounties();
        return amount;
    }

    public void removeBounty(UUID uuid) {
        bounties.remove(uuid);
        saveBounties();
    }

    public Economy getEconomy() {
        return economy;
    }
}