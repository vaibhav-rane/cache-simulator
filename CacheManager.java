import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
// TODO: 9/30/22 Remove unnecessary try-catch blocks after testing
public class CacheManager {
    Cache L1;
    Cache L2;
    int occurrence = 0;
    List<String> opt;
    int trafficCounter = 0;
    int globalIndex = 0;
    int globalRowIdx = 0;
    List<Integer> blankIndices;
    Map<Integer, List<OPTBlock>> setIndexBlockMap;

    private int blockAccessSequence = 0;
    private EvictionManager evictionManager;
    private static String READ = "r";
    private static String WRITE = "w";
    public CacheManager() {
        this.blankIndices = new ArrayList<>();
        this.setIndexBlockMap = new HashMap<>();
        this.evictionManager = new EvictionManager();
    }

    public  void initializeCache(Cache c) {
        int setCount = c.getSetCount();
        for (int i = 0; i < setCount; i++)
            c.getSets().add(new ArrayList<>());
    }

    public Cache getL1() {
        return L1;
    }

    public void setL1(Cache l1) {
        this.L1 = l1;
        l1.setSets(new ArrayList<>());
    }

    public Cache getL2() {
        return L2;
    }

    public void setL2(Cache l2) {
        this.L2 = l2;
        l2.setSets(new ArrayList<>());
    }

    public int getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(int occurrence) {
        this.occurrence = occurrence;
    }

    public List<String> getOpt() {
        return opt;
    }

    public void setOpt(List<String> opt) {
        this.opt = opt;
    }

    public int getTrafficCounter() {
        return trafficCounter;
    }

    public void setTrafficCounter(int trafficCounter) {
        this.trafficCounter = trafficCounter;
    }

    public List<Integer> getBlankIndices() {
        return blankIndices;
    }

    public void setBlankIndices(List<Integer> blankIndices) {
        this.blankIndices = blankIndices;
    }

    public int getGlobalRowIdx() {
        return globalRowIdx;
    }

    public void setGlobalRowIdx(int globalRowIdx) {
        this.globalRowIdx = globalRowIdx;
    }

    public Map<Integer, List<OPTBlock>> getSetIndexBlockMap() {
        return setIndexBlockMap;
    }

    public void setSetIndexBlockMap(Map<Integer, List<OPTBlock>> setIndexBlockMap) {
        this.setIndexBlockMap = setIndexBlockMap;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex = globalIndex;
    }

    /**
     * Reads block from L1 cache
     * If found -> Read HIT
     * If not found, reads from L2
     * Adds the read block to L1
     * updates the block access*/
    public void readFromL1(String address){
        L1.setReadCount(L1.getReadCount() + 1);
        if (CacheManagerUtils.isHit(address, L1)){
            L1.setReadHitCount(L1.getReadHitCount() + 1);
        }
        else {
            L1.setReadMissCount(L1.getReadMissCount() + 1);

            if(L1.hasNextLevel()){
                readFromL2(address);
            }
            allocateBlockToL1(address);
        }
        updateBlockAccess(L1, address, false);
    }

    /**
     * Reads block from L2 cache
     * if found READ HIT
     * if not found, adds the block to L2
     * updated the block access*/
    public void readFromL2(String address){

        if ( ! L1.hasNextLevel()) return;
        L2.setReadCount(L2.getReadCount() + 1);

        if(CacheManagerUtils.isHit(address, L2)){
            L2.setReadHitCount(L2.getReadHitCount() + 1);
        }
        else{
            L2.setReadMissCount(L2.getReadMissCount() + 1);
            allocateBlockToL2(address);
        }
        updateBlockAccess(L2, address, false);

    }

    public void writeToL1(String address){
        if (CacheManagerUtils.isHit(address, L1)){
            L1.setWriteHitCount(L1.getWriteHitCount() + 1);
            CacheBlock block = CacheManagerUtils.getBlockAt(address, L1);
            makeMeDirty(block);
            updateBlockAccess(L1, address, true);
        }
        else {
            L1.setWriteMissCount(L1.getWriteMissCount() + 1);
            readFromL2(address);

            int index = CacheManagerUtils.getSetIndexFor(address, L1);
            String tag = CacheManagerUtils.getTagFor(address, L1);

            CacheBlock block = new CacheBlock();
            block.setTag(tag);

            makeMeDirty(block);

            List<CacheBlock> set = CacheManagerUtils.getSetForSetIndex(index, L1);

            if (CacheManagerUtils.isSetVacantFor(L1, address)){
                set.add(block);
            }
            else{
                evictionManager
                        .getEvictionProcessorFor(L1.getReplacementPolicy(), CacheType.L1)
                        .evict(address, L1);
            }
        }
        L1.setWriteCount(L1.getWriteCount() + 1);
    }

    public void makeMeDirty(CacheBlock block){
        block.setDirty(true);
    }
    public void allocateBlockToL1(String address){
        if (CacheManagerUtils.isSetVacantFor(L1, address)){
            writeBlock(L1, address);
        }
        else {
            evictionManager
                    .getEvictionProcessorFor(L1.getReplacementPolicy(), CacheType.L1)
                    .evict(address, L1);
            writeBlock(L1, address);
        }
    }

    public void allocateBlockToL2(String address){
        if (CacheManagerUtils.isSetVacantFor(L2, address)){
            writeBlock(L2, address);
        }
        else {
            evictionManager
                    .getEvictionProcessorFor(L2.getReplacementPolicy(), CacheType.L2)
                    .evict(address, L2);

            writeBlock(L2, address);
        }
    }

    public void writeBlock(Cache cache, String address){
        int index = CacheManagerUtils.getSetIndexFor(address, cache);
        List<CacheBlock> set = cache.getSets().get(index);
        String tag = CacheManagerUtils.getTagFor(address, cache);

        CacheBlock block = new CacheBlock();
        block.setTag(tag);

        set.add(block);
        cache.setWriteCount(cache.getWriteCount() + 1);
    }

    public void updateBlockAccess(Cache cache, String address, boolean isDirty){
        if (cache.getReplacementPolicy().equals(ReplacementPolicy.LRU)){
            int index = CacheManagerUtils.getSetIndexFor(address, cache);
            String tag = CacheManagerUtils.getTagFor(address, cache);

            List<CacheBlock> set = cache.getSets().get(index);
            for (CacheBlock cb : set){
                if (cb.getTag().equals(tag)){
                    if (isDirty){
                        makeMeDirty(cb);
                    }
                    cb.setLastAccess(Constants.blockAccessCounter++);
                }
            }
        }

    }

    /**
     * Initializes sets and instructions for both the caches
     * Initialized replacement config -> i.e. data structures required for eviction logic
     * */
    public void initializeCaches(){
        List<Cache> caches = Arrays.asList(L1,L2);

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

        initializeInstructionsForCaches();
        initializeReplacementPolicyConfig();
    }

    public void initializeInstructionsForCaches() {
        List<String> instructions = new ArrayList<>();
        String traceFile = L1.getTraceFile();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(traceFile)));
            String instruction;
            while ((instruction = br.readLine()) != null) {
                if(instruction.isEmpty() || instruction.length() < 4) continue;
                instructions.add(instruction);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }

        L1.setInstructions(instructions);
        L2.setInstructions(instructions);
    }

    public void initializeReplacementPolicyConfig(){
        ReplacementPolicy replacementPolicy = L1.getReplacementPolicy();

        switch (replacementPolicy){
            case LRU : {
                initLruConfig();
                break;
            }
            case PLRU : {
                initPlruConfig();
                break;
            }
            case OPT : {
                initOptConfig();
                break;
            }
            default : {
                break;
            }
        }
    }

    public void initLruConfig(){

    }

    public void initPlruConfig(){

    }

    public void initOptConfig(){

    }

    public void executeInstructions(){
        List<String> instructions = L1.getInstructions();

        for( String instruction : instructions ){

            String[] instructionComponents = instruction.split(" ");
            String operation = instructionComponents[0].trim().toLowerCase();
            String unformattedMemoryAddress = instructionComponents[1];
            String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);

            if (READ.equals(operation)){
                readFromL1(memoryAddress);
            }
            else {
                writeToL1(memoryAddress);
            }
        }
    }

    public void read(String address){
        //attempt read from L1 if failed from L2
        boolean cacheHit = readFrom(address, L1);
        if(! cacheHit && L1.getNextLevelCache()!=null){
            readFrom(address, L2);
        }

    }

    public boolean readFrom(String address, Cache cache){
        //return true of HIT else false
        cache.setReadCount(cache.getReadCount() + 1);

        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        String tag = CacheManagerUtils.getTagFor(address, cache);

        List<CacheBlock> set = cache.getSets().get(setIndex);

        if (Objects.isNull(set) || set.size() == 0){
            //MISS
            cache.setReadMissCount(cache.getReadMissCount() + 1);

            if (cache.getNextLevelCache() == null){
                //this means this is L2's read miss
                //need to allocate block in set or if the set is full, evict and add
                writeTo(address, cache);
                if(cache.getPrevLevelCache() != null)
                    writeTo(address, cache.getPrevLevelCache());
            }
            return false;
        }
        for (CacheBlock block : set){
            if (block.getTag().equals(tag)){
                //HIT

                //write this block to the prev level cache
                if(Objects.nonNull(cache.getPrevLevelCache())){
                    writeTo(address, cache.getPrevLevelCache());
                }
                return true;
            }
        }

        return false;
    }

    public void write(String address){
        L1.setWriteCount(L1.getWriteCount() + 1);
        if (CacheManagerUtils.isHit(address, L1)){
            //write hit in L1
            //mark the block as dirty
            makeMeDirty(address, L1);
            //L1.setWriteCount(L1.getWriteCount() + 1);
        }
        else {
            //L1 write miss
            L1.setWriteMissCount(L1.getWriteMissCount() + 1);
            //looking in L2
            if ( L1.getNextLevelCache() != null ){
                //L2.setWriteCount(L2.getWriteCount() + 1);
                if ( CacheManagerUtils.isHit(address, L2) ){
                    //CacheBlock block = CacheManagerUtils.getBlockAt(address, L2);
                    //block.setDirty(true);
                    L2.setWriteCount(L2.getWriteCount() + 1);
                    //bringing the block in L1
                    writeTo(address, L1);
                    //marking the block as dirty in L1
                    makeMeDirty(address, L1);
                    return;
                }
                //L2 miss
                //bringing block in for L2 and L1
                writeTo(address, L1);
                writeTo(address, L2);
            }
        }
    }

    public void makeMeDirty(String address, Cache cache){
        CacheBlock block = CacheManagerUtils.getBlockAt(address, cache);
        block.setDirty(true);
    }
    public void writeTo(String address, Cache cache){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        List<CacheBlock> set = cache.getSets().get(setIndex);

        String tag = CacheManagerUtils.getTagFor(address, cache);


        if (set.size() == cache.getAssociativity()){
            // making space for the new block
            evictFrom(address, cache);
        }

        CacheBlock block = new CacheBlock();
        block.setTag(tag);
        set.add(block);

        cache.setWriteCount(cache.getWriteCount() + 1);
        postProcessWrite(cache, block, setIndex, cache.getReplacementPolicy());
    }

    public void postProcessWrite(Cache cache, CacheBlock writtenBlock, int setIndex, ReplacementPolicy policy){
        if(policy.equals(ReplacementPolicy.LRU)){
            Queue<CacheBlock> LruQueue = cache.getSetLruQueueMap().get(setIndex);
            if(Objects.isNull(LruQueue)){
                LruQueue = new LinkedList<>();
                cache.getSetLruQueueMap().put(setIndex, LruQueue);
            }
            LruQueue.add(writtenBlock);
        }
    }

    public void evictFrom(String address, Cache cache){
        ReplacementPolicy policy = cache.getReplacementPolicy();
        this.evictionManager.getEvictionProcessorFor(policy, cache.getType()).evict(address, cache);
        return;
    }
    public void startSimulation() {
        initializeCaches();
        executeInstructions();

        print();
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

        int total_traffic = (L1.getReadMissCount() + L1.getWriteMissCount() + L1.getWriteBackCount());
        if (L2.getSize() !=0) {
            total_traffic =   L2.getReadMissCount() + L2.getWriteMissCount() + L2.getWriteBackCount() + trafficCounter;
        }
        System.out.println("m. total memory traffic:      "	+	total_traffic);
    }
}
