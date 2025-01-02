package Top_HUIM_PosNeg;

import java.util.*;

class UtilityCalculator {
    public static Map<Integer, Map<String, Integer>> calculateItemsetUtilities(List<Transaction> transactions, Set<Integer> items) {
        Map<Integer, Map<String, Integer>> itemsetUtilities = new HashMap<>();

        for (int item : items) {
            int posUtility = 0;
            int negUtility = 0;
//            int posFrequency = 0;  // Count of positive occurrences
//            int negFrequency = 0;  // Count of negative occurrences
//            int totalOccurrences = 0;

            for (Transaction t : transactions) {
                if (t.items.contains(item)) {
//                    totalOccurrences++;
                    int idx = t.items.indexOf(item);
                    int utility = t.utilities.get(idx);
                    if (utility > 0) {
                        posUtility += utility;
//                        posFrequency++;
                    } else {
                        negUtility += utility;
//                        negFrequency++;
                    }
                }
            }

            Map<String, Integer> utilities = new HashMap<>();
            utilities.put("posUtility", posUtility);
            utilities.put("negUtility", negUtility);
//            utilities.put("posFrequency", posFrequency);
//            utilities.put("negFrequency", negFrequency);
//            utilities.put("totalOccurrences", totalOccurrences);
            itemsetUtilities.put(item, utilities);
        }

        return itemsetUtilities;
    }

    public static int applyPRIUStrategy(Map<Integer, Map<String, Integer>> itemsetUtilities, int k) {
        List<Integer> priuList = new ArrayList<>();
        for (Map<String, Integer> utilities : itemsetUtilities.values()) {
            priuList.add(utilities.get("posUtility"));
        }
        Collections.sort(priuList, Collections.reverseOrder());
        return priuList.size() >= k ? priuList.get(k - 1) : 0;
    }

    public static Map<List<Integer>, Integer> buildLIUStructure(List<Transaction> transactions, List<Integer> items) {
        Map<List<Integer>, Integer> liuStructure = new HashMap<>();

        for (int i = 0; i < items.size(); i++) {
            for (int j = i; j < items.size(); j++) {
                List<Integer> sequence = Arrays.asList(items.get(i), items.get(j));
                sequence.sort(Integer::compareTo);

                int totalUtility = 0;
                for (Transaction t : transactions) {
                    if (t.items.containsAll(sequence)) {
                        int utility1 = t.utilities.get(t.items.indexOf(sequence.get(0)));
                        int utility2 = t.utilities.get(t.items.indexOf(sequence.get(1)));
                        totalUtility += utility1 + utility2;
                    }
                }

                liuStructure.put(sequence, totalUtility);
            }
        }

        return liuStructure;
    }

    public static int applyPLIUStrategy(Map<List<Integer>, Integer> liuStructure, int k, int minUtil) {
        List<Integer> liuValues = new ArrayList<>(liuStructure.values());
        liuValues.sort(Collections.reverseOrder());
        return liuValues.size() >= k ? Math.max(minUtil, liuValues.get(k - 1)) : minUtil;
    }
}
