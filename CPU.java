import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
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
    int trafficCounter = 0;
    private static final String READ = "r";

    public void setTraceFile(String traceFile) {
        this.traceFile = traceFile;
    }
    public void setL1(Cache l1) {
        L1 = l1;
    }

    public void setL2(Cache l2) {
        L2 = l2;
    }

    public void boot(){

        if(L2.getSize() > 0){
            L1.setNextLevelCache(L2);
            L2.setPrevLevelCache(L1);
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

        Constants.preprocessedOPTTrace = new ArrayList<>(instructions);

        /**
         * Preparing Map for OPT*/
        Constants.addressOccurrenceMap = new HashMap<>();
        int i = 0;
        for (String instruction : instructions){
            String address = CacheManagerUtils.getMemoryAddress(instruction);
            List<Integer> occurrences = Constants.addressOccurrenceMap.get(address);
            if (Objects.isNull(occurrences)){
                occurrences = new ArrayList<>();
                Constants.addressOccurrenceMap.put(address, occurrences);
            }
            occurrences.add(i++);
        }
    }

    /**
     * Executes Instructions one-by-one*/
    public void run(){
        for (int i = 0; i < instructions.size(); i++){
            Constants.programCounter = i;
            String instruction = instructions.get(i);

            String operation = CacheManagerUtils.getOperation(instruction);
            String address = CacheManagerUtils.getMemoryAddress(instruction);

            // TODO: 10/5/22 remove after testing
            if (i == 194){
                System.out.println("");
            }

            System.out.println("----------------------------------------");

            if (operation.equals(READ)){
                System.out.println("# "+(i+1)+" : read "+address);
                L1.read(address);
            }
            else{
                System.out.println("# "+(i+1)+" : write "+address);
                L1.write(address);
            }
            //Constants.programCounter++;
        }
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

        int traffic = (L1.getReadMissCount() + L1.getWriteMissCount() + L1.getWriteBackCount());
        if (L2.getSize() !=0) {
            traffic =   L2.getReadMissCount() + L2.getWriteMissCount() + L2.getWriteBackCount() + trafficCounter;
        }
        System.out.println("m. total memory traffic:      "	+	traffic);
    }
}
