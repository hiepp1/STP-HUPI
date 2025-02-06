package algorithm.ST_HUPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SHTreeNode {
    List<Integer> itemset;
    float expectedUtility;
    float posUtility;
    float negUtility;
    Map<Integer, SHTreeNode> children; // Tree structure

    public SHTreeNode(List<Integer> itemset, float expectedUtility) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.children = new HashMap<>();
    }

    public SHTreeNode(List<Integer> itemset, float expectedUtility, float posUtility, float negUtility) {
        this.itemset = new ArrayList<>(itemset);
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.children = new HashMap<>();
    }
}
