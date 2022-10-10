/**
 * Created by varane on 10/2/22.
 */

import java.util.List;

/**
 * Uses LRU policy to evict a block corresponding to the supplied address from L1 cache
 * If the evicted block is dirty, issues write-back to L2*/
public class L1LruFifoEvictionProcessor implements EvictionProcessor{
    @Override
    public int getEvictionIndex(String address, Cache cache) {

        int lruBlockIndex = CacheManagerUtils.getLruBlockIndex(address, cache);

        return lruBlockIndex;
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
