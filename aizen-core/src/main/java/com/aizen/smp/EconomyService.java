package com.aizen.smp;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class EconomyService implements CommandExecutor {
    private final AizenCore plugin;
    private final Map<UUID, Double> balances = new HashMap<>();
    public EconomyService(AizenCore plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.isConfigurationSection("balances")) {
            for (String key : cfg.getConfigurationSection("balances").getKeys(false)) {
                try { balances.put(UUID.fromString(key), cfg.getDouble("balances." + key)); } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Double> e : balances.entrySet()) plugin.getConfig().set("balances." + e.getKey(), e.getValue());
        plugin.saveConfig();
    }

    public double get(UUID id) { return balances.getOrDefault(id, 0.0); }
    public void add(UUID id, double amount) { balances.put(id, get(id) + amount); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("balance")) {
            if (!(sender instanceof Player p)) return true;
            sender.sendMessage("§6AizenSMP §7| §eBalance: §a$" + String.format(Locale.US, "%.2f", get(p.getUniqueId())));
            return true;
        }
        if (!(sender instanceof Player p)) return true;
        if (args.length != 2) {
            p.sendMessage("§cUsage: /pay <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (Exception e) {
            p.sendMessage("§cInvalid amount."); return true;
        }
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            p.sendMessage("§cInvalid amount."); return true;
        }
        if (get(p.getUniqueId()) < amount) { p.sendMessage("§cInsufficient balance."); return true; }
        add(p.getUniqueId(), -amount);
        add(target.getUniqueId(), amount);
        p.sendMessage("§aPaid $" + String.format(Locale.US, "%.2f", amount) + " to " + target.getName());
        target.sendMessage("§aYou received $" + String.format(Locale.US, "%.2f", amount) + " from " + p.getName());
        return true;
    }
}
