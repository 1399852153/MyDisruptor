package mydisruptor;

/**
 * 序列号对象（仿Disruptor.Sequence）
 *
 * 由于需要被生产者、消费者线程同时访问，因此内部是一个volatile修饰的long值
 * */
public class MySequence {

    /**
     * 序列起始值默认为-1，保证下一序列恰好是0（即第一个合法的序列号）
     * */
    private volatile long value = -1;

    public MySequence() {
    }

    public MySequence(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        this.value = value;
    }
}
