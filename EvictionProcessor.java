/**
 * Created by varane on 10/1/22.
 */
public interface EvictionProcessor {
    void evict(String address, Cache cache);
}
