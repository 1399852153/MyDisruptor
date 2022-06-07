package mydisruptor.demo;

import mydisruptor.api.MyEventHandler;
import mydisruptor.model.OrderEventModel;
import mydisruptor.util.LogUtil;

/**
 * 订单事件处理器
 * */
public class OrderEventHandlerDemo implements MyEventHandler<OrderEventModel> {

    private String consumerName;

    public OrderEventHandlerDemo(String consumerName) {
        this.consumerName = consumerName;
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        System.out.println(consumerName + " 消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
