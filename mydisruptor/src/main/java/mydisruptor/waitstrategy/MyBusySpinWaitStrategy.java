package mydisruptor.waitstrategy;

import mydisruptor.MySequence;
import mydisruptor.util.LogUtil;
import mydisruptor.util.MyThreadHints;
import mydisruptor.util.SequenceUtil;

import java.util.List;

public class MyBusySpinWaitStrategy implements MyWaitStrategy{
    @Override
    public long waitFor(long currentConsumeSequence, MySequence currentProducerSequence, List<MySequence> dependentSequences) throws InterruptedException {
        long availableSequence;

        while ((availableSequence = SequenceUtil.getMinimumSequence(dependentSequences)) < currentConsumeSequence) {
            // 由于消费者消费速度一般会很快，所以这里使用自旋阻塞来等待上游消费者进度推进（响应及时，且实现简单）
            LogUtil.logWithThreadName("dependentSequences= "+ dependentSequences + "availableSequence=" + availableSequence);
            // 在jdk9开始引入的Thread.onSpinWait方法，优化自旋性能
            MyThreadHints.onSpinWait();
        }

        return availableSequence;
    }

    @Override
    public void signalWhenBlocking() {

    }
}
