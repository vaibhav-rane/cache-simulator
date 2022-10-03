/**
 * Created by varane on 10/2/22.
 */

import java.util.List;
import java.util.Objects;

/**
 * Uses LRU policy to evict a block corresponding to the supplied address from L1 cache
 * If the evicted block is dirty, issues write-back to L2*/
public class L1LruEvictionProcessor implements EvictionProcessor{
    @Override
    public void evict(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        List<CacheBlock> set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        /***
         * finding index of the block with the least access counter
         */
        int evictionIndex = -1;
        int minAccessCounter = Integer.MAX_VALUE;

        for (int i = 0; i < set.size(); i++){
            CacheBlock block = set.get(i);
            if ( block.getLastAccess() < minAccessCounter){
                minAccessCounter = block.getLastAccess();
                evictionIndex = i;
            }
        }

        CacheBlock evictedBlock = set.remove(evictionIndex);

        if (evictedBlock.isDirty()){
            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
            if(cache.hasNextLevel()){
                issueWriteBackTo(address, cache.getNextLevelCache());
            }
        }
    }

    public void issueWriteBackTo(String address, Cache cache){
        cache.setWriteCount(cache.getWriteCount() + 1);
        if(Objects.isNull(CacheManagerUtils.getBlockAt(address, cache))){
            //block is not present in the set
            cache.setWriteMissCount(cache.getWriteMissCount() + 1);
            CacheBlock block = CacheManagerUtils.createNewCacheBlockFor(cache, address);

            if(CacheManagerUtils.isSetVacantFor(cache, address)){
                CacheManagerUtils.addBlockToCache(cache, block);
            }
            else {

                new EvictionManager()
                        .getEvictionProcessorFor(cache.getReplacementPolicy(), CacheType.L2)
                        .evict(address, cache);
                CacheManagerUtils.addBlockToCache(cache, block);
            }

        }
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
