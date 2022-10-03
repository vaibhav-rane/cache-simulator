import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringJoiner;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */

public class Cache {

    private CacheType type;
    private int blockSize;
    private int size;
    private int associativity;
    private ReplacementPolicy replacementPolicy;
    private InclusiveProperty inclusionProperty;
    private int setCount;

    private int blockCount;
    private int readCount = 0;
    private int writeCount = 0;
    private int readMissCount = 0;

    private int readHitCount = 0;
    private int writeMissCount = 0;

    private int writeHitCount = 0;
    private int writeBackCount = 0;
    private String traceFile;
    private List<String> instructions;
    private List<List<CacheBlock>> sets;
    private List<String> opt;
    int PLRU[][];

    private Cache prevLevelCache;
    private Cache nextLevelCache;

    private Map<Integer, Queue<CacheBlock>> setLruQueueMap;
    public Cache(){
        this.sets = new ArrayList<>();
        initializeSets();
    }

    public void initializePLRU(){
        int tempAssoc = this.associativity;
        if(this.associativity <2)
            tempAssoc =2;
        PLRU = new int [setCount][tempAssoc-1];
    }
    public void initializeSets(){
        if (this.associativity != 0) {
            this.setCount = ((this.size) /( this.associativity * this.blockSize));
        }
    }

    public int getWriteHitCount() {
        return writeHitCount;
    }

    public void setWriteHitCount(int writeHitCount) {
        this.writeHitCount = writeHitCount;
    }

    public int getReadHitCount() {
        return readHitCount;
    }

    public void setReadHitCount(int readHitCount) {
        this.readHitCount = readHitCount;
    }

    public Map<Integer, Queue<CacheBlock>> getSetLruQueueMap() {
        return setLruQueueMap;
    }

    public void setSetLruQueueMap(Map<Integer, Queue<CacheBlock>> setLruQueueMap) {
        this.setLruQueueMap = setLruQueueMap;
    }

    public CacheType getType() {
        return type;
    }

    public void setType(CacheType type) {
        this.type = type;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getAssociativity() {
        return associativity;
    }

    public void setAssociativity(int associativity) {
        this.associativity = associativity;
    }

    public Cache getNextLevelCache() {
        return nextLevelCache;
    }

    public void setNextLevelCache(Cache nextLevelCache) {
        this.nextLevelCache = nextLevelCache;
    }

    public Cache getPrevLevelCache() {
        return prevLevelCache;
    }

    public void setPrevLevelCache(Cache prevLevelCache) {
        this.prevLevelCache = prevLevelCache;
    }

    public ReplacementPolicy getReplacementPolicy() {
        return replacementPolicy;
    }

    public void setReplacementPolicy(ReplacementPolicy replacementPolicy) {
        this.replacementPolicy = replacementPolicy;
    }

    public InclusiveProperty getInclusionProperty() {
        return inclusionProperty;
    }

    public void setInclusionProperty(InclusiveProperty inclusionProperty) {
        this.inclusionProperty = inclusionProperty;
    }

    public int getSetCount() {
        return setCount;
    }

    public void setSetCount(int setCount) {
        this.setCount = setCount;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public int getReadCount() {
        return readCount;
    }

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    public int getWriteCount() {
        return writeCount;
    }

    public void setWriteCount(int writeCount) {
        this.writeCount = writeCount;
    }

    public int getReadMissCount() {
        return readMissCount;
    }

    public void setReadMissCount(int readMissCount) {
        this.readMissCount = readMissCount;
    }

    public int getWriteMissCount() {
        return writeMissCount;
    }

    public void setWriteMissCount(int writeMissCount) {
        this.writeMissCount = writeMissCount;
    }

    public int getWriteBackCount() {
        return writeBackCount;
    }

    public void setWriteBackCount(int writeBackCount) {
        this.writeBackCount = writeBackCount;
    }

    public String getTraceFile() {
        return traceFile;
    }

    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }

    public List<String> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<String> instructions) {
        this.instructions = instructions;
    }

    public List<List<CacheBlock>> getSets() {
        return sets;
    }

    public void setSets(List<List<CacheBlock>> sets) {
        this.sets = sets;
    }

    public List<String> getOpt() {
        return opt;
    }

    public void setOpt(List<String> opt) {
        this.opt = opt;
    }

    public int[][] getPLRU() {
        return PLRU;
    }

    public void setPLRU(int[][] PLRU) {
        this.PLRU = PLRU;
    }

    public boolean hasNextLevel(){
        return this.getNextLevelCache() != null;
    }

    public List<CacheBlock> getSetAtIndex(int setIndex){
        return this.sets.get(setIndex);
    }
    @Override
    public String toString() {
        return new StringJoiner(", ", Cache.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .add("blockSize=" + blockSize)
                .add("size=" + size)
                .add("associativity=" + associativity)
                .add("replacementPolicy=" + replacementPolicy)
                .add("inclusionProperty=" + inclusionProperty)
                .add("setCount=" + setCount)
                .add("READ=" + readCount)
                .add("WRITE=" + writeCount)
                .add("readMissCount=" + readMissCount)
                .add("writeMissCount=" + writeMissCount)
                .add("writeBackCount=" + writeBackCount)
                .add("traceFile='" + traceFile + "'")
//                .add("inputData=" + instructions.size())
//                .add("#sets=" + sets.size())
                .add("opt=" + opt)
                .add("pLRU=" + Arrays.toString(PLRU))
                .toString();
    }
}

