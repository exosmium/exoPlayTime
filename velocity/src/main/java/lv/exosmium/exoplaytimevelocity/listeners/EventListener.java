package lv.exosmium.exoplaytimevelocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import lv.exosmium.exoplaytimevelocity.managers.DatabaseManager;

import java.util.HashMap;

public class EventListener {
    private final DatabaseManager sqLite;
    private HashMap<Player, Long> timeData = new HashMap<>();
    private HashMap<Player, String> lastServerData = new HashMap<>();

    public EventListener(DatabaseManager sqLite) {
        this.sqLite = sqLite;
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String lastServerName = event.getServer().getServerInfo().getName();
        if (!timeData.containsKey(player) && !lastServerData.containsKey(player)){
            timeData.put(player, System.currentTimeMillis());
            lastServerData.put(player, lastServerName);
            return;
        }

        long currentTime = System.currentTimeMillis();
        String username = player.getUsername();
        sqLite.addPlaytime(username, lastServerData.get(player), currentTime - timeData.get(player));
        timeData.replace(player, currentTime);
        lastServerData.replace(player, lastServerName);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        if (timeData.containsKey(player) && lastServerData.containsKey(player)) {
            long currentTime = System.currentTimeMillis();
            String username = player.getUsername();

            sqLite.addPlaytime(username, lastServerData.get(player), currentTime - timeData.get(player));
            timeData.remove(username);
            lastServerData.remove(username);
        }
    }
}
