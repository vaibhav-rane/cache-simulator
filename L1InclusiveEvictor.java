import java.util.List;
import java.util.Objects;

/**
 * Created by varane on 10/3/22.
 */
public class L1InclusiveEvictor {
    public void evict(String address, Cache cache){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        CacheBlock inclusiveBlock = CacheManagerUtils.getBlockAt(address, cache);
        if (Objects.nonNull(inclusiveBlock)){

            int removalIndex = -1;
            for (int i = 0; i < set.length; i++){
                CacheBlock blockAtI = set[i];
                if (blockAtI.getTag().equals(inclusiveBlock.getTag())){
                    removalIndex = i;
                    break;
                }
            }
            //CacheBlock evictedInclusiveBlock = set.remove(removalIndex);
            CacheBlock evictedInclusiveBlock = set[removalIndex];
            set[removalIndex] = null;
            if (evictedInclusiveBlock.isDirty()){
                cache.setWriteBackCount(cache.getWriteBackCount() + 1);
            }
        }

    }
}
