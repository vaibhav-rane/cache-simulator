import java.math.BigInteger;
import java.util.List;

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

    public static int getTagBitsFor(Cache cache) {
        int tagBitsCount = 32 - getIndexBitsCountFor(cache) - getOffsetBitsCountFor(cache);
        return tagBitsCount;
    }

    public static int getIndexBitsFor(String address32, Cache cache ) {
        try {
            address32 = hexToBinary(address32);
            int lower = getTagBitsFor(cache);
            int higher = getIndexBitsCountFor(cache);
            String index_bits = address32.substring(lower,lower+higher);
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
            List<CacheBlock> set = cache.getCache().get(i);

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

    public static void write(){

    }
}
