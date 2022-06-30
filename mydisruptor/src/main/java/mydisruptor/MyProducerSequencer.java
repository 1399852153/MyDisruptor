package mydisruptor;

/**
 * 生产者序列器接口（仿disruptor.ProducerSequencer）
 * */
public interface MyProducerSequencer {

    /**
     * 获得一个可用的生产者序列值
     * @return 可用的生产者序列值
     * */
    long next();

    /**
     * 获得一个可用的生产者序列值区间
     * @param n 区间长度
     * @return 可用的生产者序列区间的最大值
     * */
    long next(int n);

    /**
     * 发布一个生产者序列
     * @param publishIndex 需要发布的生产者序列号
     * */
    void publish(long publishIndex);

    /**
     * 创建一个无上游消费者依赖的序列屏障
     * @return 新的序列屏障
     * */
    MySequenceBarrier newBarrier();

    /**
     * 创建一个有上游依赖的序列屏障
     * @param dependenceSequences 上游依赖的序列集合
     * @return 新的序列屏障
     * */
    MySequenceBarrier newBarrier(MySequence... dependenceSequences);

    /**
     * 向生产者注册一个消费者序列
     * @param newGatingConsumerSequence 新的消费者序列
     * */
    void addGatingConsumerSequenceList(MySequence newGatingConsumerSequence);

    /**
     * 向生产者注册一个消费者序列集合
     * @param newGatingConsumerSequences 新的消费者序列集合
     * */
    void addGatingConsumerSequenceList(MySequence... newGatingConsumerSequences);

    /**
     * 获得当前的生产者序列（cursor）
     * @return 当前的生产者序列
     * */
    MySequence getCurrentProducerSequence();

    /**
     * 获得ringBuffer的大小
     * @return ringBuffer大小
     * */
    int getRingBufferSize();

    /**
     * 获得最大的已发布的，可用的消费者序列值
     * @param nextSequence 已经明确发布了的最小生产者序列号
     * @param availableSequence 需要申请的，可能的最大的序列号
     * @return 最大的已发布的，可用的消费者序列值
     * */
    long getHighestPublishedSequence(long nextSequence, long availableSequence);
}
