package mydisruptor;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

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

    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;

    static {
        try {
            // 由于提供给cas内存中字段偏移量的unsafe类只能在被jdk信任的类中直接使用，这里使用反射来绕过这一限制
            Field getUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            getUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) getUnsafe.get(null);
            VALUE_OFFSET = UNSAFE.objectFieldOffset(MySequence.class.getDeclaredField("value"));
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public void lazySet(long value) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }

    public boolean compareAndSet(long expect, long update){
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expect, update);
    }

    @Override
    public String toString() {
        return Long.toString(get()) + " " + ownerName;
    }

    public String ownerName;
}
