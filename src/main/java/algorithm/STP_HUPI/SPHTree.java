package algorithm.STP_HUPI;

import java.util.*;

public class SPHTree {
    List<Integer> itemset;
    float expectedUtility;
    float posUtility;
    float negUtility;
    int maxPeriod;
    Map<Integer, SPHTree> children; // Tree structure

    public SPHTree(List<Integer> itemset, float expectedUtility, float posUtility, float negUtility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}