package mydisruptor.dsl;

import mydisruptor.MyEventProcessor;
import mydisruptor.MySequence;

import java.util.concurrent.Executor;

/**
 * 单线程事件处理器信息（仿Disruptor.EventProcessorInfo）
 * */
public class MyEventProcessorInfo<T> implements MyConsumerInfo {

    private final MyEventProcessor myEventProcessor;

    /**
     * 默认是最尾端的消费者
     * */
    private boolean endOfChain = true;

    public MyEventProcessorInfo(MyEventProcessor myEventProcessor) {
        this.myEventProcessor = myEventProcessor;
    }

    @Override
    public void start(Executor executor) {
        executor.execute(myEventProcessor);
    }

    @Override
    public void halt() {
        this.myEventProcessor.halt();
    }

    @Override
    public boolean isEndOfChain() {
        return endOfChain;
    }

    @Override
    public void markIsNotEndOfChain() {
        this.endOfChain = false;
    }

    @Override
    public boolean isRunning() {
        return this.myEventProcessor.isRunning();
    }

    @Override
    public MySequence[] getSequences() {
        return new MySequence[]{this.myEventProcessor.getCurrentConsumeSequence()};
    }
}
