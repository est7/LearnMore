# 从 LiveData 迁移到 Kotlin 数据流

**LiveData** 的历史要追溯到 2017 年。彼时，观察者模式有效简化了开发，但诸如 RxJava 一类的库对新手而言有些太过复杂。为此，架构组件团队打造了 **LiveData**: 一个专用于 Android 的具备自主生命周期感知能力的可观察的数据存储器类。LiveData 被有意简化设计，这使得开发者很容易上手；而对于较为复杂的交互数据流场景，建议您使用 RxJava，这样两者结合的优势就发挥出来了。

## **DeadData?**

LiveData **对于 Java 开发者、初学者或是一些简单场景而言仍是可行的解决方案**。而对于一些其他的场景，更好的选择是使用 **Kotlin 数据流 (Kotlin Flow)**。虽说数据流 (相较 LiveData) 有更陡峭的学习曲线，但由于它是 JetBrains 力挺的 Kotlin 语言的一部分，且 Jetpack Compose 正式版即将发布，故两者配合更能发挥出 Kotlin 数据流中响应式模型的潜力。

此前一段时间，我们探讨了 [如何使用 Kotlin 数据流](https://link.juejin.cn/?target=https%3A%2F%2Fzhuanlan.zhihu.com%2Fp%2F139582669) 来连接您的应用当中除了视图和 View Model 以外的其他部分。而现在我们有了 [一种更安全的方式来从 Android 的界面中获得数据流](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fandroiddevelopers%2Fa-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)，已经可以创作一份完整的迁移指南了。

在这篇文章中，您将学到如何把数据流暴露给视图、如何收集数据流，以及如何通过调优来适应不同的需求。

## **数据流: 把简单复杂化，又把复杂变简单**

LiveData 就做了一件事并且做得不错: 它在 [缓存最新的数据](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fandroiddevelopers%2Flivedata-with-coroutines-and-flow-part-i-reactive-uis-b20f676d25d7) 和感知 Android 中的生命周期的同时将数据暴露了出来。稍后我们会了解到 LiveData 还可以 [启动协程](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fandroiddevelopers%2Flivedata-with-coroutines-and-flow-part-ii-launching-coroutines-with-architecture-components-337909f37ae7) 和 [创建复杂的数据转换](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fandroiddevelopers%2Flivedata-beyond-the-viewmodel-reactive-patterns-using-transformations-and-mediatorlivedata-fda520ba00b7)，这可能会需要花点时间。

接下来我们一起比较 LiveData 和 Kotlin 数据流中相对应的写法吧:

**#1: 使用可变数据存储器暴露一次性操作的结果**

这是一个经典的操作模式，其中您会使用协程的结果来改变状态容器:

![△ 将一次性操作的结果暴露给可变的数据容器 (LiveData)](../../../../art/356b22fe7ec64d2995f49466847b1e48tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 将一次性操作的结果暴露给可变的数据容器 (LiveData)

```Kotlin
<!-- Copyright 2020 Google LLC.  
   SPDX-License-Identifier: Apache-2.0 -->

class MyViewModel {
    private val _myUiState = MutableLiveData<Result<UiState>>(Result.Loading)
    val myUiState: LiveData<Result<UiState>> = _myUiState

// 从挂起函数和可变状态中加载数据
    init {
        viewModelScope.launch { 
            val result = ...
            _myUiState.value = result
        }
    }
}
复制代码
```

如果要在 Kotlin 数据流中执行相同的操作，我们需要使用 (可变的) StateFlow (状态容器式可观察数据流):

![△ 使用可变数据存储器 (StateFlow) 暴露一次性操作的结果](../../../../art/f7f4b7596fda4f1aa19cd8c070a0824dtplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 使用可变数据存储器 (StateFlow) 暴露一次性操作的结果

```Kotlin
class MyViewModel {
    private val _myUiState = MutableStateFlow<Result<UiState>>(Result.Loading)
    val myUiState: StateFlow<Result<UiState>> = _myUiState

    // 从挂起函数和可变状态中加载数据
    init {
        viewModelScope.launch { 
            val result = ...
            _myUiState.value = result
        }
    }
}
复制代码
```

[**StateFlow**](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Fkotlin%2Fflow%2Fstateflow-and-sharedflow%23stateflow) 是 [**SharedFlow**](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Fkotlin%2Fflow%2Fstateflow-and-sharedflow%23sharedflow) 的一个比较特殊的变种，而 SharedFlow 又是 Kotlin 数据流当中比较特殊的一种类型。StateFlow 与 LiveData 是最接近的，因为:

- 它始终是有值的。
- 它的值是唯一的。
- 它允许被多个观察者共用 (因此是共享的数据流)。
- 它永远只会把最新的值重现给订阅者，这与活跃观察者的数量是无关的。

> *当暴露 UI 的状态给视图时，应该使用 StateFlow。这是一种安全和高效的观察者，专门用于容纳 UI 状态。*

**#2: 把一次性操作的结果暴露出来**

这个例子与上面代码片段的效果一致，只是这里暴露协程调用的结果而无需使用可变属性。

如果使用 LiveData，我们需要使用 [LiveData](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Ftopic%2Flibraries%2Farchitecture%2Fcoroutines%23livedata) 协程构建器:

![△ 把一次性操作的结果暴露出来 (LiveData)](../../../../art/52ea2a8759e5459ea017488938454ce3tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 把一次性操作的结果暴露出来 (LiveData)

```Kotlin
class MyViewModel(...) : ViewModel() {
    val result: LiveData<Result<UiState>> = liveData {
        emit(Result.Loading)
        emit(repository.fetchItem())
    }
}
复制代码
```

由于状态容器总是有值的，那么我们就可以通过某种 **Result** 类来把 UI 状态封装起来，比如加载中、成功、错误等状态。

与之对应的数据流方式则需要您多做一点*配置*:

![△ 把一次性操作的结果暴露出来 (StateFlow)](../../../../art/5726c7bf6abf4e4abc37dffff059311btplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 把一次性操作的结果暴露出来 (StateFlow)

```Kotlin
class MyViewModel(...) : ViewModel() {
    val result: StateFlow<Result<UiState>> = flow {
        emit(repository.fetchItem())
    }.stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), //由于是一次性操作，也可以使用 Lazily 
        initialValue = Result.Loading
    )
}
复制代码
```

stateIn 是专门将数据流转换为 StateFlow 的运算符。由于需要通过更复杂的示例才能更好地解释它，所以这里暂且把这些参数放在一边。

**#3: 带参数的一次性数据加载**

比方说您想要加载一些依赖用户 ID 的数据，而信息来自一个提供数据流的 AuthManager:

![△ 带参数的一次性数据加载 (LiveData)](../../../../art/ec4edb5220b84d94bc4e8f2ef67a2fb0tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 带参数的一次性数据加载 (LiveData)

使用 LiveData 时，您可以用类似这样的代码:

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: LiveData<String?> = 
        authManager.observeUser().map { user -> user.id }.asLiveData()

    val result: LiveData<Result<Item>> = userId.switchMap { newUserId ->
        liveData { emit(repository.fetchItem(newUserId)) }
    }
}
复制代码
```

`switchMap` 是数据变换中的一种，它订阅了 userId 的变化，并且其代码体会在感知到 userId 变化时执行。

如非必须要将 `userId` 作为 LiveData 使用，那么更好的方案是将流式数据和 Flow 结合，并将最终的结果 (result) 转化为 LiveData。

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: Flow<UserId> = authManager.observeUser().map { user -> user.id }

    val result: LiveData<Result<Item>> = userId.mapLatest { newUserId ->
       repository.fetchItem(newUserId)
    }.asLiveData()
}
复制代码
```

如果改用 Kotlin Flow 来编写，代码其实似曾相识:

![△ 带参数的一次性数据加载 (StateFlow)](../../../../art/49576c7649a94101ba98063198b68d9ftplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 带参数的一次性数据加载 (StateFlow)

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: Flow<UserId> = authManager.observeUser().map { user -> user.id }

    val result: StateFlow<Result<Item>> = userId.mapLatest { newUserId ->
        repository.fetchItem(newUserId)
    }.stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), 
        initialValue = Result.Loading
    )
}
复制代码
```

假如说您想要更高的灵活性，可以考虑显式调用 transformLatest 和 emit 方法:

```Kotlin
val result = userId.transformLatest { newUserId ->
        emit(Result.LoadingData)
        emit(repository.fetchItem(newUserId))
    }.stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), 
        initialValue = Result.LoadingUser //注意此处不同的加载状态
    )
复制代码
```

**#4: 观察带参数的数据流**

接下来我们让刚才的案例变得更具交互性。数据不再被读取，而是**被观察**，因此我们对数据源的改动会直接被传递到 UI 界面中。

继续刚才的例子: 我们不再对源数据调用 fetchItem 方法，而是通过假定的 observeItem 方法获取一个 Kotlin 数据流。

若使用 LiveData，可以将数据流转换为 LiveData 实例，然后通过 emitSource 传递数据的变化。

![△ 观察带参数的数据流 (LiveData)](../../../../art/c7190a38b53f4ee38362ce7437bdb451tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 观察带参数的数据流 (LiveData)

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: LiveData<String?> = 
        authManager.observeUser().map { user -> user.id }.asLiveData()

    val result = userId.switchMap { newUserId ->
        repository.observeItem(newUserId).asLiveData()
    }
}
复制代码
```

或者采用更推荐的方式，把两个流通过 [flatMapLatest](https://link.juejin.cn/?target=https%3A%2F%2Fkotlin.github.io%2Fkotlinx.coroutines%2Fkotlinx-coroutines-core%2Fkotlinx.coroutines.flow%2Fflat-map-latest.html) 结合起来，并且仅将最后的输出转换为 LiveData:

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: Flow<String?> = 
        authManager.observeUser().map { user -> user?.id }

    val result: LiveData<Result<Item>> = userId.flatMapLatest { newUserId ->
        repository.observeItem(newUserId)
    }.asLiveData()
}
复制代码
```

使用 Kotlin 数据流的实现方式非常相似，但是省下了 LiveData 的转换过程:

![△ 观察带参数的数据流 (StateFlow)](../../../../art/969df0ebbaf04cc4b18e8cb1442ba2d1tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 观察带参数的数据流 (StateFlow)

```Kotlin
class MyViewModel(authManager..., repository...) : ViewModel() {
    private val userId: Flow<String?> = 
        authManager.observeUser().map { user -> user?.id }

    val result: StateFlow<Result<Item>> = userId.flatMapLatest { newUserId ->
        repository.observeItem(newUserId)
    }.stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), 
        initialValue = Result.LoadingUser
    )
}
复制代码
```

每当用户实例变化，或者是存储区 (repository) 中用户的数据发生变化时，上面代码中暴露出来的 StateFlow 都会收到相应的更新信息。

**#5: 结合多种源: MediatorLiveData -> Flow.combine**

MediatorLiveData 允许您观察一个或多个数据源的变化情况，并根据得到的新数据进行相应的操作。通常可以按照下面的方式更新 MediatorLiveData 的值:

```Kotlin
val liveData1: LiveData<Int> = ...
val liveData2: LiveData<Int> = ...

val result = MediatorLiveData<Int>()

result.addSource(liveData1) { value ->
    result.setValue(liveData1.value ?: 0 + (liveData2.value ?: 0))
}
result.addSource(liveData2) { value ->
    result.setValue(liveData1.value ?: 0 + (liveData2.value ?: 0))
}
复制代码
```

同样的功能使用 Kotlin 数据流来操作会更加直接:

```Kotlin
val flow1: Flow<Int> = ...
val flow2: Flow<Int> = ...

val result = combine(flow1, flow2) { a, b -> a + b }
复制代码
```

此处也可以使用 [combineTransform](https://link.juejin.cn/?target=https%3A%2F%2Fkotlin.github.io%2Fkotlinx.coroutines%2Fkotlinx-coroutines-core%2Fkotlinx.coroutines.flow%2Fcombine-transform.html) 或者 [zip](https://link.juejin.cn/?target=https%3A%2F%2Fkotlin.github.io%2Fkotlinx.coroutines%2Fkotlinx-coroutines-core%2Fkotlinx.coroutines.flow%2Fzip.html) 函数。

## **通过 stateIn 配置对外暴露的 StateFlow**

早前我们使用 `stateIn` 中间运算符来把普通的流转换成 StateFlow，但转换之后还需要一些配置工作。如果现在不想了解太多细节，只是想知道怎么用，那么可以使用下面的推荐配置:

```Kotlin
val result: StateFlow<Result<UiState>> = someFlow
    .stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), 
        initialValue = Result.Loading
    )
复制代码
```

不过，如果您想知道为什么会使用这个看似随机的 5 秒的 started 参数，请继续往下读。

根据文档，`stateIn` 有三个参数:‍

```
@param scope 共享开始时所在的协程作用域范围

@param started 控制共享的开始和结束的策略

@param initialValue 状态流的初始值

当使用 [SharingStarted.WhileSubscribed] 并带有 `replayExpirationMillis` 参数重置状态流时，也会用到 initialValue。
复制代码
```

`started` 接受以下的三个值:

- `Lazily`: 当首个订阅者出现时开始，在 `scope` 指定的作用域被结束时终止。
- `Eagerly`: 立即开始，而在 `scope` 指定的作用域被结束时终止。
- `WhileSubscribed`: ***这种情况有些复杂 (后文详聊)。***

对于那些只执行一次的操作，您可以使用 Lazily 或者 Eagerly。然而，如果您需要观察其他的流，就应该使用 WhileSubscribed 来实现细微但又重要的优化工作，参见后文的解答。

## **WhileSubscribed 策略**

WhileSubscribed 策略会在没有收集器的情况下取消**上游数据流**。通过 stateIn 运算符创建的 StateFlow 会把数据暴露给视图 (View)，同时也会观察来自其他层级或者是上游应用的数据流。让这些流持续活跃可能会引起不必要的资源浪费，例如一直通过从数据库连接、硬件传感器中读取数据等等。**当您的应用转而在后台运行时，您应当保持克制并中止这些协程**。

`WhileSubscribed` 接受两个参数:

```Kotlin
public fun WhileSubscribed(
   stopTimeoutMillis: Long = 0,
   replayExpirationMillis: Long = Long.MAX_VALUE
)
复制代码
```

**超时停止**

根据其文档:

> **stopTimeoutMillis** 控制一个以毫秒为单位的延迟值，指的是最后一个订阅者结束订阅与停止上游流的时间差。默认值是 0 (立即停止)。

这个值非常有用，因为您可能并不想因为视图有几秒钟不再监听就结束上游流。这种情况非常常见——比如当用户旋转设备时，原来的视图会先被销毁，然后数秒钟内重建。

liveData 协程构建器所使用的方法是 [添加一个 5 秒钟的延迟](https://link.juejin.cn/?target=https%3A%2F%2Fcs.android.com%2Fandroidx%2Fplatform%2Fframeworks%2Fsupport%2F%2B%2Fandroidx-main%3Alifecycle%2Flifecycle-livedata-ktx%2Fsrc%2Fmain%2Fjava%2Fandroidx%2Flifecycle%2FCoroutineLiveData.kt%3Bl%3D356)，即如果等待 5 秒后仍然没有订阅者存在就终止协程。前文代码中的 WhileSubscribed (5000) 正是实现这样的功能:

```Kotlin
class MyViewModel(...) : ViewModel() {
    val result = userId.mapLatest { newUserId ->
        repository.observeItem(newUserId)
    }.stateIn(
        scope = viewModelScope, 
        started = WhileSubscribed(5000), 
        initialValue = Result.Loading
    )
}
复制代码
```

这种方法会在以下场景得到体现:

- 用户将您的应用转至后台运行，5 秒钟后所有来自其他层的数据更新会停止，这样可以节省电量。
- 最新的数据仍然会被缓存，所以当用户切换回应用时，视图立即就可以得到数据进行渲染。
- 订阅将被重启，新数据会填充进来，当数据可用时更新视图。

**数据重现的过期时间**

如果用户离开应用太久，此时您不想让用户看到陈旧的数据，并且希望显示数据正在加载中，那么就应该在 WhileSubscribed 策略中使用 replayExpirationMillis 参数。在这种情况下此参数非常适合，由于缓存的数据都恢复成了 stateIn 中定义的初始值，因此可以有效节省内存。虽然用户切回应用时可能没那么快显示有效数据，但至少不会把过期的信息显示出来。

> `replayExpirationMillis` 配置了以毫秒为单位的延迟时间，定义了从停止共享协程到重置缓存 (恢复到 stateIn 运算符中定义的初始值 initialValue) 所需要等待的时间。它的默认值是长整型的最大值 Long.MAX_VALUE (表示永远不将其重置)。如果设置为 0，可以在符合条件时立即重置缓存的数据。

## **从视图中观察 StateFlow**

我们此前已经谈到，ViewModel 中的 StateFlow 需要知道它们已经不再需要监听。然而，当所有的这些内容都与生命周期 (lifecycle) 结合起来，事情就没那么简单了。

要收集一个数据流，就需要用到协程。Activity 和 Fragment 提供了若干协程构建器:

- **Activity.lifecycleScope.launch** : 立即启动协程，并且在本 Activity 销毁时结束协程。
- **Fragment.lifecycleScope.launch** : 立即启动协程，并且在本 Fragment 销毁时结束协程。
- **Fragment.viewLifecycleOwner.lifecycleScope.launch** : 立即启动协程，并且在本 Fragment 中的视图生命周期结束时取消协程。

## **LaunchWhenStarted 和 LaunchWhenResumed**

对于一个状态 X，有专门的 launch 方法称为 launchWhenX。它会在 lifecycleOwner 进入 X 状态之前一直等待，又在离开 X 状态时挂起协程。对此，需要注意**对应的协程只有在它们的生命周期所有者被销毁时才会被取消**。

![△ 使用 launch/launchWhenX 来收集数据流是不安全的](../../../../art/6d2e0e47e1bd479fa1e3f04aa06c74c3tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 使用 launch/launchWhenX 来收集数据流是不安全的

当应用在后台运行时接收数据更新可能会引起应用崩溃，但这种情况可以通过将视图的数据流收集操作挂起来解决。然而，上游数据流会在应用后台运行期间保持活跃，因此可能浪费一定的资源。

这么说来，目前我们对 StateFlow 所进行的配置都是无用功；不过，现在有了一个新的 API。

## **lifecycle.repeatOnLifecycle 前来救场**

这个新的协程构建器 (自 [lifecycle-runtime-ktx 2.4.0-alpha01](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Fjetpack%2Fandroidx%2Freleases%2Flifecycle%232.4.0-alpha01) 后可用) 恰好能满足我们的需要: 在某个特定的状态满足时启动协程，并且在生命周期所有者退出该状态时停止协程。

![△ 不同数据流收集方法的比较](../../../../art/b5b31c118c7345a0add842aabed3c587tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 不同数据流收集方法的比较

比如在某个 Fragment 的代码中:

```
onCreateView(...) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(STARTED) {
            myViewModel.myUiState.collect { ... }
        }
    }
}
复制代码
```

当这个 Fragment 处于 STARTED 状态时会开始收集流，并且在 RESUMED 状态时保持收集，最终在 Fragment 进入 STOPPED 状态时结束收集过程。如需获取更多信息，请参阅: [使用更为安全的方式收集 Android UI 数据流](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2Fandroiddevelopers%2Fa-safer-way-to-collect-flows-from-android-uis-23080b1f8bda)。

**结合使用 \*repeatOnLifecycle\* API 和上面的 StateFlow 示例可以帮助您的应用妥善利用设备资源的同时，发挥最佳性能。**

![△ 该 StateFlow 通过 WhileSubscribed(5000) 暴露并通过 repeatOnLifecycle(STARTED) 收集](../../../../art/18816ce5cfdc411dbd642b2f66e0a406tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

△ 该 StateFlow 通过 WhileSubscribed(5000) 暴露并通过 repeatOnLifecycle(STARTED) 收集

> **注意**: *[近期在 **Data Binding** 中加入的 StateFlow 支持](https://link.juejin.cn/?target=https%3A%2F%2Fdeveloper.android.google.cn%2Ftopic%2Flibraries%2Fdata-binding%2Fobservability%23stateflow) 使用了 `launchWhenCreated` 来描述收集数据更新，并且它会在进入稳定版后转而使用 **`repeatOnLifecyle`**。*
>
> *对于**数据绑定**，您应该在各处都使用 Kotlin 数据流并简单地加上 `asLiveData()` 来把数据暴露给视图。数据绑定会在 `lifecycle-runtime-ktx 2.4.0` 进入稳定版后更新。*

## **总结**

通过 ViewModel 暴露数据，并在视图中获取的最佳方式是:

- ✔️ 使用带超时参数的 `WhileSubscribed` 策略暴露 `StateFlow`。[[示例 1](https://link.juejin.cn/?target=https%3A%2F%2Fgist.github.com%2FJoseAlcerreca%2F4eb0be817d8f94880dab279d1c27a4af)]
- ✔️ 使用 `repeatOnLifecycle` 来收集数据更新。[[示例 2](https://link.juejin.cn/?target=https%3A%2F%2Fgist.github.com%2FJoseAlcerreca%2F6e2620b5615425a516635744ba59892e)]

如果采用其他方式，上游数据流会被一直保持活跃，导致资源浪费:

- ❌ 通过 `WhileSubscribed` 暴露 StateFlow，然后在 `lifecycleScope.launch/launchWhenX` 中收集数据更新。
- ❌ 通过 `Lazily/Eagerly` 策略暴露 StateFlow，并在 `repeatOnLifecycle` 中收集数据更新。

当然，如果您并不需要使用到 Kotlin 数据流的强大功能，就用 LiveData 好了 :)

*向 [Manuel](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2F%40manuelvicnt)、[Wojtek](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2F%40wkalicinski)、[Yigit](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2F%40yigit%2F)、Alex Cook、[Florina](https://link.juejin.cn/?target=https%3A%2F%2Fmedium.com%2F%40florina.muntenescu) 和 [Chris](https://link.juejin.cn/?target=https%3A%2F%2Fchrisbanes.medium.com%2F) 致谢！*