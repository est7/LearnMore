在执行程序时，为了提高性能，编译器和处理器常常会对指令做重排序。重排序分3种类型：

1. 编译器优化的重排序。编译器在**不改变单线程程序语义**的前提下，可以重新安排语句的执行顺序。
2. 指令级并行的重排序。现代处理器采用了指令级并行技术（Instruction-Level Parallelism，ILP）来将多条指令重叠执行。如果不存在**数据依赖性**，处理器可以改变语句对应机器指令的执行顺序。
3. 内存系统的重排序。由于处理器使用缓存和读/写缓冲区，这使得加载和存储操作看上去可能是在乱序执行。

1属于编译器重排序，2和3属于处理器重排序。从Java源代码到最终实际执行的指令序列，会分别经历下面3种重排序：



![1613703954266](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613703954266.png)

Java中的指令重排.png

# as-if-serial 语义

as-if-serial的意思是：不管指令怎么重排序，在**单线程**下执行结果不能被改变。不管是编译器级别还是处理器级别的重排序都必须遵循as-if-serial语义。

为了遵守as-if-serial语义，编译器和处理器不会对存在**数据依赖关系**的操作做重排序。但是as-if-serial规则允许对**有控制依赖关系**的指令做重排序，因为在单线程程序中，对存在控制依赖的操作重排序，不会改变执行结果，但是多线程下确有可能会改变结果。

## 数据依赖



```java
int a = 1; // 1
int b = 2; // 2
int c = a + b; // 3
```

上述代码，`a`和`b`不存在依赖关系，所以1、2可以进行重排序；`c`依赖 `a`和`b` ，所以3必须在1、2的后面执行。

## 控制依赖



```java
public void use(boolean flag, int a, int b) {
    if (flag) { // 1
        int i = a * b; // 2
    }
}
```

`flag`和`i`存在控制依赖关系。当指令重排序后，2这一步会将结果值写入重排序缓冲（Reorder Buffer，ROB）的硬件缓存中，当判断为true时，再把结果值写入变量ｉ中。

# happens-before 语义

JSR-133使用happens-before的概念来阐述操作之间的内存可见性。**在JMM中，如果一个操作执行的结果需要对另一个操作可见，那么这两个操作之间必须要存在happens-before关系。**这里提到的两个操作既可以是在一个线程之内，也可以是在不同线程之间。

> 两个操作之间具有happens-before关系，并不意味着前一个操作必须要在后一个 操作之前执行！happens-before仅仅要求前一个操作（执行的结果）对后一个操作可见，且前一 个操作按顺序排在第二个操作之前（the first is visible to and ordered before the second）。

## happens-before 部分规则

1. **程序顺序规则：**一个线程中的每个操作，happens-before于该线程中的任意后续操作。
    主要含义是：在一个线程内不管指令怎么重排序，程序运行的结果都不会发生改变。和as-if-serial 比较像。
2. **监视器锁规则：**对一个锁的解锁，happens-before于随后对这个锁的加锁。
    主要含义是：同一个锁的解锁一定发生在加锁之后
3. **管程锁定规则：**一个线程获取到锁后，它能看到前一个获取到锁的线程所有的操作结果。
    主要含义是：无论是在单线程环境还是多线程环境，对于同一个锁来说，一个线程对这个锁解锁之后，另一个线程获取了这个锁都能看到前一个线程的操作结果！(管程是一种通用的同步原语，synchronized就是管程的实现）
4. **volatile变量规则：**对一个volatile域的写，happens-before于任意后续对这个volatile域的读。
    主要含义是：如果一个线程先去写一个volatile变量，然后另一个线程又去读这个变量，那么这个写操作的结果一定对读的这个线程可见。
5. **传递性：**如果A happens-before B，且B happens-before C，那么A happens-before C。
6. **start()规则：**如果线程A执行操作ThreadB.start()（启动线程B），那么A线程的ThreadB.start()操作happens-before于线程B中的任意操作。
    主要含义是：线程A在启动子线程B之前对共享变量的修改结果对线程B可见。
7. **join()规则：**如果线程A执行操作ThreadB.join()并成功返回，那么线程B中的任意操作happens-before于线程A从ThreadB.join()操作成功返回。
    主要含义是：如果在线程A执行过程中调用了线程B的join方法，那么当B执行完成后，在线程B中所有操作结果对线程A可见。
8. **线程中断规则：**对线程interrupt方法的调用happens-before于被中断线程的代码检测到中断事件的发生。
    主要含义是：响应中断一定发生在发起中断之后。
9. **对象终结规则：**就是一个对象的初始化的完成，也就是构造函数执行的结束一定 happens-before它的finalize()方法。

一个happens-before规则对应于一个或多个编译器和处理器重排序规则。

> as-if-serial和happens-before的主要作用都是：在保证不改变程序运行结果的前提下，允许部分指令的重排序，最大限度的提升程序执行的效率。

# 内存屏障

我们先来看一个并发环境下指令重排序带来的问题：


![1613703986963](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613703986963.png)

并发环境下指令重排序带来的问题.png


 这里有两个线程A和线程B，当A执行方法时发生了指令重排，2先执行，这时线程B执行方法，这时我们拿到的变量a却还是0，所以最后得到的结果 i=0，而不是i=1。

如何解决上述问题呢？一种是使用内存屏障（volatile），另一种使用临界区（synchronized ）。

如果我们使用内存屏障，那么JMM的处理器，会要求Java编译器在生成指令序列时，插入特定类型的内存屏障（Memory Barriers，Intel称之为 Memory Fence）指令，通过内存屏障指令来禁止特定类型的处理器重排序。

## 内存屏障的类型

![1613704009187](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704009187.png)

内存屏障的类型.png

StoreLoad Barriers是一个“全能型”的屏障，它同时具有其他3个屏障的效果。现代的多处理器大多支持该屏障（其他类型的屏障不一定被所有处理器支持）。执行该屏障开销会很昂 贵，因为当前处理器通常要把写缓冲区中的数据全部刷新到内存中（Buffer Fully Flush）。

常见处理器允许的重排序类型的列表，“N”表示处理器不允许两个操作重排序，“Y”表示允许重排序：

![1613704129490](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704129490.png)

常见处理器允许的重排序类型的列表.png

那么上面的问题，我们可以在`flag`处插入一个内存屏障，其作用是：保证在`init()`方法中，第1步操作一定在第2步之前，禁止第1步和第2步操作出现指令重排序，代码如下：



```java
public class ControlDep {
    int a = 0;
    volatile boolean flag = false;

    public void init() {
        a = 1; // 1
        flag = true; // 2
        //.......
    }

    public void use() {
        if (flag) { // 3
            int i = a * a; // 4
        }
        //.......
    }
}
```

> A线程在写volatile变量之前所有可见的共享变量，在B线程读同一个volatile变量后，将立即变得对B线程可见。也就是说程序执行执行完第2步的时候，处理器会将第2步和其之前的所有结果强制刷新到主内存。也就是说`a=1`也会被强制刷新到主内存中。那么当另一个线程执行到步骤3的时候，如果判断到`flag=true`时，那么第4步处a一定是等于1的，这样就保证了程序的正确运行。

# 顺序一致性

顺序一致性内存模型是一个被计算机科学家理想化了的理论参考模型，它为程序员提供了极强的内存可见性保证。顺序一致性内存模型有两大特性：

1. 一个线程中的所有操作必须按照程序的顺序来执行。
2. （不管程序是否同步）所有线程都只能看到一个单一的操作执行顺序。在顺序一致性内存模型中，每个操作都必须原子执行且立刻对所有线程可见。

JMM对正确同步的多线程程序的内存一致性做了如下保证：如果程序是正确同步的，程序的执行将具有顺序一致性（Sequentially Consistent）——即程序的执行结果与该程序在顺序一致性内存模型中的执行结果相同。

我们看到JMM仅仅是保证了程序运行的结果是和顺序执行是一致，并没有实现真正的顺一致性。它又是怎么实现的呢？JMM使用了临界区（加锁）来保证程序的顺序执行，但是在临界区内是允许出现指令重排的（JMM不允许临界区内的代码“逸出”到临界区之外，那样会破坏监视器的语义）。

我们在回过来看下上面遇到的并发问题，在上面我们说了使用内存屏障来解决，这里我们使用临界区。



```java
public class ControlDep {
    int a = 0;
    boolean flag = false;

    public synchronized void init() {
        a = 1; // 1
        flag = true; // 2
        //.......
    }

    public synchronized void use() {
        if (flag) { // 3
            int i = a * a; // 4
        }
        //.......
    }
}
```

虽然线程A执行`init()`方法时，在临界区内做了重排序，但由于监视器互斥执行的特性，线程B执行`use()`方法时，根本无法“观察”到线程A在临界区内的重排序。这种重排序既提高了执行效率，又没有改变程序的执行结果。


![1613704044644](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704044644.png)

两个内存模型中的执行时序对比图.jpg



从这里我们可以看到，JMM在具体实现上的基本方针为：在不改变（正确同步的）程序执 行结果的前提下，尽可能地为编译器和处理器的优化打开方便之门。

# volatile的内存语义

## volatile的特性

-  **可见性：**对一个volatile变量的读，总是能看到（任意线程）对这个volatile变量最后的写入
-  **原子性：**对任意单个volatile变量的读/写具有原子性，但类似于volatile++这种复合操作不具有原子性

理解volatile特性的一个好方法是把对volatile变量的单个读/写，看成是使用同一个锁对这 些单个读/写操作做了同步。下面通过具体的示例来说明，示例代码如下：



```java
class VolatileFeaturesExample {
    // 使用volatile声明64位的long型变量
    volatile long vl = 0L;

    // 普通long型变量
    volatile long v2 = 0L;

    public void set(long l) {
        // 单个volatile变量的写
        vl = l;
    }

    public synchronized void syncSet(long l) {
        // 单个volatile变量的写执行效果等价于对普通变量的加同步锁来写
        v2 = l;
    }

    public long get() {
        // 单个volatile变量的读
        return vl;
    }

    public synchronized long syncGet() {
        // 单个volatile变量的读执行效果等价于对普通变量的加同步锁来读
        return vl;
    }

    public void getAndIncrement() {
        // 复合（多个）volatile变量的读/写 不具备原子性
        vl++;
        // v1++ 等价于 如下代码(不具备原子性)
        long temp = syncGet();
        temp = temp + 1;
        syncSet(temp);
    }
}
```

## volatile写和读的内存语义

- volatile写的内存语义: 当写一个volatile变量时，JMM会把该线程对应的本地内存中的所有共享变量值刷新到主内存

  ![1613704083762](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704083762.png)

  volatile写的内存语义.png

- volatile读的内存语义：当读一个volatile变量时，JMM会把该线程对应的本地内存置为无效。线程接下来将从主内存中读取所有共享变量。

  ![1613704101579](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704101579.png)

  volatile读的内存语义.png

## volatile内存语义的实现

为了实现volatile的内存语义，编译器在生成字节码时，会在指令序列中插入内存屏障来禁止特定类型的处理器重排序。

### 具体限制规则如下

![1613704159284](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704159284.png)

volatile重排序规则表.jpg

- 当第二个操作是volatile写时，不管第一个操作是什么，都不能重排序。这个规则确保 volatile写之前的操作不会被编译器重排序到volatile写之后。
- 当第一个操作是volatile读时，不管第二个操作是什么，都不能重排序。这个规则确保 volatile读之后的操作不会被编译器重排序到volatile读之前。
- 当第一个操作是volatile写，第二个操作是volatile读时，不能重排序。

### 具体插入的内存屏障

- 在每个volatile写操作的前面插入一个StoreStore屏障。在每个volatile写操作的后面插入一个StoreLoad屏障。

  ![1613704171765](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704171765.png)

  volatile写操插入的内存屏障.png

- 在每个volatile读操作的后面插入一个LoadLoad屏障。在每个volatile读操作的后面插入一个LoadStore屏障。

  ![1613704186874](.art/java%E6%8C%87%E4%BB%A4%E9%87%8D%E6%8E%92.assets/1613704186874.png)

  volatile读操插入的内存屏障.png

# 锁的内存语义

- 当线程释放锁时，JMM会把该线程对应的本地内存中的共享变量刷新到主内存中。 线程A释放一个锁，实质上是线程A向接下来将要获取这个锁的某个线程发出了（线程A 对共享变量所做修改的）消息。
- 当线程获取锁时，JMM会把该线程对应的本地内存置为无效。从而使得被监视器保护的临界区代码必须从主内存中读取共享变量。线程B获取一个锁，实质上是线程B接收了之前某个线程发出的（在释放这个锁之前对共 享变量所做修改的）消息。

> 线程A释放锁，随后线程B获取这个锁，这个过程实质上是线程A通过主内存向线程B发送消息。

# final的内存语义

1. 在构造函数内对一个final域的写入，与随后把这个被构造对象的引用赋值给一个引用变量，这两个操作之间不能重排序。也就是说只有将对象实例化完成后，才能将对象引用赋值给变量。
2. 初次读一个包含final域的对象的引用，与随后初次读这个final域，这两个操作之间不能重排序。也就是下面示例的4和5不能重排序。
3. 当final域为引用类型时，在构造函数内对一个final引用的对象的成员域的写入，与随后在构造函数外把这个被构造对象的引用赋值给一个引用变量，这两个操作之间不能重排序。

下面通过代码在说明一下：



```java
public class FinalExample {
    int i;   // 普通变量
    final int j;   // final变量
    static FinalExample obj;

    public FinalExample() { // 构造函数
        i = 1;// 写普通域
        j = 2;// 写final域
    }

    public static void writer() {   // 写线程A执行
        // 这一步实际上有三个指令，如下：
        // memory = allocate();　　// 1：分配对象的内存空间
        // ctorInstance(memory);　// 2：初始化对象
        // instance = memory;　　// 3：设置instance指向刚分配的内存地址
        obj = new FinalExample();
    }

    public static void reader() {   // 读线程B执行            
        FinalExample object = obj;  // 4. 读对象引用
        int a = object.i; // 5. 读普通域
        int b = object.j; // 读final域
    }
}
```

1. 如果没有final语义的保证，在`writer()`方法中，那三个指令可能发生重排序，导致步骤3先于2执行，然后线程B在执行`reader()`方法时拿到一个没有初始化的对象。
2. 在读一个对象的final域之前，一定会先读包含这个final 域的对象的引用。在这个示例程序中，如果该引用不为null，那么引用对象的final域一定已经 被A线程初始化过了。

## final语义在处理器中的实现

- 会要求编译器在final域的写之后，构造函数return之前插入一个StoreStore障屏。
- 读final域的重排序规则要求编译器在读final域的操作前面插入一个LoadLoad屏障。

# 源码

[https://github.com/wyh-spring-ecosystem-student/spring-boot-student/tree/releases](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2Fwyh-spring-ecosystem-student%2Fspring-boot-student%2Ftree%2Freleases)

spring-boot-student-concurrent 工程

# 参考

《java并发编程的艺术》

# layering-cache

[为监控而生的多级缓存框架 layering-cache](https://links.jianshu.com/go?to=https%3A%2F%2Fgithub.com%2Fxiaolyuh%2Flayering-cache)这是我开源的一个多级缓存框架的实现，如果有兴趣可以看一下