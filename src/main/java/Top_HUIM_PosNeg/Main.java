package Top_HUIM_PosNeg;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {
        String filePath1 = "src/main/java/dataset/test.txt";
        String filePath2 = "src/main/java/dataset/chess_negative.txt";
        String filePath3 = "src/main/java/dataset/mushroom_utility.txt";

        int k = 4; // Number of top-k itemsets to retrieve
        int minUtil = 0; // Initial minimum utility

        // Step 1: Parse the dataset
        List<Transaction> transactions = DatasetParser.parseSPMFDataset(filePath1);

//        // Step 2: Test Parsing
//        UtilityCalculator.testParsing(transactions);
//
//        // Step 3: Test Utility Calculation
        UtilityCalculator.testUtilityCalculation(transactions);

        // Step 4: Generate Ordered Items (for candidate generation)
        Set<Integer> allItems = new HashSet<>();
        for (Transaction t : transactions) {
            allItems.addAll(t.items);
        }
        Map<Integer, Integer> priu = UtilityCalculator.calculatePRIU(transactions, allItems);
        List<Set<Integer>> orderedItems = UtilityCalculator.sortItemsByTotalOrder(priu);

        // Step 5: Test Candidate Generation
        UtilityCalculator.testCandidateGeneration(transactions, orderedItems, minUtil);

        // Step 6: Test MinUtil Adjustment
        Map<List<Integer>, Integer> liuStructure = UtilityCalculator.buildLIUStructure(transactions, new ArrayList<>(allItems));
        UtilityCalculator.testMinUtilAdjustment(liuStructure, k, minUtil);

        // Step 7: Test Pruning
        Set<Set<Integer>> candidateItemsets = UtilityCalculator.generateCandidateItemsets(transactions, orderedItems, minUtil);
        Map<Set<Integer>, Integer> itemsetUtilities = UtilityCalculator.calculateItemsetUtilities(transactions, candidateItemsets);
        Map<Set<Integer>, Integer> psuMap = UtilityCalculator.calculatePSU(transactions, candidateItemsets);
        UtilityCalculator.testPruning(itemsetUtilities, psuMap, minUtil);

        // Step 8: Test Top-K Results
        UtilityCalculator.testTopKResults(transactions, k);
    }


}
