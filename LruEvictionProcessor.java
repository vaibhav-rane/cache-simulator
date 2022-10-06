import java.util.List;

/**
 * Created by varane on 10/1/22.
 */
public class LruEvictionProcessor implements EvictionProcessor{
    @Override
    public int getEvictionIndex(String address, Cache cache) {
        return 0;
    }

    @Override
    public CacheType getSupportedType() {
        return null;
    }

    //    @Override
//    public int getEvictionIndex(String address, Cache cache) {
//
//        int setIndex = CacheManagerUtils.getSetIndexFor(address, cache);
//
//        CacheBlock[] set = CacheManagerUtils.getSetForSetIndex(setIndex, cache);
//
//        int minAccess = Integer.MAX_VALUE;
//        int lruIndex = -1;
//
//        for (int i = 0; i < set.length; i++){
//            CacheBlock block = set[i];
//
//            if (block.getLastAccess() < minAccess){
//                minAccess = block.getLastAccess();
//                lruIndex = i;
//            }
//
//        }
//
//        CacheBlock evictedBlock = set[lruIndex];
//        set[lruIndex] = null;
//
//
//
////        Map<Integer, Queue<CacheBlock>> setLruMap = cache.getSetLruQueueMap();
////
////        Queue<CacheBlock> lruQueueForSet = setLruMap.get(setIndex);
////        CacheBlock blockToEvict = lruQueueForSet.remove();
////
////        int evictionBlockIndex = 0;
////        for (int i = 0; i < set.size(); i++){
////            CacheBlock block = set.get(i);
////            if (block.getTag().equals(blockToEvict.getTag())){
////                evictionBlockIndex = i;
////                break;
////            }
////        }
////
////        CacheBlock evictedBlock = set.remove(evictionBlockIndex);
//
//        if (evictedBlock.isDirty()){
//
//            if(cache.getType().equals(CacheType.L2)){
//                handleL2Dirt(evictedBlock, cache);
//            }
//            else{
//                writeBackDirtyBlockToNextLevel(cache, address);
//            }
//        }
//        return 1;
//    }
//
//    @Override
//    public CacheType getSupportedType() {
//        return CacheType.L1;
//    }
//
//    public void handleL2Dirt(CacheBlock evictedBlock, Cache cache){
//        cache.setWriteBackCount(cache.getWriteBackCount() + 1);
//    }
//
//    public void writeBackDirtyBlockToNextLevel(Cache cache, String address){
//        if(cache.getNextLevelCache() != null){
//            Cache L2 = cache.getNextLevelCache();
//            int setIndexL2 = CacheManagerUtils.getSetIndexFor(address, L2);
//            String tag = CacheManagerUtils.getTagFor(address, L2);
//
//            CacheBlock[] setL2 = L2.getSets().get(setIndexL2);
//            for(CacheBlock block : setL2){
//                if (block.getTag().equals(tag)){
//                    //HIT
//                    block.setDirty(true);
//                    updateBlockAccess(L2, address);
//                    L2.setWriteCount(L2.getWriteCount() + 1);
//                    return;
//                }
//            }
//            //miss
//            L2.setWriteMissCount(L2.getWriteMissCount() + 1);
//            //bring block to L2 and mark it dirty
//            if (CacheManagerUtils.isSetVacantFor(L2, address)){
//                writeBlock(L2, address);
//            }
//            else {
//                getEvictionIndex(address, L2);
//                writeBlock(L2, address);
//            }
//
//            updateBlockAccess(L2, address);
//            cache.setWriteBackCount(cache.getWriteBackCount() + 1);
//        }
//    }
//
//    private void writeBlock(Cache cache, String address){
//        int index = CacheManagerUtils.getSetIndexFor(address, cache);
//        CacheBlock[] set = cache.getSets().get(index);
//        String tag = CacheManagerUtils.getTagFor(address, cache);
//
//        CacheBlock block = new CacheBlock();
//        block.setDirty(true);
//        block.setTag(tag);
//
//        set.add(block);
//        cache.setWriteCount(cache.getWriteCount() + 1);
//    }
//
//    public void updateBlockAccess(Cache cache,  String address){
//        if (cache.getReplacementPolicy().equals(ReplacementPolicy.LRU)){
////
////            int index = CacheManagerUtils.getSetIndexFor(address, cache);
////
////            Queue<CacheBlock> lruQueue = cache.getSetLruQueueMap().get(index);
////
////            String tag = CacheManagerUtils.getTagFor(address, cache);
////            while (true){
////                CacheBlock lruBlock = lruQueue.peek();
////                if ( Objects.nonNull(lruBlock) && lruBlock.getTag().equals(tag)){
////                    lruQueue.remove();
////                }
////                else {
////                    break;
////                }
////            }
////
////            CacheBlock block = new CacheBlock();
////            block.setTag(tag);
////
////            lruQueue.add(block);
//
//            int index = CacheManagerUtils.getSetIndexFor(address, cache);
//            String tag = CacheManagerUtils.getTagFor(address, cache);
//            CacheBlock[] set = cache.getSets().get(index);
//            for (CacheBlock cb : set){
//                if (cb.getTag().equals(tag)){
//                    cb.setLastAccess(Constants.blockAccessCounter++);
//                }
//            }
//        }
//
//    }
}
