package Top_HUIM_PosNeg;

import java.util.*;
import java.util.stream.*;

class TopKHUIM {

    public static List<Map.Entry<Set<Integer>, Integer>> calculateTopKOptimized(
            List<Transaction> transactions, int k) {

        // Input validation
        if (k <= 0 || transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("Invalid input parameters");
        }

        // Step 1: Merge and sort transactions
        transactions = UtilityCalculator.mergeAndSortTransactions(transactions);

        // Step 2: Extract items and calculate initial PRIU
        Set<Integer> items = transactions.stream()
                .flatMap(t -> t.items.stream())
                .collect(Collectors.toSet());

        Map<Integer, Integer> priu = UtilityCalculator.calculatePRIU(transactions, items);

        // Step 3: Initialize and update minUtil using PRIU and LIU
        int minUtil = UtilityCalculator.initializeMinUtil(priu, k);
        Map<List<Integer>, Integer> liuStructure = UtilityCalculator.buildLIUStructure(
                transactions, new ArrayList<>(items));
        minUtil = UtilityCalculator.applyPLIUStrategies(liuStructure, k, minUtil);

        // Step 4: Generate and prune candidates
        List<Set<Integer>> orderedItems = UtilityCalculator.sortItemsByTotalOrder(priu);
        Set<Set<Integer>> candidateItemsets = UtilityCalculator.generateCandidateItemsets(
                transactions, orderedItems, minUtil);

        // Step 5: Calculate final utilities and apply pruning
        Map<Set<Integer>, Integer> itemsetUtilities = UtilityCalculator.calculateItemsetUtilities(
                transactions, candidateItemsets);
        Map<Set<Integer>, Integer> psuMap = UtilityCalculator.calculatePSU(
                transactions, candidateItemsets);
        Map<Set<Integer>, Integer> prunedItemsets = UtilityCalculator.pruneItemsetsUsingPSU(
                itemsetUtilities, psuMap, minUtil);

        // Step 6: Sort and return top-k results
        return prunedItemsets.entrySet().stream()
                .sorted(Map.Entry.<Set<Integer>, Integer>comparingByValue().reversed())
                .limit(k)
                .collect(Collectors.toList());
    }
}