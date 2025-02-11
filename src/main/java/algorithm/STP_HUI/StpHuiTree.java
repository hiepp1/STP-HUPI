package algorithm.STP_HUI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a tree structure used in the STP-HUI algorithm
 * to store and expand high-utility itemsets while maintaining period constraints.
 * Each node in the tree corresponds to an itemset, along with its associated utility values.
 * The tree is used in the STP-HUI mining process to efficiently explore and prune candidate itemsets
 * while ensuring that the discovered patterns satisfy the short-time constraints.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StpHuiTree {
    private List<Integer> itemset;
    private int utility;
    private int maxPeriod;
    private Map<Integer, StpHuiTree> children;

    /**
     * Constructs a new StpHuiTree node with the given parameters.
     * Initializes the children map to manage tree growth dynamically.
     *
     * @param itemset The itemset represented by this node.
     * @param utility The total raw utility of the itemset.
     * @param maxPeriod The maximum period constraint for the itemset.
     */
    public StpHuiTree(List<Integer> itemset, int utility, int maxPeriod) {
        this.itemset = itemset;
        this.utility = utility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}