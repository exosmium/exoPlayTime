package lv.exosmium.exoplaytimevelocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;

public class ProxyListener {
    private final ProxyServer server;

    public ProxyListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
        String translatedMessage = new String(event.getData());
        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), translatedMessage);
    }
}
