import java.io.*;
import java.util.*;

public class TPPHUIM {
    public static void main(String[] args) throws IOException {
        String datasetPath = "src/main/java/dataset.txt";
        List<Transaction> database = DatasetParser.parseDataset(datasetPath);

        int k = 3;
        int minPeriod = 5;
        double confidenceThreshold = 0.1;

        TPPHUIM_Algorithm algorithm = new TPPHUIM_Algorithm(database, k, minPeriod, confidenceThreshold);
        List<Itemset> topKItemsets = algorithm.run();

        System.out.println("Top-k Periodic High Utility Itemsets:");
        for (Itemset itemset : topKItemsets) {
            System.out.println(itemset);
        }
    }
}
