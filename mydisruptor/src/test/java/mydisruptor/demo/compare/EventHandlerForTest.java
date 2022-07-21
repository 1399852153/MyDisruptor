package mydisruptor.demo.compare;

import com.lmax.disruptor.EventHandler;
import mydisruptor.api.MyEventHandler;
import mydisruptor.model.OrderEventModel;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;

public class EventHandlerForTest implements EventHandler<OrderEventModel>, MyEventHandler<OrderEventModel> {

    private int totalConsumeCount;
    private long startConsumeTime;
    private CountDownLatch countDownLatch;
    private boolean valid;
    private HashSet<String> hashSet;

    public EventHandlerForTest(int totalConsumeCount, CountDownLatch countDownLatch) {
        this.totalConsumeCount = totalConsumeCount;
        this.countDownLatch = countDownLatch;
    }

    public EventHandlerForTest(int totalConsumeCount, CountDownLatch countDownLatch, boolean valid) {
        this.totalConsumeCount = totalConsumeCount;
        this.countDownLatch = countDownLatch;
        this.valid = valid;
        this.hashSet = new HashSet<>();
    }

    @Override
    public void onEvent(OrderEventModel t, long l, boolean b) throws Exception {
        doConsume(t,l);
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        doConsume(event,sequence);
    }

    private void doConsume(OrderEventModel event, long sequence){
//        System.out.println("消费event=" + event + " sequence=" + sequence);

        if(sequence == 0){
            startConsumeTime = System.currentTimeMillis();
        }

        if(valid){
            hashSet.add(event.getMessage());
        }

        if(sequence == (totalConsumeCount-1)){
            System.out.println("消费完成 总耗时：" + (System.currentTimeMillis() - startConsumeTime));

            if(valid){
                if(hashSet.size() != totalConsumeCount){
                    System.out.println("消费数据有问题，校验失败");
                }else{
                    System.out.println("消费数据没问题，校验成功");
                }
            }

            countDownLatch.countDown();
        }
    }
}
