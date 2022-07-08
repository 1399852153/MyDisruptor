package mydisruptor.dsl;

import mydisruptor.MyEventProcessor;

import java.util.concurrent.Executor;

/**
 * 单线程事件处理器信息（仿Disruptor.EventProcessorInfo）
 * */
public class MyEventProcessorInfo<T> implements MyConsumerInfo {

    private final MyEventProcessor myEventProcessor;

    public MyEventProcessorInfo(MyEventProcessor myEventProcessor) {
        this.myEventProcessor = myEventProcessor;
    }

    @Override
    public void start(Executor executor) {
        executor.execute(myEventProcessor);
    }
}
