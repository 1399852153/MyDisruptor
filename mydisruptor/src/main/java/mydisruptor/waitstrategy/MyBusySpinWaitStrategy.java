package mydisruptor.waitstrategy;

import mydisruptor.MyAlertException;
import mydisruptor.MySequence;
import mydisruptor.MySequenceBarrier;
import mydisruptor.util.MyThreadHints;
import mydisruptor.util.SequenceUtil;

import java.util.List;

public class MyBusySpinWaitStrategy implements MyWaitStrategy{
    @Override
    public long waitFor(long currentConsumeSequence, MySequence currentProducerSequence, List<MySequence> dependentSequences,
                        MySequenceBarrier barrier) throws MyAlertException {
        long availableSequence;

        while ((availableSequence = SequenceUtil.getMinimumSequence(dependentSequences)) < currentConsumeSequence) {
            // 每次循环都检查运行状态
            barrier.checkAlert();

            // 由于消费者消费速度一般会很快，所以这里使用自旋阻塞来等待上游消费者进度推进（响应及时，且实现简单）
            // 在jdk9开始引入的Thread.onSpinWait方法，优化自旋性能
            MyThreadHints.onSpinWait();
        }

        return availableSequence;
    }

    @Override
    public void signalWhenBlocking() {

    }
}
