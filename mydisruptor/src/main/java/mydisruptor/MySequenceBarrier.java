package mydisruptor;

import mydisruptor.waitstrategy.MyWaitStrategy;


public class MySequenceBarrier {

    private final MySequence currentProducerSequence;
    private final MyWaitStrategy myWaitStrategy;

    public MySequenceBarrier(MySequence currentProducerSequence, MyWaitStrategy myWaitStrategy) {
        this.currentProducerSequence = currentProducerSequence;
        this.myWaitStrategy = myWaitStrategy;
    }

    /**
     * 获得可用的消费者下标
     * */
    public long getAvailableConsumeSequence(long currentConsumeSequence) throws InterruptedException {
        // v1版本只是简单的调用waitFor，等待其返回即可
        return this.myWaitStrategy.waitFor(currentConsumeSequence,currentProducerSequence,this);
    }
}
