package com.jinzo.listeners;

import com.jinzo.utils.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class BountyDeathListener implements Listener {

    private final BountyManager bountyManager;

    public BountyDeathListener(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        if (killer == null || killer.equals(dead)) return;

        UUID targetId = dead.getUniqueId();
        if (!bountyManager.getBounties().containsKey(targetId)) return;

        int reward = bountyManager.claimBounty(targetId);
        bountyManager.getEconomy().depositPlayer(killer, reward);

        killer.sendMessage(ChatColor.YELLOW + "You claimed a bounty of " + reward + " gold for killing " + dead.getName() + "!");
        Bukkit.broadcastMessage(ChatColor.GOLD + dead.getName() + " was killed! Bounty of " + reward + " gold claimed by " + killer.getName() + "!");
        bountyManager.log(killer.getName() + " killed " + dead.getName() + " and earned " + reward + " gold.");
    }
}
