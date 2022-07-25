package mydisruptor;

/**
 * 事件处理器（消费者）接口
 * */
public interface MyEventProcessor extends Runnable{

    /**
     * 获得当前消费者序列对象
     * @return 当前消费者序列对象
     * */
    MySequence getCurrentConsumeSequence();

    /**
     * 停止当前消费者
     * */
    void halt();

    boolean isRunning();
}
