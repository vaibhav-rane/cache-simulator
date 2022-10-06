import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

/**
 * @author varane
 * @created 09/25/2022
 */
public class CacheManagerUtils {
    public static int getIndexBitsCountFor(Cache cache) {
        int indexBitCount = (int) (Math.log(cache.getSetCount()) / Math.log(2));
        return indexBitCount;
    }

    public static int getOffsetBitsCountFor(Cache cache) {
        int offsetBitsCount = (int) (Math.log(cache.getBlockSize()) / Math.log(2));
        return offsetBitsCount;
    }

    public static int getTagSize(Cache cache) {
        int tagBitsCount = 32 - getIndexBitsCountFor(cache) - getOffsetBitsCountFor(cache);
        return tagBitsCount;
    }

    public static String getTagFor(String memoryAddress, Cache cache ) {
        String binaryMemoryAddress = CacheManagerUtils.hexToBinary(memoryAddress);
        int tagSize = CacheManagerUtils.getTagSize(cache);
        String tagBits = binaryMemoryAddress.substring(0, tagSize);
        //todo converting back to hexa tag
        tagBits = new BigInteger(tagBits, 2).toString(16);
        return tagBits;
    }

    public static int getSetIndexFor(String address, Cache cache ) {
        try {
            address = hexToBinary(address);
            int lower = getTagSize(cache);
            int higher = getIndexBitsCountFor(cache);
            String index_bits = address.substring(lower,lower+higher);
            int index = Integer.parseInt(index_bits,2);
            return index;
        }catch(Exception e) {
            return 0;
        }
    }

    public static String hexToBinary(String hexCode){
        String binary = new BigInteger(hexCode.toLowerCase(), 16).toString(2);
        String msbPadding = getMSBPaddingFor(binary);
        String binary32 = msbPadding + binary;
        return binary32;

    }

    public static String getMSBPaddingFor(String address){
        int paddingLength = 32 - address.length();
        if(paddingLength == 0) return "";
        StringBuilder msbPaddingString = new StringBuilder("");
        for (int i = 1; i <= paddingLength; i++){
            msbPaddingString.append("0");
        }
        return msbPaddingString.toString();
    }

    // TODO: 10/4/22 Refactor this 
    public static void printCacheState(Cache cache){
        System.out.println("===== "+cache.getType().name()+" contents =====");

        for(int i = 0; i < cache.getSetCount(); i++)
        {
            System.out.print("Set	"+i+": ");
            CacheBlock[] set = cache.getSets().get(i);

            for(int j = 0; j <set.length ; j++) {
                // TODO: 10/4/22 Null check?
                //System.out.print(toHex(set[j].getTag()) + " " + (set[j].isDirty()?"D":"")+"	");

                //after making getTag() return hex instead of binary
                System.out.print(set[j].getTag() + " " + (set[j].isDirty()?"D":"")+"	");
            }
            System.out.println();
        }
    }

    public static String formatHexAddressTo32BitHexAddress(String input) {
        while (input.length() < 8){
            input = "0" + input;
        }
        return input;
    }

    public static String toHex(String binary) {
        int decimal = Integer.parseInt(binary,2);
        String hexStr = Integer.toString(decimal,16);
        return hexStr;
    }

    public static CacheBlock[] getSetForSetIndex(int setIndex, Cache cache){
        return cache.getSets().get(setIndex);
    }

    public static CacheBlock getBlockAt(String address, Cache cache){
        int setIndex = getSetIndexFor(address, cache);
        String tag = getTagFor(address, cache);
        CacheBlock[] set = getSetForSetIndex(setIndex, cache);
        for (CacheBlock block : set){
            if(Objects.nonNull(block) && block.getTag().equals(tag))
                return block;
        }
        return null;
    }

    public static boolean isSetVacantFor(Cache cache, String address){
        int index = getSetIndexFor(address, cache);
        CacheBlock[] set = cache.getSets().get(index);
        for (CacheBlock block : set){
            if (Objects.isNull(block)){
                return true;
            }
        }
        return false;
    }

    public static boolean isSetVacantFor(Cache cache, CacheBlock block){
        int index = getSetIndexFor(block.getAddress(), cache);
        CacheBlock[] set = cache.getSets().get(index);
        for (CacheBlock cb : set){
            if (Objects.isNull(cb)){
                return true;
            }
        }
        return false;
    }

    public static String getOperation(String instruction){
        String[] instructionComponents = instruction.split(" ");
        String operation = instructionComponents[0].trim().toLowerCase();
        return operation;
    }

    public static String getMemoryAddress(String instruction){
        String[] instructionComponents = instruction.split(" ");
        String unformattedMemoryAddress = instructionComponents[1].trim();
        String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);
        return memoryAddress;
    }

    public static void addBlockToCache(Cache cache, CacheBlock block){
        CacheBlock[] targetSet = cache.getSetAtIndex(getSetIndexFor(block.getAddress(), cache));
        for (int i = 0; i < targetSet.length; i++){
            CacheBlock cb = targetSet[i];
            if (Objects.isNull(cb)){
                targetSet[i] = block;
                break;
            }
        }
    }

    public static CacheBlock createNewCacheBlockFor(Cache cache, String address){
        CacheBlock block = new CacheBlock();
        block.setAddress(address);
        block.setTag(CacheManagerUtils.getTagFor(address, cache));
        block.setDirty(false);
        //todo update the access based on policy
        block.setLastAccess(Constants.blockAccessCounter++);
        return block;
    }

    public static int getLruBlockIndex(String address, Cache cache){
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        int lruBlockIndex = -1;
        int minAccessCounter = Integer.MAX_VALUE;

        for (int i = 0; i < set.length; i++){
            CacheBlock block = set[i];
            if ( block.getLastAccess() < minAccessCounter){
                minAccessCounter = block.getLastAccess();
                lruBlockIndex = i;
            }
        }
        return lruBlockIndex;
    }

    // TODO: 10/5/22 To Review
    public static int getMostRecentFutureDistanceOf(String address){
        for (int i = Constants.programCounter; i < Constants.preprocessedOPTTrace.size(); i++){
            String instruction = Constants.preprocessedOPTTrace.get(i);
            String memoryAddress = CacheManagerUtils.getMemoryAddress(instruction);
            if (memoryAddress.equals(address))
                return i;
        }
        return Integer.MAX_VALUE;
    }
    public static void removeInclusiveBlock(Cache L1, CacheBlock block){
        int index = getSetIndexFor(block.getAddress(), L1);
        CacheBlock[] set = L1.getSets().get(index);
        int removalIndex = -1;
        for (int i = 0; i < set.length; i++){
            CacheBlock blockAtI = set[i];
            if (blockAtI.getTag().equals(block.getTag())){
                removalIndex = i;
                break;
            }
        }
        set[removalIndex] = null;
    }

    public static boolean blockExistsIn(CacheBlock block, Cache cache){
        CacheBlock inclusiveBlock = CacheManagerUtils.getBlockAt(block.getAddress(), cache);
        return Objects.nonNull(inclusiveBlock);
    }
    public static int getIndexOfBlockInSet(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        //CacheBlock inclusiveBlock = CacheManagerUtils.getBlockAt(address, cache);
        String tag = getTagFor(address, cache);
        int index = -1;

        for (int i = 0; i < set.length; i++) {
            CacheBlock blockAtI = set[i];
            if (blockAtI.getTag().equals(tag)) {
                index = i;
                break;
            }
        }
        return index;
    }
}
