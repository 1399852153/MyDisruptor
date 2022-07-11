# MyDisruptor V5版本介绍
在v4版本的MyDisruptor实现多线程生产者后。按照计划，v5版本的MyDisruptor需要支持更便于用户使用的DSL风格的API。
#####
由于该文属于系列博客的一部分，需要先对之前的博客内容有所了解才能更好地理解本篇博客
* v1版本博客：[从零开始实现lmax-Disruptor队列（一）RingBuffer与单生产者、单消费者工作原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16318972.html)
* v2版本博客：[从零开始实现lmax-Disruptor队列（二）多消费者、消费者组间消费依赖原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16361197.html)
* v3版本博客：[从零开始实现lmax-Disruptor队列（三）多线程消费者WorkerPool原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16386982.html)
* v4版本博客：[从零开始实现lmax-Disruptor队列（四）多线程生产者MultiProducerSequencer原理解析](https://www.cnblogs.com/xiaoxiongcanguan/p/16448674.html)
# 为什么Disruptor需要DSL风格的API
通过前面4个版本的迭代，MyDisruptor已经实现了disruptor的大多数功能。但对程序可读性有要求的读者可能会注意到，demo示例代码中对于构建多个消费者之间的依赖关系时细节有点多。
构建一个有上游消费者依赖的EventProcessor消费者来说需要以下几步完成：
1. 获得所要依赖的上游消费者序列集合，并在创建EventProcessor时通过参数传入
2. 通过getCurrentConsumeSequence方法获得所创建的EventProcessor对应的消费者序列对象
3. 将获得的消费者序列对象注册到RingBuffer中
4. 通过线程池或者start等方式启动EventProcessor线程，监听并消费  
**目前的版本中，每创建一个消费者都需要写一遍上述的模板代码。虽然对于理解Disruptor原理的人来说还能勉强接受，但还是很繁琐且容易在细节上犯错。更遑论对disruptor底层不大了解的普通用户了。**
基于上述原因，disruptor提供了更加简单易用的API，使得对disruptor底层各组件间依赖不甚了解的用户也能很方便的使用disruptor，并构建不同消费者组间的依赖。
# MyDisruptor实现DSL风格API介绍
### 什么是DSL风格的API?
DSL即Domain Specific Language，领域特定语言。DSL是针对特定领域抽象出的一个特定语言，通过进一层的抽象来代替大量繁琐的通用代码段，如sql、shell等都是常见的dsl。

