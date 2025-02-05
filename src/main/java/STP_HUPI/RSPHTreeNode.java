package STP_HUPI;

import java.util.*;

class RSPHTreeNode {
    List<Integer> itemset;
    float expectedUtility;
    int maxPeriod;
    Map<Integer, RSPHTreeNode> children; // Tree structure

    public RSPHTreeNode(List<Integer> itemset, float expectedUtility, int maxPeriod) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}
