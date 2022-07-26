package mydisruptor.dsl;

import mydisruptor.MyEventProcessor;
import mydisruptor.MySequence;
import mydisruptor.MyWorkerPool;

import java.util.*;

/**
 * 维护当前disruptor的所有消费者对象信息的仓库（仿Disruptor.ConsumerRepository）
 */
public class MyConsumerRepository<T> {

    private final ArrayList<MyConsumerInfo> consumerInfos = new ArrayList<>();

    /**
     * 不重写Sequence的hashCode，equals,因为比对的就是原始对象是否相等
     * */
    private final Map<MySequence, MyConsumerInfo> eventProcessorInfoBySequence = new IdentityHashMap<>();

    public ArrayList<MyConsumerInfo> getConsumerInfos() {
        return consumerInfos;
    }

    public void add(final MyEventProcessor processor) {
        final MyEventProcessorInfo<T> consumerInfo = new MyEventProcessorInfo<>(processor);
        eventProcessorInfoBySequence.put(processor.getCurrentConsumeSequence(),consumerInfo);
        consumerInfos.add(consumerInfo);
    }

    public void add(final MyWorkerPool<T> workerPool) {
        final MyWorkerPoolInfo<T> workerPoolInfo = new MyWorkerPoolInfo<>(workerPool);
        for (MySequence sequence : workerPool.getCurrentWorkerSequences()) {
            eventProcessorInfoBySequence.put(sequence, workerPoolInfo);
        }
        consumerInfos.add(workerPoolInfo);
    }

    /**
     * 找到所有还在运行的、处于尾端的消费者
     * */
    public List<MySequence> getLastSequenceInChain() {
        List<MySequence> lastSequenceList = new ArrayList<>();
        for (MyConsumerInfo consumerInfo : consumerInfos) {
            // 找到所有还在运行的、处于尾端的消费者
            if (consumerInfo.isRunning() && consumerInfo.isEndOfChain()) {
                final MySequence[] sequences = consumerInfo.getSequences();
                // 将其消费者序列号全部放进lastSequenceList
                Collections.addAll(lastSequenceList, sequences);
            }
        }

        return lastSequenceList;
    }

    public void unMarkEventProcessorsAsEndOfChain(final MySequence... barrierEventProcessors) {
        for (MySequence barrierEventProcessor : barrierEventProcessors) {
            eventProcessorInfoBySequence.get(barrierEventProcessor).markAsUsedInBarrier();
        }
    }
}
