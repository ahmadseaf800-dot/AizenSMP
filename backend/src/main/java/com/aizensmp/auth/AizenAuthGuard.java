package com.aizensmp.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AizenAuthGuard extends JavaPlugin implements Listener {
    private final Map<UUID, Float> oldSpeed = new HashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private String dashboardUrl = "";
    private String dashboardToken = "";
    private String serverName = "unknown";

    @Override
    public void onEnable() {
        dashboardUrl = env("DASHBOARD_URL");
        dashboardToken = env("DASHBOARD_API_TOKEN");
        serverName = env("AIZEN_SERVER_NAME");
        if (serverName.isBlank()) serverName = getConfig().getString("server-name", "unknown");

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isAuthenticated(p)) unlock(p); else lock(p);
            }
        }, 1L, 10L);

        if (!dashboardUrl.isBlank() && !dashboardToken.isBlank()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::publishStats, 20L, 100L);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::pollCommands, 40L, 60L);
            getLogger().info("AizenSMP Dashboard bridge enabled for server: " + serverName);
        } else {
            getLogger().warning("Dashboard bridge disabled: DASHBOARD_URL or DASHBOARD_API_TOKEN is missing.");
        }
    }

    private String env(String key) {
        return String.valueOf(System.getenv().getOrDefault(key, "")).trim();
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
        if (!isAuthenticated(p)) lock(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isAuthenticated(p) && e.getTo() != null) {
            if (e.getFrom().getX() != e.getTo().getX()
                    || e.getFrom().getY() != e.getTo().getY()
                    || e.getFrom().getZ() != e.getTo().getZ()) e.setTo(e.getFrom());
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

    private void publishStats() {
        List<String> players = new ArrayList<>();
        List<String> admins = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = json(p.getName());
            players.add("{\"player\":\"" + name + "\",\"status\":\"Online\",\"op\":" + p.isOp() + ",\"violations\":0,\"lastDetection\":\"\"}");
            if (p.isOp() || hasRolePermission(p)) {
                admins.add("{\"player\":\"" + name + "\",\"op\":" + p.isOp()
                        + ",\"role\":\"" + json(roleOf(p)) + "\",\"permissions\":" + permissionsJson(p) + "}");
            }
        }
        String body = "{\"type\":\"stats\",\"server\":\"" + json(serverName) + "\",\"online\":"
                + Bukkit.getOnlinePlayers().size() + ",\"players\":[" + String.join(",", players)
                + "],\"admins\":[" + String.join(",", admins) + "],\"time\":\"" + json(Instant.now().toString()) + "\"}";
        post("/api/event", body);
    }

    private boolean hasRolePermission(Player p) {
        for (String role : List.of("owner","admin","developer","moderator","support","staff","player")) {
            if (p.hasPermission("aizensmp.role." + role)) return true;
        }
        return false;
    }

    private String roleOf(Player p) {
        String[] roles = {"owner","admin","developer","moderator","support","staff","player"};
        for (String role : roles) if (p.hasPermission("aizensmp.role." + role)) return role;
        return p.isOp() ? "OP" : "Player";
    }

    private String permissionsJson(Player p) {
        List<String> permissions = new ArrayList<>();
        p.getEffectivePermissions().forEach(info -> {
            if (info.getValue() && permissions.size() < 100) permissions.add(json(info.getPermission()));
        });
        return "[" + String.join(",", permissions) + "]";
    }

    private void pollCommands() {
        String url = dashboardUrl.replaceAll("/+$", "") + "/api/commands?server="
                + java.net.URLEncoder.encode(serverName, StandardCharsets.UTF_8);
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + dashboardToken)
                    .header("Accept", "application/json")
                    .GET().build();

            HttpResponse<String> response = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return;

            JsonObject payload = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray commands = payload.has("commands") && payload.get("commands").isJsonArray()
                    ? payload.getAsJsonArray("commands") : new JsonArray();

            for (var element : commands) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                String id = item.has("id") ? item.get("id").getAsString() : "";
                String command = item.has("command") ? item.get("command").getAsString() : "";
                if (id.isBlank() || command.isBlank()) continue;
                Bukkit.getScheduler().runTask(this, () -> executeQueuedCommand(id, command));
            }
        } catch (Exception ignored) {
        }
    }

    private void executeQueuedCommand(String id, String command) {
        boolean success = false;
        try {
            success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Throwable error) {
            getLogger().warning("Dashboard command failed: " + error.getMessage());
        }

        String result = "{\"id\":\"" + json(id) + "\",\"success\":" + success
                + ",\"server\":\"" + json(serverName) + "\",\"command\":\"" + json(command) + "\"}";
        post("/api/command-result", result);

        post("/api/event", "{\"type\":\"command\",\"server\":\"" + json(serverName)
                + "\",\"action\":\"" + (success ? "completed" : "failed")
                + "\",\"reason\":\"Dashboard AI command\",\"time\":\"" + json(Instant.now().toString()) + "\"}");
    }

    private void post(String endpoint, String body) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(dashboardUrl.replaceAll("/+$", "") + endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + dashboardToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private String json(String value) {
        return String.valueOf(value == null ? "" : value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
