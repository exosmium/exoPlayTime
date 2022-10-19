package lv.exosmium.exoplaytimevelocity.managers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClaimManager {
    private final String dataPath;

    public ClaimManager(Path dataDirectory, String dataFileName) throws IOException {
        this.dataPath = dataDirectory + "/" + dataFileName;
        File dataFile = new File(dataPath);
        if (!dataFile.exists()) dataFile.createNewFile();
    }

    public boolean hadClaimed(String username, int rewardId) {
        try {
            return getJsonRewards(username).contains(rewardId);
        } catch (Exception ignored) {}
        return false;
    }

    public void addClaimed(String username, List<Integer> rewards) throws JSONException, IOException {
        JSONObject rewardObject = new JSONObject();
        JSONObject rewardItem =  new JSONObject();
        rewards.addAll(getJsonRewards(username));
        rewardItem.put("claimed_rewards", Set.copyOf(rewards));
        rewardObject.put(username, rewardItem);
        writeToJson(rewardObject.toString(), dataPath);
    }

    private List<Integer> getJsonRewards(String username) throws IOException, JSONException {
        List<Integer> rewards = new ArrayList<>();
        try {
            JSONArray array = readFromJson(dataPath).getJSONObject(username).getJSONArray("claimed_rewards");
            for (int i = 0; i < array.length(); i++) {
                rewards.add(array.getInt(i));
            }
            return rewards;
        } catch (Exception ignored) {}
        return rewards;
    }

    private void writeToJson(String text, String path) {
        try (FileWriter file = new FileWriter(path)) { file.write(text); }
        catch (Exception e){ e.printStackTrace(); }
    }

    private JSONObject readFromJson(String path) throws IOException, JSONException {
        FileReader file = new FileReader(path);
        return new JSONObject(new BufferedReader(file).readLine());
    }
}
