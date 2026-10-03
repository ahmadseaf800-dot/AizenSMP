package com.aizensmp.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(id = "aizenproxy", name = "AizenProxy", version = "1.0.0")
public final class AizenProxy {
    private final ProxyServer proxy;

    @Inject
    public AizenProxy(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        proxy.getScheduler().buildTask(this, () -> {
            if (!player.isActive()) return;
            Optional<RegisteredServer> germany = proxy.getServer("Germany");
            if (germany.isPresent() && player.getCurrentServer().isEmpty()) {
                player.createConnectionRequest(germany.get()).connect();
            }
        }).delay(800, TimeUnit.MILLISECONDS).schedule();
    }

    private static final class RtpCommand implements SimpleCommand {
        private final ProxyServer proxy;
        RtpCommand(ProxyServer proxy) { this.proxy = proxy; }

        @Override
        public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                invocation.source().sendMessage(Component.text("This command is for players only.", NamedTextColor.RED));
                return;
            }
            String[] args = invocation.arguments();
            if (args.length == 0) {
                player.sendMessage(Component.text("✦ AizenSMP Regions ✦", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                sendRegion(player, "EUROPE", "Germany", "eu");
                sendRegion(player, "ASIA", "Turkey", "as");
                sendRegion(player, "AMERICA", "America", "us");
                player.sendMessage(Component.text("Click a region to connect instantly.", NamedTextColor.GRAY));
                return;
            }
            String key = args[0].toLowerCase(Locale.ROOT);
            String target = switch (key) {
                case "eu", "europe", "de", "germany" -> "Germany";
                case "as", "asia", "tr", "turkey" -> "Turkey";
                case "us", "america", "na" -> "America";
                default -> null;
            };
            if (target == null) {
                player.sendMessage(Component.text("Use /rtp, /rtp eu, /rtp as, or /rtp us", NamedTextColor.YELLOW));
                return;
            }
            connect(player, target);
        }

        private void sendRegion(Player player, String label, String serverName, String command) {
            RegisteredServer server = proxy.getServer(serverName).orElse(null);
            int count = server == null ? 0 : server.getPlayersConnected().size();
            Component line = Component.text("▸ " + label + "  ", NamedTextColor.AQUA)
                    .append(Component.text("[" + count + " players]", NamedTextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand("/rtp " + command))
                    .hoverEvent(HoverEvent.showText(Component.text("Connect to " + serverName, NamedTextColor.YELLOW)));
            player.sendMessage(line);
        }

        private void connect(Player player, String name) {
            Optional<RegisteredServer> server = proxy.getServer(name);
            if (server.isEmpty()) {
                player.sendMessage(Component.text("Region server is offline: " + name, NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("Connecting to " + name + "...", NamedTextColor.YELLOW));
            player.createConnectionRequest(server.get()).connect().thenAccept(result -> {
                if (!result.isSuccessful()) {
                    player.sendMessage(Component.text("Could not connect to " + name + ".", NamedTextColor.RED));
                }
            });
        }
    }
}
