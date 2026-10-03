package com.aizensmp.auth;

import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AizenAuthGuard extends JavaPlugin implements Listener {
    private final Map<UUID, Float> oldSpeed = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isAuthenticated(p)) {
                    unlock(p);
                } else {
                    lock(p);
                }
            }
        }, 1L, 10L);
    }

    private boolean isAuthenticated(Player p) {
        try {
            return AuthMeApi.getInstance().isAuthenticated(p.getName());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void lock(Player p) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        oldSpeed.putIfAbsent(p.getUniqueId(), p.getWalkSpeed());
        p.setWalkSpeed(0.0f);
        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 255, false, false, false));
        p.closeInventory();
        p.setFoodLevel(20);
        p.setFireTicks(0);
    }

    private void unlock(Player p) {
        Float speed = oldSpeed.remove(p.getUniqueId());
        if (speed != null) p.setWalkSpeed(speed);
        p.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!isAuthenticated(p)) {
            lock(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isAuthenticated(p) && e.getTo() != null) {
            if (e.getFrom().getX() != e.getTo().getX()
                    || e.getFrom().getY() != e.getTo().getY()
                    || e.getFrom().getZ() != e.getTo().getZ()) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        oldSpeed.remove(e.getPlayer().getUniqueId());
    }
}
