package lv.exosmium.exoplaytime.mysql;

import com.velocitypowered.api.proxy.ProxyServer;
import lv.exosmium.exoplaytime.spigot.SpigotPlugin;
import lv.exosmium.exoplaytime.velocity.VelocityPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.simpleyaml.configuration.file.YamlFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SQLManager {
    private SQLBase mySQL;

    String rewardTableQuery = "CREATE TABLE IF NOT EXISTS "
            + "REWARDS"
            + " (`id` bigint(32) NOT NULL AUTO_INCREMENT, "
            + "`username` NVARCHAR(36) NOT NULL, `reward` NVARCHAR(32) NOT NULL, "
            + "PRIMARY KEY (`id`), UNIQUE KEY `id` (`id`));";
    String playtimeTableQuery = "CREATE TABLE IF NOT EXISTS "
            + "PLAYTIME"
            + " (`id` bigint(32) NOT NULL AUTO_INCREMENT, "
            + "`username` NVARCHAR(36) NOT NULL, `server` NVARCHAR(32) NOT NULL, `time_played` NVARCHAR(32) NOT NULL, "
            + "PRIMARY KEY (`id`), UNIQUE KEY `id` (`id`));";

    public SQLManager(YamlFile yamlConfig, ProxyServer server) {
        String database = yamlConfig.getString("Database.database");
        String address = yamlConfig.getString("Database.address");
        String username = yamlConfig.getString("Database.username");
        String password = yamlConfig.getString("Database.password");
        if (!password.equals("123")) {
            mySQL = new SQLBase(database, address, username, password);
            mySQL.executeUpdate(rewardTableQuery);
            mySQL.executeUpdate(playtimeTableQuery);
        } else {
            VelocityPlugin.logger.error("§cОшибка инициализации плагина!");
            VelocityPlugin.logger.error("§cНастройте конфигурацию MySQL.");
            server.shutdown(Component.text("§cНеправильная конфигурация exoPlayTime!"));
        }
    }

    public SQLManager(FileConfiguration yamlConfig, SpigotPlugin plugin) {
        String database = yamlConfig.getString("Database.database");
        String address = yamlConfig.getString("Database.address");
        String username = yamlConfig.getString("Database.username");
        String password = yamlConfig.getString("Database.password");
        if (!password.equals("123")) {
            mySQL = new SQLBase(database, address, username, password);
            mySQL.executeUpdate(rewardTableQuery);
            mySQL.executeUpdate(playtimeTableQuery);
        } else {
            plugin.logger.severe("§cОшибка инициализации плагина!");
            plugin.logger.severe("§cНастройте конфигурацию MySQL.");
            plugin.getServer().shutdown();
        }
    }

    public void addPlaytime(String username, String server, long playtime) {
        long oldPlaytime = getPlaytime(username, server);
        if (oldPlaytime == 0) {
            mySQL.executeUpdate(String.format("INSERT INTO PLAYTIME (`username`, `server`, `time_played`) VALUES ('%s', '%s', '%s');", username, server, playtime));
        } else {
            long newPlaytime = oldPlaytime + playtime;
            mySQL.executeUpdate(String.format("UPDATE PLAYTIME SET `time_played`='%s' WHERE `server`='%s' AND `username`='%s';", newPlaytime, server, username));
        }
    }

    private long getPlaytime(String username, String server) {
        List<Map<String, Object>> resultSet = mySQL.executeQuery(String.format("SELECT * FROM PLAYTIME WHERE `username`='%s' AND `server`='%s';", username, server));
        if (!resultSet.isEmpty()) {
            return Long.valueOf(resultSet.get(0).get("time_played").toString());
        }
        return 0;
    }

    public HashMap<String, Long> getGlobalPlaytime(String username) {
        HashMap<String, Long> globalPlaytime = new HashMap<>();
        List<Map<String, Object>> resultSet = mySQL.executeQuery(String.format("SELECT * FROM PLAYTIME WHERE `username`='%s';", username));
        for (Map<String, Object> map : resultSet) {
            globalPlaytime.put(map.get("server").toString(), Long.valueOf(map.get("time_played").toString()));
        }
        return globalPlaytime;
    }

    public void addClaimed(String username, String rewardId) {
        mySQL.executeUpdate(String.format("INSERT INTO REWARDS (`username`, `reward`) VALUES ('%s', '%s');", username, rewardId));
    }

    public boolean hadClaimed(String username, String rewardId) {
        List<Map<String, Object>> resultSet = mySQL.executeQuery(String.format("SELECT * FROM REWARDS WHERE `username`='%s' AND `reward`='%s';", username, rewardId));
        return !resultSet.isEmpty();
    }
}
