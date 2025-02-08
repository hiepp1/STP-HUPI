package algorithm.STP_HUPI;

import algorithm.Itemset;
import algorithm.Occurrence;
import algorithm.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class STPHUPIAlgorithm {
    // Fields for dataset, parameters, and utility tracking.
    private List<Transaction> transactions;
    private int[] kValues;                       // Array of k-values for performance evaluation.
    private int maxPer;                          // Maximum allowed period for an itemset.
    private int k;                               // Current top-K value.
    private float minUtil;                       // Minimum expected utility threshold.
    private int dbUtil;                          // Database utility (sum of transaction utilities).
    private PriorityQueue<Itemset> topKItemsets; // Priority queue to maintain top-K itemsets.
    private Map<Integer, Float> twu;             // Transaction-weighted utility map.
    private Map<Integer, Float> posUtil;         // Positive utility map.
    private Map<Integer, Float> negUtil;         // Negative utility map.
    private Map<String, Float> psuCache;         // Cache for PSU calculations.
    private final Set<String> processedPSU = new HashSet<>(); // Set to avoid duplicate PSU computations.
    private final List<Transaction> originalTransactions;      // Original transaction list.
    private Set<String> topKSeen;                // Set to track processed (canonical) itemset keys.
    private Map<Integer, Double> runTimeResults = new LinkedHashMap<>(); // Runtime results per k-value.
    private Map<Integer, Double> memoryResults = new LinkedHashMap<>();  // Memory usage per k-value.

    // Constructor.
    public STPHUPIAlgorithm(List<Transaction> transactions, int[] kValues, int maxPer) {
        this.originalTransactions = new ArrayList<>(transactions);
        this.transactions = new ArrayList<>(transactions);
        this.kValues = kValues;
        this.maxPer = maxPer;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.twu = new HashMap<>();
        this.posUtil = new HashMap<>();
        this.negUtil = new HashMap<>();
        this.psuCache = new HashMap<>();
        this.topKSeen = new HashSet<>();
    }


    // ------------- PRIU STRATEGIES -------------//

    /**
     * Calculates the maximum positive utility (PRIU) for each transaction.
     * It sums the positive utility values for all items in each transaction and returns the maximum.
     */
    private float calculatePRIU() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> posUtil.getOrDefault(item, 0f))
                        .sum())
                .max().orElse(0);
    }

    /**
     * Calculates PLIU_E: For each transaction, sorts items by their positive utility and sums the top two.
     * Returns the maximum sum observed across transactions.
     */
    private float calculatePLIU_E() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> {List<Integer> sortedItems = transaction.getItems()
                        .stream().sorted(Comparator.comparingDouble(item -> posUtil.getOrDefault(item, 0f)))
                        .collect(Collectors.toList());
            return sortedItems.stream().limit(2)
                    .mapToDouble(item -> posUtil.getOrDefault(item, 0f))
                    .sum();
        }).max().orElse(0);
    }

    /**
     * Calculates PLIU_LB: Returns the smallest expected utility among the current top-K itemsets.
     */
    private float calculatePLIU_LB() {
        if (topKItemsets.isEmpty()) return 0;

        return (float) topKItemsets.stream()
                .mapToDouble(Itemset::getExpectedUtility)
                .min().orElse(0);
    }


    // ------------- DATABASE & TRANSACTION UTILITY -------------//

    /**
     * Calculates the total utility of the database by summing the transaction utilities.
     */
    private int calculateDatabaseUtility() {
        return transactions.stream()
                .mapToInt(Transaction::getTransactionUtility).sum();
    }


    // ------------- UTILITY & OCCURRENCE CALCULATIONS -------------//

    /**
     * Calculates the utility of an itemset in a given transaction.
     */
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }

    /**
     * Finds all occurrences of an itemset in the transactions and computes the utility, probability, and expected utility for each occurrence.
     */
    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream().filter(transaction -> transaction.getItems().containsAll(itemset)).map(transaction -> {
            int utility = calculateUtility(transaction, itemset);
            float posUtil = Math.max(utility, 0);
            float negUtil = Math.min(utility, 0);
            float probability = (posUtil + Math.abs(negUtil)) > 0 ? (posUtil + negUtil) / transaction.getTransactionUtility() : 0;
            float expectedUtility = (posUtil + negUtil) * probability;

            return new Occurrence(transaction.getId(), probability, utility, expectedUtility);
        }).collect(Collectors.toList());
    }

    /**
     * Calculates the maximum period (largest gap between consecutive occurrences) for an itemset.
     */
    private int calculateMaxPeriod(List<Occurrence> occurrences) {
        if (occurrences.size() < 2) return 0;

        List<Integer> indices = occurrences.stream()
                .map(Occurrence::getTransactionID)
                .sorted().collect(Collectors.toList());

        int maxPeriod = indices.get(0); // the first transaction that contains itemset: TransactionID - 0
        for (int i = 0; i < indices.size() - 1; i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i + 1) - indices.get(i));
        }
        return maxPeriod;
    }

    /**
     * Sums the expected utility values for all occurrences of an itemset.
     */
    private float getTotalExpectedUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(Occurrence::getExpectedUtility).sum();
    }

    /**
     * Sums the raw utility values for all occurrences of an itemset.
     */
    private int getTotalUtility(List<Occurrence> occurrences) {
        return occurrences.stream().mapToInt(Occurrence::getUtility).sum();
    }

    /**
     * Sums the positive utility values for all occurrences.
     */
    private float getTotalPositiveUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(o -> Math.max(o.getUtility(), 0)).sum();
    }

    /**
     * Sums the negative utility values for all occurrences.
     */
    private float getTotalNegativeUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(o -> Math.min(o.getUtility(), 0)).sum();
    }

    // ------------- PRUNING STRATEGY -------------//

    /**
     * Filters out low-utility items from transactions based on the current min utility threshold.
     */
    private void filterLowUtilityItems() {
        transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> twu.getOrDefault(item, 0f) < minUtil);
            return transaction.getItems().isEmpty();
        });
    }


    // ------------- PSU (Positive Sub-tree Utility) -------------//

    /**
     * Calculates the PSU for a given prefix and extension item.
     * Returns the maximum PSU computed across transactions.
     */
    public float calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (processedPSU.contains(key)) return 0;
        processedPSU.add(key);

        // Track max PSU instead of sum
        float maxPSU = 0;

        for (Transaction transaction : transactions) {
            if (!transaction.getItems().containsAll(prefix) || !transaction.getItems().contains(extensionItem)) {
                continue;
            }

            int prefixUtility = calculateUtility(transaction, prefix);
            int extensionUtility = calculateUtility(transaction, List.of(extensionItem));
            // Only sum positive extension utility
            int adjustedExtensionUtility = Math.max(extensionUtility, 0);

            int remainingPositiveUtility = transaction.getItems().stream().filter(item -> !prefix.contains(item) && item != extensionItem) // Exclude prefix & extension item
                    .mapToInt(item -> {
                        int index = transaction.getItems().indexOf(item);
                        if (index == -1) return 0;
                        int utility = transaction.getUtilities().get(index);
                        return Math.max(utility, 0); // Ignore negative utilities
                    }).sum();

            float computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            // Track max PSU instead of sum
            maxPSU = Math.max(maxPSU, computedPSU);
        }

        psuCache.put(key, maxPSU);
        return maxPSU;
    }


    // ------------- TWU COMPUTING -------------//

    /**
     * Computes the Transaction-Weighted Utility (TWU) for each item and updates posUtil and negUtil maps.
     */
    private void computeTWU() {
        for (Transaction transaction : transactions) {
            float transactionUtility = (float) transaction.getTransactionUtility();

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);

                if (utility >= 0) posUtil.merge(item, utility, Float::sum);
                else {
                    negUtil.merge(item, utility, Float::sum);
                    transactionUtility += utility; // Adjust transaction utility with negative values.
                }
            }

            for (int item : transaction.getItems()) {
                twu.merge(item, transactionUtility, Float::sum);
            }
        }
    }


    // ------------- ITEMSET GENERATION AND TREE GROWTH -------------//

    /**
     * Generates candidate high-utility itemsets by building a ShortTimePeriodTree.
     * It collects unique items, sorts them by TWU, initializes tree nodes for single-item itemsets,
     * and grows the tree recursively.
     */
    private List<Itemset> generateItemsets() {
        // Collect unique items using a Set for efficiency.
        Set<Integer> uniqueItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .collect(Collectors.toSet());

        // Sort unique items in descending order based on TWU.
        List<Integer> sortedUniqueItemsByTWEU = uniqueItems.stream()
                .sorted((a, b) -> Float.compare(twu.getOrDefault(b, 0f), twu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        ShortTimePeriodTree root = new ShortTimePeriodTree(new ArrayList<>(), 0, 0, 0, 0);

        // For each unique item, initialize a single-item itemset and grow the tree.
        for (Integer item : sortedUniqueItemsByTWEU) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            List<Occurrence> occurrences = findOccurrences(currentItemset);
            if (occurrences.size() > 1) {
                ShortTimePeriodTree node = new ShortTimePeriodTree(currentItemset, 0, 0, 0, 0);
                root.children.put(item, node);
                stpTreeGrowth(node, occurrences);
            }
        }
        List<Itemset> results = new ArrayList<>(topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    /**
     * Recursively grows the ShortTimePeriodTree by extending the current node's itemset.
     * It computes new occurrences and utility measures for candidate extensions and prunes
     * branches that do not meet the required thresholds.
     */
    private void stpTreeGrowth(ShortTimePeriodTree node, List<Occurrence> occurrences) {
        int maxPeriod = calculateMaxPeriod(occurrences);
        if (maxPeriod > maxPer || maxPeriod == 0) return;

        float totalExpectedUtility = getTotalExpectedUtility(occurrences);
        if (totalExpectedUtility < minUtil) return;

        // Process and potentially add the current itemset to the top-K list.
        processCurrentItemset(node.itemset, occurrences);

        // Restrict candidate extensions to transactions that contain the current itemset.
        List<Integer> extensionItems = transactions.stream()
                .filter(t -> t.getItems().containsAll(node.itemset))
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.itemset.contains(item))
                .distinct()
                .sorted((a, b) -> Float.compare(twu.getOrDefault(b, 0f), twu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = calculatePSU(node.itemset, item);
            if (psu >= minUtil) {
                // Create an extended itemset.
                List<Integer> newItemset = new ArrayList<>(node.itemset);
                newItemset.add(item);

                // Recompute occurrences for the extended itemset.
                List<Occurrence> newOccurrences = findOccurrences(newItemset);
                if (newOccurrences.size() < 2) continue;

                int newMaxPeriod = calculateMaxPeriod(newOccurrences);
                if (newMaxPeriod > maxPer || newMaxPeriod == 0) continue;

                float newTotalExpUtil = getTotalExpectedUtility(newOccurrences);
                if (newTotalExpUtil < minUtil) continue;

                float newPosUtility = getTotalPositiveUtility(newOccurrences);
                float newNegUtility = getTotalNegativeUtility(newOccurrences);

                ShortTimePeriodTree childNode = new ShortTimePeriodTree(new ArrayList<>(newItemset),
                        newTotalExpUtil, newPosUtility, newNegUtility, newMaxPeriod);
                node.children.put(item, childNode);

                stpTreeGrowth(childNode, newOccurrences);
            }
        }
    }

    /**
     * Processes the current itemset. If it satisfies the constraints (utility and period),
     * the itemset is added to the top-K candidate list. It also updates the dynamic threshold.
     */
    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        String canonicalKey = getItemsetKey(currentItemset);
        if (topKSeen.contains(canonicalKey)) return;

        int maxPeriod = calculateMaxPeriod(occurrences);
        if (maxPeriod > maxPer || maxPeriod == 0) return;

        float totalExpectedUtility = getTotalExpectedUtility(occurrences);
        int totalUtility = getTotalUtility(occurrences);
        if (totalUtility < 0) return;

        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);

        if (topKItemsets.size() < k) {
            topKItemsets.offer(itemset);
            topKSeen.add(canonicalKey);
        } else {
            Itemset lowestUtilityItemset = topKItemsets.peek();
            if (lowestUtilityItemset != null && itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility()) {
                topKItemsets.poll();
                topKSeen.remove(getItemsetKey(lowestUtilityItemset.getItems()));
                topKItemsets.offer(itemset);
                topKSeen.add(canonicalKey);
            }
        }
        updateMinExpectedUtility();
    }

    /**
     * Dynamically updates the minimum expected utility threshold based on the current top-K itemsets.
     * This threshold is used for pruning less promising candidate itemsets.
     */
    private void updateMinExpectedUtility() {
        if (topKItemsets.size() >= k) {
            List<Itemset> sortedItemsets = new ArrayList<>(topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());

            float newMinUtil = sortedItemsets.get(sortedItemsets.size() - 1).getExpectedUtility();
            float priu = calculatePRIU() * 0.3f;
            float pliue = calculatePLIU_E() * 0.3f;
            float pliulb = calculatePLIU_LB();

            float dynamicThreshold = (newMinUtil * 0.4f) + (Math.min(priu, Math.min(pliue, pliulb)) * 0.6f);

            if (dynamicThreshold > minUtil * 1.1f) {
                minUtil = dynamicThreshold;
            }
        }
    }


    // ------------------------------------------- HELPER FUNCTIONS -----------------------------------//

    /**
     * Returns the canonical (sorted) order of the items.
     */
    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Generates a canonical key (String) for the itemset based on its sorted order.
     */
    private String getItemsetKey(List<Integer> items) {
        return getCanonicalOrder(items).toString();
    }

    // ------------------------------------------- RUN & EVALUATION -----------------------------------//

    /**
     * Evaluates top-K performance by running the algorithm for each k-value.
     * It measures runtime and memory usage, then prints the final top-K itemsets.
     */
    public void evaluateTopKPerformance() {
        List<Itemset> copyFinalResults = new ArrayList<>();
        dbUtil = calculateDatabaseUtility();
        float globalMinUtil = dbUtil * 0.001f;  // Shared minUtil across k-values
        float minUtilLowerBound = globalMinUtil * 0.00001f;


        for (int kValue : kValues) {
            k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + k + " Itemsets...");

            // RUNTIME MEASUREMENT BEFORE EXECUTION
            long startTime = System.nanoTime();

            // MEMORY MEASUREMENT BEFORE EXECUTION
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            while (!foundEnoughCandidates && globalMinUtil >= minUtilLowerBound) {
                reset();
                minUtil = globalMinUtil;
                filterLowUtilityItems();
                finalResults = generateItemsets();  // Save results

                if (finalResults.size() >= k) {
                    foundEnoughCandidates = true;
                    copyFinalResults = new ArrayList<>(finalResults);
                } else {
                    globalMinUtil *= 0.8f;  // Reduce minUtil globally
                }
            }

            // TIME MEASUREMENT AFTER EXECUTION
            double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("Top-%d Execution Time: %.2f s%n", k, executionTime);
            runTimeResults.put(k, executionTime);

            // MEMORY MEASUREMENT AFTER EXECUTION
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsedMB = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);
            System.out.printf("Top-%d Memory Usage: %.2f MB%n", k, memoryUsedMB);
            memoryResults.put(k, memoryUsedMB);

            // PRINT FINAL TOP-K ITEMSETS
            System.out.println("\n🔹 Final Top-" + k + " Itemsets:");
            if (copyFinalResults.isEmpty()) {
                System.out.println("⚠️ No itemsets satisfy your condition.");
            } else if (finalResults.isEmpty()) {
                int i = 1;
                for (Itemset itemset : copyFinalResults) {
                    System.out.println(i + ": " + itemset);
                    i+=1;
                }
            } else {
                int i = 1;
                for (Itemset itemset : finalResults) {
                    System.out.println(i + ": " + itemset);
                    i += 1;
                }
            }
        }
    }

    /**
     * Resets the algorithm state: clears all tracking structures and recomputes TWEU.
     */
    private void reset() {
        topKItemsets.clear();
        topKSeen.clear();
        twu.clear();
        posUtil.clear();
        negUtil.clear();
        psuCache.clear();
        transactions = new ArrayList<>(originalTransactions);
        computeTWU();
    }
}