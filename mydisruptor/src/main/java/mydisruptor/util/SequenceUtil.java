package mydisruptor.util;

import mydisruptor.MySequence;

import java.util.List;

/**
 * 序列号工具类
 * */
public class SequenceUtil {

    /**
     * 从依赖的序列集合dependentSequence和申请的最小序列号minimumSequence中获得最小的序列号
     * @param minimumSequence 申请的最小序列号
     * @param dependentSequenceList 依赖的序列集合
     * */
    public static long getMinimumSequence(long minimumSequence, MySequence[] dependentSequenceList){
        for (MySequence mySequence : dependentSequenceList) {
            long value = mySequence.get();
            minimumSequence = Math.min(minimumSequence, value);
        }

//        for(int i=0; i<dependentSequenceList.length; i++){
//            long value = dependentSequenceList[i].get();
//            minimumSequence = Math.min(minimumSequence, value);
//        }

        return minimumSequence;
    }

    /**
     * 获得传入的序列集合中最小的一个序列号
     * @param dependentSequenceList 依赖的序列集合
     * */
    public static long getMinimumSequence(MySequence[] dependentSequenceList){
        // Long.MAX_VALUE作为上界，即使dependentSequenceList为空，也会返回一个Long.MAX_VALUE作为最小序列号
        return getMinimumSequence(Long.MAX_VALUE, dependentSequenceList);
    }
}
