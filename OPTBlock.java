/**
 * @author varane
 * @created 09/27/2022
 */
public class OPTBlock {
    String data;
    int index;

    public OPTBlock(String data, int index) {

        this.data = data;
        this.index = index;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
    }
    public int getIndex() {
        return index;
    }
    public void setIndex(int index) {
        this.index = index;
    }
}
