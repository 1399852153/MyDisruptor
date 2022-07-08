package mydisruptor.dsl;

import mydisruptor.MySequence;
import mydisruptor.api.MyEventHandler;
import mydisruptor.api.MyWorkHandler;

/**
 * 事件处理器组（仿Disruptor.EventHandlerGroup）
 * */
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

    public final MyEventHandlerGroup<T> then(final MyEventHandler<T>... myEventHandlers) {
        return handleEventsWith(myEventHandlers);
    }

    public final MyEventHandlerGroup<T> handleEventsWith(final MyEventHandler<T>... handlers) {
        return disruptor.createEventProcessors(sequences, handlers);
    }

    public final MyEventHandlerGroup<T> thenHandleEventsWithWorkerPool(final MyWorkHandler<T>... handlers) {
        return handleEventsWithWorkerPool(handlers);
    }

    public final MyEventHandlerGroup<T> handleEventsWithWorkerPool(final MyWorkHandler<T>... handlers) {
        return disruptor.createWorkerPool(sequences, handlers);
    }
}

