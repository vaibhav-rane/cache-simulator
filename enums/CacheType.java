package enums;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public enum CacheType {
    L1("level 1 cache"),
    L2("level 2 cache");

    private String description;

    CacheType(String description) {
        this.description = description;
    }

    public String getDescription(){
        return this.description;
    }
}
