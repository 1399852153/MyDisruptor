package mydisruptor.dsl;

import mydisruptor.MyEventProcessor;
import mydisruptor.MyWorkerPool;

import java.util.ArrayList;

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
}
