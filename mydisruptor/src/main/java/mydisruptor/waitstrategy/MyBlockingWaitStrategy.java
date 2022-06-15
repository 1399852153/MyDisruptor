package mydisruptor.waitstrategy;

import mydisruptor.MySequence;
import mydisruptor.util.LogUtil;
import mydisruptor.util.SequenceUtil;
import mydisruptor.util.MyThreadHints;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 阻塞等待策略
 * */
public class MyBlockingWaitStrategy implements MyWaitStrategy{

    private final Lock lock = new ReentrantLock();
    private final Condition processorNotifyCondition = lock.newCondition();

    @Override
    public long waitFor(long currentConsumeSequence, MySequence currentProducerSequence, List<MySequence> dependentSequences)
            throws InterruptedException {
        // 强一致的读生产者序列号
        if (currentProducerSequence.get() < currentConsumeSequence) {
            // 如果ringBuffer的生产者下标小于当前消费者所需的下标，说明目前消费者消费速度大于生产者生产速度

            lock.lock();
            try {
                //
                while (currentProducerSequence.get() < currentConsumeSequence) {
                    // 消费者的消费速度比生产者的生产速度快，阻塞等待
                    processorNotifyCondition.await();
                }
            }
            finally {
                lock.unlock();
            }
        }

        // 跳出了上面的循环，说明生产者序列已经超过了当前所要消费的位点（currentProducerSequence > currentConsumeSequence）
        long availableSequence;
        if(!dependentSequences.isEmpty()){
            // 受制于屏障中的dependentSequences，用来控制当前消费者消费进度不得超过其所依赖的链路上游的消费者进度
            while ((availableSequence = SequenceUtil.getMinimumSequence(dependentSequences)) < currentConsumeSequence) {
                // 由于消费者消费速度一般会很快，所以这里使用自旋阻塞来等待上游消费者进度推进（响应及时，且实现简单）
                LogUtil.logWithThreadName("dependentSequences= "+ dependentSequences + "availableSequence=" + availableSequence);
                // 在jdk9开始引入的Thread.onSpinWait方法，优化自旋性能
                MyThreadHints.onSpinWait();
            }
        }else{
            // 并不存在依赖的上游消费者，大于当前消费进度的生产者序列就是可用的消费序列
            availableSequence = currentProducerSequence.get();
        }

        return availableSequence;
    }

    @Override
    public void signalWhenBlocking() {
        lock.lock();
        try {
            // signal唤醒所有阻塞在条件变量上的消费者线程（后续支持多消费者时，会改为signalAll）
            processorNotifyCondition.signalAll();
        }
        finally {
            lock.unlock();
        }
    }
}
