package mydisruptor.dsl;

import mydisruptor.*;
import mydisruptor.api.MyEventFactory;
import mydisruptor.api.MyEventHandler;
import mydisruptor.api.MyWorkHandler;
import mydisruptor.waitstrategy.MyWaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * disruptor dsl(仿Disruptor.Disruptor)
 * */
public class MyDisruptor<T> {

    private final MyRingBuffer<T> ringBuffer;
    private final Executor executor;
    private final MyConsumerRepository<T> consumerRepository = new MyConsumerRepository<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    public MyDisruptor(
            final MyEventFactory<T> eventProducer,
            final int ringBufferSize,
            final Executor executor,
            final ProducerType producerType,
            final MyWaitStrategy myWaitStrategy) {

        this.ringBuffer = MyRingBuffer.create(producerType,eventProducer,ringBufferSize,myWaitStrategy);
        this.executor = executor;
    }

    /**
     * 注册单线程消费者 (无上游依赖消费者，仅依赖生产者序列)
     * */
    @SafeVarargs
    public final MyEventHandlerGroup<T> handleEventsWith(final MyEventHandler<T>... myEventHandlers){
        return createEventProcessors(new MySequence[0], myEventHandlers);
    }

    /**
     * 注册单线程消费者 (有上游依赖消费者，仅依赖生产者序列)
     * @param barrierSequences 依赖的序列屏障
     * @param myEventHandlers 用户自定义的事件消费者集合
     * */
    public MyEventHandlerGroup<T> createEventProcessors(
            final MySequence[] barrierSequences,
            final MyEventHandler<T>[] myEventHandlers) {

        final MySequence[] processorSequences = new MySequence[myEventHandlers.length];
        final MySequenceBarrier barrier = ringBuffer.newBarrier(barrierSequences);

        int i=0;
        for(MyEventHandler<T> myEventConsumer : myEventHandlers){
            final MyBatchEventProcessor<T> batchEventProcessor =
                    new MyBatchEventProcessor<>(ringBuffer, myEventConsumer, barrier);

            processorSequences[i] = batchEventProcessor.getCurrentConsumeSequence();
            i++;

            // consumer对象都维护起来，便于后续start时启动
            consumerRepository.add(batchEventProcessor);
        }

        // 更新当前生产者注册的消费者序列
        updateGatingSequencesForNextInChain(barrierSequences,processorSequences);

        return new MyEventHandlerGroup<>(this,this.consumerRepository,processorSequences);
    }

    /**
     * 注册多线程消费者 (无上游依赖消费者，仅依赖生产者序列)
     * */
    @SafeVarargs
    public final MyEventHandlerGroup<T> handleEventsWithWorkerPool(final MyWorkHandler<T>... myWorkHandlers) {
        return createWorkerPool(new MySequence[0], myWorkHandlers);
    }

    /**
     * 注册多线程消费者 (有上游依赖消费者，仅依赖生产者序列)
     * @param barrierSequences 依赖的序列屏障
     * @param myWorkHandlers 用户自定义的事件消费者集合
     * */
    public MyEventHandlerGroup<T> createWorkerPool(
            final MySequence[] barrierSequences, final MyWorkHandler<T>[] myWorkHandlers) {
        final MySequenceBarrier sequenceBarrier = ringBuffer.newBarrier(barrierSequences);
        final MyWorkerPool<T> workerPool = new MyWorkerPool<>(ringBuffer, sequenceBarrier, myWorkHandlers);

        // consumer都保存起来，便于start启动
        consumerRepository.add(workerPool);

        final MySequence[] workerSequences = workerPool.getCurrentWorkerSequences();

        updateGatingSequencesForNextInChain(barrierSequences, workerSequences);

        return new MyEventHandlerGroup<>(this, consumerRepository,workerSequences);
    }

    private void updateGatingSequencesForNextInChain(final MySequence[] barrierSequences, final MySequence[] processorSequences) {
        if (processorSequences.length != 0) {
            // 这是一个优化操作：
            // 由于新的消费者通过ringBuffer.newBarrier(barrierSequences)，已经是依赖于之前ringBuffer中已有的消费者序列
            // 消费者即EventProcessor内部已经设置好了老的barrierSequences为依赖，因此可以将ringBuffer中已有的消费者序列去掉
            // 只需要保存，依赖当前消费者链条最末端的序列即可（也就是最慢的序列），这样生产者可以更快的遍历注册的消费者序列
            for(MySequence sequence : barrierSequences){
                ringBuffer.removeConsumerSequence(sequence);
            }
            for(MySequence sequence : processorSequences){
                // 新设置的就是当前消费者链条最末端的序列
                ringBuffer.addConsumerSequence(sequence);
            }
        }
    }

    /**
     * 启动所有已注册的消费者
     * */
    public void start(){
        // cas设置启动标识，避免重复启动
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Disruptor只能启动一次");
        }

        // 遍历所有的消费者，挨个start启动
        this.consumerRepository.getConsumerInfos().forEach(
                item->item.start(this.executor)
        );
    }

    /**
     * 停止注册的所有消费者
     * */
    public void halt() {
        // 遍历消费者信息列表，挨个调用halt方法终止
        for (final MyConsumerInfo consumerInfo : this.consumerRepository.getConsumerInfos()) {
            consumerInfo.halt();
        }
    }


    /**
     * 获得当亲Disruptor的ringBuffer
     * */
    public MyRingBuffer<T> getRingBuffer() {
        return ringBuffer;
    }
}
