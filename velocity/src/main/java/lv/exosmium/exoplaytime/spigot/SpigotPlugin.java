package lv.exosmium.exoplaytime.spigot;

import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeSpigot;
import lv.exosmium.exoplaytime.Reward;
import lv.exosmium.exoplaytime.mysql.SQLManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class SpigotPlugin extends JavaPlugin {
    private FileConfiguration config;
    private SQLManager sqlManager;
    private List<Reward> rewards = new ArrayList<>();
    private SockExchangeSpigot sockExchangeSpigot;

    public static Logger logger;

    public void onEnable() {
        setupConfig();
        setupRewards();
        this.logger = getLogger();
        setupMysql();
        new Expansion(sqlManager).register();
        this.getCommand("reward").setExecutor(new CommandListener(sqlManager, config, rewards));
        sockExchangeSpigot = new SockExchangeSpigot(this);
        sockExchangeSpigot.loadSocket();
        sockExchangeSpigot.initSocket();
    }

    public void onDisable() {
        sockExchangeSpigot.disableSocket();
    }

    private void setupConfig() {
        this.saveDefaultConfig();
        config = this.getConfig();
    }

    private void setupMysql() {
        this.sqlManager = new SQLManager(config, this);
    }

    private void setupRewards() {
        for (String id : config.getConfigurationSection("Rewards").getKeys(false)) {
            String path = "Rewards." + id + ".";
            List<String> actions = config.getStringList(path + "actions");
            List<String> titles = config.getStringList(path + "title");
            HashMap<String, Object> requiredTime = new HashMap<>();
            for (String server : config.getConfigurationSection(path + "required-time").getKeys(false)) {
                String timePath = path + "required-time." + server;
                requiredTime.put(server, config.getInt(timePath));
            }
            Reward reward = new Reward(id, actions, titles, requiredTime);
            rewards.add(reward);
        }
    }
}
