package algorithm.ST_HUPI;

import algorithm.Itemset;
import algorithm.Occurrence;
import algorithm.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

import org.knowm.xchart.*;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class STHUPIAlgorithm {
    private List<Transaction> transactions;
    private int[] kValues;
    private int k;
    private float minExpectedUtility;
    private PriorityQueue<Itemset> topKItemsets;
    private Map<Integer, Float> tweu;
    private Map<Integer, Float> posUtility;
    private Map<Integer, Float> negUtility;
    private Map<String, Float> psuCache = new HashMap<>();
    private final List<Transaction> originalTransactions;
    private Map<Integer, Double> runTimeResults = new LinkedHashMap<>();
    private Map<Integer, Double> memoryResults = new LinkedHashMap<>();

    public STHUPIAlgorithm(List<Transaction> transactions, int[] kValues) {
        this.originalTransactions = new ArrayList<>(transactions);
        this.transactions = new ArrayList<>(transactions);
        this.kValues = kValues;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.tweu = new HashMap<>();
        this.posUtility = new HashMap<>();
        this.negUtility = new HashMap<>();
    }

    // ------------------------------------------- PRIU STRATEGIES -----------------------------------//
    private float calculatePRIU() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> posUtility.getOrDefault(item, 0f))
                        .sum())
                .max().orElse(0);
    }

    private float calculatePLIU_E() {
        return (float) transactions.stream()
                .mapToDouble(transaction -> {List<Integer> sortedItems = transaction.getItems()
                        .stream().sorted(Comparator.comparingDouble(item -> posUtility.getOrDefault(item, 0f)))
                        .collect(Collectors.toList());
                    return sortedItems.stream().limit(2)
                            .mapToDouble(item -> posUtility.getOrDefault(item, 0f))
                            .sum();
                }).max().orElse(0);
    }

    private float calculatePLIU_LB() {
        if (topKItemsets.isEmpty()) return 0;

        return (float) topKItemsets.stream()
                .mapToDouble(Itemset::getExpectedUtility)  // Use probability-based expected utility
                .min().orElse(0); // Get the smallest expected utility in top-K
    }

    // ------------------------------------------- PRIU STRATEGIES -----------------------------------//

    private int calculateDatabaseUtility() {
        return transactions.stream()
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
        return transactions.stream().filter(transaction -> transaction.getItems().containsAll(itemset)).map(transaction -> {
            int utility = this.calculateUtility(transaction, itemset);
            float posUtil = Math.max(utility, 0);
            float negUtil = Math.min(utility, 0);
            float probability = (posUtil + Math.abs(negUtil)) > 0 ? (posUtil + negUtil) / transaction.getTransactionUtility() : 0;
            float expectedUtility = (posUtil + negUtil) * probability;

            return new Occurrence(transaction.getId(), probability, utility, expectedUtility);
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

    // ---------------------------- Pruned Strategies ---------------------------//
    private void filterLowUtilityItems() {
        transactions.removeIf(transaction -> {
            transaction.getItems().removeIf(item -> tweu.getOrDefault(item, 0f) < minExpectedUtility);
            return transaction.getItems().isEmpty();
        });
    }

    //     ---------------------------- New Method for PSU --------------------------- //
    public float calculatePSU(List<Integer> prefix, int extensionItem) {
        String key = prefix + "-" + extensionItem;

        // Track max PSU instead of sum
        float maxPSU = 0;

        for (Transaction transaction : transactions) {
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

        psuCache.put(key, maxPSU);
        return maxPSU;
    }
    // ---------------------------- Pruned Strategies ---------------------------//

    private void computeTWEU() {
        for (Transaction transaction : transactions) {

            // Calculate actual transaction utility based on the present items only
            float transactionUtility = 0;

            for (int i = 0; i < transaction.getItems().size(); i++) {
                int item = transaction.getItems().get(i);
                float utility = transaction.getUtilities().get(i);

                if (utility >= 0) posUtility.merge(item, utility, Float::sum);
                else negUtility.merge(item, utility, Float::sum);

                transactionUtility += utility;
            }
            for (int item : transaction.getItems()) {
                tweu.merge(item, transactionUtility, Float::sum);
            }
        }
    }

    public List<Itemset> generateItemsets() {
        List<Integer> sortedItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .distinct().sorted((a, b) ->
                        Float.compare(tweu.getOrDefault(b, 0f), tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        SHTreeNode root = new SHTreeNode(new ArrayList<>(), 0, 0, 0);

        for (Integer item : sortedItems) {
            if (tweu.getOrDefault(item, 0f) >= minExpectedUtility) {
                List<Integer> currentItemset = new ArrayList<>();
                currentItemset.add(item);
                SHTreeNode node = new SHTreeNode(currentItemset, 0, 0, 0);
                root.children.put(item, node);

                List<Occurrence> occurrences = this.findOccurrences(currentItemset);
                this.shTreeGrowth(node, occurrences);
            }
        }

        List<Itemset> results = new ArrayList<>(topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }


    private void updateMinExpectedUtility() {
        if (topKItemsets.size() >= k) {
            List<Itemset> sortedItemsets = new ArrayList<>(topKItemsets);
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

            if (dynamicThreshold > minExpectedUtility * 1.1f) {  // 10% increase rule
                minExpectedUtility = dynamicThreshold;
//                System.out.println("Updated minExpectedUtility: " + minExpectedUtility);
            }
        }
    }


    private Set<String> topKSeen = new HashSet<>();

    private List<Integer> getCanonicalOrder(List<Integer> items) {
        return items.stream().sorted().collect(Collectors.toList());
    }

    private String getItemsetKey(List<Integer> items) {
        return getCanonicalOrder(items).toString();
    }

    private void shTreeGrowth(SHTreeNode node, List<Occurrence> occurrences) {
        float posUtility = getTotalPositiveUtility(occurrences);
        float negUtility = getTotalNegativeUtility(occurrences);
        float totalExpectedUtility = getTotalExpectedUtility(occurrences);

        if (totalExpectedUtility < minExpectedUtility) return;

        processCurrentItemset(node.itemset, occurrences);

        List<Integer> extensionItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !node.itemset.contains(item)).distinct().sorted((a, b) ->
                        Float.compare(tweu.getOrDefault(b, 0f), tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = calculatePSU(node.itemset, item);
            if (psu >= minExpectedUtility) {
                List<Integer> newItemset = new ArrayList<>(node.itemset);
                newItemset.add(item);

                SHTreeNode childNode = new SHTreeNode(newItemset, totalExpectedUtility, posUtility, negUtility);
                node.children.put(item, childNode);

                List<Occurrence> newOccurrences = this.findOccurrences(newItemset);
                this.shTreeGrowth(childNode, newOccurrences);
            }
        }
    }

    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences) {
        String canonicalKey = this.getItemsetKey(currentItemset);
        if (topKSeen.contains(canonicalKey)) {
            return;
        }

        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
        int totalUtility = this.getTotalUtility(occurrences);

        if (totalUtility < 0) {
            return;
        }

        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, 0);

        if (topKItemsets.size() < this.k) {
            topKItemsets.offer(itemset);
            topKSeen.add(canonicalKey);
        } else {
            Itemset lowestUtilityItemset = topKItemsets.peek();
            if (lowestUtilityItemset != null) {
                if (itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility()) {
                    Itemset removed = topKItemsets.poll();
                    topKSeen.remove(getItemsetKey(removed.getItems()));
                    topKItemsets.offer(itemset);
                    topKSeen.add(canonicalKey);
                }
            }
        }
        updateMinExpectedUtility();
    }


    // ------------------------------ RUN METHOD ---------------------------------------//

    public void evaluateTopKPerformance() {
        float databaseUtility = calculateDatabaseUtility();
        float globalMinUtil = databaseUtility * 0.01f;  // Shared minUtil across k-values
        float minUtilLowerBound = globalMinUtil * 0.00001f;

//        System.out.println("Database Utility: " + databaseUtility);
//        System.out.println("Minimum Utility: " + globalMinUtil);
        for (int kValue : kValues) {
            k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + k + " Itemsets...");

            // ✅ RUNTIME MEASUREMENT BEFORE EXECUTION
            long startTime = System.nanoTime();

            // ✅ MEMORY MEASUREMENT BEFORE EXECUTION
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();  // Suggest garbage collection before measuring
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            while (!foundEnoughCandidates && globalMinUtil >= minUtilLowerBound) {
                reset();  // Reset algorithm state
                minExpectedUtility = globalMinUtil;  // Use shared minUtil
//                System.out.println("Initial minExpectedUtility: " + minExpectedUtility);
                filterLowUtilityItems();
                finalResults = generateItemsets();  // Save results

                // Check if we found enough candidates
                if (finalResults.size() >= kValue) {
                    foundEnoughCandidates = true;
                } else {
//                    System.out.println("Not enough candidates found (" + finalResults.size() + "). Lowering minUtil...");
                    globalMinUtil *= 0.8f;  // Reduce minUtil globally
                }
            }

            // ✅ TIME MEASUREMENT AFTER EXECUTION
            double executionTime = (System.nanoTime() - startTime) / 1_000_000.0;;
            System.out.printf("Top-%d Execution Time: %.2f ms%n", kValue, executionTime);
            runTimeResults.put(kValue, executionTime);

            // ✅ MEMORY MEASUREMENT AFTER EXECUTION
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsedMB = (memoryAfter - memoryBefore) / (1024.0 * 1024.0); // Convert to MB
            System.out.printf("Top-%d Memory Usage: %.2f MB%n", kValue, memoryUsedMB);
            memoryResults.put(kValue, memoryUsedMB);

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

    public Map<Integer, Double> evaluateMemoryUsage() {
        int[] kValues = {1, 10, 20, 30, 40, 50};
        Map<Integer, Double> memoryResults = new LinkedHashMap<>();

        float databaseUtility = calculateDatabaseUtility();
        float globalMinUtil = databaseUtility * 0.01f;
        float minUtilLowerBound = globalMinUtil * 0.00001f;

        System.out.println("Database Utility: " + databaseUtility);
        System.out.println("Minimum Utility: " + globalMinUtil);

        for (int kValue : kValues) {
            k = kValue;
            boolean foundEnoughCandidates = false;
            List<Itemset> finalResults = new ArrayList<>();
            System.out.println("\nRunning for Top-" + k + " Itemsets...");

            // ✅ MEMORY MEASUREMENT BEFORE EXECUTION
            Runtime runtime = Runtime.getRuntime();
            runtime.gc();  // Suggest garbage collection before measuring
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

            while (!foundEnoughCandidates && globalMinUtil >= minUtilLowerBound) {
                reset();
                minExpectedUtility = globalMinUtil;
                filterLowUtilityItems();
                finalResults = generateItemsets();

                if (finalResults.size() >= kValue) {
                    foundEnoughCandidates = true;
                } else {
                    System.out.println("Not enough candidates found (" + finalResults.size() + "). Lowering minUtil...");
                    globalMinUtil *= 0.8f;
                }
            }

            // ✅ MEMORY MEASUREMENT AFTER EXECUTION
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsedMB = (memoryAfter - memoryBefore) / (1024.0 * 1024.0); // Convert to MB

            System.out.printf("Top-%d Memory Usage: %.2f MB%n", kValue, memoryUsedMB);
            memoryResults.put(kValue, memoryUsedMB);

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
        return memoryResults;
    }


    private void reset() {
        // Clear existing collections
        topKItemsets.clear();
        topKSeen.clear();
        tweu.clear();
        posUtility.clear();
        negUtility.clear();
        psuCache.clear();
        transactions = new ArrayList<>(originalTransactions); // Need to add originalTransactions field
        computeTWEU();
    }
}


