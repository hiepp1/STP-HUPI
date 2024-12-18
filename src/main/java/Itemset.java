import java.util.*;

public class Itemset {
    private final Set<String> items;
    private final int utility;
    private final int periodicity;
    private final double confidence;

    public Itemset(Set<String> items, int utility, int periodicity, double confidence) {
        this.items = items;
        this.utility = utility;
        this.periodicity = periodicity;
        this.confidence = confidence;
    }

    public Set<String> getItems() {
        return items;
    }

    public int getUtility() {
        return utility;
    }

    public int getPeriodicity() {
        return periodicity;
    }

    public double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return String.format("Itemset: %s, Utility: %d, Periodicity: %d, Confidence: %.2f%%",
                items, utility, periodicity, confidence * 100);
    }
}
