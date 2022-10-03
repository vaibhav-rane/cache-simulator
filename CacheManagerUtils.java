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

    public static String getMSBPaddingFor(String hex){
        int paddingLength = 32 - hex.length();
        if(paddingLength == 0) return "";
        StringBuilder msbPaddingString = new StringBuilder("");
        for (int i = 1; i <= paddingLength; i++){
            msbPaddingString.append("0");
        }
        return msbPaddingString.toString();
    }

    public static void printCacheState(Cache cache){
        System.out.println("===== "+cache.getType().name()+" contents =====");

        for(int i = 0; i < cache.getSetCount(); i++)
        {
            System.out.print("Set	"+i+": ");
            List<CacheBlock> set = cache.getSets().get(i);

            for(int j = 0; j <set.size() ; j++) {
                System.out.print(toHex(set.get(j).getTag()) + " " + (set.get(j).isDirty()?"D":"")+"	");
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

    public static List<CacheBlock> getSetForSetIndex(int setIndex, Cache cache){
        return cache.getSets().get(setIndex);
    }

    public static void write(){

    }

    public static boolean isHit(String address, Cache cache){
        int setIndex = getSetIndexFor(address, cache);
        List<CacheBlock> set = getSetForSetIndex(setIndex, cache);
        if(Objects.isNull(set) || set.isEmpty()){
            return false;
        }

        String tag = getTagFor(address, cache);
        for (CacheBlock block : set){
            if(block.getTag().equals(tag))
                return true;
            block.setLastAccess(Constants.blockAccessCounter++);
        }
        return false;
    }

    public static CacheBlock getBlockAt(String address, Cache cache){
        int setIndex = getSetIndexFor(address, cache);
        String tag = getTagFor(address, cache);
        List<CacheBlock> set = getSetForSetIndex(setIndex, cache);
        for (CacheBlock block : set){
            if(block.getTag().equals(tag))
                return block;
        }
        return null;
    }

    public static boolean isSetVacantFor(Cache cache, String address){
        int index = getSetIndexFor(address, cache);
        List<CacheBlock> set = cache.getSets().get(index);
        return set.size() < cache.getAssociativity();
    }

    public static boolean isSetVacantFor(Cache cache, CacheBlock block){
        int index = getSetIndexFor(block.getAddress(), cache);
        List<CacheBlock> set = cache.getSets().get(index);
        return set.size() < cache.getAssociativity();
    }

    public static String getOperation(String instruction){
        String[] instructionComponents = instruction.split(" ");
        String operation = instructionComponents[0].trim().toLowerCase();
        return operation;
    }

    public static String getMemoryAddress(String instruction){
        String[] instructionComponents = instruction.split(" ");
        String unformattedMemoryAddress = instructionComponents[1];
        String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);
        return memoryAddress;
    }

    public static void addBlockToCache(Cache cache, CacheBlock block){
        List<CacheBlock> targetSet = cache.getSetAtIndex(getSetIndexFor(block.getAddress(), cache));
        targetSet.add(block);
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
}
