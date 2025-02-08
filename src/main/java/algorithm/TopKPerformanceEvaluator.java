package algorithm;

import algorithm.STP_HUI.STPHUIAlgorithm;
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
import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopKPerformanceEvaluator {
    private List<Map<Integer, Double>> stpExecuteRunTimes = new ArrayList<>();
    private List<Map<Integer, Double>> stExecuteRunTimes = new ArrayList<>();
    private List<Map<Integer, Double>> stp3ExecuteRunTimes = new ArrayList<>();
    private List<Map<Integer, Double>> stpExecuteMemory = new ArrayList<>();
    private List<Map<Integer, Double>> stExecuteMemory = new ArrayList<>();
    private List<Map<Integer, Double>> stp3ExecuteMemory = new ArrayList<>();


    private List<String> timeSeries = new ArrayList<>();
    private String filePath;
    private List<List<Transaction>> transactions;
    private int[] kValues;
    private int maxPer;

    public TopKPerformanceEvaluator(String filePath, int[] kValues, int maxPer) {
        this.filePath = filePath;
        this.kValues = kValues;
        this.maxPer = maxPer;
    }

    private void evaluate() {
        try {
            this.transactions = DatasetReader.readDataset(this.filePath); // Read dataset

            int i = 1;
            for (List<Transaction> transactionList : this.transactions) {
                System.out.println("\n-------------------------------------------- Processing transaction list " + i + "--------------------------------------------\n");
                String startTimeDate = getLocalDateTime(transactionList.get(0).getTimestamp());
                String endTimeDate = getLocalDateTime(transactionList.get(transactionList.size() - 1).getTimestamp());
                String title = "From " + startTimeDate + " to " + endTimeDate;

                // Short Time Period High Utility Probabilities Itemsets
                System.out.println("Running STP-HUPI algorithm...");
                STPHUPIAlgorithm stp = new STPHUPIAlgorithm(transactionList, kValues, maxPer);
                stp.evaluateTopKPerformance();
                Map<Integer, Double> stpTime = stp.getRunTimeResults();
                Map<Integer, Double> stpMemory = stp.getMemoryResults();
                stpExecuteRunTimes.add(stpTime);
                stpExecuteMemory.add(stpMemory);

                // Short Time Period High Utility Itemsets
                System.out.println("\nRunning STP-HUI algorithm...");
                STPHUIAlgorithm stp3 = new STPHUIAlgorithm(transactionList, kValues, maxPer);
                stp3.evaluateTopKPerformance();
                Map<Integer, Double> stp3Time = stp3.getRunTimeResults();
                Map<Integer, Double> stp3Memory = stp3.getMemoryResults();
                stp3ExecuteRunTimes.add(stp3Time);
                stp3ExecuteMemory.add(stp3Memory);

                // Short Time High Utility Probabilities Itemsets
                System.out.println("\nRunning ST-HUIP algorithm...");
                STHUPIAlgorithm st = new STHUPIAlgorithm(transactionList, kValues);
                st.evaluateTopKPerformance();
                Map<Integer, Double> stTime = st.getRunTimeResults();
                Map<Integer, Double> stMemory = st.getMemoryResults();
                stExecuteRunTimes.add(stTime);
                stExecuteMemory.add(stMemory);

                timeSeries.add(title);
                i += 1;
            }
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
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
                            this.stp3ExecuteRunTimes.get(i), "STP-HUI",
                            this.stExecuteRunTimes.get(i), "ST-HUPI",
                            title);
                } else if (unit.equals("memory")) {
                    plotMemoryComparisonChart(this.stpExecuteMemory.get(i), "STP-HUPI",
                            this.stp3ExecuteMemory.get(i), "STP-HUI",
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
                .xAxisTitle("K-value").yAxisTitle("Memory (MB)")
                .build();

        chart.addSeries(algorithm1, kValues1, memories1);
        chart.addSeries(algorithm2, kValues2, memories2);
        chart.addSeries(algorithm3, kValues2, memories3);

        new SwingWrapper<>(chart).displayChart();
    }

    private static void plotRunTimeComparisonChart(
            Map<Integer, Double> runtime1, String algorithm1,
            Map<Integer, Double> runtime2, String algorithm2,
            Map<Integer, Double> runtime3, String algorithm3, String title) {

        List<Integer> kValues1 = new ArrayList<>(runtime1.keySet());
        List<Double> runtimes1 = new ArrayList<>(runtime1.values());

        List<Integer> kValues2 = new ArrayList<>(runtime2.keySet());
        List<Double> runtimes2 = new ArrayList<>(runtime2.values());

        List<Integer> kValues3 = new ArrayList<>(runtime3.keySet());
        List<Double> runtimes3 = new ArrayList<>(runtime3.values());

        XYChart chart = new XYChartBuilder()
                .width(800).height(600)
                .title(title)
                .xAxisTitle("K-value").yAxisTitle("Runtime (Sec.)")
                .build();

        chart.addSeries(algorithm1, kValues1, runtimes1);
        chart.addSeries(algorithm2, kValues2, runtimes2);
        chart.addSeries(algorithm3, kValues3, runtimes3);

        new SwingWrapper<>(chart).displayChart();
    }
}
