package com.aizensmp.proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

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

}
