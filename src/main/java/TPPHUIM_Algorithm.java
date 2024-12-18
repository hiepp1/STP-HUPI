import java.util.*;

public class TPPHUIM_Algorithm {
    private final List<Transaction> database;
    private final int k;
    private final int minPeriod;
    private final double confidenceThreshold;
    private final long MAX_EXECUTION_TIME = 30000;
    private long startTime;

    private final PriorityQueue<Itemset> topKQueue = new PriorityQueue<>(Comparator.comparingInt(Itemset::getUtility));

    public TPPHUIM_Algorithm(List<Transaction> database, int k, int minPeriod, double confidenceThreshold) {
        this.database = database;
        this.k = k;
        this.minPeriod = minPeriod;
        this.confidenceThreshold = confidenceThreshold;
    }

    public List<Itemset> run() {
        startTime = System.currentTimeMillis(); // Track start time
        List<Itemset> topKItemsets = new ArrayList<>();
        topKItemsets.removeIf(itemset -> itemset.getPeriodicity() > minPeriod || itemset.getConfidence() < confidenceThreshold);

        Map<String, Integer> ptwu = computePtwu();
        System.out.println("Computed PTWU: " + ptwu);

        // Sort items based on PTWU
        List<String> sortedItems = new ArrayList<>(ptwu.keySet());
        sortedItems.sort(Comparator.comparingInt(ptwu::get));
        System.out.println("Sorted items based on PTWU: " + sortedItems);

        // Begin exploration
        explorePatterns(new HashSet<>(), sortedItems, 0, topKItemsets);

        return topKItemsets;
    }

    // Step 1: Compute PTWU (positive transaction-weighted utility)
    private Map<String, Integer> computePtwu() {
        Map<String, Integer> ptwu = new HashMap<>();
        for (Transaction transaction : database) {
            for (String item : transaction.getItems().keySet()) {
                int itemUtility = transaction.getItems().get(item) * transaction.getUtilities().get(item);
                ptwu.put(item, ptwu.getOrDefault(item, 0) + itemUtility); // Sum both positive and negative utilities
            }
        }
        return ptwu;
    }

    // Step 2: Recursive pattern exploration
    private void explorePatterns(Set<String> currentItemset, List<String> candidateList, int minUtility, List<Itemset> topKItemsets) {
        for (int i = 0; i < candidateList.size(); i++) {
            // Check time limit
            if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME) {
                System.out.println("Execution time limit reached.");
                return;
            }

            String currentItem = candidateList.get(i);
            Set<String> newItemset = new HashSet<>(currentItemset);
            newItemset.add(currentItem);

            // Validate if the newItemset exists in the dataset
            boolean existsInAnyTransaction = database.stream()
                    .anyMatch(transaction -> transaction.getItems().keySet().containsAll(newItemset));

            if (!existsInAnyTransaction) {
                continue; // Skip further processing for this itemset
            }

            // Calculate utility, periodicity, and confidence
            int utility = calculateUtility(newItemset);
            int periodicity = calculatePeriodicity(newItemset);
            if (periodicity > minPeriod) {
                System.out.printf("Itemset %s rejected due to periodicity: %d%n", newItemset, periodicity);
                continue; // Skip itemset
            }
            double confidence = calculateConfidence(newItemset);

            // Add valid itemsets to Top-K
            if (utility >= minUtility && periodicity <= minPeriod && confidence >= confidenceThreshold) {
                Itemset newItemsetEntry = new Itemset(newItemset, utility, periodicity, confidence);
                addToTopK(topKItemsets, newItemsetEntry);
                minUtility = topKQueue.peek().getUtility(); // Update minUtility
            }

            // Recursive call for remaining candidates
            List<String> remainingCandidates = candidateList.subList(i + 1, candidateList.size());
            if (utility + estimateRemainingUtility(newItemset, remainingCandidates) >= minUtility) {
                explorePatterns(newItemset, remainingCandidates, minUtility, topKItemsets);
            }
        }
    }


    // Add itemset to Top-K priority queue
    private void addToTopK(List<Itemset> topKItemsets, Itemset newItemset) {
        if (topKQueue.size() < k) {
            topKQueue.offer(newItemset);
            topKItemsets.add(newItemset);
        } else if (topKQueue.peek().getUtility() < newItemset.getUtility()) {
            Itemset removedItemset = topKQueue.poll();
            topKQueue.offer(newItemset);
            topKItemsets.removeIf(item -> item.equals(removedItemset));
            topKItemsets.add(newItemset);
        }
    }

    // Step 3: Utility calculation
    private int calculateUtility(Set<String> itemset) {
        int totalUtility = 0;

        // Check if itemset exists in any transaction
        boolean existsInAnyTransaction = false;

        for (Transaction transaction : database) {
            if (transaction.getItems().keySet().containsAll(itemset)) {
                existsInAnyTransaction = true;
                for (String item : itemset) {
                    int itemUtility = transaction.getItems().get(item) * transaction.getUtilities().get(item);
                    totalUtility += itemUtility;

                }
            }
        }

        if (!existsInAnyTransaction) {
            return 0;
        }

        return totalUtility;
    }





    // Step 4: Periodicity calculation
    private int calculatePeriodicity(Set<String> itemset) {
        List<Integer> transactionIndices = new ArrayList<>();
        for (int i = 0; i < database.size(); i++) {
            if (database.get(i).getItems().keySet().containsAll(itemset)) {
                transactionIndices.add(i);
            }
        }

        if (transactionIndices.size() < 2) {
            return database.size(); // Maximum periodicity for sparse itemsets
        }

        int maxGap = 0;
        for (int i = 1; i < transactionIndices.size(); i++) {
            maxGap = Math.max(maxGap, transactionIndices.get(i) - transactionIndices.get(i - 1));
        }
        return maxGap;
    }

    // Step 5: Confidence calculation
    private double calculateConfidence(Set<String> itemset) {
        double totalUtilityContribution = 0;
        double totalTransactionUtility = 0;

        for (Transaction transaction : database) {
            double transactionUtility = transaction.computeTransactionUtility();
            totalTransactionUtility += Math.abs(transactionUtility);

            if (transaction.getItems().keySet().containsAll(itemset)) {
                double itemsetUtility = itemset.stream()
                        .mapToDouble(item -> transaction.getItems().get(item) * transaction.getUtilities().get(item))
                        .sum();
                totalUtilityContribution += Math.abs(itemsetUtility);
            }
        }
        return totalUtilityContribution / totalTransactionUtility;
    }

    // Step 6: Estimate remaining utility
    private int estimateRemainingUtility(Set<String> currentItemset, List<String> remainingItems) {
        int remainingUtility = 0;
        for (String item : remainingItems) {
            for (Transaction transaction : database) {
                if (transaction.getItems().containsKey(item)) {
                    int utility = transaction.getItems().get(item) * transaction.getUtilities().get(item);
                    remainingUtility += Math.max(utility, 0);
                }
            }
        }
        return remainingUtility;
    }
}
