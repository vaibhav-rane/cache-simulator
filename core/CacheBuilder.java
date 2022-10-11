package core;

import enums.CacheType;
import enums.InclusiveProperty;
import enums.ReplacementPolicy;
import evictionprocessors.EvictionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by varane on 10/1/22.
 * A builder class that builds core.Cache objects.
 */
public class CacheBuilder {
    private Cache cache;

    private EvictionManager evictionManager;
    public CacheBuilder builder(){
        this.cache = new Cache();
        this.evictionManager = new EvictionManager();
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

    public CacheBuilder type(CacheType type){
        this.cache.setType(type);
        return this;
    }
    public CacheBuilder traceFile(String traceFile){
        this.cache.setTraceFile(traceFile);
        return this;
    }

    public Cache build(){
        if (this.cache.getSize() > 0){
            int blockCount = this.cache.getSize() / this.cache.getBlockSize();
            int setCount = blockCount / this.cache.getAssociativity();

            this.cache.setSetCount(setCount);
            this.cache.setBlockCount(blockCount);

            List<CacheBlock[]> sets = new ArrayList<>();
            for (int i = 1; i <= setCount; i++){
                CacheBlock[] set = new CacheBlock[this.cache.getAssociativity()];
                sets.add(set);
            };
            this.cache.setSets(sets);

            if (this.cache.getReplacementPolicy().equals(ReplacementPolicy.LRU)){
                this.cache.setEvictionProcessor(evictionManager.getEvictionProcessorFor(ReplacementPolicy.LRU, this.cache.getType()));
            }
            else if (this.cache.getReplacementPolicy().equals(ReplacementPolicy.FIFO)){
                this.cache.setEvictionProcessor(evictionManager.getEvictionProcessorFor(ReplacementPolicy.FIFO, this.cache.getType()));
            }
            else{
                this.cache.setEvictionProcessor(evictionManager.getEvictionProcessorFor(ReplacementPolicy.OPT, this.cache.getType()));
            }
        }
        return this.cache;
    }
}
