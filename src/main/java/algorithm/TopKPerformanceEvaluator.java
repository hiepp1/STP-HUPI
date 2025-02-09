package algorithm;

import algorithm.STP_HUI.StpHuiAlgorithm;
import algorithm.STP_HUPI.StpHupiAlgorithm;
import algorithm.ST_HUPI.StHupiAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopKPerformanceEvaluator {
    private Map<Integer, Double> runTimes1 = new HashMap<>();
    private Map<Integer, Double> runTimes2 = new HashMap<>();
    private Map<Integer, Double> runTimes3 = new HashMap<>();
    private Map<Integer, Double> memories1 = new HashMap<>();
    private Map<Integer, Double> memories2 = new HashMap<>();
    private Map<Integer, Double> memories3 = new HashMap<>();

    private List<String> timeSeries = new ArrayList<>();
    private String filePath;
    private List<List<Transaction>> transactions;
    private int k;
    private int maxPer;

    public TopKPerformanceEvaluator(String filePath, int k, int maxPer) {
        this.filePath = filePath;
        this.k = k;
        this.maxPer = maxPer;
    }

    public void run() {
        try {
            this.transactions = DatasetReader.readDataset(this.filePath); // Read dataset

            int i = 1;
            for (List<Transaction> transactionList : this.transactions) {
                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");

                this.runStpHUPI(transactionList, i); // [1] Short Time Period High Utility Probabilities Itemsets
                this.runStpHUI(transactionList, i); // [2] Short Time Period High Utility Itemsets
                this.runStHUPI(transactionList, i); // [3] Short Time High Utility Probabilities Itemsets

                i += 1;
            }
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }

    private void runStpHUPI(List<Transaction> transactions, int week) {
        System.out.println("Running [1] Short Time Period High Utility Probabilities Itemsets algorithm with k = " + this.k + "...");
        List<Transaction> copyTransactions = new ArrayList<>(transactions);
        StpHupiAlgorithm stpHUPI = new StpHupiAlgorithm(copyTransactions, k, maxPer);
        stpHUPI.evaluateTopKPerformance();
        this.runTimes1.put(week, stpHUPI.getRunTime());
        this.memories1.put(week, stpHUPI.getMemoryUsed());
    }

    private void runStpHUI(List<Transaction> transactions, int week) {
        System.out.println("\nRunning [2] Short Time Period High Utility Itemsets algorithm with k = " + this.k + "...");
        List<Transaction> copyTransactions = new ArrayList<>(transactions);
        StpHuiAlgorithm stpHUI = new StpHuiAlgorithm(copyTransactions, k, maxPer);
        stpHUI.evaluateTopKPerformance();
        this.runTimes2.put(week, stpHUI.getRunTime());
        this.memories2.put(week, stpHUI.getMemoryUsed());
    }

    private void runStHUPI(List<Transaction> transactions, int week) {
        System.out.println("\nRunning [3] Short Time High Utility Probabilities Itemsets algorithm with k = " + this.k + "...");
        List<Transaction> copyTransactions = new ArrayList<>(transactions);
        StHupiAlgorithm stHUPI = new StHupiAlgorithm(copyTransactions, k);
        stHUPI.evaluateTopKPerformance();
        this.runTimes3.put(week, stHUPI.getRunTime());
        this.memories3.put(week, stHUPI.getMemoryUsed());
    }

    public void displayResults() {

        String datasetTitle = extractDatasetTitle(this.filePath);

        plotRunTimeComparisonChart(this.runTimes1, "STP-HUPI",
                        this.runTimes2, "STP-HUI",
                        this.runTimes3, "ST-HUPI",
                        datasetTitle);

        plotMemoryComparisonChart(this.memories1, "STP-HUPI",
                        this.memories2, "STP-HUI",
                        this.memories3, "ST-HUPI",
                        datasetTitle);
    }

    private static String extractDatasetTitle(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.'); // Find last dot for extension
        if (dotIndex > 0) {
            filename = filename.substring(0, dotIndex); // Remove extension
        }
        // Improved title extraction to handle potential directory structures:
        String[] pathParts = filepath.split("[\\\\/]"); // Split by \ or /
        if (pathParts.length > 1) { // If it is inside a directory
            filename = pathParts[pathParts.length - 1]; // Get the last part
            int dotIndex2 = filename.lastIndexOf('_');
            if (dotIndex2 > 0) {
                filename = filename.substring(0, dotIndex2); // Remove extension
            }
        }
        return filename;
    }

    private static void plotMemoryComparisonChart(
            Map<Integer, Double> memory1, String algorithm1,
            Map<Integer, Double> memory2, String algorithm2,
            Map<Integer, Double> memory3, String algorithm3, String title) {

        List<Integer> kValues1 = new ArrayList<>(memory1.keySet());
        List<Double> memories1 = new ArrayList<>(memory1.values());

        List<Integer> kValues2 = new ArrayList<>(memory2.keySet());
        List<Double> memories2 = new ArrayList<>(memory2.values());

        List<Integer> kValues3 = new ArrayList<>(memory3.keySet());
        List<Double> memories3 = new ArrayList<>(memory3.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle("Week").yAxisTitle("Memory (MB)")
                .build();

        chart.addSeries(algorithm1, kValues1, memories1);
        chart.addSeries(algorithm2, kValues2, memories2);
        chart.addSeries(algorithm3, kValues3, memories3);

        new SwingWrapper<>(chart).displayChart();
    }

    private static void plotRunTimeComparisonChart(
            Map<Integer, Double> runtime1, String algorithm1,
            Map<Integer, Double> runtime2, String algorithm2,
            Map<Integer, Double> runtime3, String algorithm3,
            String title) {

        List<Integer> kValues1 = new ArrayList<>(runtime1.keySet());
        List<Double> runtimes1 = new ArrayList<>(runtime1.values());

        List<Integer> kValues2 = new ArrayList<>(runtime2.keySet());
        List<Double> runtimes2 = new ArrayList<>(runtime2.values());

        List<Integer> kValues3 = new ArrayList<>(runtime3.keySet());
        List<Double> runtimes3 = new ArrayList<>(runtime3.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle("Week").yAxisTitle("Runtime (Sec.)")
                .build();

        chart.addSeries(algorithm1, kValues1, runtimes1);
        chart.addSeries(algorithm2, kValues2, runtimes2);
        chart.addSeries(algorithm3, kValues3, runtimes3);

        new SwingWrapper<>(chart).displayChart();
    }
}
