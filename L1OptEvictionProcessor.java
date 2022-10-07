import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by varane on 10/1/22.
 */
public class L1OptEvictionProcessor implements EvictionProcessor{
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
            List<Integer> futureOccurrencesOfBlockAddress = Constants.addressOccurrenceMap.get(blockAddress);

            /**
             * If there are no future occurrences, this block will never be needed -> ideal for eviction.*/
            if (Objects.isNull(futureOccurrencesOfBlockAddress) || futureOccurrencesOfBlockAddress.isEmpty()){
                return i;
            }
            else{
                int nearestFutureOccurrence = Integer.MAX_VALUE;

                for (Integer futureOccurrence : futureOccurrencesOfBlockAddress){
                    /**
                     * We are only interested in the first future occurrence after the current PC*/
                    if (futureOccurrence < Constants.programCounter)
                        continue;
                    else{
                        nearestFutureOccurrence = futureOccurrence;
                        break;
                    }
                }
                blockIndexFutureDistanceMap.put(i, nearestFutureOccurrence);
            }
        }

        int farthestBlockDistance = Integer.MIN_VALUE;
        int evictionIndex = -1;
        for (Map.Entry<Integer, Integer> entry : blockIndexFutureDistanceMap.entrySet()){
            int blockIndex = entry.getKey();
            int nearestFutureOccurrence = entry.getValue();

            if (nearestFutureOccurrence > farthestBlockDistance){
                farthestBlockDistance = nearestFutureOccurrence;
                evictionIndex = blockIndex;
            }
        }
        return evictionIndex;
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
