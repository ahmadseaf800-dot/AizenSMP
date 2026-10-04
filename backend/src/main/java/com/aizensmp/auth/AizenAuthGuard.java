package com.aizensmp.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.server.ServerCommandEvent;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AizenAuthGuard extends JavaPlugin implements Listener {
    private final Map<UUID, Float> oldSpeed = new HashMap<>();
    private final Map<String, StaffSnapshot> staffCache = new LinkedHashMap<>();
    private final Map<String, BanSnapshot> knownBans = new HashMap<>();
    private final Map<String, DetectionSnapshot> detections = new HashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private String dashboardUrl = "";
    private String dashboardToken = "";
    private String serverName = "unknown";
    private boolean ownerInitialized = false;

    private static final Set<String> DASHBOARD_COMMANDS = Set.of(
            "kick", "ban", "tempban", "ipban", "tempipban", "unban", "pardon",
            "mute", "tempmute", "unmute", "op", "deop", "giveop", "makeop", "operator", "whitelist"
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dashboardUrl = env("DASHBOARD_URL");
        dashboardToken = env("DASHBOARD_API_TOKEN");
        serverName = env("AIZEN_SERVER_NAME");
        if (serverName.isBlank()) serverName = getConfig().getString("server-name", "unknown");

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("aizenac").setExecutor(new InternalDetectionCommand());

        Bukkit.getScheduler().runTaskLater(this, this::ensureOwner, 40L);
        Bukkit.getScheduler().runTaskTimer(this, this::ensureOwner, 1200L, 1200L);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isAuthenticated(p)) unlock(p); else lock(p);
            }
        }, 1L, 10L);

        if (!dashboardUrl.isBlank() && !dashboardToken.isBlank()) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::publishStats, 20L, 100L);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::pollCommands, 40L, 60L);
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::scanBans, 60L, 100L);
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
            return AuthMeApi.getInstance().isAuthenticated(p);
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
        if (p.getName().equalsIgnoreCase("Aizenx")) {
            p.setOp(true);
            getLogger().info("Aizenx owner OP enforced on " + serverName + ".");
        }
        cacheStaff(p);
        if (!isAuthenticated(p)) lock(p);
    }

    private void ensureOwner() {
        Player owner = Bukkit.getPlayerExact("Aizenx");
        if (owner != null && !owner.isOp()) owner.setOp(true);
        if (!ownerInitialized) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "op Aizenx");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp creategroup owner");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp group owner permission set * true");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp group owner meta setprefix 100 \"&6&lOWNER &f\"");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user Aizenx parent set owner");
            ownerInitialized = true;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isAuthenticated(p) && e.getTo() != null &&
                (e.getFrom().getX() != e.getTo().getX()
                        || e.getFrom().getY() != e.getTo().getY()
                        || e.getFrom().getZ() != e.getTo().getZ())) {
            e.setTo(e.getFrom());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && !isAuthenticated(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        if (!isAuthenticated(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        oldSpeed.remove(e.getPlayer().getUniqueId());
        cacheStaff(e.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        post("/api/event", eventJson("kick", e.getPlayer().getName(), "PlayerKickEvent",
                cleanReason(e.getReason()), "", "server"));
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        String raw = e.getCommand() == null ? "" : e.getCommand().trim();
        String root = commandRoot(raw);
        if (Set.of("ban","tempban","ipban","tempipban","unban","pardon","kick","op","deop","mute","tempmute","unmute").contains(root)) {
            post("/api/event", eventJson(root, extractTarget(raw), "ServerCommand",
                    extractReason(raw), extractDuration(raw), e.getSender().getName()));
        }
    }

    private void cacheStaff(Player p) {
        if (!(p.isOp() || hasRolePermission(p))) return;
        String role = roleOf(p);
        List<String> permissions = new ArrayList<>();
        p.getEffectivePermissions().forEach(info -> {
            if (info.getValue() && permissions.size() < 100) permissions.add(info.getPermission());
        });
        staffCache.put(p.getUniqueId().toString(),
                new StaffSnapshot(p.getUniqueId().toString(), p.getName(), p.isOnline(), p.isOp(), role, permissions));
    }

    private void publishStats() {
        for (Player p : Bukkit.getOnlinePlayers()) cacheStaff(p);

        List<String> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            DetectionSnapshot d = detections.get(p.getName().toLowerCase());
            players.add("{\"player\":\"" + json(p.getName()) + "\",\"status\":\"Online\",\"op\":" + p.isOp()
                    + ",\"violations\":" + (d == null ? 0 : d.vl)
                    + ",\"lastDetection\":\"" + json(d == null ? "" : d.reason) + "\"}");
        }

        List<String> admins = new ArrayList<>();
        for (StaffSnapshot s : staffCache.values()) {
            admins.add(s.toJson());
        }

        String body = "{\"type\":\"stats\",\"server\":\"" + json(serverName) + "\",\"online\":"
                + Bukkit.getOnlinePlayers().size() + ",\"players\":[" + String.join(",", players)
                + "],\"admins\":[" + String.join(",", admins) + "],\"time\":\"" + json(Instant.now().toString()) + "\"}";
        post("/api/event", body);
    }

    private boolean hasRolePermission(Player p) {
        for (String role : List.of("owner","admin","developer","moderator","support","staff")) {
            if (p.hasPermission("aizensmp.role." + role)) return true;
        }
        return false;
    }

    private String roleOf(Player p) {
        String[] roles = {"owner","admin","developer","moderator","support","staff"};
        for (String role : roles) if (p.hasPermission("aizensmp.role." + role)) return role;
        return p.isOp() ? "OP" : "Player";
    }

    private void scanBans() {
        try {
            BanList list = Bukkit.getBanList(BanList.Type.NAME);
            Map<String, BanSnapshot> current = new HashMap<>();
            for (Object raw : list.getBanEntries()) {
                if (!(raw instanceof BanEntry entry) || entry.getTarget() == null) continue;
                BanSnapshot b = new BanSnapshot(entry.getTarget(), entry.getReason(), entry.getSource(),
                        entry.getCreated(), entry.getExpiration());
                current.put(entry.getTarget().toLowerCase(), b);
            }

            if (!knownBans.isEmpty()) {
                for (Map.Entry<String, BanSnapshot> e : current.entrySet()) {
                    if (!knownBans.containsKey(e.getKey())) {
                        BanSnapshot b = e.getValue();
                        post("/api/event", eventJson("ban", b.target, "BanList",
                                b.reason, b.durationText(), b.source));
                    }
                }
            }
            knownBans.clear();
            knownBans.putAll(current);
        } catch (Throwable ignored) {
        }
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
                String command = item.has("command") ? item.get("command").getAsString().trim() : "";
                String target = item.has("target") ? item.get("target").getAsString() : "";
                String reason = item.has("reason") ? item.get("reason").getAsString() : "";
                String duration = item.has("duration") ? item.get("duration").getAsString() : "";
                if (id.isBlank() || command.isBlank()) continue;
                Bukkit.getScheduler().runTask(this, () -> executeQueuedCommand(id, command, target, reason, duration));
            }
        } catch (Exception ignored) {
        }
    }

    private void executeQueuedCommand(String id, String command, String target, String reason, String duration) {
        String root = commandRoot(command);
        // The website may send a structured command ("op") plus a separate target.
        // Normalize that form before dispatching so the AI does not get a false "not found".
        if ((root.equals("op") || root.equals("giveop") || root.equals("makeop") || root.equals("operator"))
                && !root.equals("op")) {
            command = "op" + (target.isBlank() ? "" : " " + target);
            root = "op";
        } else if (root.equals("op") && command.trim().equalsIgnoreCase("op") && !target.isBlank()) {
            command = "op " + target;
        } else if (root.equals("deop") && command.trim().equalsIgnoreCase("deop") && !target.isBlank()) {
            command = "deop " + target;
        }

        // Aizenx is the permanent owner. Never allow a dashboard command
        // to remove the owner's OP status, even if the AI/dashboard requests /deop.
        if (root.equals("deop") && command.matches("(?i)deop\\s+Aizenx\\s*")) {
            command = "op Aizenx";
            root = "op";
        }

        boolean allowed = DASHBOARD_COMMANDS.contains(root);
        boolean success = false;
        String resultReason = allowed ? "" : "Command not allowed by AizenAuthGuard allowlist";

        if (allowed) {
            try {
                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Throwable error) {
                resultReason = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
                getLogger().warning("Dashboard command failed: " + resultReason);
            }
        }

        String result = "{\"id\":\"" + json(id) + "\",\"success\":" + success
                + ",\"server\":\"" + json(serverName) + "\",\"command\":\"" + json(command)
                + "\",\"reason\":\"" + json(resultReason) + "\"}";
        post("/api/command-result", result);

        post("/api/event", eventJson(allowed ? root : "blocked-command",
                target, "AIZEN Dashboard", reason.isBlank() ? resultReason : reason, duration, "AIZEN AI"));
    }

    private String commandRoot(String command) {
        String c = command.trim();
        while (c.startsWith("/")) c = c.substring(1);
        int space = c.indexOf(' ');
        return (space < 0 ? c : c.substring(0, space)).toLowerCase();
    }

    private String extractTarget(String command) {
        String[] parts = command.trim().replaceFirst("^/+", "").split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    private String extractDuration(String command) {
        String[] parts = command.trim().replaceFirst("^/+", "").split("\\s+");
        if (parts.length < 3) return "";
        String root = parts[0].toLowerCase();
        if (root.startsWith("temp")) return parts[2];
        return "";
    }

    private String extractReason(String command) {
        String[] parts = command.trim().replaceFirst("^/+", "").split("\\s+");
        if (parts.length < 3) return "";
        int start = rootNeedsDuration(parts[0]) ? 3 : 2;
        if (parts.length <= start) return "";
        return String.join(" ", java.util.Arrays.copyOfRange(parts, start, parts.length));
    }

    private boolean rootNeedsDuration(String root) {
        String r = root.toLowerCase();
        return r.equals("tempban") || r.equals("tempipban") || r.equals("tempmute");
    }

    private String cleanReason(String reason) {
        return reason == null ? "" : reason.replaceAll("§[0-9A-FK-ORa-fk-or]", "").replace("\n", " ").trim();
    }

    private String eventJson(String action, String player, String source, String reason, String duration, String issuer) {
        return "{\"type\":\"audit\",\"server\":\"" + json(serverName)
                + "\",\"action\":\"" + json(action) + "\",\"player\":\"" + json(player)
                + "\",\"issuer\":\"" + json(issuer) + "\",\"source\":\"" + json(source)
                + "\",\"reason\":\"" + json(cleanReason(reason)) + "\",\"duration\":\"" + json(duration)
                + "\",\"time\":\"" + json(Instant.now().toString()) + "\"}";
    }

    public void recordDetection(String player, int vl, String reason) {
        if (player == null || player.isBlank()) return;
        detections.put(player.toLowerCase(), new DetectionSnapshot(vl, reason));
    }

    private void post(String endpoint, String body) {
        if (dashboardUrl.isBlank() || dashboardToken.isBlank()) return;
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

    private final class InternalDetectionCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.getName().equalsIgnoreCase("CONSOLE")) return false;
            if (args.length < 3 || !args[0].equalsIgnoreCase("flag")) return false;
            try {
                int vl = Integer.parseInt(args[2]);
                recordDetection(args[1], vl, String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length)));
            } catch (Exception ignored) {
            }
            return true;
        }
    }

    private record DetectionSnapshot(int vl, String reason) {}
    private record StaffSnapshot(String uuid, String name, boolean online, boolean op, String role, List<String> permissions) {
        String toJson() {
            List<String> ps = new ArrayList<>();
            for (String p : permissions) ps.add("\"" + p.replace("\\","\\\\").replace("\"","\\\"") + "\"");
            return "{\"uuid\":\"" + uuid + "\",\"player\":\"" + name + "\",\"online\":" + online
                    + ",\"op\":" + op + ",\"role\":\"" + role + "\",\"permissions\":[" + String.join(",", ps) + "]}";
        }
    }
    private record BanSnapshot(String target, String reason, String source, java.util.Date created, java.util.Date expiration) {
        String durationText() {
            if (expiration == null) return "Permanent";
            if (created == null) return expiration.toString();
            long ms = expiration.getTime() - created.getTime();
            if (ms <= 0) return "Expired";
            long minutes = ms / 60000L;
            if (minutes < 60) return minutes + "m";
            long hours = minutes / 60;
            if (hours < 24) return hours + "h";
            return (hours / 24) + "d";
        }
    }
}
