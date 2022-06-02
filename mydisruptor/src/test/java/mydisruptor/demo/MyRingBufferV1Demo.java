package mydisruptor.demo;

import mydisruptor.MyBatchEventProcessor;
import mydisruptor.MyRingBuffer;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.model.OrderEventModel;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

public class MyRingBufferV1Demo {

    public static void main(String[] args) {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        // 创建环形队列
        MyRingBuffer<OrderEventModel> myRingBuffer = MyRingBuffer.createSingleProducer(
                new OrderEventProducer(), ringBufferSize, new MyBlockingWaitStrategy());

        // 获得ringBuffer的内存屏障（v1版本的序列屏障内只维护生产者的序列）
        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        // 基于序列屏障，创建消费者
        MyBatchEventProcessor<OrderEventModel> eventProcessor =
                new MyBatchEventProcessor<>(myRingBuffer, new OrderEventHandlerDemo(), mySequenceBarrier);
        // RingBuffer设置消费者的序列，用于控制生产速度
        MySequence consumeSequence = eventProcessor.getCurrentConsumeSequence();
        myRingBuffer.setConsumerSequence(consumeSequence);

        // 启动消费者线程
        new Thread(eventProcessor).start();

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
