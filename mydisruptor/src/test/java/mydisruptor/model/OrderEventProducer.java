package mydisruptor.model;

import mydisruptor.api.MyEventFactory;

public class OrderEventProducer implements MyEventFactory<OrderEventModel> {
    @Override
    public OrderEventModel newInstance() {
        return new OrderEventModel();
    }
}
