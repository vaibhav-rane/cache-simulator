import java.util.*;

/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public class CacheManager {
    Cache L1;
    Cache L2;
    int occurrence = 0;
    List<String> opt;
    int traffic_counter = 0;

    public CacheManager() {
    }

    public  void initializeCache(Cache c) {
        int setCount = c.getSetCount();
        for (int i = 0; i < setCount; i++)
            c.getCache().add(new ArrayList<>());
    }

    public Cache getL1() {
        return L1;
    }

    public void setL1(Cache l1) {
        this.L1 = l1;
        l1.setCache(new ArrayList<>());
    }

    public Cache getL2() {
        return L2;
    }

    public void setL2(Cache l2) {
        this.L2 = l2;
        l2.setCache(new ArrayList<>());
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

    public int getTraffic_counter() {
        return traffic_counter;
    }

    public void setTraffic_counter(int traffic_counter) {
        this.traffic_counter = traffic_counter;
    }

    public List<Integer> getBlankIndices() {
        return blankIndices;
    }

    public void setBlankIndices(List<Integer> blankIndices) {
        this.blankIndices = blankIndices;
    }

    public int getGlblRowIndex() {
        return glblRowIndex;
    }

    public void setGlblRowIndex(int glblRowIndex) {
        this.glblRowIndex = glblRowIndex;
    }

    public Map<Integer, List<OPTBlock>> getDataTable() {
        return dataTable;
    }

    public void setDataTable(Map<Integer, List<OPTBlock>> dataTable) {
        this.dataTable = dataTable;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex = globalIndex;
    }

    void allocate(int ar[], int middle, int index, int lvl, int direction)
    {
        if(lvl == 0)
        {
            ar[index] = direction;
            return;
        }
        else if(middle > index)
        {
            ar[middle] = 0;
            allocate(ar,middle+lvl, index, lvl/2, direction);
        }
        else
        {
            ar[middle] = 1;
            allocate(ar,middle-lvl, index, lvl/2, direction);
        }
    }

    void PLRU(int ar[], int index)
    {
        int temp = index;
        int direction = 0;
        if(temp % 2 != 0)
        {
            direction = 1;
            temp--;
        }
        int middle = (ar.length-1)/2;
        allocate(ar, middle, temp, (middle+1)/2, direction);
    }


    private int deallocation(int middle, int lvl, int[] ar) {
        // TODO Auto-generated method stub
        if(lvl == 0)
        {
            if(ar[middle] == 0)
            {
                ar[middle] = 1;
                return middle+1;
            }
            else
            {
                ar[middle] = 0;
                return middle;
            }
        }
        else if(ar[middle] == 0)
        {
            ar[middle] = 1;
            return deallocation(middle + lvl, lvl/2, ar);
        }
        else
        {
            ar[middle] = 0;
            return deallocation(middle - lvl, lvl/2, ar);
        }

    }


    private int evictionPLRU(int[] ar) {
        // TODO Auto-generated method stub
        int mid = (ar.length-1)/2;
        int levelValue = (mid+1)/2;

        return deallocation(mid,levelValue,ar);
    }


    // TODO: 9/25/22 moved to CacheManagerUtils
    public  String hexToBinary(String hexcode){
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

    // TODO: 9/25/22 moved to CacheManagerUtils
    public int getIndexBitsCountFor(Cache cache) {
        int indexBitCount = (int) (Math.log(cache.getSetCount()) / Math.log(2));
        return indexBitCount;
    }

    // TODO: 9/25/22 moved to CacheManagerUtils
    public  int getOffsetBitsCountFor(Cache cache) {
        int offsetBitsCount = (int) (Math.log(cache.getBlockSize()) / Math.log(2));
        return offsetBitsCount;
    }

    // TODO: 9/25/22 moved to CacheManagerUtils
    public  int getTagBitsFor(Cache cache) {
        int tagBitsCount = 32 - getIndexBitsCountFor(cache) - getOffsetBitsCountFor(cache);
        return tagBitsCount;
    }


    public String format(String input) {
        while (input.length() < 8){
            input = "0" + input;
        }
        return input;
    }


    // TODO: 9/25/22 moved to CacheManagerUtils
    public  int getIndexBitsFor(String address, Cache cache ) {
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

    public  String get_tag_bits(String address, Cache cache ) {
        address = hexToBinary(address);
        return address.substring( 0, getTagBitsFor(cache));

    }

    List<Integer> blankIndices = new ArrayList<>();

    int glblRowIndex = 0;
    public  void read_l1(String data, Cache l1, int counter) {

        String address = data;
        l1.setReadCount(l1.getReadCount() + 1);

        int index_bit = getIndexBitsFor(address,l1 );

        List<CacheBlock> block = l1.getCache().get(index_bit);
        //List<CacheBlock> block = l1.cache.get(index_bit);
        String tag = get_tag_bits(address,l1);

        for(CacheBlock cacheBlock :block)
        {
            if(cacheBlock.getTag().equals(tag))
            {
                //READ HIT
                int value = cacheBlock.getLastAccess();

                for(CacheBlock cb: block)
                {
                    if(cb.getTag().equals(tag)) {

                        cb.setLastAccess(l1.getAssociativity() -1);;
                    }
                    else if(cb.getLastAccess() > value)
                    {
                        cb.setLastAccess(cb.getLastAccess()-1);
                    }


                }
                PLRU(l1.pLRU[index_bit], block.indexOf(cacheBlock));

                return;

            }

        }
        l1.setReadMissCount(l1.getReadMissCount() + 1);
        glblRowIndex = index_bit;

        // MISS IF SPACE AVAILABLE
        if(block.size()<l1.getAssociativity())
        {
            for(CacheBlock d: block)
            {
                d.setLastAccess(d.getLastAccess()-1);
                d.setOPTCounter(d.getOPTCounter()+1);
            }

            if(blankIndices.size() != 0)
            {
                block.add(blankIndices.get(0),new CacheBlock(address, tag, l1.getAssociativity() -1 , false));

                PLRU(l1.pLRU[index_bit], blankIndices.remove(0));


            }else {
                block.add(new CacheBlock(address, tag, l1.getAssociativity() -1 , false));

                PLRU(l1.pLRU[index_bit], block.size()-1);

            }

            if(L2.getSize() != 0)
                read_l2(data, L2);
        }
        else // REPLACEMENT
        {
            updateCache(address, tag,block, false  );
            //OPT(address, tag,block, false, counter  );
        }
    }

    public  void read_l2(String data, Cache l2) {

        String address = data;
        l2.setReadCount(l2.getReadCount() + 1);

        int index_bit = getIndexBitsFor(address,l2 );

        List<CacheBlock> block = l2.getCache().get(index_bit);
        //List<CacheBlock> block = l2.cache.get(index_bit);
        String tag = get_tag_bits(address,l2);

        for(CacheBlock cacheBlock :block)
        {
            if(cacheBlock.getTag().equals(tag))
            {
                //READ HIT
                int value = cacheBlock.getLastAccess();

                for(CacheBlock cb: block)
                {
                    if(cb.getTag().equals(tag)) {

                        cb.setLastAccess(l2.getAssociativity() -1);;
                    }
                    else if(cb.getLastAccess() > value)
                    {
                        cb.setLastAccess(cb.getLastAccess()-1);
                    }


                }

                PLRU(l2.pLRU[index_bit], block.indexOf(cacheBlock));
                return;

            }

        }

        l2.setReadMissCount(l2.getReadMissCount() + 1);



        // MISS IF SPACE AVAILABLE
        if(block.size()<l2.getAssociativity())
        {
            for(CacheBlock d: block)
                d.setLastAccess(d.getLastAccess()-1);

            block.add(new CacheBlock(address, tag, l2.getAssociativity() -1 , false));

            PLRU(l2.pLRU[index_bit], block.size()-1);
        }
        else // REPLACEMENT
        {
            updateCache2(address, tag,block, false  );
        }
    }

    public void write_l1(String data, Cache l1, int counter ) {
        data = format(data);


        String address = data;
        int index_bit = getIndexBitsFor(address,l1);

        List<CacheBlock> block = l1.getCache().get(index_bit);
        //List<CacheBlock> block = l1.cache.get(index_bit);
        String tag = get_tag_bits(address,l1);
        l1.setWriteCount(l1.getWriteCount() + 1);
        for(CacheBlock cacheBlock :block)
        {
            if(cacheBlock.getTag().equals(tag))
            {
                int value = cacheBlock.getLastAccess();

                for(CacheBlock cb: block)
                {
                    if(cb.getTag().equals(tag)) {
                        cb.setDirty(true);
                        cb.setLastAccess(l1.getAssociativity() -1);;
                    }
                    else if(cb.getLastAccess() > value)
                    {
                        cb.setLastAccess(cb.getLastAccess()-1);
                    }


                }

                PLRU(l1.pLRU[index_bit], block.indexOf(cacheBlock));

                return;

            }

        }
        l1.setWriteMissCount(l1.getWriteMissCount() + 1);
        glblRowIndex = index_bit;
        if(block.size()<l1.getAssociativity())
        {
            for(CacheBlock d: block)
            {
                d.setLastAccess(d.getLastAccess()-1);
                d.setOPTCounter(d.getOPTCounter()+1);
            }

            if(blankIndices.size() != 0)
            {
                block.add(blankIndices.get(0),new CacheBlock(address, tag, l1.getAssociativity() -1 , true));

                PLRU(l1.pLRU[index_bit], blankIndices.remove(0));


            }else {
                block.add(new CacheBlock(address, tag, l1.getAssociativity() -1 , true));

                PLRU(l1.pLRU[index_bit], block.size()-1);

            }



            if(L2.getSize() != 0)
                read_l2(data, L2);
        }
        else // REPLACEMENT
        {	updateCache(address, tag,block, true  );
            //OPT(address, tag,block, true , counter );
        }

    }


    public void write_l2(String data, Cache l2 ) {
        data = format(data);


        String address = data;
        int index_bit = getIndexBitsFor(address,l2);
        List<CacheBlock> block = l2.getCache().get(index_bit);
        //List<CacheBlock> block = l2.cache.get(index_bit);
        String tag = get_tag_bits(address,l2);
        l2.setWriteCount(l2.getWriteCount() + 1);

        for(CacheBlock cacheBlock :block)
        {
            if(cacheBlock.getTag().equals(tag))
            {
                int value = cacheBlock.getLastAccess();

                for(CacheBlock cb: block)
                {
                    if(cb.getTag().equals(tag)) {
                        cb.setDirty(true);
                        cb.setLastAccess(l2.getAssociativity() -1);;
                    }
                    else if(cb.getLastAccess() > value)
                    {
                        cb.setLastAccess(cb.getLastAccess()-1);
                    }


                }
                PLRU(l2.pLRU[index_bit], block.indexOf(cacheBlock));
                return;

            }

        }
        l2.setWriteMissCount(l2.getWriteMissCount() + 1);

        if(block.size()<l2.getAssociativity())
        {
            for(CacheBlock d: block)
                d.setLastAccess(d.getLastAccess()-1);

            block.add(new CacheBlock(address, tag, l2.getAssociativity() -1 , true));
            PLRU(l2.pLRU[index_bit], block.size()-1);
        }
        else // REPLACEMENT
        {
            updateCache2(address, tag,	block, true );
        }

    }

    public void updateCache(String address, String tag, List<CacheBlock> l, boolean dirty) {

        int index = 0;


        switch (L1.getReplacementPolicy())
        {
            case 1:{
                int index_bit = getIndexBitsFor(address, L1);

                index = evictionPLRU(L1.pLRU[index_bit]);

                break;
            }
            case 2:{
                index = getEvictedBlockUsingOPT(address,l);

                break;
            }
            default:{

                for(int i=0; i<l.size(); i++)
                {
                    CacheBlock d = l.get(i);

                    if(d.getLastAccess() == 0)
                    {

                        index = i;


                    }
                    else
                    {
                        d.setLastAccess(d.getLastAccess()-1);
                    }
                }

                break;
            }
        }



        CacheBlock temp = l.remove(index);

        if(temp.isDirty())
        {
            if(L2.getSize() != 0) {

                write_l2(temp.getData(), L2);
            }
            L1.setWriteBackCount(L1.getWriteBackCount() + 1);
            //l1.writeBackCount++;
        }

        l.add(index, new CacheBlock(address, tag, L1.getAssociativity() -1 , dirty));

        if(L2.getSize() != 0 )
            read_l2(address, L2);

    }

    private int getEvictedBlockUsingOPT(String address,  List<CacheBlock> l) {
        // TODO Auto-generated method stub

        int returnIndex = 0;

        int arr[]=new int [l.size()];

        Arrays.fill(arr, Integer.MAX_VALUE);

        for(int i=0; i<arr.length; i++)
        {
            CacheBlock d= l.get(i);

            List<OPTBlock> li = dataTable.get(glblRowIndex);

            for(int j=0; j<li.size(); j++)
            {
                OPTBlock temp = li.get(j);

                if(temp.getIndex() > globalIndex)
                {
                    String tag = get_tag_bits(temp.getData(), L1);
                    if(d.getTag().equals(tag))
                    {
                        arr[i] = temp.getIndex() - globalIndex;
                        break;
                    }
                }
            }

        }

        int max = -1;
        for(int i:arr)
            max= Math.max(max, i);

        for(int i=0;i<l.size();i++)
        {
            if(arr[i] == max)
                return i;
        }

        return 0;
    }

    public void updateCache2(String address, String tag, List<CacheBlock> l, boolean dirty) {

        int index = 0;

        switch(L2.getReplacementPolicy())
        {
            case 1:{
                int index_bit = getIndexBitsFor(address, L2);

                index = evictionPLRU(L2.pLRU[index_bit]);
                break;
            }
            case 2:{
                break;
            }
            default:{
                for(int i=0; i<l.size(); i++)
                {
                    CacheBlock d = l.get(i);
                    if(d.getLastAccess() == 0)
                    {

                        index = i;


                    }
                    else
                        d.setLastAccess(d.getLastAccess()-1);
                }

                break;
            }

        }




        CacheBlock temp = l.remove(index);

        if(temp.isDirty())
        {
            L2.setWriteBackCount(L2.getWriteBackCount() + 1);
            //l2.writeBackCount++;
        }


        l.add(index, new CacheBlock(address, tag, L2.getAssociativity() -1 , dirty));

        if(L2.getInclusionProperty() == 1)
        {
            evictFromL1Cache(temp);
        }

    }

    public void evictFromL1Cache(CacheBlock temp) {
        // TODO Auto-generated method stub
        int index_bit = getIndexBitsFor(temp.getData(), L1);
        String tag = get_tag_bits(temp.getData(), L1);

        List<CacheBlock> li = L1.getCache().get(index_bit);
        //List<CacheBlock> li = l1.cache.get(index_bit);

        for(CacheBlock cacheBlock : li)
        {
            if(cacheBlock.getTag().equals(tag)) {

                int index = li.indexOf(cacheBlock);
                blankIndices.add(index);
                CacheBlock traffic = li.remove(index);
                if (traffic.isDirty()) {
                    traffic_counter++;
                }
                break;
            }
        }
    }

    // TODO: 9/26/22 Migrated to CacheManagerUtils
    public void printCache() {

        System.out.println("===== L1 contents =====");

        for(int i = 0; i < L1.getSetCount(); i++)
        {

            System.out.print("Set	"+i+": ");
            List<CacheBlock> row = L1.getCache().get(i);

            for(int j = 0; j <row.size() ; j++) {
                System.out.print(toHex(row.get(j).getTag())+" "+(row.get(j).isDirty()?"D":"")+"	");
            }
            System.out.println();
        }

    }

    // TODO: 9/26/22 Migrated to CacheManagerUtils
    public void printCache2() {

        System.out.println("===== L2 contents =====");

        for(int i = 0; i < L2.getSetCount(); i++)
        {

            System.out.print("Set	"+i+": ");
            List<CacheBlock> row = L2.getCache().get(i);

            for(int j = 0; j <row.size() ; j++) {
                System.out.print(toHex(row.get(j).getTag())+" "+(row.get(j).isDirty()?"D":"")+"	");
            }
            System.out.println();
        }

    }

    class OPTBlock{
        String data;
        int index;



        public OPTBlock(String data, int index) {

            this.data = data;
            this.index = index;
        }
        public String getData() {
            return data;
        }
        public void setData(String data) {
            this.data = data;
        }
        public int getIndex() {
            return index;
        }
        public void setIndex(int index) {
            this.index = index;
        }


    }

    Map<Integer, List<OPTBlock>> dataTable = new HashMap<>();
    int globalIndex = 0;
    public void insert(Cache l1) {
        // TODO Auto-generated method stub
        opt = new ArrayList<String>();
        int i=0;
        for(String str: l1.getInputData())
        {


            try {
                str = str.split(" ")[1];
                str = format(str);
                int index = getIndexBitsFor(str, l1);
                if(!dataTable.containsKey(index))
                    dataTable.put(index, new ArrayList<>());
                dataTable.get(index).add(new OPTBlock(str,i++));
            }catch(Exception ignored) {}
        }

        for(String str : l1.getInputData())
        {
            try {
                str = str.split(" ")[1];
                str = format(str);
                str = get_tag_bits(str,l1);
                opt.add(str);
            } catch (Exception e) {
                continue;
            }

        }
        for(String str:l1.getInputData())
        {
            try {

                boolean read = str.split(" ")[0].toLowerCase().contains("r"); //true for read

                str = str.split(" ")[1];
                str = format(str);
                occurrence++;

                if(read)
                    read_l1(str,l1, occurrence);
                else
                    write_l1(str, l1, occurrence);

                globalIndex++;

            } catch (Exception e) {
                continue;
            }


        }
        double l1_miss_rate = 0.0;
        double l2_miss_rate = 0.0;

        if (l1.getSize() != 0) {
            l1_miss_rate = ((double)(l1.getReadMissCount() + l1.getWriteMissCount())/(double)(l1.getReadCount()+l1.getWriteCount()));
        }

        if (L2.getSize() != 0) {
            l2_miss_rate = ((double)(L2.getReadMissCount())/(double)(L2.getReadCount()));
        }

        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE:             "	+	l1.getBlockSize());
        System.out.println("L1_SIZE:               "	+	l1.getSize());
        System.out.println("L1_ASSOC:              "	+	l1.getAssociativity());
        System.out.println("L2_SIZE:               "	+	L2.getSize());
        System.out.println("L2_ASSOC:              "	+	L2.getAssociativity());
        System.out.println("REPLACEMENT POLICY:    "	+	(l1.getReplacementPolicy() == 0?"LRU":(l1.getReplacementPolicy() == 1?"Pseudo-LRU":"Optimal")));
        System.out.println("INCLUSION PROPERTY:    "	+	(l1.getInclusionProperty() == 0?"non-inclusive":"inclusive"));
        System.out.println("trace_file:            "	+	l1.getTraceFile());



        //printCache();
        CacheManagerUtils.printCacheState(L1);

        if(L2.getSize() != 0 )
            //printCache2();
            CacheManagerUtils.printCacheState(L2);

        System.out.println("===== Simulation results (raw) =====");

        System.out.println("a. number of L1 reads:        "	+	l1.getReadCount());
        System.out.println("b. number of L1 read misses:  "	+	l1.getReadMissCount());
        System.out.println("c. number of L1 writes:       "	+	l1.getWriteCount());
        System.out.println("d. number of L1 write misses: "	+	l1.getWriteMissCount());
        System.out.println("e. L1 miss rate:              "	+	String.format("%.6f",l1_miss_rate));
        System.out.println("f. number of L1 writebacks:   "	+	l1.getWriteBackCount());
        System.out.println("g. number of L2 reads:        "	+	L2.getReadCount());
        System.out.println("h. number of L2 read misses:  "	+	L2.getReadMissCount());
        System.out.println("i. number of L2 writes:       "	+	L2.getWriteCount());
        System.out.println("j. number of L2 write misses: "	+	L2.getWriteMissCount());
        System.out.println("k. L2 miss rate:              "	+	String.format("%.6f",l2_miss_rate));
        System.out.println("l. number of L2 writebacks:   "	+	L2.getWriteBackCount());

        int total_traffic = (l1.getReadMissCount() + l1.getWriteMissCount() + l1.getWriteBackCount());
        if (L2.getSize() !=0) {
            total_traffic =   L2.getReadMissCount() + L2.getWriteMissCount() + L2.getWriteBackCount() + traffic_counter;
        }
        System.out.println("m. total memory traffic:      "	+	total_traffic);

    }


    public String toHex(String binary) {
        int decimal = Integer.parseInt(binary,2);
        return Integer.toString(decimal,16);
    }


}
