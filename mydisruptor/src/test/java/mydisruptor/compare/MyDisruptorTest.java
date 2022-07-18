package mydisruptor.compare;

import mydisruptor.MyRingBuffer;
import mydisruptor.dsl.MyDisruptor;
import mydisruptor.dsl.ProducerType;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MyDisruptorTest {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int totalProductCount = 1000000;
        MyDisruptor<String> myDisruptor = new MyDisruptor<>(
                () -> "12345", 128,
                new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()),
                ProducerType.SINGLE,
                new MyBlockingWaitStrategy()
        );

        myDisruptor.handleEventsWith(new EventHandlerForTest<>(totalProductCount,countDownLatch));

        myDisruptor.start();

        MyRingBuffer<String> ringBuffer = myDisruptor.getRingBuffer();
        // 生产者发布事件
        for(int i=0; i<totalProductCount; i++) {
            long nextIndex = ringBuffer.next();
            String orderEvent = ringBuffer.get(nextIndex);
//            System.out.println("生产者发布事件：" + orderEvent);
            ringBuffer.publish(nextIndex);
        }

        countDownLatch.await();
        myDisruptor.halt();
        System.out.println("myDisruptor 执行完毕");
    }
}
