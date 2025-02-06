package algorithm;

import java.util.*;

class SPHTreeNode {
    List<Integer> itemset;
    float expectedUtility;
    float posUtility;
    float negUtility;
    int maxPeriod;
    Map<Integer, SPHTreeNode> children; // Tree structure

    public SPHTreeNode(List<Integer> itemset, float expectedUtility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }

    public SPHTreeNode(List<Integer> itemset, float expectedUtility, float posUtility, float negUtility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}
