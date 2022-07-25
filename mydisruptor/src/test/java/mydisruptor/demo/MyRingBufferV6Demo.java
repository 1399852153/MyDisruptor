package mydisruptor.demo;

import mydisruptor.MyRingBuffer;
import mydisruptor.dsl.MyDisruptor;
import mydisruptor.dsl.MyEventHandlerGroup;
import mydisruptor.dsl.ProducerType;
import mydisruptor.model.OrderEventConsumerWithSleep;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyRingBufferV6Demo {

    /**
     * 验证shutdown方法实现的对不对
     * */
    public static void main(String[] args) {
        int ringBufferSize = 128;

        MyDisruptor<OrderEventModel> myDisruptor = new MyDisruptor<>(
                new OrderEventProducer(), ringBufferSize,
                new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()),
                ProducerType.SINGLE,
                new MyBlockingWaitStrategy()
        );

        MyEventHandlerGroup<OrderEventModel> hasAHandlerGroup = myDisruptor.handleEventsWith(new OrderEventConsumerWithSleep("consumerA"));

        hasAHandlerGroup.then(new OrderEventHandlerDemo("consumerB"),new OrderEventHandlerDemo("consumerC"))
                .then(new OrderEventHandlerDemo("consumerD"));

        hasAHandlerGroup.then(new OrderEventHandlerDemo("consumerE"))
                .then(new OrderEventHandlerDemo("consumerF"));
        // 启动disruptor中注册的所有消费者
        myDisruptor.start();

        MyRingBuffer<OrderEventModel> myRingBuffer = myDisruptor.getRingBuffer();
        // 生产者发布100个事件
        for(int i=0; i<100; i++) {
            long nextIndex = myRingBuffer.next();
            OrderEventModel orderEvent = myRingBuffer.get(nextIndex);
            orderEvent.setMessage("message-"+i);
            orderEvent.setPrice(i * 10);
            System.out.println("生产者发布事件：" + orderEvent);
            myRingBuffer.publish(nextIndex);
        }

        myDisruptor.shutdown(30,TimeUnit.SECONDS);
        System.out.println("myDisruptor shutdown");
    }
}
