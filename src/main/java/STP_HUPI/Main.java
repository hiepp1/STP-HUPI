package STP_HUPI;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/fruithut_utility_timestamps.txt";
        String filepath2 = "src/main/java/dataset/test1.txt";
        String filepath3 = "src/main/java/dataset/BMS_utility.txt";

        try {
//            List<List<Transaction>> transactions = DatasetReader.readDataset(filepath);
//
//            int i = 1;
//            for (List<Transaction> transactionList : transactions) {
//                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
//                ShortTimePeriodMining stp = new ShortTimePeriodMining(transactionList, 50, 0.001f, 5);
//                stp.run();
//                i += 1;
//            }

            List<Transaction> transactions = DatasetReader.readDataset(filepath3);
            System.out.println("Transactions loaded: " + transactions.size());
            ShortTimePeriodMining stp = new ShortTimePeriodMining(transactions, 500, 0.001f, 5);
            stp.run();

        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
