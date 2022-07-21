package mydisruptor.demo.compare;

import mydisruptor.MyRingBuffer;
import mydisruptor.dsl.MyDisruptor;
import mydisruptor.dsl.ProducerType;
import mydisruptor.model.OrderEventModel;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyDisruptorTest {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int totalProductCount = 1000000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        MyDisruptor<OrderEventModel> myDisruptor = new MyDisruptor<>(
                OrderEventModel::new, 128,
                executor,ProducerType.SINGLE,
                new MyBlockingWaitStrategy()
        );

        myDisruptor.handleEventsWith(new EventHandlerForTest(totalProductCount,countDownLatch,true));

        myDisruptor.start();

        MyRingBuffer<OrderEventModel> ringBuffer = myDisruptor.getRingBuffer();
        // 生产者发布事件
        for(int i=0; i<totalProductCount; i++) {
            long nextIndex = ringBuffer.next();
            OrderEventModel orderEvent = ringBuffer.get(nextIndex);
            orderEvent.setMessage(i + "");
//            System.out.println("生产者发布事件：" + orderEvent);
            ringBuffer.publish(nextIndex);
        }

        countDownLatch.await();
        myDisruptor.halt();
        executor.shutdown();
        System.out.println("myDisruptor 执行完毕" + totalProductCount);
    }
}
