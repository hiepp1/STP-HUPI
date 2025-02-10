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
    private int maxPeriod;
    private Map<Integer, StpHupiTree> children;

    public StpHupiTree(List<Integer> itemset, int utility, float expectedUtility, int maxPeriod) {
        this.itemset = itemset;
        this.utility = utility;
        this.expectedUtility = expectedUtility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}