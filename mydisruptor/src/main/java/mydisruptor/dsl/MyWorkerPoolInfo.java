package mydisruptor.dsl;

import mydisruptor.MySequence;
import mydisruptor.MyWorkerPool;

import java.util.concurrent.Executor;

/**
 * 多线程消费者信息（仿Disruptor.WorkerPoolInfo）
 * */
public class MyWorkerPoolInfo<T> implements MyConsumerInfo {

    private final MyWorkerPool<T> workerPool;

    /**
     * 默认是最尾端的消费者
     * */
    private boolean endOfChain = true;

    public MyWorkerPoolInfo(MyWorkerPool<T> workerPool) {
        this.workerPool = workerPool;
    }

    @Override
    public void start(Executor executor) {
        workerPool.start(executor);
    }

    @Override
    public void halt() {
        this.workerPool.halt();
    }

    @Override
    public boolean isEndOfChain() {
        return endOfChain;
    }

    @Override
    public void markIsNotEndOfChain() {
        this.endOfChain = true;
    }

    @Override
    public boolean isRunning() {
        return this.workerPool.isRunning();
    }

    @Override
    public MySequence[] getSequences() {
        return this.workerPool.getCurrentWorkerSequences();
    }
}
