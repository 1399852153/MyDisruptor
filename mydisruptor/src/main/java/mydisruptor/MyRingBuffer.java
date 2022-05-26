package mydisruptor;

import mydisruptor.api.MyEventFactory;

/**
 * 环形队列（仿Disruptor.RingBuffer）
 * */
public class MyRingBuffer<T> {

    private final T[] elementList;
    private final MySingleProducerSequencer mySingleProducerSequencer;
    private final MyEventFactory<T> myEventFactory;
    private final int ringBufferSize;
    private final int mask;

    public MyRingBuffer(MySingleProducerSequencer mySingleProducerSequencer , MyEventFactory<T> myEventFactory) {
        int bufferSize = mySingleProducerSequencer.getRingBufferSize();
        if (Integer.bitCount(bufferSize) != 1) {
            // ringBufferSize需要是2的倍数，类似hashMap，求余数时效率更高
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }

        this.mySingleProducerSequencer = mySingleProducerSequencer;
        this.myEventFactory = myEventFactory;
        this.ringBufferSize = bufferSize;
        this.elementList = (T[]) new Object[bufferSize];
        // 回环掩码
        this.mask = ringBufferSize;

        // 预填充事件对象（后续生产者/消费者都只会更新事件对象，不会发生插入、删除等操作，避免GC）
        for(int i=0; i<this.elementList.length; i++){
            this.elementList[i] = myEventFactory.newInstance();
        }
    }

    public T get(long sequence){
        int index = (int) (sequence % mask);
        return elementList[index];
    }

    public void publish(Long index){
        this.mySingleProducerSequencer.publish(index);
    }

    public void setConsumerSequence(MySequence consumerSequence){
        this.mySingleProducerSequencer.setConsumerSequence(consumerSequence);
    }

    public MySequenceBarrier newBarrier() {
        return this.mySingleProducerSequencer.newBarrier();
    }
}
