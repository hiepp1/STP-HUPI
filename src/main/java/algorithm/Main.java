package algorithm;

public class Main {
    public static void main(String[] args) {
        // Define dataset file paths
        String filepath1 = "src/main/java/dataset/retail.txt"; // Currently selected dataset
        String filepath2 = "src/main/java/dataset/ecommerce.txt";
        String filepath3 = "src/main/java/dataset/mushroom.txt";
        String filepath4 = "src/main/java/dataset/korasak.txt";

        // Set algorithm parameters
        int k = 50;       // Top-K value (number of high-utility patterns to find)
        int maxPer = 200; // Maximum period constraint for itemsets
        float threshold = 0.001f;

        // Initialize the performance evaluator with the selected dataset
        TopKPerformanceEvaluator evaluator = new TopKPerformanceEvaluator(filepath1, k, maxPer, threshold);

        // Run the evaluation process
        evaluator.run();

        // Display the final results, including runtime and memory usage comparisons
        evaluator.displayResults();
    }
}
