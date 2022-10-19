package lv.exosmium.exoplaytimespigot;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "velocity:runcmd",
                new PluginMessaging(getConfig().getString("Settings.server-name")));
        System.out.println("§4§nCoded by§8:§r §cExosmium§7 (vk.com/prodbyhakin)");
    }
}
