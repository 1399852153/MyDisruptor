package mydisruptor;

import mydisruptor.api.MyEventHandler;
import mydisruptor.util.LogUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单线程消费者（仿Disruptor.BatchEventProcessor）
 * */
public class MyBatchEventProcessor<T> implements MyEventProcessor{

    private final MySequence currentConsumeSequence = new MySequence(-1);
    private final MyRingBuffer<T> myRingBuffer;
    private final MyEventHandler<T> myEventConsumer;
    private final MySequenceBarrier mySequenceBarrier;
    private final AtomicBoolean running = new AtomicBoolean();

    public MyBatchEventProcessor(MyRingBuffer<T> myRingBuffer,
                                 MyEventHandler<T> myEventConsumer,
                                 MySequenceBarrier mySequenceBarrier) {
        this.myRingBuffer = myRingBuffer;
        this.myEventConsumer = myEventConsumer;
        this.mySequenceBarrier = mySequenceBarrier;
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Thread is already running");
        }
        this.mySequenceBarrier.clearAlert();

        // 下一个需要消费的下标
        long nextConsumerIndex = currentConsumeSequence.get() + 1;

        // 消费者线程主循环逻辑，不断的尝试获取事件并进行消费（为了让代码更简单，暂不考虑优雅停止消费者线程的功能）
        while(true) {
            try {
                long availableConsumeIndex = this.mySequenceBarrier.getAvailableConsumeSequence(nextConsumerIndex);

                while (nextConsumerIndex <= availableConsumeIndex) {
                    // 取出可以消费的下标对应的事件，交给eventConsumer消费
                    T event = myRingBuffer.get(nextConsumerIndex);
                    this.myEventConsumer.consume(event, nextConsumerIndex, nextConsumerIndex == availableConsumeIndex);
                    // 批处理，一次主循环消费N个事件（下标加1，获取下一个）
                    nextConsumerIndex++;
                }

                // 更新当前消费者的消费的序列（lazySet，不需要生产者实时的强感知刷缓存性能更好，因为生产者自己也不是实时的读消费者序列的）
                this.currentConsumeSequence.lazySet(availableConsumeIndex);

                // 注释掉，为了和官方disruptor比较性能
//                LogUtil.logWithThreadName("更新当前消费者的消费的序列:" + availableConsumeIndex);
            } catch (final MyAlertException ex) {
                LogUtil.logWithThreadName("消费者MyAlertException" + ex);

                // 被外部alert打断，检查running标记
                if (!running.get()) {
                    // running == false, break跳出主循环，运行结束
                    break;
                }
            } catch (final Throwable ex) {
                // 发生异常，消费进度依然推进（跳过这一批拉取的数据）（lazySet 原理同上）
                this.currentConsumeSequence.lazySet(nextConsumerIndex);
                nextConsumerIndex++;
            }
        }
    }

    @Override
    public MySequence getCurrentConsumeSequence() {
        return this.currentConsumeSequence;
    }

    @Override
    public void halt() {
        // 当前消费者状态设置为停止
        running.set(false);

        // 唤醒消费者线程（令其能立即检查到状态为停止）
        this.mySequenceBarrier.alert();
    }

    @Override
    public boolean isRunning() {
        return this.running.get();
    }
}
