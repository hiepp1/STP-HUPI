package Top_HUIM_PosNeg;

import java.util.*;

class TopKHUIM {
    private static final double MIN_POSITIVE_RATIO = 0.3; // Minimum ratio of positive utilities required

    public static List<Map.Entry<Integer, Integer>> getTopKItemsets(Map<Integer, Map<String, Integer>> itemsetUtilities, int k) {
        List<Map.Entry<Integer, Integer>> sortedItemsets = new ArrayList<>();

        for (Map.Entry<Integer, Map<String, Integer>> entry : itemsetUtilities.entrySet()) {
            Map<String, Integer> metrics = entry.getValue();
//            int netUtility = metrics.get("posUtility") + metrics.get("negUtility");
            int netUtility = entry.getValue().get("posUtility") + entry.getValue().get("negUtility");
            sortedItemsets.add(Map.entry(entry.getKey(), netUtility));
        }

        sortedItemsets.sort((a, b) -> b.getValue() - a.getValue());
        return sortedItemsets.subList(0, Math.min(k, sortedItemsets.size()));
    }
//    public static void printDetailedAnalysis(Map<Integer, Map<String, Integer>> itemsetUtilities, List<Map.Entry<Integer, Integer>> topK) {
//        System.out.println("\nDetailed Analysis of Top-k High Utility Itemsets:");
//        System.out.println("------------------------------------------------");
//
//        for (Map.Entry<Integer, Integer> entry : topK) {
//            int item = entry.getKey();
//            Map<String, Integer> metrics = itemsetUtilities.get(item);
//
//            int posUtility = metrics.get("posUtility");
//            int negUtility = metrics.get("negUtility");
//            int posFreq = metrics.get("posFrequency");
//            int negFreq = metrics.get("negFrequency");
//            int totalOcc = metrics.get("totalOccurrences");
//
//            double posUtilityRatio = posUtility / (double)(posUtility - negUtility) * 100;
//            double posFrequencyRatio = (posFreq / (double)totalOcc) * 100;
//
//            System.out.println("\nItemset: " + item);
//            System.out.printf("Net Utility: %d%n", entry.getValue());
//            System.out.printf("Positive Utility: %d%n", posUtility);
//            System.out.printf("Negative Utility: %d%n", negUtility);
//            System.out.printf("Utility Ratio (Pos/Total): %.2f%%%n", posUtilityRatio);
//            System.out.printf("Occurrence Frequency:%n");
//            System.out.printf("  - Positive: %d (%.2f%%)%n", posFreq, posFrequencyRatio);
//            System.out.printf("  - Negative: %d (%.2f%%)%n", negFreq, 100 - posFrequencyRatio);
//            System.out.printf("  - Total Occurrences: %d%n", totalOcc);
//        }
//    }

    public static List<Map.Entry<Integer, Integer>> calculateTopKOptimized(List<Transaction> transactions, int k) {
        Set<Integer> candidateItems = new HashSet<>();
        for (Transaction t : transactions) {
            candidateItems.addAll(t.items);
        }

        Map<Integer, Map<String, Integer>> itemsetUtilities = UtilityCalculator.calculateItemsetUtilities(transactions, candidateItems);
        int minUtil = UtilityCalculator.applyPRIUStrategy(itemsetUtilities, k);

        List<Integer> itemsList = new ArrayList<>(candidateItems);
        Map<List<Integer>, Integer> liuStructure = UtilityCalculator.buildLIUStructure(transactions, itemsList);
        minUtil = UtilityCalculator.applyPLIUStrategy(liuStructure, k, minUtil);

        return getTopKItemsets(itemsetUtilities, k);
    }
}