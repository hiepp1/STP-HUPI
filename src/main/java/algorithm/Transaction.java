package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    int id;
    List<Integer> items;
    List<Integer> utilities;
    int transactionUtility;
    long timestamp;

    public Transaction(Transaction other) {
        this.id = other.getId();
        this.items = new ArrayList<>(other.getItems());
        this.utilities = new ArrayList<>(other.getUtilities());
        this.transactionUtility = other.getTransactionUtility();
    }
}
