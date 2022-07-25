package mydisruptor.dsl;

import mydisruptor.MySequence;

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

    /**
     * 是否是最尾端的消费者
     * */
    boolean isEndOfChain();

    /**
     * 将当前消费者标记为不是最尾端消费者
     * */
    void markIsNotEndOfChain();

    /**
     * 当前消费者是否还在运行
     * */
    boolean isRunning();

    /**
     * 获得消费者的序列号(多线程消费者由多个序列号对象)
     * */
    MySequence[] getSequences();

}
