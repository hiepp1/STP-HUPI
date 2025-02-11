package algorithm;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The DatasetReader class is responsible for reading a dataset file,
 * parsing transactions, and transforming the transactions into short-time
 * sub-datasets based on the dataset type.
 */
public class DatasetReader {

    /**
     * Reads the dataset from the given file path, parses each transaction,
     * and transforms the transactions into short-time sub-datasets.
     * For the "korasak" dataset, one-hour sub-datasets are produced;
     * for other datasets (e.g., retail, ecommerce, mushroom), weekly sub-datasets are produced.
     *
     * @param filepath the path of the dataset file.
     * @return a list of short-time transaction sub-datasets.
     * @throws IOException if there is an error reading the file.
     */
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
        List<List<Transaction>> shortTimeDatasets;
        if (extractDatasetName(filepath).equals("korasak")) {
            shortTimeDatasets = transformToOneHourTransactions(transactions);
        } else {
            shortTimeDatasets = transformToWeeklyTransactions(transactions);
        }
        System.out.println("Transactions loaded: " + shortTimeDatasets.stream().mapToInt(List::size).sum());
        System.out.println("Short Time Transactions loaded: " + shortTimeDatasets.size());
        return shortTimeDatasets;
    }

    /**
     * Parses a single transaction from a line of text.
     * The expected format is: "item_list: transactionUtility: utility_list: timestamp".
     *
     * @param line          the input line containing transaction data.
     * @param transactionID the unique identifier to assign to this transaction.
     * @return a Transaction object or null if parsing fails.
     */
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

    /**
     * Parses a space-separated string of integers into a list.
     *
     * @param input the input string containing integers.
     * @return a list of integers.
     */
    private static List<Integer> parseIntegerList(String input) {
        String[] tokens = input.trim().split("\\s+");
        List<Integer> integerList = new ArrayList<>();
        for (String token : tokens) {
            integerList.add(Integer.parseInt(token));
        }
        return integerList;
    }

    /**
     * Transforms the list of transactions into weekly sub-datasets.
     * This method is used for retail, ecommerce, and mushroom datasets.
     *
     * @param transactions the complete list of transactions.
     * @return a list of weekly sub-datasets.
     */
    private static List<List<Transaction>> transformToWeeklyTransactions(List<Transaction> transactions) {
        List<List<Transaction>> weeklyTransactions = new ArrayList<>();
        // Process transactions in weekly segments.
        long startTimestamp = transactions.get(0).getTimestamp();

        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp) {
                List<Transaction> weeklyTransaction = extractWeeklyTransactions(transactions, startTimestamp);
                weeklyTransactions.add(weeklyTransaction);
                startTimestamp = alignToEndOfDay(startTimestamp) + 604801; // Move to the next week; 604801 seconds = one week + 1 second to avoid overlap.
            }
        }

        return weeklyTransactions;
    }

    /**
     * Extracts transactions within a weekly window from the list of transactions.
     *
     * @param transactions the complete list of transactions.
     * @param startTimestamp the starting timestamp of the week.
     * @return a list of transactions that fall within the week.
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

    /**
     * Transforms the list of transactions into one-hour sub-datasets.
     * This method is used for the Kosarak (click-stream) dataset.
     * For visualization, only six hourly subsets are extracted.
     *
     * @param transactions the complete list of transactions.
     * @return a list of one-hour sub-datasets.
     */
    private static List<List<Transaction>> transformToOneHourTransactions(List<Transaction> transactions) {
        List<List<Transaction>> oneHourTransList = new ArrayList<>();
        long startTimestamp = transactions.get(0).getTimestamp();
        // Extract only six hourly subsets for clarity.
        for (Transaction transaction : transactions) {
            // For clarity and ease of visualization of performance, we only take six hourly subsets of the 'korasak' dataset.
            if (oneHourTransList.size() == 6) break;
            if (transaction.getTimestamp() >= startTimestamp) {
                List<Transaction> oneHourTrans = extractOneHourTransactions(transactions, startTimestamp);
                oneHourTransList.add(oneHourTrans);
                startTimestamp = startTimestamp + 3601; // Increment start timestamp by one hour + 1 second to avoid overlap.
            }
        }
        return oneHourTransList;
    }

    /**
     * Extracts transactions within a one-hour window.
     *
     * @param transactions the complete list of transactions.
     * @param startTimestamp the starting timestamp of the hour.
     * @return a list of transactions that fall within the hour.
     */
    private static List<Transaction> extractOneHourTransactions(List<Transaction> transactions, long startTimestamp) {
        List<Transaction> result = new ArrayList<>();
        long endTimestamp = startTimestamp + 3600; // 3600 seconds in one hour.
        for (Transaction transaction : transactions) {
            if (transaction.getTimestamp() >= startTimestamp && transaction.getTimestamp() <= endTimestamp) {
                result.add(transaction);
            }
        }

        System.err.println("Extracted " + result.size() + " transactions from "
                + getLocalDateTime(startTimestamp) + " to " + getLocalDateTime(endTimestamp));
        return result;
    }

    /**
     * Aligns the given timestamp to the end of its day (23:59:59).
     *
     * @param timestamp the input timestamp (in seconds).
     * @return the timestamp corresponding to 23:59:59 of the same day.
     */
    private static long alignToEndOfDay(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        LocalDateTime endOfDay = localDateTime.toLocalDate().atTime(23, 59, 59);
        return endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * Converts a timestamp to a formatted local date and time string.
     *
     * @param timestamp the input timestamp (in seconds).
     * @return a formatted date/time string (e.g., "HH:mm:ss dd/MM/yyyy").
     */
    private static String getLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return dateTime.format(formatter);
    }

    /**
     * Extracts the dataset name from the given file path by removing the extension.
     *
     * @param filepath the path of the dataset file.
     * @return the dataset name.
     */
    public static String extractDatasetName(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) return filename.substring(0, dotIndex);
        return filename;
    }
}