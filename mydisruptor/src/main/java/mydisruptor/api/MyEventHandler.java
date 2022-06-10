package mydisruptor.api;

/**
 * 单线程消费者-事件处理器接口
 * */
public interface MyEventHandler<T> {

    /**
     * 消费者消费事件
     * @param event 事件对象本身
     * @param sequence 事件对象在队列里的序列
     * @param endOfBatch 当前事件是否是这一批处理事件中的最后一个
     * */
    void consume(T event, long sequence, boolean endOfBatch);
}
