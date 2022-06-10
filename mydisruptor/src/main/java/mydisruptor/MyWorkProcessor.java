package mydisruptor;


import mydisruptor.api.MyWorkHandler;

public class MyWorkProcessor<T> implements Runnable,MyEventProcessor{

    private final MySequence currentConsumeSequence = new MySequence(-1);
    private final MyRingBuffer<T> myRingBuffer;
    private final MyWorkHandler<T> myWorkHandler;
    private final MySequenceBarrier sequenceBarrier;
    private final MySequence workGroupSequence;


    public MyWorkProcessor(MyRingBuffer<T> myRingBuffer,
                           MyWorkHandler<T> myWorkHandler,
                                MySequenceBarrier sequenceBarrier,
                                MySequence workGroupSequence) {
        this.myRingBuffer = myRingBuffer;
        this.myWorkHandler = myWorkHandler;
        this.sequenceBarrier = sequenceBarrier;
        this.workGroupSequence = workGroupSequence;
    }

    @Override
    public MySequence getCurrentConsumeSequence() {
        return currentConsumeSequence;
    }

    @Override
    public void run() {
        long nextConsumerIndex = this.currentConsumeSequence.get() + 1;
        long cachedAvailableSequence = Long.MIN_VALUE;

        // 最近是否处理过了序列
        boolean processedSequence = true;

        while (true) {
            try {
                if(processedSequence) {
                    // 如果已经处理过序列，则重新cas的争抢一个新的待消费序列
                    do {
                        nextConsumerIndex = this.workGroupSequence.get() + 1L;
                        // 由于currentConsumeSequence会被注册到生产者侧，因此需要始终和workGroupSequence worker组的实际sequence保持协调
                        // 即当前worker的消费序列currentConsumeSequence = 当前消费者组的序列workGroupSequence
                        this.currentConsumeSequence.lazySet(nextConsumerIndex - 1L);
                        // 问题：只使用workGroupSequence，每个worker不维护currentConsumeSequence行不行？
                        // 回答：这是不行的。因为和单线程消费者的行为一样，都是具体的消费者eventHandler/workHandler执行过之后才更新消费者的序列号，令其对外部可见（生产者、下游消费者）
                        // 因为消费依赖关系中约定，对于序列i事件只有在上游的消费者消费过后（eventHandler/workHandler执行过），下游才能消费序列i的事件
                        // workGroupSequence主要是用于通过cas协调同一workerPool内消费者线程序列争抢的，对外的约束依然需要workProcessor本地的消费者序列currentConsumeSequence来控制

                        // cas更新，保证每个worker线程都会获取到唯一的一个sequence
                    } while (!workGroupSequence.compareAndSet(nextConsumerIndex - 1L, nextConsumerIndex));

                    // 争抢到了一个新的待消费序列，但还未实际进行消费（标记为false）
                    processedSequence = false;
                }else{
                    // processedSequence == false(手头上存在一个还未消费的序列)
                    // 走到这里说明之前拿到了一个新的消费序列，但是由于nextConsumerIndex > cachedAvailableSequence，没有实际执行消费逻辑
                    // 而是被阻塞后返回获得了最新的cachedAvailableSequence，重新执行一次循环走到了这里
                    // 需要先把手头上的这个序列给消费掉，才能继续拿下一个消费序列
                }

                // cachedAvailableSequence只会存在两种情况
                // 1 第一次循环，初始化为Long.MIN_VALUE，则必定会走到下面的else分支中
                // 2 非第一次循环，则cachedAvailableSequence为序列屏障所允许的最大可消费序列

                if (nextConsumerIndex <= cachedAvailableSequence) {
                    // 争抢到的消费序列是满足要求的（小于序列屏障值，被序列屏障允许的），则调用消费者进行实际的消费

                    // 取出可以消费的下标对应的事件，交给eventConsumer消费
                    T event = myRingBuffer.get(nextConsumerIndex);
                    this.myWorkHandler.consume(event);

                    // 实际调用消费者进行消费了，标记为true.这样一来就可以在下次循环中cas争抢下一个新的消费序列了
                    processedSequence = true;
                } else {
                    // 1 第一次循环会获取当前序列屏障的最大可消费序列
                    // 2 非第一次循环，说明争抢到的序列超过了屏障序列的最大值，等待生产者推进到争抢到的sequence
                    cachedAvailableSequence = sequenceBarrier.getAvailableConsumeSequence(nextConsumerIndex);
                }
            } catch (final Throwable ex) {
                // 消费者消费时发生了异常，也认为是成功消费了，避免阻塞消费序列
                // 下次循环会cas争抢一个新的消费序列
                processedSequence = true;
            }
        }
    }
}
