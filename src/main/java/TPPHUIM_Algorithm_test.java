//import java.util.*;
//
//public class TPPHUIM_Algorithm_test {
//    private final List<Transaction> database;
//    private final int k;
//    private final int minPeriod;
//    private final double confidenceThreshold;
//    private final long MAX_EXECUTION_TIME = 30000;
//    private long startTime;
//
//    public TPPHUIM_Algorithm_test(List<Transaction> database, int k, int minPeriod, double confidenceThreshold) {
//        this.database = database;
//        this.k = k;
//        this.minPeriod = minPeriod;
//        this.confidenceThreshold = confidenceThreshold;
//    }
//
//    public TPPHUIM_Algorithm_test() {
//    }
//
//    public List<Itemset> run() {
//        startTime = System.currentTimeMillis();  // Track start time
//        List<Itemset> topKItemsets = new ArrayList<>();
//
//        System.out.println("Starting TPPHUIM algorithm...");
//        Map<String, Integer> ptwu = computePtwu();
//        System.out.println("Computed PTWU: " + ptwu);
//
//        List<String> sortedItems = new ArrayList<>(ptwu.keySet());
//        sortedItems.sort(Comparator.comparingInt(ptwu::get));
//        System.out.println("Sorted items based on PTWU: " + sortedItems);
//
//        explorePatterns(new HashSet<>(), sortedItems, 0, topKItemsets);
//
//        System.out.println("Execution finished.");
//        return topKItemsets;
//    }
//
//    private Map<String, Integer> computePtwu() {
//        Map<String, Integer> ptwu = new HashMap<>();
//        for (Transaction transaction : database) {
//            for (String item : transaction.getItems().keySet()) {
//                int tu = transaction.computeTransactionUtility();
//                ptwu.put(item, ptwu.getOrDefault(item, 0) + tu);
//            }
//        }
//        return ptwu;
//    }
//
//    private PriorityQueue<Itemset> topKQueue = new PriorityQueue<>(Comparator.comparingInt(Itemset::getUtility));
//
////    private void explorePatterns(Set<String> currentItemset, List<String> candidateList, int minUtility, List<Itemset> topKItemsets) {
////        for (int i = 0; i < candidateList.size(); i++) {
////            String currentItem = candidateList.get(i);
////
////            // Extend current itemset with the current item
////            Set<String> newItemset = new HashSet<>(currentItemset);
////            newItemset.add(currentItem);
////
////            // Calculate utility, periodicity, and confidence
////            int utility = calculateUtility(newItemset);
////            int periodicity = calculatePeriodicity(newItemset);
////            double confidence = calculateConfidence(newItemset);
////
////            // Prune unpromising candidates
////            if (utility + estimateRemainingUtility(newItemset, candidateList) < minUtility ||
////                    periodicity > minPeriod ||
////                    confidence < confidenceThreshold) {
////                continue;
////            }
////
////            // Add itemset to Top-K if it meets constraints
////            if (utility >= minUtility && periodicity <= minPeriod && confidence >= confidenceThreshold) {
////                Itemset newItemsetEntry = new Itemset(newItemset, utility, periodicity, confidence);
////                topKItemsets.add(newItemsetEntry);
////                System.out.println("Adding itemset to Top-K: " + newItemsetEntry);
////
////                // Maintain only top-k results
////                if (topKItemsets.size() > k) {
////                    topKItemsets.sort(Comparator.comparingInt(Itemset::getUtility).reversed());
////                    Itemset removedItemset = topKItemsets.remove(topKItemsets.size() - 1);
////                    minUtility = topKItemsets.get(topKItemsets.size() - 1).getUtility();
////                    System.out.println("Removed least utility itemset: " + removedItemset);
////                }
////            }
////
////            // Recursive call for remaining candidates
////            List<String> remainingCandidates = candidateList.subList(i + 1, candidateList.size());
////            if (utility + estimateRemainingUtility(newItemset, remainingCandidates) >= minUtility) {
////                explorePatterns(newItemset, remainingCandidates, minUtility, topKItemsets);
////            }
////        }
////    }
//
//    private void explorePatterns(Set<String> currentItemset, List<String> candidateList, int minUtility, List<Itemset> topKItemsets) {
//        for (int i = 0; i < candidateList.size(); i++) {
//            if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME) {
//                System.out.println("Execution time limit reached.");
//                return;  // Stop if execution time exceeded
//            }
//
//            String currentItem = candidateList.get(i);
//            Set<String> newItemset = new HashSet<>(currentItemset);
//            newItemset.add(currentItem);
//
//            int utility = calculateUtility(newItemset);
//            int periodicity = calculatePeriodicity(newItemset);
//            double confidence = calculateConfidence(newItemset);
//
//            if (utility >= minUtility && periodicity <= minPeriod && confidence >= confidenceThreshold) {
//                Itemset newItemsetEntry = new Itemset(newItemset, utility, periodicity, confidence);
//
//                // Maintain Top-K using a priority queue
//                if (topKQueue.size() < k) {
//                    topKQueue.offer(newItemsetEntry);
//                } else if (topKQueue.peek().getUtility() < utility) {
//                    System.out.println("Replacing: " + topKQueue.poll() + " with " + newItemsetEntry);
//                    topKQueue.offer(newItemsetEntry);
//                }
//
//                minUtility = topKQueue.peek().getUtility();  // Update min utility
//            }
//
//            List<String> remainingCandidates = candidateList.subList(i + 1, candidateList.size());
//            explorePatterns(newItemset, remainingCandidates, minUtility, topKItemsets);
//        }
//    }
//
//
//    private int calculateUtility(Set<String> itemset) {
//        int totalUtility = 0;
//        Random rand = new Random();
//
//        for (Transaction transaction : database) {
//            if (transaction.getItems().keySet().containsAll(itemset)) {
//                for (String item : itemset) {
//                    // Apply probabilistic adjustment
//                    double probability = 0.5 + (rand.nextDouble() * 0.5);  // Random value between 0.5 and 1.0
//                    int utility = (int)(transaction.getItems().get(item) *
//                            transaction.getUtilities().get(item) * probability);
//                    totalUtility += utility;
//                }
//            }
//        }
//        return totalUtility;
//    }
//
//    private int calculatePeriodicity(Set<String> itemset) {
//        List<Integer> transactionIndices = new ArrayList<>();
//        for (int i = 0; i < database.size(); i++) {
//            Transaction transaction = database.get(i);
//            if (transaction.getItems().keySet().containsAll(itemset)) {
//                transactionIndices.add(i);
//            }
//        }
//
//        if (transactionIndices.size() < 2) {
//            return database.size(); // Return maximum periodicity for sparse itemsets
//        }
//
//        int maxGap = 0;
//        for (int i = 1; i < transactionIndices.size(); i++) {
//            maxGap = Math.max(maxGap, transactionIndices.get(i) - transactionIndices.get(i - 1));
//        }
//
//        return maxGap;
//    }
//
//    private double calculateConfidence(Set<String> itemset) {
//        double totalUtilityContribution = 0;
//        double totalTransactionUtility = 0;
//
//        for (Transaction transaction : database) {
//            double transactionUtility = transaction.computeTransactionUtility();
//            totalTransactionUtility += Math.abs(transactionUtility);
//
//            if (transaction.getItems().keySet().containsAll(itemset)) {
//                double itemsetUtility = itemset.stream()
//                        .mapToDouble(item -> transaction.getItems().get(item) * transaction.getUtilities().get(item))
//                        .sum();
//                totalUtilityContribution += Math.abs(itemsetUtility);
//            }
//        }
//
//        return totalUtilityContribution / totalTransactionUtility;
//    }
//
//    private int estimateRemainingUtility(Set<String> currentItemset, List<String> remainingItems) {
//        int remainingUtility = 0;
//        for (String item : remainingItems) {
//            for (Transaction transaction : database) {
//                if (transaction.getItems().containsKey(item)) {
//                    int utility = transaction.getItems().get(item) * transaction.getUtilities().get(item);
//                    remainingUtility += Math.max(utility, 0);
//                }
//            }
//        }
//        return remainingUtility;
//    }
//}
