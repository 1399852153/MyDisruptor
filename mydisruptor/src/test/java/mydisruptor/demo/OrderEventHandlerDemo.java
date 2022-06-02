package mydisruptor.demo;

import mydisruptor.api.MyEventHandler;
import mydisruptor.model.OrderEventModel;

/**
 * 订单事件处理器
 * */
public class OrderEventHandlerDemo implements MyEventHandler<OrderEventModel> {
    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
        System.out.println("消费者消费事件" + event + " sequence=" + sequence + " endOfBatch=" + endOfBatch);
    }
}
