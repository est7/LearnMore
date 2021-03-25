读源码-LeakCanary2.4解析
1-基本原理
1.1-Reference & ReferenceQueue
1.2-对象回收监听
2-LeakCanary源码分析
2.1-初始化
2.2-Activity监听
2.3-hprof文件解析
总结

本文基于LeakCanary版本：
'com.squareup.leakcanary:leakcanary-android:2.4'

1-基本原理
  在开始LeakCanary源码分析前，先来了解下Refercence及ReferenceQueue，它们是LeakCanary实现内存泄漏监听的核心。

1.1-Reference & ReferenceQueue
  (1) Reference即引用，是一个泛型抽象类。Android中的SoftReference(软引用)、WeakReference(弱引用)、PhantomReference(虚引用)都是继承自Reference。来看下Reference的几个主要成员变量。

```
public abstract class Reference<T> {

    // 引用对象，被回收时置null
    volatile T referent;
    //保存即将被回收的reference对象
    final ReferenceQueue<? super T> queue;
    
    //在Enqueued状态下即引用加入队列时，指向下一个待处理Reference对象,默认为null
    Reference queueNext;
    //在Pending状态下，待入列引用，默认为null
    Reference<?> pendingNext;

}
```

  Reference有四种状态：Active、Pending、Enqueued、Inactive。声明的时候默认Active状态，四种状态的切换关系：

queue不为空时

–>GC回收referent时，将referent置为null，并将该Reference对象放入clear队列，状态变为Pending，此时queueNext为空，pendingNext不为空。

–>GC会唤醒ReferenceQueueDaemon线程处理clear队列，将Reference对象放入queue队列，状态变为Enqueued，此时queueNext不为空，pendingNext为该Reference)。

–>当queue调用poll()将该Reference对象出列后，状态变为Inactive，此时queueNext为一个新建虚引用(虚引用get返回null)，pendingNext为该Reference
queue为空

–>GC回收referent时，将referent置为null，状态变为Inactive，此时queueNext、pendingNext都为null
  (2) ReferenceQueue则是一个单向链表实现的队列数据结构，存储的是Reference对象。包含了入列enqueue、出列poll和移除remove操作

1.2-对象回收监听
  Reference配合ReferenceQueue就可以实现对象回收监听了，先通过一个示例来看看是怎么实现的。

```
//创建一个引用队列
ReferenceQueue queue = new ReferenceQueue();
//创建弱引用，并关联引用队列queue
WeakReference reference = new WeakReference(new Object(),queue);
System.out.println(reference);
System.gc();
//当reference被成功回收后，可以从queue中获取到该引用
System.out.println(queue.remove());
```


  示例中的对象当然是可以正常回收的，所以回收后可以在关联的引用队列queue中获取到该引用。反之，若某个应该被回收的对象，GC结束后在queue中未找到该引用，则表明该引用存在内存泄漏风险，这也就是LeakCanary的基本原理了。

  示例中的对象当然是可以正常回收的，所以回收后可以在关联的引用队列queue中获取到该引用。反之，若某个应该被回收的对象，GC结束后在queue中未找到该引用，则表明该引用存在内存泄漏风险，这也就是LeakCanary的基本原理了。

2-LeakCanary源码分析
2.1-初始化
  2.0之前的版本接入过程除了在build.gradle中引入项目外，还需要调用LeakCanary.install(this);来进行初始化工作。在2.0之后的版本只需要在build.gradle引入项目就完事了。那么问题来了：2.0之后的版本初始化工作是在哪里完成的呢？

  找了许久，终于在项目工程：leakcanary-object-watcher-android的manifest文件中发现了秘密：

```
<application>
    <provider
        android:name="leakcanary.internal.AppWatcherInstaller$MainProcess"
        android:authorities="${applicationId}.leakcanary-installer"
        android:enabled="@bool/leak_canary_watcher_auto_install"
        android:exported="false"/>
</application>
```


  这里注册了一个继承自ContentProvider的AppWatcherInstaller。我们知道在app启动时，会先调用注册的ContentProvider的onCreate完成初始化，在AppWatcherInstaller.onCreate中果然找到了熟悉的install方法：

  这里注册了一个继承自ContentProvider的AppWatcherInstaller。我们知道在app启动时，会先调用注册的ContentProvider的onCreate完成初始化，在AppWatcherInstaller.onCreate中果然找到了熟悉的install方法：

```
override fun onCreate(): Boolean {
    val application = context!!.applicationContext as Application
    AppWatcher.manualInstall(application)
    return true
}

```


  调用链：AppWatcher.manualInstall–>InternalAppWatcher.install。具体的初始化逻辑是在InternalAppWatcher，来看源码：

```
fun install(application: Application) {
    //确保在主线程，否则抛出UnsupportedOperationException异常
    checkMainThread()
    //确保application已赋值，application是lateinit修饰的延迟初始化变量
    if (this::application.isInitialized) {
      return
    }
    //leakcanary日志初始化
    SharkLog.logger = DefaultCanaryLog()
    InternalAppWatcher.application = application
    //日志配置初始化
    val configProvider = { AppWatcher.config }
    //Activity内存泄漏监听器初始化
    ActivityDestroyWatcher.install(application, objectWatcher, configProvider)
    //Fragment内存泄漏监听器初始化
    FragmentDestroyWatcher.install(application, objectWatcher, configProvider)
    //注册内存泄漏事件回调
    onAppWatcherInstalled(application)
}
```


  ps:ContentProvider的核心方法CURD在AppWatcherInstaller都是空实现，只用到了onCreate。原来ContentProvider还可以这么玩，新姿势get。需要注意的是ContentProvider.onCreate调用时机介于Application的attachBaseContext和onCreate之间，所以不能依赖之后初始化的其他SDK。

  ps:ContentProvider的核心方法CURD在AppWatcherInstaller都是空实现，只用到了onCreate。原来ContentProvider还可以这么玩，新姿势get。需要注意的是ContentProvider.onCreate调用时机介于Application的attachBaseContext和onCreate之间，所以不能依赖之后初始化的其他SDK。

2.2-Activity监听
  在前面初始过程中，分别创建了针对Activity及Fragment的监听器。我们这里以Activity监听为例进行分析，Fragment监听除了生命周期监听方式不同外后面的流程都是一样的。

```
companion object {
    fun install(
      application: Application,
      objectWatcher: ObjectWatcher,
      configProvider: () -> Config
    ) {
        //实例化ActivityDestroyWatcher
      val activityDestroyWatcher =
        ActivityDestroyWatcher(objectWatcher, configProvider)
        //注册ActivityLifecycle监听
      application.registerActivityLifecycleCallbacks(activityDestroyWatcher.lifecycleCallbacks)
    }
}
```

  registerActivityLifecycleCallbacks是Android Application的一个方法，注册了该方法，可以通过回调获取app中每一个Activity的生命周期变化。再来看看ActivityDestroyWatcher对生命周期回调的处理：

```
private val lifecycleCallbacks =
    object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
      override fun onActivityDestroyed(activity: Activity) {
        if (configProvider().watchActivities) {
          objectWatcher.watch(
              activity, "${activity::class.java.name} received Activity#onDestroy() callback"
          )
        }
      }
}
```

  ps:ActivityLifecycleCallbacks生命周期回调有那么多，为什么只用重写其中一个？关键在于by noOpDelegate(),通过类委托机制将其他回调实现都交给noOpDelegate，而noOpDelegate是一个空实现的动态代理。新姿势get+1，在遇到只需要实现接口的部分方法时，就可以这么玩了，其他方法实现都委托给空实现代理类就好了。

  接着看监听到Activity onDestroy后的处理:

```
private val lifecycleCallbacks =
    object : Application.ActivityLifecycleCallbacks by noOpDelegate() {
      override fun onActivityDestroyed(activity: Activity) {
        if (configProvider().watchActivities) {
          objectWatcher.watch(
              activity, "${activity::class.java.name} received Activity#onDestroy() callback"
          )
        }
      }
}
```

  通过ObjectWatcher来监听该Activity，即认为该Activity实例应该被销毁。如果不能正常销毁则表明存在内存泄漏。

```
@Synchronized fun watch(
    watchedObject: Any,
    description: String
  ) {
    if (!isEnabled()) {
      return
    }
    //@1.清空queue，即移除之前已回收的引用
    removeWeaklyReachableObjects()
    //生成UUID
    val key = UUID.randomUUID()
        .toString()
    //记录当前时间
    val watchUptimeMillis = clock.uptimeMillis()
    //将当前Activity对象封装成KeyedWeakReference，并关联引用队列queue
    //KeyedWeakReference继承自WeakReference，封装了用于监听对象的辅助信息
    val reference =
      KeyedWeakReference(watchedObject, key, description, watchUptimeMillis, queue)
    //输出日志
    SharkLog.d {
      "Watching " +
          (if (watchedObject is Class<*>) watchedObject.toString() else "instance of ${watchedObject.javaClass.name}") +
          (if (description.isNotEmpty()) " ($description)" else "") +
          " with key $key"
    }
    //将弱引用reference存入监听列表watchedObjects
    watchedObjects[key] = reference
    //@2.进行一次后台检查任务，判断引用对象是否未被回收
    checkRetainedExecutor.execute {
      moveToRetained(key)
    }
}
```

@1.清空queue，即移除之前已回收的引用。

  这个方法很重要，第一次调用是清除之前的已回收对象，后面还会再次调用该方法判断引用是否正常回收。

  这里涉及到的两个重要变量：

queue 即引用队列ReferenceQueue
watchedObjects 所有监听Reference对象的map,key为引用对象对应的UUID，value为Reference对象

```
private fun removeWeaklyReachableObjects() {
    var ref: KeyedWeakReference?
    do {
      //遍历引用队列
      ref = queue.poll() as KeyedWeakReference?
      //将引用队列中的Reference对象从监听列表watchedObjects中移除
      if (ref != null) {
        watchedObjects.remove(ref.key)
      }
    } while (ref != null)
}

@2.进行一次后台检查任务moveToRetained，5秒后判断引用对象是否未被回收。

该任务是延迟5s后执行的

private val checkRetainedExecutor = Executor {
    //val watchDurationMillis: Long = TimeUnit.SECONDS.toMillis(5),
    mainHandler.postDelayed(it, AppWatcher.config.watchDurationMillis)
}

@Synchronized private fun moveToRetained(key: String) {
    //遍历引用队列，并将引用队列中的引用从监听列表watchedObjects中移除
    removeWeaklyReachableObjects()
    //若对象未能成功移除，则表明引用对象可能存在内存泄漏
    val retainedRef = watchedObjects[key]
    if (retainedRef != null) {
      retainedRef.retainedUptimeMillis = clock.uptimeMillis()
      //@3.onObjectRetainedListeners内存泄漏事件回调
      onObjectRetainedListeners.forEach { it.onObjectRetained() }
    }
}
```

  在这里理一下moveToRetained的处理逻辑：

正常情况：Activity对象被GC回收掉进入引用队列queue，通过removeWeaklyReachableObjects方法遍历queue获取该引用对象后，将其从监听列表watchedObjects中移除。所以watchedObjects[key]也就无法获取到引用对象了。
异常情况：Activity对象onDestroy后未能被GC回收掉，所以在引用队列queue中也就找不到该对象，也就是说监听列表watchedObjects中该对象没有被删掉。通过watchedObjects[key]可以拿到该引用对象，即可以判断该引用对象存在内存泄漏问题。
@3.onObjectRetainedListeners内存泄漏事件回调

  发现内存泄漏对象后会调用onObjectRetainedListeners监听回调，进行后续处理。那么这个onObjectRetainedListeners是在哪里实现的呢？

  在前面InternalAppWatcher.install初始化时，InternalAppWatcher的初始化方法onAppWatcherInstalled()中初始化了该监听。

init {
    val internalLeakCanary = try {
      val leakCanaryListener = Class.forName("leakcanary.internal.InternalLeakCanary")
      leakCanaryListener.getDeclaredField("INSTANCE")
          .get(null)
    } catch (ignored: Throwable) {
      NoLeakCanary
    }
    @kotlin.Suppress("UNCHECKED_CAST")
    onAppWatcherInstalled = internalLeakCanary as (Application) -> Unit
}

  我们发现这里通过反射获取InternalLeakCanary.INSTANCE单列对象，这个类位于另一个包leakcanary-android-core，所以用了反射。由于InternalLeakCanary是一个函数对象，onAppWatcherInstalled()对应的调用方法为invoke()来完成监听注册。

```
override fun invoke(application: Application) {
    _application = application
    //检查是否debug构建模式
    checkRunningInDebuggableBuild()
    //注册监听
    AppWatcher.objectWatcher.addOnObjectRetainedListener(this)
    //创建AndroidHeapDumper对象，用于虚拟机dump hprof产生内存快照文件
    val heapDumper = AndroidHeapDumper(application, createLeakDirectoryProvider(application))
    //GcTrigger通过Runtime.getRuntime().gc()触发GC
    val gcTrigger = GcTrigger.Default
    val configProvider = { LeakCanary.config }
    //创建子线程及对应looper
    val handlerThread = HandlerThread(LEAK_CANARY_THREAD_NAME)
    handlerThread.start()
    val backgroundHandler = Handler(handlerThread.looper)
    //HeapDumpTrigger监听注册
    heapDumpTrigger = HeapDumpTrigger(
        application, backgroundHandler, AppWatcher.objectWatcher, gcTrigger, heapDumper,
        configProvider
    )
    //注册应用可见监听
    application.registerVisibilityListener { applicationVisible ->
      this.applicationVisible = applicationVisible
      heapDumpTrigger.onApplicationVisibilityChanged(applicationVisible)
    }
    registerResumedActivityListener(application)
    addDynamicShortcut(application)

    disableDumpHeapInTests()

}
```

  当ObjectWatcher中moveToRetained发现未回收对象后，通过回调onObjectRetained()处理时，调用的就是这里注册的HeapDumpTrigger.onObjectRetained()。处理调用链较长,直接看关键方法：

–>onObjectRetained–>scheduleRetainedObjectCheck–>checkRetainedObjects

```
private fun checkRetainedObjects(reason: String) {
    ...//代码省略
    //监听器中未回收对象个数
    var retainedReferenceCount = objectWatcher.retainedObjectCount
    //执行一次GC，再更新未回收对象个数
    if (retainedReferenceCount > 0) {
      gcTrigger.runGc()
      retainedReferenceCount = objectWatcher.retainedObjectCount
    }
    //若对象个数未达到阈值5，返回
    if (checkRetainedCount(retainedReferenceCount, config.retainedVisibleThreshold)) return

    ...//代码省略，60s内只会执行一次
    
    //核心方法,获取内存快照
    dumpHeap(retainedReferenceCount, retry = true)

}

private fun dumpHeap(
    retainedReferenceCount: Int,
    retry: Boolean
  ) {
    ...//代码省略
    

    //获取当前内存快照hprof文件
    val heapDumpFile = heapDumper.dumpHeap()
    ...//省略hprof获取失败处理
    lastDisplayedRetainedObjectCount = 0
    lastHeapDumpUptimeMillis = SystemClock.uptimeMillis()
    //清理之前注册的监听
    objectWatcher.clearObjectsWatchedBefore(heapDumpUptimeMillis)
    //开启hprof分析Service，解析hprof文件生成报告
    HeapAnalyzerService.runAnalysis(application, heapDumpFile)

}
```

2.3-hprof文件解析
  在上面讲到的内存泄漏回调处理中，生成了hprof文件，并开启一个服务来解析该文件。调用链：HeapAnalyzerService.analyzeHeap–>HeapAnalyzer.analyze。该方法实现了解析hprof文件找到内存泄漏对象，并计算对象到GC roots的最短路径，输出报告。

fun analyze(.../*参数省略*/): HeapAnalysis {
    ...//代码省略

```


    return try {
      //PARSING_HEAP_DUMP解析状态回调
      listener.onAnalysisProgress(PARSING_HEAP_DUMP)
      //开始解析hprof文件
      Hprof.open(heapDumpFile)
          .use { hprof ->
            //从文件中解析获取对象关系图结构graph
            //并获取图中的所有GC roots根节点
            val graph = HprofHeapGraph.indexHprof(hprof, proguardMapping)
            //创建FindLeakInput对象
            //@4.查找内存泄漏对象
            val helpers =
              FindLeakInput(graph, referenceMatchers, computeRetainedHeapSize, objectInspectors)
            helpers.analyzeGraph(
                metadataExtractor, leakingObjectFinder, heapDumpFile, analysisStartNanoTime
            )
          }
    } catch (exception: Throwable) {
      ...//省略解析异常处理
    }

}
```

@4.查找内存泄漏对象

private fun FindLeakInput.analyzeGraph(.../*参数省略*/): HeapAnalysisSuccess {
    ...//代码省略
    //通过过滤graph中的KeyedWeakReference类型对象来
    //找到对应的内存泄漏对象
    val leakingObjectIds = leakingObjectFinder.findLeakingObjectIds(graph)
    //@5.计算内存泄漏对象到GC roots的路径
    val (applicationLeaks, libraryLeaks) = findLeaks(leakingObjectIds)
    //输出最终hprof分析结果
    return HeapAnalysisSuccess(.../*参数省略*/)
}

@5.计算内存泄漏对象到GC roots的路径

```
private fun FindLeakInput.findLeaks(leakingObjectIds: Set<Long>): Pair<List<ApplicationLeak>, List<LibraryLeak>> {
    val pathFinder = PathFinder(graph, listener, referenceMatchers)
    //计算并获取目标对象到GC roots的最短路径
    val pathFindingResults =
      pathFinder.findPathsFromGcRoots(leakingObjectIds, computeRetainedHeapSize)

    SharkLog.d { "Found ${leakingObjectIds.size} retained objects" }
    //将这些内存泄漏对象的最短路径合并成树结构返回。
    return buildLeakTraces(pathFindingResults)

}
```


  最终在可视化界面中将hprof分析结果HeapAnalysisSuccess展示出来：

  最终在可视化界面中将hprof分析结果HeapAnalysisSuccess展示出来：



总结
  最后来总结下LeakCanary内存泄漏分析过程吧(Activity)：

(1)注册监听Activity生命周期onDestroy事件
(2)在Activity onDestroy事件回调中创建KeyedWeakReference对象，并关联ReferenceQueue
(3)延时5秒检查目标对象是否回收
(4)未回收则开启服务，dump heap获取内存快照hprof文件
(5)解析hprof文件根据KeyedWeakReference类型过滤找到内存泄漏对象
(6)计算对象到GC roots的最短路径，并合并所有最短路径为一棵树
(7)输出分析结果，并根据分析结果展示到可视化页面
  除了这些外，LeakCanary中代码风格同样值得学习，包括巧用ContentProvider初始化，kolint类委托进行选择性方法实现等。
————————————————
版权声明：本文为CSDN博主「Super鸣」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
原文链接：https://blog.csdn.net/wanmeilang123/article/details/107175113/