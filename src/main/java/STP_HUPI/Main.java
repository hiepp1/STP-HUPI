package STP_HUPI;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/test1.txt";

        try {
            List<Transaction> transactions = DatasetReader.readDataset(filepath);
            System.out.println("Transactions loaded: " + transactions.size());

            ShortTimePeriodMining stp = new ShortTimePeriodMining(transactions, 100, 0.001f, 10);
            stp.run();
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
