package mydisruptor.api;

/**
 * 队列事件对象工厂（仿Disruptor.EventFactory）
 * */
public interface MyEventFactory<T> {

    /**
     * 创建一个裸事件对象实例
     * */
    T newInstance();
}
