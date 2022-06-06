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
    public static long getMinimumSequence(long minimumSequence, List<MySequence> dependentSequenceList){
        for (MySequence sequence : dependentSequenceList) {
            long value = sequence.get();
            minimumSequence = Math.min(minimumSequence, value);
        }

        return minimumSequence;
    }

    /**
     * 获得传入的序列集合中最小的一个序列号
     * @param dependentSequenceList 依赖的序列集合
     * */
    public static long getMinimumSequence(List<MySequence> dependentSequenceList){
        // Long.MAX_VALUE作为上界，即使dependentSequenceList为空，也会返回一个Long.MAX_VALUE作为最小序列号
        return getMinimumSequence(Long.MAX_VALUE, dependentSequenceList);
    }
}
