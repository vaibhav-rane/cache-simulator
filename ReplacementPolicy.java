/**
 * Created by varane on 10/1/22.
 */
public enum ReplacementPolicy {
    LRU(0, "LRU"),
    PLRU(1, "Pseudo-LRU"),
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
