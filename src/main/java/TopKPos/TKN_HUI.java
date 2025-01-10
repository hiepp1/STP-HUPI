package TopKPos;

import java.io.*;
import java.util.*;

public class TKN_HUI {

    // ---------------- Initial Class ---------------- //
    static class Transaction {
        int id;
        List<Integer> items;
        List<Integer> utilities;
        int transactionUtility;

        public Transaction(int id, List<Integer> items, List<Integer> utilities, int transactionUtility) {
            this.id = id;
            this.items = items;
            this.utilities = utilities;
            this.transactionUtility = transactionUtility;
        }
    }

    // Class to store item utility data
    static class ItemData {
        int itemId;
        int twu; // Transaction Weighted Utility
        int ptwu; // Positive Transaction Weighted Utility

        public ItemData(int itemId, int twu) {
            this.itemId = itemId;
            this.twu = twu;
            this.ptwu = 0; // Initialize with 0
        }
    }

    // Class to represent an itemset with its utility
    static class Itemset implements Comparable<Itemset> {
        List<Integer> items;
        int utility;

        public Itemset(List<Integer> items, int utility) {
            this.items = items;
            this.utility = utility;
        }

        @Override
        public int compareTo(Itemset other) {
            return Integer.compare(this.utility, other.utility);
        }

        @Override
        public String toString() {
            return "Itemset: " + items + ", Utility: " + utility;
        }
    }
    // ---------------- Initial Class ---------------- //


    // ---------------- Pruning Strategies ---------------- //
    public static boolean pruneByPSU(int subTreeUtility, int minUtil) {
        return subTreeUtility < minUtil; // Prune if subtree utility is less than minUtil
    }

    public static boolean pruneByPLU(int localUtility, int minUtil) {
        return localUtility < minUtil; // Prune if local utility is less than minUtil
    }

    public static boolean earlyPruning(int currentUtility, int minUtil) {
        return currentUtility < minUtil; // Prune if current utility is less than minUtil
    }

    public static boolean earlyAbandoning(int currentUtility, int remainingUtility, int minUtil) {
        return currentUtility + remainingUtility <= minUtil; // Prune if combined utility cannot exceed minUtil
    }

    private static Set<Integer> prunedItems = new HashSet<>();

    // ---------------- DFS Mining with Dynamic Pruning ---------------- //
    public static void performDFSWithPruning(List<Transaction> transactions, List<Integer> prefix, int minUtil, Map<Integer, ItemData> itemDataMap) {
        for (ItemData itemData : itemDataMap.values()) {
            int item = itemData.itemId;

            // Skip if the item is already pruned
            if (prunedItems.contains(item)) {
                continue;
            }

            int itemUtility = itemData.ptwu; // Use Ptwu as utility for this example

            // Evaluate PSU
            if (pruneByPSU(itemUtility, minUtil)) {
                System.out.println("PSU Pruned: Item " + item + ", Utility: " + itemUtility);
                prunedItems.add(item); // Mark item as pruned
                continue;
            }

            // Evaluate PLU
            if (pruneByPLU(itemUtility, minUtil)) {
                System.out.println("PLU Pruned: Item " + item + ", Utility: " + itemUtility);
                prunedItems.add(item); // Mark item as pruned
                continue;
            }

            // Calculate current utility
            int currentUtility = calculateItemUtility(transactions, item);

            // Evaluate EP
            if (earlyPruning(currentUtility, minUtil)) {
                System.out.println("EP Pruned: Item " + item + ", Current Utility: " + currentUtility);
                prunedItems.add(item); // Mark item as pruned
                continue;
            }

            // Calculate remaining utility
            int remainingUtility = calculateRemainingUtility(transactions, item);

            // Evaluate EA
            if (earlyAbandoning(currentUtility, remainingUtility, minUtil)) {
                System.out.println("EA Pruned: Item " + item + ", Combined Utility: " + (currentUtility + remainingUtility));
                prunedItems.add(item); // Mark item as pruned
                continue;
            }

            // Pass all pruning checks
            System.out.println("Item " + item + " is part of a promising itemset.");
        }
    }


    // ---------------- Utility Calculations ---------------- //
    private static int calculateItemUtility(List<Transaction> transactions, int item) {
        int utility = 0;
        for (Transaction transaction : transactions) {
            int index = transaction.items.indexOf(item);
            if (index != -1) {
                utility += transaction.utilities.get(index);
            }
        }
        return utility;
    }

    private static int calculateRemainingUtility(List<Transaction> transactions, int item) {
        int remainingUtility = 0;
        for (Transaction transaction : transactions) {
            for (int i = 0; i < transaction.items.size(); i++) {
                if (transaction.items.get(i) > item) {
                    remainingUtility += transaction.utilities.get(i);
                }
            }
        }
        return remainingUtility;
    }



    // ---------------- Parse Dataset ---------------- //
    public static List<Transaction> parseDataset(String filename) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");

            // Parse items
            String[] itemStrings = parts[0].split(" ");
            List<Integer> items = new ArrayList<>();
            for (String item : itemStrings) {
                items.add(Integer.parseInt(item));
            }

            // Parse transaction utility
            int transactionUtility = Integer.parseInt(parts[1]);

            // Parse item utilities
            String[] utilityStrings = parts[2].split(" ");
            List<Integer> utilities = new ArrayList<>();
            for (String utility : utilityStrings) {
                utilities.add(Integer.parseInt(utility));
            }

            // Add transaction
            transactions.add(new Transaction(transactions.size() + 1, items, utilities, transactionUtility));
        }
        reader.close();
        return transactions;
    }

    // ---------------- TWU, PTWU Calculation  ---------------- //

    public static Map<Integer, ItemData> computeTWU(List<Transaction> transactions) {
        Map<Integer, ItemData> itemDataMap = new HashMap<>();

        for (Transaction transaction : transactions) {
            for (int item : transaction.items) {
                itemDataMap.putIfAbsent(item, new ItemData(item, 0));
                itemDataMap.get(item).twu += transaction.transactionUtility;
            }
        }
        return itemDataMap;
    }

    // Method to compute Positive Transaction Weighted Utility (Ptwu) for each item
    public static void computePtwu(List<Transaction> transactions, Map<Integer, ItemData> itemDataMap) {
        for (Transaction transaction : transactions) {
            for (int i = 0; i < transaction.items.size(); i++) {
                int item = transaction.items.get(i);
                int utility = transaction.utilities.get(i);
                // Only consider positive utilities for Ptwu
                if (utility > 0) {
                    itemDataMap.get(item).ptwu += utility;
                }
            }
        }
    }

    // ---------------- minUtil Calculation ---------------- //
    public static int applyPRIUStrategy(Map<Integer, ItemData> itemDataMap, int k) {
        // Create a list of PRIU values (Ptwu values)
        List<Integer> priuList = new ArrayList<>();
        for (ItemData data : itemDataMap.values()) {
            if (data.ptwu > 0) { // Only consider positive Ptwu values
                priuList.add(data.ptwu);
            }
        }
//        System.out.println("PRIU List before sorting: " + priuList);

        // Sort the list in descending order
        priuList.sort(Collections.reverseOrder());

        System.out.println("PRIU List after sorting: " + priuList);

        // Determine the kth largest value
        int minUtil = (priuList.size() >= k) ? priuList.get(k - 1) : 0;
        return minUtil;
    }
//
//    // Method to build the Leaf Itemset Utility (LIU) Structure
//    public static Map<String, Integer> buildLIUStructure(List<Transaction> transactions, List<Integer> positiveItems) {
//        Map<String, Integer> liuStructure = new HashMap<>();
//
//        // Iterate through each transaction
//        for (Transaction transaction : transactions) {
//            // Filter only positive items from the transaction
//            List<Integer> filteredItems = new ArrayList<>();
//            for (int i = 0; i < transaction.items.size(); i++) {
//                if (positiveItems.contains(transaction.items.get(i))) {
//                    filteredItems.add(transaction.items.get(i));
//                }
//            }
//
//            // Generate all contiguous ordered subsets
//            int n = filteredItems.size();
//            for (int i = 0; i < n; i++) {
//                int utilitySum = 0;
//                StringBuilder sequenceKey = new StringBuilder();
//
//                for (int j = i; j < n; j++) {
//                    int item = filteredItems.get(j);
//                    utilitySum += transaction.utilities.get(transaction.items.indexOf(item));
//                    sequenceKey.append(item).append(" ");
//
//                    // Store utility of the sequence in LIU structure
//                    String key = sequenceKey.toString().trim();
//                    liuStructure.put(key, liuStructure.getOrDefault(key, 0) + utilitySum);
//                }
//            }
//        }
//        return liuStructure;
//    }
//
//    // PLIUE Strategy: Positive LIU-Exact Strategy
//    public static int applyPLIUEStrategy(Map<String, Integer> liuStructure, int minUtil, int k) {
//        PriorityQueue<Integer> topKQueue = new PriorityQueue<>(k);
//
//        // Traverse LIU structure
//        for (int utility : liuStructure.values()) {
//            if (utility >= minUtil) {
//                topKQueue.offer(utility);
//                if (topKQueue.size() > k) {
//                    topKQueue.poll();
//                }
//            }
//        }
//
//        // Update minUtil to the kth largest value
//        if (topKQueue.size() == k) {
//            minUtil = topKQueue.peek();
//        }
//
//        System.out.println("Final top-k utilities in PLIUE: " + topKQueue);
//        return minUtil;
//    }
//
//    // PLIULB Strategy: Positive LIU-Lower Bound Strategy
//    public static int applyPLIULBStrategy(List<Transaction> transactions, Map<String, Integer> liuStructure, int minUtil, int k) {
//        PriorityQueue<Integer> topKQueue = new PriorityQueue<>(k);
//
//        for (Map.Entry<String, Integer> entry : liuStructure.entrySet()) {
//            String sequence = entry.getKey();
//            int originalUtility = entry.getValue();
//            String[] items = sequence.split(" ");
//            List<Integer> itemList = new ArrayList<>();
//
//            for (String item : items) {
//                itemList.add(Integer.parseInt(item));
//            }
//
//            // Generate subsets by removing intermediate items (max 4 items)
//            for (int i = 0; i < itemList.size(); i++) {
//                for (int j = i + 1; j <= Math.min(i + 4, itemList.size() - 1); j++) {
//                    List<Integer> subsetItems = new ArrayList<>(itemList);
//                    subsetItems.subList(i, j).clear();
//
//                    int subsetUtility = calculateSubsetUtility(transactions, subsetItems);
//
//                    if (subsetUtility >= minUtil) {
//                        topKQueue.offer(subsetUtility);
//                        if (topKQueue.size() > k) {
//                            topKQueue.poll();
//                        }
//                    }
//                }
//            }
//        }
//
//        if (topKQueue.size() == k) {
//            minUtil = topKQueue.peek();
//        }
//
//        System.out.println("Final top-k utilities in PLIULB: " + topKQueue);
//        return minUtil;
//    }
//
//    // Helper: Calculate subset utility
//    private static int calculateSubsetUtility(List<Transaction> transactions, List<Integer> subsetItems) {
//        int subsetUtility = 0;
//
//        for (Transaction transaction : transactions) {
//            if (transaction.items.containsAll(subsetItems)) {
//                int transactionSubsetUtility = 0;
//                for (int item : subsetItems) {
//                    int index = transaction.items.indexOf(item);
//                    transactionSubsetUtility += transaction.utilities.get(index);
//                }
//                subsetUtility += transactionSubsetUtility;
//            }
//        }
//
//        return subsetUtility;
//    }

//     Method to build the Leaf Itemset Utility (LIU) Structure
    public static Map<String, Integer> buildLIUStructure(List<Transaction> transactions, List<Integer> positiveItems) {
        Map<String, Integer> liuStructure = new HashMap<>();

        // Iterate through each transaction
        for (Transaction transaction : transactions) {
            // Filter only positive items from the transaction
            List<Integer> filteredItems = new ArrayList<>();
            for (int i = 0; i < transaction.items.size(); i++) {
                if (positiveItems.contains(transaction.items.get(i))) {
                    filteredItems.add(transaction.items.get(i));
                }
            }

            // Generate all possible subsets of the filtered items
            int n = filteredItems.size();
            for (int subsetMask = 1; subsetMask < (1 << n); subsetMask++) { // Exclude empty subset
                StringBuilder subsetKey = new StringBuilder();
                int subsetUtility = 0;

                // Construct subset and calculate its utility
                for (int j = 0; j < n; j++) {
                    if ((subsetMask & (1 << j)) != 0) { // Check if j-th item is in the subset
                        int item = filteredItems.get(j);
                        subsetKey.append(item).append(" ");
                        subsetUtility += transaction.utilities.get(transaction.items.indexOf(item));
                    }
                }

                // Add subset to LIU structure
                String key = subsetKey.toString().trim();
                liuStructure.put(key, liuStructure.getOrDefault(key, 0) + subsetUtility);
            }
            System.out.println(liuStructure.toString());
        }

        return liuStructure;
    }


    // Method to apply LIU-Exact (LIU-E) Strategy
    public static int applyLIUExactStrategy(Map<String, Integer> liuStructure, int minUtil, int k) {
        // Priority queue to keep the top-k utilities
        PriorityQueue<Integer> topKQueue = new PriorityQueue<>(k);

        System.out.println("Starting LIU-E Strategy...");

        for (int utility : liuStructure.values()) {
            System.out.println("Processing Utility: " + utility);

            if (utility >= minUtil) {
                topKQueue.offer(utility);
                if (topKQueue.size() > k) {
                    topKQueue.poll();
                }
            }

//            System.out.println("Current top-k utilities: " + topKQueue);
        }

        // Update minUtil to the kth largest value
        if (topKQueue.size() == k) {
            minUtil = topKQueue.peek();
        }

        System.out.println("Final top-k utilities in LIU-E: " + topKQueue);
        return minUtil;
    }


    // Method to apply LIU-Lower Bound (LIU-LB) Strategy
    public static int applyLIULowerBoundStrategy(List<Transaction> transactions,
                                                 Map<String, Integer> liuStructure,
                                                 int minUtil, int k) {
        // Priority queue to store top-k utilities
        PriorityQueue<Integer> topKQueue = new PriorityQueue<>(k);

//        System.out.println("Starting LIU-LB Strategy...");

        for (Map.Entry<String, Integer> entry : liuStructure.entrySet()) {
            String sequence = entry.getKey();
            int originalUtility = entry.getValue();
            String[] items = sequence.split(" ");
            List<Integer> itemList = new ArrayList<>();

            for (String item : items) {
                itemList.add(Integer.parseInt(item));
            }

            // Generate subsets by removing 1 to 4 intermediate items
            for (int i = 0; i < itemList.size(); i++) {
                for (int j = i + 1; j <= Math.min(i + 4, itemList.size() - 1); j++) {
                    // Create a subset
                    List<Integer> subsetItems = new ArrayList<>(itemList);
                    subsetItems.subList(i, j).clear();

                    // Calculate utility of the subset by re-scanning transactions
                    int subsetUtility = calculateSubsetUtility(transactions, subsetItems);

                    // Debug: Log subset and its utility
//                    System.out.println("Subset: " + subsetItems + ", Subset Utility: " + subsetUtility);

                    // Update top-k utilities if subsetUtility >= minUtil
                    if (subsetUtility >= minUtil) {
                        topKQueue.offer(subsetUtility);
                        if (topKQueue.size() > k) {
                            topKQueue.poll();
                        }
                    }
                }
            }
        }

        // Update minUtil to the kth largest value
        if (topKQueue.size() == k) {
            minUtil = topKQueue.peek();
        }

        System.out.println("Final top-k utilities in LIU-LB: " + topKQueue);
        return minUtil;
    }

    // Method to calculate the utility of a subset by re-scanning transactions
    private static int calculateSubsetUtility(List<Transaction> transactions, List<Integer> subsetItems) {
        int subsetUtility = 0;

        for (Transaction transaction : transactions) {
            if (transaction.items.containsAll(subsetItems)) {
                int transactionSubsetUtility = 0;
                for (int item : subsetItems) {
                    int index = transaction.items.indexOf(item);
                    transactionSubsetUtility += transaction.utilities.get(index);
                }
                subsetUtility += transactionSubsetUtility;
            }
        }

        return subsetUtility;
    }
    // ---------------- minUtil Calculation ---------------- //

    // ---------------- DFS Mining ---------------- //
//    public static int computeUtilityEfficiently(List<Transaction> transactions, List<Integer> itemset) {
//        int totalUtility = 0;
//
//        // Iterate over transactions
//        for (Transaction transaction : transactions) {
//            // Check if transaction contains the itemset
//            if (transaction.items.containsAll(itemset)) {
//                int itemsetUtility = 0;
//
//                // Calculate utility for the itemset in this transaction
//                for (int item : itemset) {
//                    int index = transaction.items.indexOf(item);
//                    if (index != -1) {
//                        itemsetUtility += transaction.utilities.get(index);
//                    }
//                }
//
//                totalUtility += itemsetUtility;
//            }
//        }
//
//        return totalUtility;
//    }
//
//    // Recursive DFS method
//    public static void depthFirstSearch(
//            List<Transaction> transactions,
//            List<Integer> prefix,
//            int minUtil,
//            Map<Integer, ItemData> itemDataMap,
//            PriorityQueue<Itemset> topKQueue,
//            int k
//    ) {
//        for (ItemData itemData : itemDataMap.values()) {
//            int item = itemData.itemId;
//
//            // Extend the prefix with the current item
//            List<Integer> extendedPrefix = new ArrayList<>(prefix);
//            extendedPrefix.add(item);
//
//            // Compute utility of the extended prefix
//            int currentUtility = computeUtilityEfficiently(transactions, extendedPrefix);
//
//            // Pruning Strategies
//            if (earlyPruning(currentUtility, minUtil)) {
//                System.out.println("EP Pruned: Itemset " + extendedPrefix + ", Utility: " + currentUtility);
//                continue;
//            }
//
//            int remainingUtility = calculateRemainingUtility(transactions, item);
//
//            if (earlyAbandoning(currentUtility, remainingUtility, minUtil)) {
//                System.out.println("EA Pruned: Itemset " + extendedPrefix + ", Combined Utility: "
//                        + (currentUtility + remainingUtility));
//                continue;
//            }
//
//            // Update the top-k queue
//            if (currentUtility >= minUtil) {
//                topKQueue.offer(new Itemset(extendedPrefix, currentUtility));
//                if (topKQueue.size() > k) {
//                    topKQueue.poll();
//                }
//                minUtil = (topKQueue.size() == k) ? topKQueue.peek().utility : minUtil;
//            }
//
//            // Recursive call to explore further extensions
//            Map<Integer, ItemData> reducedItemDataMap = filterRemainingItems(itemDataMap, item);
//            depthFirstSearch(transactions, extendedPrefix, minUtil, reducedItemDataMap, topKQueue, k);
//        }
//    }
//
//    private static Map<Integer, ItemData> filterRemainingItems(Map<Integer, ItemData> itemDataMap, int currentItem) {
//        Map<Integer, ItemData> filteredMap = new HashMap<>();
//        for (Map.Entry<Integer, ItemData> entry : itemDataMap.entrySet()) {
//            if (entry.getKey() > currentItem) { // Retain only items greater than the current item
//                filteredMap.put(entry.getKey(), entry.getValue());
//            }
//        }
//        return filteredMap;
//    }
    // ---------------- DFS Mining ---------------- //

    public static void main(String[] args) {
        try {
            // Parse dataset
            String filename = "src/main/java/dataset/test.txt";
            List<Transaction> transactions = parseDataset(filename);

            // Compute TWU and Ptwu
            Map<Integer, ItemData> itemDataMap = computeTWU(transactions);
            computePtwu(transactions, itemDataMap);

            System.out.println("TWU and Ptwu Values:");
            for (ItemData data : itemDataMap.values()) {
                System.out.println("Item: " + data.itemId + ", TWU: " + data.twu + ", Ptwu: " + data.ptwu);
            }

            // Apply PRIU Strategy
            int k = 3;
            int minUtil = applyPRIUStrategy(itemDataMap, k);
            System.out.println("Initial minUtil after PRIU Strategy: " + minUtil);

//            PriorityQueue<Itemset> topKQueue = new PriorityQueue<>(k);
//            depthFirstSearch(transactions, new ArrayList<>(), minUtil, itemDataMap, topKQueue, k);

            // Apply LIU-E Strategy
            List<Integer> positiveItems = new ArrayList<>();
            for (ItemData data : itemDataMap.values()) {
                if (data.ptwu > 0) positiveItems.add(data.itemId);
            }
            Map<String, Integer> liuStructure = buildLIUStructure(transactions, positiveItems);
            minUtil = applyLIUExactStrategy(liuStructure, minUtil, k);
//            minUtil = applyPLIUEStrategy(liuStructure, minUtil, k);
            System.out.println("Minimum Utility after LIU-E Strategy: " + minUtil);

            // Apply LIU-LB Strategy
            minUtil = applyLIULowerBoundStrategy(transactions, liuStructure, minUtil, k);
//            minUtil = applyPLIULBStrategy(transactions,liuStructure,minUtil,k);
            System.out.println("Minimum Utility after LIU-LB Strategy: " + minUtil);

//            System.out.println("Top-k High Utility Itemsets:");
//            while (!topKQueue.isEmpty()) {
//                System.out.println(topKQueue.poll());
//            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
