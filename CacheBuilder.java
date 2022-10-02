/**
 * Created by varane on 10/1/22.
 */
public class CacheBuilder {
    private Cache cache;

    public CacheBuilder builder(){
        this.cache = new Cache();
        return this;
    }

    public CacheBuilder blockSize(int blockSize){
        this.cache.setBlockSize(blockSize);
        return this;
    }

    public CacheBuilder size(int size){
        this.cache.setSize(size);
        return this;
    }

    public CacheBuilder associativity(int associativity){
        this.cache.setAssociativity(associativity);
        return this;
    }

    public CacheBuilder replacementPolicy(int policyCode){
        for (ReplacementPolicy policy : ReplacementPolicy.values()){
            if (policy.getCode() == policyCode){
                this.cache.setReplacementPolicy(policy);
                break;
            }
        }
        return this;
    }

    public CacheBuilder inclusiveProperty(int code){
        for (InclusiveProperty inclusiveProperty : InclusiveProperty.values()){
            if(inclusiveProperty.getCode() == code){
                this.cache.setInclusionProperty(inclusiveProperty);
                break;
            }
        }
        return this;
    }

    public CacheBuilder traceFile(String traceFile){
        this.cache.setTraceFile(traceFile);
        return this;
    }

    public Cache build(){
        return this.cache;
    }
}
