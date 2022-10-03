/**
 * Created by varane on 10/1/22.
 */
public class L1OptEvictionProcessor implements EvictionProcessor{
    @Override
    public void evict(String address, Cache cache) {

    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
