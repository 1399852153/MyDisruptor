package mydisruptor;

import mydisruptor.waitstrategy.MyWaitStrategy;

import java.util.Collections;
import java.util.List;

/**
 * 序列栅栏（仿Disruptor.SequenceBarrier）
 * */
public class MySequenceBarrier {

    private final MyProducerSequencer myProducerSequencer;
    private final MySequence currentProducerSequence;
    private volatile boolean alerted = false;
    private final MyWaitStrategy myWaitStrategy;
    private final MySequence[] dependentSequencesList;

    public MySequenceBarrier(MyProducerSequencer myProducerSequencer, MySequence currentProducerSequence,
                             MyWaitStrategy myWaitStrategy, MySequence[] dependentSequencesList) {
        this.myProducerSequencer = myProducerSequencer;
        this.currentProducerSequence = currentProducerSequence;
        this.myWaitStrategy = myWaitStrategy;

        if(dependentSequencesList.length != 0) {
            this.dependentSequencesList = dependentSequencesList;
        }else{
            // 如果传入的上游依赖序列为空，则生产者序列号作为兜底的依赖
            this.dependentSequencesList = new MySequence[]{currentProducerSequence};
        }
    }

    /**
     * 获得可用的消费者下标（disruptor中的waitFor）
     * */
    public long getAvailableConsumeSequence(long currentConsumeSequence) throws InterruptedException, MyAlertException {
        // 每次都检查下是否有被唤醒，被唤醒则会抛出MyAlertException代表当前消费者要终止运行了
        checkAlert();

        long availableSequence = this.myWaitStrategy.waitFor(currentConsumeSequence,currentProducerSequence,dependentSequencesList,this);

        if (availableSequence < currentConsumeSequence) {
            return availableSequence;
        }

        // 多线程生产者中，需要进一步约束（于v4版本新增）
        return myProducerSequencer.getHighestPublishedSequence(currentConsumeSequence,availableSequence);
    }

    /**
     * 唤醒可能处于阻塞态的消费者
     * */
    public void alert() {
        this.alerted = true;
        this.myWaitStrategy.signalWhenBlocking();
    }

    /**
     * 重新启动时，清除标记
     */
    public void clearAlert() {
        this.alerted = false;
    }

    /**
     * 检查当前消费者的被唤醒状态
     * */
    public void checkAlert() throws MyAlertException {
        if (alerted) {
            throw MyAlertException.INSTANCE;
        }
    }
}
