package algorithm.STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StpHupiTree {
    private List<Integer> itemset;
    private int utility;
    private float expectedUtility;
    private float posUtility;
    private float negUtility;
    private int maxPeriod;
    private Map<Integer, StpHupiTree> children;

    public StpHupiTree(List<Integer> itemset, int utility, float expectedUtility, float posUtility, float negUtility, int maxPeriod) {
        this.itemset = itemset;
        this.utility = utility;
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}