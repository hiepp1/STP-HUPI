package algorithm;

public class Main {
    public static void main(String[] args) {
        String filepath1 = "src/main/java/dataset/pumsp.txt";
        String filepath2 = "src/main/java/dataset/mushroom.txt";
        String filepath3 = "src/main/java/dataset/ecommerce.txt"; //too many transactions per week
        String filepath4 = "src/main/java/dataset/retail.txt"; //too many transactions per week

        int k = 20;
        int maxPer = 20;
        TopKPerformanceEvaluator evaluator = new TopKPerformanceEvaluator(filepath1, k, maxPer);
        evaluator.run();
        evaluator.displayResults();
    }
}
