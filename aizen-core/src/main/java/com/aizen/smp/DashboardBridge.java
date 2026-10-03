package com.aizen.smp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DashboardBridge implements Listener {
    private final JavaPlugin plugin;
    private final String url;
    private final String token;

    public DashboardBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        this.url = plugin.getConfig().getString("dashboard.url", "").trim();
        this.token = plugin.getConfig().getString("dashboard.token", "").trim();
    }

    public void start() {
        if (url.isEmpty()) {
            plugin.getLogger().warning("Dashboard API URL is empty; dashboard bridge is disabled.");
            return;
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sendStats, 20L, 200L);
        plugin.getLogger().info("Dashboard bridge enabled: " + url);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sendEvent("join", event.getPlayer().getName(), "Player joined", "Online");
        sendStats();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sendEvent("quit", event.getPlayer().getName(), "Player left", "Offline");
        sendStats();
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        sendEvent("kick", event.getPlayer().getName(), "Server kick", "Kicked");
        sendStats();
    }

    public void sendEvent(String type, String player, String detection, String action) {
        if (url.isEmpty()) return;
        String json = "{"
                + "\"type\":\"" + escape(type) + "\","
                + "\"player\":\"" + escape(player) + "\","
                + "\"detection\":\"" + escape(detection) + "\","
                + "\"action\":\"" + escape(action) + "\","
                + "\"time\":\"" + Instant.now() + "\"}";
        post(json);
    }

    public void sendStats() {
        if (url.isEmpty()) return;
        List<String> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            players.add("\"player\":\"" + escape(p.getName()) + "\",\"status\":\"Online\",\"violations\":0");
        }
        String playerJson = "[" + String.join("},{", players) + "]";
        String json = "{\"type\":\"stats\",\"online\":" + Bukkit.getOnlinePlayers().size()
                + ",\"players\":[" + (players.isEmpty() ? "" : "{" + playerJson.substring(1, playerJson.length()-1) + "}") + "]}";
        post(json);
    }

    private void post(String json) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(url + "/api/event").toURL().openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(json.getBytes(StandardCharsets.UTF_8));
                }
                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception ignored) {
                // Dashboard outages must never affect the Minecraft server.
            }
        });
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}