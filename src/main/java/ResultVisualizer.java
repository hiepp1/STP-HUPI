import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.util.List;

public class ResultVisualizer {

    // Method to plot Line Chart for Utilities
    public static void plotLineChart(List<Itemset> itemsets, String title, String yAxisLabel) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Add data to the dataset
        int index = 1; // Represents the itemset number
        for (Itemset itemset : itemsets) {
            String seriesName = "Itemset " + index;

            System.out.println("Adding Data - Utility: " + itemset.getUtility()
                    + ", Periodicity: " + itemset.getPeriodicity()
                    + ", Confidence: " + itemset.getConfidence());

            dataset.addValue(itemset.getUtility(), "Utility", seriesName);
            dataset.addValue(itemset.getPeriodicity(), "Periodicity", seriesName);
            dataset.addValue(itemset.getConfidence() * 100, "Confidence (%)", seriesName);
            index++;
        }


        // Create the Line Chart
        JFreeChart lineChart = ChartFactory.createLineChart(
                title,               // Chart Title
                "Itemsets",          // X-axis Label
                yAxisLabel,          // Y-axis Label
                dataset,             // Data
                PlotOrientation.VERTICAL,
                true,                // Include Legend
                true,                // Tooltips
                false                // URLs
        );

        // Display the chart in a frame
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(lineChart));
        frame.pack();
        frame.setVisible(true);
    }
}
