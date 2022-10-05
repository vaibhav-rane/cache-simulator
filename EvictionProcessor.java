/**
 * Created by varane on 10/1/22.
 */
public interface EvictionProcessor {
    int getEvictionIndex(String address, Cache cache);
    CacheType getSupportedType();
}
