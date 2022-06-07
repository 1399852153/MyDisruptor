package mydisruptor.demo;

import mydisruptor.MyBatchEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.model.OrderEventModel;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

public class MyRingBufferV2Demo {

    /**
     * A->B->C
     * 线性依赖
     * */
    public static void main(String[] args){
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        // 创建环形队列
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createSingleProducer(
                new OrderEventProducer(), ringBufferSize, new MyBlockingWaitStrategy());

        // 获得ringBuffer的序列屏障（最上游的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        // 基于序列屏障，创建消费者A
        MyBatchEventProcessor<OrderEventModel> eventProcessorA =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerA"), mySequenceBarrier);
        MySequence consumeSequence = eventProcessorA.getCurrentConsumeSequence();
        // RingBuffer监听消费者A的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence);

        // 消费者B依赖上游的消费者A，通过消费者A的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrier2 = myRingBuffer.newBarrier(consumeSequence);
        // 基于序列屏障，创建消费者B
        MyBatchEventProcessor<OrderEventModel> eventProcessorB =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerB"), mySequenceBarrier2);
        MySequence consumeSequence2 = eventProcessorB.getCurrentConsumeSequence();
        // RingBuffer监听消费者B的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence2);

        // 消费者C依赖上游的消费者B，通过消费者B的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrier3 = myRingBuffer.newBarrier(consumeSequence2);
        // 基于序列屏障，创建消费者C
        MyBatchEventProcessor<OrderEventModel> eventProcessorC =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumerC"), mySequenceBarrier3);
        MySequence consumeSequence3 = eventProcessorC.getCurrentConsumeSequence();
        // RingBuffer监听消费者C的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence3);

        // 启动消费者线程1、2、3
        new Thread(eventProcessorA).start();
        new Thread(eventProcessorB).start();
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
    }
}
