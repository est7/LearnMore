# 一、WorkManager

WorkManager 是持久工作的推荐解决方案。当通过应用程序重启和系统重启保持计划时，工作是持久的。由于大多数后台处理最好通过持久性工作来完成，WorkManager 是后台处理的主要推荐API。

使用 WorkManager 注册的周期性任务不一定会准时执行，这不是Bug，而是系统为了减少电量消耗可能会将触发事件接近的几个任务放在一起执行，这样可以大幅度减少CPU唤起次数，从而有效延长电池时间。

> 注意：WorkManager 和 Service 不是一个概念，也没有直接联系。Service 是Android系统的四大组件之一。WorkManager 只是一个处理定时任务的工具。

## 1.1 WorkManager的类型

WorkManager处理三种类型的持久性工作：

- Immediate(立即)：必须立即开始并很快完成的任务。可能会加快。
- Long Running(长时间)：任务可能会运行更长时间，可能会超过10分钟。
- Deferrable(可延期)：计划的任务，在以后开始，可以定期运行。

下图概述了不同类型的持久性工作是如何相互关联的：

![img](../../../../art/0c5ede8131fc451fb6bdf157946097e1tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

## 1.2 概述了各种类型的 work

| **Type**     | **Periodicity** | **How to access**                                            |
| ------------ | --------------- | ------------------------------------------------------------ |
| Immediate    | 一次            | **`OneTimeWorkRequest`** 和 **`Worker`** 。 对于加急工作，请在OneTimeWorkRequest上调用 **`setExpedited()`** |
| Long Running | 一次或定期      | 任何 **`WorkRequest`** 和 **`Worker`**.  在 **`Worker`** 中调用 **`setForeground()`** 处理通知. |
| Deferrable   | 一次或定期      | **`PeriodicWorkRequest`** 和 **`Worker`**.                   |

## 1.3 使用 WorkManager 进行可靠的工作

WorkManager 旨在用于需要可靠运行的工作，即使用户离开屏幕、应用程序退出或设备重新启动也是如此。例如：

- 将日志或分析发送到后端服务。
- 定期与服务器同步应用程序数据。

WorkManager 不适用于在应用程序进程消失时可以安全终止的进程内后台工作。它也不是所有需要立即执行的工作的通用解决方案。

## 1.4 为什么使用 WorkManager？

WorkManager 运行后台工作，是处理兼容性问题以及电池和系统健康的最佳实践。

此外，使用 WorkManager，你可以安排周期性任务和复杂的相关任务链：后台工作可以并行或顺序执行，并且指定执行顺序。WorkManager 无缝处理任务之间的输入和输出传递。

你还可以设置有关何时运行后台任务的标准。例如，如果设备没有网络连接，则没有理由向远程服务器发出 HTTP 请求。

例如，我们构建一个并发任务的管道，将过滤器应用于图像。然后将结果发送到压缩任务，然后发送到上传任务。

![img](../../../../art/9184f9a5159548ca80307ce18b339b08tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

## 1.5 使用 WorkManager 的好处

- 处理与不同操作系统版本的兼容性。
- 遵循系统健康最佳实践。
- 支持异步一次性和周期性任务。
- 支持带有输入/输出的链式任务。
- 允许你设置任务运行时间的约束。
- 保证任务执行，即使应用程序或设备重新启动。

## 1.6 WorkManager 调度程序的工作原理

为确保与 API 级别 14 的兼容性，WorkManager 会根据设备 API 级别选择适当的方式来安排后台任务。WorkManager 可能使用 JobScheduler 或 BroadcastReceiver 和 AlarmManager 的组合。

![img](../../../../art/83048d44fe4a42a5aab87c3390888232tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

## 1.7 替换已弃用的 API

WorkManager API 是所有以前 Android 后台调度 API 的推荐替代品，包括 **FirebaseJobDispatcher**、**GcmNetworkManager** 和 Job **Scheduler**。

> 注意：如果您的应用面向 Android 10（API 级别 29）或更高版本，您的 FirebaseJobDispatcher 和 GcmNetworkManager API 调用将不再适用于运行 Android Marshmallow (6.0) 及更高版本的设备。

# 二、WorkManager 基本用法

## 2.0 概念

### 2.0.1 **WorkManager 基本用法十分简单，三步搞定**。

- 1、定义任务，并实现具体的任务逻辑。
- 2、配置该任务的运行条件和约束，并构建任务请求。
- 3、向系统提交任务请求。

### 2.0.2 **构建任务请求有两种方式:**

- OneTimeWorkRequest：一次性Work请求
- PeriodicWorkRequest：周期性Work请求

### 2.0.3 任务工作状态

**一次性任务工作状态**：

![img](../../../../art/9f3b00d99cb542a49d548f2ca3998a7ftplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

**周期性任务工作状态**：

![img](../../../../art/ca73cc96899b48fca3905f4c70fa87d3tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

### 2.0.4 核心类概述

- Worker(定义任务用到)：Worker是一个抽象类，这个类用来指定具体需要执行的任务。使用时要继承这个类并且实现里面的doWork()方法，在其中写具体的业务逻辑。
- WorkRequest(构建任务请求)：代表一项任务请求。一个 WorkRequest对象至少要指定一个Worker类。同时，还可以向WorkRequest对象添加，指定任务应运行的环境等。每个人WorkRequest都有一个自动生成的唯一ID， 可以使用该ID来执行诸如取消排队的任务或获取任务状态等操作。WorkRequest是一个抽象类; 有两个直接子类 OneTimeWorkRequest和 PeriodicWorkRequest。与WorkerRequest相关的有如下两个类：
  - WorkRequest.Builder：用于创建WorkRequest对象的助手类 ，其有两个子类OneTimeWorkRequest.Builder和 PeriodicWorkRequest.Builder，分别对应两者创建上述两种WorkerRequest。
  - Constraints：指定任务运行时的限制（例如，“仅在连接到网络时才能运行”）。可以通过 Constraints.Builder来创建该对象，并在调用WorkRequest.Builder的build()方法之前，将其传递 给WorkerRequest。
- WorkManager(提交任务请求)：这个类用来安排和管理工作请求。前面创建的WorkRequest 对象通过WorkManager来安排的顺序。 WorkManager调度任务的时候会分散系统资源，做好类似负载均衡的操作，同时会遵循前面设置的对任务的约束条件。
- WorkInfo(监听信息)：这个类包含 WorkRequest，其中包含 WorkRequest 的 id、其当前 WorkStatus(仅包含SUCCEEDED和FAILED)、输出、标签和进度。

是不是很简单，下面咱们通过 Java 和 Kotlin 两种语言来实现一下。

## 2.1 添加依赖

将以下依赖项添加到app/build.gradle文件中：

```java
dependencies {
    def work_version = "2.7.1"

    // (Java only)
    implementation "androidx.work:work-runtime:$work_version"

    // Kotlin + coroutines
    implementation "androidx.work:work-runtime-ktx:$work_version"

    // optional - RxJava2 support
    implementation "androidx.work:work-rxjava2:$work_version"

    // optional - GCMNetworkManager support
    implementation "androidx.work:work-gcm:$work_version"

    // optional - Test helpers
    androidTestImplementation "androidx.work:work-testing:$work_version"

    // optional - Multiprocess support
    implementation "androidx.work:work-multiprocess:$work_version"
}
复制代码
```

## 2.2 使用 WorkManager (Java)

### 2.2.1 定义任务

定义任务必须继承自 Worker 类，并调用它的构造方法(唯一)。重写 doWork() 方法。

> 注意:doWork() 方法 在 WorkManager 提供的后台线程上异步运行,因此你可以放心的在这里执行耗时操作。

```java
/**
  * 创建人：帅次
  * 创建时间：2022/1/27
  * 功能：定义任务
  */
public class JavaTestWork extends Worker {
    public JavaTestWork(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //执行耗时操作
        doSmothing();
        return Result.success();
    }
    private void doSmothing(){
        //这里仅仅简单打印一下
        Log.e("JavaTestWork","JavaTestWork 在执行 doWork ");
    }
}
复制代码
```

从 doWork() 返回的 Result 通知 WorkManager 服务工作是否成功，以及在失败的情况下是否应该重试工作:

- Result.success()：成功。
- Result.failure()：失败。
- Result.retry()：失败，可以结合 WorkRequest.Builder 的 setBackoffCriteria() 方法重新执行任务。

### 2.2.2 构建任务请求

使用 WorkRequest.Worker 定义了工作单元，而 WorkRequest（及其子类）定义了它应该如何以及何时运行。在最简单的情况下，你可以使用 OneTimeWorkRequest。

```java
        //构建任务请求
        //方法一：不需要额外配置的简单Work
        WorkRequest fromWorkRequest = OneTimeWorkRequest.from(JavaTestWork.class);
        //方法二：复杂的Work，可使用构建器进行配制
        WorkRequest workRequest =
                new OneTimeWorkRequest.Builder(JavaTestWork.class).build();
复制代码
```

### 2.2.3 向系统提交 WorkRequest

最后，使用 enqueue() 方法将 WorkRequest 构建的任务提交给 WorkManager。系统会在合适的时间去运行。

```java
        binding.btnDowork.setOnClickListener(view -> {
            Snackbar.make(view, "点击 doWork ", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            //向系统提交 WorkRequest
            WorkManager.getInstance(JavaWorkActivity.this).enqueue(workRequest);
        });
复制代码
```

![img](../../../../art/1c32bf44cc54447bba635d1ae9ddc5c4tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

# 三、WorkManager 进阶用法

上面的内容也就仅仅是WorkMannager的简单用法，下面咱们来看看如何用WorkManager来处理复杂的任务。

## 3.1 定时任务

这个在项目中没少用到，一起来看看。这里在构建任务的时候借助 setInitialDelay() 方法就可以实现。

```java
        //构建复杂任务
        WorkRequest timedWorkRequest =
                new OneTimeWorkRequest.Builder(JavaTestWork.class)
                        //1分钟后执行，当然你也可以用此方法指定单位(毫秒/秒/分钟/小时/天)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .build();
        binding.btnTimedwrok.setOnClickListener(v -> {
            Log.e("btnTimedwrok","start");
            WorkManager.getInstance(JavaWorkActivity.this).enqueue(timedWorkRequest);
            Log.e("btnTimedwrok","end");
        });
复制代码
```

![img](../../../../art/166f185313f045f4b0fe97671faefb03tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

结果还真是一分钟后执行。

## 3.2 取消或停止任务

有些情况需要取消我们提交的任务。可以通过其 **name 、id  或 tag** 来取消。

### 3.2.1 通过 Id 取消任务

```java
        binding.btnCancelWork.setOnClickListener(v -> {
            Log.e("btnCancelWork","cancel");
            // 通过 id 取消任务
            workManager.cancelWorkById(timedWorkRequest.getId());
        });
复制代码
```

![img](../../../../art/240bbedc38044124994f6101d74d1656tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

### 3.2.2 通过 Tag 取消任务

```java
        WorkRequest timedWorkRequest =
                new OneTimeWorkRequest.Builder(JavaTestWork.class)
                        //1分钟后执行，当然你也可以用此方法指定单位(毫秒/秒/分钟/小时/天)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        //添加 tag ，可用于取消任务
                        .addTag("timedWrok")
                        .build();
        binding.btnCancelWork.setOnClickListener(v -> {
            Log.e("btnCancelWork","cancel");
            //通过 tag 取消任务
            workManager.cancelAllWorkByTag("timedWrok");
        });
复制代码
```

![img](../../../../art/e8f4991d858841e597e2838b9871f906tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

### 3.2.3 取消所有任务

```java
            //取消所有任务
            workManager.cancelAllWork();
复制代码
```

## 3.3 观察任务

观察任务信息也很简单。可以使用 getWorkInfoBy...() 或 getWorkInfoBy...LiveData() 方法，并获取对 WorkInfo 的引用。

这里咱们观察一下任务的**进度和任务返回状态**。

```java
        //观察任务(Observing)
        WorkRequest observeWork = new OneTimeWorkRequest.Builder(JavaTestWorkInfo.class)
                .build();
        binding.btnObserveWorkinfo.setOnClickListener(v -> {
            Log.e("btnObserveWorkinfo", "start");
            workManager.getWorkInfoByIdLiveData(observeWork.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null) {
                            Data progress = workInfo.getProgress();
                            int value = progress.getInt("PROGRESS", 0);
                            if (value>0) {
                                Log.e("MainObserve", "当前进度：" + value);
                            }
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                Log.e("MainState", "成功");
                            } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                                Log.e("MainState", "失败");
                            } else if (workInfo.getState() == WorkInfo.State.CANCELLED) {
                                Log.e("MainState", "取消");
                            }
                        } else {
                            Log.e("MainObserve", "workInfo == null");
                        }
                    });
            //注意别忘记添加到WorkManager队列中
            workManager.enqueue(observeWork);
        });
    }
复制代码
```

![img](../../../../art/273969167b764d8181378f5d04c4416dtplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

下面咱们看看通过 setProgressAsync() API 更新进度的 Worker。

```java
public class JavaTestWorkInfo extends Worker {
    private static final String PROGRESS = "PROGRESS";
    private static final long DELAY = 5000L;
    public JavaTestWorkInfo(@NonNull @NotNull Context context, @NonNull @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.e("JavaTestWorkInfo","构造方法");
        setProgressAsync(new Data.Builder().putInt(PROGRESS,10).build());
    }

    @NonNull
    @NotNull
    @Override
    public Result doWork() {
        //这里仅仅简单打印一下
        Log.e("JavaTestWorkInfo","在执行 doWork ");
        //执行耗时操作
        doSmothing();
        try {
            Log.e("JavaTestWorkInfo","Result.success");
            setProgressAsync(new Data.Builder().putInt(PROGRESS,100).build());
            Thread.sleep(DELAY);
        } catch (InterruptedException exception) {
            // ... handle exception
        }
        return Result.success();
    }
    private void doSmothing(){
        try {
            setProgressAsync(new Data.Builder().putInt(PROGRESS,40).build());
            Thread.sleep(DELAY);
        } catch (InterruptedException exception) {
            // ... handle exception
        }
    }
}
复制代码
```

当你上传下载资源的时候这个进度是不是对你就有用了很多？

## 3.4 重新执行失败任务

当后台任务 doWork() 返回 Result.retry() 工作失败，可以结合 WorkRequest.Builder 的 setBackoffCriteria() 方法重新执行任务。

```java
        WorkRequest retryWork = new OneTimeWorkRequest.Builder(JavaTestWorkRetry.class)
                .setBackoffCriteria(BackoffPolicy.LINEAR,10,TimeUnit.SECONDS)
                .build();
        binding.btnReturnRetry.setOnClickListener(v -> {
            Log.e("btnReturnRetry", "start");
            workManager.getWorkInfoByIdLiveData(retryWork.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null) {
                            //...
                        } else {
                            Log.e("btnReturnRetry", "workInfo == null");
                        }
                    });
            //注意别忘记添加到WorkManager队列中
            workManager.enqueue(retryWork);
        });
复制代码
```

- 第一个参数：BackoffPolicy有两个值：
  - BackoffPolicy.LINEAR(每次重试的时间线性增加，比如第一次10秒，第二次就是20秒)
  - BackoffPolicy.EXPONENTIAL(每次重试时间指数增加)。
- 第二个和第三个参数：指定多久之后重新执行任务，时间最短不能少于10秒。

![img](../../../../art/1b58242f0b98492c80fd967a30874eb3tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

## 3.5 传值

- 将 Activity 的数据传到任务中取。
- 将任务中的数据传给 Activity 。

```java
        //传入Worker的值
        Data data = new Data.Builder()
                .putInt("age",20)
                .putString("name","Scc").build();
        WorkRequest inputWork = new OneTimeWorkRequest.Builder(JavaTestWorkInput.class)
                .setInputData(data)
                .build();
        binding.btnInputData.setOnClickListener(v -> {
            Log.e("btnInputData", "start");
            workManager.getWorkInfoByIdLiveData(inputWork.getId())
                    .observe(this, workInfo -> {
                        if (workInfo != null) {
                            //...
                            Data outputData = workInfo.getOutputData();
                            String str = outputData.getString("work")+":"+outputData.getInt("price",-50);
                            if (outputData.getString("work")!=null) {
                                Log.e("btnInputData", str);
                            }
                        } else {
                            Log.e("btnInputData", "workInfo == null");
                        }
                    });
            //注意别忘记添加到WorkManager队列中
            workManager.enqueue(inputWork);
        });
        
public class JavaTestWorkInput extends Worker {
    ...
    public Result doWork() {
        //这里仅仅简单打印一下
        Log.e("JavaTestWorkInput","在执行 doWork ");
        //接受Activity传过来的值
        Data data = getInputData();
        //打印
        String msg = data.getString("name")+" 今年 "+data.getInt("age",5)+" 岁了";
        Log.e("JavaTestWorkInput",msg);
        Data outputData  = new Data.Builder()
                .putString("work","我来回礼")
                .putInt("price",500)
                .build();
        return Result.success(outputData);
    }
}
复制代码
```

![img](../../../../art/f5865f6a061d4338bdb9487d86991127tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)

## 3.6 约束条件

```java
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)//触发时设备是否为空闲
                .setRequiresCharging(true)//触发时设备是否充电
                .setRequiredNetworkType(NetworkType.UNMETERED)//触发时网络状态
                .setRequiresBatteryNotLow(true)//指定设备电池是否不应低于临界阈值
                .setRequiresStorageNotLow(true)//指定设备可用存储是否不应低于临界阈值
//                .addContentUriTrigger(myUri,false)//指定内容{@link android.net.Uri}时是否应该运行{@link WorkRequest}更新
                .build();
        WorkRequest inputWork = new OneTimeWorkRequest.Builder(JavaTestWorkInput.class)
                .setInputData(data)
                .setConstraints(constraints)
                .build();
复制代码
```

## 3.7 链式任务

假设咱们有三个独立的后台任务：获取图片，压缩图片和上传图片。那么我们想要实现这个功能就可以使用链式任务来搞定。

```java
        //链式调用
        workManager
                // 并行运行
                .beginWith(Arrays.asList(work1, work2, work3))
                // 执行完签名的操作继续调用压缩任务
                .then(compression)
                //执行完压缩任务调用上传任务
                .then(upload)
                // 添加到WorkManager队列中
                .enqueue();
复制代码
```

- beginWith：beginWith()方法表示开始一个链式任务。
- then：后面不管什么任务都用then()方法来链接即可。

![img](../../../../art/10fa2ffbf18149a993284d1dcfa7f4b4tplv-k3u1fbpfcp-zoom-in-crop-mark1304000.awebp)