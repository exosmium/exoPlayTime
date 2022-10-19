package lv.exosmium.exoplaytimevelocity;

import java.util.List;
import java.util.Map;

public class Reward {
    private final int id;
    private final List<String> actions;
    private final List<String> title;
    private final Map<String, Object> requiredTime;

    public Reward(int id, List<String> actions, List<String> title, Map<String, Object> requiredTime) {
        this.id = id;
        this.actions = actions;
        this.requiredTime = requiredTime;
        this.title = title;
    }

    public int getId() { return id; }

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
