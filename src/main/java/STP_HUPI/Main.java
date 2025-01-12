package STP_HUPI;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/foodmart_utility_timestamp.txt";
        try {
            List<List<Transaction>> transactions = DatasetReader.readDataset(filepath);



            int i = 1;
            for (List<Transaction> transactionList : transactions) {
                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                ShortTimePeriodMining stp = new ShortTimePeriodMining(transactionList, 50, 0.001f, 5);
                stp.run();
                i += 1;
            }

//            Map<Integer, UtilityList> utilityLists = DatasetReader.buildUtilityLists(transactions);
//            utilityLists.forEach((key, value) -> System.out.println(value));

//
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
