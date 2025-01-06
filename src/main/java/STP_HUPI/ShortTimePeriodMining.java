package STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortTimePeriodMining {
    private List<Transaction> transactions;
    private int maxPer;
    private int k;
    private float threshold;
    private float minUtil;
    private PriorityQueue<Itemset> topKItemsets;

    public ShortTimePeriodMining(List<Transaction> transactions) {
        this.transactions = transactions;
        this.k = 0;
        this.minUtil = 100;
        this.threshold = 0.2f;
        this.maxPer = 10;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility).reversed());
    }

    public ShortTimePeriodMining(List<Transaction> transactions, int maxPer, int k, float threshold) {
        this.transactions = transactions;
        this.k = k;
        this.minUtil = 0;
        this.threshold = threshold;
        this.maxPer = maxPer;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility).reversed());
    }
    public float calculateMinimumUtility() {
        int totalTransactionUtility = transactions.stream()
                .mapToInt(Transaction::getTransactionUtility)
                .sum();
        return totalTransactionUtility * this.threshold;
    }

    public List<Occurrence> findOccurrences(List<Integer> itemset) {
        List<Occurrence> occurrences = new ArrayList<>();

        for (Transaction transaction : this.transactions) {
            List<Integer> utilities = new ArrayList<>();
            boolean allItemsExist = true;

            for (Integer item : itemset) {
                int indexOfItem = transaction.getItems().indexOf(item);
                if (indexOfItem != -1) {
                    utilities.add(transaction.getUtilities().get(indexOfItem));
                } else {
                    allItemsExist = false;
                    break;
                }
            }
            if (allItemsExist) {
                int utility = utilities.stream().mapToInt(Integer::intValue).sum();
                float probability = (float) utility / transaction.getTransactionUtility();
                float expectedUtility = (float) utility * probability;
                occurrences.add(new Occurrence(transaction.getId(), probability, utility, expectedUtility));
            }
        }
        return occurrences;
    }

    public int calculateMaxPeriod(List<Integer> transactionID) {
        if (transactionID.size() < 2) return Integer.MAX_VALUE;

        int maxPeriod = 0;
        for (int i = 0; i < transactionID.size() - 1; i++) {
            int period = transactionID.get(i + 1) - transactionID.get(i);
            maxPeriod = Math.max(maxPeriod, period);
        }
        return maxPeriod;
    }

    public List<Itemset> dfs() {
        List<Itemset> allItemsets = new ArrayList<>();
        Set<List<Integer>> seenItemsets = new HashSet<>();
        dfGenerateItemsets(new ArrayList<>(), 0, allItemsets, seenItemsets);
        return allItemsets;
    }

    public void dfs_v1(List<Integer> currentItemset, int startIndex, Set<String> seenItemsets) {
        List<Occurrence> occurrences = this.findOccurrences(currentItemset);
        List<Integer> indices = new ArrayList<>();
        for (Occurrence o : occurrences) {
            indices.add(o.getTransactionID());
        }
        int maxPer = this.calculateMaxPeriod(indices);
        float expectedUtility = this.calculateExpectedUtility(occurrences);

        // Pruning conditions
        if (maxPer > this.maxPer || expectedUtility < this.minUtil) {
            return;
        }

        // Check and add to the top-k results
        if (!currentItemset.isEmpty()) {
            Itemset newItemset = new Itemset(new ArrayList<>(currentItemset), (int) expectedUtility, expectedUtility, maxPer);
            if (this.topKItemsets.size() < this.k) {
                topKItemsets.add(newItemset);
            } else if (expectedUtility > this.topKItemsets.peek().getExpectedUtility()) {
                topKItemsets.poll();
                topKItemsets.add(newItemset);
            }
        }

        // Explore
        for (int i = startIndex; i < this.transactions.size(); i++) {
            List<Integer> items = this.transactions.get(i).getItems();
            for (Integer item: items) {
                if (!currentItemset.contains(item)) {
                    currentItemset.add(item);
                    this.dfs_v1(currentItemset, i+1, seenItemsets);
                    currentItemset.remove(currentItemset.size() - 1);
                }
            }
        }
    }

    public List<Itemset> getTopKItemsets() {
        List<Itemset> results = new ArrayList<>(this.topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }
    private void dfGenerateItemsets(ArrayList<Integer> currentItemset, int index, List<Itemset> allItemsets, Set<List<Integer>> seenItemsets) {
        if (!currentItemset.isEmpty()) {
            List<Occurrence> occurrences = this.findOccurrences(currentItemset);
            if (!occurrences.isEmpty()) {
                List<Integer> indices = new ArrayList<>();
                float expectedUtility = 0f;
                for (Occurrence occurrence : occurrences) {
                    indices.add(occurrence.getTransactionID());
                    expectedUtility += occurrence.getExpectedUtility();
                }

//                if (expectedUtility < this.minUtil) return;
//
//                int maxPeriod = calculateMaxPeriod(indices);
//                if (maxPeriod > this.maxPer) return;

                currentItemset.sort(Integer::compareTo);
                if (!seenItemsets.contains(currentItemset)) {
                    allItemsets.add(new Itemset(new ArrayList<>(currentItemset), 0, expectedUtility, 0));
                    seenItemsets.add(new ArrayList<>(currentItemset));
                }
            }
        }

        for (int i = index; i < this.transactions.size(); i++) {
            for (Integer item : this.transactions.get(i).getItems()) {
                if (!currentItemset.contains(item)) {
                    currentItemset.add(item);
                    this.dfGenerateItemsets(currentItemset, i + 1, allItemsets, seenItemsets);
                    currentItemset.remove(item);
                }
            }
        }
    }

    public List<Itemset> findTopKItemsets() {
        List<Itemset> allItemsets = this.dfs();
        for (Itemset i : allItemsets) {
            System.out.println(i.getItems());
        }
        allItemsets.sort((a, b) -> Float.compare(b.getExpectedUtility(), a.getExpectedUtility()));
        return allItemsets.subList(0, Math.min(this.k, allItemsets.size()));
    }

    public void run() {
        this.minUtil = this.calculateMinimumUtility();
        System.out.println("Minimum Utility: " + this.minUtil);
//        List<Itemset> itemsets = this.findTopKItemsets();
//        System.out.println("Top " + k + " STP-HUPI:");
//        for (Itemset itemset : itemsets) {
//            System.out.println(itemset);
//        }
    }

    private float calculateExpectedUtility(List<Occurrence> occurrences) {
        float totalExpectedUtility = 0f;

        for (Occurrence occurrence : occurrences) {
            totalExpectedUtility += occurrence.getExpectedUtility();
        }
        return totalExpectedUtility;
    }
}
