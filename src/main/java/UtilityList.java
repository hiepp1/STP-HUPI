import java.util.HashMap;
import java.util.Map;

public class UtilityList {
    private Map<String, Integer> utilityMap;

    public UtilityList() {
        utilityMap = new HashMap<>();
    }

    public void addUtility(String item, int utility) {
        utilityMap.put(item, utilityMap.getOrDefault(item, 0) + utility);
    }

    public int getUtility(String item) {
        return utilityMap.getOrDefault(item, 0);
    }

    public int totalUtility() {
        return utilityMap.values().stream().mapToInt(Integer::intValue).sum();
    }
}
