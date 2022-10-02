/**
 * @author Vaibhav R
 * @created 09/25/2022
 */
public class CacheBlock {

    private String data, tag;
    private int lastAccess;
    private int OPTCounter = 0;
    private Boolean dirty = true;

    public CacheBlock() {
    }

    public CacheBlock(String data, String tag, int lastAccess, Boolean dirty) {
        super();
        this.data = data;
        this.tag = tag;
        this.lastAccess = lastAccess;
        this.dirty = dirty;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public int getLastAccess() {
        return lastAccess;
    }
    public void setLastAccess(int lastAccess) {
        this.lastAccess = lastAccess;
    }
    public Boolean isDirty() {
        return dirty;
    }
    public void setDirty(Boolean dirty) {
        this.dirty = dirty;
    }
    public int getOPTCounter() {
        return OPTCounter;
    }
    public void setOPTCounter(int oPTCounter) {
        OPTCounter = oPTCounter;
    }
}
