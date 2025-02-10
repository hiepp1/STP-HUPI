package algorithm.STP_HUI;

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
public class StpHuiTree {
    private List<Integer> itemset;
    private int utility;
    private int maxPeriod;
    private Map<Integer, StpHuiTree> children;

    public StpHuiTree(List<Integer> itemset, int utility, int maxPeriod) {
        this.itemset = itemset;
        this.utility = utility;
        this.maxPeriod = maxPeriod;
        this.children = new HashMap<>();
    }
}