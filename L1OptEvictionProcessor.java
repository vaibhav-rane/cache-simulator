import java.util.List;
import java.util.Objects;

/**
 * Created by varane on 10/1/22.
 */
public class L1OptEvictionProcessor implements EvictionProcessor{
    @Override
    public int getEvictionIndex(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        int farthestAccess = Integer.MIN_VALUE;
        int evictionIndex = -1;

        for (int i = 0; i < set.length; i++){
            CacheBlock block = set[i];
            String blockAddress = block.getAddress().trim();

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
             * If there are no future occurrences after removing the past accesses, this block will never be needed -> ideal for eviction.*/
            if (Objects.isNull(futureAccesses) || futureAccesses.isEmpty()){
                return i;
            }
            else{
                int nearestFutureAccess = futureAccesses.get(0);
                if (nearestFutureAccess > farthestAccess){
                    farthestAccess = nearestFutureAccess;
                    evictionIndex = i;
                }
//                blockFutureAccessMap.put(i, nearestFutureAccess);
            }
        }

//        int farthestFutureAccess = Integer.MIN_VALUE;
//        int evictionIndex = -1;
//
//        for (Map.Entry<Integer, Integer> entry : blockFutureAccessMap.entrySet()){
//            int blockIndex = entry.getKey();
//            int nearestFutureAccess = entry.getValue();
//
//            if (nearestFutureAccess > farthestFutureAccess){
//                farthestFutureAccess = nearestFutureAccess;
//                evictionIndex = blockIndex;
//            }
//        }
        return evictionIndex;
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
