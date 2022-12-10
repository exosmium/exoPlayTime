package lv.exosmium.exoplaytime.spigot;

import lv.exosmium.exoplaytime.mysql.SQLManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;

public class Expansion extends PlaceholderExpansion {
    private final SQLManager sqlManager;

    public Expansion(SQLManager sqlManager) {
        this.sqlManager = sqlManager;
    }
    @Override
    public String getAuthor() {
        return "Exosmium";
    }

    @Override
    public String getIdentifier() {
        return "exoplaytime";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.contains("time_played_")) {
            String serverName = params.split("time_played_")[1];
            HashMap<String, Long> playedTime = sqlManager.getGlobalPlaytime(player.getName());
            if (!playedTime.isEmpty() && playedTime.get(serverName) != null) {
                return convertSecondsToHMmSs(playedTime.get(serverName) / 1000);
            }
        }
        return "0h 0m";
    }

    private String convertSecondsToHMmSs(long seconds) {
        long m = (seconds / 60) % 60;
        long h = (seconds / (60 * 60)) % 24;
        return String.format("%dh %02dm", h, m);
    }
}
