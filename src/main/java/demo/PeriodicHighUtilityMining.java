package demo;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class PeriodicHighUtilityMining {

    private List<Transaction> database; // List of transactions
    private double minUtilThreshold; // Minimum utility threshold
    private List<Itemset> resultItemsets;

    public PeriodicHighUtilityMining() {
        this.database = new ArrayList<>();
        this.resultItemsets = new ArrayList<>();
    }

    // Transaction class to store parsed data
    public static class Transaction {
        List<Integer> items;
        List<Integer> utilities;
        int transactionUtility;

        public Transaction(List<Integer> items, List<Integer> utilities, int transactionUtility) {
            this.items = items;
            this.utilities = utilities;
            this.transactionUtility = transactionUtility;
        }
    }

    // Itemset class
    public static class Itemset {
        List<Integer> items;
        int utility;

        public Itemset(List<Integer> items, int utility) {
            this.items = items;
            this.utility = utility;
        }

        @Override
        public String toString() {
            return "Itemset{" + "items=" + items + ", utility=" + utility + '}';
        }
    }

    // Parse the SPMF file
    public void loadDataset(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");

            // Parse items
            List<Integer> items = Arrays.stream(parts[0].split(" "))
                    .map(Integer::parseInt).collect(Collectors.toList());

            // Parse transaction utility
            int transactionUtility = Integer.parseInt(parts[1]);

            // Parse item utilities
            List<Integer> utilities = Arrays.stream(parts[2].split(" "))
                    .map(Integer::parseInt).collect(Collectors.toList());

            // Add transaction to database
            database.add(new Transaction(items, utilities, transactionUtility));
        }
        reader.close();
    }

    // Calculate the utility of each item
    private Map<Integer, Integer> calculateItemUtilities() {
        Map<Integer, Integer> itemUtilities = new HashMap<>();
        for (Transaction transaction : database) {
            for (int i = 0; i < transaction.items.size(); i++) {
                int item = transaction.items.get(i);
                int utility = transaction.utilities.get(i);
                itemUtilities.put(item, itemUtilities.getOrDefault(item, 0) + utility);
            }
        }
        return itemUtilities;
    }

    // Find itemsets above minUtil threshold
    public void findHighUtilityItemsets(double minUtil) {
        resultItemsets.clear();
        Map<Integer, Integer> itemUtilities = calculateItemUtilities();

        // Filter items by utility threshold
        for (Map.Entry<Integer, Integer> entry : itemUtilities.entrySet()) {
            if (entry.getValue() >= minUtil) {
                resultItemsets.add(new Itemset(Collections.singletonList(entry.getKey()), entry.getValue()));
            }
        }
    }

    // Run algorithm and return results for plotting
    public Map<Double, Integer> runAlgorithm(double[] minUtils) {
        Map<Double, Integer> results = new LinkedHashMap<>();

        for (double minUtil : minUtils) {
            long startTime = System.currentTimeMillis();

            findHighUtilityItemsets(minUtil);
            int patternCount = resultItemsets.size();

            long endTime = System.currentTimeMillis();
            System.out.println("min_util: " + minUtil + ", Patterns: " + patternCount + ", Time: " + (endTime - startTime) + " ms");

            results.put(minUtil, patternCount);
        }
        return results;
    }

    public static void main(String[] args) throws IOException {
        PeriodicHighUtilityMining mining = new PeriodicHighUtilityMining();

        // Load SPMF dataset
        String filePath = "sample_dataset.txt"; // Replace with your dataset path
        mining.loadDataset(filePath);

        // Define minUtil thresholds
        double[] minUtils = {30, 50, 70, 90, 110};

        // Run the algorithm
        Map<Double, Integer> results = mining.runAlgorithm(minUtils);

        // Output results for plotting
        System.out.println("Export for Plot:");
        System.out.println("min_util,patterns");
        results.forEach((key, value) -> System.out.println(key + "," + value));
    }
}
