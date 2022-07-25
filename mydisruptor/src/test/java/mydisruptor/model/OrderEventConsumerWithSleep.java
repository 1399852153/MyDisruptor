package mydisruptor.model;

import mydisruptor.api.MyEventHandler;
import mydisruptor.util.LogUtil;

import java.util.HashSet;
import java.util.Stack;

public class OrderEventConsumerWithSleep implements MyEventHandler<OrderEventModel> {

    private String consumerName;

    private Integer productCount;

    private HashSet<String> uniqueSet;

    public OrderEventConsumerWithSleep(String consumerName) {
        this.consumerName = consumerName;
    }

    public OrderEventConsumerWithSleep(String consumerName, Integer productCount) {
        this.consumerName = consumerName;
        this.productCount = productCount;
        this.uniqueSet = new HashSet<>();
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (uniqueSet != null){
            boolean notAlreadyExist = uniqueSet.add(event.getMessage());
            if(!notAlreadyExist){
                System.out.println(consumerName + " 重复消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch + "!!!!!!");
            }
        }

        System.out.println(consumerName + " 消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
