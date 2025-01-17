package STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Test {
    private List<Transaction> transactions;
    private int maxPer;
    private int k;
    private float minExpectedUtility; // Minimum expected utility threshold
    private PriorityQueue<Itemset> topKItemsets;
    private Map<Integer, Float> tweu; // Transaction-Weighted Expected Utility map
    private Map<Integer, Float> posUtility;
    private Map<Integer, Float> negUtility;


    public Test(List<Transaction> transactions, int maxPer, float minUtilityThreshold, int k) {
        this.transactions = new ArrayList<>(transactions); // Create defensive copy
        this.maxPer = maxPer;
        this.k = k;
        this.minExpectedUtility = this.calculateDatabaseUtility() * minUtilityThreshold;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.tweu = new HashMap<>();
        this.posUtility = new HashMap<>();
        this.negUtility = new HashMap<>();
    }

    // Calculate total utility of the dataset
    private int calculateDatabaseUtility() {
        return transactions.stream()
                .mapToInt(Transaction::getTransactionUtility)
                .sum();
    }

    // Calculate the utility of an itemset in a transaction
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream()
                .mapToInt(item -> {
                    int index = transaction.getItems().indexOf(item);
                    return index != -1 ? transaction.getUtilities().get(index) : 0;
                })
                .sum();
    }

    private int calculateMaxPeriod(List<Occurrence> occurrences) {
        if (occurrences.size() < 2) return Integer.MAX_VALUE;

        List<Integer> indices = occurrences.stream()
                .map(Occurrence::getTransactionID)
                .sorted()
                .collect(Collectors.toList());

        int maxPeriod = indices.get(0); // the first transaction that contains itemset: TransactionID - 0
        for (int i = 0; i < indices.size() - 1; i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i+1) - indices.get(i));
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
            if (new HashSet<>(transaction.getItems()).containsAll(itemset)) {
                // Only include positive contributions
                upperBound += Math.max(transaction.getTransactionUtility(), 0);
            }
        }
        return upperBound;
    }


    // ---------------------------- Pruned Strategies ---------------------------//
    private void filterLowUtilityItems() {
        float threshold = this.minExpectedUtility;
        this.transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> this.tweu.getOrDefault(item, 0f) < threshold);
            return transaction.getItems().isEmpty();
        });
    }

    // ---------------------------- New Method for PSU --------------------------- //
    private final Map<String, Float> psuCache = new HashMap<>();

    private float calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (psuCache.containsKey(key)) {
            return psuCache.get(key);
        }

        float psu = 0;
        for (Transaction transaction : transactions) {
            if (new HashSet<>(transaction.getItems()).containsAll(prefix) && transaction.getItems().contains(extensionItem)) {
                int prefixUtility = calculateUtility(transaction, prefix);
                int extensionUtility = calculateUtility(transaction, List.of(extensionItem));
                int remainingUtility = transaction.getItems().stream()
                        .filter(item -> !prefix.contains(item) && item != extensionItem)
                        .mapToInt(item -> transaction.getUtilities().get(transaction.getItems().indexOf(item)))
                        .sum();
                psu += prefixUtility + extensionUtility + remainingUtility;
            }
        }

        psuCache.put(key, psu); // Cache result
        return psu;
    }

    // ---------------------------- Pruned Strategies ---------------------------//

    private void computeTWEU() {
        // Reset maps before computation
        tweu.clear();
        posUtility.clear();
        negUtility.clear();

        for (Transaction transaction : transactions) {
            // Calculate actual transaction utility based on the present items only
            float transactionUtility = 0;

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);

                // Add to positive or negative utility maps
                if (utility >= 0) {
                    posUtility.merge(item, utility, Float::sum);
                } else {
                    negUtility.merge(item, utility, Float::sum);
                }
                // Add to transaction utility only for this item
                transactionUtility += utility;
            }
            // Now update TWEU for each item with the correct transaction utility
            for (int item : transaction.getItems()) {
                tweu.merge(item, transactionUtility, Float::sum);
            }
        }
    }

    private float calculateItemsetTWEU(List<Integer> itemset) {
        float itemsetTWEU = Float.MAX_VALUE;

        // Find minimum TWEU among all items in the itemset
        for (Integer item : itemset) {
            float itemTWEU = tweu.getOrDefault(item, 0f);
            itemsetTWEU = Math.min(itemsetTWEU, itemTWEU);
        }
        System.out.println(itemset + " TWEU: " + itemsetTWEU);
        return itemsetTWEU;
    }

    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream()
                .filter(transaction -> new HashSet<>(transaction.getItems()).containsAll(itemset))
                .map(transaction -> {
                    int utility = calculateUtility(transaction, itemset);
                    float positiveUtility = Math.max(utility, 0);
                    float probability = positiveUtility > 0 ?
                            positiveUtility / transaction.getTransactionUtility() : 0;
                    float expectedUtility = utility * probability;

                    return new Occurrence(transaction.getId(), probability,
                            utility, expectedUtility);
                })
                .collect(Collectors.toList());
    }

    public List<Itemset> generateItemsets() {
        Set<Set<Integer>> seenItemsets = new HashSet<>();

        // Get and sort unique items by TWEU
        List<Integer> sortedItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .distinct()
                .sorted((a, b) -> Float.compare(tweu.getOrDefault(b, 0f),
                        tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        // Process each item as starting point
        for (Integer item : sortedItems) {
            if (tweu.getOrDefault(item, 0f) >= minExpectedUtility) {
                List<Integer> currentItemset = new ArrayList<>();
                currentItemset.add(item);
                this.dfs(currentItemset, seenItemsets);
            }
        }

        // Return sorted results
        List<Itemset> results = new ArrayList<>(topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    private void updateMinExpectedUtility() {
        if (topKItemsets.size() == k) {
            assert topKItemsets.peek() != null;

            float newMinUtil = topKItemsets.peek().getExpectedUtility();
            if (newMinUtil > minExpectedUtility) {
                minExpectedUtility = newMinUtil;
                System.out.println("Raising minUtil: " + this.minExpectedUtility);
            }
        }
    }


    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets) {
        // Set a maximum depth to avoid excessively deep chains

        Set<Integer> itemsetKey = new HashSet<>(currentItemset);
        if (!seenItemsets.add(itemsetKey)) return;

        // Check if current itemset is promising
//        if (!isItemsetPromising(currentItemset)) {
//            System.out.println("Pruned " + currentItemset);
//            return;
//        }

        List<Occurrence> occurrences = findOccurrences(currentItemset);
        if (!occurrences.isEmpty()) {
            processCurrentItemset(currentItemset, occurrences);
        }

        // Generate extensions and prune using PSU
        Set<Integer> extensionItems = this.transactions.stream()
                .filter(t -> new HashSet<>(t.getItems()).containsAll(currentItemset))
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !currentItemset.contains(item) &&
                        item > currentItemset.get(currentItemset.size() - 1))
                .filter(item -> {
                    float psu = calculatePSU(currentItemset, item);
                    return psu >= this.minExpectedUtility; // Prune unpromising items
                })
                .collect(Collectors.toSet());

        for (Integer item : extensionItems) {
            List<Integer> newItemset = new ArrayList<>(currentItemset);
            newItemset.add(item);
            this.dfs(newItemset, seenItemsets); // Pass depth
        }
    }

    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);


        int maxPeriod = this.calculateMaxPeriod(occurrences);


        if (totalExpectedUtility >= minExpectedUtility && maxPeriod <= this.maxPer) {
            System.out.println("Get");
            int totalUtility = occurrences.stream()
                    .mapToInt(Occurrence::getUtility)
                    .sum();

            Itemset itemset = new Itemset(new ArrayList<>(currentItemset),
                    totalUtility, totalExpectedUtility, maxPeriod);
            System.out.println(itemset);
            if (topKItemsets.size() < k) {
                topKItemsets.offer(itemset);
            } else {
                assert topKItemsets.peek() != null;
                if (itemset.getExpectedUtility() > topKItemsets.peek().getExpectedUtility()) {
                    topKItemsets.poll();
                    topKItemsets.offer(itemset);
                }
            }
            updateMinExpectedUtility();
        }
    }
    // ------------------------------ BONUS ---------------------------------------//

    public void run() {
        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
        System.out.println("Minimum Expected Utility: " + this.minExpectedUtility);

        long startTime = System.nanoTime();

        // Initialize and optimize
        computeTWEU();
        filterLowUtilityItems();
//        mergeSimilarTransactions();

        // Generate itemsets
        List<Itemset> results = generateItemsets();

        // Print results
        double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
        System.out.printf("Execution time: %.2f seconds%n", executionTime);
        System.out.println("Final top-k itemsets:");
        results.forEach(System.out::println);
    }
}
