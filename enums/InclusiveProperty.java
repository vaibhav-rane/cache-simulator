package enums;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public enum InclusiveProperty {
    NON_INCLUSIVE(0, "non-inclusive"),
    INCLUSIVE(1, "inclusive");

    InclusiveProperty(int code, String description) {
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
