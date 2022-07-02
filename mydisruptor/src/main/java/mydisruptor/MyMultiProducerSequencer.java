package mydisruptor;

import mydisruptor.util.SequenceUtil;
import mydisruptor.util.UnsafeUtil;
import mydisruptor.waitstrategy.MyWaitStrategy;
import sun.misc.Unsafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * 多线程生产者（仿disruptor.MultiProducerSequencer）
 */
public class MyMultiProducerSequencer implements MyProducerSequencer{

    private final int ringBufferSize;
    private final MySequence currentProducerSequence = new MySequence();
    private final List<MySequence> gatingConsumerSequenceList = new ArrayList<>();
    private final MyWaitStrategy myWaitStrategy;

    private final MySequence gatingSequenceCache = new MySequence();
    private final int[] availableBuffer;
    private final int indexMask;
    private final int indexShift;

    /**
     * 通过unsafe访问availableBuffer数组，可以在读写时按需插入读/写内存屏障
     */
    private static final Unsafe UNSAFE = UnsafeUtil.getUnsafe();
    private static final long BASE = UNSAFE.arrayBaseOffset(int[].class);
    private static final long SCALE = UNSAFE.arrayIndexScale(int[].class);

    public MyMultiProducerSequencer(int ringBufferSize, final MyWaitStrategy myWaitStrategy) {
        this.ringBufferSize = ringBufferSize;
        this.myWaitStrategy = myWaitStrategy;
        this.availableBuffer = new int[ringBufferSize];
        this.indexMask = this.ringBufferSize - 1;
        this.indexShift = log2(ringBufferSize);
        initialiseAvailableBuffer();
    }

    private void initialiseAvailableBuffer() {
        for (int i = availableBuffer.length - 1; i >= 0; i--) {
            this.availableBuffer[i] = -1;
        }
    }

    private static int log2(int i) {
        int r = 0;
        while ((i >>= 1) != 0) {
            ++r;
        }
        return r;
    }

    @Override
    public long next() {
        return next(1);
    }

    @Override
    public long next(int n) {
        do {
            // 保存申请前的最大生产者序列
            long currentMaxProducerSequenceNum = currentProducerSequence.get();
            // 申请之后的生产者位点
            long nextProducerSequence = currentMaxProducerSequenceNum + n;

            // 新申请的位点下，生产者恰好超过消费者一圈的环绕临界点序列
            long wrapPoint = nextProducerSequence - this.ringBufferSize;
            // 获得当前已缓存的消费者位点(使用Sequence对象维护位点，volatile的读。因为多生产者环境下，多个线程会并发读写gatingSequenceCache)
            long cachedGatingSequence = this.gatingSequenceCache.get();

            // 消费者位点cachedValue并不是实时获取的（因为在没有超过环绕点一圈时，生产者是可以放心生产的）
            // 每次发布都实时获取反而会触发对消费者sequence强一致的读，迫使消费者线程所在的CPU刷新缓存（而这是不需要的）
            if(wrapPoint > cachedGatingSequence){
                long gatingSequence = SequenceUtil.getMinimumSequence(currentMaxProducerSequenceNum, this.gatingConsumerSequenceList);
                if(wrapPoint > gatingSequence){
                    // 如果确实超过了一圈，则生产者无法获取队列空间
                    LockSupport.parkNanos(1);
                    // park短暂阻塞后continue跳出重新进入循环
                    continue;

                    // 为什么不能像单线程生产者一样在这里while循环park？
                    // 因为别的生产者线程也在争抢currentMaxProducerSequence，如果在这里直接阻塞，会导致当前拿到的序列号可能也被别的线程获取到
                    // 但最终是否可用需要通过cas的结果来决定，所以每次循环必须重新获取gatingSequenceCache最新的值
                }

                // 满足条件了，则缓存获得最新的消费者序列
                // 因为不是实时获取消费者序列，可能gatingSequence比上一次的值要大很多
                // 这种情况下，待到下一次next申请时就可以不用去强一致的通过getMinimumSequence读consumerSequence了（走else分支）
                this.gatingSequenceCache.set(gatingSequence);
            }else {
                if (this.currentProducerSequence.compareAndSet(currentMaxProducerSequenceNum, nextProducerSequence)) {
                    // 由于是多生产者序列，可能存在多个生产者同时执行next方法申请序列，因此只有cas成功的线程才视为申请成功，可以跳出循环
                    return nextProducerSequence;
                }

                // cas更新失败，重新循环获取最新的消费位点
                // continue;
            }
        }while (true);
    }

    @Override
    public void publish(long publishIndex) {
        setAvailable(publishIndex);
        this.myWaitStrategy.signalWhenBlocking();
    }

    @Override
    public MySequenceBarrier newBarrier() {
        return new MySequenceBarrier(this,this.currentProducerSequence,this.myWaitStrategy,new ArrayList<>());
    }

    @Override
    public MySequenceBarrier newBarrier(MySequence... dependenceSequences) {
        return new MySequenceBarrier(this,this.currentProducerSequence,this.myWaitStrategy,new ArrayList<>(Arrays.asList(dependenceSequences)));

    }

    @Override
    public void addGatingConsumerSequenceList(MySequence newGatingConsumerSequence) {
        this.gatingConsumerSequenceList.add(newGatingConsumerSequence);
    }

    @Override
    public void addGatingConsumerSequenceList(MySequence... newGatingConsumerSequences) {
        this.gatingConsumerSequenceList.addAll(Arrays.asList(newGatingConsumerSequences));
    }

    @Override
    public MySequence getCurrentProducerSequence() {
        return this.currentProducerSequence;
    }

    @Override
    public int getRingBufferSize() {
        return this.ringBufferSize;
    }

    @Override
    public long getHighestPublishedSequence(long lowBound, long availableSequence) {
        // lowBound是消费者传入的，保证是已经明确发布了的最小生产者序列号
        // 因此，从lowBound开始，向后寻找,有两种情况
        // 1 在lowBound到availableSequence中间存在未发布的下标(isAvailable(sequence) == false)，
        // 那么，找到的这个未发布下标的前一个序列号，就是当前最大的已经发布了的序列号（可以被消费者正常消费）
        // 2 在lowBound到availableSequence中间不存在未发布的下标，那么就和单生产者的情况一样
        // 包括availableSequence以及之前的序列号都已经发布过了，availableSequence就是当前可用的最大的的序列号（已发布的）
        for(long sequence = lowBound; sequence <= availableSequence; sequence++){
            if (!isAvailable(sequence)) {
                // 属于上述的情况1，lowBound和availableSequence中间存在未发布的序列号
                return sequence - 1;
            }
        }

        // 属于上述的情况2，lowBound和availableSequence中间不存在未发布的序列号
        return availableSequence;
    }

    private void setAvailable(long sequence){
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);

        // 计算index对应下标相对于availableBuffer引用起始位置的指针偏移量
        long bufferAddress = (index * SCALE) + BASE;

        // 功能上等价于this.availableBuffer[index] = flag，但添加了写屏障防止和对事件对象的更新逻辑之间出现重排序
        // 和单线程生产者中的lazySet作用一样，保证了对publish发布的event事件对象的更新一定先于对availableBuffer对应下标值的更新
        UNSAFE.putOrderedInt(availableBuffer, bufferAddress, flag);
    }

    private int calculateAvailabilityFlag(long sequence) {
        return (int) (sequence >>> indexShift);
    }

    private int calculateIndex(long sequence) {
        return ((int) sequence) & indexMask;
    }

    public boolean isAvailable(long sequence) {
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);

        // 计算index对应下标相对于availableBuffer引用起始位置的指针偏移量
        long bufferAddress = (index * SCALE) + BASE;

        // 功能上等价于this.availableBuffer[index] == flag
        // 但是添加了读屏障保证了强一致的读，可以让消费者实时的获取到生产者新的发布
        return UNSAFE.getIntVolatile(availableBuffer, bufferAddress) == flag;
    }
}
