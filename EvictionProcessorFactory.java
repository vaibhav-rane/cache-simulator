import java.util.HashMap;
import java.util.Map;

/**
 * Created by varane on 10/1/22.
 */
public class EvictionProcessorFactory {
    private Map<ReplacementPolicy, EvictionProcessor> evictionProcessorMap;

    public EvictionProcessorFactory() {
        this.evictionProcessorMap = new HashMap<>();
        this.evictionProcessorMap.put(ReplacementPolicy.LRU, new LruEvictionProcessor());
        this.evictionProcessorMap.put(ReplacementPolicy.PLRU, new PlruEvictionProcessor());
        this.evictionProcessorMap.put(ReplacementPolicy.OPT, new OptEvictionProcessor());
    }

    public EvictionProcessor getEvictionProcessor(ReplacementPolicy policy){
        return this.evictionProcessorMap.get(policy);
    }
}
