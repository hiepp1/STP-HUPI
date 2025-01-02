package Top_HUIM_PosNeg;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String filePath = "src/main/java/dataset/test.txt"; // Replace with your dataset path
        int[] kValues = {5, 10, 20, 50};

        System.out.println("Runtime for different k values:");
        // Iterate over k values
        for (int k : kValues) {
            long startTime = System.currentTimeMillis(); // Start time

            List<Transaction> transactions = DatasetParser.parseSPMFDataset(filePath);
            List<Map.Entry<Integer, Integer>> topK = TopKHUIM.calculateTopKOptimized(transactions, k);

            long endTime = System.currentTimeMillis(); // End time
            double runtime = (endTime - startTime) / 1000.0; // Convert to seconds

            System.out.printf("k = %d, Runtime = %.2f seconds\n", k, runtime);
        }
    }
}

//    public static void main(String[] args) throws IOException {
//        String filepath = "src/main/java/dataset/test.txt";
//        int[] kValues = {5,10,20,50};
//
//        RuntimeVisualizer.plotRuntime(filepath, kValues);
//
//        List<Transaction> transactions = DatasetParser.parseSPMFDataset(filepath);
//        int k = 5;
//        List<Map.Entry<Integer,Integer>> topK = TopKHUIM.calculateTopKOptimized(transactions, k);
//        System.out.println("\nTop-k High Utility Itemsets:");
//
//        for (Map.Entry<Integer, Integer> entry : topK) {
//            System.out.printf("Itemset: %d, Net Utility: %d\n", entry.getKey(), entry.getValue());
//        }
//
//    }

    //  public class Main {
    //    public static void main(String[] args) throws IOException {
    //        String filePath = "src/main/java/dataset/test.txt"; // Your dataset path
    //        List<Transaction> transactions = DatasetParser.parseSPMFDataset(filePath);
    //        int k = 5;
    //
    //        Set<Integer> candidateItems = new HashSet<>();
    //        for (Transaction t : transactions) {
    //            candidateItems.addAll(t.items);
    //        }
    //
    //        Map<Integer, Map<String, Integer>> itemsetUtilities =
    //                UtilityCalculator.calculateItemsetUtilities(transactions, candidateItems);
    //        List<Map.Entry<Integer, Integer>> topK =
    //                TopKHUIM.calculateTopKOptimized(transactions, k);
    //
    //        // Print detailed analysis
    ////        TopKHUIM.printDetailedAnalysis(itemsetUtilities, topK);
    //    }
    //}
