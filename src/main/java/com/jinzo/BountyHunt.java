package com.jinzo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class BountyHunt extends JavaPlugin implements Listener, TabExecutor {

    private final Map<UUID, Integer> bounties = new HashMap<>();
    private final Gson gson = new Gson();
    private File dataFile;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("bounty").setExecutor(this);
        getCommand("bounty").setTabCompleter(this);

        getCommand("bountytop").setExecutor(this);
        getCommand("bountycheck").setExecutor(this);

        dataFile = new File(getDataFolder(), "bounties.json");
        if (!dataFile.exists()) {
            try {
                getDataFolder().mkdirs();
                dataFile.createNewFile();
                saveBounties();
            } catch (IOException e) {
                getLogger().warning("Failed to create bounty data file");
            }
        }

        loadBounties();
    }

    @Override
    public void onDisable() {
        saveBounties();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();

        if (killer == null || killer.equals(dead)) return;

        UUID deadUUID = dead.getUniqueId();
        if (!bounties.containsKey(deadUUID)) return;

        int reward = bounties.remove(deadUUID);
        saveBounties();

        giveGoldReward(killer, reward);
        killer.sendMessage(ChatColor.GOLD + "You claimed a bounty of " +
                reward + " gold (" + (reward / 9) + " blocks & " + (reward % 9) + " ingots) for killing " +
                dead.getName() + "!");

        Bukkit.broadcastMessage(ChatColor.RED + dead.getName() + " was killed! " +
                "Bounty of " + reward + " gold claimed by " + killer.getName() + "!");
    }

    private void giveGoldReward(Player player, int amount) {
        int blocks = amount / 9;
        int ingots = amount % 9;

        List<ItemStack> items = new ArrayList<>();
        if (blocks > 0) items.add(new ItemStack(Material.GOLD_BLOCK, blocks));
        if (ingots > 0) items.add(new ItemStack(Material.GOLD_INGOT, ingots));

        for (ItemStack item : items) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private boolean takeGold(Player player, int amount) {
        int totalIngots = countGold(player);

        if (totalIngots < amount) return false;

        int toRemove = amount;

        toRemove = removeGold(player.getInventory(), toRemove);
        if (toRemove > 0) {
            removeGold(player.getEnderChest(), toRemove);
        }

        return true;
    }

    private int countGold(Player player) {
        return countGoldInInv(player.getInventory()) + countGoldInInv(player.getEnderChest());
    }

    private int countGoldInInv(Inventory inv) {
        int total = 0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            if (item.getType() == Material.GOLD_INGOT) total += item.getAmount();
            else if (item.getType() == Material.GOLD_BLOCK) total += item.getAmount() * 9;
        }
        return total;
    }

    private int removeGold(Inventory inv, int toRemove) {
        int removed = 0;

        // First, try removing gold ingots
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != Material.GOLD_INGOT) continue;

            int amt = item.getAmount();
            if (amt <= toRemove) {
                inv.setItem(i, null);
                toRemove -= amt;
                removed += amt;
            } else {
                item.setAmount(amt - toRemove);
                inv.setItem(i, item);
                removed += toRemove;
                toRemove = 0;
                return 0;
            }

            if (toRemove == 0) return 0;
        }

        // Then use gold blocks and convert them to ingots
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != Material.GOLD_BLOCK) continue;

            int blocks = item.getAmount();
            while (blocks > 0 && toRemove > 0) {
                // Break one block into 9 ingots
                blocks--;
                toRemove -= 9;
                removed += 9;
            }

            if (blocks == 0) {
                inv.setItem(i, null);
            } else {
                item.setAmount(blocks);
                inv.setItem(i, item);
            }

            // If we overpaid, refund the extra as ingots
            if (toRemove < 0) {
                int refund = -toRemove;
                toRemove = 0;
                inv.addItem(new ItemStack(Material.GOLD_INGOT, refund));
                return 0;
            }

            if (toRemove == 0) return 0;
        }

        return toRemove; // Remaining if not enough gold overall
    }

    private void saveBounties() {
        try (FileWriter writer = new FileWriter(dataFile)) {
            gson.toJson(bounties, writer);
        } catch (IOException e) {
            getLogger().warning("Failed to save bounties!");
            e.printStackTrace();
        }
    }

    private void loadBounties() {
        try (FileReader reader = new FileReader(dataFile)) {
            Type type = new TypeToken<Map<UUID, Integer>>() {}.getType();
            Map<UUID, Integer> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                bounties.clear();
                bounties.putAll(loaded);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to load bounties!");
            e.printStackTrace();
        }
    }

    // /bounty <player> <amount>
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "bounty" -> {
                if (args.length != 2) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /bounty <player> <amount>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(ChatColor.RED + "Player not found or offline.");
                    return true;
                }

                if (target.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You can't place a bounty on yourself.");
                    return true;
                }

                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Amount must be a number.");
                    return true;
                }

                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "Amount must be greater than zero.");
                    return true;
                }

                if (!takeGold(player, amount)) {
                    player.sendMessage(ChatColor.RED + "You don't have enough gold ingots or blocks.");
                    return true;
                }

                bounties.put(target.getUniqueId(), bounties.getOrDefault(target.getUniqueId(), 0) + amount);
                saveBounties();

                Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " placed a bounty of " + amount +
                        " gold on " + target.getName() + "!");
                return true;
            }

            case "bountytop" -> {
                List<Map.Entry<UUID, Integer>> sorted = bounties.entrySet().stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .limit(10)
                        .toList();

                player.sendMessage(ChatColor.GOLD + "Top 10 Bounties:");
                int rank = 1;
                for (Map.Entry<UUID, Integer> entry : sorted) {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());
                    player.sendMessage(ChatColor.YELLOW + "#" + rank++ + " " +
                            ChatColor.RED + target.getName() + ": " +
                            ChatColor.GOLD + entry.getValue() + " gold");
                }
                return true;
            }

            case "bountycheck" -> {
                if (args.length != 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /bountycheck <player>");
                    return true;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
                int amount = bounties.getOrDefault(target.getUniqueId(), 0);

                if (amount <= 0) {
                    player.sendMessage(ChatColor.GREEN + target.getName() + " has no bounty.");
                } else {
                    player.sendMessage(ChatColor.GOLD + target.getName() + " has a bounty of " +
                            amount + " gold.");
                }
                return true;
            }

            default -> {
                return false;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return Collections.emptyList();
    }
}
