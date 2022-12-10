package lv.exosmium.exoplaytime.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ProxyServer;

public class ChannelListener {
    private final ProxyServer server;

    public ChannelListener(ProxyServer server) {
        this.server = server;
    }

    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
        String translatedMessage = new String(event.getData());
        server.getCommandManager().executeAsync(server.getConsoleCommandSource(), translatedMessage);
    }
}
