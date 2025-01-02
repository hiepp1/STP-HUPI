package Top_HUIM_PosNeg;

import java.util.*;

// Class to represent a transaction
class Transaction {
    List<Integer> items;
    List<Integer> utilities;

    public Transaction(List<Integer> items, List<Integer> utilities) {
        this.items = items;
        this.utilities = utilities;
    }
}