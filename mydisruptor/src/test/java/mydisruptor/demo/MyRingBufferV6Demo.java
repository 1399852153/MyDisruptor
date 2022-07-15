package mydisruptor.demo;

import mydisruptor.MyRingBuffer;
import mydisruptor.dsl.MyDisruptor;
import mydisruptor.dsl.ProducerType;
import mydisruptor.waitstrategy.MyBlockingWaitStrategy;

public class MyRingBufferV6Demo {

    public static void main(String[] args) {
        MyDisruptor<String> disruptor = new MyDisruptor<>(
                () -> "123", 16, command -> new Thread(command), ProducerType.SINGLE,
                new MyBlockingWaitStrategy()
        );

        MyRingBuffer<String> ringBuffer = disruptor.getRingBuffer();
        System.out.println(ringBuffer);
    }
}
