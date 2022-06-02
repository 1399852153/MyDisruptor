package mydisruptor.model;

import mydisruptor.api.MyEventHandler;
import mydisruptor.util.LogUtil;

import java.util.Stack;

public class OrderEventConsumer implements MyEventHandler<OrderEventModel> {

    private Stack<Integer> priceOrderStack = new Stack<>();
    private int maxConsumeTime;
    private boolean notOrdered = false;

    public OrderEventConsumer(int maxConsumeTime) {
        this.maxConsumeTime = maxConsumeTime;
    }

    @Override
    public void consume(OrderEventModel event, long sequence, boolean endOfBatch) {
//        try {
//            Thread.sleep(1000L);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        if(!priceOrderStack.isEmpty() && event.getPrice() < priceOrderStack.peek()){
            LogUtil.logWithThreadName("price not ordered event=" + event);
            this.notOrdered = true;
            throw new RuntimeException("price not ordered event=" + event);
        }else{
            priceOrderStack.push(event.getPrice());
        }
//        LogUtil.logWithThreadName("OrderEventConsumer1 消费订单事件：sequence=" + sequence + "     " + event);

//        if(this.priceOrderStack.size() == this.maxConsumeTime){
//            if(this.notOrdered){
//                LogUtil.logWithThreadName("OrderEventConsumer1 消费订单事件失败");
//            }else{
//                LogUtil.logWithThreadName("OrderEventConsumer1 消费订单事件完毕" + priceOrderStack);
//            }
//        }
    }

    public void clear(){
        this.priceOrderStack.clear();
    }
}
