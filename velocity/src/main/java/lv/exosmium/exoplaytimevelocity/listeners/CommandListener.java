package lv.exosmium.exoplaytimevelocity.listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lv.exosmium.exoplaytimevelocity.Reward;
import lv.exosmium.exoplaytimevelocity.managers.ClaimManager;
import lv.exosmium.exoplaytimevelocity.managers.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.time.temporal.ChronoUnit.SECONDS;

public final class CommandListener implements SimpleCommand {
    private final DatabaseManager sqLite;
    private final ClaimManager claimManager;
    private final List<Reward> configRewards;
    private final Map<String, Object> configMessages;

    public CommandListener(DatabaseManager sqLite, ClaimManager claimManager, List<Reward> configRewards, Map<String, Object> configMessages) {
        this.sqLite = sqLite;
        this.claimManager = claimManager;
        this.configRewards = configRewards;
        this.configMessages = configMessages;
    }

    @Override
    public void execute(final Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player) {
            Player player = (Player) source;
            String username = player.getUsername();
            Reward rewardToTake = configRewards.get(Integer.parseInt(args[0]) - 1);

            if (!enoughTimePlayed(player, rewardToTake)) {
                sendCooldownMessage(player);
                return;
            }

            if (claimManager.hadClaimed(username, rewardToTake.getId())) {
                sendExistMessage(player);
                return;
            }

            RegisteredServer playerServer = player.getCurrentServer().get().getServer();
            for (String command : rewardToTake.getActions()) {
                String serverName = command.split("]")[0].split("\\[")[1];
                String commandToSend = command.replace("{player}", username).split("\\]")[1].replaceFirst("^\\s*", "");
                sendServerCommand(playerServer, serverName, commandToSend);
            }
            sendTitle(player, translateColorCodes(rewardToTake.getTitle().get(0)), translateColorCodes(rewardToTake.getTitle().get(1)), 1, 1, 1);
            player.sendMessage(Component.text(translateColorCodes(String.valueOf(configMessages.get("given")))));
            try {
                claimManager.addClaimed(username, new ArrayList<>(List.of(rewardToTake.getId())));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(final Invocation invocation) {
        return CompletableFuture.completedFuture(List.of());
    }

    private void sendCooldownMessage(Player player) {
        ArrayList<String> title = getListFromString(String.valueOf(configMessages.get("cooldown-title")));
        sendTitle(player, translateColorCodes(title.get(0)), translateColorCodes(title.get(1)), 1, 1, 1);
        player.sendMessage(Component.text(translateColorCodes(String.valueOf(configMessages.get("cooldown")))));
    }

    private void sendExistMessage(Player player) {
        ArrayList<String> title = getListFromString(String.valueOf(configMessages.get("exist-title")));
        sendTitle(player, translateColorCodes(title.get(0)), translateColorCodes(title.get(1)), 1, 1, 1);
        player.sendMessage(Component.text(translateColorCodes(String.valueOf(configMessages.get("exist")))));
    }

    private ArrayList<String> getListFromString(String str) {
        return new ArrayList<>(Arrays.asList(String.valueOf(str).replace("[", "").replace("]", "").split(", ")));
    }

    private boolean enoughTimePlayed(Player player, Reward reward) {
        HashMap<String, Long> playerGlobalPlaytime = sqLite.getGlobalPlaytime(player.getUsername());
        for (Object server : reward.getRequiredTime().keySet()) {
            Long playtimeOnServer = playerGlobalPlaytime.get(server);
            long requiredPlaytimeOnServer = Long.parseLong(reward.getRequiredTime().get(server).toString());
            if (playtimeOnServer == null || playtimeOnServer < requiredPlaytimeOnServer) return false;
        }
        return true;
    }

    private String translateColorCodes(String toTranslate) {
        return toTranslate.replace("&", "§");
    }

    private void sendServerCommand(RegisteredServer server, String serverName, String command) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.write(serverName.getBytes());
        out.write(":".getBytes());
        out.write(command.getBytes());
        server.sendPluginMessage(() -> "velocity:runcmd", out.toByteArray());
    }

    private void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitlePart(TitlePart.TITLE, Component.text(translateColorCodes(title)));
        player.sendTitlePart(TitlePart.SUBTITLE, Component.text(translateColorCodes(subtitle)));
        player.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.of(fadeOut, SECONDS), Duration.of(stay, SECONDS), Duration.of(fadeOut, SECONDS)));
    }
}