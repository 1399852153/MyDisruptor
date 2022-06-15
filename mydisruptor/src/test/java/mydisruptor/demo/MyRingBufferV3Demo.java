package mydisruptor.demo;


import mydisruptor.*;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;
import mydisruptor.waitstrategy.MyBusySpinWaitStrategy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRingBufferV3Demo {

    /**
     *              -> 多线程消费者B（依赖A）
     * 单线程消费者A                       -> 单线程消费者D（依赖B、C）
     *              -> 单线程消费者C（依赖A）
     * */
    public static void main(String[] args) throws InterruptedException {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 4;

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
        // 基于序列屏障，创建多线程消费者B
        MyWorkerPool<OrderEventModel> workerPoolProcessorB =
                new MyWorkerPool<>(myRingBuffer, workerSequenceBarrier,
                        new OrderWorkHandlerDemo("workerHandler1"),
                        new OrderWorkHandlerDemo("workerHandler2"),
                        new OrderWorkHandlerDemo("workerHandler3"));
        MySequence[] workerSequences = workerPoolProcessorB.getCurrentWorkerSequences();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(workerSequences);

        // ================================== 通过消费者A的序列号创建序列屏障（构成消费的顺序依赖），创建消费者C
        MySequenceBarrier mySequenceBarrierC = myRingBuffer.newBarrier(consumeSequenceA);

        MyBatchEventProcessor<OrderEventModel> eventProcessorC =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC"), mySequenceBarrierC);
        MySequence consumeSequenceC = eventProcessorC.getCurrentConsumeSequence();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceC);

        // ================================== 基于多线程消费者B，单线程消费者C的序列屏障，创建消费者D
        MySequence[] bAndCSequenceArr = new MySequence[workerSequences.length+1];
        // 把多线程消费者B的序列复制到合并的序列数组中
        System.arraycopy(workerSequences, 0, bAndCSequenceArr, 0, workerSequences.length);
        // 数组的最后一位是消费者C的序列
        bAndCSequenceArr[bAndCSequenceArr.length-1] = consumeSequenceC;
        MySequenceBarrier mySequenceBarrierD = myRingBuffer.newBarrier(bAndCSequenceArr);

//        MySequenceBarrier mySequenceBarrierD = myRingBuffer.newBarrier(consumeSequenceC);
        MyBatchEventProcessor<OrderEventModel> eventProcessorD =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerD"), mySequenceBarrierD);
        MySequence consumeSequenceD = eventProcessorD.getCurrentConsumeSequence();
        // RingBuffer监听消费者D的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceD);


        // 启动消费者线程A
        Thread ta = new Thread(eventProcessorA);
        ta.start();

        ExecutorService executorService = Executors.newFixedThreadPool(100, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"worker" + mCount.getAndIncrement());
            }
        });
        // 启动workerPool多线程消费者B
        workerPoolProcessorB.start(executorService);

        // 启动消费者线程C
        Thread tc = new Thread(eventProcessorC);
        tc.start();

        // 启动消费者线程D
        Thread td = new Thread(eventProcessorD);
        td.start();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Thread producerThread = new Thread(
                ()->{
                    // 生产者发布100个事件
                    for(int i=0; i<100; i++) {
                        long nextIndex = myRingBuffer.next();
                        OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
                        orderEvent.setMessage("message-"+i);
                        orderEvent.setPrice(i * 10);
                        System.out.println("生产者发布事件：" + orderEvent);
                        myRingBuffer.publish(nextIndex);
                    }

                    System.out.println("生产者生产完毕");
                    countDownLatch.countDown();
                }
        );
        producerThread.start();

        countDownLatch.await();
        producerThread.stop();

        // 简单阻塞下，避免lazySet导致未消费完
        Thread.sleep(5000L);

        ta.stop();
        System.out.println("关闭消费者a");
        Thread.sleep(5000L);
//        executorService.shutdown();
//        System.out.println("关闭消费者组b");
//        Thread.sleep(5000L);
//        tc.stop();
//        System.out.println("关闭消费者c");
//        Thread.sleep(5000L);
//        td.stop();
//        System.out.println("关闭消费者d");

        System.out.println("关闭所有消费者完毕");
    }
}
