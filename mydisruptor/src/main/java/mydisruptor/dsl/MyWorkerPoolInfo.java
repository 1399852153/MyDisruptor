package mydisruptor.dsl;

import mydisruptor.MyWorkerPool;

import java.util.concurrent.Executor;

/**
 * 多线程消费者信息（仿Disruptor.WorkerPoolInfo）
 * */
public class MyWorkerPoolInfo<T> implements MyConsumerInfo {

    private final MyWorkerPool<T> workerPool;

    public MyWorkerPoolInfo(MyWorkerPool<T> workerPool) {
        this.workerPool = workerPool;
    }

    @Override
    public void start(Executor executor) {
        workerPool.start(executor);
    }
}
