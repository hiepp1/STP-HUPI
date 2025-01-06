package Top_HUIM_PosNeg;

import java.util.*;
import java.util.stream.Collectors;

class UtilityCalculator {

    // Generate all candidate itemsets
    public static void testParsing(List<Transaction> transactions) {
        System.out.println("Testing Parsing...");
        for (Transaction t : transactions) {
            System.out.println(t);
        }
        System.out.println("Parsing test completed.\n");
    }
    public static void testUtilityCalculation(List<Transaction> transactions) {
        System.out.println("Testing Utility Calculation...");
        for (Transaction t : transactions) {
            int totalUtility = t.getTotalUtility();
            System.out.println("Transaction: " + t + " | Total Utility: " + totalUtility);
        }
        System.out.println("Utility calculation test completed.\n");
    }
    public static void testCandidateGeneration(List<Transaction> transactions, List<Set<Integer>> orderedItems, int minUtil) {
        System.out.println("Testing Candidate Generation...");
        Set<Set<Integer>> candidateItemsets = generateCandidateItemsets(transactions, orderedItems, minUtil);
        System.out.println("Candidate Itemsets: " + candidateItemsets);
        System.out.println("Candidate generation test completed.\n");
    }
    public static void testPruning(Map<Set<Integer>, Integer> itemsetUtilities, Map<Set<Integer>, Integer> psuMap, int minUtil) {
        System.out.println("Testing Pruning...");
        Map<Set<Integer>, Integer> prunedItemsets = pruneItemsetsUsingPSU(itemsetUtilities, psuMap, minUtil);
        System.out.println("Pruned Itemsets: " + prunedItemsets);
        System.out.println("Pruning test completed.\n");
    }
    public static void testMinUtilAdjustment(Map<List<Integer>, Integer> liuStructure, int k, int minUtil) {
        System.out.println("Testing MinUtil Adjustment...");
        System.out.println("Initial MinUtil: " + minUtil);
        minUtil = applyPLIUStrategies(liuStructure, k, minUtil);
        System.out.println("Adjusted MinUtil: " + minUtil);
        System.out.println("MinUtil adjustment test completed.\n");
    }
    public static void testTopKResults(List<Transaction> transactions, int k) {
        System.out.println("Testing Top-K Results...");
        List<Map.Entry<Set<Integer>, Integer>> topK = TopKHUIM.calculateTopKOptimized(transactions, k);
        System.out.println("Top-k Results:");
        for (Map.Entry<Set<Integer>, Integer> entry : topK) {
            System.out.println("Itemset: " + entry.getKey() + " | Utility: " + entry.getValue());
        }
        System.out.println("Top-K results test completed.\n");
    }


    public static List<Transaction> mergeAndSortTransactions(List<Transaction> transactions) {
        Map<Set<Integer>, Transaction> mergedMap = new HashMap<>();

        for (Transaction t : transactions) {
            Set<Integer> itemSet = new HashSet<>(t.items);
            if (!mergedMap.containsKey(itemSet)) {
                mergedMap.put(itemSet, new Transaction(new ArrayList<>(t.items), new ArrayList<>(t.utilities)));
            } else {
                Transaction existing = mergedMap.get(itemSet);
                for (int i = 0; i < t.utilities.size(); i++) {
                    existing.utilities.set(i, existing.utilities.get(i) + t.utilities.get(i));
                }
            }
        }

        List<Transaction> mergedTransactions = new ArrayList<>(mergedMap.values());
        for (Transaction t : mergedTransactions) {
            t.sortItemsByUtility();
        }

        return mergedTransactions;
    }

    public static Map<Integer, Integer> calculatePRIU(List<Transaction> transactions, Set<Integer> items) {
        Map<Integer, Integer> priu = new HashMap<>();

        for (Integer item : items) {
            int utility = 0;
            for (Transaction t : transactions) {
                if (t.items.contains(item)) {
                    utility += t.utilities.get(t.items.indexOf(item));
                }
            }
            priu.put(item, utility);
        }

        return priu;
    }

    public static int initializeMinUtil(Map<Integer, Integer> priu, int k) {
        List<Integer> priuValues = new ArrayList<>(priu.values());
        priuValues.sort(Collections.reverseOrder());

        return priuValues.size() >= k ? priuValues.get(k - 1) : 0;
    }

    public static Map<List<Integer>, Integer> buildLIUStructure(List<Transaction> transactions, List<Integer> items) {
        Map<List<Integer>, Integer> liuStructure = new HashMap<>();

        for (Transaction t : transactions) {
            for (int i = 0; i < items.size(); i++) {
                for (int j = i; j < items.size(); j++) {
                    List<Integer> itemset = items.subList(i, j + 1);
                    int utility = 0;

                    if (t.items.containsAll(itemset)) {
                        for (Integer item : itemset) {
                            utility += t.utilities.get(t.items.indexOf(item));
                        }
                    }

                    liuStructure.put(itemset, liuStructure.getOrDefault(itemset, 0) + utility);
                }
            }
        }

        return liuStructure;
    }

    public static int applyPLIUStrategies(Map<List<Integer>, Integer> liuStructure, int k, int minUtil) {
        List<Integer> liuValues = new ArrayList<>(liuStructure.values());
        liuValues.sort(Collections.reverseOrder());

        if (liuValues.size() >= k) {
            minUtil = Math.max(minUtil, liuValues.get(k - 1));
        }

        return minUtil;
    }

    public static List<Set<Integer>> sortItemsByTotalOrder(Map<Integer, Integer> priu) {
        List<Map.Entry<Integer, Integer>> itemList = new ArrayList<>(priu.entrySet());
        itemList.sort((a, b) -> b.getValue() - a.getValue());

        List<Set<Integer>> orderedItems = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : itemList) {
            orderedItems.add(Collections.singleton(entry.getKey()));
        }

        return orderedItems;
    }

    // Claude AI //  ------------------------------------------
    public static Set<Set<Integer>> generateCandidateItemsets(List<Transaction> transactions, List<Set<Integer>> orderedItems, int minUtil) {
        Set<Set<Integer>> candidates = new HashSet<>();
        Queue<Set<Integer>> queue = new LinkedList<>(orderedItems);

        // Breadth-first generation of candidate itemsets
        while (!queue.isEmpty()) {
            Set<Integer> current = queue.poll();
            candidates.add(current);

            // Generate next level candidates
            for (Set<Integer> item : orderedItems) {
                if (shouldGenerateCandidate(current, item)) {
                    Set<Integer> newCandidate = new HashSet<>(current);
                    newCandidate.addAll(item);

                    // Check if all subsets are valid before adding
                    if (isValidCandidate(newCandidate, candidates, transactions, minUtil)) {
                        queue.offer(newCandidate);
                    }
                }
            }
        }

        return candidates;
    }
    private static boolean shouldGenerateCandidate(Set<Integer> current, Set<Integer> item) {
        // Ensure we only combine with items that maintain total order
        return Collections.max(current) < Collections.min(item);
    }
    private static boolean isValidCandidate(Set<Integer> candidate, Set<Set<Integer>> existingCandidates,
                                            List<Transaction> transactions, int minUtil) {
        // Check if all k-1 subsets exist in candidates
        for (Integer item : candidate) {
            Set<Integer> subset = new HashSet<>(candidate);
            subset.remove(item);
            if (!subset.isEmpty() && !existingCandidates.contains(subset)) {
                return false;
            }
        }

        // Early utility estimation
        int estimatedUtility = estimateUpperBoundUtility(candidate, transactions);
        return estimatedUtility >= minUtil;
    }
    private static int estimateUpperBoundUtility(Set<Integer> itemset, List<Transaction> transactions) {
        int upperBound = 0;
        for (Transaction t : transactions) {
            if (t.items.containsAll(itemset)) {
                int localUtility = 0;
                for (Integer item : itemset) {
                    localUtility += t.utilities.get(t.items.indexOf(item));
                }
                upperBound += localUtility;
            }
        }
        return upperBound;
    }
    public static Map<Set<Integer>, Integer> calculateItemsetUtilities(
            List<Transaction> transactions, Set<Set<Integer>> candidateItemsets) {
        Map<Set<Integer>, Integer> utilities = new HashMap<>();

        // Process each transaction only once for all candidates
        for (Transaction t : transactions) {
            Set<Set<Integer>> matchingItemsets = findMatchingItemsets(t, candidateItemsets);
            for (Set<Integer> itemset : matchingItemsets) {
                int utility = calculateUtilityInTransaction(itemset, t);
                utilities.merge(itemset, utility, Integer::sum);
            }
        }

        return utilities;
    }
    private static Set<Set<Integer>> findMatchingItemsets(
            Transaction t, Set<Set<Integer>> candidateItemsets) {
        return candidateItemsets.stream()
                .filter(itemset -> t.items.containsAll(itemset))
                .collect(Collectors.toSet());
    }
    private static int calculateUtilityInTransaction(Set<Integer> itemset, Transaction t) {
        return itemset.stream()
                .mapToInt(item -> t.utilities.get(t.items.indexOf(item)))
                .sum();
    }
    // Claude AI //  ------------------------------------------


    public static Map<Set<Integer>, Integer> calculatePSU(List<Transaction> transactions, Set<Set<Integer>> candidateItemsets) {
        Map<Set<Integer>, Integer> psuMap = new HashMap<>();

        for (Set<Integer> itemset : candidateItemsets) {
            int psu = 0;

            for (Transaction t : transactions) {
                if (t.items.containsAll(itemset)) {
                    for (Integer item : itemset) {
                        psu += Math.max(t.utilities.get(t.items.indexOf(item)), 0);
                    }
                }
            }

            psuMap.put(itemset, psu);
        }

        return psuMap;
    }

    public static Map<Set<Integer>, Integer> pruneItemsetsUsingPSU(Map<Set<Integer>, Integer> itemsetUtilities, Map<Set<Integer>, Integer> psuMap, int minUtil) {
        Map<Set<Integer>, Integer> prunedItemsets = new HashMap<>();

        for (Map.Entry<Set<Integer>, Integer> entry : itemsetUtilities.entrySet()) {
            if (entry.getValue() >= minUtil || psuMap.getOrDefault(entry.getKey(), 0) >= minUtil) {
                prunedItemsets.put(entry.getKey(), entry.getValue());
            }
        }

        return prunedItemsets;
    }
}


