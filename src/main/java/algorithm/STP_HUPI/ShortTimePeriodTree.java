package algorithm.STP_HUPI;

import java.util.*;

public class ShortTimePeriodTree {
    List<Integer> itemset;
    float expectedUtility;
    float posUtility;
    float negUtility;
    int maxPeriod;
    Map<Integer, ShortTimePeriodTree> children; // Tree structure

    public ShortTimePeriodTree(List<Integer> itemset, float expectedUtility, float posUtility, float negUtility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}