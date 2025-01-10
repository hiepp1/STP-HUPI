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
    private float minUtilityThreshold;
    private float minUtil;

    public ShortTimePeriodMining(List<Transaction> transactions) {
        this.transactions = transactions;
        this.k = 0;
        this.minUtil = 100;
        this.minUtilityThreshold = 0.2f;
        this.maxPer = 10;
    }

    public ShortTimePeriodMining(List<Transaction> transactions, int maxPer, float minUtilityThreshold, int k) {
        this.transactions = transactions;
        this.k = k;
        this.minUtilityThreshold = minUtilityThreshold;
        this.minUtil = this.calculateDatabaseUtility() * this.minUtilityThreshold;
        this.maxPer = maxPer;
    }

    // Calculate total utility of the dataset
    private int calculateDatabaseUtility() {
        return this.transactions.stream()
                .map(Transaction::getTransactionUtility)
                .reduce(0, Integer::sum);
    }

    // Calculate the utility of an itemset in a transaction
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        int utility = 0;
        for (Integer item : itemset) {
            int index = transaction.getItems().indexOf(item);
            if (index != -1) {
                utility += transaction.getUtilities().get(index);
            }
        }
        return utility;
    }


    // Find occurrences of the itemset in transactions
    public List<Occurrence> findOccurrences(List<Integer> itemset) {
        List<Occurrence> occurrences = new ArrayList<>();

        for (Transaction transaction : this.transactions) {
            List<Integer> items = transaction.getItems();
            if (new HashSet<>(items).containsAll(itemset)) {
                int utility = this.calculateUtility(transaction, itemset);
                float probability = (float) utility / transaction.getTransactionUtility();
                float expectedUtility = (float) utility * probability;
                occurrences.add(new Occurrence(transaction.getId(), probability, utility, expectedUtility));
            }
        }
        return occurrences;
    }

    private int calculateMaxPeriod(List<Occurrence> occurrences) {
        if (occurrences.size() < 2) return Integer.MAX_VALUE;

//        int maxPeriod = 0;
//        for (int i = 0; i < transactionID.size() - 1; i++) {
//            int period = transactionID.get(i + 1) - transactionID.get(i);
//            maxPeriod = Math.max(maxPeriod, period);
//        }

        List<Integer> indices = new ArrayList<>();
        for (Occurrence occurrence : occurrences) {
            indices.add(occurrence.getTransactionID());
        }

        int maxPeriod = indices.get(0);
        for (int i = 0; i < indices.size() - 1; i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i + 1) - indices.get(i));
        }
        return maxPeriod;
    }

    private float getTotalExpectedUtility(List<Occurrence> occurrences) {
        float totalExpectedUtility = 0;
        for (Occurrence occurrence : occurrences) {
            totalExpectedUtility += occurrence.getExpectedUtility();
        }
        return totalExpectedUtility;
    }

    private int getTotalUtility(List<Occurrence> occurrences) {
        int totalUtility = 0;
        for (Occurrence occurrence : occurrences) {
            totalUtility += occurrence.getUtility();
        }
        return totalUtility;
    }

    public List<Itemset> generateItemsets() {
        List<Itemset> results = new ArrayList<>();
        Set<Set<Integer>> seenItems = new HashSet<>();

        for (Transaction transaction : this.transactions) {
            List<Integer> items = transaction.getItems();
            for (int i = 0; i < items.size(); i++) {
                List<Integer> currentItemset = new ArrayList<>();
                currentItemset.add(items.get(i));
                this.dfs(currentItemset, seenItems, results);
            }
        }
        results.sort((a, b) -> Float.compare(b.getExpectedUtility(), a.getExpectedUtility()));
        return results.subList(0, Math.min(this.k, results.size()));
    }

    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets, List<Itemset> resultItemsets) {
        Set<Integer> sortedItemset = new HashSet<>(currentItemset);
        if (seenItemsets.contains(sortedItemset)) return;
        seenItemsets.add(sortedItemset);

        List<Occurrence> occurrences = this.findOccurrences(currentItemset);
        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
        int maxPeriod = this.calculateMaxPeriod(occurrences);

        // Prune
        if (maxPeriod > this.maxPer || totalExpectedUtility < this.minUtil) {
            System.out.println("Prune itemset: " + currentItemset);
            return;
        }

        if (totalExpectedUtility >= this.minUtil) {
            int totalUtility = this.getTotalUtility(occurrences);
            Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);
//            System.out.println("-----------");
//            System.out.println(itemset);
//            System.out.println("-----------");
            resultItemsets.add(itemset);
        }

        for (Transaction transaction : transactions) {
            List<Integer> items = transaction.getItems();
            for (Integer item : items) {
                if (!currentItemset.contains(item)) {
                    currentItemset.add(item);
                    dfs(currentItemset, seenItemsets, resultItemsets);
                    currentItemset.remove(currentItemset.size() - 1); // Backtrack
                }
            }
        }
    }

    public void run() {
        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
        System.out.println("Minimum Expected Utility: " + this.minUtil);
        List<Itemset> results = this.generateItemsets();
        for (Itemset itemset : results) {
            System.out.println(itemset);
        }
    }
}
