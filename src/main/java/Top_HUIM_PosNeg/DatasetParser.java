package Top_HUIM_PosNeg;

import java.io.*;
import java.util.*;

class DatasetParser {
    public static List<Transaction> parseSPMFDataset(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(":");
                    List<Integer> items = parseIntegerList(parts[0].split(" "));
                    List<Integer> utilities = parseIntegerList(parts[2].split(" "));
                    transactions.add(new Transaction(items, utilities));
                }
            }
        }
        return transactions;
    }

    private static List<Integer> parseIntegerList(String[] array) {
        List<Integer> list = new ArrayList<>();
        for (String s : array) {
            list.add(Integer.parseInt(s));
        }
        return list;
    }
}