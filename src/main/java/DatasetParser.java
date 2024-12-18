import java.io.*;
import java.util.*;

public class DatasetParser {
    public static List<Transaction> parseDataset(String filePath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int transactionID = 1;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            String[] items = parts[0].trim().split(" ");
            int totalUtility = Integer.parseInt(parts[1].trim());
            String[] itemUtilities = parts[2].trim().split(" ");

            Map<String, Integer> itemMap = new LinkedHashMap<>();
            Map<String, Integer> utilityMap = new LinkedHashMap<>();

            for (int i = 0; i < items.length; i++) {
                String item = "I" + items[i];
                int utility = Integer.parseInt(itemUtilities[i]);
                itemMap.put(item, 1);
                utilityMap.put(item, utility);
            }

            transactions.add(new Transaction("T" + transactionID, "2024-06-01", itemMap, utilityMap));
            transactionID++;
        }
        reader.close();
        return transactions;
    }
}
