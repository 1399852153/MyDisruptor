package mydisruptor.v1;

import mydisruptor.*;
import mydisruptor.model.OrderEventConsumer;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.model.OrderModel;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;
import mydisruptor.waitstrategy.MyWaitStrategy;
import org.junit.Test;

public class MyRingBufferTestV1 {

    @Test
    public void testV1(){
        MySingleProducerSequencer singleProducerSequencer = new MySingleProducerSequencer(
                8,new MyBlockingWaitStrategy());
        MyRingBuffer<OrderModel> myRingBuffer = new MyRingBuffer<>(singleProducerSequencer,new OrderEventProducer());

        int produceCount = 1000;

        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        MyBatchEventProcessor<OrderModel> eventProcessor =
                new MyBatchEventProcessor<>(myRingBuffer,
                        new OrderEventConsumer(produceCount),mySequenceBarrier);
        MySequence consumeSequence = eventProcessor.getCurrentConsumeSequence();
        myRingBuffer.setConsumerSequence(consumeSequence);

        new Thread(eventProcessor).start();

        for(int i=0; i<produceCount; i++) {
            long nextIndex = singleProducerSequencer.next();
            OrderModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            myRingBuffer.publish(nextIndex);
        }
    }
}
