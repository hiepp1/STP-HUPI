package algorithm.STP_HUPI;

import algorithm.Itemset;
import algorithm.Occurrence;
import algorithm.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The StpHupiAlgorithm class implements the Short Time Period High Utility Probabilistic Itemsets (STP-HUPI)
 * mining algorithm using a tree-based candidate generation approach. It uses a StpHupiTree to recursively extend
 * candidate itemsets while enforcing utility and period constraints, and applies dynamic threshold raising strategies
 * to prune the search space. This algorithm handles both positive and negative utilities.
 */
@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class StpHupiAlgorithm {
    // --------------------------- Fields ---------------------------
    private List<Transaction> transactions;      // The list of transactions to process.
    private int maxPer;                          // Maximum allowed period for an itemset.
    private float threshold;
    private int k;                               // Current top-K value.
    private float minUtil;                       // Minimum expected utility threshold.
    private PriorityQueue<Itemset> topKItemsets; // Priority queue to maintain top-K itemsets.
    private Map<Integer, Float> twu;             // Transaction-weighted utility map.
    private Map<Integer, Float> posUtil;         // Positive utility map.
    private final Set<String> processedPSU = new HashSet<>(); // Set to avoid duplicate PSU computations.
    private Set<String> topKSeen;                // Set to track processed (canonical) itemset keys.
    private double runTime; // Runtime result per k-value.
    private double memoryUsed;  // Memory usage per k-value.

    // --------------------------- Constructor ---------------------------

    /**
     * Constructs a StpHupiAlgorithm instance with the specified transactions, top-K value, and maximum period.
     *
     * @param transactions the list of transactions to process.
     * @param k the top-K parameter.
     * @param maxPer the maximum allowed period.
     */
    public StpHupiAlgorithm(List<Transaction> transactions, int k, int maxPer, float threshold) {
        this.transactions = new ArrayList<>(transactions);
        this.k = k;
        this.maxPer = maxPer;
        this.threshold = threshold;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.twu = new HashMap<>();
        this.posUtil = new HashMap<>();
        this.topKSeen = new HashSet<>();
    }

    private int calculateDbUtil() {
        return this.transactions.stream().mapToInt(Transaction::getTransactionUtility).sum();
    }

    private void initialMinUtil() {
        int dbUtil = this.calculateDbUtil();
        this.minUtil = dbUtil * this.threshold;
    }
    // --------------------------- THRESHOLD RAISING STRATEGIES ---------------------------

    /**
     * Calculates the maximum Positive Remaining Item Utility (PRIU) over all transactions.
     * For each transaction, it sums the positive utilities (from posUtil) of all items and returns the maximum sum.
     *
     * @return the maximum PRIU value.
     */
    private float calculatePRIU() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> this.posUtil.getOrDefault(item, 0f))
                        .sum())
                .max().orElse(0);
    }

    /**
     * Computes the Positive Leaf Itemset Utility Exact strategy (PLIU_E).
     * For each transaction, it sorts items by positive utility and sums the top two values,
     * then returns the maximum sum across all transactions.
     *
     * @return the maximum PLIU_E value.
     */
    private float calculatePLIU_E() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> {
                    List<Integer> sortedItems = transaction.getItems().stream()
                            .sorted(Comparator.comparingDouble(item -> this.posUtil.getOrDefault(item, 0f)))
                            .collect(Collectors.toList());
                    return sortedItems.stream().limit(2)
                            .mapToDouble(item -> this.posUtil.getOrDefault(item, 0f))
                            .sum();
                }).max().orElse(0);
    }

    /**
     * Computes the Positive Leaf Itemset Utility Lower Bound (PLIU_LB) by returning the smallest
     * expected utility among the current top-K itemsets.
     *
     * @return the minimum expected utility among top-K itemsets.
     */
    private float calculatePLIU_LB() {
        if (this.topKItemsets.isEmpty()) return 0;
        return (float) this.topKItemsets.stream()
                .mapToDouble(Itemset::getExpectedUtility)
                .min().orElse(0);
    }

    // --------------------------- TRANSACTION UTILITY ---------------------------

    /**
     * Calculates the positive transaction utility (PTU) for a given transaction by summing the positive
     * utilities of all items in that transaction.
     *
     * @param transaction the transaction to process.
     * @return the positive transaction utility.
     */
    private float calculatePTU(Transaction transaction) {
        return (float) transaction.getItems().stream()
                .mapToDouble(item -> {
                    int idx = transaction.getItems().indexOf(item);
                    return idx != -1 ? Math.max(transaction.getUtilities().get(idx), 0) : 0;
                }).sum();
    }

    /**
     * Calculates the absolute negative transaction utility (ANTU) for a given transaction by summing
     * the absolute values of the negative utilities.
     *
     * @param transaction the transaction to process.
     * @return the absolute negative transaction utility.
     */
    private float calculateANTU(Transaction transaction) {
        return (float) transaction.getItems().stream()
                .mapToDouble(item -> {
                    int idx = transaction.getItems().indexOf(item);
                    return idx != -1 ? -Math.min(transaction.getUtilities().get(idx), 0) : 0;
                }).sum();
    }

    // --------------------------- UTILITY & OCCURRENCE CALCULATIONS ---------------------------

    /**
     * Calculates the raw utility of an itemset in a given transaction by summing the utilities of its items.
     *
     * @param transaction the transaction to process.
     * @param itemset the itemset for which utility is calculated.
     * @return the raw utility of the itemset.
     */
    private int calculateItemsetUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }

    /**
     * Finds all occurrences of an itemset across the transactions.
     * For each transaction that contains the itemset, it computes:
     * - the raw utility of the itemset,
     * - a probability value based on positive and negative utility normalization,
     * - the expected utility as raw utility multiplied by the probability.
     *
     * @param itemset the itemset to search for.
     * @return a list of Occurrence objects representing the itemset's presence in transactions.
     */
    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return transactions.stream()
                .filter(transaction -> transaction.getItems().containsAll(itemset))
                .map(transaction -> {
                    int utility = this.calculateItemsetUtility(transaction, itemset);

                    // Compute positive and negative components for normalization.
                    float ptu = this.calculatePTU(transaction);
                    float ntu = this.calculateANTU(transaction);
                    float probability = 0f;

                    if (utility > 0 && ptu > 0) {
                        probability = utility / ptu;
                    } else if (utility < 0 && ntu > 0) {
                        probability = (-utility) / ntu;
                    }
                    float expectedUtility = utility * probability;
                    return new Occurrence(transaction.getId(), probability, utility, expectedUtility);
                }).collect(Collectors.toList());
    }

    /**
     * Calculates the maximum period for an itemset based on the gaps between consecutive transaction IDs
     * in which the itemset occurs.
     *
     * @param occurrences the list of Occurrence objects for the itemset.
     * @return the maximum period (largest gap) between consecutive occurrences.
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
     * Returns the total expected utility for an itemset by summing the expected utilities of all occurrences.
     *
     * @param occurrences the list of Occurrence objects for the itemset.
     * @return the total expected utility.
     */
    private float getTotalExpectedUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream().mapToDouble(Occurrence::getExpectedUtility).sum();
    }

    /**
     * Returns the total raw utility for an itemset by summing the utilities of all occurrences.
     *
     * @param occurrences the list of Occurrence objects for the itemset.
     * @return the total raw utility.
     */
    private int getTotalUtility(List<Occurrence> occurrences) {
        return occurrences.stream().mapToInt(Occurrence::getUtility).sum();
    }

    // --------------------------- PRUNING STRATEGY ---------------------------

    /**
     * Filters out items from each transaction whose transaction-weighted utility (TWU) is below the current minUtil threshold.
     * This pruning reduces the search space for candidate itemset generation.
     */
    private void filterLowUtilityItems() {
        this.transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> this.twu.getOrDefault(item, 0f) < this.minUtil);
            return transaction.getItems().isEmpty();
        });
    }

    // --------------------------- PSU (POSITIVE SUB-TREE UTILITY) ---------------------------

    /**
     * Calculates the Positive Sub-tree Utility (PSU) for a given prefix and candidate extension item.
     * PSU is defined as the sum of:
     * - the utility of the prefix,
     * - the positive part of the candidate extension item's utility,
     * - the remaining positive utility of items not in the prefix or the extension.
     *
     * @param prefix the current itemset prefix.
     * @param extensionItem the candidate item to extend the prefix.
     * @return the computed PSU value.
     */
    private float calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;
        if (this.processedPSU.contains(key)) return 0;
        this.processedPSU.add(key);
        float maxPSU = 0;
        for (Transaction transaction : this.transactions) {
            if (!transaction.getItems().containsAll(prefix) || !transaction.getItems().contains(extensionItem)) {
                continue;
            }
            int prefixUtility = this.calculateItemsetUtility(transaction, prefix);
            int extensionUtility = this.calculateItemsetUtility(transaction, List.of(extensionItem));
            int adjustedExtensionUtility = Math.max(extensionUtility, 0);
            int remainingPositiveUtility = this.calculateRPU(transaction, prefix, extensionItem);
            float computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            // Track max PSU instead of sum
            maxPSU = Math.max(maxPSU, computedPSU);
        }
        return maxPSU;
    }

    /**
     * Calculates the Remaining Positive Utility (RPU) for a transaction.
     * This is the sum of the positive utilities of items that are not in the prefix or the candidate extension.
     *
     * @param transaction the transaction.
     * @param prefix the current itemset prefix.
     * @param extensionItem the candidate extension item.
     * @return the sum of positive utilities of the remaining items.
     */
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

    // --------------------------- TWU COMPUTING ---------------------------

    /**
     * Computes the Transaction-Weighted Utility (TWU) for each item and updates the positive utility maps.
     */
    private void computeTWU() {
        for (Transaction transaction : transactions) {
            float twu = (float) transaction.getTransactionUtility();

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);

                if (utility >= 0) posUtil.merge(item, utility, Float::sum);
                else twu += utility; // Adjust transaction utility with negative values.
            }

            for (int item : transaction.getItems()) {
                this.twu.merge(item, twu, Float::sum);
            }
        }
    }

    // --------------------------- ITEMSET GENERATION AND TREE GROWTH ---------------------------

    /**
     * Generates candidate high-utility itemsets using a tree-based approach.
     * Unique items are collected and sorted by TWU in descending order. For each unique item,
     * a single-item itemset is initialized and extended recursively using the stpHupiTreeGrowth method.
     *
     * @return a list of the final top-K itemsets.
     */
    private List<Itemset> generateItemsets() {
        // Collect unique items from all transactions.
        Set<Integer> uniqueItems = this.transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .collect(Collectors.toSet());

        // Sort unique items in descending order by TWU.
        List<Integer> sortedUniqueItemsByTWU = uniqueItems.stream()
                .sorted((a, b) -> Float.compare(this.twu.getOrDefault(b, 0f), this.twu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        // Create the root of the STP-HUPI tree.
        StpHupiTree root = new StpHupiTree(new ArrayList<>(), 0, 0f, 0);

        // For each unique item, initialize a single-item itemset and grow the tree.
        for (Integer item : sortedUniqueItemsByTWU) {
            List<Integer> currentItemset = new ArrayList<>();
            currentItemset.add(item);
            List<Occurrence> occurrences = this.findOccurrences(currentItemset);
            if (occurrences.size() > 1) {
                int maxPeriod = this.calculateMaxPeriod(occurrences);
                if (maxPeriod > this.maxPer) continue;

                float expectedUtility = this.getTotalExpectedUtility(occurrences);
                if (expectedUtility < this.minUtil) continue;

                int utility = this.getTotalUtility(occurrences);
                if (utility < 0) continue;

                // Initialize a new tree node for the single-item itemset.
                StpHupiTree node = new StpHupiTree(currentItemset, utility, expectedUtility, maxPeriod);
                Map<Integer, StpHupiTree> children = node.getChildren();
                children.put(item, node);
                root.setChildren(children);

                // Extend the tree recursively.
                this.stpTreeGrowth(node);
            }
        }
        List<Itemset> results = new ArrayList<>(this.topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    /**
     * Recursively grows the STP-HUPI tree by extending the current node's itemset.
     * For each candidate extension, it recomputes occurrences and utility measures,
     * and prunes branches that do not meet the required thresholds.
     *
     * @param node the current tree node.
     */
    private void stpTreeGrowth(StpHupiTree node) {
        if (node.getExpectedUtility() < this.minUtil) return;

        // Process and potentially add the current itemset to the top-K list.
        this.processCurrentItemset(node);

        // Restrict candidate extensions to transactions that contain the current itemset.
        List<Integer> extensionItems = this.transactions.stream()
                .filter(t -> t.getItems().containsAll(node.getItemset()))
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.getItemset().contains(item))
                .distinct()
                .sorted((a, b) -> Float.compare(this.twu.getOrDefault(b, 0f), this.twu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = this.calculatePSU(node.getItemset(), item);
            if (psu >= this.minUtil) {
                // Create an extended itemset.
                List<Integer> newItemset = new ArrayList<>(node.getItemset());
                newItemset.add(item);

                // Recompute occurrences for the extended itemset.
                List<Occurrence> newOccurrences = this.findOccurrences(newItemset);
                if (newOccurrences.size() < 2) continue;

                int newMaxPeriod = this.calculateMaxPeriod(newOccurrences);
                if (newMaxPeriod > this.maxPer || newMaxPeriod == 0) continue;
                int newUtility = this.getTotalUtility(newOccurrences);
                if (newUtility < 0) continue;
                float newTotalExpUtil = this.getTotalExpectedUtility(newOccurrences);
                if (newTotalExpUtil < this.minUtil) continue;

                // Create a new tree node for the extended itemset.
                StpHupiTree childNode = new StpHupiTree(new ArrayList<>(newItemset),
                        newUtility, newTotalExpUtil, newMaxPeriod);

                Map<Integer, StpHupiTree> children = childNode.getChildren();
                children.put(item, childNode);
                node.setChildren(children);

                // Recursively extend the new node.
                this.stpTreeGrowth(childNode);
            }
        }
    }

    /**
     * Processes the current itemset contained in the given tree node.
     * If the itemset satisfies the utility and period constraints, it is added to the top-K candidate list.
     * After processing, the dynamic minimum utility threshold is updated.
     *
     * @param node the tree node containing the candidate itemset.
     */
    private void processCurrentItemset(StpHupiTree node) {
        List<Integer> currentItemset = node.getItemset();
        String canonicalKey = this.getItemsetKey(currentItemset);
        if (this.topKSeen.contains(canonicalKey)) return;
        Itemset itemset = new Itemset(currentItemset, node.getUtility(), node.getExpectedUtility(), node.getMaxPeriod());

        if (this.topKItemsets.size() < this.k) {
            this.topKItemsets.offer(itemset);
            this.topKSeen.add(canonicalKey);
        } else {
            Itemset lowestUtilityItemset = this.topKItemsets.peek();
            if (lowestUtilityItemset != null && itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility()) {
                this.topKItemsets.poll();
                this.topKSeen.remove(getItemsetKey(lowestUtilityItemset.getItems()));
                this.topKItemsets.offer(itemset);
                this.topKSeen.add(canonicalKey);
            }
        }
        this.updateMinUtil();
    }

    /**
     * Dynamically updates the minimum expected utility threshold (minUtil) based on the current top-K itemsets.
     * The new threshold is computed using a weighted combination of the lowest expected utility among the current
     * top-K candidates and upper-bound measures (PRIU, PLIU_E, and PLIU_LB). If the computed dynamic threshold exceeds
     * the current minUtil by at least 10%, minUtil is updated.
     */
    private void updateMinUtil() {
        if (this.topKItemsets.size() >= this.k) {
            List<Itemset> sortedItemsets = new ArrayList<>(this.topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
            float newMinUtil = sortedItemsets.get(sortedItemsets.size() - 1).getExpectedUtility();

            // Use the threshold raising strategies to update minUtil
            float priu = calculatePRIU() * 0.3f;
            float pliuE = calculatePLIU_E() * 0.3f;
            float pliuLB = calculatePLIU_LB();
            float dynamicThreshold = (newMinUtil * 0.4f) + (Math.min(priu, Math.min(pliuE, pliuLB)) * 0.6f);

            if (dynamicThreshold > this.minUtil * 1.1f) {
                this.minUtil = dynamicThreshold;
            }
        }
    }

    // --------------------------- HELPER FUNCTIONS ---------------------------

    /**
     * Returns the canonical (sorted) order of the given list of items.
     *
     * @param items the list of items.
     * @return a new list containing the items in sorted order.
     */
    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }

    /**
     * Generates a canonical key (String) for an itemset by converting its sorted order to a string.
     *
     * @param items the list of items in the itemset.
     * @return the canonical key representing the itemset.
     */
    private String getItemsetKey(List<Integer> items) {
        return this.getCanonicalOrder(items).toString();
    }

    // --------------------------- RUN & EVALUATION ---------------------------

    /**
     * Evaluates the top-K performance of the algorithm by executing the candidate generation process,
     * measuring execution time and memory usage, and printing the final top-K itemsets.
     */
    public void evaluateTopKPerformance() {
        // Measure runtime and memory for the candidate generation process.
        long startTime = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        this.computeTWU();
        this.initialMinUtil();
        this.filterLowUtilityItems();

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