/**
 * Created by varane on 10/1/22.
 */
public class L1PlruEvictionProcessor implements EvictionProcessor{

    @Override
    public int getEvictionIndex(String address, Cache cache) {
        return 1;
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
