package mydisruptor.demo;

import mydisruptor.MyBatchEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyRingBufferV4Demo {

    public static void main(String[] args) {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        // 创建环形队列(多线程生产者，即多线程安全的生产者（可以并发的next、publish）)
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createMultiProducer(
                new OrderEventProducer(), ringBufferSize, new MyBlockingWaitStrategy());

        // 获得ringBuffer的序列屏障（最上游的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();

        // ================================== 基于生产者序列屏障，创建消费者A
        MyBatchEventProcessor<OrderEventModel> eventProcessorA =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerA"), mySequenceBarrier);
        MySequence consumeSequenceA = eventProcessorA.getCurrentConsumeSequence();
        // RingBuffer监听消费者A的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceA);

        // 启动消费者线程A
        new Thread(eventProcessorA).start();

        // 启动多线程生产者
        ExecutorService executorService = Executors.newFixedThreadPool(100, new ThreadFactory() {
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r,"workerProducer" + mCount.getAndIncrement());
            }
        });
        for(int i=1; i<4; i++) {
            int num = i;
            executorService.submit(() -> {
                // 每个生产者发布100个事件
                for (int j = 0; j < 100; j++) {
                    long nextIndex = myRingBuffer.next();
                    OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
                    orderEvent.setMessage("message-" + num + "-" + j);
                    orderEvent.setPrice(num * j * 10);
//                    System.out.println("生产者" + num + "发布事件：" + orderEvent);
                    myRingBuffer.publish(nextIndex);
                }
            });
        }
    }
}
