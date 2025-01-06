package Top_HUIM_PosNeg;

import java.util.*;

// Class to represent a transaction
class Transaction {
    List<Integer> items;
    List<Integer> utilities;

    public Transaction(List<Integer> items, List<Integer> utilities) {
        this.items = items;
        this.utilities = utilities;
    }

    public void sortItemsByUtility() {
        List<Map.Entry<Integer, Integer>> itemUtilityPairs = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            itemUtilityPairs.add(new AbstractMap.SimpleEntry<>(items.get(i), utilities.get(i)));
        }
        itemUtilityPairs.sort((a, b) -> b.getValue() - a.getValue());
        items.clear();
        utilities.clear();
        for (Map.Entry<Integer, Integer> pair : itemUtilityPairs) {
            items.add(pair.getKey());
            utilities.add(pair.getValue());
        }
    }

    public int getTotalUtility() {
        int total = 0;
        for (int utility : utilities) {
            total += utility;
        }
        return total;
    }

    @Override
    public String toString() {
        return "Items: " + items + ", Utilities: " + utilities;
    }
    String filename = "src/main/java/dataset/test.txt";

}