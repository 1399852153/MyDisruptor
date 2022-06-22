package mydisruptor;

public interface MyProducerSequencer {

    long next();

    long next(int n);

    void publish(long publishIndex);

    MySequenceBarrier newBarrier();

    MySequenceBarrier newBarrier(MySequence... dependenceSequences);

    void addGatingConsumerSequenceList(MySequence newGatingConsumerSequence);

    void addGatingConsumerSequenceList(MySequence... newGatingConsumerSequences);

    MySequence getCurrentProducerSequence();

    int getRingBufferSize();

    long getHighestPublishedSequence(long nextSequence, long availableSequence);

}
