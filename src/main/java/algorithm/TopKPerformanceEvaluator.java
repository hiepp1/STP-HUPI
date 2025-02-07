package algorithm;

import algorithm.STP_HUPI.STPHUPIAlgorithm;
import algorithm.ST_HUPI.STHUPIAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopKPerformanceEvaluator {
    private List<Map<Integer, Double>> stpExecuteRunTimes = new ArrayList<>();
    private List<Map<Integer, Double>> stExecuteRunTimes = new ArrayList<>();
    private List<Map<Integer, Double>> stpExecuteMemory = new ArrayList<>();
    private List<Map<Integer, Double>> stExecuteMemory = new ArrayList<>();
    private List<String> timeSeries = new ArrayList<>();
    private String filePath;
    private List<List<Transaction>> transactions;

    public TopKPerformanceEvaluator(String filePath, List<List<Transaction>> transactions) {
        this.filePath = filePath;
        this.transactions = transactions;
    }

    private void evaluate() {

        int i = 1;
        for (List<Transaction> transactionList : this.transactions) {
            System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
            String startTimeDate = getLocalDateTime(transactionList.get(0).getTimestamp());
            String endTimeDate = getLocalDateTime(transactionList.get(transactionList.size() - 1).getTimestamp());
            String title = "From " + startTimeDate + " to " + endTimeDate;

            // Short Time Period High Utility Probabilities Itemsets
            System.out.println("Running STP-HUPI algorithm...");
            STPHUPIAlgorithm stp = new STPHUPIAlgorithm(transactionList, 300);
            stp.evaluateTopKPerformance();
            Map<Integer, Double> stpTime = stp.getRunTimeResults();
            Map<Integer, Double> stpMemory = stp.getMemoryResults();
            stpExecuteRunTimes.add(stpTime);
            stpExecuteMemory.add(stpMemory);

            // Short Time High Utility Probabilities Itemsets
            System.out.println("\nRunning ST-HUPI algorithm...");
            STHUPIAlgorithm st = new STHUPIAlgorithm(transactionList);
            st.evaluateTopKPerformance();
            Map<Integer, Double> stTime = st.getRunTimeResults();
            Map<Integer, Double> stMemory = st.getMemoryResults();
            stExecuteRunTimes.add(stTime);
            stExecuteMemory.add(stMemory);

            timeSeries.add(title);
            i += 1;
        }
    }

    public void displayResults(String unit) {
        try {
            String datasetTitle = extractDatasetTitle(this.filePath);
            int i = 0;

            for (String time : timeSeries) {
                String title = "Short Time Dataset 1: " + datasetTitle + " - " + time;

                if (unit.equals("runtime")) {
                    plotRunTimeComparisonChart(this.stpExecuteRunTimes.get(i), "STP-HUPI",
                            this.stExecuteRunTimes.get(i), "ST-HUPI",
                            title);
                } else if (unit.equals("memory")) {
                    plotMemoryComparisonChart(this.stpExecuteMemory.get(i), "STP-HUPI",
                            this.stExecuteMemory.get(i), "ST-HUPI",
                            title);
                } else {
                    System.out.println("Unrecognized unit: " + unit);
                    return;
                }
                i += 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        this.evaluate();
    }

    private static String getLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, TimeZone.getDefault().toZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        return dateTime.format(formatter);
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
            Map<Integer, Double> memory2, String algorithm2, String title) {

        List<Integer> kValues1 = new ArrayList<>(memory1.keySet());
        List<Double> memories1 = new ArrayList<>(memory1.values());

        List<Integer> kValues2 = new ArrayList<>(memory2.keySet());
        List<Double> memories2 = new ArrayList<>(memory2.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle("K-value").yAxisTitle("Memory (MB)")
                .build();

        chart.addSeries(algorithm1, kValues1, memories1);
        chart.addSeries(algorithm2, kValues2, memories2);

        new SwingWrapper<>(chart).displayChart();
    }

    private static void plotRunTimeComparisonChart(
            Map<Integer, Double> runtime1, String algorithm1,
            Map<Integer, Double> runtime2, String algorithm2, String title) {

        List<Integer> kValues1 = new ArrayList<>(runtime1.keySet());
        List<Double> runtimes1 = new ArrayList<>(runtime1.values());

        List<Integer> kValues2 = new ArrayList<>(runtime2.keySet());
        List<Double> runtimes2 = new ArrayList<>(runtime2.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle("K-value").yAxisTitle("Runtime (ms)")
                .build();

        chart.addSeries(algorithm1, kValues1, runtimes1);
        chart.addSeries(algorithm2, kValues2, runtimes2);

        new SwingWrapper<>(chart).displayChart();
    }
}
