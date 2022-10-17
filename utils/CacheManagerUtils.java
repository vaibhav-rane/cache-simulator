package utils;

import constants.Constants;
import core.Cache;
import core.CacheBlock;

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 * Utility class to perform frequent cache operations
 */
public class CacheManagerUtils {
    /**
     * @apiNote returns number of index bits required to uniquely identify each set of the supplied cache
     * Simply computes log(#set). e.g. if #set = 8, returns 3*/
    public static int getIndexBitsCountFor(Cache cache) {
        int indexBitCount = (int) (Math.log(cache.getSetCount()) / Math.log(2));
        return indexBitCount;
    }

    /**
     * @apiNote returns number of bits required to uniquely identify each byte in a block for the supplied cache
     * Computes log(#blocksize)*/
    public static int getOffsetBitsCountFor(Cache cache) {
        int offsetBitsCount = (int) (Math.log(cache.getBlockSize()) / Math.log(2));
        return offsetBitsCount;
    }

    /**
     * @apiNote returns tag size for blocks of the supplied cache
     * 32 - (index bits count) - (offset bits count)*/
    public static int getTagSize(Cache cache) {
        int tagBitsCount = 32 - getIndexBitsCountFor(cache) - getOffsetBitsCountFor(cache);
        return tagBitsCount;
    }

    /**
     * @apiNote computes tag String for the memory block corresponding to the supplied address and the cache that will uniquely identify the block.
     * */
    public static String getTagFor(String memoryAddress, Cache cache ) {
        String binaryMemoryAddress = CacheManagerUtils.hexToBinary(memoryAddress);
        int tagSize = CacheManagerUtils.getTagSize(cache);
        String tagBits = binaryMemoryAddress.substring(0, tagSize);
        tagBits = new BigInteger(tagBits, 2).toString(16);
        return tagBits;
    }

    /**
     * @apiNote Returns set index for the supplied address in the supplied cache.*/
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

    /**
     * @apiNote Converts Hexadecimal address to  32 bit Binary String.
     * Makes sure if the translated binary address is 32 bit long. if not, adds MSB padding.
     * */
    public static String hexToBinary(String hexCode){
        String binary = new BigInteger(hexCode.toLowerCase(), 16).toString(2);
        String msbPadding = getMSBPaddingFor(binary);
        String binary32 = msbPadding + binary;
        return binary32;
    }

    /**
     * @apiNote Returns MSB padding string for the supplied address to ensure its 32 bit long*/
    public static String getMSBPaddingFor(String address){
        int paddingLength = 32 - address.length();
        if(paddingLength == 0) return "";
        StringBuilder msbPaddingString = new StringBuilder("");
        for (int i = 1; i <= paddingLength; i++){
            msbPaddingString.append("0");
        }
        return msbPaddingString.toString();
    }

    /**
     * @apiNote Prints the contents of the supplied cache
     * includes cache type, contents of each set (blocks), and whether the blocks are clear or dirty*/
    public static void printCacheState(Cache cache){
        System.out.println("===== " + cache.getType().name() + " contents =====");

        for(int i = 0; i < cache.getSetCount(); i++)
        {
            System.out.print("Set	"+i+": ");
            CacheBlock[] set = cache.getSets().get(i);

            for(int j = 0; j < set.length ; j++) {
                if (set[j] != null)
                    System.out.print( set[j].getTag() + " " + ( set[j].isDirty() ? "D" : "" ) + " ");
            }
            System.out.println();
        }
    }

    /**
     * @apiNote Converts the supplied hexadecimal address to 32 bits hexadecimal address.
     * One hex character = 1 nibble = 4 bits. Adds MSB 0s to make it 8 character long (32 bits)*/
    public static String formatHexAddressTo32BitHexAddress(String input) {
        while (input.length() < 8){
            input = "0" + input;
        }
        return input;
    }

    /**
     * @apiNote Converts 32 bits binary address to 32 bits hexadecimal address*/
    public static String binary32toHex32(String binary) {
        int decimal = Integer.parseInt(binary,2);
        String hexadecimal = Integer.toString(decimal,16);
        return hexadecimal;
    }

    /**
     * @apiNote Returns the set in the supplied cache at the supplied index*/
    public static CacheBlock[] getSetForSetIndex(int setIndex, Cache cache){
        return cache.getSets().get(setIndex);
    }

    /**
     * @apiNote Returns cache block corresponding to the supplied address in the supplied cache*/
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

    /**
     * @apiNote Decodes the supplied instruction and returns the operation
     * the input instruction is in format: <operation> <address>
     * Splits the instruction and simply returns the first string from the split array */
    public static String getOperation(String instruction){
        String[] instructionComponents = instruction.split(" ");
        String operation = instructionComponents[0].trim().toLowerCase();
        return operation;
    }

    /**
     * @apiNote Returns hexadecimal memory address after excluding the offset bits.
     * - Translates supplied address to 32 bits binary address
     * - Removes offset bits
     * - Adds 0s in place of removed offset bits
     * - Converts it back to hexadecimal address and returns it back*/
    public static String getAddressAfterExcludingBlockOffset(String instruction, int blockSize){
        String[] instructionComponents = instruction.split(" ");
        String unformattedMemoryAddress = instructionComponents[1].trim();
        String memoryAddress = CacheManagerUtils.formatHexAddressTo32BitHexAddress(unformattedMemoryAddress);

        String binary = hexToBinary(memoryAddress);
        int offsetBitsCount = (int) (Math.log(blockSize) / Math.log(2));
        String binaryAddressWithoutOffsetBits = binary.substring(0, binary.length() - offsetBitsCount);
        String lsbPadding = getLSBPadding(offsetBitsCount);

        String binaryAddressWithLsbPadding = binaryAddressWithoutOffsetBits + lsbPadding;
        String processedHexAddress = binary32toHex32(binaryAddressWithLsbPadding);
        return processedHexAddress;
    }

    /**
     * @apiNote Returns 0 padded string with length = offset bits*/
    public static String getLSBPadding(int offsetBitCount){
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= offsetBitCount; i++){
            sb.append("0");
        }
        return sb.toString();
    }

    /**
     * @apiNote Creates a new cache block for the supplied address and cache*/
    public static CacheBlock createNewCacheBlockFor(Cache cache, String address){
        CacheBlock block = new CacheBlock();
        block.setAddress(address);
        block.setTag(CacheManagerUtils.getTagFor(address, cache));
        block.setDirty(false);
        block.setLastAccess(Constants.blockAccessCounter++);
        return block;
    }

    /**
     * @apiNote Returns index of LRU block in the supplied cache in the set corresponding to the supplied address.*/
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

    /**
     * @apiNote Checks if the supplied block exists in the supplied cache*/
    public static boolean blockExistsIn(CacheBlock block, Cache cache){
        CacheBlock inclusiveBlock = CacheManagerUtils.getBlockAt(block.getAddress(), cache);
        return Objects.nonNull(inclusiveBlock);
    }

    /**
     * @apiNote Returns the index of the block corresponding to the supplied address in an appropriate set of the supplied cache.
     * Returns -1 if not found*/
    public static int getIndexOfBlockInSet(String address, Cache cache) {
        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);

        String tag = getTagFor(address, cache);
        int index = -1;

        for (int i = 0; i < set.length; i++) {
            CacheBlock blockAtI = set[i];
            if (Objects.nonNull(blockAtI) && blockAtI.getTag().equals(tag)) {
                index = i;
                break;
            }
        }
        return index;
    }
}
