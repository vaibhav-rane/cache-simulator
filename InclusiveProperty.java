/**
 * Created by varane on 10/1/22.
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
