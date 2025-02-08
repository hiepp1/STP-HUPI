package algorithm.STP_HUI;

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
public class STPHUIAlgorithm {
    // Fields for transactions, parameters, and tracking structures.
    private List<Transaction> transactions;
    private int[] kValues;                   // Array of different k-values to test.
    private int maxPer;                      // Maximum allowed period for an itemset.
    private int k;                           // Current top-K value.
    private float minUtil;                   // Minimum utility threshold (raw utility, not probability-based).
    private int dbUtil;                      // Total database utility.
    private PriorityQueue<Itemset> topKItemsets;  // Priority queue to hold current top-K itemsets.
    private Map<Integer, Integer> twu;         // Transaction-Weighted Utility map.
    private Map<Integer, Integer> posUtil;     // Positive utility map.
    private Map<Integer, Integer> negUtil;     // Negative utility map.
    private Map<String, Integer> psuCache;     // Cache for PSU values.
    private final Set<String> processedPSU = new HashSet<>(); // Set to avoid duplicate PSU computations.
    private final List<Transaction> originalTransactions; // Original list of transactions.
    private Set<String> topKSeen;            // Set to track already processed itemsets (canonical key).
    private Map<Integer, Double> runTimeResults = new LinkedHashMap<>();  // Runtime results per k-value.
    private Map<Integer, Double> memoryResults = new LinkedHashMap<>();   // Memory usage per k-value.

    // Constructor.
    public STPHUIAlgorithm(List<Transaction> transactions, int[] kValues, int maxPer) {
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

    // ---------- PRIU STRATEGIES ----------
    private int calculatePRIU() {
        return transactions.stream()
                .mapToInt(transaction -> transaction.getItems().stream()
                        .mapToInt(item -> posUtil.getOrDefault(item, 0))
                        .sum())
                .max().orElse(0);
    }

    private int calculatePLIU_E() {
        return transactions.stream()
                .mapToInt(transaction -> {
                    List<Integer> sortedItems = transaction.getItems().stream()
                            .sorted(Comparator.comparingInt(item -> posUtil.getOrDefault(item, 0)))
                            .collect(Collectors.toList());
                    return sortedItems.stream().limit(2)
                            .mapToInt(item -> posUtil.getOrDefault(item, 0))
                            .sum();
                }).max().orElse(0);
    }

    private int calculatePLIU_LB() {
        if (topKItemsets.isEmpty()) return 0;
        return topKItemsets.stream()
                .mapToInt(Itemset::getUtility)
                .min().orElse(0);
    }

    // ---------- DATABASE & TRANSACTION UTILITY ----------
    private int calculateDatabaseUtility() {
        return transactions.stream()
                .mapToInt(Transaction::getTransactionUtility)
                .sum();
    }

    // ---------- UTILITY & OCCURRENCE CALCULATIONS ----------
    // Calculates raw utility of an itemset in a transaction.
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }

    // Finds occurrences of an itemset in transactions.
    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream()
                .filter(transaction -> transaction.getItems().containsAll(itemset))
                .map(transaction -> {
                    int utility = calculateUtility(transaction, itemset);
                    return new Occurrence(transaction.getId(), 1.0f, utility, utility);
                }).collect(Collectors.toList());
    }

    // Calculates the maximum period (largest gap between transaction IDs) for an itemset.
    private int calculateMaxPeriod(List<Occurrence> occurrences) {
        if (occurrences.size() < 2) return 0;
        List<Integer> indices = occurrences.stream()
                .map(Occurrence::getTransactionID)
                .sorted()
                .collect(Collectors.toList());
        int maxPeriod = indices.get(0);
        for (int i = 0; i < indices.size() - 1; i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i + 1) - indices.get(i));
        }
        return maxPeriod;
    }

    // Sum of raw utilities over all occurrences.
    private int getTotalUtility(List<Occurrence> occurrences) {
        return occurrences.stream()
                .mapToInt(Occurrence::getUtility)
                .sum();
    }

    private int getTotalPositiveUtility(List<Occurrence> occurrences) {
        return occurrences.stream()
                .mapToInt(o -> Math.max(o.getUtility(), 0))
                .sum();
    }

    private int getTotalNegativeUtility(List<Occurrence> occurrences) {
        return occurrences.stream()
                .mapToInt(o -> Math.min(o.getUtility(), 0))
                .sum();
    }

    // ---------- PRUNING STRATEGY ----------
    // Removes items whose TWU (raw utility based) is below the minUtil threshold.
    private void filterLowUtilityItems() {
        transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> twu.getOrDefault(item, 0) < minUtil);
            return transaction.getItems().isEmpty();
        });
    }

    // ---------- PSU (Projected Sequential Utility) ----------
    // Computes PSU for extending an itemset; using raw utilities.
    public int calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (processedPSU.contains(key)) return 0;
        processedPSU.add(key);
        int maxPSU = 0;
        for (Transaction transaction : transactions) {
            if (!transaction.getItems().containsAll(prefix) || !transaction.getItems().contains(extensionItem)) {
                continue;
            }
            int prefixUtility = calculateUtility(transaction, prefix);
            int extensionUtility = calculateUtility(transaction, List.of(extensionItem));
            int adjustedExtensionUtility = Math.max(extensionUtility, 0);
            int remainingPositiveUtility = transaction.getItems().stream()
                    .filter(item -> !prefix.contains(item) && item != extensionItem)
                    .mapToInt(item -> {
                        int index = transaction.getItems().indexOf(item);
                        if (index == -1) return 0;
                        int util = transaction.getUtilities().get(index);
                        return Math.max(util, 0);
                    }).sum();
            int computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            maxPSU = Math.max(maxPSU, computedPSU);
        }
        psuCache.put(key, maxPSU);
        return maxPSU;
    }

    // ---------- TWU COMPUTING ----------
    // Computes the Transaction-Weighted Utility (TWU) using raw utilities.
    private void computeTWU() {
        for (Transaction transaction : transactions) {
            int transactionUtility = transaction.getTransactionUtility();
            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                int utility = transaction.getUtilities().get(i);
                if (utility >= 0) {
                    posUtil.merge(item, utility, Integer::sum);
                } else {
                    negUtil.merge(item, utility, Integer::sum);
                    transactionUtility += utility;
                }
            }
            for (int item : transaction.getItems()) {
                twu.merge(item, transactionUtility, Integer::sum);
            }
        }
    }

    // ---------- ITEMSET GENERATION AND TREE GROWTH ----------
    // Generates candidate itemsets by building a ShortTimePeriodTree.
    private List<Itemset> generateItemsets() {
        // Collect unique items from transactions.
        Set<Integer> uniqueItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .collect(Collectors.toSet());
        // Sort unique items descending by TWEU.
        List<Integer> sortedUniqueItemsByTWEU = uniqueItems.stream()
                .sorted((a, b) -> Float.compare(twu.getOrDefault(b, 0), twu.getOrDefault(a, 0)))
                .collect(Collectors.toList());

        ShortTimePeriodTree root = new ShortTimePeriodTree(new ArrayList<>(), 0, 0, 0, 0);

        // For each unique item, initialize a single-item itemset and grow the tree.
        for (Integer item : sortedUniqueItemsByTWEU) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            List<Occurrence> occurrences = findOccurrences(currentItemset);
            if (occurrences.size() > 1) {
                ShortTimePeriodTree node = new ShortTimePeriodTree(new ArrayList<>(currentItemset), 0, 0, 0, 0);
                root.children.put(item, node);
                stpTreeGrowth(node, occurrences);
            }
        }

        List<Itemset> results = new ArrayList<>(topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    // Recursively grows the tree by extending the current itemset and applying pruning based on raw utility and period.
    private void stpTreeGrowth(ShortTimePeriodTree node, List<Occurrence> occurrences) {
        int maxPeriod = calculateMaxPeriod(occurrences);
        if (maxPeriod > maxPer || maxPeriod == 0) return;

        float totalUtil = getTotalUtility(occurrences);
        if (totalUtil < minUtil) return;

        processCurrentItemset(node.itemset, occurrences);

        // Restrict candidate extensions to transactions that contain the current itemset.
        List<Integer> extensionItems = transactions.stream()
                .filter(t -> t.getItems().containsAll(node.itemset))
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.itemset.contains(item))
                .distinct()
                .sorted((a, b) -> Float.compare(twu.getOrDefault(b, 0), twu.getOrDefault(a, 0)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = calculatePSU(node.itemset, item);
            if (psu >= minUtil) {
                List<Integer> newItemset = new ArrayList<>(node.itemset);
                newItemset.add(item);
                List<Occurrence> newOccurrences = findOccurrences(newItemset);
                if (newOccurrences.size() < 2) continue;
                int newTotalExpUtil = getTotalUtility(newOccurrences);
                if (newTotalExpUtil < minUtil) continue;
                int newPosUtil = getTotalPositiveUtility(newOccurrences);
                int newNegUtil = getTotalNegativeUtility(newOccurrences);

                ShortTimePeriodTree childNode = new ShortTimePeriodTree(new ArrayList<>(newItemset),
                        newTotalExpUtil, newPosUtil, newNegUtil, calculateMaxPeriod(newOccurrences));
                node.children.put(item, childNode);

                stpTreeGrowth(childNode, newOccurrences);
            }
        }
    }

    // Processes the current itemset and adds it to the top-K list if it meets the criteria.
    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        String canonicalKey = getItemsetKey(currentItemset);
        if (topKSeen.contains(canonicalKey)) return;

        int maxPeriod = calculateMaxPeriod(occurrences);
        if (maxPeriod > maxPer || maxPeriod == 0) return;
        float totalExpUtil = getTotalUtility(occurrences);
        int totalUtil = getTotalUtility(occurrences);
        if (totalUtil < 0) return;

        // Here, expected utility equals the raw utility.
        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtil, totalExpUtil, maxPeriod);
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
        updateMinUtility();
    }

    // Dynamically updates the minimum utility threshold based on the current top-K itemsets.
    private void updateMinUtility() {
        if (topKItemsets.size() >= k) {
            List<Itemset> sortedItemsets = new ArrayList<>(topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
            int newMinUtil = sortedItemsets.get(sortedItemsets.size() - 1).getUtility();
            float priu = calculatePRIU() * 0.3f;
            float pliue = calculatePLIU_E() * 0.3f;
            int pliulb = calculatePLIU_LB();
            float dynamicThreshold = (newMinUtil * 0.4f) + (Math.min(priu, Math.min(pliue, pliulb)) * 0.6f);
            if (dynamicThreshold > minUtil * 1.1f) {
                minUtil = dynamicThreshold;
                System.out.println("Updated minExpectedUtility: " + minUtil);
            }
        }
    }

    // ---------- HELPER FUNCTIONS ----------
    // Returns the canonical (sorted) order of items.
    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }

    // Returns a canonical key (string) for the itemset.
    private String getItemsetKey(List<Integer> items) {
        return getCanonicalOrder(items).toString();
    }

    // ---------- RUN & EVALUATION METHODS ----------
    // Evaluates top-K performance (runtime and memory usage) for each k-value.
    public void evaluateTopKPerformance() {
        List<Itemset> copyFinalResults = new ArrayList<>();
        dbUtil = calculateDatabaseUtility();
        float globalMinUtil = dbUtil * 0.001f;  // Shared min utility
        float minUtilLowerBound = globalMinUtil * 0.00001f;

        System.out.println("Database Utility: " + dbUtil);
        System.out.println("Minimum Utility: " + globalMinUtil);

        for (int kValue : kValues) {
            k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + k + " Itemsets...");

            // Runtime measurement.
            long startTime = System.nanoTime();
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            while (!foundEnoughCandidates && globalMinUtil >= minUtilLowerBound) {
                reset();
                minUtil = globalMinUtil;
                filterLowUtilityItems();
                finalResults = generateItemsets();
                if (finalResults.size() >= k) {
                    foundEnoughCandidates = true;
                    copyFinalResults = new ArrayList<>(finalResults);
                } else {
                    globalMinUtil *= 0.8f;
                }
            }

            double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("Top-%d Execution Time: %.2f s%n", k, executionTime);
            runTimeResults.put(k, executionTime);

            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsedMB = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);
            System.out.printf("Top-%d Memory Usage: %.2f MB%n", k, memoryUsedMB);
            memoryResults.put(k, memoryUsedMB);

            System.out.println("\n🔹 Final Top-" + k + " Itemsets:");
            if (copyFinalResults.isEmpty()) {
                System.out.println("⚠️ No itemsets satisfy your condition.");
            } else {
                int i = 1;
                for (Itemset itemset : finalResults) {
                    System.out.println(i + ": " + itemset);
                    i++;
                }
            }
        }
    }

    // Resets algorithm state and recomputes TWEU.
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
