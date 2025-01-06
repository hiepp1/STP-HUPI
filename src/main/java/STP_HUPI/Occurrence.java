package STP_HUPI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {
    int transactionID;
    float probability;
    int utility;
    float expectedUtility;
}
