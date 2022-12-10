package lv.exosmium.exoplaytime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reward {
    private final String rewardId;
    private final List<String> actions;
    private final List<String> title;
    private final HashMap<String, Object> requiredTime;

    public Reward(String rewardId, List<String> actions, List<String> title, HashMap<String, Object> requiredTime) {
        this.rewardId = rewardId;
        this.actions = actions;
        this.requiredTime = requiredTime;
        this.title = title;
    }

    public String getId() { return rewardId; }

    public List<String> getActions() {
        return actions;
    }

    public List<String> getTitle() {
        return title;
    }

    public Map<String, Object> getRequiredTime() {
        return requiredTime;
    }
}
