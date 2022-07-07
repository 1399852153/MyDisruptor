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
    private final MyWaitStrategy myWaitStrategy;
    private final List<MySequence> dependentSequencesList;

    public MySequenceBarrier(MyProducerSequencer myProducerSequencer, MySequence currentProducerSequence,
                             MyWaitStrategy myWaitStrategy, List<MySequence> dependentSequencesList) {
        this.myProducerSequencer = myProducerSequencer;
        this.currentProducerSequence = currentProducerSequence;
        this.myWaitStrategy = myWaitStrategy;

        if(!dependentSequencesList.isEmpty()) {
            this.dependentSequencesList = dependentSequencesList;
        }else{
            // 如果传入的上游依赖序列为空，则生产者序列号作为兜底的依赖
            this.dependentSequencesList = Collections.singletonList(currentProducerSequence);
        }
    }

    /**
     * 获得可用的消费者下标（disruptor中的waitFor）
     * */
    public long getAvailableConsumeSequence(long currentConsumeSequence) throws InterruptedException {
        long availableSequence = this.myWaitStrategy.waitFor(currentConsumeSequence,currentProducerSequence,dependentSequencesList);

        if (availableSequence < currentConsumeSequence) {
            return availableSequence;
        }

        // 多线程生产者中，需要进一步约束（于v4版本新增）
        return myProducerSequencer.getHighestPublishedSequence(currentConsumeSequence,availableSequence);
    }
}
