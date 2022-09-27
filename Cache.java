import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private int replacementPolicy;
    private int inclusionProperty;
    private int setCount;
    private int readCount = 0;
    private int writeCount = 0;
    private int readMissCount = 0;
    private int writeMissCount = 0;
    private int writeBackCount = 0;
    private String traceFile;
    private List<String> instructions;
    private List<List<CacheBlock>> cache;
    private List<String> opt;

    int PLRU[][];


    public Cache(int blockSize, int size, int associativity, int replacementPolicy, int inclusionProperty, String traceFile, CacheType type) {

        this.blockSize = blockSize;
        this.size = size;
        this.associativity = associativity;
        this.replacementPolicy = replacementPolicy;
        this.inclusionProperty = inclusionProperty;
        this.type = type;
        /**
         * Computing number of sets/cache
         * */
//        if (this.associativity != 0) {
//            setCount = ((this.size) /( this.associativity * blockSize));
//        }

        this.traceFile = traceFile;
        instructions = readInstructionsFromTrace();
        cache = new ArrayList<>();

        int tempAssoc = this.associativity;
        if(this.associativity <2)
            tempAssoc =2;

        initializeSets();
        PLRU = new int [setCount][tempAssoc-1];
    }

    public void initializeSets(){
        if (this.associativity != 0) {
            this.setCount = ((this.size) /( this.associativity * this.blockSize));
        }
    }

    public List<String> readInstructionsFromTrace() {
        List<String> instructions = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(traceFile)));
            String instruction;
            while ((instruction = br.readLine()) != null) {
                instructions.add(instruction);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }

        return instructions;
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

    public int getReplacementPolicy() {
        return replacementPolicy;
    }

    public void setReplacementPolicy(int replacementPolicy) {
        this.replacementPolicy = replacementPolicy;
    }

    public int getInclusionProperty() {
        return inclusionProperty;
    }

    public void setInclusionProperty(int inclusionProperty) {
        this.inclusionProperty = inclusionProperty;
    }

    public int getSetCount() {
        return setCount;
    }

    public void setSetCount(int setCount) {
        this.setCount = setCount;
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

    public List<List<CacheBlock>> getCache() {
        return cache;
    }

    public void setCache(List<List<CacheBlock>> cache) {
        this.cache = cache;
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
                .add("inputData=" + instructions)
                .add("cache=" + cache)
                .add("opt=" + opt)
                .add("pLRU=" + Arrays.toString(PLRU))
                .toString();
    }
}

