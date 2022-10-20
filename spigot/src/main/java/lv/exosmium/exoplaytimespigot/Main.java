package lv.exosmium.exoplaytimespigot;

import lv.exosmium.exoplaytimespigot.listeners.CommandListener;
import lv.exosmium.exoplaytimespigot.listeners.MessagingListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "velocity:run-cmd",
                new MessagingListener(getConfig().getString("Settings.server-name")));
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "velocity:hook-cmd");
        this.getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        System.out.println("§4§nCoded by§8:§r §cExosmium§7 (vk.com/prodbyhakin)");
    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }
}
