package lv.exosmium.exoplaytime.spigot;

import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import lv.exosmium.exoplaytime.Reward;
import lv.exosmium.exoplaytime.mysql.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandListener implements CommandExecutor {
    private final ConsoleCommandSender consoleCommandSender;
    private final SQLManager sqlManager;
    private final FileConfiguration config;
    private final List<Reward> rewards;

    public CommandListener(SQLManager sqlManager, FileConfiguration config, List<Reward> rewards) {
        this.sqlManager = sqlManager;
        this.config = config;
        this.rewards = rewards;
        this.consoleCommandSender = Bukkit.getConsoleSender();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(ChatColor.RED + "Данная команда доступна только от имени игрока!");
        }

        if (args.length == 1) {
            Reward rewardToTake;
            try {
                rewardToTake = rewards.get(Integer.parseInt(args[0]) - 1);
            } catch (Exception ignored) {
                sender.sendMessage(translateColorCodes(String.valueOf(config.getString("Messages.does-not-exist"))));
                return true;
            }
            rewardPlayer((Player) sender, rewardToTake);
        }
        return true;
    }

    private void dispatchCommand(String command) {
        Bukkit.dispatchCommand(consoleCommandSender, command);
    }


    private void rewardPlayer(Player player, Reward rewardToTake) {
        String username = player.getName();
        if (!enoughTimePlayed(player, rewardToTake)) {
            sendCooldownMessage(player);
            return;
        }
        if (sqlManager.hadClaimed(username, rewardToTake.getId())) {
            sendExistMessage(player);
            return;
        }


        String currentServerName = SockExchangeApi.instance().getServerName();
        for (String command : rewardToTake.getActions()) {
            String serverName = command.split("]")[0].split("\\[")[1];

            String commandToSend = command.replace("{player}", username).split("\\]")[1].replaceFirst("^\\s*", "");
            if (currentServerName.equals(serverName)) {
                dispatchCommand(commandToSend);
            } else {
                SockExchangeApi.instance().sendCommandsToServers(List.of(commandToSend), Collections.singletonList(serverName));
            }
        }
        sendTitle(player, translateColorCodes(rewardToTake.getTitle().get(0)), translateColorCodes(rewardToTake.getTitle().get(1)), 20, 20, 20);
        player.sendMessage(translateColorCodes(config.getString("Messages.given")));
        try {
            sqlManager.addClaimed(username, rewardToTake.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCooldownMessage(Player player) {
        ArrayList<String> title = getListFromString(String.valueOf(config.getString("Messages.cooldown-title")));
        sendTitle(player, translateColorCodes(title.get(0)), translateColorCodes(title.get(1)), 20, 20, 20);
        player.sendMessage(translateColorCodes(String.valueOf(config.getString("Messages.cooldown"))));
    }

    private void sendExistMessage(Player player) {
        ArrayList<String> title = getListFromString(String.valueOf(config.getString("Messages.exist-title")));
        sendTitle(player, translateColorCodes(title.get(0)), translateColorCodes(title.get(1)), 20, 20, 20);
        player.sendMessage(translateColorCodes(String.valueOf(config.getString("Messages.exist"))));
    }

    private ArrayList<String> getListFromString(String str) {
        return new ArrayList<>(Arrays.asList(String.valueOf(str).replace("[", "").replace("]", "").split(", ")));
    }

    private boolean enoughTimePlayed(Player player, Reward reward) {
        HashMap<String, Long> playerGlobalPlaytime = sqlManager.getGlobalPlaytime(player.getName());
        for (Object server : reward.getRequiredTime().keySet()) {
            Long playtimeOnServer = playerGlobalPlaytime.get(server);
            long requiredPlaytimeOnServer = Long.parseLong(reward.getRequiredTime().get(server).toString());
            if (playtimeOnServer == null || playtimeOnServer / 60 < requiredPlaytimeOnServer) return false;
        }
        return true;
    }

    private String translateColorCodes(String toTranslate) {
        return ChatColor.translateAlternateColorCodes('&', toTranslate);
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(translateColorCodes(title), translateColorCodes(subtitle), fadeIn, stay, fadeOut);
    }
}
