package STP_HUPI;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/foodmart_utility_timestamps.txt";
        String filepath2 = "src/main/java/dataset/test1.txt";

        String filepath3 = "src/main/java/dataset/pumsp_negative.txt";

        String filepath4 = "src/main/java/done_dataset/retail_negative.txt";

        try {
//            List<List<Transaction>> transactions = DatasetReader.readDataset(filepath2);
//
//            int i = 1;
//            for (List<Transaction> transactionList : transactions) {
//                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
//                Algorithm stp = new Algorithm(transactionList, 1000, 0.000001f, 5);
//                stp.run();
//                i += 1;

            List<Transaction> transactions = DatasetReader.readDataset(filepath4);
            System.out.println("Transactions loaded: " + transactions.size());

            Algorithm stp = new Algorithm(transactions, Integer.MAX_VALUE, 10);
            stp.evaluateTopKPerformance(extractDatasetTitle(filepath3));

        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }

    private static String extractDatasetTitle(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.'); // Find last dot for extension
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex); // Remove extension
        }
        // Improved title extraction to handle potential directory structures:
        String[] pathParts = filepath.split("[\\\\/]"); // Split by \ or /
        if (pathParts.length > 1) { // If it is inside a directory
            filename = pathParts[pathParts.length - 1]; // Get the last part
            int dotIndex2 = filename.lastIndexOf('_');
            if (dotIndex2 > 0) {
                filename = filename.substring(0, dotIndex2); // Remove extension
            }
        }
        return filename;
    }
}
