import java.util.List;
import java.util.Objects;

/**
 * Created by varane on 10/3/22.
 */
public class L1InclusiveEvictor {
    public void evict(String address, Cache cache){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        List<CacheBlock> set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        CacheBlock inclusiveBlock = CacheManagerUtils.getBlockAt(address, cache);
        if (Objects.nonNull(inclusiveBlock)){

            int removalIndex = -1;
            for (int i = 0; i < set.size(); i++){
                CacheBlock blockAtI = set.get(i);
                if (blockAtI.getTag().equals(inclusiveBlock.getTag())){
                    removalIndex = i;
                    break;
                }
            }
            CacheBlock evictedInclusiveBlock = set.remove(removalIndex);
            if (evictedInclusiveBlock.isDirty()){
                cache.setWriteBackCount(cache.getWriteBackCount() + 1);
            }
        }

    }
}
