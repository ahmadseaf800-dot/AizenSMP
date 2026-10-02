package com.aizen.smp;

import org.bukkit.plugin.java.JavaPlugin;

public final class AizenCore extends JavaPlugin {
    private EconomyService economy;
    private BountyService bounty;

    @Override
    public void onEnable() {
        economy = new EconomyService(this);
        bounty = new BountyService(this, economy);

        getCommand("balance").setExecutor(economy);
        getCommand("pay").setExecutor(economy);
        getCommand("bounty").setExecutor(bounty);

        getServer().getPluginManager().registerEvents(bounty, this);
        getLogger().info("AizenCore enabled - custom systems loaded from source.");
    }

    public EconomyService economy() { return economy; }
}
