package evictionprocessors;

import core.Cache;
import enums.CacheType;

/**
 * Created by varane on 10/1/22.
 * Superinterface that specifies the eviction processor
 */
public interface EvictionProcessor {
    int getEvictionIndex(String address, Cache cache);
    CacheType getSupportedType();
}
