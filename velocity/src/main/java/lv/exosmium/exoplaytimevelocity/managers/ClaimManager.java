package lv.exosmium.exoplaytimevelocity.managers;

import com.google.gson.Gson;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClaimManager {
    private final String dataPath;
    private final Gson gson = new Gson();

    public ClaimManager(Path dataDirectory, String dataFileName) throws IOException {
        this.dataPath = dataDirectory + "/" + dataFileName;
        File dataFile = new File(dataPath);
        if (!dataFile.exists()) dataFile.createNewFile();
    }

    private Player[] getJsonPlayers(String filePath, Gson gson) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String currentLine = reader.readLine();
        Player[] players = gson.fromJson(currentLine, Player[].class);
        return players;
    }

    public boolean hadClaimed(String username, int rewardId) throws IOException {
        Player[] jsonPlayers = getJsonPlayers(dataPath, gson);
        if (jsonPlayers != null) {
            for (Player player : jsonPlayers) {
                if (player.getUsername().equals(username)) {
                    if (player.getTaken().contains(rewardId)) return true;
                }
            }
        }
        return false;
    }

    public void addClaimed(String username, List<Integer> rewards) throws IOException {
        Player[] players = getJsonPlayers(dataPath, gson);
        List<Player> playerArray = new ArrayList<>();

        if (players == null) {
            playerArray.add(new Player(username, rewards));
        } else {
            playerArray.addAll(List.of(players));
        }
        if (playerInArray(username, playerArray)) {
            for (Player player : playerArray) {
                if (player.getUsername().equals(username)) {
                    List<Integer> newTaken = player.getTaken();
                    newTaken.addAll(rewards);
                    player.setTaken(List.copyOf(Set.copyOf(newTaken)));
                }
            }
        } else  {
            playerArray.add(new Player(username, rewards));
        }
        FileWriter fileWriter = new FileWriter(dataPath);
        gson.toJson(playerArray, fileWriter);
        fileWriter.close();
    }

    private static boolean playerInArray(String username, List<Player> array) {
        for (Player player : array) if (player.getUsername().equals(username)) return true;
        return false;
    }
}
