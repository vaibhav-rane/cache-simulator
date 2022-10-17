package enums;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public enum ReplacementPolicy {
    LRU(0, "LRU"),
    FIFO(1, "FIFO"),
    OPT(2, "Optimal");

    ReplacementPolicy(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    private int code;
    private String description;
}
