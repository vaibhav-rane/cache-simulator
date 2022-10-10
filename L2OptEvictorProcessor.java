import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by varane on 10/4/22.
 */
public class L2OptEvictorProcessor implements EvictionProcessor{
    @Override
    public int getEvictionIndex(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        Map<Integer, Integer> blockIndexFutureDistanceMap = new HashMap<>();

        for (int i = 0; i < set.length; i++){
            CacheBlock block = set[i];
            String blockAddress = block.getAddress();

            /**
             * Getting all the future occurrences of this block*/
            List<Integer> futureAccesses = Constants.addressOccurrenceMap.get(blockAddress);

            /**
             * If there are no future occurrences, this block will never be needed -> ideal for eviction.*/
            if (Objects.isNull(futureAccesses) || futureAccesses.isEmpty()){
                return i;
            }

            /**
             * Removing the past accesses*/
            futureAccesses.removeIf(futureAccess -> futureAccess <= Constants.programCounter);

            /**
             * If there are no future occurrences, this block will never be needed -> ideal for eviction.*/
            if (Objects.isNull(futureAccesses) || futureAccesses.isEmpty()){
                return i;
            }
            else{
                int nearestFutureAccess = futureAccesses.get(0);

//                for (Integer futureOccurrence : futureAccesses){
//                    /**
//                     * We are only interested in the first future occurrence after the current PC*/
//                    if (futureOccurrence < Constants.programCounter)
//                        continue;
//                    else{
//                        occurrence = futureOccurrence;
//                        break;
//                    }
//                }
                blockIndexFutureDistanceMap.put(i, nearestFutureAccess);
            }
        }

        int farthestBlockDistance = Integer.MIN_VALUE;
        int evictionIndex = -1;
        for (Map.Entry<Integer, Integer> entry : blockIndexFutureDistanceMap.entrySet()){
            int blockIndex = entry.getKey();
            int blockDistance = entry.getValue();

            if (blockDistance > farthestBlockDistance){
                farthestBlockDistance = blockDistance;
                evictionIndex = blockIndex;
            }
        }
        return evictionIndex;
    }

    @Override
    public CacheType getSupportedType() {
        return CacheType.L2;
    }
}
