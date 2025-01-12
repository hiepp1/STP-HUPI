package STP_HUPI;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/test.txt";
        try {
            List<Transaction> transactions = DatasetReader.readDataset(filepath);
            System.out.println("Transactions loaded: " + transactions.size());

//            Map<Integer, UtilityList> utilityLists = DatasetReader.buildUtilityLists(transactions);
//            utilityLists.forEach((key, value) -> System.out.println(value));

            ShortTimePeriodMining stp = new ShortTimePeriodMining(transactions, 50, 0.001f, 5);
            stp.run();
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
