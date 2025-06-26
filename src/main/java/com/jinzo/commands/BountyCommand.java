package com.jinzo.commands;

import com.jinzo.BountyHunt;
import com.jinzo.utils.BountyManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BountyCommand implements TabExecutor {

    private final BountyManager bountyManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public BountyCommand(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /bounty <set|check|top> ...");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(player, args);
            case "check" -> handleCheck(player, args);
            case "top" -> handleTop(player);
            default -> player.sendMessage(ChatColor.RED + "Unknown subcommand. Use: set, check, or top.");
        }
        return true;
    }

    private void handleSet(Player player, String[] args) {
        if (!player.hasPermission("bountyhunt.bounty.set")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to set bounties.");
            return;
        }

        if (args.length != 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /bounty set <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player " + args[1] + " not found.");
            return;
        }

        if (!target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Player " + target.getName() + " is not online.");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot place a bounty on yourself.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Amount must be a number.");
            return;
        }

        int min = BountyHunt.getInstance().getConfig().getInt("bounty-minimum-amount", 0);
        int max = BountyHunt.getInstance().getConfig().getInt("bounty-max-amount", 1000000);
        if (amount < min || amount > max) {
            player.sendMessage(ChatColor.RED + "Amount must be between " + min + " and " + max);
            return;
        }

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        int cooldown = BountyHunt.getInstance().getConfig().getInt("bounty-cooldown-seconds", 60);
        if ((now - last) < cooldown * 1000L) {
            player.sendMessage(ChatColor.RED + "Wait before setting another bounty.");
            return;
        }

        double taxRate = BountyHunt.getInstance().getConfig().getDouble("bounty-tax-percentage", 0);
        int tax = (int) Math.ceil(amount * taxRate / 100.0);
        int cost = amount + tax;

        if (!bountyManager.getEconomy().has(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " gold to place this bounty.");
            return;
        }

        bountyManager.getEconomy().withdrawPlayer(player, cost);
        bountyManager.addBounty(target.getUniqueId(), amount);
        cooldowns.put(player.getUniqueId(), now);

        Bukkit.broadcastMessage(ChatColor.GOLD + player.getName() + " placed a bounty of " + amount + " gold on " + target.getName() + "!");
        bountyManager.log(player.getName() + " placed a bounty of " + amount + " gold on " + target.getName());
    }

    private void handleTop(Player player) {
        if (!player.hasPermission("bountyhunt.bounty.top")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        AtomicInteger rank = new AtomicInteger(1);
        player.sendMessage(ChatColor.GOLD + "Top 10 Bounties:");
        bountyManager.getBounties().entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(entry -> {
                    String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    player.sendMessage(ChatColor.YELLOW + "#" + rank.getAndIncrement() + " - " + name + ": " + entry.getValue());
                });
    }

    private void handleCheck(Player player, String[] args) {
        if (!player.hasPermission("bountyhunt.bounty.check")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /bounty check <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        int bounty = bountyManager.getBounty(target.getUniqueId());
        if (bounty > 0) {
            player.sendMessage(ChatColor.GOLD + target.getName() + " has a bounty of " + bounty + " gold.");
        } else {
            player.sendMessage(ChatColor.GREEN + target.getName() + " has no bounty.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1)
            return Arrays.asList("set", "check", "top");
        else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("check")))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return Collections.emptyList();
    }
}
