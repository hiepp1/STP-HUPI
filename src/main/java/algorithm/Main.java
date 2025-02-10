package algorithm;

public class Main {
    public static void main(String[] args) {
        String filepath1 = "src/main/java/dataset/retail.txt";
        String filepath2 = "src/main/java/dataset/ecommerce.txt";
        String filepath3 = "src/main/java/dataset/mushroom.txt";
        String filepath4 = "src/main/java/dataset/korasak.txt";

        int k = 20;
        int maxPer = 150;
        TopKPerformanceEvaluator evaluator = new TopKPerformanceEvaluator(filepath4, k, maxPer);
        evaluator.run();
        evaluator.displayResults();
    }
}
