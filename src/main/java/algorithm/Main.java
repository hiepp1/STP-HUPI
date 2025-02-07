package algorithm;

public class Main {
    public static void main(String[] args) {
        String filepath1 = "src/main/java/dataset/timestampDataset/foodmart_timestamp.txt";
        String filepath2 = "src/main/java/dataset/timestampDataset/mushroom_timestamp.txt";
        String filepath3 = "src/main/java/dataset/timestampDataset/ecommerce_timestamp.txt";
        String filepath4 = "src/main/java/dataset/timestampDataset/kosarak_timestamp.txt";
        String filepath5 = "src/main/java/dataset/timestampDataset/retail_timestamp.txt";

        int[] kValues = {5, 10, 15, 20};
        int maxPer = 100;
        TopKPerformanceEvaluator evaluator = new TopKPerformanceEvaluator(filepath3, kValues, maxPer);
        evaluator.run();
        evaluator.displayResults("runtime");

    }
}
