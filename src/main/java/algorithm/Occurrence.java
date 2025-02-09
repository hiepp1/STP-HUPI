package algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {
    private int transactionID;
    private float probability;
    private int utility;
    private float expectedUtility;
}
