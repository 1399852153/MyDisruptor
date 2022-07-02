package mydisruptor.demo;

import mydisruptor.api.MyEventHandler;
import mydisruptor.model.OrderEventModel;
import mydisruptor.util.LogUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 订单事件处理器
 * */
public class OrderEventHandlerDemo implements MyEventHandler<OrderEventModel> {

    private String consumerName;

    private Integer productCount;

    private HashSet<String> uniqueSet;

    public OrderEventHandlerDemo(String consumerName) {
        this.consumerName = consumerName;
    }

    public OrderEventHandlerDemo(String consumerName, Integer productCount) {
        this.consumerName = consumerName;
        this.productCount = productCount;
        this.uniqueSet = new HashSet<>();
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        if (uniqueSet != null){
            boolean notAlreadyExist = uniqueSet.add(event.getMessage());
            if(!notAlreadyExist){
                System.out.println(consumerName + " 重复消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch + "!!!!!!");
            }
        }

        System.out.println(consumerName + " 消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
