package com.jinzo;

import com.jinzo.commands.BountyCommand;
import com.jinzo.listeners.BountyDeathListener;
import com.jinzo.utils.BountyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import com.jinzo.utils.ConfigManager;

public class BountyHunt extends JavaPlugin {

    private static BountyHunt instance;
    private Economy economy;
    private BountyManager bountyManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault or a compatible economy plugin not found! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        bountyManager = new BountyManager(this, economy);
        bountyManager.loadBounties();

        getServer().getPluginManager().registerEvents(new BountyDeathListener(bountyManager), this);
        getCommand("bounty").setExecutor(new BountyCommand(bountyManager, configManager));
        getCommand("bounty").setTabCompleter(new BountyCommand(bountyManager, configManager));
    }

    @Override
    public void onDisable() {
        bountyManager.saveBounties();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static BountyHunt getInstance() {
        return instance;
    }
}