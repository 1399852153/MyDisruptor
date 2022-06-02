package mydisruptor.demo;

import mydisruptor.api.MyEventFactory;
import mydisruptor.model.OrderEventModel;

/**
 * 订单事件工厂
 * */
public class OrderEventFactoryDemo implements MyEventFactory<OrderEventModel> {
    @Override
    public OrderEventModel newInstance() {
        return new OrderEventModel();
    }
}
