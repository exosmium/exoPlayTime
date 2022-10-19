package lv.exosmium.exoplaytimespigot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class PluginMessaging implements PluginMessageListener {
    private final String serverName;

    public PluginMessaging(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velocity:runcmd")) {
            return;
        }
        String[] translatedMessages = new String(message).split(":");
        if (translatedMessages[0].equals(serverName)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), translatedMessages[1]);
        }
    }
}
