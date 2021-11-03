### 前言

  Synchronized原理是面试中的一个难点。网上的各种资料太乱了 ，概念晦涩难懂，看了不少资料、博客，花了不少时间，才整理成这篇笔记。看完对你大有帮助。



------



### 1、内存布局

  要想了解Synchronized的原理，你先必须了解下**Java对象内存布局**。

  我这里就先介绍下Java内存布局。

  当你通过关键字new关键字创建一个类的实例对象，对象存于内存的堆中，并给其分配一个内存地址，那么是否想过如下这些问题：

- 这个实例对象是以怎样的形态存在内存中的？
- 一个Object对象在内存中占用多大？
- 对象中的属性是如何在内存中分配的？

> ps：创建一个对象的方式有很多种。你可以想想有哪些哦!

  Java对象在内存中的布局分为三块区域：**对象头**、**实例数据**和**对齐填充**。如下图： ![在这里插入图片描述](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722090.webp)

#### 实例变量

  即**实例数据**。存放类的**属性数据信息**，**包括父类的属性信息**。

- 如果对象有属性字段，则这里会有数据信息。如果对象无属性字段，则这里就不会有数据。
- 根据字段类型的不同占不同的字节。例如boolean类型占1个字节，int类型占4个字节等等。这部分内存按4字节对齐。 这部分的存储顺序会受到虚拟机分配策略参数（FieldsAllocationStyle）和字段在Java源码中定义顺序的影响。 HotSpot虚拟机 默认的分配策略为longs/doubles、ints、shorts/chars、bytes/booleans、oops（Ordinary Object Pointers）。 从分配策略中可以看出，相同宽度的字段总是被分配到一起。 在满足这个前提条件的情况下，在父类中定义的变量会出现在子类之前。如果 CompactFields参数值为true（默认为true），那子类之中较窄的变量也可能会插入到父类变量的空隙之中。

#### 填充数据

  **填充数据不是必须存在的，仅仅是为了字节对齐**。   由于HotSpot VM的自动内存管理系统要求**对象起始地址必须是8字节的整数倍**，换句话说，就是对象的大小必须是8字节的整数倍。而对象头部分正好是8字节的倍数（1倍或者2倍），因此，**当对象实例数据部分没有对齐时**，就需要**通过对齐填充来补全**。

>   **为什么要对齐数据？**

  字段内存对齐的其中一个原因，是让字段只出现在同一CPU的缓存行中。   如果字段不是对齐的，那么就有可能出现跨缓存行的字段。也就是说，该字段的读取可能需要替换两个缓存行，而该字段的存储也会同时污染两个缓存行。这两种情况对程序的执行效率而言都是不利的。其实对其填充的**最终目的是为了计算机高效寻址**。

#### 对象头

  **对象头是实现synchronized的锁对象的基础**，我们重点分析下。

  我们可以在Hotspot **[官方文档](https://link.juejin.cn?target=http%3A%2F%2Fopenjdk.java.net%2Fgroups%2Fhotspot%2Fdocs%2FHotSpotGlossary.html)** 中找到它的描述（如下）：

> **object header** 
> Common structure at the beginning of every GC-managed heap object. (Every oop points to an object header.) Includes fundamental information about the heap object's layout, type, GC state, synchronization state, and identity hash code. Consists of two words. In arrays it is immediately followed by a length field. Note that both Java objects and VM-internal objects have a common object header format.

  从中可以发现，它是Java对象和虚拟机内部对象都有的共同格式，由**两个字**(计算机术语)组成。另外，如果对象是一个Java数组，那在对象头中还必须有一块用于记录**数组长度**的数据，因为虚拟机可以通过普通Java对象的元数据信息确定Java对象的大小，但是从数组的元数据中无法确定数组的大小。

  它里面提到了**对象头由两个字组成**，这两个字是什么呢？我们还是在上面的那个Hotspot官方文档中往上看，可以发现还有另外两个名词的定义解释，分别是 mark word 和 klass pointer：

> **klass pointer** 
> The second word of every object header. Points to another object (a metaobject) which describes the layout and behavior of the original object. For Java objects, the "klass" contains a C++ style "vtable". 
>
> **mark word** 
> The first word of every object header. Usually a set of bitfields including synchronization state and identity hash code. May also be a pointer (with characteristic low bit encoding) to synchronization related information. During GC, may contain GC state bits.

  从中可以发现对象头中那两个字：第一个字就是 mark word，第二个就是 klass pointer。

#### Mark Word

  即**标记字段**。用于**存储对象自身的运行时数据，如哈希码（HashCode）、GC分代年龄、锁状态标志、线程持有的锁、偏向线程ID、偏向时间戳**等等。   Mark Word在32位JVM中的长度是32bit，在64位JVM中长度是64bit。我们打开openjdk的源码包，对应路径/openjdk/hotspot/src/share/vm/oops，Mark Word对应到C++的代码markOop.hpp，可以从注释中看到它们的组成，本文所有代码是基于Jdk1.8。

> 需要源码的同学可以关注公众号“Java尖子生”，回复“openjdk”免费获取。

  由于对象头的信息是与对象自身定义的数据没有关系的额外存储成本，因此**考虑到JVM的空间效率**，**Mark Word 被设计成为一个非固定的数据结构**，以便存储更多有效的数据，它会根据对象本身的状态复用自己的存储空间。

  Mark Word在不同的锁状态下存储的内容不同，在32位JVM中是这么存的: ![在这里插入图片描述](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722836.webp)   在64位JVM中是这么存的: ![在这里插入图片描述](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722061.webp)

  虽然它们在不同位数的JVM中长度不一样，但是基本组成内容是一致的。

- **锁标志位（lock）**：区分锁状态，11时表示对象待GC回收状态, 只有最后2位锁标识(11)有效。
- **biased_lock**：是否偏向锁，由于正常锁和偏向锁的锁标识都是 01，没办法区分，这里引入一位的偏向锁标识位。
- **分代年龄（age）**：表示对象被GC的次数，当该次数到达阈值的时候，对象就会转移到老年代。
- **对象的hashcode（hash）**：运行期间调用System.identityHashCode()来计算，延迟计算，并把结果赋值到这里。当对象加锁后，计算的结果31位不够表示，在偏向锁，轻量锁，重量锁，hashcode会被转移到Monitor中。
- **偏向锁的线程ID（JavaThread）**：偏向模式的时候，当某个线程持有对象的时候，对象这里就会被置为该线程的ID。 在后面的操作中，就无需再进行尝试获取锁的动作。
- **epoch**：偏向锁在CAS锁操作过程中，偏向性标识，表示对象更偏向哪个锁。
- **ptr_to_lock_record**：轻量级锁状态下，指向栈中锁记录的指针。当锁获取是无竞争的时，JVM使用原子操作而不是OS互斥。这种技术称为轻量级锁定。在轻量级锁定的情况下，JVM通过CAS操作在对象的标题字中设置指向锁记录的指针。
- **ptr_to_heavyweight_monitor**：重量级锁状态下，指向对象监视器Monitor的指针。如果两个不同的线程同时在同一个对象上竞争，则必须将轻量级锁定升级到Monitor以管理等待的线程。在重量级锁定的情况下，JVM在对象的ptr_to_heavyweight_monitor设置指向Monitor的指针。

#### Klass Pointer

  即**类型指针**，**是对象指向它的类元数据的指针，虚拟机通过这个指针来确定这个对象是哪个类的实例**。

#### 数组长度（只有数组对象有）

  如果对象是一个数组，那在对象头中还必须有一块数据用于记录数组长度。   因为虚拟机可以通过普通Java对象的元数据信息确定Java对象的大小，但是从数组的元数据中无法确定数组的大小。

  至此，我们已经了解了对象在堆内存中的整体结构布局，如下图所示： ![在这里插入图片描述](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722871.webp) 

------



### 2、Synchronized底层实现

  这里我们主要分析一下synchronized对象锁（也就是重量级锁）。   在32位和64位机器上锁标识位都为10，其中指针指向的是monitor对象（也称为管程或监视器锁）的起始地址。   每个对象都存在着一个 monitor 与之关联，对象与其 monitor 之间的关系有存在多种实现方式，如：monitor可以与对象一起创建销毁或当线程试图获取对象锁时自动生成，但当一个 monitor 被某个线程持有后，它便处于锁定状态。   在Java虚拟机(HotSpot)中，monitor是由ObjectMonitor实现的，其主要数据结构如下（位于HotSpot虚拟机源码ObjectMonitor.hpp文件，C++实现）

```
ObjectMonitor() {
    _header       = NULL;
    _count        = 0; //记录个数
    _waiters      = 0,
            _recursions   = 0;
    _object       = NULL;
    _owner        = NULL;
    _WaitSet      = NULL; //处于wait状态的线程，会被加入到_WaitSet
    _WaitSetLock  = 0 ;
    _Responsible  = NULL ;
    _succ         = NULL ;
    _cxq          = NULL ;
    FreeNext      = NULL ;
    _EntryList    = NULL ; //处于等待锁block状态的线程，会被加入到该列表
    _SpinFreq     = 0 ;
    _SpinClock    = 0 ;
    OwnerIsThread = 0 ;
}
复制代码
```

我们分析下上面源码中几个关键属性：

- **_WaitSet和_EntryList**：用来保存ObjectWaiter对象列表（ObjectWaiter对象：每个等待锁的线程都会被封装成ObjectWaiter对象）。
- **_owner**：指向持有ObjectMonitor对象的线程。

  当多个线程同时访问一段同步代码时，首先会进入 _EntryList 集合，当线程获取到对象的monitor 后进入 _Owner 区域并把monitor中的owner变量设置为当前线程同时monitor中的**计数器count**加1，若线程调用 wait() 方法，将释放当前持有的monitor，owner变量恢复为null，count自减1，同时该线程进入 WaitSet集合中等待被唤醒。若当前线程执行完毕也将释放monitor(锁)并复位变量的值，以便其他线程进入获取monitor(锁)。如下图所示（图片来源：[Thread Synchronization](https://link.juejin.cn?target=https%3A%2F%2Fwww.artima.com%2Finsidejvm%2Fed2%2Fthreadsynch.html)）: ![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722251.webp)

  由此看来，**monitor对象存在于每个Java对象的对象头中(存储的是指针)，synchronized锁便是通过这种方式获取锁的，也是为什么Java中任意对象可以作为锁的原因，同时也是notify/notifyAll/wait等方法存在于顶级对象Object中的原因**。

  下面我们将进一步分析synchronized在字节码层面的具体语义实现。



------



### 3、synchronized修饰代码块底层原理

  现在我们重新定义一个synchronized修饰的同步代码块（i++），在代码块中操作共享变量i，如下：

```
public class TestSafeAddI {
    public int i;

    public void addI() {
        synchronized (this) {
            i++;
        }
    }
}
复制代码
```

  使用反编译工具，查看编译后的字节码（完整）：

> 如何查看字节码文件，有多种工具，我这里提供2种： 
> 方式一：luyten工具 
> 运行工具，然后Settings选择ByteCode，然后导入本地的.class文件即可。 
> 需要改工具的同学，在公众号“Java尖子生”，回复“luyten”即可获取。 
>
> 方式二：使用idea编辑器的同学，可以在idea中选中编译后的.class文件，然后View->Show ByteCode 
> ps:本人使用的是idea2020最新版本。

```
class com.top.test.mutiTheread.TestSafeAddI
        Minor version: 0
        Major version: 52
        Flags: PUBLIC, SUPER

public int i;
        Flags: PUBLIC

public void <init>();
        Flags: PUBLIC
        Code:
        linenumber      3
        0: aload_0         /* this */
        1: invokespecial   java/lang/Object.<init>:()V
        4: return

public void addI();
        Flags: PUBLIC
        Code:
        linenumber      7
        0: aload_0         /* this */
        1: dup
        2: astore_1
        3: monitorenter
        linenumber      8
        4: aload_0         /* this */
        5: dup
        6: getfield        com/top/test/mutiTheread/TestSafeAddI.i:I
        9: iconst_1
        10: iadd
        11: putfield        com/top/test/mutiTheread/TestSafeAddI.i:I
        linenumber      9
        14: aload_1
        15: monitorexit
        16: goto            24
        19: astore_2
        20: aload_1
        21: monitorexit
        22: aload_2
        23: athrow
        linenumber      10
        24: return
        StackMapTable: 00 02 FF 00 13 00 02 07 00 10 07 00 11 00 01 07 00 12 FA 00 04
        Exceptions:
        Try           Handler
        Start  End    Start  End    Type
        -----  -----  -----  -----  ----
        4      16     19     24     Any
        19     22     19     24     Any
复制代码
```

  我们主要关注字节码中的如下代码：

```
3: monitorenter  //进入同步方法
//..........省略其他  
15: monitorexit   //退出同步方法
16: goto          24
//省略其他.......
21: monitorexit //退出同步方法
复制代码
```

  从字节码中可知同步语句块的实现使用的是**monitorenter**和**monitorexi**指令，其中monitorenter指令指向同步代码块的开始位置，monitorexit指令则指明同步代码块的结束位置。

**当执行monitorenter指令时:**

- 当前线程将试图获取 objectref(即对象锁) 所对应的 monitor 的持有权，当 objectref 的 monitor 的进入计数器为 0，那线程可以成功取得 monitor，并将计数器值设置为 1，取锁成功。
- 如果当前线程已经拥有 objectref 的 monitor 的持有权，那它可以重入这个 monitor，重入时计数器的值也会加 1。这正是synchronized的可重入特性。（关于可重入锁可以看这篇:[可重入锁-synchronized是可重入锁吗？](https://link.juejin.cn?target=https%3A%2F%2Fblog.csdn.net%2FKurry4ever_%2Farticle%2Fdetails%2F109560971)）
- 倘若其他线程已经拥有 objectref 的 monitor 的所有权，即目标锁对象的计数器不为0。那当前线程将被阻塞，直到正在执行线程执行完毕.

**当执行 monitorexit 时:**

- Java 虚拟机则需将锁对象的计数器减 1。当计数器减为 0 时，那便代表该锁已经被释放掉了。这样其他线程将有机会持有 monitor 。
- 计数器不为0，表示当前线程还持有该对象锁。

  值得注意的是：**一条指令Monitorenter可以对应到多条monitorexit 指令**。这是因为 **Java 虚拟机需要确保所获得的锁在正常执行路径，以及异常执行路径上都能够被解锁**。  也就是说：编译器将会确保无论方法通过何种方式完成，方法中调用过的每条 monitorenter 指令都有执行其对应 monitorexit 指令，而无论这个方法是正常结束还是异常结束。为了保证在方法异常完成时 monitorenter 和 monitorexit 指令依然可以正确配对执行，编译器会自动产生一个异常处理器，这个异常处理器声明可处理所有的异常，它的目的就是用来执行 monitorexit 指令。从字节码中也可以看出多了一个monitorexit指令，它就是异常结束时被执行的释放monitor 的指令。



------



### 4、synchronized修饰方法底层原理

  synchronized修饰方法与修饰代码块有不同。

  我们把上面的同步方法改下 ，改成synchronized修饰方法：

```
public class TestSafeAddI {
    public int i;

    public synchronized void addI() {
        i++;
    }
}
复制代码
```

  反编译后的字节码如下：

```
class com.top.test.mutiTheread.TestSafeAddI
        Minor version: 0
        Major version: 52
        Flags: PUBLIC, SUPER

public int i;
        Flags: PUBLIC

public void <init>();
        Flags: PUBLIC
        Code:
        linenumber      3
        0: aload_0         /* this */
        1: invokespecial   java/lang/Object.<init>:()V
        4: return

public synchronized void addI();
        Flags: PUBLIC, SYNCHRONIZED
        Code:
        linenumber      7
        0: aload_0         /* this */
        1: dup
        2: getfield        com/top/test/mutiTheread/TestSafeAddI.i:I
        5: iconst_1
        6: iadd
        7: putfield        com/top/test/mutiTheread/TestSafeAddI.i:I
        linenumber      8
        10: return
复制代码
```

  当用**synchronized 标记方法**时，并**没有monitorenter指令和monitorexit指令**，从字节码中，我们可以看到方法的**访问标记**包括**ACC_SYNCHRONIZED**了。该标识指明了该方法是一个**同步方法**，JVM通过该ACC_SYNCHRONIZED访问标志来辨别一个方法是否声明为同步方法，从而执行相应的同步调用。在**进入该方法时**，Java 虚拟机需要**进行 monitorenter操作**。而在**退出该方法时**，不管是正常返回，还是向调用者抛异常，Java 虚拟机均需要**进行monitorexit操作**。

  这里 monitorenter 和 monitorexit 操作所对应的锁对象是**隐式**的。对于实例方法来说，这两个操作对应的锁对象是 this；对于静态方法来说，这两个操作对应的锁对象则是所在类的 Class 实例。

  同时我们还必须注意到的是在Java早期版本中，synchronized属于**重量级锁**，效率低下。因为监视器锁（monitor）是依赖于底层的操作系统的Mutex Lock来实现的，而操作系统实现线程之间的切换时需要**从用户态转换到核心态**。这个状态之间的转换需要相对比较长的时间，时间成本相对较高，这也是为什么**早期的synchronized效率低**的原因。



------



### 5、锁的升级

  锁的升级，我们可以理解为：Java虚拟机对synchronized的优化

  为了尽量避免昂贵的线程阻塞、唤醒操作，Java 虚拟机会在线程进入阻塞状态之前，以及被唤醒后竞争不到锁的情况下，进入**自旋状态**，在处理器上**空跑并且轮询**锁是否被释放。如果此时锁恰好被释放了，那么当前线程便无须进入阻塞状态，而是直接获得这把锁。我们称其为**自旋锁**。   同时在**Java6**之后Java官方对从JVM层面对synchronized较大优化，所以现在的synchronized锁效率也优化得很不错了，Java 6之后，为了减少获得锁和释放锁所带来的性能消耗，引入了**轻量级锁**和**偏向锁**（也叫：偏斜锁，英文单词为，Biased Locking）。

  **锁的升级：\**锁的状态总共有四种（上面的Mark Word图结构也可以看出），\*\*无锁状态\*\*、\*\*偏向锁\*\*、\*\*轻量级锁\*\*和\**重量级锁**。随着锁的竞争，锁可以**从偏向锁升级到轻量级锁，再升级的重量级锁。**。

> ps:有的观点认为 Java 不会进行锁降级。实际上，锁降级确实是会发生的，当 JVM 进入**安全点**（[SafePoint](https://link.juejin.cn?target=http%3A%2F%2Fblog.ragozin.info%2F2012%2F10%2Fsafepoints-in-hotspot-jvm.html)）的时候，会检查是否有闲置的 Monitor，然后试图进行降级。

  关于重量级锁，前面我们已详细分析过。下面我们将介绍偏向锁、轻量级锁、自旋锁以及JVM的其他优化手段。



------



### 6、偏向锁

  **偏向锁**是Java 6之后加入的新锁，它是一种针对加锁操作的优化手段。

  偏向锁是**最乐观的**一种情况：在大多数情况下，锁不仅不存在多线程竞争，而且总是由同一线程多次获得。 因此为了**减少同一线程获取锁的代价**而引入偏向锁。

  偏向锁的**核心思想**是：如果一个线程获得了锁，那么锁就进入偏向模式，此时Mark Word 的结构也变为偏向锁结构，当这个线程再次请求锁时，无需再做任何同步操作，直接可以获取锁。这样就省去了大量有关锁申请的操作，从而也就提供程序的性能。

  **加锁时**，如果该锁对象支持偏向锁，那么 Java 虚拟机会通过**CAS**操作，将**当前线程的地址**（我理解的是线程ID，不过都能确定唯一线程）**记录在锁对象的标记字段之中**，并且将标记字段的最后三位设置为**101**。（便于理解，我把Mark Word的结构图再放在这里）

> CAS 是一个原子操作，它会比较目标地址的值是否和期望值相等，如果相等，则替换为一个新的值。

![在这里插入图片描述](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110261722949.webp)

  在接下来的运行过程中，每当有线程请求这把锁，Java 虚拟机只需判断锁对象标记字段中：最后三位是否为 101，是否包含当前线程的地址，以及**epoch值**是否和锁对象的类的 epoch 值相同。如果都满足，那么当前线程持有该偏向锁，可以直接返回。

**理解epoch值：**

  我们先从**偏向锁的撤销**讲起。当请求加锁的线程和锁对象标记字段的线程地址不匹配时（而且 epoch 值相等，如若不等，那么当前线程可以将该锁重偏向至自己），Java 虚拟机需要撤销该偏向锁。这个撤销过程非常麻烦，它要求持有偏向锁的线程到达安全点，再将偏向锁替换成轻量级锁。

  如果某一类锁对象的**总撤销数超过了一个阈值**（对应 Java 虚拟机参数 -XX:BiasedLockingBulkRebiasThreshold，默认为 20），那么 Java 虚拟机会宣布这个类的**偏向锁失效**。

  具体的做法便是在每个类中维护一个 epoch 值，你可以理解为第几代偏向锁。当设置偏向锁时，Java 虚拟机需要将该 epoch 值复制到锁对象的标记字段中。

  在宣布某个类的偏向锁失效时，Java 虚拟机实则将该类的 epoch 值加 1，表示之前那一代的偏向锁已经失效。而新设置的偏向锁则需要复制新的 epoch 值。

  为了保证当前持有偏向锁并且已加锁的线程不至于因此丢锁，Java 虚拟机需要遍历所有线程的 Java 栈，找出该类已加锁的实例，并且将它们标记字段中的 epoch 值加 1。该操作需要所有线程处于安全点状态。

  如果**总撤销数超过另一个阈值**（对应 Java 虚拟机参数 -XX:BiasedLockingBulkRevokeThreshold，默认值为 40），那么 Java 虚拟机会认为**这个类已经不再适合偏向锁**。此时，Java 虚拟机会**撤销该类实例的偏向锁**，并且在**之后**的加锁过程中直接为该类实例设置**轻量级锁**。



------



### 7、轻量级锁

  倘若偏向锁失败，并不会立即膨胀为重量级锁，而是先升级为轻量级锁

  轻量级锁时Java6引入的。

  轻量级锁是一种比较乐观的情况：多个线程在不同的时间段请求同一把锁，也就是说没有锁竞争。

  标记字段（mark word）的最后两位被用来表示该对象的锁状态。其中，00 代表轻量级锁，01 代表无锁（或偏向锁），10 代表重量级锁。

  当进行**加锁**操作时，Java 虚拟机会判断是否已经是重量级锁。如果不是，它会在**当前线程**的当前**栈桢**中划出一块空间，作为该锁的**锁记录**，并且将**锁对象的标记字段 复制**到该锁记录中（可以理解为保存之前锁对象的标记字段。如果是同一个线程这个值会是0：后面的锁记录清零就是这个意思）。

  然后，Java 虚拟机会尝试用 CAS（compare-and-swap）操作替换锁对象的标记字段。

  假设当前锁对象的标记字段为 X…XYZ，Java 虚拟机会比较该字段是否为 X…X01（锁标志位01表示偏向锁）。如果是，则替换为刚才分配的锁记录的地址。由于内存对齐的缘故，它的最后两位为 00（锁标志位00表示轻量级锁）。此时，该线程已成功获得这把锁，可以继续执行了。

  如果不是 X…X01，那么有两种可能。第一，该线程重复获取同一把锁（此刻持有的是轻量级锁）。此时，Java 虚拟机会将锁记录清零，以代表该锁被重复获取（可重入锁可以阅读下：）。第二，其他线程持有该锁（此刻持有的是轻量级锁）。此时，Java 虚拟机会将这把锁膨胀为重量级锁，并且阻塞当前线程。

  当进行**解锁**操作时，如果当前锁记录（你可以将一个线程的所有锁记录想象成一个栈结构，每次加锁压入一条锁记录，解锁弹出一条锁记录，当前锁记录指的便是栈顶的锁记录）的值为 0，则代表重复进入同一把锁，直接返回即可。

  否则，Java 虚拟机会尝试用 CAS 操作，比较锁对象的标记字段的值是否为当前锁记录的地址。如果是，则替换为锁记录中的值，也就是锁对象原本的标记字段。此时，该线程已经成功释放这把锁。

  如果不是，则意味着这把锁已经被膨胀为**重量级锁**。此时，Java 虚拟机会进入重量级锁的释放过程，唤醒因竞争该锁而被阻塞了的线程。



------



### 8、自旋锁

  轻量级锁失败后，虚拟机为了避免线程真实地在操作系统层面挂起，还会进行一项称为自旋锁的优化手段。

  这是**基于在大多数情况**下，线程持有锁的时间都不会太长，如果直接挂起操作系统层面的线程可能会得不偿失，毕竟操作系统实现线程之间的切换时需要从用户态转换到核心态，这个状态之间的转换需要相对比较长的时间，时间成本相对较高。

  因此自旋锁会假设在不久将来，当前的线程可以获得锁，因此虚拟机会让当前想要获取锁的线程做几个空循环(这也是称为自旋的原因)，一般不会太久，可能是50个循环或100循环，在经过若干次循环后，如果得到锁，就顺利进入临界区。如果还不能获得锁，那就会将线程在操作系统层面挂起。

  这就是自旋锁的优化方式，这种方式确实也是可以提升效率的。最后没办法也就只能升级为重量级锁了。

**举个例子：**   我们可以用等红绿灯作为例子。Java 线程的阻塞相当于熄火停车，而自旋状态相当于怠速停车。如果红灯的等待时间非常长，那么熄火停车相对省油一些；如果红灯的等待时间非常短，比如说我们在 synchronized 代码块里只做了一个整型加法，那么在短时间内锁肯定会被释放出来，因此怠速停车更加合适。   然而，对于 Java 虚拟机来说，它并不能看到红灯的剩余时间，也就没办法根据等待时间的长短来选择自旋还是阻塞。Java 虚拟机给出的方案是自适应自旋，根据以往自旋等待时是否能够获得锁，来动态调整自旋的时间（循环数目）。 就我们的例子来说，如果之前不熄火等到了绿灯，那么这次不熄火的时间就长一点；如果之前不熄火没等到绿灯，那么这次不熄火的时间就短一点。

  自旋状态还带来另外一个副作用，那便是**不公平的锁机制**。处于阻塞状态的线程，并没有办法立刻竞争被释放的锁。然而，处于自旋状态的线程，则很有可能优先获得这把锁。（关于公平锁与非公平锁可以看这篇：[公平锁和非公平锁-ReentrantLock是如何实现公平、非公平的](https://link.juejin.cn?target=https%3A%2F%2Fblog.csdn.net%2FKurry4ever_%2Farticle%2Fdetails%2F109561095)）



------



### 9、锁消除

  消除锁是虚拟机另外一种锁的优化，这种优化更彻底，Java虚拟机在JIT编译时(可以简单理解为当某段代码即将第一次被执行时进行编译，又称即时编译)，通过对运行上下文的扫描，去除不可能存在共享资源竞争的锁，通过这种方式消除没有必要的锁，可以节省毫无意义的请求锁时间。

  如下StringBuffer的append是一个同步方法，但是在add方法中的StringBuffer属于一个局部变量，并且不会被其他线程所使用，因此StringBuffer不可能存在共享资源竞争的情景，JVM会自动将其锁消除。

```
public class StringBufferRemoveSync {
    public void add(String str1, String str2) {
        //StringBuffer是线程安全,由于sb只会在append方法中使用,不可能被其他线程引用
        //因此sb属于不可能共享的资源,JVM会自动消除内部的锁
        StringBuffer sb = new StringBuffer();
        sb.append(str1).append(str2);
    }

    public static void main(String[] args) {
        StringBufferRemoveSync rmsync = new StringBufferRemoveSync();
        for (int i = 0; i < 10000000; i++) {
            rmsync.add("abc", "123");
        }
    }
}
复制代码
```



------



### 总结

  我整理的还不够完善，比如：内存布局的压缩指针和字段重排列我都没有提及。

  不足之处，有疑问的同学可以留言讨论哦。

------

沪漂程序员一枚；坚持写博客，你的支持就是我创作的动力！



作者：Java尖子生
链接：https://juejin.cn/post/6894099621694406669
来源：稀土掘金
著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。