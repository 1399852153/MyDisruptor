# MyDisruptor V6版本介绍
在v5版本的MyDisruptor实现DSL风格的API后。按照计划，v6版本的MyDisruptor作为最后一个版本，需要对MyDisruptor进行最终的一些细节优化。
v6版本一共做了三处优化：
* 解决伪共享问题
* 支持消费者线程优雅停止
* 生产者序列器中维护消费者序列集合的数据结构由ArrayList优化为数组Array类型(减少ArrayList在get操作时额外的rangeCheck检查)
#####
由于该文属于系列博客的一部分，需要先对之前的博客内容有所了解才能更好地理解本篇博客
* v1版本博客：[从零开始实现lmax-Disruptor队列（一）RingBuffer与单生产者、单消费者工作原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16318972.html)
* v2版本博客：[从零开始实现lmax-Disruptor队列（二）多消费者、消费者组间消费依赖原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16361197.html)
* v3版本博客：[从零开始实现lmax-Disruptor队列（三）多线程消费者WorkerPool原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16386982.html)
* v4版本博客：[从零开始实现lmax-Disruptor队列（四）多线程生产者MultiProducerSequencer原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16448674.html)
* v5版本博客：[从零开始实现lmax-Disruptor队列（五）Disruptor DSL风格API原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16479148.html)
# 解决伪共享问题原理详解
**在第一篇博客中我们就已经介绍过伪共享问题了，这里复制原博客内容如下:**  
_现代的CPU都是多核的，每个核心都拥有独立的高速缓存。高速缓存由固定大小的缓存行组成（通常为32个字节或64个字节）。_  
_CPU以缓存行作为最小单位读写，且一个缓存行通常会被多个变量占据（例如32位的引用指针占4字节，64位的引用指针占8个字节）。_    
_这样的设计导致了一个问题：即使缓存行上的变量是无关联的（比如不属于同一个对象），但只要缓存行上的某一个共享变量发生了变化，则整个缓存行都会进行缓存一致性的同步。_  
_而CPU间缓存一致性的同步是有一定性能损耗的，能避免则尽量避免。这就是所谓的“**伪共享**”问题。_    
_disruptor通过对队列中一些关键变量进行了缓存行的填充，避免其因为不相干的变量读写而无谓的刷新缓存，解决了伪共享的问题。_
### 举例展示伪共享问题对性能的影响
* 假设存在一个Point对象，其中有两个volatile修饰的long类型字段，x和y。  
  有两个线程并发的访问一个Point对象，但其中一个线程1只读写x字段，而另一个线程2只读写y字段。  
* 这是一个典型的伪共享场景，两个线程各自独立访问两个不同的数据，但x和y是连续分布的，因此大概率读写时会被放到同一个高速缓存行中，
  由于volatile变量修饰的原因，线程1对x线程的修改会对当前缓存行进行触发高速缓存间同步进行强一致地写，使得线程2中x、y字段所在CPU的高速缓存行失效，被迫重新读取主存中最新的数据。  
  但实际上线程1读写x和线程2读写y是完全不相关的，线程1与线程2在业务上实际并不共享同一片内存空间，因此强一致的高速缓存同步完全是画蛇添足，只会降低性能。
```java
public class Point {
    public volatile int x;
    public volatile int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```
```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FalseSharingDemo {

    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Point point = new Point(1,2);
        long start = System.currentTimeMillis();
        executor.execute(()->{
            // 线程1 x自增1亿次
            for(int i=0; i<100000000; i++){
                point.x++;
            }
            countDownLatch.countDown();
        });

        executor.execute(()->{
            // 线程2 y自增1亿次
            for(int i=0; i<100000000; i++){
                point.y++;
            }
            countDownLatch.countDown();
        });

        countDownLatch.await();
        long end = System.currentTimeMillis();
        System.out.println("testNormal 耗时=" + (end-start));
        executor.shutdown();
    }
}
```

# 支持消费者线程优雅停止详解
# 消费者序列集合的数据结构由ArrayList优化为数组详解