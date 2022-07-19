package mydisruptor.compare;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import mydisruptor.model.OrderEventModel;

import java.util.concurrent.*;

public class LmaxDisruptorTest {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int totalProductCount = 1000000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        Disruptor<OrderEventModel> disruptor = new Disruptor<>(
                OrderEventModel::new,128,executor,
                ProducerType.SINGLE,new BlockingWaitStrategy()
                );

        disruptor.handleEventsWith(new EventHandlerForTest(totalProductCount,countDownLatch,true));

        disruptor.start();

        RingBuffer<OrderEventModel> ringBuffer = disruptor.getRingBuffer();
        // 生产者发布事件
        for(int i=0; i<totalProductCount; i++) {
            long nextIndex = ringBuffer.next();
            OrderEventModel orderEvent = ringBuffer.get(nextIndex);
            orderEvent.setMessage(i + "");
            //  System.out.println("生产者发布事件：" + orderEvent);
            ringBuffer.publish(nextIndex);
        }

        countDownLatch.await();
        disruptor.halt();
        executor.shutdown();
        System.out.println("lmax-disruptor 执行完毕" + totalProductCount);
    }
}
