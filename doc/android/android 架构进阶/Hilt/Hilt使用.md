### Hilt是什么？

Hilt是Android的依赖注入库，可以减少在项目中执行手动依赖项注入的样板代码。执行手动依赖项注入需要手动构造每个类及其依赖项，并借助容器重复使用和管理依赖项。

### 和Degger有什么不同？

Hilt是在Degger的基础上构建而成，更具场景化。Hilt通过为项目中的每个Android类提供容器并自动管理其生命周期，在简化依赖注入使用的同时保留了Degger原有的强大功能。（类似于retrofit与okhttp的关系）

### Hilt凭什么吸引我？

是数据共享和依赖管理，确切一点是生命周期内数据共享，是不是有点不知所云，没关系咱看代码
 一个数据类：

```java
@ActivityScoped
public class User {
    private String name;
    private String mood;

    @Inject
    public User() {
        this("matthew", "百无聊赖");
    }

    public User(String name, String mood) {
        this.name = name;
        this.mood = mood;
    }
    ......省略不重要的代码
}
复制代码
```

Activity

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        user.mood = "一切都好"
     }
 ......省略不重要的代码
}
复制代码
```

一个自定义view继承于AppCompatTextView ，它需要使用User对象的数据进行展示.它被放在了MainActivity的layout里

```java
@AndroidEntryPoint
public class UserView extends androidx.appcompat.widget.AppCompatTextView {
    @Inject
    User user;
     ......省略不重要的代码
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setText(user.getName() + "现在的心情是" + user.getMood());
    }
}
复制代码
```

我们的使用场景是，需要在MainActivity里动态的去修改User里的值，然后UserView去展示修改过后的值。老司机门一定马上就想到了，先在UserView添加一个接收User对象的方法，比如`public void setUser(User user){...}`然后在activity里获取userView的对象并调用userView.setUser函数把对象传过去。这种方式我用了好多年了，但是接触hilt之后瞬间感觉不优雅了。假设之后需求变了不需要这个User对象了呢？。。。。一顿删除。 。。。。头都大了吧
 如果使用Hilt来依赖注入，我们只需要删除注入的地方`@Inject User user;`即可，就是这么简单。
 *例子举的还是有一点点不太理想*

想在介绍一大堆枯燥的规则配置项之前先给大家讲讲怎么最简单的实现上面的生命周期类共享数据（只讲操作，为什么这么写后面会讲）
 1、添加依赖项 （往下翻翻，不重复写了）
 2、给Applicaton添加@HiltAndroidApp注解

```java
@HiltAndroidApp
public class ExampleApplication extends Application { ... }
复制代码
```

3、准备需要注入的数据类，给无参构造函数添加`@Inject`注解，给User类添加`@ActivityScoped`注解

```java
@ActivityScoped
public class User {
    private String name;
    private String mood;

    @Inject
    public User() {
        this("matthew", "百无聊赖");
    }

    public User(String name, String mood) {
        this.name = name;
        this.mood = mood;
    }
    ......省略不重要的代码
}
复制代码
```

4、需要时引入
 给需要依赖注入的类添加`@AndroidEntryPoint`注解
 给对象声明添加`@Inject`注解

```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        user.mood = "一切都好"
     }
 ......省略不重要的代码
}
复制代码
@AndroidEntryPoint
public class UserView extends androidx.appcompat.widget.AppCompatTextView {
    @Inject
    User user;
     ......省略不重要的代码
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setText(user.getName() + "现在的心情是" + user.getMood());
    }
}
复制代码
```

至此就完成了！！！

### Hilt怎么用

- ##### 添加依赖项

首先，将 hilt-android-gradle-plugin 插件添加到项目的根级 build.gradle 文件中：

```java
buildscript {
    ...
    dependencies {
        ...
        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.28-alpha'
    }
}
复制代码
```

然后，应用 Gradle 插件并在 app/build.gradle 文件中添加以下依赖项：

```java
...
apply plugin: 'kotlin-kapt'
apply plugin: 'dagger.hilt.android.plugin'

android {
    ...
}

dependencies {
    implementation "com.google.dagger:hilt-android:2.28-alpha"
    kapt "com.google.dagger:hilt-android-compiler:2.28-alpha"
}
复制代码
```

Hilt 使用 [Java 8 功能](https://link.juejin.cn?target=https%3A%2F%2Flinks.jianshu.com%2Fgo%3Fto%3Dhttps%3A%2F%2Fdeveloper.android.google.cn%2Fstudio%2Fwrite%2Fjava8-support%3Fhl%3Dzh_cn)。如需在项目中启用 Java 8，请将以下代码添加到 `app/build.gradle` 文件中：

```
android {
  ...
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
}
复制代码
```

- ##### Hilt 目前支持以下 Android 类：

> 通过使用 @HiltAndroidApp
>  Application（）
>  其它通过使用@AndroidEntryPoint
>  Activity
>  Fragment
>  View
>  Service
>  BroadcastReceiver

@HiltAndroidApp 会触发 Hilt 的代码生成操作，生成的代码包括应用的一个基类，该基类充当应用级依赖项容器。

```java
@HiltAndroidApp
public class ExampleApplication extends Application { ... }
复制代码
```

在 Application 类中设置了 Hilt 且有了应用级组件后，Hilt 可以为带有 @AndroidEntryPoint 注释的其他 Android 类提供依赖项：

```java
@AndroidEntryPoint
public class ExampleActivity extends AppCompatActivity { ... }
复制代码
```

- ##### 注入

如需从组件获取依赖项，请使用 @Inject 注释执行字段注入：

```java
@AndroidEntryPoint
public class ExampleActivity extends AppCompatActivity {

  @Inject
  AnalyticsAdapter analytics;
  ...
}
复制代码
```

- ##### 定义Hilt绑定

为了执行字段注入，Hilt需要知道如何从相应组件提供必要依赖项的实例，向Hilt提供绑定信息的一种方法是构造函数注入。在类的构造函数中使用`@Inject`注释以告知Hilt如何提供该类实例。

```java
public class AnalyticsAdapter {

  private final AnalyticsService service;

  @Inject
  AnalyticsAdapter(AnalyticsService service) {
    this.service = service;
  }
  ...
}
复制代码
```

- ##### Hilt 模块

上例中给出的构造函数需要一个参数，那么我们还需要告知Hilt如何获得AnalyticsService的实例。很简单啊！对AnalyticsService的构造函数添加`@Inject`不就可以了。
 但是有时候实例不能通过构造函数注入。发生这种情况可能有多种原因。例如，不能通过构造函数注入接口；或者不归我们所有的类型，比如第三方库；或者必须通过构造器构建的实例。
 怎么办？需要用到Hilt模块，一种带有`@Module`注释的类。它会告诉Hilt如何提供某些类型的实例。同时必须使用`@InstallIn`为模块添加注释，告诉Hilt每个模块将用在或安装在那个Android类中。

- ##### 使用 @Provides 注入实例

如果类不归我们所有，或者需要使用构造器时，可以在Hilt模块内创建一个函数添加 `@Provides`注解
 `@Provides`注解会告诉Hilt以下信息
 1、函数返回类型会告诉Hilt提供哪个类型的实例
 2、函数参数或告诉Hilt相应类型的依赖项
 3、函数体会告诉Hilt如何提供相应类型的实例

```java
@Module
@InstallIn(ActivityComponent.class)
public class AnalyticsModule {

  @Provides
  @ActivityScoped  //共享范围
  public static AnalyticsService provideAnalyticsService(
    // Potential dependencies of this type
  ) {
      return new Retrofit.Builder()
               .baseUrl("https://example.com")
               .build()
               .create(AnalyticsService.class);
  }
}
复制代码
```

- ##### 使用 @Binds 注入接口实例

如果 `AnalyticsService`是一个接口，则无法通过构造函数注入，而应向Hilt提供绑定信息，在Hilt模块内创建一个带有`@Binds` 注释的抽象函数。
 带有注释的函数会向Hilt提供以下信息
 1、函数返回类型会告诉Hilt需要提供哪个接口的实例
 2、函数参数会告诉Hilt要提供哪种实现

```java
public interface AnalyticsService {
  void analyticsMethods();
}

// Constructor-injected, because Hilt needs to know how to
// provide instances of AnalyticsServiceImpl, too.
public class AnalyticsServiceImpl implements AnalyticsService {
  ...
  @Inject
  AnalyticsServiceImpl(...) {
    ...
  }
}

@Module
@InstallIn(ActivityComponent.class)
public abstract class AnalyticsModule {

  @Binds
  public abstract AnalyticsService bindAnalyticsService(
    AnalyticsServiceImpl analyticsServiceImpl
  );
}
复制代码
```

- ##### Hilt组件

可以从中执行字段注入的每个Android类都有一个与之关联的Hilt组件。可以在`@InstallIn`中引入该组件，每个Hilt负责将其绑定注入相应的Android类。

| Hilt 组件                 | 注入器面向的对象                       |
| ------------------------- | -------------------------------------- |
| ApplicationComponent      | Application                            |
| ActivityRetainedComponent | ViewModel                              |
| ActivityComponent         | Activity                               |
| FragmentComponent         | Fragment                               |
| ViewComponent             | View                                   |
| ViewWithFragmentComponent | 带有 @WithFragmentBindings 注释的 View |
| ServiceComponent          | Service                                |

那么比如使用`@InstallIn(ActivityComponent.class)`将module注入到activity之后，activity下的view能否获得依赖呢？具体规则是什么？

- ##### 组件层级结构

将模块安装到组件后，其绑定就可以用做该组件内其它绑定的依赖项，也可以用作组件层级中该组件下的任何子组件中其它绑定的依赖项。就是向下使用关系

![image.png](../../../../art/Hilt%E4%BD%BF%E7%94%A8/12164fa6043d4e58a71402eb42bbd002tplv-k3u1fbpfcp-watermark.webp)

> 注意：默认情况下，如果您在视图中执行字段注入，ViewComponent 可以使用 ActivityComponent 中定义的绑定。如果您还需要使用 FragmentComponent 中定义的绑定并且视图是 Fragment 的一部分，应将 @WithFragmentBindings 注释和 @AndroidEntryPoint 一起使用。

- ##### 组件的声明周期

Hilt会根据Android类的生命周期自动创建和销毁组件类的实例

| 生成的组件                | 创建时机               | 销毁时机                |
| ------------------------- | ---------------------- | ----------------------- |
| ApplicationComponent      | Application#onCreate() | Application#onDestroy() |
| ActivityRetainedComponent | Activity#onCreate()    | Activity#onDestroy()    |
| ActivityComponent         | Activity#onCreate()    | Activity#onDestroy()    |
| FragmentComponent         | Fragment#onAttach()    | Fragment#onDestroy()    |
| ViewComponent             | View#super()           | 视图销毁时              |
| ViewWithFragmentComponent | View#super()           | 视图销毁时              |
| ServiceComponent          | Service#onCreate()     | Service#onDestroy()     |

> 注意：ActivityRetainedComponent 在配置更改后仍然存在，因此它在第一次调用 Activity#onCreate() 时创建，在最后一次调用 Activity#onDestroy() 时销毁。

- ##### 作用域

如果使用`@ActivityScoped`将AnalyticsService的作用域限定为`ActivityComponent`，Hilt会在Activity的整个周期内提供AnalyticsService的同一实例

```java
@Provides
  @ActivityScoped  //共享范围
  public static AnalyticsService provideAnalyticsService(
    // Potential dependencies of this type
  ) {
      return new Retrofit.Builder()
               .baseUrl("https://example.com")
               .build()
               .create(AnalyticsService.class);
  }
复制代码
```

| Android 类                             | 生成的组件                | 作用域                 |
| -------------------------------------- | ------------------------- | ---------------------- |
| Application                            | ApplicationComponent      | @Singleton             |
| View Model                             | ActivityRetainedComponent | @ActivityRetainedScope |
| Activity                               | ActivityComponent         | @ActivityScoped        |
| Fragment                               | FragmentComponent         | @FragmentScoped        |
| View                                   | ViewComponent             | @ViewScoped            |
| 带有 @WithFragmentBindings 注释的 View | ViewWithFragmentComponent | @ViewScoped            |
| Service                                | ServiceComponent          | @ServiceScoped         |

- ##### Hilt中的预定义限定符

Hilt提供了一些自定义的限定符，例如开发中可能需要来自应用或者Activity的context对象。因此 Hilt 提供了 `@ApplicationContext`和 `@ActivityContext`限定符。

```java
public class AnalyticsAdapter {

  private final Context context;
  private final AnalyticsService service;

  @Inject
  AnalyticsAdapter(
    @ActivityContext Context context,
    AnalyticsService service
  ) {
    this.context = context;
    this.service = service;
  }
}
复制代码
```

- ##### 为同一类型提供多个绑定

我将这个放在了最后，为什么？因为按照我的使用习惯，这个用到的情况比较少。
 比如现在需要获取同一个接口的不同实例。上面讲到了使用 @Binds 注入接口实例，但是现在需要多个不同的实例，该怎么办？返回值类型都是一样的啊！！！
 首先，要定义用于`@Binds`或`@Provides`方法添加注释的限定符

```java
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
private @interface AuthInterceptorOkHttpClient {}

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
private @interface OtherInterceptorOkHttpClient {}
复制代码
```

然后，Hilt需要知道如何提供与每个限定符对应的类型的实例。下面的两个方法具有相同的返回值类型，但是限定符将它们标记为两个不同的绑定。

```java
@Module
@InstallIn(ActivityComponent.class)
public class NetworkModule {

  @AuthInterceptorOkHttpClient
  @Provides
  public static OkHttpClient provideAuthInterceptorOkHttpClient(
    AuthInterceptor authInterceptor
  ) {
      return new OkHttpClient.Builder()
                   .addInterceptor(authInterceptor)
                   .build();
  }

  @OtherInterceptorOkHttpClient
  @Provides
  public static OkHttpClient provideOtherInterceptorOkHttpClient(
    OtherInterceptor otherInterceptor
  ) {
      return new OkHttpClient.Builder()
                   .addInterceptor(otherInterceptor)
                   .build();
  }
}
复制代码
```

在使用过程中使用相应的限定符为字段或参数添加注释来注入所需的特定类型：

```java
// As a dependency of another class.
@Module
@InstallIn(ActivityComponent.class)
public class AnalyticsModule {

  @Provides
  public static AnalyticsService provideAnalyticsService(
    @AuthInterceptorOkHttpClient OkHttpClient okHttpClient
  ) {
      return new Retrofit.Builder()
                  .baseUrl("https://example.com")
                  .client(okHttpClient)
                  .build()
                  .create(AnalyticsService.class);
  }
}

// As a dependency of a constructor-injected class.
public class ExampleServiceImpl ... {

  private final OkHttpClient okHttpClient;

  @Inject
  ExampleServiceImpl(@AuthInterceptorOkHttpClient OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
  }
}

// At field injection.
@AndroidEntryPoint
public class ExampleActivity extends AppCompatActivity {

  @AuthInterceptorOkHttpClient
  @Inject
  OkHttpClient okHttpClient;
  ...
}
```