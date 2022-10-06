import java.util.List;

/**
 * Created by varane on 10/2/22.
 */
public class L2LruFifoEvictionProcessor implements EvictionProcessor{

    private L1LruFifoEvictionProcessor l1LruFifoEvictionProcessor = new L1LruFifoEvictionProcessor();
    @Override
    public int getEvictionIndex(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        int lruBlockIndex = CacheManagerUtils.getLruBlockIndex(address, cache);
        return lruBlockIndex;
//        CacheBlock evictedBlock = set.remove(lruBlockIndex);
//
//
//        if (evictedBlock.isDirty()){
//            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
//        }
//
//        //if the policy is inclusive, check if this block is present in L1. if yes -> evict
//        if (cache.getInclusionProperty().equals(InclusiveProperty.INCLUSIVE)){
//            L1InclusiveEvictor l1InclusiveEvictor = new L1InclusiveEvictor();
//            l1InclusiveEvictor.evict(evictedBlock.getAddress(), cache.getPrevLevelCache());
//        }
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L2;
    }
}
