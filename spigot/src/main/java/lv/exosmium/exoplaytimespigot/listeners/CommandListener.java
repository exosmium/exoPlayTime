package lv.exosmium.exoplaytimespigot.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lv.exosmium.exoplaytimespigot.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {
    private final Main instance;

    public CommandListener(Main instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String[] cmd = event.getMessage().split(" ");
        Player player = event.getPlayer();

        if (cmd.length != 2) return;
        if (cmd[0].equals("/velocityreward")) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.write("reward".getBytes());
            out.write(" ".getBytes());
            out.write(cmd[1].getBytes());
            out.write(" ".getBytes());
            out.write(player.getName().getBytes());
            player.sendPluginMessage(instance, "velocity:hook-cmd", out.toByteArray());
            event.setCancelled(true);
        }
    }
}
