package evictionprocessors;

import constants.Constants;
import core.Cache;
import core.CacheBlock;
import enums.CacheType;
import evictionprocessors.EvictionProcessor;
import utils.CacheManagerUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 * Handles eviction in L1 Cache using Optimal Replacement Policy
 */
public class L1OptEvictionProcessor implements EvictionProcessor {

    /**
     * @apiNote Returns the index of a block to be evicted from the cache based on the Optimal replacement policy
     * - Identifies the target set
     * - identifies the block to evict based on the future accesses of each blocks in the target set.
     * - Returns the index of the block that is needed farthest in the future or not needed at all*/
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
            }
        }
        return evictionIndex;
    }
    @Override
    public CacheType getSupportedType() {
        return CacheType.L1;
    }
}
