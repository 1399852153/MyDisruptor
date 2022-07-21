package mydisruptor.falsesharing;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NoFalseSharingDemo {

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        CountDownLatch countDownLatch = new CountDownLatch(2);
        PointNoFalseSharing point = new PointNoFalseSharing(1,2);
        long start = System.currentTimeMillis();
        executor.execute(()->{
            // 线程1 x自增1亿次
            for(int i=0; i<100000000; i++){
                point.x++;
            }
            countDownLatch.countDown();
        });

        executor.execute(()->{
            // 线程2 y自增1亿次
            for(int i=0; i<100000000; i++){
                point.y++;
            }
            countDownLatch.countDown();
        });

        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("testNoFalseSharing 耗时=" + (end-start));
        executor.shutdown();
    }
}
