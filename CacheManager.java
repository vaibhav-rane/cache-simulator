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
    private EvictionProcessorFactory evictionProcessorFactory;
    private static String READ = "r";
    private static String WRITE = "w";
    public CacheManager() {
        this.blankIndices = new ArrayList<>();
        this.setIndexBlockMap = new HashMap<>();
        this.evictionProcessorFactory = new EvictionProcessorFactory();
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
                evictionProcessorFactory
                        .getEvictionProcessor(L1.getReplacementPolicy())
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
            evictionProcessorFactory
                    .getEvictionProcessor(L1.getReplacementPolicy())
                    .evict(address, L1);
            writeBlock(L1, address);
        }
    }

    public void allocateBlockToL2(String address){
        if (CacheManagerUtils.isSetVacantFor(L2, address)){
            writeBlock(L2, address);
        }
        else {
            evictionProcessorFactory
                    .getEvictionProcessor(L2.getReplacementPolicy())
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

//    void allocate(int ar[], int middle, int index, int lvl, int direction)
//    {
//        if(lvl == 0)
//        {
//            ar[index] = direction;
//            return;
//        }
//        else if(middle > index)
//        {
//            ar[middle] = 0;
//            allocate(ar,middle+lvl, index, lvl/2, direction);
//        }
//        else
//        {
//            ar[middle] = 1;
//            allocate(ar,middle-lvl, index, lvl/2, direction);
//        }
//    }
//
//    void PLRU(int ar[], int index)
//    {
//        int temp = index;
//        int direction = 0;
//        if(temp % 2 != 0)
//        {
//            direction = 1;
//            temp--;
//        }
//        int middle = (ar.length-1)/2;
//        allocate(ar, middle, temp, (middle+1)/2, direction);
//    }
//
//
//    private int deallocation(int middle, int lvl, int[] ar) {
//        // TODO Auto-generated method stub
//        if(lvl == 0)
//        {
//            if(ar[middle] == 0)
//            {
//                ar[middle] = 1;
//                return middle+1;
//            }
//            else
//            {
//                ar[middle] = 0;
//                return middle;
//            }
//        }
//        else if(ar[middle] == 0)
//        {
//            ar[middle] = 1;
//            return deallocation(middle + lvl, lvl/2, ar);
//        }
//        else
//        {
//            ar[middle] = 0;
//            return deallocation(middle - lvl, lvl/2, ar);
//        }
//
//    }
//
//
//    private int evictionPLRU(int[] ar) {
//        // TODO Auto-generated method stub
//        int mid = (ar.length-1)/2;
//        int levelValue = (mid+1)/2;
//
//        return deallocation(mid,levelValue,ar);
//    }

//    /**
//     * @apiNote read block at address
//     * common method to read instead of two separate methods for each cache*/
//    public void read(String address, Cache cache){
//        int readCount = cache.getReadCount();
//        readCount++;
//        cache.setReadCount(readCount);
//
//        int index = CacheManagerUtils.getIndexBitsFor(address, cache);
//        int tag = CacheManagerUtils.getTagBitsFor(cache);
//
//        List<CacheBlock> set = cache.getSets().get(index);
//
//        for(CacheBlock block : set){
//            if(block.getTag().equals(tag)){
//                //hit
//                int value = block.getLastAccess();
//
//
//            }
//        }
//    }
//    public void readFromL1(String address) {
//
//        L1.setReadCount(L1.getReadCount() + 1);
//        //getting index bits to get appropriate set
//        int index_bit = CacheManagerUtils.getIndexBitsFor(address,L1 );
//        //getting set at index
//        List<CacheBlock> setAtIndex = L1.getSets().get(index_bit);
//        //computing tag from the address
//        String tag = getTagBitsFor(address,L1);
//        //checking if the block with tag exists in the set -> HIT
//        for(CacheBlock block : setAtIndex)
//        {
//            if(block.getTag().equals(tag))
//            {
//                //HIT
//                int value = block.getLastAccess();
//
//                for(CacheBlock cb: setAtIndex)
//                {
//                    if(cb.getTag().equals(tag)) {
//
//                        cb.setLastAccess(L1.getAssociativity() -1);;
//                    }
//                    else if(cb.getLastAccess() > value)
//                    {
//                        cb.setLastAccess(cb.getLastAccess()-1);
//                    }
//                }
//                PLRU(L1.PLRU[index_bit], setAtIndex.indexOf(block));
//                return;
//            }
//
//        }
//        L1.setReadMissCount(L1.getReadMissCount() + 1);
//        globalRowIdx = index_bit;
//
//        // MISS IF SPACE AVAILABLE
//        if(setAtIndex.size()<L1.getAssociativity())
//        {
//            for(CacheBlock d: setAtIndex)
//            {
//                d.setLastAccess(d.getLastAccess()-1);
//                d.setOPTCounter(d.getOPTCounter()+1);
//            }
//
//            if(blankIndices.size() != 0)
//            {
//                setAtIndex.add(blankIndices.get(0),new CacheBlock(address, tag, L1.getAssociativity() -1 , false));
//
//                PLRU(L1.PLRU[index_bit], blankIndices.remove(0));
//
//
//            }else {
//                setAtIndex.add(new CacheBlock(address, tag, L1.getAssociativity() -1 , false));
//
//                PLRU(L1.PLRU[index_bit], setAtIndex.size()-1);
//
//            }
//
//            if(L2.getSize() != 0)
//                read_l2(address, L2);
//        }
//        else // REPLACEMENT
//        {
//            updateCache(address, tag,setAtIndex, false  );
//            //OPT(address, tag,block, false, counter  );
//        }
//    }
//
//    public  void read_l2(String data, Cache l2) {
//
//        String address = data;
//        l2.setReadCount(l2.getReadCount() + 1);
//
//        int index_bit = CacheManagerUtils.getIndexBitsFor(address,l2 );
//
//        List<CacheBlock> block = l2.getSets().get(index_bit);
//        //List<CacheBlock> block = l2.cache.get(index_bit);
//        String tag = getTagBitsFor(address,l2);
//
//        for(CacheBlock cacheBlock :block)
//        {
//            if(cacheBlock.getTag().equals(tag))
//            {
//                //READ HIT
//                int value = cacheBlock.getLastAccess();
//
//                for(CacheBlock cb: block)
//                {
//                    if(cb.getTag().equals(tag)) {
//
//                        cb.setLastAccess(l2.getAssociativity() -1);;
//                    }
//                    else if(cb.getLastAccess() > value)
//                    {
//                        cb.setLastAccess(cb.getLastAccess()-1);
//                    }
//
//
//                }
//
//                PLRU(l2.PLRU[index_bit], block.indexOf(cacheBlock));
//                return;
//
//            }
//
//        }
//
//        l2.setReadMissCount(l2.getReadMissCount() + 1);
//
//
//
//        // MISS IF SPACE AVAILABLE
//        if(block.size()<l2.getAssociativity())
//        {
//            for(CacheBlock d: block)
//                d.setLastAccess(d.getLastAccess()-1);
//
//            block.add(new CacheBlock(address, tag, l2.getAssociativity() -1 , false));
//
//            PLRU(l2.PLRU[index_bit], block.size()-1);
//        }
//        else // REPLACEMENT
//        {
//            updateCache2(address, tag,block, false  );
//        }
//    }
//
//    public void writeToL1(String data, Cache l1) {
//        data = CacheManagerUtils.formatHexAddressTo32BitHexAddress(data);
//
//
//        String address = data;
//        int index_bit = CacheManagerUtils.getIndexBitsFor(address,l1);
//
//        List<CacheBlock> block = l1.getSets().get(index_bit);
//        //List<CacheBlock> block = l1.cache.get(index_bit);
//        String tag = getTagBitsFor(address,l1);
//        l1.setWriteCount(l1.getWriteCount() + 1);
//        for(CacheBlock cacheBlock :block)
//        {
//            if(cacheBlock.getTag().equals(tag))
//            {
//                int value = cacheBlock.getLastAccess();
//
//                for(CacheBlock cb: block)
//                {
//                    if(cb.getTag().equals(tag)) {
//                        cb.setDirty(true);
//                        cb.setLastAccess(l1.getAssociativity() -1);;
//                    }
//                    else if(cb.getLastAccess() > value)
//                    {
//                        cb.setLastAccess(cb.getLastAccess()-1);
//                    }
//
//
//                }
//
//                PLRU(l1.PLRU[index_bit], block.indexOf(cacheBlock));
//
//                return;
//
//            }
//
//        }
//        l1.setWriteMissCount(l1.getWriteMissCount() + 1);
//        globalRowIdx = index_bit;
//        if(block.size()<l1.getAssociativity())
//        {
//            for(CacheBlock d: block)
//            {
//                d.setLastAccess(d.getLastAccess()-1);
//                d.setOPTCounter(d.getOPTCounter()+1);
//            }
//
//            if(blankIndices.size() != 0)
//            {
//                block.add(blankIndices.get(0),new CacheBlock(address, tag, l1.getAssociativity() -1 , true));
//
//                PLRU(l1.PLRU[index_bit], blankIndices.remove(0));
//
//
//            }else {
//                block.add(new CacheBlock(address, tag, l1.getAssociativity() -1 , true));
//
//                PLRU(l1.PLRU[index_bit], block.size()-1);
//
//            }
//
//
//
//            if(L2.getSize() != 0)
//                read_l2(data, L2);
//        }
//        else // REPLACEMENT
//        {	updateCache(address, tag,block, true  );
//            //OPT(address, tag,block, true , counter );
//        }
//
//    }
//
//
//    public void write_l2(String data, Cache l2 ) {
//        data = CacheManagerUtils.formatHexAddressTo32BitHexAddress(data);
//
//
//        String address = data;
//        int index_bit = CacheManagerUtils.getIndexBitsFor(address,l2);
//        List<CacheBlock> block = l2.getSets().get(index_bit);
//        //List<CacheBlock> block = l2.cache.get(index_bit);
//        String tag = getTagBitsFor(address,l2);
//        l2.setWriteCount(l2.getWriteCount() + 1);
//
//        for(CacheBlock cacheBlock :block)
//        {
//            if(cacheBlock.getTag().equals(tag))
//            {
//                int value = cacheBlock.getLastAccess();
//
//                for(CacheBlock cb: block)
//                {
//                    if(cb.getTag().equals(tag)) {
//                        cb.setDirty(true);
//                        cb.setLastAccess(l2.getAssociativity() -1);;
//                    }
//                    else if(cb.getLastAccess() > value)
//                    {
//                        cb.setLastAccess(cb.getLastAccess()-1);
//                    }
//
//
//                }
//                PLRU(l2.PLRU[index_bit], block.indexOf(cacheBlock));
//                return;
//
//            }
//
//        }
//        l2.setWriteMissCount(l2.getWriteMissCount() + 1);
//
//        if(block.size()<l2.getAssociativity())
//        {
//            for(CacheBlock d: block)
//                d.setLastAccess(d.getLastAccess()-1);
//
//            block.add(new CacheBlock(address, tag, l2.getAssociativity() -1 , true));
//            PLRU(l2.PLRU[index_bit], block.size()-1);
//        }
//        else // REPLACEMENT
//        {
//            updateCache2(address, tag,	block, true );
//        }
//
//    }
//
//    public void updateCache(String address, String tag, List<CacheBlock> l, boolean dirty) {
//        int index = 0;
//
//        switch (L1.getReplacementPolicy())
//        {
//            case 1:{
//                int index_bit = CacheManagerUtils.getIndexBitsFor(address, L1);
//
//                index = evictionPLRU(L1.PLRU[index_bit]);
//
//                break;
//            }
//            case 2:{
//                index = getEvictedBlockUsingOPT(l);
//
//                break;
//            }
//            default:{
//
//                for(int i=0; i<l.size(); i++)
//                {
//                    CacheBlock d = l.get(i);
//
//                    if(d.getLastAccess() == 0)
//                    {
//
//                        index = i;
//
//
//                    }
//                    else
//                    {
//                        d.setLastAccess(d.getLastAccess()-1);
//                    }
//                }
//
//                break;
//            }
//        }
//
//
//
//        CacheBlock temp = l.remove(index);
//
//        if(temp.isDirty())
//        {
//            if(L2.getSize() != 0) {
//
//                write_l2(temp.getData(), L2);
//            }
//            L1.setWriteBackCount(L1.getWriteBackCount() + 1);
//            //l1.writeBackCount++;
//        }
//
//        l.add(index, new CacheBlock(address, tag, L1.getAssociativity() -1 , dirty));
//
//        if(L2.getSize() != 0 )
//            read_l2(address, L2);
//
//    }
//
//    private int getEvictedBlockUsingOPT(List<CacheBlock> set) {
//        int arr[]=new int [set.size()];
//        Arrays.fill(arr, Integer.MAX_VALUE);
//
//        for(int i = 0; i < arr.length; i++)
//        {
//            CacheBlock d= set.get(i);
//
//            List<OPTBlock> li = setIndexBlockMap.get(globalRowIdx);
//
//            for(int j=0; j<li.size(); j++)
//            {
//                OPTBlock temp = li.get(j);
//
//                if(temp.getIndex() > globalIndex)
//                {
//                    String tag = getTagBitsFor(temp.getData(), L1);
//                    if(d.getTag().equals(tag))
//                    {
//                        arr[i] = temp.getIndex() - globalIndex;
//                        break;
//                    }
//                }
//            }
//
//        }
//
//        int max = -1;
//        for(int i:arr)
//            max= Math.max(max, i);
//
//        for(int i=0;i<set.size();i++)
//        {
//            if(arr[i] == max)
//                return i;
//        }
//
//        return 0;
//    }
//
//    public void updateCache2(String address, String tag, List<CacheBlock> l, boolean dirty) {
//
//        int index = 0;
//
//        switch(L2.getReplacementPolicy())
//        {
//            case 1:{
//                int index_bit = CacheManagerUtils.getIndexBitsFor(address, L2);
//
//                index = evictionPLRU(L2.PLRU[index_bit]);
//                break;
//            }
//            case 2:{
//                break;
//            }
//            default:{
//                for(int i=0; i<l.size(); i++)
//                {
//                    CacheBlock d = l.get(i);
//                    if(d.getLastAccess() == 0)
//                    {
//                        index = i;
//                    }
//                    else
//                        d.setLastAccess(d.getLastAccess()-1);
//                }
//                break;
//            }
//
//        }
//
//
//
//
//        CacheBlock temp = l.remove(index);
//
//        if(temp.isDirty())
//        {
//            L2.setWriteBackCount(L2.getWriteBackCount() + 1);
//            //l2.writeBackCount++;
//        }
//
//
//        l.add(index, new CacheBlock(address, tag, L2.getAssociativity() -1 , dirty));
//
//        if(L2.getInclusionProperty() == 1)
//        {
//            evictFromL1Cache(temp);
//        }
//
//    }
//
//    public void evictFromL1Cache(CacheBlock block) {
//        int index_bit = CacheManagerUtils.getIndexBitsFor(block.getData(), L1);
//        String tag = getTagBitsFor(block.getData(), L1);
//
//        List<CacheBlock> li = L1.getSets().get(index_bit);
//        //List<CacheBlock> li = l1.cache.get(index_bit);
//
//        for(CacheBlock cacheBlock : li)
//        {
//            if(cacheBlock.getTag().equals(tag)) {
//
//                int index = li.indexOf(cacheBlock);
//                blankIndices.add(index);
//                CacheBlock traffic = li.remove(index);
//                if (traffic.isDirty()) {
//                    trafficCounter++;
//                }
//                break;
//            }
//        }
//    }
//
//    public void bootstrap(){
//        opt = new ArrayList<>();
//        int i = 0;
//        for(String instruction : L1.getInstructions())
//        {
//            try {
//                String[] instructionComponents = instruction.split(" ");
//                String operation = instructionComponents[0].toLowerCase().trim();
//                String unformattedMemoryAddress = instructionComponents[1];
//
//                String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);
//
//                int indexBits = CacheManagerUtils.getIndexBitsFor(memoryAddress, L1);
//
//                List<OPTBlock> blocksForSet = setIndexBlockMap.get(indexBits);
//                if(Objects.isNull(blocksForSet) || blocksForSet.isEmpty()){
//                    blocksForSet = new ArrayList<>();
//                    setIndexBlockMap.put(indexBits, blocksForSet);
//                }
//
//                OPTBlock blockForMemoryAddress = new OPTBlock();
//                blockForMemoryAddress.setData(memoryAddress);
//                blockForMemoryAddress.setIndex(i);
//                i++;
//
//                blocksForSet.add(blockForMemoryAddress);
//
//                String tagBits = getTagBitsFor(memoryAddress, L1);
//                opt.add(tagBits);
//            }catch(Exception ignored) {}
//        }
//    }
//
//    public void executeInstructions(){
//        for(String instruction : L1.getInstructions())
//        {
//            try {
//                String[] instructionComponents = instruction.split(" ");
//                String operation = instructionComponents[0].toLowerCase().trim();
//                String unformattedMemoryAddress = instructionComponents[1];
//                String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);
//
//                if(operation.equals(READ))
//                    readFromL1(memoryAddress);
//                else
//                    writeToL1(memoryAddress, L1);
//
//                globalIndex++;
//
//            } catch (Exception e) {}
//        }
//    }

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
        this.evictionProcessorFactory.getEvictionProcessor(policy).evict(address, cache);
        return;
    }
    public void startSimulation() {
        initializeCaches();
        executeInstructions();

//        System.out.println("L1 Cache: ");
//        System.out.println(L1);
//
//        System.out.println("L2 Cache: ");
//        System.out.println(L2);

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
