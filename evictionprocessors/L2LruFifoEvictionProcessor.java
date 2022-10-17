package evictionprocessors;

import core.Cache;
import enums.CacheType;
import utils.CacheManagerUtils;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 * Handles eviction in L2 Cache using LRU/FIFO Replacement Policy
 */
public class L2LruFifoEvictionProcessor implements EvictionProcessor {
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
