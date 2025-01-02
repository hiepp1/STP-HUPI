package Top_HUIM_PosNeg;

import java.io.*;
import java.util.*;

class RuntimeVisualizer {
    public static void plotRuntime(String filePath, int[] kValues) throws IOException, IOException {
        List<Transaction> transactions = DatasetParser.parseSPMFDataset(filePath);

        System.out.println("Runtime for different k values:");
        for (int k : kValues) {
            long startTime = System.currentTimeMillis();
            TopKHUIM.calculateTopKOptimized(transactions, k);
            long endTime = System.currentTimeMillis();

            System.out.printf("k = %d, Runtime = %.2f seconds\n", k, (endTime - startTime) / 1000.0);
        }
    }
}