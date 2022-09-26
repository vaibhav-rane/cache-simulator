
/**
 * @author Vaibhav R.
 * @created 09/25/2022
 */
public class sim_cache {

    public static void main(String[] args) {

        Integer blockSize = Integer.parseInt(args[0]);
        Integer l1Size = Integer.parseInt(args[1]);
        Integer l1Assoc = Integer.parseInt(args[2]);
        Integer l2Size = Integer.parseInt(args[3]);
        Integer l2Assoc = Integer.parseInt(args[4]);
        Integer replacementPolicy = Integer.parseInt(args[5]);
        Integer inclusionProperty = Integer.parseInt(args[6]);
        String traceFile = args[7];

        Cache L1 = new Cache(blockSize, l1Size, l1Assoc, replacementPolicy, inclusionProperty, traceFile);
        Cache L2 = new Cache(blockSize, l2Size, l2Assoc, replacementPolicy, inclusionProperty, traceFile);

//        new CacheManager(L1, L2);

        CacheSimulatorBuilder simulatorBuilder = new CacheSimulatorBuilder();
        simulatorBuilder
                .init()
                .addCache(L1, CacheType.L1)
                .addCache(L2, CacheType.L2)
                .prepare()
                .start();
    }
}
