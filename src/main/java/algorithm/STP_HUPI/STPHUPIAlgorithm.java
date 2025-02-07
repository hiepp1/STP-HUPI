package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

import org.knowm.xchart.*;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class STPHUPIAlgorithm {
    private List<Transaction> transactions;
    private int maxPer;
    private int k;
    private float minExpectedUtility;
    private PriorityQueue<Itemset> topKItemsets;
    private Map<Integer, Float> tweu;
    private Map<Integer, Float> posUtility;
    private Map<Integer, Float> negUtility;
    private Map<String, Float> psuCache = new HashMap<>();
    private final List<Transaction> originalTransactions;

    public STPHUPIAlgorithm(List<Transaction> transactions, int maxPer) {
        this.originalTransactions = new ArrayList<>(transactions);
        this.transactions = new ArrayList<>(transactions);
        this.maxPer = maxPer;
//        this.minExpectedUtility = calculateDatabaseUtility() * 0.001f;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.tweu = new HashMap<>();
        this.posUtility = new HashMap<>();
        this.negUtility = new HashMap<>();
    }

    // ------------------------------------------- PRIU STRATEGIES -----------------------------------//
    private float calculatePRIU() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> this.posUtility.getOrDefault(item, 0f))
                        .sum())
                .max().orElse(0);
    }

    private float calculatePLIU_E() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> {List<Integer> sortedItems = transaction.getItems()
                        .stream().sorted(Comparator.comparingDouble(item -> this.posUtility.getOrDefault(item, 0f)))
                        .collect(Collectors.toList());
            return sortedItems.stream().limit(2)
                    .mapToDouble(item -> this.posUtility.getOrDefault(item, 0f))
                    .sum();
        }).max().orElse(0);
    }

    private float calculatePLIU_LB() {
        if (this.topKItemsets.isEmpty()) return 0;

        return (float) this.topKItemsets.stream()
                .mapToDouble(Itemset::getExpectedUtility)  // Use probability-based expected utility
                .min().orElse(0); // Get the smallest expected utility in top-K
    }

    // ------------------------------------------- PRIU STRATEGIES -----------------------------------//

    private int calculateDatabaseUtility() {
        return this.transactions.stream()
                .mapToInt(Transaction::getTransactionUtility).sum();
    }

    // Calculate the utility of an itemset in a transaction
    private int calculateUtility(Transaction transaction, List<Integer> itemset) {
        return itemset.stream().mapToInt(item -> {
            int index = transaction.getItems().indexOf(item);
            return index != -1 ? transaction.getUtilities().get(index) : 0;
        }).sum();
    }

    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return this.transactions.stream().filter(transaction -> transaction.getItems().containsAll(itemset)).map(transaction -> {
            int utility = this.calculateUtility(transaction, itemset);
            float posUtil = Math.max(utility, 0);
            float negUtil = Math.min(utility, 0);
            float probability = (posUtil + Math.abs(negUtil)) > 0 ? (posUtil + negUtil) / transaction.getTransactionUtility() : 0;
            float expectedUtility = (posUtil + negUtil) * probability;

            return new Occurrence(transaction.getId(), probability, utility, expectedUtility);
        }).collect(Collectors.toList());
    }

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

    // ---------------------------- Pruned Strategies ---------------------------//
    private void filterLowUtilityItems() {
        this.transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> this.tweu.getOrDefault(item, 0f) < this.minExpectedUtility);
            return transaction.getItems().isEmpty();
        });
    }

    //     ---------------------------- New Method for PSU --------------------------- //
    public float calculatePSU(List<Integer> prefix, int extensionItem) {
        Set<String> processedPSU = new HashSet<>();
        String key = prefix + "-" + extensionItem;

        if (processedPSU.contains(key)) {
            return 0; // Avoid duplicate calculations
        }
        processedPSU.add(key);

        // Track max PSU instead of sum
        float maxPSU = 0;

        for (Transaction transaction : this.transactions) {
            if (!transaction.getItems().containsAll(prefix) || !transaction.getItems().contains(extensionItem)) {
                continue;
            }

            int prefixUtility = this.calculateUtility(transaction, prefix);
            int extensionUtility = this.calculateUtility(transaction, List.of(extensionItem));
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

        this.psuCache.put(key, maxPSU);
        return maxPSU;
    }
    // ---------------------------- Pruned Strategies ---------------------------//

    private void computeTWEU() {
        // Reset maps before computation
        this.tweu.clear();
        this.posUtility.clear();
        this.negUtility.clear();

        for (Transaction transaction : this.transactions) {
            // Calculate actual transaction utility based on the present items only
            float transactionUtility = 0;

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);

                // Add to positive or negative utility maps
                if (utility >= 0) {
                    this.posUtility.merge(item, utility, Float::sum);
                } else {
                    this.negUtility.merge(item, utility, Float::sum);
                }

                // Add to transaction utility only for this item
                transactionUtility += utility;
            }
            for (int item : transaction.getItems()) {
                this.tweu.merge(item, transactionUtility, Float::sum);
            }
        }
    }

    public List<Itemset> generateItemsets() {
        List<Integer> sortedItems = this.transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .distinct().sorted((a, b) ->
                        Float.compare(this.tweu.getOrDefault(b, 0f), this.tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        SPHTreeNode root = new SPHTreeNode(new ArrayList<>(), 0, 0, 0, 0);

        for (Integer item : sortedItems) {
            if (this.tweu.getOrDefault(item, 0f) >= this.minExpectedUtility) {
                List<Integer> currentItemset = new ArrayList<>();
                currentItemset.add(item);
                SPHTreeNode node = new SPHTreeNode(currentItemset, 0, 0, 0, 0);
                root.children.put(item, node);

                List<Occurrence> occurrences = findOccurrences(currentItemset);
                sphTreeGrowth(node, occurrences);
            }
        }

        List<Itemset> results = new ArrayList<>(this.topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }


    private void updateMinExpectedUtility() {
        if (this.topKItemsets.size() >= k) {
            List<Itemset> sortedItemsets = new ArrayList<>(this.topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());

            float newMinUtil = sortedItemsets.get(sortedItemsets.size() - 1).getExpectedUtility();
            float priu = this.calculatePRIU() * 0.3f;  // Reduce PRIU influence
            float pliue = this.calculatePLIU_E() * 0.3f;  // Reduce PLIU_E influence
            float pliulb = this.calculatePLIU_LB();  // Keep PLIU_LB as it is

            // More flexible threshold calculation
//            float dynamicThreshold = Math.max(
//                    newMinUtil * 0.4f,  // Increase the minimum utility factor
//                    Math.min(priu, Math.min(pliue, pliulb))  // Take minimum of all three
//            );

            float dynamicThreshold = (newMinUtil * 0.4f) + (Math.min(priu, Math.min(pliue, pliulb)) * 0.6f);

            if (dynamicThreshold > this.minExpectedUtility * 1.1f) {  // 10% increase rule
                this.minExpectedUtility = dynamicThreshold;
                System.out.println("Updated minExpectedUtility: " + this.minExpectedUtility);
            }
        }
    }


    private Set<String> topKSeen = new HashSet<>();

//    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets) {
//        Set<Integer> itemsetKey = new TreeSet<>(currentItemset);
//        if (!seenItemsets.add(itemsetKey)) return;
//
//        List<Occurrence> occurrences = findOccurrences(currentItemset);
//        int maxPeriod = calculateMaxPeriod(occurrences);
//        if (maxPeriod > this.maxPer) return;
//
//        float totalExpectedUtility = getTotalExpectedUtility(occurrences);
//        if (totalExpectedUtility < this.minExpectedUtility) return;
//        int totalUtility = getTotalUtility(occurrences);
//
//        // Always process itemsets if they are valid
//        processCurrentItemset(currentItemset, occurrences);
//
//        List<Integer> extensionItems = transactions.stream().flatMap(t -> t.getItems().stream()).filter(item -> !currentItemset.contains(item)).distinct().sorted((a, b) -> Float.compare(tweu.getOrDefault(b, 0f), tweu.getOrDefault(a, 0f))).collect(Collectors.toList());
//
//        for (Integer item : extensionItems) {
//            float psu = calculatePSU(currentItemset, item);
//            if (psu >= this.minExpectedUtility) {
//                List<Integer> newItemset = new ArrayList<>(currentItemset);
//                newItemset.add(item);
//                dfs(newItemset, seenItemsets);
//            }
//        }
//        updateMinExpectedUtility();
//    }

    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }

    private String getItemsetKey(List<Integer> items) {
        return getCanonicalOrder(items).toString();
    }

    private void sphTreeGrowth(SPHTreeNode node, List<Occurrence> occurrences) {
        int maxPeriod = this.calculateMaxPeriod(occurrences);
        if (maxPeriod > this.maxPer || maxPeriod == 0) return;

        float posUtility = this.getTotalPositiveUtility(occurrences);
        float negUtility = this.getTotalNegativeUtility(occurrences);
        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);

        if (totalExpectedUtility < this.minExpectedUtility) return;

        this.processCurrentItemset(node.itemset, occurrences);

        List<Integer> extensionItems = this.transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.itemset.contains(item)).distinct().sorted((a, b) ->
                        Float.compare(this.tweu.getOrDefault(b, 0f), this.tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = this.calculatePSU(node.itemset, item);
            if (psu >= this.minExpectedUtility) {
                List<Integer> newItemset = new ArrayList<>(node.itemset);
                newItemset.add(item);

                SPHTreeNode childNode = new SPHTreeNode(newItemset, totalExpectedUtility, posUtility, negUtility, maxPeriod);
                node.children.put(item, childNode);

                List<Occurrence> newOccurrences = findOccurrences(newItemset);
                sphTreeGrowth(childNode, newOccurrences);
            }
        }
    }

    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        String canonicalKey = this.getItemsetKey(currentItemset);
        if (this.topKSeen.contains(canonicalKey)) {
            return;
        }

        int maxPeriod = this.calculateMaxPeriod(occurrences);
        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
        int totalUtility = this.getTotalUtility(occurrences);

        if (totalUtility < 0) {
            return;
        }

        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);

        if (this.topKItemsets.size() < k) {
            this.topKItemsets.offer(itemset);
            this.topKSeen.add(canonicalKey);
        } else {
            Itemset lowestUtilityItemset = this.topKItemsets.peek();
            if (lowestUtilityItemset != null) {
                if (itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility()) {
                    Itemset removed = this.topKItemsets.poll();
                    this.topKSeen.remove(getItemsetKey(removed.getItems()));
                    this.topKItemsets.offer(itemset);
                    this.topKSeen.add(canonicalKey);
                }
            }
        }
        this.updateMinExpectedUtility();
    }


    // ------------------------------ RUN METHOD ---------------------------------------//

    public void reset() {
        // Clear existing collections
        this.topKItemsets.clear();
        this.topKSeen.clear();
        this.tweu.clear();
        this.posUtility.clear();
        this.negUtility.clear();
        this.psuCache.clear();

        // Reinitialize transactions from original data
        this.transactions = new ArrayList<>(originalTransactions); // Need to add originalTransactions field

        // Recalculate initial threshold
        this.minExpectedUtility = calculateDatabaseUtility() * 0.001f;

        // Recompute utility maps
        this.computeTWEU();
    }


    public void evaluateTopKPerformance(String datasetTitle) {
        int[] kValues = {1, 5, 10, 20};  // Different k values to test
        Map<Integer, Double> runtimeResults = new LinkedHashMap<>();

        float globalMinUtil = this.calculateDatabaseUtility() * 0.01f;  // Shared minUtil across k-values
        float minUtilLowerBound = this.calculateDatabaseUtility() * 0.00001f;

        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
        for (int kValue : kValues) {
            this.k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + this.k + " Itemsets...");
            long startTime = System.nanoTime();

            while (!foundEnoughCandidates && globalMinUtil >= minUtilLowerBound) {
                reset();  // Reset algorithm state
                this.minExpectedUtility = globalMinUtil;  // Use shared minUtil
                System.out.println("Initial minExpectedUtility: " + this.minExpectedUtility);
                this.filterLowUtilityItems();
                finalResults = this.generateItemsets();  // Save results

                // Check if we found enough candidates
                if (finalResults.size() >= kValue) {
                    foundEnoughCandidates = true;
                } else {
                    System.out.println("Not enough candidates found (" + finalResults.size() + "). Lowering minUtil...");
                    globalMinUtil *= 0.8f;  // Reduce minUtil globally
                }
            }

            double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("Top-%d Execution Time: %.2f seconds%n", kValue, executionTime);
            runtimeResults.put(kValue, executionTime);

            // ✅ PRINT FINAL TOP-K ITEMSETS
            System.out.println("\n🔹 Final Top-" + kValue + " Itemsets:");
            if (finalResults.isEmpty()) {
                System.out.println("⚠️ No itemsets satisfy your condition.");
            } else {
                for (Itemset itemset : finalResults) {
                    System.out.println(itemset);
                }
            }
        }
        this.plotRuntimeResults(runtimeResults, datasetTitle);
    }


    public void plotRuntimeResults(Map<Integer, Double> runtimeResults, String datasetTitle) { // Add parameter
        List<Integer> kValues = new ArrayList<>(runtimeResults.keySet());
        List<Double> runtimes = new ArrayList<>(runtimeResults.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(datasetTitle)
                .xAxisTitle("K-value").yAxisTitle("Runtime (s)")
                .build();

        chart.addSeries("Runtime", kValues, runtimes);
        new SwingWrapper<>(chart).displayChart();
    }
}


