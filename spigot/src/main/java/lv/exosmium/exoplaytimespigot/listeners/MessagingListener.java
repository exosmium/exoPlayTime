package lv.exosmium.exoplaytimespigot.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class MessagingListener implements PluginMessageListener {
    private final String serverName;

    public MessagingListener(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velocity:run-cmd")) {
            return;
        }
        String[] translatedMessages = new String(message).split(":");
        if (translatedMessages[0].equals(serverName)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), translatedMessages[1]);
        }
    }
}
