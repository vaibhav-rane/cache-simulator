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
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        int lruBlockIndex = CacheManagerUtils.getLruBlockIndex(address, cache);

        return lruBlockIndex;
//        CacheBlock evictedBlock = set.remove(lruBlockIndex);
//
//        if (evictedBlock.isDirty()){
//            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
//            if(cache.hasNextLevel()){
//                issueWriteBackTo(evictedBlock.getAddress(), cache.getNextLevelCache());
//            }
//        }
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
