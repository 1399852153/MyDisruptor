package mydisruptor.norepeat;

import mydisruptor.*;
import mydisruptor.demo.OrderEventHandlerDemo;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBusySpinWaitStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRingBufferV4NoRepeatTest {

    public static void main(String[] args) {
        int productThreadCount = 3;
        int singleThreadProductCount = 10000;
        int totalConsumeCount = productThreadCount * singleThreadProductCount;

        // 环形队列容量
        int ringBufferSize = 2;

        // 创建环形队列(多线程生产者，即多线程安全的生产者（可以并发的next、publish）)
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createMultiProducer(
                new OrderEventProducer(), ringBufferSize, new MyBusySpinWaitStrategy());

        // 获得ringBuffer的序列屏障（最上游的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();

        // ================================== 基于生产者序列屏障，创建消费者A
        MyBatchEventProcessor<OrderEventModel> eventProcessorA =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerA",totalConsumeCount), mySequenceBarrier);
        MySequence consumeSequenceA = eventProcessorA.getCurrentConsumeSequence();
        // RingBuffer监听消费者A的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceA);

        // ================================== 消费者组依赖上游的消费者A，通过消费者A的序列号创建序列屏障（构成消费的顺序依赖）
//        MySequenceBarrier workerSequenceBarrier = myRingBuffer.newBarrier(consumeSequenceA);
//        // 基于序列屏障，创建多线程消费者B
//        MyWorkerPool<OrderEventModel> workerPoolProcessorB =
//                new MyWorkerPool<>(myRingBuffer, workerSequenceBarrier,
//                        new OrderWorkHandlerDemo("workerHandler1"),
//                        new OrderWorkHandlerDemo("workerHandler2"),
//                        new OrderWorkHandlerDemo("workerHandler3"));
//        MySequence[] workerSequences = workerPoolProcessorB.getCurrentWorkerSequences();
//        // RingBuffer监听消费者C的序列
//        myRingBuffer.addGatingConsumerSequenceList(workerSequences);

        // ================================== 通过消费者A的序列号创建序列屏障（构成消费的顺序依赖），创建消费者C
//        MySequenceBarrier mySequenceBarrierC = myRingBuffer.newBarrier(consumeSequenceA);
//
//        MyBatchEventProcessor<OrderEventModel> eventProcessorC =
//                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC",totalConsumeCount), mySequenceBarrierC);
//        MySequence consumeSequenceC = eventProcessorC.getCurrentConsumeSequence();
//        // RingBuffer监听消费者C的序列
//        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceC);

        // ================================== 基于多线程消费者B，单线程消费者C的序列屏障，创建消费者D
//        MySequence[] bAndCSequenceArr = new MySequence[workerSequences.length+1];
//        // 把多线程消费者B的序列复制到合并的序列数组中
//        System.arraycopy(workerSequences, 0, bAndCSequenceArr, 0, workerSequences.length);
//        // 数组的最后一位是消费者C的序列
//        bAndCSequenceArr[bAndCSequenceArr.length-1] = consumeSequenceC;
//        MySequenceBarrier mySequenceBarrierD = myRingBuffer.newBarrier(bAndCSequenceArr);
//
//        MyBatchEventProcessor<OrderEventModel> eventProcessorD =
//                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerD",totalConsumeCount), mySequenceBarrierD);
//        MySequence consumeSequenceD = eventProcessorD.getCurrentConsumeSequence();
//        // RingBuffer监听消费者D的序列
//        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceD);


        // 启动消费者线程A
        new Thread(eventProcessorA).start();

//        // 启动workerPool多线程消费者B
//        workerPoolProcessorB.start(Executors.newFixedThreadPool(10, new ThreadFactory() {
//            private final AtomicInteger mCount = new AtomicInteger(1);
//
//            @Override
//            public Thread newThread(Runnable r) {
//                return new Thread(r,"worker" + mCount.getAndIncrement());
//            }
//        }));
//
//        // 启动消费者线程C
//        new Thread(eventProcessorC).start();
//        // 启动消费者线程D
//        new Thread(eventProcessorD).start();

        // 启动多线程生产者
        ExecutorService executorService = Executors.newFixedThreadPool(10, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"workerProducer" + mCount.getAndIncrement());
            }
        });
        for(int i=1; i<productThreadCount+1; i++) {
            int num = i;
            executorService.submit(() -> {
                // 3个生产者线程，每个生产者并发发布
                for (int j = 0; j < singleThreadProductCount; j++) {
                    long nextIndex = myRingBuffer.next();
                    OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
                    orderEvent.setPrice(num * j * 10);
                    orderEvent.setMessage("message-" + num + "-" + j);
//                    System.out.println("生产者" + num + "发布事件：" + orderEvent);
                    myRingBuffer.publish(nextIndex);
                }
            });
        }
    }
}
