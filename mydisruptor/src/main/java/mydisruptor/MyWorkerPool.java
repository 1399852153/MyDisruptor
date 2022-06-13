package mydisruptor;

import mydisruptor.api.MyEventHandler;
import mydisruptor.api.MyWorkHandler;

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
            final MyWorkHandler<T>... myWorkHandlerList) {

        this.myRingBuffer = myRingBuffer;
        final int numWorkers = myWorkHandlerList.length;
        this.workEventProcessorList = new ArrayList<>(numWorkers);

        // 为每个自定义事件消费逻辑MyEventHandler，创建一个对应的MyWorkProcessor去处理
        for (MyWorkHandler<T> myEventConsumer : myWorkHandlerList) {
            workEventProcessorList.add(new MyWorkProcessor<>(
                    myRingBuffer,
                    myEventConsumer,
                    sequenceBarrier,
                    this.workSequence));
        }
    }

    /**
     * 返回包括每个workerEventProcessor + workerPool自身的序列号集合
     * */
    public MySequence[] getCurrentWorkerSequences() {
        final MySequence[] sequences = new MySequence[this.workEventProcessorList.size() + 1];
        for (int i = 0, size = workEventProcessorList.size(); i < size; i++) {
            sequences[i] = workEventProcessorList.get(i).getCurrentConsumeSequence();
        }
        sequences[sequences.length - 1] = workSequence;

        return sequences;
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
