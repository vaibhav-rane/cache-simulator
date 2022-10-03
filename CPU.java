import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by varane on 10/2/22.
 */
public class CPU {
    private String traceFile;
    private List<String> instructions;
    private Cache L1;
    private Cache L2;
    private EvictionManager evictionManager;
    int trafficCounter = 0;
    private static final String READ = "r";

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

    public Cache getL1() {
        return L1;
    }

    public void setL1(Cache l1) {
        L1 = l1;
    }

    public Cache getL2() {
        return L2;
    }

    public void setL2(Cache l2) {
        L2 = l2;
    }

    public EvictionManager getEvictionManager() {
        return evictionManager;
    }

    public void setEvictionManager(EvictionManager evictionManager) {
        this.evictionManager = evictionManager;
    }

    public void boot(){
        this.L1.setType(CacheType.L1);
        this.L2.setType(CacheType.L2);

        /**
         * Initializing Caches*/
        List<Cache> caches = Arrays.asList(L1, L2);

        for (Cache cache : caches){

            if(cache.getSize() == 0){
                cache.getPrevLevelCache().setNextLevelCache(null);
                continue;
            }
            int blockCount = cache.getSize() / cache.getBlockSize();
            int setCount = blockCount / cache.getAssociativity();

            cache.setSetCount(setCount);
            cache.setBlockCount(blockCount);

            List<List<CacheBlock>> sets = new ArrayList<>();
            for (int i = 1; i <= setCount; i++){
                List<CacheBlock> blocks = new ArrayList<>();
                sets.add(blocks);
            };
            cache.setSets(sets);

        }

        /**
         * Reading instructions from trace file*/
        this.instructions = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(traceFile)));
            String instruction;
            while ((instruction = br.readLine()) != null) {
                if(instruction.isEmpty() || instruction.length() < 4) continue;
                this.instructions.add(instruction);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.evictionManager = new EvictionManager();
    }

    /**
     * Executes Instructions one-by-one*/
    public void run(){
        int i = 1;
        for (String instruction : instructions){
            System.out.println("executing: #"+i);
            String operation = CacheManagerUtils.getOperation(instruction);
            String address = CacheManagerUtils.getMemoryAddress(instruction);

            if(i == 212){
                System.out.println("stop");
            }
            if (operation.equals(READ))
                read(address);
            else
                write(address);
            i++;
        }
        print();
    }

    public void read(String address){
        /**
         * Reading from L1*/
        boolean hit = isReadHit(L1, address);

        if(! hit){
            if(L1.hasNextLevel()){
                /**
                 * L1 Read MISS. Reading from L2*/
                if(isReadHit(L2, address)){
                    /**
                     * L2 Read HIT. Allocating block in L1*/
                    allocateBlockToL1(address);
                }
                else {
                    /**
                     * L1 READ MISS, L2 READ MISS -> Allocating block in both L1 and L2*/
                    allocateBlockToL2(address);
                    allocateBlockToL1(address);
                }
            }
            else {
                /**
                 * No L2 Cache found*/
                allocateBlockToL1(address);
            }
        }
    }

    /**
     * -Allocates a memory block corresponding to the supplied memory address
     * -if the target set is full, evicts the block as per the replacement policy
     * -if the evicted block is dirty, issues a write-back on L2 cache
     * ----if the block is not present in L2 -> WRITE MISS
     * ----allocateBlockToL2(address)*/
    public void allocateBlockToL1(String address){
        CacheBlock block = CacheManagerUtils.createNewCacheBlockFor(L1, address);
        if(! CacheManagerUtils.isSetVacantFor(L1, block)){
            evictionManager
                    .getEvictionProcessorFor(L1.getReplacementPolicy(), CacheType.L1)
                    .evict(address, L1);
        }
        CacheManagerUtils.addBlockToCache(L1, block);
    }

    /**
     * -Allocates a memory block corresponding to the supplied memory address
     * -if the target set is full, evicts the block as per the replacement policy
     * -if the evicted block is dirty, issues a write-back on RAM */
    public void allocateBlockToL2(String address){
        CacheBlock block = CacheManagerUtils.createNewCacheBlockFor(L2, address);
        if(! CacheManagerUtils.isSetVacantFor(L2, block) ){
            evictionManager
                    .getEvictionProcessorFor(L2.getReplacementPolicy(), CacheType.L2)
                    .evict(address, L2);
        }
        CacheManagerUtils.addBlockToCache(L2, block);
    }

    /**
     * Checks if a block for the address already present in the cache
     * increments the readCounter
     * if present -> HIT -> updates the access counter
     *      increments the readHit counter
     * MISS -> increments the readMiss counter*/
    public boolean isReadHit(Cache cache, String address){
        cache.setReadCount(cache.getReadCount() + 1);

        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        String tag = CacheManagerUtils.getTagFor(address, cache);
        List<CacheBlock> set = cache.getSetAtIndex(setIndex);

        for (CacheBlock block : set){
            if (block.getTag().equals(tag)){
                block.setLastAccess(Constants.blockAccessCounter++);
                cache.setReadHitCount(cache.getReadHitCount() + 1);
                return true;
            }
        }
        cache.setReadMissCount(cache.getReadMissCount() + 1);
        return false;
    }

    public void write(String address){
        /**
         * Attempting to WRITE in L1*/
        boolean writeHit = isWriteHit(L1, address);

        if(! writeHit){
            /**
             * L1 WRITE MISS -> READ from L2*/

            if(L1.hasNextLevel()){

                L2.setReadCount(L2.getReadCount() + 1);

                CacheBlock blockToWrite = CacheManagerUtils.getBlockAt(address, L2);
                if ( Objects.nonNull(blockToWrite) ){
                    /**
                     * L2 READ HIT*/
                    L2.setReadHitCount(L2.getReadHitCount() + 1);
                    blockToWrite.setLastAccess(Constants.blockAccessCounter++);

                    /**
                     * Allocating Block in L1*/
                    allocateBlockToL1(address);
                    CacheBlock allocatedBlock = CacheManagerUtils.getBlockAt(address, L1);
                    makeMeDirty(allocatedBlock);
                }
                else{
                    /**
                     * L1 WRITE MISS L2 READ MISS. Allocating Blocks to both L1 and L2 */
                    L2.setReadMissCount(L2.getReadMissCount() + 1);
                    allocateBlockToL2(address);
                    allocateBlockToL1(address);

                    CacheBlock allocatedBlock = CacheManagerUtils.getBlockAt(address, L1);
                    makeMeDirty(allocatedBlock);
                }
            }
            else {
                /**
                 * L2 cache not found*/
                allocateBlockToL1(address);
                CacheBlock allocatedBlock = CacheManagerUtils.getBlockAt(address, L1);
                makeMeDirty(allocatedBlock);
            }
        }
    }

    /**
     * Attempts to write in cache
     * increments the writeCounter
     * if the block is already present -> WRITE HIT
     *      increments the writeHit count
     *      makes it dirty.
     *      updates the block access.
     *      returns true
     * else -> WRITE MISS
     *      increments the writeMiss counter
     *      returns false
     * */
    public boolean isWriteHit(Cache cache, String address){
        cache.setWriteCount(cache.getWriteCount() + 1);

        CacheBlock blockToWrite = CacheManagerUtils.getBlockAt(address, cache);
        if ( Objects.nonNull(blockToWrite) ){
            /**
             * HIT*/
            cache.setWriteHitCount(cache.getWriteHitCount() + 1);
            makeMeDirty(blockToWrite);
            blockToWrite.setLastAccess(Constants.blockAccessCounter++);
            return true;
        }
        //MISS
        cache.setWriteMissCount(cache.getWriteMissCount() + 1);
        return false;
    }

    public void makeMeDirty(CacheBlock block){
        block.setDirty(true);
    }

    public void print(){
        double l1MissRate = 0.0;
        double l2MissRate = 0.0;

        if (L1.getSize() != 0) {
            l1MissRate = ((double)(L1.getReadMissCount() + L1.getWriteMissCount())/(double)(L1.getReadCount()+L1.getWriteCount()));
        }

        if (L2.getSize() != 0) {
            l2MissRate = ((double)(L2.getReadMissCount())/(double)(L2.getReadCount()));
        }

        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE:             "	+	L1.getBlockSize());
        System.out.println("L1_SIZE:               "	+	L1.getSize());
        System.out.println("L1_ASSOC:              "	+	L1.getAssociativity());
        System.out.println("L2_SIZE:               "	+	L2.getSize());
        System.out.println("L2_ASSOC:              "	+	L2.getAssociativity());
        System.out.println("REPLACEMENT POLICY:    "	+	L1.getReplacementPolicy().getDescription());
        System.out.println("INCLUSION PROPERTY:    "	+	L1.getInclusionProperty().getDescription());
        System.out.println("trace_file:            "	+	L1.getTraceFile());

        CacheManagerUtils.printCacheState(L1);

        if(L2.getSize() != 0 )
            CacheManagerUtils.printCacheState(L2);

        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads:        "	+	L1.getReadCount());
        System.out.println("b. number of L1 read misses:  "	+	L1.getReadMissCount());
        System.out.println("c. number of L1 writes:       "	+	L1.getWriteCount());
        System.out.println("d. number of L1 write misses: "	+	L1.getWriteMissCount());
        System.out.println("e. L1 miss rate:              "	+	String.format("%.6f",l1MissRate));
        System.out.println("f. number of L1 writebacks:   "	+	L1.getWriteBackCount());
        System.out.println("g. number of L2 reads:        "	+	L2.getReadCount());
        System.out.println("h. number of L2 read misses:  "	+	L2.getReadMissCount());
        System.out.println("i. number of L2 writes:       "	+	L2.getWriteCount());
        System.out.println("j. number of L2 write misses: "	+	L2.getWriteMissCount());
        System.out.println("k. L2 miss rate:              "	+	String.format("%.6f",l2MissRate));
        System.out.println("l. number of L2 writebacks:   "	+	L2.getWriteBackCount());

        int traffic = (L1.getReadMissCount() + L1.getWriteMissCount() + L1.getWriteBackCount());
        if (L2.getSize() !=0) {
            traffic =   L2.getReadMissCount() + L2.getWriteMissCount() + L2.getWriteBackCount() + trafficCounter;
        }
        System.out.println("m. total memory traffic:      "	+	traffic);
    }
}
