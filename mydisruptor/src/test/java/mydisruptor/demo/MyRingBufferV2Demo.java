package mydisruptor.demo;

import mydisruptor.MyBatchMyEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

public class MyRingBufferV2Demo {

    /**
     * 树形依赖关系
     * A，B->C->E
     *    ->D->F,G
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
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorA =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerA"), mySequenceBarrier);
        MySequence consumeSequenceA = eventProcessorA.getCurrentConsumeSequence();
        // RingBuffer监听消费者A的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceA);

        // ================================== 基于生产者序列屏障，创建消费者B
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorB =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerB"), mySequenceBarrier);
        MySequence consumeSequenceB = eventProcessorB.getCurrentConsumeSequence();
        // RingBuffer监听消费者B的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceB);

        // ================================== 消费者C依赖上游的消费者A，B，通过消费者A、B的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierC = myRingBuffer.newBarrier(consumeSequenceA,consumeSequenceB);
        // 基于序列屏障，创建消费者C
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorC =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC"), mySequenceBarrierC);
        MySequence consumeSequenceC = eventProcessorC.getCurrentConsumeSequence();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceC);

        // ================================== 消费者E依赖上游的消费者C，通过消费者C的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierE = myRingBuffer.newBarrier(consumeSequenceC);
        // 基于序列屏障，创建消费者E
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorE =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerE"), mySequenceBarrierE);
        MySequence consumeSequenceE = eventProcessorE.getCurrentConsumeSequence();
        // RingBuffer监听消费者E的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceE);

        // ================================== 消费者D依赖上游的消费者A,B，通过消费者A、B的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierD = myRingBuffer.newBarrier(consumeSequenceA,consumeSequenceB);
        // 基于序列屏障，创建消费者D
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorD =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerD"), mySequenceBarrierD);
        MySequence consumeSequenceD = eventProcessorD.getCurrentConsumeSequence();
        // RingBuffer监听消费者D的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceD);

        // ================================== 消费者F依赖上游的消费者D，通过消费者D的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierF = myRingBuffer.newBarrier(consumeSequenceD);
        // 基于序列屏障，创建消费者F
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorF =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerF"), mySequenceBarrierF);
        MySequence consumeSequenceF = eventProcessorF.getCurrentConsumeSequence();
        // RingBuffer监听消费者F的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceF);

        // ================================== 消费者G依赖上游的消费者D，通过消费者D的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrierG = myRingBuffer.newBarrier(consumeSequenceD);
        // 基于序列屏障，创建消费者G
        MyBatchMyEventProcessor<OrderEventModel> eventProcessorG =
                new MyBatchMyEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerG"), mySequenceBarrierG);
        MySequence consumeSequenceG = eventProcessorG.getCurrentConsumeSequence();
        // RingBuffer监听消费者G的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequenceG);

        // 启动消费者线程
        new Thread(eventProcessorA).start();
        new Thread(eventProcessorB).start();
        new Thread(eventProcessorC).start();
        new Thread(eventProcessorD).start();
        new Thread(eventProcessorE).start();
        new Thread(eventProcessorF).start();
        new Thread(eventProcessorG).start();

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
