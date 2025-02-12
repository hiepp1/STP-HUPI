package algorithm;

import algorithm.STP_HUI.StpHuiAlgorithm;
import algorithm.STP_HUPI.StpHupiAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.util.*;

/**
 * This class evaluates the performance of two top-K mining algorithms:
 * - STP-HUPI (Short Time Period High Utility Probabilistic Itemsets)
 * - STP-HUI (Short Time Period High Utility Itemsets)
 * It compares them based on runtime and memory consumption.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopKPerformanceEvaluator {
    private List<Double> runTimes1 = new ArrayList<>();
    private List<Double> runTimes2 = new ArrayList<>();

    private List<Double> memories1 = new ArrayList<>();
    private List<Double> memories2 = new ArrayList<>();

    private Map<Integer, Integer> shortTimeTransactions = new HashMap<>();

    private String filePath;
    private List<List<Transaction>> transactions;
    private int k;
    private int maxPer;
    private float threshold;

    /**
     * Constructor to initialize the evaluator with dataset path, top-K value, and max period.
     */
    public TopKPerformanceEvaluator(String filePath, int k, int maxPer, float threshold) {
        this.filePath = filePath;
        this.k = k;
        this.maxPer = maxPer;
        this.threshold = threshold;
    }

    /**
     * Runs the evaluation process by loading the dataset, transforming it into short-time segments,
     * and executing both algorithms on each segment.
     */
    public void run() {
        try {
            this.transactions = DatasetReader.readDataset(this.filePath);
            if (DatasetReader.extractDatasetName(filePath).equals("korasak")) {
                for (int i = 0; i < 6; i++) {
                    System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                    System.out.println("No. of transactions: " + transactions.get(i).size());
                    this.shortTimeTransactions.put(i+1, transactions.get(i).size());
                    if (!transactions.isEmpty()) {
                        this.runStpHUPI(transactions.get(i)); // [1] Short Time Period High Utility Probabilities Itemsets
                        this.runStpHUI(transactions.get(i)); // [2] Short Time Period High Utility Itemsets
                    }
                }
            } else {
                int i = 1;
                for (List<Transaction> transactions : this.transactions) {
                    System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                    System.out.println("No. of transactions: " + transactions.size());
                    this.shortTimeTransactions.put(i, transactions.size());
                    if (!transactions.isEmpty()) {
                        this.runStpHUPI(transactions); // [1] Short Time Period High Utility Probabilities Itemsets
                        this.runStpHUI(transactions); // [2] Short Time Period High Utility Itemsets
                    }
                    i += 1;
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }

    /**
     * Runs the STP-HUPI algorithm on a given transaction list and records its runtime and memory usage.
     */
    private void runStpHUPI(List<Transaction> transactions) {
        System.out.println("\nRunning [1] Short-time Top-" + this.k + " Periodic High-utility Probabilistic Itemsets...");
        StpHupiAlgorithm stpHUPI = new StpHupiAlgorithm(new ArrayList<>(transactions), k, maxPer, threshold);
        stpHUPI.evaluateTopKPerformance();
        this.runTimes1.add(stpHUPI.getRunTime());
        this.memories1.add(stpHUPI.getMemoryUsed());
    }

    /**
     * Runs the STP-HUI algorithm on a given transaction list and records its runtime and memory usage.
     */
    private void runStpHUI(List<Transaction> transactions) {
        System.out.println("\nRunning [2] Short-time Top-" + this.k + " Periodic High-utility Itemsets...");
        StpHuiAlgorithm stpHUI = new StpHuiAlgorithm(new ArrayList<>(transactions), k, maxPer, threshold);
        stpHUI.evaluateTopKPerformance();
        this.runTimes2.add(stpHUI.getRunTime());
        this.memories2.add(stpHUI.getMemoryUsed());
    }

    /**
     * Displays evaluation results by generating comparison charts for:
     * - Short-time transaction distribution
     * - Runtime comparison
     * - Memory usage comparison
     */
    public void displayResults() {

        String datasetTitle = DatasetReader.extractDatasetName(this.filePath);
        this.plotShortTimeTransactionComparisonChart(this.shortTimeTransactions, datasetTitle);
        this.plotRunTimeComparisonChart(datasetTitle);
        this.plotMemoryComparisonChart(datasetTitle);
    }

    /**
     * Plots a bar chart comparing the number of transactions in each short-time dataset segment.
     */
    private void plotShortTimeTransactionComparisonChart(Map<Integer, Integer> shortTimeTransactions, String title) {
        List<Integer> weeks = new ArrayList<>(shortTimeTransactions.keySet());
        List<Integer> transactionCounts = new ArrayList<>(shortTimeTransactions.values());

        String shortTimeWindow;
        if (title.equals("korasak")) {
            shortTimeWindow = "One Hour";
        } else {
            shortTimeWindow = "Week";
        }
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Short Time Transaction Comparison: " + title)
                .xAxisTitle(shortTimeWindow)
                .yAxisTitle("No. of Transactions")
                .build();

//        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Horizontal);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setAvailableSpaceFill(0.3);

        chart.addSeries("Transactions", weeks, transactionCounts);

        new SwingWrapper<>(chart).displayChart();
    }

    /**
     * Plots a grouped bar chart comparing memory usage of STP-HUPI and STP-HUI.
     */
    private void plotMemoryComparisonChart(String title) {
        List<Integer> weeks = new ArrayList<>();
        for (int i = 0; i < this.memories1.size(); i++) {
            weeks.add(i + 1);
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(700)
                .height(500)
                .title(title + ", k = " + this.k + ", maxPer = " + this.maxPer + ", threshold = " + this.threshold)
                .xAxisTitle("Week")
                .yAxisTitle("Memory (MB)")
                .build();

        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Horizontal);
        chart.getStyler().setAvailableSpaceFill(0.3);
        chart.getStyler().setOverlapped(false);

        chart.addSeries("STP-HUPI", weeks, this.memories1);
        chart.addSeries("STP-HUI", weeks, this.memories2);

        new SwingWrapper<>(chart).displayChart();
    }

    /**
     * Plots a grouped bar chart comparing runtime performance of STP-HUPI and STP-HUI.
     */
    private void plotRunTimeComparisonChart(String title) {
        List<Integer> weeks = new ArrayList<>();
        for (int i = 0; i < this.runTimes1.size(); i++) {
            weeks.add(i + 1);
        }

        CategoryChart chart = new CategoryChartBuilder()
                .width(700)
                .height(500)
                .title(title + ", k = " + this.k + ", maxPer = " + this.maxPer + ", threshold = " + this.threshold)
                .xAxisTitle("Week")
                .yAxisTitle("Runtime (Sec.)")
                .build();

        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Horizontal);
        chart.getStyler().setAvailableSpaceFill(0.3);  // Adjust bar width (smaller value => narrower bars)
        chart.getStyler().setOverlapped(false);         // Group bars side by side

        chart.addSeries("STP-HUPI", weeks, this.runTimes1);
        chart.addSeries("STP-HUI", weeks, this.runTimes2);

        new SwingWrapper<>(chart).displayChart();
    }
}
