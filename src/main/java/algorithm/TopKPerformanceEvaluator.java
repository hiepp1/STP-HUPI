package algorithm;

import algorithm.STP_HUI.StpHuiAlgorithm;
import algorithm.STP_HUPI.StpHupiAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public TopKPerformanceEvaluator(String filePath, int k, int maxPer) {
        this.filePath = filePath;
        this.k = k;
        this.maxPer = maxPer;
    }

    public void run() {
        try {
            this.transactions = DatasetReader.readDataset(this.filePath); // Read dataset
            for (int i = 0; i < 6; i++) {
                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                System.out.println("No. of transactions: " + transactions.get(i).size());
                this.shortTimeTransactions.put(i+1, transactions.get(i).size());
                if (!transactions.isEmpty()) {
                    this.runStpHUPI(transactions.get(i)); // [1] Short Time Period High Utility Probabilities Itemsets
                    this.runStpHUI(transactions.get(i)); // [2] Short Time Period High Utility Itemsets
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }

    private void runStpHUPI(List<Transaction> transactions) {
        System.out.println("\nRunning [1] Short-time Top-" + this.k + " Periodic High-utility Probabilistic Itemsets...");
        StpHupiAlgorithm stpHUPI = new StpHupiAlgorithm(new ArrayList<>(transactions), k, maxPer);
        stpHUPI.evaluateTopKPerformance();
        this.runTimes1.add(stpHUPI.getRunTime());
        this.memories1.add(stpHUPI.getMemoryUsed());
    }

    private void runStpHUI(List<Transaction> transactions) {
        System.out.println("\nRunning [2] Short-time Top-" + this.k + " Periodic High-utility Itemsets...");
        StpHuiAlgorithm stpHUI = new StpHuiAlgorithm(new ArrayList<>(transactions), k, maxPer);
        stpHUI.evaluateTopKPerformance();
        this.runTimes2.add(stpHUI.getRunTime());
        this.memories2.add(stpHUI.getMemoryUsed());
    }

    public void displayResults() {

        String datasetTitle = extractDatasetName(this.filePath);
        this.plotShortTimeTransactionComparisonChart(this.shortTimeTransactions, datasetTitle);
        this.plotRunTimeComparisonChart(datasetTitle);
        this.plotMemoryComparisonChart(datasetTitle);
    }

    public String extractDatasetName(String filepath) {
        File file = new File(filepath);
        String filename = file.getName();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) return filename.substring(0, dotIndex);
        return filename;
    }

    private void plotShortTimeTransactionComparisonChart(Map<Integer, Integer> shortTimeTransactions, String title) {
        List<Integer> weeks = new ArrayList<>(shortTimeTransactions.keySet());
        List<Integer> transactionCounts = new ArrayList<>(shortTimeTransactions.values());

        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(600)
                .title("Short Time Transaction Comparison: " + title)
                .xAxisTitle("An Hour")
                .yAxisTitle("No. of Transactions")
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setPlotGridVerticalLinesVisible(false);
        chart.getStyler().setAvailableSpaceFill(0.3);

        chart.addSeries("Transactions", weeks, transactionCounts);

        new SwingWrapper<>(chart).displayChart();
    }

    private void plotMemoryComparisonChart(String title) {
        // Create a list of week numbers from 1 to the size of memories1.
        List<Integer> weeks = new ArrayList<>();
        for (int i = 0; i < this.memories1.size(); i++) {
            weeks.add(i + 1);
        }

        // Create a CategoryChart for a bar/column chart.
        CategoryChart chart = new CategoryChartBuilder()
                .width(700)
                .height(500)
                .title(title + ", k = " + this.k + ", maxPer = " + this.maxPer)
                .xAxisTitle("Week")
                .yAxisTitle("Memory (MB)")
                .build();

        // Customize styling.
        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Horizontal);
        chart.getStyler().setAvailableSpaceFill(0.3);  // Adjust bar width (smaller value => narrower bars)
        chart.getStyler().setOverlapped(false);         // Group bars side by side

        // Add series for each algorithm.
        chart.addSeries("STP-HUPI", weeks, this.memories1);
        chart.addSeries("STP-HUI", weeks, this.memories2);

        // Display the chart.
        new SwingWrapper<>(chart).displayChart();
    }

    private void plotRunTimeComparisonChart(String title) {
        // Convert the week keys (integers) to String categories.
        List<Integer> weeks = new ArrayList<>();
        for (int i = 0; i < this.runTimes1.size(); i++) {
            weeks.add(i + 1);
        }

        // (Assume both maps have the same set of week keys. If not, you may need to merge them.)
        CategoryChart chart = new CategoryChartBuilder()
                .width(700)
                .height(500)
                .title(title + ", k = " + this.k + ", maxPer = " + this.maxPer)
                .xAxisTitle("Week")
                .yAxisTitle("Runtime (Sec.)")
                .build();

        // Customize styling.
        chart.getStyler().setLegendPosition(org.knowm.xchart.style.Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(org.knowm.xchart.style.Styler.LegendLayout.Horizontal);
        chart.getStyler().setAvailableSpaceFill(0.3);  // Adjust bar width (smaller value => narrower bars)
        chart.getStyler().setOverlapped(false);         // Group bars side by side

        // Add series for each algorithm.
        chart.addSeries("STP-HUPI", weeks, this.runTimes1);
        chart.addSeries("STP-HUI", weeks, this.runTimes2);

        // Display the chart.
        new SwingWrapper<>(chart).displayChart();
    }
}
