package algorithm.ST_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
class StHupiTree {
    private List<Integer> itemset;
    private int utility;
    private float expectedUtility;
    private float posUtility;
    private float negUtility;
    private Map<Integer, StHupiTree> children;

    public StHupiTree(List<Integer> itemset, int utility, float expectedUtility, float posUtility, float negUtility) {
        this.itemset = itemset;
        this.utility = utility;
        this.expectedUtility = expectedUtility;
        this.posUtility = posUtility;
        this.negUtility = negUtility;
        this.children = new HashMap<>();
    }
}
