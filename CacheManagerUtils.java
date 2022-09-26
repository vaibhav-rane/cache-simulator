import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static int getIndexBitsFor(String address, Cache cache ) {
        try {
            address = hexToBinary(address);
            int lower = getTagBitsFor(cache);
            int higher = getIndexBitsCountFor(cache);
            String index_bits = address.substring(lower,lower+higher);
            int index = Integer.parseInt(index_bits,2);
            return index;
        }catch(Exception e) {
            return 0;
        }
    }

    public static String hexToBinary(String hexcode){
        String binary = "";
        hexcode = hexcode.toLowerCase();

        Map<Character, String> hashMap = new HashMap<>();

        hashMap.put('0', "0000");
        hashMap.put('1', "0001");
        hashMap.put('2', "0010");
        hashMap.put('3', "0011");
        hashMap.put('4', "0100");
        hashMap.put('5', "0101");
        hashMap.put('6', "0110");
        hashMap.put('7', "0111");
        hashMap.put('8', "1000");
        hashMap.put('9', "1001");
        hashMap.put('a', "1010");
        hashMap.put('b', "1011");
        hashMap.put('c', "1100");
        hashMap.put('d', "1101");
        hashMap.put('e', "1110");
        hashMap.put('f', "1111");

        int i;
        char ch;

        for (i = 0; i < hexcode.length(); i++) {
            ch = hexcode.charAt(i);
            binary += hashMap.get(ch);
        }

        return binary;
    }

    public static void printCacheState(Cache cache){
        System.out.println("===== "+cache.getType().name()+" contents =====");

        for(int i = 0; i < cache.getSetCount(); i++)
        {
            System.out.print("Set	"+i+": ");
            List<CacheBlock> row = cache.getCache().get(i);

            for(int j = 0; j <row.size() ; j++) {
                System.out.print(toHex(row.get(j).getTag())+" "+(row.get(j).isDirty()?"D":"")+"	");
            }
            System.out.println();
        }
    }

    public static String toHex(String binary) {
        int decimal = Integer.parseInt(binary,2);
        String hexStr = Integer.toString(decimal,16);
        return hexStr;
    }
}
