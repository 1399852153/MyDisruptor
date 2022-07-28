## 分享个人学习disruptor这一内存队列框架的心得  
不同于大多数博客直接分析disruptor的源码，而是尝试着自己动手参考着disruptor从简单到复杂一步步的实现disruptor  
在实现的过程中穿插着讲解disruptor中各个组件的关联和其中精妙的实现细节  
相比官方完整版的disruptor，早期版本的MyDisruptor代码会精简很多，可以让读者更容易理解当前功能的实现原理，而不会被其余旁路代码的复杂度给绕晕  
本博客会按照以下顺序，由简单到复杂的实现一个类disruptor的框架MyDisruptor
1. ringBuffer + 单线程生产者 + 单线程消费者
2. 多线程消费者 + 消费者组依赖关系（A/B -> C, AB消费成功后C才能消费）
3. worker线程组消费者  
4. 多线程生产者
5. disruptor dsl(提供简单易用的接口，屏蔽掉人工组装依赖链的复杂度)
6. ringBuffer等关键组件解决伪共享问题 + 参考disruptor对特定的数据结构做进一步优化  

每一部分都会有单独的分支和对应的技术博客（已完结）
1. v1版本分支:  feature/lab1  
   [从零开始实现lmax-Disruptor队列（一）RingBuffer与单生产者、单消费者工作原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16318972.html)
2. v2版本分支:  feature/lab2  
   [从零开始实现lmax-Disruptor队列（二）多消费者、消费者组间消费依赖原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16361197.html)
3. v3版本分支:  feature/lab3  
   [从零开始实现lmax-Disruptor队列（三）多线程消费者WorkerPool原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16386982.html)
4. v4版本分支:  feature/lab4  
   [从零开始实现lmax-Disruptor队列（四）多线程生产者MultiProducerSequencer原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16448674.html)
5. v5版本分支:  feature/lab5  
   [从零开始实现lmax-Disruptor队列（五）Disruptor DSL风格API原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16479148.html)
5. v5版本分支:  feature/lab6  
   [从零开始实现lmax-Disruptor队列（六）Disruptor 解决伪共享、消费者优雅停止实现原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16530544.html)