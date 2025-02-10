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
public class StpHuiAlgorithm {
    // Fields for dataset, parameters, and utility tracking.
    private List<Transaction> transactions;
    private int maxPer;                          // Maximum allowed period for an itemset.
    private int k;                               // Current top-K value.
    private float minUtil;                       // Minimum utility threshold.
    private PriorityQueue<Itemset> topKItemsets; // Priority queue to maintain top-K itemsets.
    private Map<Integer, Integer> twu;             // Transaction-weighted utility map.
    private Map<Integer, Integer> pwtu;         // Positive Transaction-weighted utility map.
    private final Set<String> processedPSU = new HashSet<>(); // Set to avoid duplicate PSU computations.
    private Set<String> topKSeen;                // Set to track processed (canonical) itemset keys.
    private double runTime; // Runtime result per k-value.
    private double memoryUsed;  // Memory usage per k-value.

    // Constructor.
    public StpHuiAlgorithm(List<Transaction> transactions, int k, int maxPer) {
        this.transactions = new ArrayList<>(transactions);
        this.k = k;
        this.maxPer = maxPer;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getUtility));
        this.twu = new HashMap<>();
        this.pwtu = new HashMap<>();
        this.topKSeen = new HashSet<>();
    }

    // --------------------------- THRESHOLD RAISING STRATEGIES ---------------------------//

    /**
     * Calculates the maximum Positive Remaining Item Utility (PRIU) over all transactions.
     * For each transaction, it sums the positive utilities (from posUtil) of all items and returns the maximum sum.
     */
    private float calculatePRIU() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> this.pwtu.getOrDefault(item, 0))
                        .sum())
                .max().orElse(0);
    }

    /**
     * Positive Leaf Itemset Utility Exact strategy (PLIU_E): for each transaction, it sorts items by positive utility and sums the top two values.
     * Returns the maximum sum among transactions.
     */
    private float calculatePLIU_E() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> {
                    List<Integer> sortedItems = transaction.getItems().stream()
                            .sorted(Comparator.comparingDouble(item -> this.pwtu.getOrDefault(item, 0)))
                            .collect(Collectors.toList());
                    return sortedItems.stream().limit(2)
                            .mapToDouble(item -> this.pwtu.getOrDefault(item, 0))
                            .sum();
                }).max().orElse(0);
    }

    /**
     * Positive Leaf Itemset Utility Lower Bound strategy (PLIU_LB)
     * Returns the smallest raw utility among the current top-k itemsets.
     */
    private float calculatePLIU_LB() {
        if (this.topKItemsets.isEmpty()) return 0;
        return (float) this.topKItemsets.stream()
                .mapToDouble(Itemset::getUtility)
                .min().orElse(0);
    }

    // ------------- UTILITY & OCCURRENCE CALCULATIONS -------------//

    /**
     * Calculates the utility of an itemset in a given transaction.
     */
    private int calculateItemsetUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }

    /**
     * Finds all occurrences of an itemset in the transactions.
     * For each transaction containing the itemset, the method computes:
     * - raw utility,
     */
    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream()
                .filter(transaction -> transaction.getItems().containsAll(itemset))
                .map(transaction -> {
                    int utility = this.calculateItemsetUtility(transaction, itemset);
                    return new Occurrence(transaction.getId(), 1f, utility, 1f);
                }).collect(Collectors.toList());
    }

    /**
     * Calculates the maximum period (largest gap between consecutive occurrences) for an itemset.
     */
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

    /**
     * Sums the raw utilities for all occurrences of an itemset.
     */
    private int getTotalUtility(List<Occurrence> occurrences) {
        return occurrences.stream().mapToInt(Occurrence::getUtility).sum();
    }

    // ------------- PRUNING STRATEGY -------------//

    /**
     * Filters out low-utility items from transactions based on the current minUtil threshold.
     */
    private void filterLowUtilityItems() {
        this.transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> (float) this.twu.getOrDefault(item, 0) < this.minUtil);
            return transaction.getItems().isEmpty();
        });
    }


    // ------------- PSU (Positive Sub-tree Utility) -------------//

    /**
     * Calculates the PSU for a given prefix and candidate extension.
     * Returns the maximum PSU computed across transactions.
     */
    private int calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (this.processedPSU.contains(key)) return 0;
        this.processedPSU.add(key);
        int maxPSU = 0;
        for (Transaction transaction : this.transactions) {
            if (!transaction.getItems().containsAll(prefix) || !transaction.getItems().contains(extensionItem)) {
                continue;
            }
            int prefixUtility = this.calculateItemsetUtility(transaction, prefix);
            int extensionUtility = this.calculateItemsetUtility(transaction, List.of(extensionItem));
            int adjustedExtensionUtility = Math.max(extensionUtility, 0);
            int remainingPositiveUtility = this.calculateRPU(transaction, prefix, extensionItem);
            int computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            // Track max PSU instead of sum
            maxPSU = Math.max(maxPSU, computedPSU);
        }
        return maxPSU;
    }

    private int calculateRPU(Transaction transaction, List<Integer> prefix, int extensionItem) {
        return transaction.getItems().stream()
                .filter(item -> !prefix.contains(item) && item != extensionItem)
                .mapToInt(item -> {
                    int idx = transaction.getItems().indexOf(item);
                    if (idx == -1) return 0;
                    int utility = transaction.getUtilities().get(idx);
                    return Math.max(utility, 0);
                }).sum();
    }


    // ------------- TWU COMPUTING -------------//

    /**
     * Computes the Transaction-Weighted Utility (TWU) for each item and updates posUtil and negUtil maps.
     */
    private void computeTWU() {
        for (Transaction transaction : transactions) {
            int twu = transaction.getTransactionUtility();

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                int utility = transaction.getUtilities().get(i);

                if (utility >= 0) pwtu.merge(item, utility, Integer::sum);
                else {
                    twu += utility; // Adjust transaction utility with negative values.
                }
            }

            for (int item : transaction.getItems()) {
                this.twu.merge(item, twu, Integer::sum);
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
        Set<Integer> uniqueItems = this.transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .collect(Collectors.toSet());

        // Sort unique items in descending order based on TWU.
        List<Integer> sortedUniqueItemsByTWU = uniqueItems.stream()
                .sorted((a, b) -> Integer.compare(this.twu.getOrDefault(b, 0), this.twu.getOrDefault(a, 0)))
                .collect(Collectors.toList());

        StpHuiTree root = new StpHuiTree(new ArrayList<>(), 0, 0);

        // For each unique item, initialize a single-item itemset and grow the tree.
        for (Integer item : sortedUniqueItemsByTWU) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            List<Occurrence> occurrences = this.findOccurrences(currentItemset);
            if (occurrences.size() > 1) {
                int maxPeriod = this.calculateMaxPeriod(occurrences);
                if (maxPeriod > this.maxPer) continue;
                int utility = this.getTotalUtility(occurrences);
                if (utility < 0) continue;

                StpHuiTree node = new StpHuiTree(currentItemset, utility, maxPeriod);
                Map<Integer, StpHuiTree> children = node.getChildren();
                children.put(item, node);
                root.setChildren(children);
                this.stpHuiTreeGrowth(node);
            }
        }
        List<Itemset> results = new ArrayList<>(this.topKItemsets);
        results.sort(Comparator.comparing(Itemset::getUtility).reversed());
        return results;
    }

    /**
     * Recursively grows the ShortTimePeriodTree by extending the current node's itemset.
     * It computes new occurrences and utility measures for candidate extensions and prunes
     * branches that do not meet the required thresholds.
     */
    private void stpHuiTreeGrowth(StpHuiTree node) {
        if (node.getMaxPeriod() > this.maxPer) return;

        if (node.getUtility() < this.minUtil) return;

        // Process and potentially add the current itemset to the top-K list.
        this.processCurrentItemset(node);

        // Restrict candidate extensions to transactions that contain the current itemset.
        List<Integer> extensionItems = this.transactions.stream()
                .filter(t -> t.getItems().containsAll(node.getItemset()))
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.getItemset().contains(item))
                .distinct()
                .sorted((a, b) -> Integer.compare(this.twu.getOrDefault(b, 0), this.twu.getOrDefault(a, 0)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = (float) this.calculatePSU(node.getItemset(), item);
            if (psu >= this.minUtil) {
                // Create an extended itemset.
                List<Integer> newItemset = new ArrayList<>(node.getItemset());
                newItemset.add(item);

                // Recompute occurrences for the extended itemset.
                List<Occurrence> newOccurrences = this.findOccurrences(newItemset);
                if (newOccurrences.size() < 2) continue;

                int newMaxPeriod = this.calculateMaxPeriod(newOccurrences);
                if (newMaxPeriod > this.maxPer || newMaxPeriod == 0) continue;

                int newTotalUtil = this.getTotalUtility(newOccurrences);
                if (newTotalUtil < this.minUtil) continue;

                StpHuiTree childNode = new StpHuiTree(
                        new ArrayList<>(newItemset), newTotalUtil, newMaxPeriod);
                Map<Integer, StpHuiTree> children = node.getChildren();
                children.put(item, node);
                node.setChildren(children);

                this.stpHuiTreeGrowth(childNode);
            }
        }
    }

    /**
     * Processes the current itemset. If it satisfies the constraints (utility and period),
     * the itemset is added to the top-K candidate list. It also updates the dynamic threshold.
     */
    private void processCurrentItemset(StpHuiTree node) {
        List<Integer> currentItemset = node.getItemset();
        String canonicalKey = this.getItemsetKey(currentItemset);
        if (this.topKSeen.contains(canonicalKey)) return;

        Itemset itemset = new Itemset(currentItemset, node.getUtility(), 1f, node.getMaxPeriod());

        if (this.topKItemsets.size() < this.k) {
            this.topKItemsets.offer(itemset);
            this.topKSeen.add(canonicalKey);
        } else {
            Itemset lowestUtilityItemset = this.topKItemsets.peek();
            if (lowestUtilityItemset != null && itemset.getUtility() > lowestUtilityItemset.getUtility()) {
                this.topKItemsets.poll();
                this.topKSeen.remove(getItemsetKey(lowestUtilityItemset.getItems()));
                this.topKItemsets.offer(itemset);
                this.topKSeen.add(canonicalKey);
            }
        }
        this.updateMinUtil();
    }

    /**
     * Dynamically updates the minimum utility threshold based on the current top-K itemsets.
     * This threshold is used for pruning less promising candidate itemsets.
     */
    private void updateMinUtil() {
        if (this.topKItemsets.size() >= this.k) {
            List<Itemset> sortedItemsets = new ArrayList<>(this.topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getUtility).reversed());
            int newMinUtil = sortedItemsets.get(sortedItemsets.size() - 1).getUtility();

            // Use the threshold raising strategies to update minUtil
            float priu = calculatePRIU() * 0.3f;
            float pliuE = calculatePLIU_E() * 0.3f;
            float pliuLB = calculatePLIU_LB();
            float dynamicThreshold = (newMinUtil * 0.4f) + (Math.min(priu, Math.min(pliuE, pliuLB)) * 0.6f);

            if (dynamicThreshold > this.minUtil * 1.1f) {
                this.minUtil = dynamicThreshold;
//                System.out.println("Updated MinUtil: " + this.minUtil);
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
        return this.getCanonicalOrder(items).toString();
    }

    // ------------------------------------------- RUN & EVALUATION -----------------------------------//

    /**
     * Evaluates top-K performance by running the algorithm for k-value.
     * It measures runtime and memory usage, then prints the final top-K itemsets.
     */
    public void evaluateTopKPerformance() {
        this.computeTWU();
        this.filterLowUtilityItems();
//        System.out.println("Initial Minimum Utility: " + minUtil);

        // Measure runtime and memory for the candidate generation process.
        long startTime = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        List<Itemset> allCandidates = this.generateItemsets();

        this.runTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        this.memoryUsed = (memoryAfter - memoryBefore) / (1024.0 * 1024.0);


        System.out.printf("Execution Time: %.2f s%n", this.runTime);
        System.out.printf("Memory Usage: %.2f MB%n", this.memoryUsed);
        System.out.println("\n🔹 Final Top-" + this.k + " Itemsets:");

        if (allCandidates.isEmpty()) {
            System.out.println("⚠️ No itemsets satisfy your condition.");
        } else if (allCandidates.size() < this.k) {
            System.out.println("⚠️ Only " + allCandidates.size() + " itemsets found.");
            for (int i = 0; i < allCandidates.size(); i++) {
                System.out.println((i + 1) + ": " + allCandidates.get(i));
            }
        } else {
            for (int i = 0; i < this.k; i++) {
                System.out.println((i + 1) + ": " + allCandidates.get(i));
            }
        }
    }
}