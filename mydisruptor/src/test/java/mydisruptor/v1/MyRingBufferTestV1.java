package mydisruptor.v1;

import mydisruptor.*;
import mydisruptor.model.OrderEventConsumer;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.model.OrderModel;
import mydisruptor.util.LogUtil;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;
import mydisruptor.waitstrategy.MyWaitStrategy;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

public class MyRingBufferTestV1 {

    @Test
    public void testV1(){
        int ringBufferSize = 1024;
        int produceCount = 1000000;

        int repeat = 10;
        long totalCost = 0;
        for(int i=0; i<repeat; i++){
            long cost = one2one(ringBufferSize,produceCount);
            totalCost+=cost;
        }

        System.out.println(totalCost/repeat);
    }

    public long one2one(int ringBufferSize,int produceCount){
        MySingleProducerSequencer singleProducerSequencer = new MySingleProducerSequencer(
                ringBufferSize,new MyBlockingWaitStrategy());
        MyRingBuffer<OrderModel> myRingBuffer = new MyRingBuffer<>(singleProducerSequencer,new OrderEventProducer());

        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        MyBatchEventProcessor<OrderModel> eventProcessor =
                new MyBatchEventProcessor<>(myRingBuffer,
                        new OrderEventConsumer(produceCount),mySequenceBarrier);
        MySequence consumeSequence = eventProcessor.getCurrentConsumeSequence();
        myRingBuffer.setConsumerSequence(consumeSequence);

        new Thread(eventProcessor).start();

        long start = System.currentTimeMillis();
        for(int i=0; i<produceCount; i++) {
            long nextIndex = singleProducerSequencer.next();
            OrderModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
//            LogUtil.logWithThreadName("生产者发布事件：" + orderEvent);
            myRingBuffer.publish(nextIndex);
        }
        long end = System.currentTimeMillis();

        return end-start;
    }
}
