import java.util.List;

/**
 * Created by varane on 10/2/22.
 */
public class L2LruEvictionProcessor implements EvictionProcessor{
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
        //if the policy is inclusive, check if this block is present in L1. if yes -> evict

        if (evictedBlock.isDirty()){
            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
        }
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L2;
    }
}
