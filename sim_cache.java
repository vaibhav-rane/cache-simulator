
/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public class sim_cache {
    public static void main(String[] args) {

        Integer blockSize = Integer.parseInt(args[0]);
        Integer l1Size = Integer.parseInt(args[1]);
        Integer l1Associativity = Integer.parseInt(args[2]);
        Integer l2Size = Integer.parseInt(args[3]);
        Integer l2Associativity = Integer.parseInt(args[4]);
        Integer replacementPolicy = Integer.parseInt(args[5]);
        Integer inclusionProperty = Integer.parseInt(args[6]);
        String traceFile = args[7];


        Cache L1 = new CacheBuilder()
                .builder()
                .blockSize(blockSize)
                .size(l1Size)
                .associativity(l1Associativity)
                .replacementPolicy(replacementPolicy)
                .inclusiveProperty(inclusionProperty)
                .traceFile(traceFile)
                .build();


        Cache L2 = new CacheBuilder()
                .builder()
                .blockSize(blockSize)
                .size(l2Size)
                .associativity(l2Associativity)
                .replacementPolicy(replacementPolicy)
                .inclusiveProperty(inclusionProperty)
                .traceFile(traceFile)
                .build();

        L1.setType(CacheType.L1);
        L2.setType(CacheType.L2);

        L1.setNextLevelCache(L2);
        L2.setPrevLevelCache(L1);

        CPU cpu = new CPU();
        cpu.setTraceFile(traceFile);
        cpu.setL1(L1);
        cpu.setL2(L2);

        cpu.boot();
        cpu.run();

//        CacheSimulatorBuilder simulatorBuilder = new CacheSimulatorBuilder();
//        simulatorBuilder
//                .initSimulation()
//                .addCache(L1, CacheType.L1)
//                .addCache(L2, CacheType.L2)
//                .prepare()
//                .start();
    }
}
