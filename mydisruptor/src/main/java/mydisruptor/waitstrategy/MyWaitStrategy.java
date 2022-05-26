package mydisruptor.waitstrategy;

import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;

/**
 * 消费者等待策略
 * */
public interface MyWaitStrategy {

    /**
     * 类似jdk Condition的await，如果不满足条件就会阻塞在该方法内，不返回
     * */
    long waitFor(long currentConsumeSequence, MySequence currentProducerSequence, MySequenceBarrier mySequenceBarrier) throws InterruptedException;

    /**
     * 类似jdk Condition的signal，唤醒waitFor阻塞在该等待策略对象上的消费者线程
     * */
    void signalWhenBlocking();

}
