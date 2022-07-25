package mydisruptor.dsl;

import mydisruptor.MyEventProcessor;
import mydisruptor.MySequence;
import mydisruptor.MyWorkerPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 维护当前disruptor的所有消费者对象信息的仓库（仿Disruptor.ConsumerRepository）
 */
public class MyConsumerRepository<T> {

    private final ArrayList<MyConsumerInfo> consumerInfos = new ArrayList<>();

    public ArrayList<MyConsumerInfo> getConsumerInfos() {
        return consumerInfos;
    }

    public void add(final MyEventProcessor processor) {
        final MyEventProcessorInfo<T> consumerInfo = new MyEventProcessorInfo<>(processor);
        consumerInfos.add(consumerInfo);
    }

    public void add(final MyWorkerPool<T> workerPool) {
        final MyWorkerPoolInfo<T> workerPoolInfo = new MyWorkerPoolInfo<>(workerPool);
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
}
