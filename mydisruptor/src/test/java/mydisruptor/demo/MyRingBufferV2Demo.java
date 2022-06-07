package mydisruptor.demo;

import mydisruptor.MyBatchEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.model.OrderEventModel;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

public class MyRingBufferV2Demo {

    public static void main(String[] args) {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        // 创建环形队列
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createSingleProducer(
                new OrderEventProducer(), ringBufferSize, new MyBlockingWaitStrategy());

        // 获得ringBuffer的序列屏障（最上游的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        // 基于序列屏障，创建消费者1
        MyBatchEventProcessor<OrderEventModel> eventProcessor =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumer1"), mySequenceBarrier);
        MySequence consumeSequence = eventProcessor.getCurrentConsumeSequence();
        // RingBuffer监听消费者1的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence);

        // 消费者2依赖上游的消费者1，通过消费者1的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrier2 = myRingBuffer.newBarrier(consumeSequence);
        // 基于序列屏障，创建消费者2
        MyBatchEventProcessor<OrderEventModel> eventProcessor2 =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumer2"), mySequenceBarrier2);
        MySequence consumeSequence2 = eventProcessor2.getCurrentConsumeSequence();
        // RingBuffer监听消费者2的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence2);

        // 消费者3依赖上游的消费者2，通过消费者2的序列号创建序列屏障（构成消费的顺序依赖）
        MySequenceBarrier mySequenceBarrier3 = myRingBuffer.newBarrier(consumeSequence2);
        // 基于序列屏障，创建消费者3
        MyBatchEventProcessor<OrderEventModel> eventProcessor3 =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo("consumer3"), mySequenceBarrier3);
        MySequence consumeSequence3 = eventProcessor3.getCurrentConsumeSequence();
        // RingBuffer监听消费者3的序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence3);

        // 启动消费者线程1、2、3
        new Thread(eventProcessor).start();
//        new Thread(eventProcessor2).start();
//        new Thread(eventProcessor3).start();

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
