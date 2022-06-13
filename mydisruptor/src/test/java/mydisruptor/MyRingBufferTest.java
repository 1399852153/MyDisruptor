package mydisruptor;

import mydisruptor.model.OrderEventConsumer;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.util.LogUtil;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;
import org.junit.Test;

public class MyRingBufferTest {

    @Test
    public void test(){
        int ringBufferSize = 16;
        int produceCount = 1000;

        int repeat = 10;
        long totalCost = 0;
        for(int i=0; i<repeat; i++){
            long cost = one2one2one(ringBufferSize,produceCount);
            totalCost+=cost;
        }

        System.out.println(totalCost/repeat);
    }

    /**
     * A->B->C 消费依赖链
     * */
    public long one2one2one(int ringBufferSize,int produceCount){
        MySingleProducerSequencer singleProducerSequencer = new MySingleProducerSequencer(
                ringBufferSize,new MyBlockingWaitStrategy());
        MyRingBuffer<OrderEventModel> myRingBuffer = new MyRingBuffer<>(singleProducerSequencer,new OrderEventProducer());

        MySequenceBarrier mySequenceBarrier = myRingBuffer.newBarrier();
        // 消费者1
        MyBatchEventProcessor<OrderEventModel> eventProcessor1 =
                new MyBatchEventProcessor<>(myRingBuffer,
                        new OrderEventConsumer(produceCount),mySequenceBarrier);
        MySequence consumeSequence = eventProcessor1.getCurrentConsumeSequence();
        // ringBuffer监听消费者1的消费者序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence);

        // 消费者2依赖消费者1，所以基于消费者1的sequence生成barrier
        MySequenceBarrier step2Barrier = myRingBuffer.newBarrier(consumeSequence);
        MyBatchEventProcessor<OrderEventModel> eventProcessor2 =
                new MyBatchEventProcessor<>(myRingBuffer,
                        new OrderEventConsumer(produceCount),step2Barrier);
        MySequence consumeSequence2 = eventProcessor2.getCurrentConsumeSequence();
        // ringBuffer监听消费者2的消费者序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence2);

        // 消费者3依赖消费者2，所以基于消费者2的sequence生成barrier
        MySequenceBarrier step3Barrier = myRingBuffer.newBarrier(consumeSequence2);
        MyBatchEventProcessor<OrderEventModel> eventProcessor3 =
                new MyBatchEventProcessor<>(myRingBuffer,
                        new OrderEventConsumer(produceCount),step3Barrier);
        MySequence consumeSequence3 = eventProcessor3.getCurrentConsumeSequence();
        // ringBuffer监听消费者3的消费者序列
        myRingBuffer.addGatingConsumerSequenceList(consumeSequence3);


        new Thread(eventProcessor1).start();
        new Thread(eventProcessor2).start();

        long start = System.currentTimeMillis();
        for(int i=0; i<produceCount; i++) {
            long nextIndex = singleProducerSequencer.next();
            OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            LogUtil.logWithThreadName("生产者发布事件：" + orderEvent);
            myRingBuffer.publish(nextIndex);
        }
        long end = System.currentTimeMillis();

        return end-start;
    }
}
