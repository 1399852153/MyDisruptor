# MyDisruptor V5版本介绍
在v4版本的MyDisruptor实现多线程生产者后。按照计划，v5版本的MyDisruptor需要支持更便于用户使用的DSL风格的API。
#####
由于该文属于系列博客的一部分，需要先对之前的博客内容有所了解才能更好地理解本篇博客
* v1版本博客：[从零开始实现lmax-Disruptor队列（一）RingBuffer与单生产者、单消费者工作原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16318972.html)
* v2版本博客：[从零开始实现lmax-Disruptor队列（二）多消费者、消费者组间消费依赖原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16361197.html)
* v3版本博客：[从零开始实现lmax-Disruptor队列（三）多线程消费者WorkerPool原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16386982.html)
* v4版本博客：[从零开始实现lmax-Disruptor队列（四）多线程生产者MultiProducerSequencer原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16448674.html)
# 为什么Disruptor需要DSL风格的API
通过前面4个版本的迭代，MyDisruptor已经实现了disruptor的大多数功能。但对程序可读性有要求的读者可能会注意到，demo示例代码中对于构建多个消费者之间的依赖关系时细节有点多。
构建一个有上游消费者依赖的EventProcessor消费者来说需要以下几步完成：
1. 获得所要依赖的上游消费者序列集合，并在创建EventProcessor时通过参数传入
2. 获得所创建的EventProcessor对应的消费者序列对象
3. 将获得的消费者序列对象注册到RingBuffer中
4. 通过线程池或者start等方式启动EventProcessor线程，监听并消费
#####
**目前的版本中，每创建一个消费者都需要写一遍上述的模板代码。虽然对于理解Disruptor原理的人来说还能勉强接受，但还是很繁琐且容易在细节上犯错，更遑论对disruptor底层不大了解的普通用户了。**  
基于上述原因，disruptor提供了更加简单易用的API，使得对disruptor底层各组件间依赖不甚了解的用户也能很方便的使用disruptor，并构建不同消费者组间的依赖。
### 什么是DSL风格的API?
DSL即Domain Specific Language，领域特定语言。DSL是针对特定领域抽象出的一个特定语言，通过进一层的抽象来代替大量繁琐的通用代码段，如sql、shell等都是常见的dsl。  
而DSL风格的API最大的特点就是接口的定义贴合业务场景，因此易于理解和使用。
# MyDisruptor DSL风格API实现详解
##### Disruptor
首先要介绍的就是Disruptor类，disruptor类主要用于创建一个符合用户需求的RingBuffer，并提供一组易用的api以屏蔽底层组件交互的细节。
MyDisruptor类的构造函数有五个参数，分别是:
1. 用户自定义的事件生产器（EventFactory）
2. RingBuffer的容量大小
3. 消费者执行器（juc.Executor实现类）
4. 生产者类型枚举（指定单线程生产者 or 多线程生产者）
5. 消费者阻塞策略实现（WaitStrategy）
以上都是需要用户自定义或者指定的核心参数，构建好的disruptor的同时，也创建好了RingBuffer和指定类型的生产者序列器。
```java
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
            for(MySequence sequenceV4 : barrierSequences){
                ringBuffer.removeConsumerSequence(sequenceV4);
            }
            for(MySequence sequenceV4 : processorSequences){
                // 新设置的就是当前消费者链条最末端的序列
                ringBuffer.addConsumerSequence(sequenceV4);
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
     * 获得当亲Disruptor的ringBuffer
     * */
    public MyRingBuffer<T> getRingBuffer() {
        return ringBuffer;
    }
}
```
##### EventHandlerGroup
创建好Disruptor后，便可以按照需求编排各种消费者的依赖逻辑了。Disruptor提供了


