package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Itemset {
    private List<Integer> items;
    private int utility;
    private float expectedUtility;
    private int maxPer;

    @Override
    public String toString() {
        return "Itemset: " + this.items +
                ", Raw Utility: " + this.utility +
                ", Expected Utility: " + this.expectedUtility +
                ", Max Period: " + this.maxPer;
    }
}
