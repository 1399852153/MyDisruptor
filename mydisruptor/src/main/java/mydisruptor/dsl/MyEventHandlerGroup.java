package mydisruptor.dsl;

import mydisruptor.MySequence;
import mydisruptor.api.MyEventHandler;
import mydisruptor.api.MyWorkHandler;

public class MyEventHandlerGroup<T> {

    private final MyDisruptor<T> disruptor;
    private final MyConsumerRepository<T> myConsumerRepository;
    private final MySequence[] sequences;

    public MyEventHandlerGroup(MyDisruptor<T> disruptor,
                               MyConsumerRepository<T> myConsumerRepository,
                               MySequence[] sequences) {
        this.disruptor = disruptor;
        this.myConsumerRepository = myConsumerRepository;
        this.sequences = sequences;
    }

    public final MyEventHandlerGroup<T> then(final MyEventHandler<? super T>... myEventConsumers) {
        return handleEventsWith(myEventConsumers);
    }

    public final MyEventHandlerGroup<T> handleEventsWith(final MyEventHandler<? super T>... handlers) {
//        return disruptor.createEventProcessors(sequences, handlers);
        return null;
    }

    public final MyEventHandlerGroup<T> thenHandleEventsWithWorkerPool(final MyWorkHandler<? super T>... handlers) {
        return handleEventsWithWorkerPool(handlers);
    }

    public final MyEventHandlerGroup<T> handleEventsWithWorkerPool(final MyWorkHandler<? super T>... handlers) {
//        return disruptor.createEventProcessors(sequences, handlers);
        return null;
    }
}

