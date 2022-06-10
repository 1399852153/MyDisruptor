package mydisruptor;

import mydisruptor.api.MyEventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 多线程消费者（仿Disruptor.WorkerPool）
 * */
public class MyWorkerPool<T> {

    private final MySequence workSequence = new MySequence(-1);
    private final MyRingBuffer<T> myRingBuffer;
    private final List<MyWorkProcessor<T>> workEventProcessorList;

    public MyWorkerPool(
            final MyRingBuffer<T> myRingBuffer,
            final MySequenceBarrier sequenceBarrier,
            final List<MyEventHandler<T>> myEventConsumerList) {

        this.myRingBuffer = myRingBuffer;
        final int numWorkers = myEventConsumerList.size();
        this.workEventProcessorList = new ArrayList<>(numWorkers);

        // 为每个自定义事件消费逻辑MyEventHandler，创建一个对应的MyWorkProcessor去处理
        for (MyEventHandler<T> myEventConsumer : myEventConsumerList) {
            workEventProcessorList.add(new MyWorkProcessor<>(
                    myRingBuffer,
                    myEventConsumer,
                    sequenceBarrier,
                    this.workSequence));
        }
    }

    public MyRingBuffer<T> start(final Executor executor) {
        final long cursor = myRingBuffer.getCurrentProducerSequence().get();
        workSequence.set(cursor);

        for (MyWorkProcessor<?> processor : workEventProcessorList) {
            processor.getCurrentConsumeSequence().set(cursor);
            executor.execute(processor);
        }

        return this.myRingBuffer;
    }
}
