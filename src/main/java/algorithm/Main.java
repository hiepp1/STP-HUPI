package algorithm;

import algorithm.ST_HUPI.STHUPIAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath1 = "src/main/java/dataset/negativeDataset/retail_negative.txt";
        String filepath2 = "src/main/java/dataset/negativeDataset/mushroom_negative.txt";
        String filepath3 = "src/main/java/dataset/timestampDataset/foodmart_timestamp.txt";
        String filepath4 = "src/main/java/dataset/timestampDataset/mushroom_timestamp.txt";
        String filepath5 = "src/main/java/dataset/timestampDataset/ecommerce_timestamp.txt";
        String filepath6 = "src/main/java/dataset/timestampDataset/kosarak_timestamp.txt";

        try {
            List<List<Transaction>> transactions = DatasetReader.readTimestampDataset(filepath6);

            int i = 1;
            for (List<Transaction> transactionList : transactions) {
                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                STPHUPIAlgorithm stpHupiAlgorithm = new STPHUPIAlgorithm(transactionList, 300);
                stpHupiAlgorithm.evaluateTopKPerformance(extractDatasetTitle(filepath6));
                i += 1;
            }
//            List<Transaction> transactions = DatasetReader.readDataset(filepath3);
//            System.out.println("Transactions loaded: " + transactions.size());
//
//            STPHUPIAlgorithm stpHupiAlgorithm = new STPHUPIAlgorithm(transactions, Integer.MAX_VALUE);
//            stpHupiAlgorithm.evaluateTopKPerformance(extractDatasetTitle(filepath3));

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
