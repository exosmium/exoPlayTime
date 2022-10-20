package lv.exosmium.exoplaytimevelocity;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lv.exosmium.exoplaytimevelocity.listeners.CommandListener;
import lv.exosmium.exoplaytimevelocity.listeners.EventListener;
import lv.exosmium.exoplaytimevelocity.listeners.ProxyListener;
import lv.exosmium.exoplaytimevelocity.managers.ClaimManager;
import lv.exosmium.exoplaytimevelocity.managers.DatabaseManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Plugin(
        id = "exoplaytimevelocity",
        name = "lv.exosmium.exoplaytimevelocity.Main",
        version = "1.0",
        authors = {"Exosmium"}
)
public class Main {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private DatabaseManager sqLite;
    private ClaimManager claimManager;

    private List<Reward> configRewards = new ArrayList<>();
    private Map<String, Object> configMessages;



    @Inject
    public Main(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException {
        this.claimManager = new ClaimManager(dataDirectory, "data.json");
        setupConfig();
        setupSqlite();
        setupCommand();
        System.out.println("§4§nCoded by§8:§r §cExosmium§7 (vk.com/prodbyhakin)");
        server.getEventManager().register(this, new EventListener(sqLite));
        server.getEventManager().register(this, new ProxyListener(server));
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.create("velocity", "hook-cmd"));
    }

    private void setupConfig() {
        Toml config = loadConfig(dataDirectory);
        if (config == null) {
            logger.warn("Ошибка загрузки config.toml. Выключение...");
        }
        configMessages = config.getTable("messages").toMap();
        long rewardCount = config.getTable(("settings")).getLong("reward-count");
        for (int i = 1; i <= rewardCount; i++) {
            Toml rewardTable = config.getTable(String.valueOf(i));
            configRewards.add(new Reward(i,
                    rewardTable.getList("actions"),
                    rewardTable.getList("title"),
                    rewardTable.getTable("required-time").toMap()));
        }
    }

    private void setupSqlite() {
        try {
            this.sqLite = new DatabaseManager(dataDirectory + File.separator);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setupCommand() {
        CommandManager commandManager = server.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("reward").plugin(this).build();
        SimpleCommand commandToRegister = new CommandListener(sqLite, server, claimManager, configRewards, configMessages);
        commandManager.register(commandMeta, commandToRegister);
    }

    private Toml loadConfig(final Path path) {
        final File folder = path.toFile();
        final File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                final InputStream input = Main.class.getResourceAsStream( "/" + file.getName());
                try {
                    if (input != null) Files.copy(input, file.toPath());
                    else file.createNewFile();
                    if (input != null) input.close();
                }
                catch (Throwable t) {
                    if (input != null) {
                        try { input.close(); }
                        catch (Throwable exception2) {
                            t.addSuppressed(exception2);
                        }} throw t;
                }
            }
            catch (IOException exception) { exception.printStackTrace(); return null; }
        }
        return new Toml().read(file);
    }
}
