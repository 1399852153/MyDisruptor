package mydisruptor.compare;

import com.lmax.disruptor.EventHandler;
import mydisruptor.api.MyEventHandler;

import java.util.concurrent.CountDownLatch;

public class EventHandlerForTest<T> implements EventHandler<T>, MyEventHandler<T> {

    private int totalConsumeCount;
    private long startConsumeTime;
    private CountDownLatch countDownLatch;

    public EventHandlerForTest(int totalConsumeCount, CountDownLatch countDownLatch) {
        this.totalConsumeCount = totalConsumeCount;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void onEvent(T t, long l, boolean b) throws Exception {
        doConsume(t,l);
    }

    @Override
    public void consume(T event, long sequence, boolean endOfBatch) {
        doConsume(event,sequence);
    }

    private void doConsume(T event, long sequence){
//        System.out.println("消费event=" + event + " sequence=" + sequence);

        if(sequence == 0){
            startConsumeTime = System.currentTimeMillis();
        }

        if(sequence == (totalConsumeCount-1)){
            System.out.println("消费完成 总耗时：" + (System.currentTimeMillis() - startConsumeTime));
            countDownLatch.countDown();
        }
    }
}
