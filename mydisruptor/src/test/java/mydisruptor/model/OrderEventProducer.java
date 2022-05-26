package mydisruptor.model;

import mydisruptor.api.MyEventFactory;

public class OrderEventProducer implements MyEventFactory<OrderModel> {
    @Override
    public OrderModel newInstance() {
        return new OrderModel();
    }
}
