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
//    private float minUtilityThreshold;
//    private float minUtil;
    private float minExpectedUtility; // Minimum expected utility threshold
    private PriorityQueue<Itemset> topKItemsets;
    private Map<Integer, Float> TWEU; // Transaction-Weighted Expected Utility map


    public ShortTimePeriodMining(List<Transaction> transactions, int maxPer, float minExpectedUtility, int k) {
        this.transactions = transactions;
        this.k = k;
        this.minExpectedUtility = this.calculateDatabaseUtility() * minExpectedUtility; // Initially very low
        this.maxPer = maxPer;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.TWEU = new HashMap<>();
    }

    // Code cua may` //
    // ---------------- // ---------------- BAO --------------//--------------//
//    public ShortTimePeriodMining(List<Transaction> transactions, int maxPer, float minUtilityThreshold, int k) {
//        this.transactions = transactions;
//        this.k = k;
//        this.minUtilityThreshold = minUtilityThreshold;
//        this.minUtil = this.calculateDatabaseUtility() * this.minUtilityThreshold;
//        this.maxPer = maxPer;
//    }


    // Find occurrences of the itemset in transactions
//    public List<Occurrence> findOccurrences(List<Integer> itemset) {
//        List<Occurrence> occurrences = new ArrayList<>();
//
//        for (Transaction transaction : this.transactions) {
//            List<Integer> items = transaction.getItems();
//            if (new HashSet<>(items).containsAll(itemset)) {
//                int utility = this.calculateUtility(transaction, itemset);
//                float probability = (float) utility / transaction.getTransactionUtility();
//                float expectedUtility = (float) utility * probability;
//                occurrences.add(new Occurrence(transaction.getId(), probability, utility, expectedUtility));
//            }
//        }
//        return occurrences;
//    }

//    public List<Itemset> generateItemsets() {
//        List<Itemset> results = new ArrayList<>();
//        Set<Set<Integer>> seenItems = new HashSet<>();
//
//        for (Transaction transaction : this.transactions) {
//            List<Integer> items = transaction.getItems();
//            for (int i = 0; i < items.size(); i++) {
//                List<Integer> currentItemset = new ArrayList<>();
//                currentItemset.add(items.get(i));
//                this.dfs(currentItemset, seenItems, results);
//            }
//        }
//        results.sort((a, b) -> Float.compare(b.getExpectedUtility(), a.getExpectedUtility()));
//        return results.subList(0, Math.min(this.k, results.size()));
//    }
//
//    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets, List<Itemset> resultItemsets) {
//        Set<Integer> sortedItemset = new HashSet<>(currentItemset);
//        if (seenItemsets.contains(sortedItemset)) return;
//        seenItemsets.add(sortedItemset);
//
//        List<Occurrence> occurrences = this.findOccurrences(currentItemset);
//        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
//        int maxPeriod = this.calculateMaxPeriod(occurrences);
//
//        if (totalExpectedUtility >= this.minUtil) {
//            int totalUtility = this.getTotalUtility(occurrences);
//            Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);
////            System.out.println("-----------");
////            System.out.println(itemset);
////            System.out.println("-----------");
//            resultItemsets.add(itemset);
//        }
//
//        for (Transaction transaction : transactions) {
//            List<Integer> items = transaction.getItems();
//            for (Integer item : items) {
//                if (!currentItemset.contains(item)) {
//                    currentItemset.add(item);
//                    dfs(currentItemset, seenItemsets, resultItemsets);
//                    currentItemset.remove(currentItemset.size() - 1); // Backtrack
//                }
//            }
//        }
//    }
    // ---------------- // ---------------- BAO --------------//--------------//



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



    private int calculateMaxPeriod(List<Occurrence> occurrences) {
        if (occurrences.size() < 2) return Integer.MAX_VALUE;

        List<Integer> indices = new ArrayList<>();
        for (Occurrence occurrence : occurrences) {
            indices.add(occurrence.getTransactionID());
        }
        Collections.sort(indices); // Ensure indices are sorted

        int maxPeriod = Integer.MIN_VALUE;
        for (int i = 1; i < indices.size(); i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i) - indices.get(i - 1));
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

    // ------------------------------ BONUS ---------------------------------------//
    private float calculateExpectedUtilityUpperBound(List<Integer> itemset) {
        float upperBound = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getItems().containsAll(itemset)) {
                upperBound += transaction.getTransactionUtility();
            }
        }
        return upperBound;
    }

    private void computeTWEU() {
        for (Transaction transaction : transactions) {
            for (Integer item : transaction.getItems()) {
                float transactionUtility = transaction.getTransactionUtility();
                this.TWEU.put(item, this.TWEU.getOrDefault(item, 0f) + transactionUtility);
            }
        }
    }

    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        List<Occurrence> occurrences = new ArrayList<>();
        for (Transaction transaction : transactions) {
            if (transaction.getItems().containsAll(itemset)) {
                int utility = calculateUtility(transaction, itemset);
                float probability = (float) utility / transaction.getTransactionUtility();
                float expectedUtility = utility * probability;
                occurrences.add(new Occurrence(transaction.getId(), probability, utility, expectedUtility));
            }
        }
        return occurrences;
    }

    public List<Itemset> generateItemsets() {
        List<Itemset> results = new ArrayList<>();
        Set<Set<Integer>> seenItemsets = new HashSet<>();

        // Compute TWEU before starting
        computeTWEU();

        // Get all items from all transactions
        Set<Integer> uniqueItems = new TreeSet<>();
        for (Transaction transaction : transactions) {
            uniqueItems.addAll(transaction.getItems());
        }

        // Sort items by TWEU in descending order
        List<Integer> sortedItems = new ArrayList<>(uniqueItems);
        sortedItems.sort((a, b) -> Float.compare(TWEU.getOrDefault(b, 0f), TWEU.getOrDefault(a, 0f)));

        // Process each item as starting point
        for (Integer item : sortedItems) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            dfs(currentItemset, seenItemsets);
        }

        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    private void updateMinExpectedUtility() {
        if (this.topKItemsets.size() == k) {
            float newMinUtil = this.topKItemsets.peek().getExpectedUtility();
            if (newMinUtil > this.minExpectedUtility) {
                this.minExpectedUtility = newMinUtil;
                System.out.println("Raising minUtil: " + minExpectedUtility);
            }
        }
    }

    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets) {
        Set<Integer> itemsetKey = new HashSet<>(currentItemset);
        if (seenItemsets.contains(itemsetKey)) {
            return;
        }
        seenItemsets.add(itemsetKey);

        // Calculate expected utility upper bound
        float expectedUtilityBound = calculateExpectedUtilityUpperBound(currentItemset);
        if (expectedUtilityBound < this.minExpectedUtility) {
            return; // Prune branch
        }

        // Find occurrences and calculate utilities
        List<Occurrence> occurrences = findOccurrences(currentItemset);
        if (!occurrences.isEmpty()) {
            float totalExpectedUtility = getTotalExpectedUtility(occurrences);
            int maxPeriod = calculateMaxPeriod(occurrences);

            // Prune based on utility and period
            if (totalExpectedUtility >= this.minExpectedUtility) {
                int totalUtility = getTotalUtility(occurrences);
                Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);
                System.out.println(itemset);

                // Update top-k itemsets
                if (this.topKItemsets.size() < k) {
                    this.topKItemsets.offer(itemset);
                } else if (itemset.getExpectedUtility() > this.topKItemsets.peek().getExpectedUtility()) {
                    this.topKItemsets.poll();
                    this.topKItemsets.offer(itemset);
                }
                updateMinExpectedUtility();
            }
            System.out.println("Pruned " + currentItemset);
        }

        // Generate extensions
        Set<Integer> extensionItems = new TreeSet<>();
        for (Transaction transaction : transactions) {
            if (transaction.getItems().containsAll(currentItemset)) {
                for (Integer item : transaction.getItems()) {
                    if (!currentItemset.contains(item) && item > currentItemset.get(currentItemset.size() - 1)) {
                        extensionItems.add(item);
                    }
                }
            }
        }

        // Explore extensions recursively
        for (Integer item : extensionItems) {
            List<Integer> newItemset = new ArrayList<>(currentItemset);
            newItemset.add(item);

            float itemsetTWEU = 0f;
            for (Integer i : newItemset) {
                itemsetTWEU += TWEU.getOrDefault(i, 0f);
            }

            if (itemsetTWEU >= this.minExpectedUtility) {
                dfs(newItemset, seenItemsets);
            }
        }
    }
    // ------------------------------ BONUS ---------------------------------------//

    public void run() {
//        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
//        System.out.println("Minimum Expected Utility: " + this.minExpectedUtility);
//        List<Itemset> results = this.generateItemsets();
//        System.out.println("Final top-k list:");
//        for (Itemset itemset : results) {
//            System.out.println(itemset);
//        }
        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
        System.out.println("Minimum Expected Utility: " + this.minExpectedUtility);

        long startTime = System.nanoTime();
        this.generateItemsets();
        long endTime = System.nanoTime();

        long duration = endTime - startTime;
        double seconds = (double) duration / 1_000_000_000.0; //Correct
        System.out.println("Execution time: " + seconds);

        System.out.println("Final top-k list:");
        List<Itemset> finalTopK = new ArrayList<>(this.topKItemsets);
        finalTopK.sort((a, b) -> Float.compare(b.getExpectedUtility(), a.getExpectedUtility()));

        for (Itemset itemset : finalTopK) {
            System.out.println(itemset);
        }
    }
}
