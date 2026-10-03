package com.aizen.smp;

import org.bukkit.plugin.java.JavaPlugin;

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
