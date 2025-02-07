package algorithm;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DatasetReader {
    public static List<List<Transaction>> readTimestampDataset(String filepath) throws IOException {
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

//    public static List<Transaction> readDataset(String filepath) throws IOException {
//        List<Transaction> transactions = new ArrayList<>();
//        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filepath))) {
//            String line;
//            int transactionID = 1;
//
//            while ((line = bufferedReader.readLine()) != null) {
//                Transaction transaction = parseTransaction(line, transactionID++);
//                if (transaction != null) {
//                    transactions.add(transaction);
//                }
//            }
//        }
//        return transactions;
//    }

    private static Transaction parseTransaction(String line, int transactionID) {
        try {
            String[] parts = line.split(":");
            if (parts.length != 4) {
                return null;
            }

            List<Integer> items = parseIntegerList(parts[0]);
            int transactionUtility = Integer.parseInt(parts[1].trim());
            List<Integer> utilities = parseIntegerList(parts[2]);
            long timestamp = Long.parseLong(parts[3].trim());
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

    private static List<Transaction> extractWeeklyTransactions(List<Transaction> transactions, long startTimestamp) {
        List<Transaction> result = new ArrayList<>();
        int secondsInAWeek = 604800; // Seconds in one week
        long endTimestamp = alignToEndOfDay(startTimestamp) + secondsInAWeek;
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp && transaction.getTimestamp() <= endTimestamp) {
                result.add(transaction);
            }
        }

        System.err.println("Extracted " + result.size() + " transactions from " + getLocalDateTime(startTimestamp) + " to " + getLocalDateTime(endTimestamp));
        return result;
    }

    private static long alignToEndOfDay(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        // Set time to 23:59:59 of the same day
        LocalDateTime endOfDay = localDateTime.toLocalDate().atTime(23, 59, 59);

        return endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private static List<List<Transaction>> transformToWeeklyTransactions(List<Transaction> transactions) {
        List<List<Transaction>> weeklyTransactions = new ArrayList<>();
        long startTimestamp = transactions.get(0).getTimestamp();

        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp) {
                List<Transaction> weeklyTransaction = extractWeeklyTransactions(transactions, startTimestamp);
                weeklyTransactions.add(weeklyTransaction);
                startTimestamp = alignToEndOfDay(startTimestamp) + 604801;
            }
        }

        return weeklyTransactions;
    }

    private static String getLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return dateTime.format(formatter);
    }
}