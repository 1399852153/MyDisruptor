package mydisruptor.dsl;

import java.util.concurrent.Executor;

/**
 * 消费者信息 （仿Disruptor.ConsumerInfo）
 * */
public interface MyConsumerInfo {

    /**
     * 通过executor启动当前消费者
     * @param executor 启动器
     * */
    void start(Executor executor);

    /**
     * 停止当前消费者
     * */
    void halt();
}
