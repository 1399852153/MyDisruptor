package mydisruptor.demo;


import mydisruptor.*;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRingBufferV3Demo {

    /**
     * 单线程消费者A -> 多线程消费者B -> 单线程消费者C
     * */
    public static void main(String[] args) throws InterruptedException {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        // 创建环形队列
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createSingleProducer(
                new OrderEventProducer(), ringBufferSize, new MyBlockingWaitStrategy());

        // 获得ringBuffer的序列屏障（最上游的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();

        // ================================== 基于生产者序列屏障，创建消费者A
        MyBatchEventProcessor<OrderEventModel> eventProcessorA =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerA"), mySequenceBarrier);
        MySequence consumeSequenceA = eventProcessorA.getCurrentConsumeSequence();
        // RingBuffer监听消费者A的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceA);

        // ================================== 消费者组依赖上游的消费者A，通过消费者A的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier workerSequenceBarrier = myRingBuffer.newBarrier(consumeSequenceA);
        // 基于序列屏障，创建消费者组
        MyWorkerPool<OrderEventModel> workerPoolProcessorB =
                new MyWorkerPool<>(myRingBuffer, workerSequenceBarrier,
                        new OrderWorkHandlerDemo("workerHandler1"),
                        new OrderWorkHandlerDemo("workerHandler2"),
                        new OrderWorkHandlerDemo("workerHandler3"));
        MySequence[] workerSequences = workerPoolProcessorB.getCurrentWorkerSequences();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(workerSequences);

        // ================================== 基于多线程消费者B的序列屏障，创建消费者C
        MySequenceBarrier mySequenceBarrierC = myRingBuffer.newBarrier(workerSequences);

        MyBatchEventProcessor<OrderEventModel> eventProcessorC =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC"), mySequenceBarrierC);
        MySequence consumeSequenceC = eventProcessorC.getCurrentConsumeSequence();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceC);

        // 启动消费者线程A
        new Thread(eventProcessorA).start();
        // 启动workerPool多线程消费者B
        workerPoolProcessorB.start(Executors.newFixedThreadPool(3, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"worker" + mCount.getAndIncrement());
            }
        }));
        // 启动消费者线程C
        new Thread(eventProcessorC).start();

        // 生产者发布100个事件
        for(int i=0; i<100; i++) {
            long nextIndex = myRingBuffer.next();
            OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            System.out.println("生产者发布事件：" + orderEvent);
            myRingBuffer.publish(nextIndex);
        }

        // 简单阻塞下，避免还未消费完主线程退出
        Thread.sleep(5000L);
    }
}
