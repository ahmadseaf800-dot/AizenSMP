package com.aizen.smp;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        Bukkit.getScheduler().runTaskTimer(plugin, this::snapshotStats, 20L, 200L);
        plugin.getLogger().info("Dashboard bridge enabled.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sendEvent("join", event.getPlayer().getName(), "Connection", "Online", "");
        snapshotStats();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sendEvent("quit", event.getPlayer().getName(), "Connection", "Offline", "");
        snapshotStats();
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        String reason = event.getReason() == null || event.getReason().isBlank()
                ? "No reason provided" : event.getReason();
        sendEvent("kick", event.getPlayer().getName(), "Server Kick", "Kicked", reason);
        snapshotStats();
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        inspectBanCommand(event.getMessage());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        inspectBanCommand("/" + event.getCommand());
    }

    private void inspectBanCommand(String command) {
        String raw = command == null ? "" : command.trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String lower = raw.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("ban ") || lower.startsWith("minecraft:ban ")
                || lower.startsWith("tempban ") || lower.startsWith("ban-ip "))) return;

        String[] parts = raw.split("\\s+", 3);
        if (parts.length < 2) return;

        String player = parts[1];
        String reason = parts.length >= 3 && !parts[2].isBlank() ? parts[2] : "No reason provided";
        sendEvent("ban", player, "Ban", "Banned", reason);
    }

    private void snapshotStats() {
        if (url.isEmpty()) return;

        List<Player> snapshot = new ArrayList<>(Bukkit.getOnlinePlayers());
        StringBuilder players = new StringBuilder("[");
        boolean first = true;
        for (Player p : snapshot) {
            if (!first) players.append(",");
            first = false;
            players.append("{\"player\":\"")
                    .append(escape(p.getName()))
                    .append("\",\"status\":\"Online\",\"violations\":0}");
        }
        players.append("]");

        String json = "{\"type\":\"stats\",\"online\":"
                + snapshot.size()
                + ",\"players\":" + players
                + ",\"time\":\"" + escape(Instant.now().toString()) + "\"}";
        post(json);
    }

    public void sendEvent(String type, String player, String detection, String action, String reason) {
        if (url.isEmpty()) return;
        String json = "{\"type\":\"" + escape(type)
                + "\",\"player\":\"" + escape(player)
                + "\",\"detection\":\"" + escape(detection)
                + "\",\"action\":\"" + escape(action)
                + "\",\"reason\":\"" + escape(reason)
                + "\",\"time\":\"" + Instant.now() + "\"}";
        post(json);
    }

    private void post(String json) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection connection =
                        (HttpURLConnection) URI.create(url + "/api/event").toURL().openConnection();
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