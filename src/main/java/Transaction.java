import java.util.*;

public class Transaction {
    private final String id;
    private final String timestamp;
    private final Map<String, Integer> items;
    private final Map<String, Integer> utilities;

    public Transaction(String id, String timestamp, Map<String, Integer> items, Map<String, Integer> utilities) {
        validateInput(id, timestamp, items, utilities);
        this.id = id;
        this.timestamp = timestamp;
        this.items = Collections.unmodifiableMap(new HashMap<>(items));
        this.utilities = Collections.unmodifiableMap(new HashMap<>(utilities));
    }

    private void validateInput(String id, String timestamp, Map<String, Integer> items, Map<String, Integer> utilities) {
        if (id == null || timestamp == null || items == null || utilities == null) {
            throw new IllegalArgumentException("Transaction fields cannot be null.");
        }
        if (!items.keySet().equals(utilities.keySet())) {
            throw new IllegalArgumentException("Items and utilities mismatch.");
        }
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public Map<String, Integer> getUtilities() {
        return utilities;
    }

    public int computeTransactionUtility() {
        return items.keySet().stream()
                .mapToInt(item -> items.get(item) * utilities.get(item))
                .sum();
    }
}
