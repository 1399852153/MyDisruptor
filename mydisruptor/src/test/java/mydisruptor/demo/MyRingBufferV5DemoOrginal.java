package mydisruptor.demo;

import mydisruptor.MyBatchEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyRingBufferV5DemoOrginal {

    /**
     * 消费者依赖关系图（简单起见都是单线程消费者）：
     * A -> BC -> D
     * */
    public static void main(String[] args) {
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

        // ================================== 通过消费者A的序列号创建序列屏障（构成消费的顺序依赖），创建消费者B
        MySequenceBarrier mySequenceBarrierB = myRingBuffer.newBarrier(consumeSequenceA);

        MyBatchEventProcessor<OrderEventModel> eventProcessorB =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerB"), mySequenceBarrierB);
        MySequence consumeSequenceB = eventProcessorB.getCurrentConsumeSequence();
        // RingBuffer监听消费者B的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceB);

        // ================================== 通过消费者A的序列号创建序列屏障（构成消费的顺序依赖），创建消费者C
        MySequenceBarrier mySequenceBarrierC = myRingBuffer.newBarrier(consumeSequenceA);

        MyBatchEventProcessor<OrderEventModel> eventProcessorC =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC"), mySequenceBarrierC);
        MySequence consumeSequenceC = eventProcessorC.getCurrentConsumeSequence();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceC);

        // ================================== 消费者D依赖上游的消费者B，C，通过消费者B、C的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierD = myRingBuffer.newBarrier(consumeSequenceB,consumeSequenceC);
        // 基于序列屏障，创建消费者D
        MyBatchEventProcessor<OrderEventModel> eventProcessorD =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerD"), mySequenceBarrierD);
        MySequence consumeSequenceD = eventProcessorD.getCurrentConsumeSequence();
        // RingBuffer监听消费者D的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceD);

        Executor executor = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        // 启动消费者线程A
        executor.execute(eventProcessorA);
        // 启动消费者线程B
        executor.execute(eventProcessorB);
        // 启动消费者线程C
        executor.execute(eventProcessorC);
        // 启动消费者线程D
        executor.execute(eventProcessorD);

        // 生产者发布100个事件
        for(int i=0; i<100; i++) {
            long nextIndex = myRingBuffer.next();
            OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            System.out.println("生产者发布事件：" + orderEvent);
            myRingBuffer.publish(nextIndex);
        }
    }
}
