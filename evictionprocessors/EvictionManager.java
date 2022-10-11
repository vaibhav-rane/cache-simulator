package evictionprocessors;

import enums.CacheType;
import enums.ReplacementPolicy;
import evictionprocessors.EvictionProcessor;
import evictionprocessors.L1LruFifoEvictionProcessor;
import evictionprocessors.L1OptEvictionProcessor;
import evictionprocessors.L2LruFifoEvictionProcessor;
import evictionprocessors.L2OptEvictionProcessor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by varane on 10/1/22.
 * @apiNote Factory that returns appropriate implementation of the eviction processor based on
 * the given replacement policy and the cache type.
 */
public class EvictionManager {
    /**
     * Hash map that holds list of eviction processors for a Replacement policy*/
    private Map<ReplacementPolicy, List<EvictionProcessor>> evictionProcessorMap;

    public EvictionManager() {
        this.evictionProcessorMap = new HashMap<>();
        this.evictionProcessorMap.put(ReplacementPolicy.LRU, Arrays.asList(new L1LruFifoEvictionProcessor(), new L2LruFifoEvictionProcessor()));
        this.evictionProcessorMap.put(ReplacementPolicy.FIFO, Arrays.asList(new L1LruFifoEvictionProcessor(), new L2LruFifoEvictionProcessor()));
        this.evictionProcessorMap.put(ReplacementPolicy.OPT, Arrays.asList(new L1OptEvictionProcessor(), new L2OptEvictionProcessor()));
    }

    public EvictionProcessor getEvictionProcessorFor(ReplacementPolicy policy, CacheType type){
        return this.evictionProcessorMap
                .get(policy)
                .stream()
                .filter(processor -> processor.getSupportedType().equals(type))
                .findFirst()
                .get();

    }
}
