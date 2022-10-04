import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by varane on 10/4/22.
 */
public class L2OptEvictorProcessor implements EvictionProcessor{
    @Override
    public void evict(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        List<CacheBlock> set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        Map<Integer, Integer> setIndexFutureMap = new HashMap<>();
        List<Integer> neverUsedIndexes = new ArrayList<>();
        for (int i = 0; i < set.size(); i++){
            CacheBlock block = set.get(i);
            int futureDistance = CacheManagerUtils.getMostRecentFutureDistanceOf(block.getAddress());
            if (futureDistance == Integer.MAX_VALUE)
                neverUsedIndexes.add(i);
            setIndexFutureMap.put(i, futureDistance);
        }

        int blockIndexToEvict = 0;

        if (neverUsedIndexes.isEmpty()){
            int farthestDistanceFromPC = 0;

            for (Map.Entry<Integer, Integer> entry : setIndexFutureMap.entrySet()){
                int distance = entry.getValue();
                if (distance > farthestDistanceFromPC){
                    farthestDistanceFromPC = distance;
                    blockIndexToEvict = entry.getKey();
                }
            }
        }
        else {
            blockIndexToEvict = neverUsedIndexes.get(0);
        }

        CacheBlock evictedBlock = set.get(blockIndexToEvict);
        if (evictedBlock.isDirty())
            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L2;
    }
}
