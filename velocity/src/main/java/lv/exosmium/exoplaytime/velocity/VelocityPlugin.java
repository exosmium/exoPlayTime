package lv.exosmium.exoplaytime.velocity;

import com.gmail.tracebachi.SockExchange.Velocity.SockExchangeVelocity;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lv.exosmium.exoplaytime.Reward;
import lv.exosmium.exoplaytime.mysql.SQLManager;
import org.simpleyaml.configuration.file.YamlFile;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Plugin(
        id = "exoplaytime",
        name = "lv.exosmium.exoplaytime.velocity.Velocity",
        version = "1.0",
        authors = {"Exosmium"}
)
public class VelocityPlugin {
    private final ProxyServer server;
    private final Path dataDirectory;
    private SQLManager sqlManager;
    private YamlFile yamlConfig;
    private List<Reward> rewards = new ArrayList<>();
    private String yamlName = "velocity-config.yml";
    public static Logger logger;
    private SockExchangeVelocity sockExchangeVelocity;

    @Inject
    public VelocityPlugin(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
    }


    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        setupConfig();
        setupMysql();
        setupListeners();
        sockExchangeVelocity = new SockExchangeVelocity(server, logger);
        sockExchangeVelocity.initSocket();
        logger.info("§4§nCoded by§8:§r §cExosmium§7 (vk.com/prodbyhakin)");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        sockExchangeVelocity.disableSocket();
    }

    private void setupListeners() {
        server.getEventManager().register(this, new ConnectionListener(sqlManager));
        server.getEventManager().register(this, new ChannelListener(server));
        server.getChannelRegistrar().register(MinecraftChannelIdentifier.create("velocity", "hook-cmd"));
    }

    private void setupMysql() {
        this.sqlManager = new SQLManager(yamlConfig, server);
    }

    private void setupConfig() {
        try {
            this.yamlConfig = new YamlFile(dataDirectory + File.separator + yamlName);
            if (!yamlConfig.exists()) {
                yamlConfig.createNewFile();
                writeDefaultConfig();
            }
            yamlConfig.load(dataDirectory + File.separator + yamlName);
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    private void writeDefaultConfig() throws IOException {
        InputStream stream = this.getClass().getResourceAsStream("/" + yamlName);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
        OutputStream outputStream = new FileOutputStream(yamlConfig.getConfigurationFile());
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

        bufferedOutputStream.write(bufferedInputStream.readAllBytes());
        bufferedOutputStream.close();
        outputStream.close();
        bufferedInputStream.close();
    }
}
