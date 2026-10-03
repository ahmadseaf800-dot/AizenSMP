package com.aizen.smp;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.Duration;
import java.time.Instant;

public final class AizenCore extends JavaPlugin {
    private EconomyService economy;
    private BountyService bounty;
    private DashboardBridge dashboard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        economy = new EconomyService(this);
        bounty = new BountyService(this, economy);
        dashboard = new DashboardBridge(this);

        getCommand("balance").setExecutor(economy);
        getCommand("pay").setExecutor(economy);
        getCommand("bounty").setExecutor(bounty);
        getCommand("aizenautoban").setExecutor((sender, command, label, args) -> {\n            if (args.length < 3) { sender.sendMessage("Usage: /aizenautoban <player> <days> <reason>"); return true; }\n            String player = args[0]; int days;\n            try { days = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ex) { sender.sendMessage("Invalid days."); return true; }\n            StringBuilder reason = new StringBuilder(); for (int i=2;i<args.length;i++) { if (i>2) reason.append(" "); reason.append(args[i]); }\n            Instant expires = Instant.now().plus(Duration.ofDays(days));\n            Bukkit.getBanList(BanList.Type.NAME).addBan(player, reason.toString(), java.util.Date.from(expires), "AizenSMP Anti-Cheat");\n            var target = Bukkit.getPlayerExact(player); if (target != null) target.kickPlayer("AizenSMP Anti-Cheat\\n" + reason);\n            sender.sendMessage("Auto-ban applied to " + player + " for " + days + " days."); return true;\n        });

        getServer().getPluginManager().registerEvents(bounty, this);
        getServer().getPluginManager().registerEvents(dashboard, this);

        dashboard.start();
        getLogger().info("AizenCore enabled - custom systems loaded from source.");
    }

    @Override
    public void onDisable() {
        if (economy != null) economy.save();
    }

    public EconomyService economy() {
        return economy;
    }

    public DashboardBridge dashboard() {
        return dashboard;
    }
}
