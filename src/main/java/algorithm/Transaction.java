package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single transaction in the dataset.
 * Each transaction contains a list of purchased items, their corresponding utilities,
 * the total transaction utility, and a timestamp indicating when the transaction occurred.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private int id;
    private List<Integer> items;
    private List<Integer> utilities;
    private int transactionUtility;
    private long timestamp;
}