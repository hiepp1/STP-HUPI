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
    private int id;
    private List<Integer> items;
    private List<Integer> utilities;
    private int transactionUtility;
    private long timestamp;
}
