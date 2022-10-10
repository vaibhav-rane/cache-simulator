import java.util.List;

/**
 * Created by varane on 10/2/22.
 */
public class L2LruFifoEvictionProcessor implements EvictionProcessor{
    @Override
    public int getEvictionIndex(String address, Cache cache) {
        int lruBlockIndex = CacheManagerUtils.getLruBlockIndex(address, cache);
        return lruBlockIndex;
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L2;
    }
}
