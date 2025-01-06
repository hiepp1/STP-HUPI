package STP_HUPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filepath = "src/main/java/dataset/test1.txt";

        try {
            List<Transaction> transactions = DatasetReader.readDataset(filepath);
            System.out.println("Transactions loaded: " + transactions.size());


            ShortTimePeriodMining stp = new ShortTimePeriodMining(transactions, 10, 3, 0.01f);
            stp.run();
            stp.dfs_v1(new ArrayList<>(), 0, new HashSet<>());
            List<Itemset> topKItemsets = stp.getTopKItemsets();
            for (Itemset itemset : topKItemsets) {
                System.out.println(itemset);
            }
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
