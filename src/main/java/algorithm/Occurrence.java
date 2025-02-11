package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an occurrence of an itemset in a transaction.
 * This class stores key information about an item's appearance in a transaction,
 * including its transaction ID, probability, utility, and expected utility.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {
    private int transactionID;
    private float probability;
    private int utility;
    private float expectedUtility;
}