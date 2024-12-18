package demo;

import java.util.*;
import java.util.stream.Collectors;

public class PeriodicHighUtilityMinin {

    // Sample database structure: transaction ID to list of items bought in that transaction
    private List<List<String>> database;
    private Map<String, Double> externalUtility; // External utility values for items
    private Map<String, Double> internalUtility; // Internal utility values for items
    private double minUtil = 2.0; // Minimum utility threshold
    private double minRec = 2.0; // Minimum recency threshold
    private double theta = 0.5; // Parameter for filtering transactions based on utility
    private int maxPeriod = 10; // Maximum period for periodic itemsets
    private int topK = 10; // Number of top itemsets to return
    private List<Itemset> resultItemsets = new ArrayList<>(); // Final list of itemsets

    // Constructor to initialize database and utility maps
    public PeriodicHighUtilityMinin(List<List<String>> database, Map<String, Double> externalUtility) {
        this.database = database;
        this.externalUtility = externalUtility;
        this.internalUtility = new HashMap<>();
    }

    // Class to represent an itemset with its utility, recency, and periodicity information
    public static class Itemset {
        private List<String> items;
        private double utility;
        private double recency;
        private int maxPeriod;

        public Itemset(List<String> items, double utility, double recency, int maxPeriod) {
            this.items = items;
            this.utility = utility;
            this.recency = recency;
            this.maxPeriod = maxPeriod;
        }

        public double getUtility() {
            return utility;
        }

        public double getRecency() {
            return recency;
        }

        public int getMaxPeriod() {
            return maxPeriod;
        }

        @Override
        public String toString() {
            return "Itemset{" +
                    "items=" + items +
                    ", utility=" + utility +
                    ", recency=" + recency +
                    ", maxPeriod=" + maxPeriod +
                    '}';
        }
    }

    // Calculate the probabilistic utility of an item based on a distribution
    public double calculateProbabilisticUtility(String item) {
        Random rand = new Random();
        double probability = rand.nextDouble(); // Example: Random probability for the item
        return probability * externalUtility.get(item);
    }

    // Check if an itemset is periodic (i.e., it recurs at regular intervals)
    public boolean isPeriodic(List<Integer> transactionTids, int minPeriod) {
        Set<Integer> periods = new HashSet<>();
        for (int i = 1; i < transactionTids.size(); i++) {
            int period = transactionTids.get(i) - transactionTids.get(i - 1);
            if (period <= minPeriod) {
                periods.add(period);
            }
        }
        return periods.size() > 1;
    }

    // Compute the transaction utility (TWU) for each item
    public void computeTWUAndRecForSingleItems() {
        for (List<String> transaction : database) {
            for (String item : transaction) {
                internalUtility.put(item, internalUtility.getOrDefault(item, 0.0) + calculateProbabilisticUtility(item));
            }
        }
    }

    // Build the EUCS (Extended Utility Calculation Structure)
    public class EUCS {
        private Map<String, List<Integer>> itemTransactionMap; // Maps items to transaction IDs

        public EUCS(List<List<String>> database) {
            itemTransactionMap = new HashMap<>();
            for (int i = 0; i < database.size(); i++) {
                List<String> transaction = database.get(i);
                for (String item : transaction) {
                    itemTransactionMap.computeIfAbsent(item, k -> new ArrayList<>()).add(i);
                }
            }
        }

        public List<Integer> getTransactionsForItem(String item) {
            return itemTransactionMap.getOrDefault(item, new ArrayList<>());
        }
    }

    // Construct the RSPUL (Recency and Utility) for a given itemset
    public RSPUL constructRSPUL(List<String> itemset, EUCS eucs) {
        double utilitySum = 0;
        double recencySum = 0;
        int maxPeriod = 0;

        // Get the transactions for each item in the itemset
        List<Integer> transactionTids = new ArrayList<>();
        for (String item : itemset) {
            transactionTids.addAll(eucs.getTransactionsForItem(item));
            utilitySum += calculateProbabilisticUtility(item);
        }

        // Calculate recency and periodicity
        recencySum = calculateRecency(transactionTids);
        maxPeriod = calculateMaxPeriod(transactionTids);

        return new RSPUL(utilitySum, recencySum, maxPeriod);
    }

    // Calculate recency of the itemset in terms of transaction ID proximity
    public double calculateRecency(List<Integer> transactionTids) {
        if (transactionTids.isEmpty()) return 0;
        Collections.sort(transactionTids);
        return transactionTids.get(transactionTids.size() - 1) - transactionTids.get(0); // Difference between last and first transaction
    }

    // Calculate the maximum period (interval between transactions)
    public int calculateMaxPeriod(List<Integer> transactionTids) {
        if (transactionTids.size() <= 1) return 0;
        int maxPeriod = 0;
        for (int i = 1; i < transactionTids.size(); i++) {
            maxPeriod = Math.max(maxPeriod, transactionTids.get(i) - transactionTids.get(i - 1));
        }
        return maxPeriod;
    }

    // Recursive function to explore itemsets of higher orders
    public void searchRSPHUIM(List<String> prefix, List<String> remainingItems, EUCS eucs) {
        for (int i = 0; i < remainingItems.size(); i++) {
            List<String> newPrefix = new ArrayList<>(prefix);
            newPrefix.add(remainingItems.get(i));

            // Construct RSPUL for the new itemset
            RSPUL rspul = constructRSPUL(newPrefix, eucs);

            // Check if it satisfies the utility and recency thresholds, and if the period is valid
            if (rspul.getUtilitySum() >= minUtil && rspul.getRecencySum() >= minRec && rspul.getMaxPeriod() <= maxPeriod) {
                resultItemsets.add(new Itemset(newPrefix, rspul.getUtilitySum(), rspul.getRecencySum(), rspul.getMaxPeriod()));
                System.out.println("Added Itemset: " + newPrefix + " with Utility: " + rspul.getUtilitySum());
            }

            // Recursively call the search function with the new itemset prefix
            searchRSPHUIM(newPrefix, remainingItems.subList(i + 1, remainingItems.size()), eucs);
        }
    }

    // Method to get Top-K itemsets sorted by utility
    public List<Itemset> getTopKItemsets(int k) {
        return resultItemsets.stream()
                .sorted(Comparator.comparingDouble(Itemset::getUtility).reversed())
                .limit(k)
                .collect(Collectors.toList());
    }

    // Main method to execute the algorithm
    public void runAlgorithm() {
        computeTWUAndRecForSingleItems();

        // Create EUCS (Extended Utility Calculation Structure)
        EUCS eucs = new EUCS(database);

        // Initialize the RSPUL map for single items
        for (String item : externalUtility.keySet()) {
            List<String> singleItemset = Collections.singletonList(item);
            RSPUL rspul = constructRSPUL(singleItemset, eucs);
            if (rspul.getUtilitySum() >= minUtil && rspul.getRecencySum() >= minRec && rspul.getMaxPeriod() <= maxPeriod) {
                resultItemsets.add(new Itemset(singleItemset, rspul.getUtilitySum(), rspul.getRecencySum(), rspul.getMaxPeriod()));
            }
        }

        // Proceed with recursive search for higher-order itemsets
        searchRSPHUIM(new ArrayList<>(), new ArrayList<>(externalUtility.keySet()), eucs);

        // Fetch the top-K itemsets
        List<Itemset> topKItemsets = getTopKItemsets(topK);
        topKItemsets.forEach(System.out::println);
    }

    public static void main(String[] args) {
        // Sample input data: database and external utility values for items
        List<List<String>> database = Arrays.asList(
                Arrays.asList("A", "B", "C"),
                Arrays.asList("B", "C", "D"),
                Arrays.asList("A", "D"),
                Arrays.asList("A", "B", "D")
        );

        Map<String, Double> externalUtility = new HashMap<>();
        externalUtility.put("A", 3.0);
        externalUtility.put("B", 2.5);
        externalUtility.put("C", 4.0);
        externalUtility.put("D", 1.5);

        PeriodicHighUtilityMinin mining = new PeriodicHighUtilityMinin(database, externalUtility);
        mining.runAlgorithm();
    }

    // Inner class to represent the utility and recency information for itemsets
    public static class RSPUL {
        private double utilitySum;
        private double recencySum;
        private int maxPeriod;

        public RSPUL(double utilitySum, double recencySum, int maxPeriod) {
            this.utilitySum = utilitySum;
            this.recencySum = recencySum;
            this.maxPeriod = maxPeriod;
        }

        public double getUtilitySum() {
            return utilitySum;
        }

        public double getRecencySum() {
            return recencySum;
        }

        public int getMaxPeriod() {
            return maxPeriod;
        }
    }
}
