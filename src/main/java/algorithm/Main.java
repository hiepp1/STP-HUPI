package algorithm;

import algorithm.STP_HUPI.STPHUPIAlgorithm;
import algorithm.ST_HUPI.STHUPIAlgorithm;
import org.jfree.chart.annotations.TextAnnotation;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String filepath2 = "src/main/java/dataset/negativeDataset/mushroom_negative.txt";
        String filepath3 = "src/main/java/dataset/timestampDataset/foodmart_timestamp.txt";
        String filepath4 = "src/main/java/dataset/timestampDataset/mushroom_timestamp.txt";
        String filepath5 = "src/main/java/dataset/timestampDataset/ecommerce_timestamp.txt";
        String filepath6 = "src/main/java/dataset/timestampDataset/kosarak_timestamp.txt";
        String filepath7 = "src/main/java/dataset/timestampDataset/retail_timestamp.txt";

        try {
            List<List<Transaction>> transactions = DatasetReader.readTimestampDataset(filepath7);
            TopKPerformanceEvaluator evaluator = new TopKPerformanceEvaluator(filepath7, transactions);
            evaluator.run();
            evaluator.displayResults("memory");
        } catch (IOException e) {
            System.err.println("Error reading the dataset: " + e.getMessage());
        }
    }
}
