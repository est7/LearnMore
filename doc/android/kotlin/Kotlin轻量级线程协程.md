------

在常用的并发模型中，多进程、多线程、分布式是最普遍的，不过近些年来逐渐有一些语言以first-class或者library的形式提供对基于协程的并发模型的支持。其中比较典型的有Scheme、Lua、Python、Perl、Go等以first-class的方式提供对协程的支持。

同样地，Kotlin也支持协程。

本章我们主要介绍：

- 什么是协程
- 协程的用法实例
- 挂起函数
- 通道与管道
- 协程的实现原理
- coroutine库等

## 9.1 协程简介

从硬件发展来看，从最初的单核单CPU，到单核多CPU，多核多CPU，似乎已经到了极限了，但是单核CPU性能却还在不断提升。如果将程序分为IO密集型应用和CPU密集型应用，二者的发展历程大致如下：

> IO密集型应用: 多进程->多线程->事件驱动->协程

> CPU密集型应用:多进程-->多线程

如果说多进程对于多CPU，多线程对应多核CPU，那么事件驱动和协程则是在充分挖掘不断提高性能的单核CPU的潜力。

常见的有性能瓶颈的API (例如网络 IO、文件 IO、CPU 或 GPU 密集型任务等)，要求调用者阻塞（blocking）直到它们完成才能进行下一步。后来，我们又使用异步回调的方式来实现非阻塞，但是异步回调代码写起来并不简单。

协程提供了一种避免阻塞线程并用更简单、更可控的操作替代线程阻塞的方法：协程挂起。

协程主要是让原来要使用“异步+回调方式”写出来的复杂代码, 简化成可以用看似同步的方式写出来（对线程的操作进一步抽象）。这样我们就可以按串行的思维模型去组织原本分散在不同上下文中的代码逻辑，而不需要去处理复杂的状态同步问题。

协程最早的描述是由Melvin Conway于1958年给出：“subroutines who act as the master program”(与主程序行为类似的子例程)。此后他又在博士论文中给出了如下定义：

> - 数据在后续调用中始终保持（ The values of data local to a coroutine persist between successive calls 协程的局部）

> - 当控制流程离开时，协程的执行被挂起，此后控制流程再次进入这个协程时，这个协程只应从上次离开挂起的地方继续 （The execution of a coroutine is suspended as control leaves it, only to carry on where it left off when control re-enters the coroutine at some later stage）。

协程的实现要维护一组局部状态，在重新进入协程前，保证这些状态不被改变，从而能顺利定位到之前的位置。

协程可以用来解决很多问题，比如nodejs的嵌套回调，Erlang以及Golang的并发模型实现等。

实质上，协程（coroutine）是一种用户态的轻量级线程。它由协程构建器（launch coroutine builder）启动。

下面我们通过代码实践来学习协程的相关内容。

### 9.1.1 搭建协程代码工程

首先，我们来新建一个Kotlin Gradle工程。生成标准gradle工程后，在配置文件build.gradle中，配置kotlinx-coroutines-core依赖：

添加 dependencies :



```groovy
compile 'org.jetbrains.kotlinx:kotlinx-coroutines-core:0.16'
```

kotlinx-coroutines还提供了下面的模块：



```groovy
compile group: 'org.jetbrains.kotlinx', name: 'kotlinx-coroutines-jdk8', version: '0.16'
compile group: 'org.jetbrains.kotlinx', name: 'kotlinx-coroutines-nio', version: '0.16'
compile group: 'org.jetbrains.kotlinx', name: 'kotlinx-coroutines-reactive', version: '0.16'
```

我们使用Kotlin最新的1.1.3-2 版本:



```bash
buildscript {
    ext.kotlin_version = '1.1.3-2'
    ...
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

其中，kotlin-gradle-plugin是Kotlin集成Gradle的插件。

另外，配置一下JCenter 的仓库:



```undefined
repositories {
    jcenter()
}
```

### 9.1.2 简单协程示例

下面我们先来看一个简单的协程示例。

运行下面的代码：



```kotlin
    fun firstCoroutineDemo0() {
        launch(CommonPool) {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("Hello,")
        }
        println("World!")
        Thread.sleep(5000L)
    }
```

你将会发现输出：



```swift
World!
Hello,
```

上面的这段代码：



```go
launch(CommonPool) {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("Hello,")
}
```

等价于：



```go
launch(CommonPool, CoroutineStart.DEFAULT, {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("Hello, ")
})
```

### 9.1.3 launch函数

这个launch函数定义在kotlinx.coroutines.experimental下面。



```kotlin
public fun launch(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val newContext = newCoroutineContext(context)
    val coroutine = if (start.isLazy)
        LazyStandaloneCoroutine(newContext, block) else
        StandaloneCoroutine(newContext, active = true)
    coroutine.initParentJob(context[Job])
    start(block, coroutine, coroutine)
    return coroutine
}
```

launch函数有3个入参：context、start、block，这些函数参数分别说明如下：

| 参数    | 说明                                                |
| ------- | --------------------------------------------------- |
| context | 协程上下文                                          |
| start   | 协程启动选项                                        |
| block   | 协程真正要执行的代码块，必须是suspend修饰的挂起函数 |

这个launch函数返回一个Job类型，Job是协程创建的后台任务的概念，它持有该协程的引用。Job接口实际上继承自CoroutineContext类型。一个Job有如下三种状态：

| **State**                                                | isActive | isCompleted |
| -------------------------------------------------------- | -------- | ----------- |
| *New* (optional initial state)  新建 （可选的初始状态）  | `false`  | `false`     |
| *Active* (default initial state)  活动中（默认初始状态） | `true`   | `false`     |
| *Completed* (final state)  已结束（最终状态）            | `false`  | `true`      |

也就是说，launch函数它以非阻塞（non-blocking）当前线程的方式，启动一个新的协程后台任务，并返回一个Job类型的对象作为当前协程的引用。

另外，这里的delay()函数类似Thread.sleep()的功能，但更好的是：它不会阻塞线程，而只是挂起协程本身。当协程在等待时，线程将返回到池中, 当等待完成时, 协同将在池中的空闲线程上恢复。

### 9.1.4  CommonPool：共享线程池

我们再来看一下`launch(CommonPool) {...}`这段代码。

首先，这个CommonPool是代表共享线程池，它的主要作用是来调度计算密集型任务的协程的执行。它的实现使用的是java.util.concurrent包下面的API。它首先尝试创建一个`java.util.concurrent.ForkJoinPool`   （ForkJoinPool是一个可以执行ForkJoinTask的ExcuteService，它采用了work-stealing模式：所有在池中的线程尝试去执行其他线程创建的子任务，这样很少有线程处于空闲状态，更加高效）；如果不可用，就使用`java.util.concurrent.Executors`来创建一个普通的线程池：`Executors.newFixedThreadPool`。相关代码在kotlinx/coroutines/experimental/CommonPool.kt中：



```kotlin
    private fun createPool(): ExecutorService {
        val fjpClass = Try { Class.forName("java.util.concurrent.ForkJoinPool") }
            ?: return createPlainPool()
        if (!usePrivatePool) {
            Try { fjpClass.getMethod("commonPool")?.invoke(null) as? ExecutorService }
                ?.let { return it }
        }
        Try { fjpClass.getConstructor(Int::class.java).newInstance(defaultParallelism()) as? ExecutorService }
            ?. let { return it }
        return createPlainPool()
    }

    private fun createPlainPool(): ExecutorService {
        val threadId = AtomicInteger()
        return Executors.newFixedThreadPool(defaultParallelism()) {
            Thread(it, "CommonPool-worker-${threadId.incrementAndGet()}").apply { isDaemon = true }
        }
    }
```

这个CommonPool对象类是CoroutineContext的子类型。它们的类型集成层次结构如下：

![img](https:////upload-images.jianshu.io/upload_images/1233356-a219643430e8f474.png?imageMogr2/auto-orient/strip|imageView2/2/w/828/format/webp)

螢幕快照 2017-07-12 13.14.54.png

### 9.1.5 挂起函数

代码块中的`delay(3000L, TimeUnit.MILLISECONDS)`函数，是一个用suspend关键字修饰的函数，我们称之为挂起函数。挂起函数只能从协程代码内部调用，普通的非协程的代码不能调用。

挂起函数只允许由协程或者另外一个挂起函数里面调用, 例如我们在协程代码中调用一个挂起函数，代码示例如下：



```kotlin
    suspend fun runCoroutineDemo() {
        run(CommonPool) {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("suspend,")
        }
        println("runCoroutineDemo!")
        Thread.sleep(5000L)
    }

    fun callSuspendFun() {
        launch(CommonPool) {
            runCoroutineDemo()
        }
    }
```

如果我们用Java中的Thread类来写类似功能的代码，上面的代码可以写成这样：



```kotlin
    fun threadDemo0() {
        Thread({
            Thread.sleep(3000L)
            println("Hello,")
        }).start()

        println("World!")
        Thread.sleep(5000L)
    }
```

输出结果也是：

World!
 Hello,

另外， 我们不能使用Thread来启动协程代码。例如下面的写法编译器会报错：



```kotlin
    /**
     * 错误反例：用线程调用协程 error
     */
    fun threadCoroutineDemo() {
        Thread({
            delay(3000L, TimeUnit.MILLISECONDS) // error, Suspend functions are only allowed to be called from a coroutine or another suspend function
            println("Hello,")
        })
        println("World!")
        Thread.sleep(5000L)
    }
```

## 9.2  桥接 阻塞和非阻塞

上面的例子中，我们给出的是使用非阻塞的delay函数，同时有使用了阻塞的Thread.sleep函数，这样代码写在一起可读性不是那么地好。让我们来使用纯的Kotlin的协程代码来实现上面的 *阻塞+非阻塞* 的例子（不用Thread）。

### 9.2.1 runBlocking函数

Kotlin中提供了runBlocking函数来实现类似主协程的功能：



```kotlin
fun main(args: Array<String>) = runBlocking<Unit> {
    // 主协程
    println("${format(Date())}: T0")

    // 启动主协程
    launch(CommonPool) {
        //在common thread pool中创建协程
        println("${format(Date())}: T1")
        delay(3000L)
        println("${format(Date())}: T2 Hello,")
    }
    println("${format(Date())}: T3 World!") //  当子协程被delay，主协程仍然继续运行

    delay(5000L)

    println("${format(Date())}: T4")
}
```

运行结果：



```css
14:37:59.640: T0
14:37:59.721: T1
14:37:59.721: T3 World!
14:38:02.763: T2 Hello,
14:38:04.738: T4
```

可以发现，运行结果跟之前的是一样的，但是我们没有使用Thread.sleep，我们只使用了非阻塞的delay函数。如果main函数不加 `= runBlocking` , 那么我们是不能在main函数体内调用delay(5000L)的。

如果这个阻塞的线程被中断，runBlocking抛出InterruptedException异常。

该runBlocking函数不是用来当做普通协程函数使用的，它的设计主要是用来桥接普通阻塞代码和挂起风格的（suspending style）的非阻塞代码的, 例如用在 `main` 函数中，或者用于测试用例代码中。



```kotlin
@RunWith(JUnit4::class)
class RunBlockingTest {

    @Test fun testRunBlocking() = runBlocking<Unit> {
        // 这样我们就可以在这里调用任何suspend fun了
        launch(CommonPool) {
            delay(3000L)
        }
        delay(5000L)
    }
}
```

## 9.3 等待一个任务执行完毕

我们先来看一段代码：



```kotlin
    fun firstCoroutineDemo() {
        launch(CommonPool) {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("[firstCoroutineDemo] Hello, 1")
        }

        launch(CommonPool, CoroutineStart.DEFAULT, {
            delay(3000L, TimeUnit.MILLISECONDS)
            println("[firstCoroutineDemo] Hello, 2")
        })
        println("[firstCoroutineDemo] World!")
    }
```

运行这段代码，我们会发现只输出：



```csharp
[firstCoroutineDemo] World!
```

这是为什么？

为了弄清上面的代码执行的内部过程，我们打印一些日志看下：



```kotlin
   fun testJoinCoroutine() = runBlocking<Unit> {
        // Start a coroutine
        val c1 = launch(CommonPool) {
            println("C1 Thread: ${Thread.currentThread()}")
            println("C1 Start")
            delay(3000L)
            println("C1 World! 1")
        }

        val c2 = launch(CommonPool) {
            println("C2 Thread: ${Thread.currentThread()}")
            println("C2 Start")
            delay(5000L)
            println("C2 World! 2")
        }

        println("Main Thread: ${Thread.currentThread()}")
        println("Hello,")
        println("Hi,")
        println("c1 is active: ${c1.isActive}  ${c1.isCompleted}")
        println("c2 is active: ${c2.isActive}  ${c2.isCompleted}")

    }
```

再次运行：



```csharp
C1 Thread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
C1 Start
C2 Thread: Thread[ForkJoinPool.commonPool-worker-2,5,main]
C2 Start
Main Thread: Thread[main,5,main]
Hello,
Hi,
c1 is active: true  false
c2 is active: true  false
```

我们可以看到，这里的C1、C2代码也开始执行了，使用的是`ForkJoinPool.commonPool-worker`线程池中的worker线程。但是，我们在代码执行到最后打印出这两个协程的状态isCompleted都是false，这表明我们的C1、C2的代码，在Main Thread结束的时刻（此时的运行main函数的Java进程也退出了），还没有执行完毕，然后就跟着主线程一起退出结束了。

所以我们可以得出结论：运行 main () 函数的主线程， 必须要等到我们的协程完成之前结束 , 否则我们的程序在 打印Hello, 1和Hello, 2之前就直接结束掉了。

我们怎样让这两个协程参与到主线程的时间顺序里呢？我们可以使用`join`, 让主线程一直等到当前协程执行完毕再结束, 例如下面的这段代码



```kotlin
    fun testJoinCoroutine() = runBlocking<Unit> {
        // Start a coroutine
        val c1 = launch(CommonPool) {
            println("C1 Thread: ${Thread.currentThread()}")
            println("C1 Start")
            delay(3000L)
            println("C1 World! 1")
        }

        val c2 = launch(CommonPool) {
            println("C2 Thread: ${Thread.currentThread()}")
            println("C2 Start")
            delay(5000L)
            println("C2 World! 2")
        }

        println("Main Thread: ${Thread.currentThread()}")
        println("Hello,")

        println("c1 is active: ${c1.isActive}  isCompleted: ${c1.isCompleted}")
        println("c2 is active: ${c2.isActive}  isCompleted: ${c2.isCompleted}")

        c1.join() // the main thread will wait until child coroutine completes
        println("Hi,")
        println("c1 is active: ${c1.isActive}  isCompleted: ${c1.isCompleted}")
        println("c2 is active: ${c2.isActive}  isCompleted: ${c2.isCompleted}")
        c2.join() // the main thread will wait until child coroutine completes
        println("c1 is active: ${c1.isActive}  isCompleted: ${c1.isCompleted}")
        println("c2 is active: ${c2.isActive}  isCompleted: ${c2.isCompleted}")
    }
```

将会输出：



```swift
C1 Thread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
C1 Start
C2 Thread: Thread[ForkJoinPool.commonPool-worker-2,5,main]
C2 Start
Main Thread: Thread[main,5,main]
Hello,
c1 is active: true  isCompleted: false
c2 is active: true  isCompleted: false
C1 World! 1
Hi,
c1 is active: false  isCompleted: true
c2 is active: true  isCompleted: false
C2 World! 2
c1 is active: false  isCompleted: true
c2 is active: false  isCompleted: true
```

通常，良好的代码风格我们会把一个单独的逻辑放到一个独立的函数中，我们可以重构上面的代码如下：



```kotlin
    fun testJoinCoroutine2() = runBlocking<Unit> {
        // Start a coroutine
        val c1 = launch(CommonPool) {
            fc1()
        }

        val c2 = launch(CommonPool) {
            fc2()
        }
        ...
    }

    private suspend fun fc2() {
        println("C2 Thread: ${Thread.currentThread()}")
        println("C2 Start")
        delay(5000L)
        println("C2 World! 2")
    }

    private suspend fun fc1() {
        println("C1 Thread: ${Thread.currentThread()}")
        println("C1 Start")
        delay(3000L)
        println("C1 World! 1")
    }
```

可以看出，我们这里的fc1, fc2函数是suspend fun。

## 9.4 协程是轻量级的

直接运行下面的代码：



```kotlin
    fun testThread() {
        val jobs = List(100_1000) {
            Thread({
                Thread.sleep(1000L)
                print(".")
            })
        }
        jobs.forEach { it.start() }
        jobs.forEach { it.join() }
    }
```

我们应该会看到输出报错：



```css
Exception in thread "main" java.lang.OutOfMemoryError: unable to create new native thread
    at java.lang.Thread.start0(Native Method)
    at java.lang.Thread.start(Thread.java:714)
    at com.easy.kotlin.LightWeightCoroutinesDemo.testThread(LightWeightCoroutinesDemo.kt:30)
    at com.easy.kotlin.LightWeightCoroutinesDemoKt.main(LightWeightCoroutinesDemo.kt:40)
...........................................................................................
```

我们这里直接启动了100，000个线程，并join到一起打印".", 不出意外的我们收到了`java.lang.OutOfMemoryError`。

这个异常问题本质原因是我们创建了太多的线程，而能创建的线程数是有限制的，导致了异常的发生。在Java中， 当我们创建一个线程的时候，虚拟机会在JVM内存创建一个Thread对象同时创建一个操作系统线程，而这个系统线程的内存用的不是JVMMemory，而是系统中剩下的内存(MaxProcessMemory - JVMMemory - ReservedOsMemory)。 能创建的线程数的具体计算公式如下：

> Number of Threads  = (MaxProcessMemory - JVMMemory - ReservedOsMemory) / (ThreadStackSize)

其中，参数说明如下：

| 参数             | 说明                     |
| ---------------- | ------------------------ |
| MaxProcessMemory | 指的是一个进程的最大内存 |
| JVMMemory        | JVM内存                  |
| ReservedOsMemory | 保留的操作系统内存       |
| ThreadStackSize  | 线程栈的大小             |

我们通常在优化这种问题的时候，要么是采用减小thread stack的大小的方法，要么是采用减小heap或permgen初始分配的大小方法等方式来临时解决问题。

在协程中，情况完全就不一样了。我们看一下实现上面的逻辑的协程代码：



```kotlin
    fun testLightWeightCoroutine() = runBlocking {
        val jobs = List(100_000) {
            // create a lot of coroutines and list their jobs
            launch(CommonPool) {
                delay(1000L)
                print(".")
            }
        }
        jobs.forEach { it.join() } // wait for all jobs to complete
    }
```

运行上面的代码，我们将看到输出：



```css
START: 21:22:28.913
.....................
.....................(100000个)
.....END: 21:22:30.956
```

上面的程序在2s左右的时间内正确执行完毕。

## 9.5 协程 vs 守护线程

在Java中有两类线程：用户线程 (User Thread)、守护线程 (Daemon Thread)。

所谓守护 线程，是指在程序运行的时候在后台提供一种通用服务的线程，比如垃圾回收线程就是一个很称职的守护者，并且这种线程并不属于程序中不可或缺的部分。因此，当所有的非守护线程结束时，程序也就终止了，同时会杀死进程中的所有守护线程。

我们来看一段Thread的守护线程的代码：



```kotlin
    fun testDaemon2() {
        val t = Thread({
            repeat(100) { i ->
                println("I'm sleeping $i ...")
                Thread.sleep(500L)
            }
        })
        t.isDaemon = true // 必须在启动线程前调用,否则会报错：Exception in thread "main" java.lang.IllegalThreadStateException
        t.start()
        Thread.sleep(2000L) // just quit after delay
    }
```

这段代码启动一个线程，并设置为守护线程。线程内部是间隔500ms 重复打印100次输出。外部主线程睡眠2s。

运行这段代码，将会输出：



```rust
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
I'm sleeping 3 ...
```

协程跟守护线程很像，用协程来写上面的逻辑，代码如下：



```kotlin
    fun testDaemon1() = runBlocking {
        launch(CommonPool) {
            repeat(100) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(2000L) // just quit after delay
    }
```

运行这段代码，我们发现也输出：



```rust
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
I'm sleeping 3 ...
```

我们可以看出，活动的协程不会使进程保持活动状态。它们的行为就像守护程序线程。

## 9.6 协程执行的取消

我们知道，启动函数launch返回一个Job引用当前协程，该Job引用可用于取消正在运行协程:



```kotlin
    fun testCancellation() = runBlocking<Unit> {
        val job = launch(CommonPool) {
            repeat(1000) { i ->
                println("I'm sleeping $i ... CurrentThread: ${Thread.currentThread()}")
                delay(500L)
            }
        }
        delay(1300L)
        println("CurrentThread: ${Thread.currentThread()}")
        println("Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        val b1 = job.cancel() // cancels the job
        println("job cancel: $b1")
        delay(1300L)
        println("Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")

        val b2 = job.cancel() // cancels the job, job already canceld, return false
        println("job cancel: $b2")

        println("main: Now I can quit.")
    }
```

运行上面的代码，将会输出：



```swift
I'm sleeping 0 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 1 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 2 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
CurrentThread: Thread[main,5,main]
Job is alive: true  Job is completed: false
job cancel: true
Job is alive: false  Job is completed: true
job cancel: false
main: Now I can quit.
```

我们可以看出，当job还在运行时，isAlive是true，isCompleted是false。当调用job.cancel取消该协程任务，cancel函数本身返回true,  此时协程的打印动作就停止了。此时，job的状态是isAlive是false，isCompleted是true。 如果，再次调用job.cancel函数，我们将会看到cancel函数返回的是false。

### 9.6.1 计算代码的协程取消失效

kotlinx 协程的所有suspend函数都是可以取消的。我们可以通过job的isActive状态来判断协程的状态，或者检查手否有抛出 CancellationException 时取消。

例如，协程正工作在循环计算中，并且不检查协程当前的状态, 那么调用cancel来取消协程将无法停止协程的运行, 如下面的示例所示:



```kotlin
    fun testCooperativeCancellation1() = runBlocking<Unit> {
        val job = launch(CommonPool) {
            var nextPrintTime = 0L
            var i = 0
            while (i < 20) { // computation loop
                val currentTime = System.currentTimeMillis()
                if (currentTime >= nextPrintTime) {
                    println("I'm sleeping ${i++} ... CurrentThread: ${Thread.currentThread()}")
                    nextPrintTime = currentTime + 500L
                }
            }
        }
        delay(3000L)
        println("CurrentThread: ${Thread.currentThread()}")
        println("Before cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")

        val b1 = job.cancel() // cancels the job
        println("job cancel1: $b1")
        println("After Cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")

        delay(30000L)

        val b2 = job.cancel() // cancels the job, job already canceld, return false
        println("job cancel2: $b2")

        println("main: Now I can quit.")
    }
```

运行上面的代码，输出：



```rust
I'm sleeping 0 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 1 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
...
I'm sleeping 6 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
CurrentThread: Thread[main,5,main]
Before cancel, Job is alive: true  Job is completed: false
job cancel1: true
After Cancel, Job is alive: false  Job is completed: true
I'm sleeping 7 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
...
I'm sleeping 18 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 19 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
job cancel2: false
main: Now I can quit.
```

我们可以看出，即使我们调用了cancel函数，当前的job状态isAlive是false了，但是协程的代码依然一直在运行，并没有停止。

### 9.6.2 计算代码协程的有效取消

有两种方法可以使计算代码取消成功。

#### 方法一： 显式检查取消状态isActive

我们直接给出实现的代码：



```kotlin
fun testCooperativeCancellation2() = runBlocking<Unit> {
        val job = launch(CommonPool) {
            var nextPrintTime = 0L
            var i = 0
            while (i < 20) { // computation loop

                if (!isActive) {
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                if (currentTime >= nextPrintTime) {
                    println("I'm sleeping ${i++} ... CurrentThread: ${Thread.currentThread()}")
                    nextPrintTime = currentTime + 500L
                }
            }
        }
        delay(3000L)
        println("CurrentThread: ${Thread.currentThread()}")
        println("Before cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        val b1 = job.cancel() // cancels the job
        println("job cancel1: $b1")
        println("After Cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")

        delay(3000L)
        val b2 = job.cancel() // cancels the job, job already canceld, return false
        println("job cancel2: $b2")

        println("main: Now I can quit.")
    }
```

运行这段代码，输出：



```rust
I'm sleeping 0 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 1 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 2 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 3 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 4 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 5 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 6 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
CurrentThread: Thread[main,5,main]
Before cancel, Job is alive: true  Job is completed: false
job cancel1: true
After Cancel, Job is alive: false  Job is completed: true
job cancel2: false
main: Now I can quit.
```

正如您所看到的, 现在这个循环可以被取消了。这里的isActive属性是CoroutineScope中的属性。这个接口的定义是：



```kotlin
public interface CoroutineScope {
    public val isActive: Boolean
    public val context: CoroutineContext
}
```

该接口用于通用协程构建器的接收器，以便协程中的代码可以方便的访问其isActive状态值（取消状态），以及其上下文CoroutineContext信息。

#### 方法二： 循环调用一个挂起函数yield()

该方法实质上是通过job的isCompleted状态值来捕获CancellationException完成取消功能。

我们只需要在while循环体中循环调用yield()来检查该job的取消状态，如果已经被取消，那么isCompleted值将会是true，yield函数就直接抛出CancellationException异常，从而完成取消的功能：



```kotlin
        val job = launch(CommonPool) {
            var nextPrintTime = 0L
            var i = 0
            while (i < 20) { // computation loop

                yield()

                val currentTime = System.currentTimeMillis()
                if (currentTime >= nextPrintTime) {
                    println("I'm sleeping ${i++} ... CurrentThread: ${Thread.currentThread()}")
                    nextPrintTime = currentTime + 500L
                }
            }
        }
```

运行上面的代码，输出：



```rust
I'm sleeping 0 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-1,5,main]
I'm sleeping 1 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-2,5,main]
I'm sleeping 2 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-2,5,main]
I'm sleeping 3 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-3,5,main]
I'm sleeping 4 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-3,5,main]
I'm sleeping 5 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-3,5,main]
I'm sleeping 6 ... CurrentThread: Thread[ForkJoinPool.commonPool-worker-2,5,main]
CurrentThread: Thread[main,5,main]
Before cancel, Job is alive: true  Job is completed: false
job cancel1: true
After Cancel, Job is alive: false  Job is completed: true
job cancel2: false
main: Now I can quit.
```

如果我们想看看yield函数抛出的异常，我们可以加上try catch打印出日志：



```dart
try {
    yield()
} catch (e: Exception) {
    println("$i ${e.message}")
}
```

我们可以看到类似：Job was cancelled 这样的信息。

这个yield函数的实现是：



```kotlin
suspend fun yield(): Unit = suspendCoroutineOrReturn sc@ { cont ->
    val context = cont.context
    val job = context[Job]
    if (job != null && job.isCompleted) throw job.getCompletionException()
    if (cont !is DispatchedContinuation<Unit>) return@sc Unit
    if (!cont.dispatcher.isDispatchNeeded(context)) return@sc Unit
    cont.dispatchYield(job, Unit)
    COROUTINE_SUSPENDED
}
```

如果调用此挂起函数时，当前协程的Job已经完成 (isActive = false, isCompleted = true)，当前协程将以CancellationException取消。

### 9.6.3 在finally中的协程代码

当我们取消一个协程任务时，如果有`try {...} finally {...}`代码块，那么finally {...}中的代码会被正常执行完毕：



```kotlin
    fun finallyCancelDemo() = runBlocking {
        val job = launch(CommonPool) {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
            }
        }
        delay(2000L)
        println("Before cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        job.cancel()
        println("After cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        delay(2000L)
        println("main: Now I can quit.")
    }
```

运行这段代码，输出：



```rust
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
I'm sleeping 3 ...
Before cancel, Job is alive: true  Job is completed: false
I'm running finally
After cancel, Job is alive: false  Job is completed: true
main: Now I can quit.
```

我们可以看出，在调用cancel之后，就算当前协程任务Job已经结束了，`finally{...}`中的代码依然被正常执行。

但是，如果我们在`finally{...}`中放入挂起函数：



```kotlin
    fun finallyCancelDemo() = runBlocking {
        val job = launch(CommonPool) {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
                delay(1000L)
                println("And I've delayed for 1 sec ?")
            }
        }
        delay(2000L)
        println("Before cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        job.cancel()
        println("After cancel, Job is alive: ${job.isActive}  Job is completed: ${job.isCompleted}")
        delay(2000L)
        println("main: Now I can quit.")
    }
```

运行上述代码，我们将会发现只输出了一句：I'm running finally。因为主线程在挂起函数`delay(1000L)`以及后面的打印逻辑还没执行完，就已经结束退出。



```go
            } finally {
                println("I'm running finally")
                delay(1000L)
                println("And I've delayed for 1 sec ?")
            }
```

### 9.6.4 协程执行不可取消的代码块

如果我们想要上面的例子中的`finally{...}`完整执行，不被取消函数操作所影响，我们可以使用 run 函数和 NonCancellable 上下文将相应的代码包装在 run (NonCancellable) {...} 中, 如下面的示例所示:



```kotlin
    fun testNonCancellable() = runBlocking {
        val job = launch(CommonPool) {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                run(NonCancellable) {
                    println("I'm running finally")
                    delay(1000L)
                    println("And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }
        delay(2000L)
        println("main: I'm tired of waiting!")
        job.cancel()
        delay(2000L)
        println("main: Now I can quit.")
    }
```

运行输出：



```rust
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
I'm sleeping 3 ...
main: I'm tired of waiting!
I'm running finally
And I've just delayed for 1 sec because I'm non-cancellable
main: Now I can quit.
```

## 9.7 设置协程超时时间

我们通常取消协同执行的原因给协程的执行时间设定一个执行时间上限。我们也可以使用 withTimeout 函数来给一个协程任务的执行设定最大执行时间，超出这个时间，就直接终止掉。代码示例如下:



```kotlin
    fun testTimeouts() = runBlocking {
        withTimeout(3000L) {
            repeat(100) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
    }
```

运行上述代码，我们将会看到如下输出：



```php
I'm sleeping 0 ...
I'm sleeping 1 ...
I'm sleeping 2 ...
I'm sleeping 3 ...
I'm sleeping 4 ...
I'm sleeping 5 ...
Exception in thread "main" kotlinx.coroutines.experimental.TimeoutException: Timed out waiting for 3000 MILLISECONDS
    at kotlinx.coroutines.experimental.TimeoutExceptionCoroutine.run(Scheduled.kt:110)
    at kotlinx.coroutines.experimental.EventLoopImpl$DelayedRunnableTask.invoke(EventLoop.kt:199)
    at kotlinx.coroutines.experimental.EventLoopImpl$DelayedRunnableTask.invoke(EventLoop.kt:195)
    at kotlinx.coroutines.experimental.EventLoopImpl.processNextEvent(EventLoop.kt:111)
    at kotlinx.coroutines.experimental.BlockingCoroutine.joinBlocking(Builders.kt:205)
    at kotlinx.coroutines.experimental.BuildersKt.runBlocking(Builders.kt:150)
    at kotlinx.coroutines.experimental.BuildersKt.runBlocking$default(Builders.kt:142)
    at com.easy.kotlin.CancellingCoroutineDemo.testTimeouts(CancellingCoroutineDemo.kt:169)
    at com.easy.kotlin.CancellingCoroutineDemoKt.main(CancellingCoroutineDemo.kt:193)
```

由 withTimeout 抛出的 TimeoutException 是 CancellationException 的一个子类。这个TimeoutException类型定义如下：



```kotlin
private class TimeoutException(
    time: Long,
    unit: TimeUnit,
    @JvmField val coroutine: Job
) : CancellationException("Timed out waiting for $time $unit")
```

如果您需要在超时时执行一些附加操作, 则可以把逻辑放在 try {...} catch (e: CancellationException) {...} 代码块中。例如：



```swift
    try {
        ccd.testTimeouts()
    } catch (e: CancellationException) {
        println("I am timed out!")
    }
```