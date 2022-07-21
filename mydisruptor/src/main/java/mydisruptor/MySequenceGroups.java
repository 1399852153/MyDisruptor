package mydisruptor;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * 更改Sequence数组工具类（仿Disruptor.SequenceGroups）
 * 注意：实现中cas的插入/删除机制在MyDisruptor中是不必要的，因为MyDisruptor不支持在运行时动态的注册新消费者
 *     只是为了和Disruptor的实现保持一致，可以更好的说明实现原理才这样做的，本质上只需要支持sequence数组扩容/缩容即可
 * */
public class MySequenceGroups {

    /**
     * 将新的需要注册的序列集合加入到holder对象的对应sequence数组中（sequencesToAdd集合）
     * */
    public static <T> void addSequences(
            final T holder,
            final AtomicReferenceFieldUpdater<T, MySequence[]> updater,
            final MySequence currentProducerSequence,
            final MySequence... sequencesToAdd) {
        long cursorSequence;
        MySequence[] updatedSequences;
        MySequence[] currentSequences;

        do {
            // 获得数据持有者当前的数组引用
            currentSequences = updater.get(holder);
            // 将原数组中的数据复制到新的数组中
            updatedSequences = Arrays.copyOf(currentSequences, currentSequences.length + sequencesToAdd.length);
            cursorSequence = currentProducerSequence.get();

            int index = currentSequences.length;
            // 每个新添加的sequence值都以当前生产者的序列为准
            for (MySequence sequence : sequencesToAdd) {
                sequence.set(cursorSequence);
                // 新注册sequence放入数组中
                updatedSequences[index++] = sequence;
            }
            // cas的将新数组赋值给对象，允许disruptor在运行时并发的注册新的消费者sequence集合
            // 只有cas赋值成功才会返回，失败的话会重新获取最新的currentSequences，重新构建、合并新的updatedSequences数组
        } while (!updater.compareAndSet(holder, currentSequences, updatedSequences));

        // 新注册的消费者序列，再以当前生产者序列为准做一次最终修正
        cursorSequence = currentProducerSequence.get();
        for (MySequence sequence : sequencesToAdd) {
            sequence.set(cursorSequence);
        }
    }

    /**
     * 从holder的sequence数组中删除掉一个sequence
     * */
    public static <T> void removeSequence(
            final T holder,
            final AtomicReferenceFieldUpdater<T, MySequence[]> sequenceUpdater,
            final MySequence sequenceNeedRemove) {
        int numToRemove;
        MySequence[] oldSequences;
        MySequence[] newSequences;

        do {
            // 获得数据持有者当前的数组引用
            oldSequences = sequenceUpdater.get(holder);
            // 获得需要从数组中删除的sequence个数
            numToRemove = countMatching(oldSequences, sequenceNeedRemove);

            if (0 == numToRemove) {
                // 没找到需要删除的Sequence,直接返回
                return;
            }

            final int oldSize = oldSequences.length;
            // 构造新的sequence数组
            newSequences = new MySequence[oldSize - numToRemove];

            for (int i = 0, pos = 0; i < oldSize; i++) {
                // 将原数组中的sequence复制到新数组中
                final MySequence testSequence = oldSequences[i];
                if (sequenceNeedRemove != testSequence) {
                    // 只复制不需要删除的数据
                    newSequences[pos++] = testSequence;
                }
            }
        } while (!sequenceUpdater.compareAndSet(holder, oldSequences, newSequences));
    }

    private static int countMatching(MySequence[] values, final MySequence toMatch) {
        int numToRemove = 0;
        for (MySequence value : values) {
            if (value == toMatch) {
                // 比对Sequence引用，如果和toMatch相同，则需要删除
                numToRemove++;
            }
        }
        return numToRemove;
    }
}
