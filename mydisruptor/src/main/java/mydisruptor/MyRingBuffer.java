package mydisruptor;

import mydisruptor.api.MyEventFactory;
import mydisruptor.dsl.ProducerType;
import mydisruptor.waitstrategy.MyWaitStrategy;

import java.util.Arrays;

/**
 * 环形队列（仿Disruptor.RingBuffer）
 * */
public class MyRingBuffer<T> {

    private final T[] elementList;
    private final MyProducerSequencer myProducerSequencer;
    private final int ringBufferSize;
    private final int mask;

    public MyRingBuffer(MyProducerSequencer myProducerSequencer, MyEventFactory<T> myEventFactory) {
        int bufferSize = myProducerSequencer.getRingBufferSize();
        if (Integer.bitCount(bufferSize) != 1) {
            // ringBufferSize需要是2的倍数，类似hashMap，求余数时效率更高
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }

        this.myProducerSequencer = myProducerSequencer;
        this.ringBufferSize = bufferSize;
        this.elementList = (T[]) new Object[bufferSize];
        // 回环掩码
        this.mask = ringBufferSize-1;

        // 预填充事件对象（后续生产者/消费者都只会更新事件对象，不会发生插入、删除等操作，避免GC）
        for(int i=0; i<this.elementList.length; i++){
            this.elementList[i] = myEventFactory.newInstance();
        }
    }

    public T get(long sequence){
        // 由于ringBuffer的长度是2次幂，mask为2次幂-1，因此可以将求余运算优化为位运算
        int index = (int) (sequence & mask);
        return elementList[index];
    }

    public MySequence getCurrentProducerSequence(){
        return this.myProducerSequencer.getCurrentProducerSequence();
    }

    public long next(){
        return this.myProducerSequencer.next();
    }

    public long next(int n){
        return this.myProducerSequencer.next(n);
    }

    public void publish(Long index){
        this.myProducerSequencer.publish(index);
    }

    public void addGatingConsumerSequenceList(MySequence consumerSequence){
        this.myProducerSequencer.addGatingConsumerSequenceList(consumerSequence);
    }

    public void addGatingConsumerSequenceList(MySequence... consumerSequences){
        this.myProducerSequencer.addGatingConsumerSequenceList(consumerSequences);
    }

    public MySequenceBarrier newBarrier() {
        return this.myProducerSequencer.newBarrier();
    }

    public MySequenceBarrier newBarrier(MySequence... dependenceSequences) {
        return this.myProducerSequencer.newBarrier(dependenceSequences);
    }

    public void addConsumerSequence(MySequence consumerSequence){
        this.myProducerSequencer.addGatingConsumerSequence(consumerSequence);
    }

    public void addConsumerSequence(MySequence... gatingSequences) {
        this.myProducerSequencer.addGatingConsumerSequenceList(gatingSequences);
    }

    public void removeConsumerSequence(MySequence consumerSequence){
        this.myProducerSequencer.removeConsumerSequence(consumerSequence);
    }

    public static <E> MyRingBuffer<E> createSingleProducer(MyEventFactory<E> factory, int bufferSize, MyWaitStrategy waitStrategy) {
        MySingleProducerSequencer sequencer = new MySingleProducerSequencer(bufferSize, waitStrategy);
        return new MyRingBuffer<>(sequencer,factory);
    }

    public static <E> MyRingBuffer<E> createMultiProducer(MyEventFactory<E> factory, int bufferSize, MyWaitStrategy waitStrategy) {
        MyMultiProducerSequencer sequencer = new MyMultiProducerSequencer(bufferSize, waitStrategy);
        return new MyRingBuffer<>(sequencer,factory);
    }

    /**
     * 创建RingBuffer
     * @param producerType 生产者类型（单线程生产者 or 多线程生产者）
     * @param eventFactory 用户自定义的事件工厂
     * @param bufferSize ringBuffer的容量
     * @param myWaitStrategy 指定的消费者阻塞策略
     * */
    public static <T> MyRingBuffer<T> create(
            ProducerType producerType, MyEventFactory<T> eventFactory, int bufferSize, MyWaitStrategy myWaitStrategy) {
        switch (producerType) {
            case SINGLE: {
                MySingleProducerSequencer singleProducerSequencerV5 = new MySingleProducerSequencer(bufferSize, myWaitStrategy);
                return new MyRingBuffer<>(singleProducerSequencerV5, eventFactory);
            }
            case MULTI: {
                MyMultiProducerSequencer multiProducerSequencer = new MyMultiProducerSequencer(bufferSize, myWaitStrategy);
                return new MyRingBuffer<>(multiProducerSequencer, eventFactory);
            }
            default:
                throw new RuntimeException("un support producerType:" + producerType.toString());
        }
    }

    /**
     * 获得当前RingBuffer的大小
     * */
    public int getRingBufferSize() {
        return ringBufferSize;
    }
}
