package STP_HUPI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.*;

public class DatasetReader {
    public static List<List<Transaction>> readDataset(String filepath) throws IOException {
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
        System.out.println("\n--------------------------- Starting to transform transactions to weekly transactions/short time transactions ---------------------------\n");
        List<List<Transaction>> weeklyTransactions = transformToWeeklyTransactions(transactions);
        System.out.println("Transactions loaded: " + weeklyTransactions.stream().mapToInt(List::size).sum());
        System.out.println("Short Time Transactions loaded: " + weeklyTransactions.size());
        return weeklyTransactions;
    }

    private static Transaction parseTransaction(String line, int transactionID) {
        try {
            String[] parts = line.split(":");
            if (parts.length != 4) {
                return null;
            }

            List<Integer> items = parseIntegerList(parts[0]);
            int transactionUtility = Integer.parseInt(parts[1].trim());
            List<Integer> utilities = parseIntegerList(parts[2]);
            int timestamp = Integer.parseInt(parts[3].trim());

            return new Transaction(transactionID, items, utilities, transactionUtility, timestamp);
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

    private static List<Transaction> extractWeeklyTransactions(List<Transaction> transactions, int startTimestamp) {
        List<Transaction> result = new ArrayList<>();
        int secondsInAWeek = 604800; // Seconds in one week
        int endTimestamp = startTimestamp + secondsInAWeek;
        int i = 1;
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp && transaction.getTimestamp() < endTimestamp) {
                result.add(transaction);
            }
        }
        System.err.println("Extracted " + result.size() + " transactions from " + getLocalDate(startTimestamp) + " to " + getLocalDate(endTimestamp));
        return result;
    }

    private static List<List<Transaction>> transformToWeeklyTransactions(List<Transaction> transactions) {
        List<List<Transaction>> weeklyTransactions = new ArrayList<>();
        int startTimestamp = transactions.get(0).getTimestamp();

        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp) {
                List<Transaction> weeklyTransaction = extractWeeklyTransactions(transactions, startTimestamp);
                weeklyTransactions.add(weeklyTransaction);
                startTimestamp = transaction.getTimestamp() + 604800;
            }
        }

        return weeklyTransactions;
    }

    private static LocalDate getLocalDate(int timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId());
        return localDateTime.toLocalDate();
    }
}