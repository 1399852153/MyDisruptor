package mydisruptor.compare;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LmaxDisruptorTest {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int totalProductCount = 1000000;
        Disruptor<String> disruptor = new Disruptor<>(
                () -> "12345",128,
                new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()),
                ProducerType.SINGLE,new BlockingWaitStrategy()
                );

        disruptor.handleEventsWith(new EventHandlerForTest<>(totalProductCount,countDownLatch));

        disruptor.start();

        RingBuffer<String> ringBuffer = disruptor.getRingBuffer();
        // 生产者发布事件
        for(int i=0; i<totalProductCount; i++) {
            long nextIndex = ringBuffer.next();
            String orderEvent = ringBuffer.get(nextIndex);
//            System.out.println("生产者发布事件：" + orderEvent);
            ringBuffer.publish(nextIndex);
        }

        countDownLatch.await();
        disruptor.halt();
        System.out.println("lmax-disruptor 执行完毕");
    }
}
