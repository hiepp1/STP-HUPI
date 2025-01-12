package STP_HUPI;

import java.util.*;

public class UtilityList {
    public int item;
    public List<Integer> transactionIDs = new ArrayList<>();
    public List<Integer> utilities = new ArrayList<>();
    public List<Integer> remainingUtilities = new ArrayList<>();

    public UtilityList(int item) {
        this.item = item;
    }

    public void addTransaction(int transactionID, int utility, int remainingUtility) {
        this.transactionIDs.add(transactionID);
        this.utilities.add(utility);
        this.remainingUtilities.add(remainingUtility);
    }

    @Override
    public String toString() {
        return "UtilityList{" +
                "item=" + item +
                ", transactionIDs=" + transactionIDs +
                ", utilities=" + utilities +
                ", remainingUtilities=" + remainingUtilities +
                '}';
    }
}
