package com.aizen.smp;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.PlayerDeathEvent;
import java.util.*;

public final class BountyService implements Listener, org.bukkit.command.CommandExecutor {
    private final AizenCore plugin;
    private final EconomyService economy;
    private final Map<UUID,Integer> streaks = new HashMap<>();
    private final Map<UUID,Double> bounties = new HashMap<>();

    public BountyService(AizenCore plugin, EconomyService economy) {
        this.plugin = plugin; this.economy = economy;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        UUID kid = killer.getUniqueId();
        int streak = streaks.getOrDefault(kid, 0) + 1;
        streaks.put(kid, streak);

        if (streak >= 5) {
            double bounty = 1000.0 + ((streak - 5) * 1000.0);
            bounties.put(kid, bounty);
            killer.sendMessage("§6Bounty active: §e$" + String.format(Locale.US, "%.0f", bounty));
        }

        streaks.remove(victim.getUniqueId());
        bounties.remove(victim.getUniqueId());
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (bounties.isEmpty()) {
            sender.sendMessage("§7No active bounties.");
            return true;
        }
        sender.sendMessage("§6=== AizenSMP Bounties ===");
        bounties.entrySet().stream()
            .sorted(Map.Entry.<UUID,Double>comparingByValue().reversed())
            .limit(10)
            .forEach(e -> {
                Player p = plugin.getServer().getPlayer(e.getKey());
                String name = p != null ? p.getName() : "Offline";
                sender.sendMessage("§e" + name + " §7- §a$" + String.format(Locale.US, "%.0f", e.getValue()));
            });
        return true;
    }
}
