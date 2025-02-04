package STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;
import java.util.stream.Collectors;
import org.knowm.xchart.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Algorithm {
    private List<Transaction> transactions;
    private int maxPer;
    private int k;
    private float minExpectedUtility;
    private PriorityQueue<Itemset> topKItemsets;
    private Map<Integer, Float> tweu;
    private Map<Integer, Float> posUtility;
    private Map<Integer, Float> negUtility;
    private Map<String, Float> psuCache = new HashMap<>();

    public Algorithm(List<Transaction> transactions, int maxPer) {
        this.transactions = new ArrayList<>(transactions);
        this.maxPer = maxPer;
        this.minExpectedUtility = calculateDatabaseUtility() * 0.001f;
        this.topKItemsets = new PriorityQueue<>(Comparator.comparing(Itemset::getExpectedUtility));
        this.tweu = new HashMap<>();
        this.posUtility = new HashMap<>();
        this.negUtility = new HashMap<>();
    }

    // ------------------------------------------- PRIU STRATEGIES -----------------------------------//
    private float calculatePRIU() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> transaction.getItems().stream()
                        .mapToDouble(item -> this.posUtility.getOrDefault(item, 0f))  // Only positive utilities
                        .sum()
                ).max().orElse(0);
    }

    private float calculatePLIU_E() {
        return (float) this.transactions.stream()
                .mapToDouble(transaction -> {
                    List<Integer> sortedItems = transaction.getItems().stream()
                            .sorted(Comparator.comparingDouble(item -> this.posUtility.getOrDefault(item, 0f))) // Sort by posUtility
                            .collect(Collectors.toList());

                    return sortedItems.stream().limit(2) // Only take the first 2 most probable items
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


    // Calculate total utility of the dataset
    private int calculateDatabaseUtility() {
        return this.transactions.stream()
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
        if (occurrences.size() < 2) return 0;

        List<Integer> indices = occurrences.stream()
                .map(Occurrence::getTransactionID)
                .sorted()
                .collect(Collectors.toList());

        int maxPeriod = indices.get(0); // the first transaction that contains itemset: TransactionID - 0
        for (int i = 0; i < indices.size() - 1; i++) {
            maxPeriod = Math.max(maxPeriod, indices.get(i + 1) - indices.get(i));
        }
        return maxPeriod;
    }

    private float getTotalExpectedUtility(List<Occurrence> occurrences) {
        return (float) occurrences.stream()
                .mapToDouble(Occurrence::getExpectedUtility)
                .sum();
    }

    private int getTotalUtility(List<Occurrence> occurrences) {
        int totalUtility = 0;
        for (Occurrence occurrence : occurrences) {
            totalUtility += occurrence.getUtility();
        }
        return totalUtility;
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

            int remainingPositiveUtility = transaction.getItems().stream()
                    .filter(item -> !prefix.contains(item) && item != extensionItem) // Exclude prefix & extension item
                    .mapToInt(item -> {
                        int index = transaction.getItems().indexOf(item);
                        if (index == -1) return 0;
                        int utility = transaction.getUtilities().get(index);
                        return Math.max(utility, 0); // Ignore negative utilities
                    })
                    .sum();

            float computedPSU = prefixUtility + adjustedExtensionUtility + remainingPositiveUtility;
            // Track max PSU instead of sum
            maxPSU = Math.max(maxPSU, computedPSU);
        }

        this.psuCache.put(key, maxPSU);
        return maxPSU;
    }
//    public float calculatePSU(List<Integer> prefix, int extensionItem) {
//        Set<String> processedPSU = new HashSet<>();
//        String key = prefix + "-" + extensionItem;
//
//        // Avoid duplicate computations
//        if (processedPSU.contains(key)) {
//            return 0; // Return 0 to prevent duplicate calculations
//        }
//        processedPSU.add(key);
//
//        // Compute PSU by summing only positive remaining utilities
//        float psu = (float) transactions.stream()
//                .filter(transaction -> transaction.getItems().containsAll(prefix) && transaction.getItems().contains(extensionItem))
//                .mapToDouble(transaction -> {
//                    int prefixUtility = calculateUtility(transaction, prefix);
//                    int extensionUtility = calculateUtility(transaction, List.of(extensionItem));
//
//                    // Only sum positive remaining utilities
//                    int remainingPositiveUtility = transaction.getItems().stream()
//                            .filter(item -> !prefix.contains(item) && item != extensionItem) // Exclude prefix and extension item
//                            .mapToInt(item -> {
//                                int index = transaction.getItems().indexOf(item);
//                                if (index == -1) return 0; // Skip if item is not found
//                                int utility = transaction.getUtilities().get(index);
//                                return Math.max(utility, 0); // Ensure negative values are ignored
//                            })
//                            .sum();
//
//                    return prefixUtility + extensionUtility + remainingPositiveUtility;
//                })
//                .sum();
//
//        psuCache.put(key, psu);
//        return psu;
//    }


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

    private List<Occurrence> findOccurrences(List<Integer> itemset) {
        return this.transactions.stream()
                .filter(transaction -> new HashSet<>(transaction.getItems()).containsAll(itemset))
                .map(transaction -> {
                    int utility = this.calculateUtility(transaction, itemset);
                    float positiveUtility = Math.max(utility, 0);
                    float probability = positiveUtility > 0 ?
                            positiveUtility / transaction.getTransactionUtility() : 0;
                    float expectedUtility = utility * probability;

                    return new Occurrence(transaction.getId(), probability, utility, expectedUtility);
                })
                .collect(Collectors.toList());
    }

    public List<Itemset> generateItemsets() {
        Set<Set<Integer>> seenItemsets = new HashSet<>();

        List<Integer> sortedItems = this.transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .distinct()
                .sorted((a, b) -> Float.compare(this.tweu.getOrDefault(b, 0f),
                        this.tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        // Process each item as starting point
        for (Integer item : sortedItems) {
            if (this.tweu.getOrDefault(item, 0f) >= this.minExpectedUtility) {
                List<Integer> currentItemset = new ArrayList<>();
                currentItemset.add(item);
                this.dfs(currentItemset, seenItemsets);
            }
        }

        // Return sorted results
        List<Itemset> results = new ArrayList<>(this.topKItemsets);
        results.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());
        return results;
    }

    private void updateMinExpectedUtility() {
        if (this.topKItemsets.size() >= this.k) {
            List<Itemset> sortedItemsets = new ArrayList<>(this.topKItemsets);
            sortedItemsets.sort(Comparator.comparing(Itemset::getExpectedUtility).reversed());

            List<Itemset> validItemsets = sortedItemsets.stream()
                    .filter(itemset -> itemset.getUtility() >= 0)  // Only consider positive utility
                    .collect(Collectors.toList());

            if (validItemsets.size() < k) {
                return;  // Skip updating if not enough valid itemsets
            }

            float newMinUtil = validItemsets.get(validItemsets.size() - 1).getExpectedUtility();
            float priu = this.calculatePRIU();
            float pliue = this.calculatePLIU_E();
            float pliulb = this.calculatePLIU_LB();

            float dynamicThreshold = Math.max(newMinUtil * 0.85f, Math.max(priu, Math.max(pliue, pliulb)));

            if (dynamicThreshold > this.minExpectedUtility) {
                this.minExpectedUtility = dynamicThreshold;
                System.out.println("Updated minExpectedUtility: " + this.minExpectedUtility);
            }
        }
    }


    private Set<List<Integer>> topKSeen = new HashSet<>();

    private void dfs(List<Integer> currentItemset, Set<Set<Integer>> seenItemsets) {
        Set<Integer> itemsetKey = new TreeSet<>(currentItemset);
        if (!seenItemsets.add(itemsetKey)) return;

        List<Occurrence> occurrences = this.findOccurrences(currentItemset);
        int maxPeriod = this.calculateMaxPeriod(occurrences);
        if (maxPeriod > this.maxPer || maxPeriod == 0) return;

        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
        int totalUtility = this.getTotalUtility(occurrences);

        // Always process itemsets if they are valid
        this.processCurrentItemset(currentItemset, occurrences, false);

        List<Integer> extensionItems = transactions.stream()
                .flatMap(t -> t.getItems().stream())
                .filter(item -> !currentItemset.contains(item))
                .distinct()
                .sorted((a, b) -> Float.compare(tweu.getOrDefault(b, 0f), tweu.getOrDefault(a, 0f)))
                .collect(Collectors.toList());

        for (Integer item : extensionItems) {
            float psu = calculatePSU(currentItemset, item);
            if (psu >= this.minExpectedUtility) {
                List<Integer> newItemset = new ArrayList<>(currentItemset);
                newItemset.add(item);
                this.dfs(newItemset, seenItemsets);
            }
        }
        this.updateMinExpectedUtility();
    }


    private void processCurrentItemset(List<Integer> currentItemset, List<Occurrence> occurrences, boolean updateMin) {
        int maxPeriod = this.calculateMaxPeriod(occurrences);
        float totalExpectedUtility = this.getTotalExpectedUtility(occurrences);
        int totalUtility = this.getTotalUtility(occurrences);

        if (topKSeen.contains(currentItemset) || totalUtility < 0) {
            return;
        }

        Itemset itemset = new Itemset(new ArrayList<>(currentItemset), totalUtility, totalExpectedUtility, maxPeriod);

        if (this.topKItemsets.size() < this.k) {
            this.topKItemsets.offer(itemset);
            topKSeen.add(new ArrayList<>(currentItemset));
        } else {
            Itemset lowestUtilityItemset = this.topKItemsets.peek();
            if (lowestUtilityItemset != null) {
                if (itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility() * 0.9 &&
                        itemset.getItems().size() > lowestUtilityItemset.getItems().size()) {
                    Itemset removed = this.topKItemsets.poll();
                    topKSeen.remove(removed.getItems());
                    this.topKItemsets.offer(itemset);
                    topKSeen.add(new ArrayList<>(currentItemset));
                } else if (itemset.getExpectedUtility() > lowestUtilityItemset.getExpectedUtility()) {
                    Itemset removed = this.topKItemsets.poll();
                    topKSeen.remove(removed.getItems());
                    this.topKItemsets.offer(itemset);
                    topKSeen.add(new ArrayList<>(currentItemset));
                }
            }
        }

        if (updateMin) {
            this.updateMinExpectedUtility();
        }
    }


    private void reset() {
        this.topKItemsets.clear();
        this.topKSeen.clear();
        this.tweu.clear();
        this.posUtility.clear();
        this.negUtility.clear();
        this.psuCache.clear();
        this.minExpectedUtility = this.calculateDatabaseUtility() * 0.001f;
    }

    public void evaluateTopKPerformance(String datasetTitle) {
        int[] kValues = {1, 5, 10, 15, 20, 25, 30};
        Map<Integer, Double> runtimeResults = new LinkedHashMap<>();

        System.out.println("Database Utility: " + this.calculateDatabaseUtility());
        System.out.println("Minimum Expected Utility: " + this.minExpectedUtility);
        System.out.println("-----------------------------------------------------------");

        for (int kValue : kValues) {
            this.k = kValue;
            this.reset();  // Use the new reset method
            System.out.println("\nRunning for Top-" + kValue + " Itemsets...");

            long startTime = System.nanoTime();
            this.run();
            double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;

            runtimeResults.put(kValue, executionTime);
            System.out.printf("Top-%d Execution Time: %.2f seconds%n", kValue, executionTime);
        }
        this.plotRuntimeResults(runtimeResults, datasetTitle); // Pass title to plotting
    }

    private void plotRuntimeResults(Map<Integer, Double> runtimeResults, String datasetTitle) { // Add parameter
        List<Integer> kValues = new ArrayList<>(runtimeResults.keySet());
        List<Double> runtimes = new ArrayList<>(runtimeResults.values());

        XYChart chart = new XYChartBuilder().width(800).height(600)
                .title(datasetTitle)  // Use title here
                .xAxisTitle("K-value").yAxisTitle("Runtime (s)").build();

        chart.addSeries("Runtime", kValues, runtimes);
        new SwingWrapper<>(chart).displayChart();
    }

    // ------------------------------ RUN METHOD ---------------------------------------//

    private void run() {


//        long startTime = System.nanoTime();

        // Initialize and optimize
        this.computeTWEU();
        this.filterLowUtilityItems();

        // Generate itemsets
        List<Itemset> results = this.generateItemsets();

        // Print results
//        double executionTime = (System.nanoTime() - startTime) / 1_000_000_000.0;
//        System.out.printf("Execution time: %.2f seconds%n", executionTime);
        System.out.println("Final Top-" + this.k + " Itemsets: ");
        if (results.isEmpty()) {
            System.out.println("No itemsets satisfy your condition");
        } else {
            results.forEach(System.out::println);
        }
    }
}


