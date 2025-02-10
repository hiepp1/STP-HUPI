package algorithm;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
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
        System.out.println("\n--------------------------- Starting to transform transactions to short time transactions ---------------------------\n");
        List<List<Transaction>> weeklyTransactions = transformToDailyTransactions(transactions);
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

    /***
     * Splitting Method For Retail Transactions (retail, ecommerce dataset)
     */
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

    /***
     * Splitting Method For Rapid Click-Stream Activity (kosarak dataset)
     */
    private static List<Transaction> extractDailyTransactions(List<Transaction> transactions, long startTimestamp) {
        List<Transaction> result = new ArrayList<>();
        // Align the start timestamp to the end of that same day.
//        long endTimestamp = alignToEndOfDay(startTimestamp);

        long endTimestamp = startTimestamp + 3600; //hour
        // Select transactions within the day.
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp && transaction.getTimestamp() <= endTimestamp) {
                result.add(transaction);
            }
        }

        System.err.println("Extracted " + result.size() + " transactions from "
                + getLocalDateTime(startTimestamp) + " to " + getLocalDateTime(endTimestamp));
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

    private static List<List<Transaction>> transformToDailyTransactions(List<Transaction> transactions) {
        List<List<Transaction>> dailyTransactions = new ArrayList<>();
        long startTimestamp = transactions.get(0).getTimestamp();

        for (Transaction transaction : transactions) {
            if (dailyTransactions.size() == 6) break;
            if (transaction.getTimestamp() >= startTimestamp) {
                List<Transaction> dailyTransaction = extractDailyTransactions(transactions, startTimestamp);
                dailyTransactions.add(dailyTransaction);
                startTimestamp = startTimestamp + 3601;
            }
        }
        return dailyTransactions;
    }

    private static String getLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return dateTime.format(formatter);
    }
}