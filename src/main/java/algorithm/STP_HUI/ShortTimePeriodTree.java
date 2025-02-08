package algorithm.STP_HUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShortTimePeriodTree {
    List<Integer> itemset;
    int posUtility;
    int negUtility;
    int utility;
    int maxPeriod;
    Map<Integer, ShortTimePeriodTree> children;

    public ShortTimePeriodTree(List<Integer> itemset, int posUtility, int negUtility, int utility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.utility = utility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}