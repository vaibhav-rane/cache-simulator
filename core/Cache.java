package core;

import constants.Constants;
import enums.CacheType;
import enums.InclusiveProperty;
import enums.ReplacementPolicy;
import evictionprocessors.EvictionProcessor;
import utils.CacheManagerUtils;

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
     * @apiNote Attempts to read the block corresponding to the supplied memory address in the invoking cache object.
     * - If the block is found -> READ HIT
     * - If the block is not found -> READ MISS
     *      - Ensures block space for the block coming from the next memory level.
     *      - Invoke read on the next level cache
     *      - Allocate new block*/
    public boolean read (String address){
        //----------DEBUG START-----------
        String tag = CacheManagerUtils.getTagFor(address, this);
        int setIndex = CacheManagerUtils.getSetIndexFor(address, this);
        System.out.println(type.name()+" read : "+address+" (tag "+tag+", index "+setIndex+")");
        //----------DEBUG END-----------

        this.readCount++;
        CacheBlock block = getBlock(address);
        if (Objects.nonNull(block)){
            // READ HIT

            //----------DEBUG START-----------
            System.out.println(type.name()+" hit");
            //----------DEBUG END-----------

            readHitCount++;

            if (this.replacementPolicy.equals(ReplacementPolicy.LRU)){
                //----------DEBUG START-----------
                System.out.println(type.name()+" update LRU");
                //----------DEBUG END-----------

                block.setLastAccess(Constants.blockAccessCounter++);
            }
            return true;
        }
        else {
            //READ MISS

            //----------DEBUG START-----------
            System.out.println(type.name()+" miss");
            //----------DEBUG END-----------

            readMissCount++;

            /**
             * Ensuring free space for the block coming from the next memory level*/
            ensureBlockSpace(address);

            if (hasNextLevel()){
                nextLevelCache.read(address);
            }

            CacheBlock blockFromNextLevel = CacheManagerUtils.createNewCacheBlockFor(this, address);
            allocateBlockV2(blockFromNextLevel);
        }
        return false;
    }

    /**
     * @apiNote
     * -Ensures block space for the future incoming block corresponding to the supplied address.
     * -Does nothing if the space is available for the block in the target set.
     * -If the target set is full, performs eviction based on the replacement policy.
     * -If the cache has a next level cache and the evicted block is dirty, issues a write-back to the next level cache.
     * -If eviction is needed, ensures cache inclusiveness in lower level cache if applicable.
     * */
    public void ensureBlockSpace ( String address ){
        boolean spaceAvailable = isSpaceAvailableFor(address);

        if (spaceAvailable) {
            //----------DEBUG START-----------
            System.out.println(type.name()+ " victim: none");
            //----------DEBUG END-----------
            return;
        }
        else {
            /**
             * Making space for the future block coming from the next memory level.
             * Preparing for Eviction*/
            int evictionIndex = evictionProcessor.getEvictionIndex(address, this);

            int setIndex = CacheManagerUtils.getSetIndexFor(address, this);
            CacheBlock[] targetSet = CacheManagerUtils.getSetForSetIndex(setIndex, this);

            CacheBlock evictedBlock = targetSet[evictionIndex];

            //----------DEBUG START-----------
            System.out.println(type.name()+ " victim "+evictedBlock.getAddress()+" (tag "+evictedBlock.getTag()+", index "+setIndex+", dirty "+evictedBlock.isDirty()+")");
            //----------DEBUG END-----------

            /**
             * Eviction completed*/
            targetSet[evictionIndex] = null;

            /**
             * Checking if write-back to the next level is needed*/
            if ( evictedBlock.isDirty() ) {
                writeBackToNextLevel(evictedBlock);
            }

            if (this.type.equals(CacheType.L2) && this.inclusionProperty.equals(InclusiveProperty.INCLUSIVE)){
                ensureInclusiveness(evictedBlock, this.getPrevLevelCache());
            }
        }
    }

    /***
     * @apiNote
     * - Write-backs the evicted dirty block to the next level cache if applicable.
     */
    public void writeBackToNextLevel(CacheBlock evictedBlock){
        this.writeBackCount++;
        if (this.hasNextLevel()){
            Cache nextLevelCache = this.getNextLevelCache();
            nextLevelCache.write(evictedBlock.getAddress());
        }
    }

    /**
     * */
    public boolean write (String address){
        //----------DEBUG START-----------
        System.out.println(type.name()+ " write : "+address+" (tag "+ CacheManagerUtils.getTagFor(address, this)+", index "+ CacheManagerUtils.getSetIndexFor(address, this)+")");
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
        else {
            //WRITE MISS
            //----------DEBUG START-----------
            System.out.println(type.name()+ " miss");
            //----------DEBUG END-----------

            this.writeMissCount++;

            /**
             * Ensuring free space for the block coming from the next memory level*/
            ensureBlockSpace(address);

            if (hasNextLevel()){
                nextLevelCache.read(address);
            }
            CacheBlock cacheBlockFromNextLevel = CacheManagerUtils.createNewCacheBlockFor(this, address);

            //----------DEBUG START-----------
            System.out.println(type.name()+ " set dirty");
            //----------DEBUG END-----------

            cacheBlockFromNextLevel.setDirty(true);

            allocateBlockV2(cacheBlockFromNextLevel);
        }
        return false;
    }

    /**
     * Adds the supplied block to the first available index in the appropriate set of the invoking cache.*/
    public void allocateBlockV2(CacheBlock block){
        int setIndex = CacheManagerUtils.getSetIndexFor(block.getAddress(), this);
        CacheBlock[] targetSet = CacheManagerUtils.getSetForSetIndex(setIndex, this);
        for (int i = 0; i < targetSet.length; i++) {
            if (Objects.isNull(targetSet[i])){
                targetSet[i] = block;
                break;
            }
        }
    }
    /**
     * Checks if the block with the supplied address present in the invoking cache*/
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

    public boolean isSpaceAvailableFor(CacheBlock block){
        int index = CacheManagerUtils.getSetIndexFor(block.getAddress(), this);
        CacheBlock[] set = getSetAt(index);
        for (CacheBlock cb : set){
            if (Objects.isNull(cb))
                return true;
        }
        return false;
    }

    public boolean isSpaceAvailableFor(String address){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, this);
        CacheBlock[] set = getSetAt(setIndex);
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

