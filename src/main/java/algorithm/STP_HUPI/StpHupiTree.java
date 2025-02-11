package algorithm.STP_HUPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * This class represents a tree structure used in the STP-HUPI algorithm
 * to store and expand high-utility itemsets while maintaining period constraints.
 * Each node in the tree corresponds to an itemset, along with its associated utility values.
 * The tree is used in the STP-HUPI mining process to efficiently explore and prune candidate itemsets
 * while ensuring that the discovered patterns satisfy the short-time constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StpHupiTree {
    private List<Integer> itemset;
    private int utility;
    private float expectedUtility;
    private int maxPeriod;
    private Map<Integer, StpHupiTree> children;

    /**
     * Constructs a new StpHupiTree node with the given parameters.
     * Initializes the children map to manage tree growth dynamically.
     *
     * @param itemset The itemset represented by this node.
     * @param utility The total raw utility of the itemset.
     * @param expectedUtility The expected utility of the itemset.
     * @param maxPeriod The maximum period constraint for the itemset.
     */
    public StpHupiTree(List<Integer> itemset, int utility, float expectedUtility, int maxPeriod) {
        this.itemset = itemset;
        this.utility = utility;
        this.expectedUtility = expectedUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}