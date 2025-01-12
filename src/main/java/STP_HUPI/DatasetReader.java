package STP_HUPI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.*;

public class DatasetReader {
    public static List<Transaction> readDataset(String filepath) throws IOException {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filepath))) {
            String line;
            int transactionID = 1;

            while ((line = bufferedReader.readLine()) != null) {
                Transaction transaction = parseTransaction(line, transactionID++);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        }
        return transactions;
    }

    private static Transaction parseTransaction(String line, int transactionID) {
        try {
            String[] parts = line.split(":");
            if (parts.length != 3) {
                return null;
            }

            List<Integer> items = parseIntegerList(parts[0]);
            int transactionUtility = Integer.parseInt(parts[1].trim());
            List<Integer> utilities = parseIntegerList(parts[2]);
//            int timestamp = Integer.parseInt(parts[3].trim());

            return new Transaction(transactionID, items, utilities, transactionUtility, 0);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error parsing line: " + line);
            return null;
        }
    }

    private static List<Integer> parseIntegerList(String input) {
        String[] tokens = input.trim().split("\\s+");
        List<Integer> integerList = new ArrayList<>();
        for (String token : tokens) {
            integerList.add(Integer.parseInt(token));
        }
        return integerList;
    }
}