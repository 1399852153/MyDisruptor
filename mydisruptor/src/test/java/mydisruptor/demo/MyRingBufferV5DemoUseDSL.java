package mydisruptor.demo;

import mydisruptor.MyRingBuffer;
import mydisruptor.dsl.MyDisruptor;
import mydisruptor.dsl.MyEventHandlerGroup;
import mydisruptor.dsl.ProducerType;
import mydisruptor.model.OrderEventModel;
import mydisruptor.model.OrderEventProducer;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyRingBufferV5DemoUseDSL {

    /**
     * 消费者依赖关系图（简单起见都是单线程消费者）：
     * A -> BC -> D
     *   -> E -> F
     * */
    public static void main(String[] args) {
        // 环形队列容量为16（2的4次方）
        int ringBufferSize = 16;

        MyDisruptor<OrderEventModel> myDisruptor = new MyDisruptor<>(
                new OrderEventProducer(), ringBufferSize,
                new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()),
                ProducerType.SINGLE,
                new MyBlockingWaitStrategy()
        );

        MyEventHandlerGroup<OrderEventModel> hasAHandlerGroup = myDisruptor.handleEventsWith(new OrderEventHandlerDemo("consumerA"));

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
    }
}
