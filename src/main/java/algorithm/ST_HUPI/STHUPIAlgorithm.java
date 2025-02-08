package algorithm.ST_HUPI;

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
public class STHUPIAlgorithm {
    // Fields for transactions and algorithm parameters.
    private List<Transaction> transactions;
    private int[] kValues;                   // Array of k-values to evaluate.
    private int k;                           // Current k value.
    private float minUtil;                   // Minimum expected utility threshold.
    private int dbUtil;                      // Total database utility.
    private PriorityQueue<Itemset> topKItemsets; // Top-K candidate itemsets.
    private Map<Integer, Float> twu;         // Transaction-weighted utility map.
    private Map<Integer, Float> posUtil;     // Positive utility map.
    private Map<Integer, Float> negUtil;     // Negative utility map.
    private Map<String, Float> psuCache;     // Cache for PSU values (not using period, but kept for extension utility computations).
    private final Set<String> processedPSU = new HashSet<>();
    private final List<Transaction> originalTransactions;
    private Set<String> topKSeen;            // Tracks already processed itemsets.
    private Map<Integer, Double> runTimeResults = new LinkedHashMap<>();
    private Map<Integer, Double> memoryResults = new LinkedHashMap<>();

    public STHUPIAlgorithm(List<Transaction> transactions, int[] kValues) {
        this.originalTransactions = new ArrayList<>(transactions);
        this.transactions = new ArrayList<>(transactions);
        this.kValues = kValues;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.twu = new HashMap<>();
        this.posUtil = new HashMap<>();
        this.negUtil = new HashMap<>();
        this.psuCache = new HashMap<>();
        this.topKSeen = new HashSet<>();
    }

    // ---------- PRIU STRATEGIES ----------
    private float calculatePRIU() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> posUtil.getOrDefault(item, 0f))
                        .sum())
                .max().orElse(0);
    }
    private float calculatePLIU_E() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> {
                    List<Integer> sortedItems = transaction.getItems().stream()
                            .sorted(Comparator.comparingDouble(item -> posUtil.getOrDefault(item, 0f)))
                            .collect(Collectors.toList());
                    return sortedItems.stream().limit(2)
                            .mapToDouble(item -> posUtil.getOrDefault(item, 0f))
                            .sum();
                }).max().orElse(0);
    }
    private float calculatePLIU_LB() {
        if (topKItemsets.isEmpty()) return 0;
        return (float) topKItemsets.stream()
                .mapToDouble(Itemset::getExpectedUtility)
                .min().orElse(0);
    }

    // ---------- DATABASE & TRANSACTION UTILITY ----------
    private int calculateDatabaseUtility() {
        return transactions.stream()
                .mapToInt(Transaction::getTransactionUtility)
                .sum();
    }
    private float calculateExpectedTransactionUtility(Transaction transaction) {
        float probability = (float) transaction.getTransactionUtility() / dbUtil;
        return probability * transaction.getTransactionUtility();
    }
    private float calculateExpectedDatabaseUtility() {
        float expectedDatabaseUtility = 0f;
        for (Transaction transaction : transactions) {
            expectedDatabaseUtility += calculateExpectedTransactionUtility(transaction);
        }
        return expectedDatabaseUtility;
    }

    // ---------- UTILITY & OCCURRENCE CALCULATIONS ----------
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }
    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream()
                .filter(transaction -> transaction.getItems().containsAll(itemset))
                .map(transaction -> {
                    int utility = calculateUtility(transaction, itemset);
                    float pos = Math.max(utility, 0);
                    float neg = Math.min(utility, 0);
                    float probability = (pos + Math.abs(neg)) > 0
                            ? (pos + neg) / transaction.getTransactionUtility() : 0;
                    float expUtil = (pos + neg) * probability;
                    return new Occurrence(transaction.getId(), probability, utility, expUtil);
                }).collect(Collectors.toList());
    }

    private float getTotalExpectedUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(Occurrence::getExpectedUtility).sum();
    }
    private int getTotalUtility(List<Occurrence> occurrences) {
        return occurrences.stream().mapToInt(Occurrence::getUtility).sum();
    }
    private float getTotalPositiveUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(o -> Math.max(o.getUtility(), 0)).sum();
    }
    private float getTotalNegativeUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(o -> Math.min(o.getUtility(), 0)).sum();
    }

    // ---------- PRUNING STRATEGY ----------
    private void filterLowUtilityItems() {
        transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> twu.getOrDefault(item, 0f) < minUtil);
            return transaction.getItems().isEmpty();
        });
    }

    // ---------- PSU (Positive Sub-tree Utility) ----------
    public float calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (processedPSU.contains(key)) return 0;
        processedPSU.add(key);
        float maxPSU = 0;
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
            float computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            maxPSU = Math.max(maxPSU, computedPSU);
        }
        psuCache.put(key, maxPSU);
        return maxPSU;
    }

    // ---------- TWU COMPUTING ----------
    private void computeTWU() {
        for (Transaction transaction : transactions) {
            float transactionUtility = (float) transaction.getTransactionUtility();
            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);
                if (utility >= 0) {
                    posUtil.merge(item, utility, Float::sum);
                } else {
                    negUtil.merge(item, utility, Float::sum);
                    transactionUtility += utility; // Adjust with negative utility.
                }
            }
            for (int item : transaction.getItems()) {
                twu.merge(item, transactionUtility, Float::sum);
            }
        }
    }

    // ---------- ITEMSET GENERATION AND TREE GROWTH ----------
    private List<Itemset> generateItemsets() {
        // Collect unique items from transactions.
        Set<Integer> uniqueItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .collect(Collectors.toSet());
        // Sort unique items by TWEU descending.
        List<Integer> sortedUniqueItemsByTWEU = uniqueItems.stream()
                .sorted((a, b) -> Float.compare(twu.getOrDefault(b, 0f), twu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        // Create a dummy tree root.
        ShortTimeTree root = new ShortTimeTree(new ArrayList<>(), 0, 0, 0);

        // For each unique item, generate a single-item itemset and grow the tree.
        for (Integer item : sortedUniqueItemsByTWEU) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            List<Occurrence> occurrences = findOccurrences(currentItemset);
            if (occurrences.size() > 1) { // Only consider if occurs in more than one transaction.
                ShortTimeTree node = new ShortTimeTree(new ArrayList<>(currentItemset), 0, 0, 0);
                root.children.put(item, node);
                stTreeGrowth(node, occurrences);
            }
        }

        List<Itemset> results = new ArrayList<>(topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    // Recursively grows the tree using probability-based expected utility.
    private void stTreeGrowth(ShortTimeTree node, List<Occurrence> occurrences) {
        // In this method, we ignore period; only expected utility matters.
        float totalExpUtil = getTotalExpectedUtility(occurrences);
        if (totalExpUtil < minUtil) return;

        // Process current itemset.
        processCurrentItemset(node.itemset, occurrences);

        // Get extension candidates only from transactions containing the current itemset.
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
                List<Integer> newItemset = new ArrayList<>(node.itemset);
                newItemset.add(item);
                List<Occurrence> newOccurrences = findOccurrences(newItemset);
                if (newOccurrences.size() < 2) continue;
                float newTotalExpUtil = getTotalExpectedUtility(newOccurrences);
                if (newTotalExpUtil < minUtil) continue;
                float newPosUtil = getTotalPositiveUtility(newOccurrences);
                float newNegUtil = getTotalNegativeUtility(newOccurrences);

                ShortTimeTree childNode = new ShortTimeTree(new ArrayList<>(newItemset),
                        newTotalExpUtil, newPosUtil, newNegUtil);
                node.children.put(item, childNode);

                stTreeGrowth(childNode, newOccurrences);
            }
        }
    }

    // Processes the current itemset and adds it to the top-K list if it meets criteria.
    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        String canonicalKey = getItemsetKey(currentItemset);
        if (topKSeen.contains(canonicalKey)) return;

        float totalExpUtil = getTotalExpectedUtility(occurrences);
        int totalUtil = getTotalUtility(occurrences);
        if (totalUtil < 0) return;

        // Create the itemset with expected utility computed with probability.
        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtil, totalExpUtil, 0);
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

    // Updates the minimum utility threshold based on current top-K itemsets.
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

    // ---------- HELPER FUNCTIONS ----------
    // Returns the canonical (sorted) order of items.
    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }
    // Returns a string key for an itemset (its canonical order as a string).
    private String getItemsetKey(List<Integer> items) {
        return getCanonicalOrder(items).toString();
    }

    // ---------- RUN & EVALUATION METHODS ----------
    // Runs the algorithm for each k-value and measures runtime and memory usage.
    public void evaluateTopKPerformance() {
        List<Itemset> copyFinalResults = new ArrayList<>();
        dbUtil = calculateDatabaseUtility();
        float expectedDatabaseUtility = calculateExpectedDatabaseUtility();
        float globalMinUtil = expectedDatabaseUtility * 0.001f;  // Shared min utility
        float minUtilLowerBound = globalMinUtil * 0.00001f;


        for (int kValue : kValues) {
            k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + k + " Itemsets...");

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

    // Resets algorithm state to the original transactions and recomputes TWEU.
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
