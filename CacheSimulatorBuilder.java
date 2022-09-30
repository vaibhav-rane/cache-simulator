/**
 * @author varane
 * @created 09/25/2022
 */
public class CacheSimulatorBuilder {
    private CacheManager cacheManager;

    public CacheSimulatorBuilder() {
    }

    public CacheSimulatorBuilder initSimulation() {
        if(this.cacheManager == null)
            this.cacheManager = new CacheManager();
        return this;
    }

    public CacheSimulatorBuilder addCache(Cache c, CacheType type){
        switch (type){
            case L1:
                this.cacheManager.setL1(c);
                break;
            case L2:
                this.cacheManager.setL2(c);
                break;
        }
        return this;
    }

    public CacheSimulatorBuilder prepare(){
        cacheManager.initializeCache(cacheManager.getL1());
        cacheManager.initializeCache(cacheManager.getL2());
        return this;
    }

    public void start(){
        cacheManager.startSimulation();
        return;
    }
}
