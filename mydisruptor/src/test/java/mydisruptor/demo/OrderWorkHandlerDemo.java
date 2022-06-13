package mydisruptor.demo;

import mydisruptor.api.MyWorkHandler;
import mydisruptor.model.OrderEventModel;

/**
 * 订单事件处理器
 * */
public class OrderWorkHandlerDemo implements MyWorkHandler<OrderEventModel> {

    private String consumerName;

    public OrderWorkHandlerDemo(String consumerName) {
        this.consumerName = consumerName;
    }

    @Override
    public void consume(OrderEventModel event) {
        System.out.println(consumerName + " work组消费者消费事件" + event);
    }
}
