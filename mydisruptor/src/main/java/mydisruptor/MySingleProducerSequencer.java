package mydisruptor;

import java.util.concurrent.locks.LockSupport;

/**
 * 单线程生产者序列器（仿Disruptor.SingleProducerSequencer）
 * 只支持单消费者的简易版本（只有一个consumerSequence）
 *
 * 因为是单线程序列器，因此在设计上就是线程不安全的
 * */
public class MySingleProducerSequencer {

    /**
     * 生产者序列器所属ringBuffer的大小
     * */
    private final int ringBufferSize;

    /**
     * 当前已发布的生产者序列号
     * （区别于nextValue）
     * */
    private final MySequence currentProducerSequence = new MySequence();

    /**
     * 生产者序列器所属ringBuffer的消费者的序列
     * */
    private final MySequence consumerSequence = new MySequence();

    /**
     * 当前已申请的序列(但是是否发布了，要看currentProducerSequence)
     *
     * 单线程生产者内部使用，所以就是普通的long，不考虑并发
     * */
    private long nextValue = -1;

    /**
     * 当前已缓存的消费者序列
     *
     * 单线程生产者内部使用，所以就是普通的long，不考虑并发
     * */
    private long cachedConsumerSequenceValue = -1;

    public MySingleProducerSequencer(int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
    }

    /**
     * 一次性申请可用的1个生产者序列号
     * */
    public long next(){
        return next(1);
    }

    /**
     * 一次性申请可用的n个生产者序列号
     * */
    public long next(int n){
        // 申请的下一个生产者位点
        long nextProducerSequence = this.nextValue + n;
        // 新申请的位点下，生产者恰好超过消费者一圈的环绕临界点序列
        long wrapPoint = nextProducerSequence - this.ringBufferSize;

        // 获得当前已缓存的消费者位点
        long cachedGatingSequence = this.cachedConsumerSequenceValue;

        // 消费者位点cachedValue并不是实时获取的（因为在没有超过环绕点一圈时，生产者是可以放心生产的）
        // 每次发布都实时获取反而会发起对消费者sequence强一致的读，迫使消费者线程所在的CPU刷新缓存（而这是不需要的）
        if(wrapPoint > cachedGatingSequence){
            long minSequence;

            // 当生产者发现确实当前已经超过了一圈，则必须去读最新的消费者序列了，看看消费者的消费进度是否推进了
            // 这里的consumerSequence.getValue是对volatile变量的读，是实时的
            while(wrapPoint > (minSequence = consumerSequence.get())){
                // 如果确实超过了一圈，则生产者无法获取可用的队列空间，循环的间歇性park阻塞
                LockSupport.parkNanos(1L);
            }

            // 满足条件了，则缓存获得的最新的消费者序列
            // 因为不是实时获取消费者序列，可能cachedValue比之前的要前进很多
            // 这种情况下，待到下一次next申请时就可以不用去volatile强一致的读consumerSequence了
            this.cachedConsumerSequenceValue = minSequence;
        }

        // 记录本次申请成功后的，已申请的生产者位点
        this.nextValue = nextProducerSequence;

        return nextProducerSequence;
    }

    public void publish(long publishIndex){
        // 发布时，直接volatile的更新生产者队列即可（注意：这里可以优化为lazySet，后续待实现）
        this.currentProducerSequence.set(publishIndex);

        // signalAllWhenBlocking();
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }
}
