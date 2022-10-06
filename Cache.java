import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private List<CacheBlock[]> sets;
    private List<String> opt;
    private Cache prevLevelCache;
    private Cache nextLevelCache;

    private EvictionProcessor evictionProcessor;
    public Cache(){
        this.sets = new ArrayList<>();
    }

    /**
     * Accepts read command from the CPU.
     * Reads from L1 if the invoking object is of type L1
     * else reads from L2*/
    public boolean read(String address){
        //----------DEBUG START-----------
        String tag = CacheManagerUtils.getTagFor(address, this);
        int setIndex = CacheManagerUtils.getSetIndexFor(address, this);
        System.out.println(type.name()+" read : "+address+" (tag "+tag+", index "+setIndex+")");

        //----------DEBUG END-----------


        this.readCount++;
        CacheBlock block = getBlock(address);

        if (Objects.nonNull(block)){
            /**
             * READ HIT*/
            //----------DEBUG START-----------
            System.out.println(type.name()+" hit");
            //----------DEBUG END-----------

            this.readHitCount++;
            if (this.replacementPolicy.equals(ReplacementPolicy.LRU)){
                //----------DEBUG START-----------
                System.out.println(type.name()+" update LRU");
                //----------DEBUG END-----------
                block.setLastAccess(Constants.blockAccessCounter++);
            }

            return true;
        }
        /**
         * READ MISS*/
        //----------DEBUG START-----------
        System.out.println(type.name()+" miss");
        //----------DEBUG END-----------
        this.readMissCount++;
        return false;
    }

    /**
     * Checks if the block with the supplied address present in the cache*/
    public CacheBlock getBlock(String address){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, this);
        String tag = CacheManagerUtils.getTagFor(address, this);
        CacheBlock[] set = getSetAt(setIndex);
        for (CacheBlock block : set){
            if (Objects.nonNull(block) && block.getTag().equals(tag))
                return block;
        }
        return null;
    }

    public CacheBlock[] getSetAt(int setIndex){
        return this.sets.get(setIndex);
    }

    public void allocateBlock(String address){
        CacheBlock block = CacheManagerUtils.createNewCacheBlockFor(this, address);
        if (isSpaceAvailableFor(block)){
            //----------DEBUG START-----------
            System.out.println(type.name()+ " victim: none");
            //----------DEBUG END-----------
            addBlock(block);
        }
        else{
            int evictionIndex = evictionProcessor.getEvictionIndex(address, this);
            replaceBlockAtEvictionIndex(evictionIndex, block);
            //addBlock(block);
        }
        //----------DEBUG START-----------
        System.out.println(getType().name()+ " update LRU");
        //----------DEBUG END-----------
    }

    public void replaceBlockAtEvictionIndex(int evictionIndex, CacheBlock replacementBlock){
        int setIndex = CacheManagerUtils.getSetIndexFor(replacementBlock.getAddress(), this);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, this);
        CacheBlock evictedBlock = set[evictionIndex];
        //----------DEBUG START-----------
        System.out.println(type.name()+ " victim "+evictedBlock.getAddress()+" (tag "+evictedBlock.getTag()+", index "+evictionIndex+", dirty "+evictedBlock.isDirty()+")");
        //----------DEBUG END-----------

        set[evictionIndex] = replacementBlock;
        //CacheBlock evictedBlock = set.set(evictionIndex, replacementBlock);
        if (evictedBlock.isDirty()){
            writeBack(evictedBlock);
        }
        if (this.type.equals(CacheType.L2) && inclusionProperty.equals(InclusiveProperty.INCLUSIVE)){
            ensureInclusiveness(evictedBlock, prevLevelCache);
        }
    }

    public void ensureInclusiveness(CacheBlock evictedBlockFromL2, Cache L1){
        if (CacheManagerUtils.blockExistsIn(evictedBlockFromL2, L1)){
            int evictionIndex = CacheManagerUtils.getIndexOfBlockInSet(evictedBlockFromL2.getAddress(), L1);

            if (evictionIndex != -1){
                // TODO: 10/5/22 Recheck on failure
                int setIndex = CacheManagerUtils.getSetIndexFor(evictedBlockFromL2.getAddress(), L1);
                CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, L1);
                CacheBlock evictedBlock = set[evictionIndex];

                //----------DEBUG START-----------
                System.out.println(type.name()+ " victim "+evictedBlock.getAddress()+" (tag "+evictedBlock.getTag()+", index "+evictionIndex+", dirty "+evictedBlock.isDirty()+")");
                //----------DEBUG END-----------

                set[evictionIndex] = null;

                if (evictedBlock.isDirty()){
                    L1.setWriteBackCount(L1.getWriteBackCount() + 1);
                }
            }
        }
    }

    /**
     * If the next level cache exists, attempts to write evicted block from this level cache to the next level cache.
     * if there is a WRITE MISS on the next level, allocates a new block to next level and marks it dirty.
     * */
    public void writeBack(CacheBlock evictedBlock){
        this.writeBackCount++;
        if (this.hasNextLevel()){
            Cache nextLevelCache = this.getNextLevelCache();
            boolean writeHit = nextLevelCache.write(evictedBlock.getAddress());
            if (! writeHit){
                nextLevelCache.allocateBlockAndSetDirty(evictedBlock.getAddress());
            }
        }
    }

    public void allocateBlockAndSetDirty(String address){
        CacheBlock block = CacheManagerUtils.createNewCacheBlockFor(this, address);
        block.setDirty(true);
        if (isSpaceAvailableFor(block)){
            //----------DEBUG START-----------
            System.out.println(type.name()+ " victim: none");
            //----------DEBUG END-----------
            addBlock(block);
        }
        else{
            int evictionIndex = evictionProcessor.getEvictionIndex(address, this);
            replaceBlockAtEvictionIndex(evictionIndex, block);
            //addBlock(block);
        }
        //----------DEBUG START-----------
        System.out.println(getType().name()+ " update LRU");
        //----------DEBUG END-----------
    }

    public boolean isSpaceAvailableFor(CacheBlock block){
        int index = CacheManagerUtils.getSetIndexFor(block.getAddress(), this);
        CacheBlock[] set = getSetAt(index);
        for (CacheBlock cb : set){
            if (Objects.isNull(cb))
                return true;
        }
        return false;
    }

    /**
     * Use isSpaceAvailableFor before invoking this*/
    public void addBlock(CacheBlock block){
        int setIndex = CacheManagerUtils.getSetIndexFor(block.getAddress(), this);
        CacheBlock[] set = getSetAt(setIndex);
        /**
         * find first null block and allocate there*/
        for (int i = 0; i < set.length; i++){
            CacheBlock cb = set[i];
            if (Objects.isNull(cb)){
                set[i] = block;
                break;
            }
        }
    }

    /**
     * Accepts write command from CPU.
     * Reads from the cache
     * If read is successful -> READ HIT -> returns true
     * If read is unsuccessful -> READ MISS -> returns false*/
    public boolean write(String address){
        //----------DEBUG START-----------
        System.out.println(type.name()+ " write : "+address+" (tag "+CacheManagerUtils.getTagFor(address, this)+", index "+CacheManagerUtils.getSetIndexFor(address, this)+")");
        //----------DEBUG END-----------
        this.writeCount++;
        CacheBlock block = getBlock(address);

        if (Objects.nonNull(block)){
            //----------DEBUG START-----------
            System.out.println(type.name()+ " hit");
            //----------DEBUG END-----------
            /**
             * WRITE HIT*/
            this.writeHitCount++;

            if (this.replacementPolicy.equals(ReplacementPolicy.LRU)){
                block.setLastAccess(Constants.blockAccessCounter++);
                //----------DEBUG START-----------
                System.out.println(type.name()+ " update LRU");
                //----------DEBUG END-----------
            }
            block.setDirty(true);
            //----------DEBUG START-----------
            System.out.println(type.name()+ " set dirty");
            //----------DEBUG END-----------
            return true;
        }
        //----------DEBUG START-----------
        System.out.println(type.name()+ " miss");
        //----------DEBUG END-----------
        this.writeMissCount++;
        return false;
    }

    public EvictionProcessor getEvictionProcessor() {
        return evictionProcessor;
    }

    public void setEvictionProcessor(EvictionProcessor evictionProcessor) {
        this.evictionProcessor = evictionProcessor;
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

    public List<CacheBlock[]> getSets() {
        return sets;
    }

    public void setSets(List<CacheBlock[]> sets) {
        this.sets = sets;
    }

    public List<String> getOpt() {
        return opt;
    }

    public void setOpt(List<String> opt) {
        this.opt = opt;
    }

    public boolean hasNextLevel(){
        return this.getNextLevelCache() != null;
    }

    public CacheBlock[] getSetAtIndex(int setIndex){
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
                .add("opt=" + opt)
                .toString();
    }
}

