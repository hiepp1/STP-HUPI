package STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Itemset implements Comparable<Itemset> {
    List<Integer> items;
    int utility;
    float expectedUtility;
    int maxPer;

    @Override
    public int compareTo(Itemset o) {
        return Float.compare(this.expectedUtility, o.getExpectedUtility());
    }

    @Override
    public String toString() {
        return "Itemset: " + this.items +
                ", Utility: " + this.utility +
                ", Expected Utility: " + this.expectedUtility +
                ", Max Period: " + this.maxPer;
    }
}
