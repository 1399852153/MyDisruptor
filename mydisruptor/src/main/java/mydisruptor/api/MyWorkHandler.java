package mydisruptor.api;

/**
 * 多线程消费者-事件处理器接口
 * */
public interface MyWorkHandler<T> {

    /**
     * 消费者消费事件
     * @param event 事件对象本身
     * */
    void consume(T event);
}
