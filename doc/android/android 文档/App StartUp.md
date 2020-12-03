# Android架构组件–App Startup

## 1.解决的问题

一般需要初始化的sdk都会对外提供一个初始化方法供外界调用，如：

```
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Sdk1.init(this);
    }
}
12345678
```

对调用者很不友好。另一种做法是使用`ContentProvider`初始化，如下：

```
public class Sdk1InitializeProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Sdk1.init(getContext());
        return true;
    }
	...
}
12345678
```

然后在`AndroidManifest.xml`文件中注册这个`privoder`，如下：

```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="cn.zhengzhengxiaogege.sdk1">
    <application>
        <provider
            android:authorities="${applicationId}.init-provider"
            android:name=".Sdk1InitializeProvider"
            android:exported="false"/>
    </application>
</manifest>
123456789
```

这样初始化的逻辑就由Sdk开发者在内部完成了。

但是，如果一个app依赖了很多需要初始化的sdk，如果都放在一个`ContentProvider`中会导致此`ContentProvider`代码数量增加。而且每增加一个需要初始化的sdk都要对该`ContentProvider`文件做改动，不方便合作开发。而如果每个sdk都采用同样的方式将会带来性能问题。`App Startup library`可以有效解决这个问题。

## 2.使用`App StartUp`

### (1)添加依赖

在App模块的`build.gradle`文件中添加依赖：

```
dependencies {
    implementation "androidx.startup:startup-runtime:1.0.0-alpha01"
}
123
```

### (2)实现`Initializer`接口

app通过`Initializer`接口接入`App Startup`，需要实现两个方法

```
public interface Initializer<T> {

  @NonNull
  T create(@NonNull Context context);

  @NonNull
  List<Class<? extends Initializer<?>>> dependencies();
}

123456789
```

例如有一个`Sdk1`如下：

```
public class Sdk1 {

  private static final String TAG = "Sdk1";

  private static Context sApplicationContext;

  private static volatile Sdk1 sInstance;

  public static void init(Context applicationContext){
      sApplicationContext = applicationContext;
      Log.e(TAG, "Sdk1 is initialized");
  }

  public static Sdk1 getInstance(){
      if (sInstance == null) {
          synchronized (Sdk1.class){
              if (sInstance == null) {
                  sInstance = new Sdk1();
              }
          }
      }
      return sInstance;
  }

  private Sdk1(){
  }

  public void printApplicationName(){
      Log.e(TAG, sApplicationContext.getPackageName());
  }
}
12345678910111213141516171819202122232425262728293031
```

`sdk1`对外提供`Sdk1`类，包含初始化方法`init(Context)`，实例获取方法`getInstance()`和对外的服务方法`printApplicationName()`。为了使用`App Startup`，需要提供一个初始化器如下：

```
public class Sdk1Initializer implements Initializer<Sdk1> {
    @NonNull
    @Override
    public Sdk1 create(@NonNull Context context) {
        Sdk1.init(context);
        return Sdk1.getInstance();
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
1234567891011121314
```

泛型`T`为待初始化的Sdk对外提供的对象类型；`create(Context)`方法是该`Sdk`初始化
逻辑写入的地方，其参数`context`为`Application Context`，同时需要返回一个Sdk对外提供的对象实例。

`dependencies()`方法则需要返回一个列表，这个列表需要给出一个该Sdk依赖的其它
Sdk的初始化器，也就是这个列表决定了哪些sdk会在这个sdk之前初始化，如果这个sdk是独立的没有依赖与其它的sdk，可以将该方法返回一个空列表(如`Sdk1Initializer`的实现)。

但是如果这个sdk依赖于其它的sdk，必须在其它sdk初始化之后才能初始化，则需要在`dependencies()`方法中指明。例如现在有一个`sdk2`也需要初始化，且它必须在`sdk1`初始化之后才能初始化，那么`sdk2`的初始化器的实现如下：

```
public class Sdk2Initializer implements Initializer<Sdk2> {
    @NonNull
    @Override
    public Sdk2 create(@NonNull Context context) {
        Sdk2.init(context);
        return Sdk2.getInstance();
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        List<Class<? extends Initializer<?>>> dependencies = new ArrayList<>();
        dependencies.add(Sdk1Initializer.class);
        return dependencies;
    }
}
12345678910111213141516
```

在`dependencies()`方法中指明了`Skd2`的依赖项，因此`App Startup`会在初始化`sdk2`之前先初始化`sdk1`。

### (3)注册`Provider`和`Initializer`

我们需要告诉`App Startup`我们实现了哪些Sdk初始化器(`Sdk1Initializer`、`Sdk2Initializer`)。同时`App Startup`并没有提供`AndroidManifest.xml`文件，因此`App Startup`用到的`provider`同样需要注册。在app的`AndroidManifest.xml`文件添加如下代码：

```
<provider
            android:authorities="${applicationId}.androidx-startup"
            android:name="androidx.startup.InitializationProvider"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="cn.zhengzhengxiaogege.appstartupstudy.Sdk1Initializer"
                android:value="@string/androidx_startup"/>
			<meta-data
				android:name="cn.zhengzhengxiaogege.appstartupstudy.Sdk2Initializer"
				android:value="@string/androidx_startup"/>
        </provider>
123456789101112
```

通常每一个初始化器对应一个``标签，但是如果有些初始化器已经被一个已
经注册的初始化器依赖(比如`Sdk1Initializer`已经被`Sdk2Initializer`依赖)，那么
可以不用在`AndroidManifest.xml`文件中显式地指明，因为`App Startup`已经通过
注册的`Sdk2Initializer`找到它了。

**这里的``标签的`value`属性必须指定为字符串`androidx_startup`的值，
也就是(`"androidx.startup"`)，否则将不生效。**

如果有一个`sdk3`内部通过`App Startup`帮助使用者处理了初始化，那么`sdk3`的`AndroidManifest.xml`文件中已经存在了`InitializationProvider`的`provider`标签，此时会与app模块中的冲突，因此在app模块的`provider`标签中指明`tools:node="merge"`，通过`AndroidManifest.xml`文件的合并机制。

## 3.`App StartUp`实现懒加载

为了减少app启动时间，对于一些非必须的初始化应该在app启动后、sdk使用前完成初始化，使用在`provider`中注册的方法不能达到这个目的。`App StartUp`提供了`AppInitializer`来解决这个问题。如下，在需要初始化的位置使用`AppInitializer`:

```
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity Created");

        AppInitializer.getInstance(getApplicationContext())
                .initializeComponent(Sdk2Initializer.class);

        Sdk1.getInstance().printApplicationName();
    }
}
1234567891011121314151617
```

同时需要失能`AndroidManifest.xml`文件中的对应初始化器的``，如下：

```
<provider
            android:authorities="${applicationId}.androidx-startup"
            android:name="androidx.startup.InitializationProvider"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="cn.zhengzhengxiaogege.appstartupstudy.Sdk2Initializer"
                android:value="@string/androidx_startup"
                tools:node="remove"/>
        </provider>
12345678910
```

通过`tools:node="remove"`来标记该初始化器。这样会在`AndroidManifest.xml`文件合并时将这个``移除掉，否则该初始化器仍会在`Application`中被初始化并标记为已经初始化，后面的懒加载将不执行任何初始化操作，相当于使懒加载失效了。

## 4.剖析`App StartUp`

`App StartUp`的设计思路比较简单，就是将多个需要初始化的Sdk在一个`provider`中完成，从而减少多个`provider`带来的性能问题和繁杂的`AndroidManifest.xml`文件声明。目前`App StartUp`为`1.0.0-alpha01`版本，其代码结构非常简单。

![App Startup 代码结构](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20veGlhb2dlZ2VjaGVuL21hcmtkb3duX2ZpZ3VyZXMvcmF3L21hc3Rlci9BbmRyb2lkL2FyY2hfY29tcGVudC9tZF9yZXMvYXBwX3N0YXJ0dXBfY29kZV9saXN0LnBuZw?x-oss-process=image/format,png)

只有五个类文件，除去`StartupException`和`StartupLogger`，其核心类只有三个。

`Intializer.java`的作用很简单，就是为lib的使用者提供了接入的方法，因此不再赘述。

`InitializationProvider.java`是`App StartUp`中使用的单一的`provider`，所有注册的初始化将在这个`provider`中完成。`App StartUp`只重写了`OnCreate()`方法：

```
public final class InitializationProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            AppInitializer.getInstance(context).discoverAndInitialize();
        } else {
            throw new StartupException("Context cannot be null");
        }
        return true;
    }
	...
123456789101112
```

可以看到，在`OnCreate()`方法中执行了扫描和初始化注册的组件。其实现方法在
`AppInitializer.java`中。`AppInitializer`内部是一个单例实现，它的`getInstance(Context)`方法传入的是`Application`级别的`Context`，并将其传递给注册的各`Intializer`，首先看`discoverAndInitialize()`方法：

```
@SuppressWarnings("unchecked")
void discoverAndInitialize() {
	try {
		Trace.beginSection(SECTION_NAME);
		
		// 扫描并获取Manifest文件中的 `InitializationProvider`这个组件中注册的<meta-data>
		// 信息
		ComponentName provider = new ComponentName(mContext.getPackageName(),
				InitializationProvider.class.getName());
		ProviderInfo providerInfo = mContext.getPackageManager()
				.getProviderInfo(provider, GET_META_DATA);
		Bundle metadata = providerInfo.metaData;
		
		// 然后遍历<meta-data>标签，获取到每个标签的 `Initializer`并对其初始化
		String startup = mContext.getString(R.string.androidx_startup);
		if (metadata != null) {
			Set<Class<?>> initializing = new HashSet<>();
			Set<String> keys = metadata.keySet();
			for (String key : keys) {
				
				// 注意这里会用<meta-data>标签的value属性的值和`@string/androidx_startup`
				// 对比，只有是这个值的<meta-data>标签才会被初始化。
				String value = metadata.getString(key, null);
				if (startup.equals(value)) {
					Class<?> clazz = Class.forName(key);
					if (Initializer.class.isAssignableFrom(clazz)) {
						Class<? extends Initializer<?>> component =
								(Class<? extends Initializer<?>>) clazz;
						if (StartupLogger.DEBUG) {
							StartupLogger.i(String.format("Discovered %s", key));
						}
						doInitialize(component, initializing);
					}
				}
			}
		}
	} catch (PackageManager.NameNotFoundException | ClassNotFoundException exception) {
		throw new StartupException(exception);
	} finally {
		Trace.endSection();
	}
}
123456789101112131415161718192021222324252627282930313233343536373839404142
```

`discoverAndInitialize()`方法首先扫描清单文件获取到需要初始化的初始化器`Initializer`，然后执行初始化操作，即调用`doInitialize(Class>, Set>)`方法，如下：

```
@NonNull
@SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
<T> T doInitialize(
		@NonNull Class<? extends Initializer<?>> component,
		@NonNull Set<Class<?>> initializing) {
	synchronized (sLock) {
		boolean isTracingEnabled = Trace.isEnabled();
		try {
			if (isTracingEnabled) {
				Trace.beginSection(component.getSimpleName());
			}
			
			// `initializing`存储着正在初始化的初始化器。
			// 这个判断是要解决循环依赖的问题，比如 `Sdk1Initializer`依赖了它本身，或者是
			// `Sdk1Initializer` 依赖了 `Sdk2Initializer`，同时 `Sdk2Initializer` 又
			// 依赖了 `Sdk1Initializer`，这是存在逻辑错误的，因此需要排除。
			if (initializing.contains(component)) {
				String message = String.format(
						"Cannot initialize %s. Cycle detected.", component.getName()
				);
				throw new IllegalStateException(message);
			}
			
			// `mInitialized` 是一个 `Map<Class<?>, Object>`，它缓存了已经执行过初始化的
			// `Initializer`的 `Class` 对象和初始化的结果，通过这种方法来避免重复初始化。
			Object result;
			if (!mInitialized.containsKey(component)) {
				
				// 这是这个初始化器还没被初始化的情况。
				initializing.add(component);
				try {
					
					// 首先构造一个该初始化器的实例
					Object instance = component.getDeclaredConstructor().newInstance();
					Initializer<?> initializer = (Initializer<?>) instance;
					
					// 读取它的依赖关系，如果有依赖的初始化器，要先对他们做初始化。
					List<Class<? extends Initializer<?>>> dependencies =
							initializer.dependencies();
					if (!dependencies.isEmpty()) {
						for (Class<? extends Initializer<?>> clazz : dependencies) {
							if (!mInitialized.containsKey(clazz)) {
								doInitialize(clazz, initializing);
							}
						}
					}
					
					if (StartupLogger.DEBUG) {
						StartupLogger.i(String.format("Initializing %s", component.getName()));
					}
					
					// 调用初始化器的 `create(Context)`方法，执行具体的初始化逻辑。
					result = initializer.create(mContext);
					
					if (StartupLogger.DEBUG) {
						StartupLogger.i(String.format("Initialized %s", component.getName()));
					}
					
					// 最后把这个初始化器标为已初始化并缓存结果。
					initializing.remove(component);
					mInitialized.put(component, result);
				} catch (Throwable throwable) {
					throw new StartupException(throwable);
				}
			} else {
				// 已经初始化过了，就直接从缓存中取走结果即可。
				result = mInitialized.get(component);
			}
			return (T) result;
		} finally {
			Trace.endSection();
		}
	}
}
1234567891011121314151617181920212223242526272829303132333435363738394041424344454647484950515253545556575859606162636465666768697071727374
```

`doInitialize(Class>, Set>)`方法首先会实例化一个初始化器，然后通过`dependencies()`方法找到它依赖的初始化器做递归初始化，这个过程中如果遇到诸如依赖自身、循环依赖等逻辑错误问题将抛出异常。处理完依赖后调用它的`create(Context)`方法执行具体的初始化逻辑。最后初始化完成，将状态和结果缓存，防止多次初始化。

用来做懒加载的`initializeComponent(Class>)`的方法就比较简单了，它直接调用`doInitialize(Class>, Set>)`方法对指定的初始化器做初始化，如下：

```
@NonNull
@SuppressWarnings("unused")
public <T> T initializeComponent(@NonNull Class<? extends Initializer<T>> component) {
	return doInitialize(component, new HashSet<Class<?>>());
}
12345
```

## 5.`App StartUp`利弊

优点：

- 解决了多个sdk初始化导致`Application`文件和`Mainfest`文件需要频繁改动的问题，同时也减少了`Application`文件和`Mainfest`文件的代码量，更方便维护了
- 方便了sdk开发者在内部处理sdk的初始化问题，并且可以和调用者共享一个`ContentProvider`，减少性能损耗。
- 提供了所有sdk使用同一个`ContentProvider`做初始化的能力，并精简了sdk的使用流程。
- 符合面向对象中类的单一职责原则
- 有效解耦，方便协同开发

缺点：

- 会通过反射实例化`Initializer<>`的实现类，在低版本系统中会有一定的性能损耗。
- 必须给`Initializer<>`的实现类提供一个无参构造器，当然也不能算是缺点，如果缺少的话新版的android studio会通过lint检查给出提醒。

![Lint Check](https://imgconvert.csdnimg.cn/aHR0cHM6Ly9naXRlZS5jb20veGlhb2dlZ2VjaGVuL21hcmtkb3duX2ZpZ3VyZXMvcmF3L21hc3Rlci9BbmRyb2lkL2FyY2hfY29tcGVudC9tZF9yZXMvYXBwX3N0YXJ0dXBfbGludF9jaGVjay5wbmc?x-oss-process=image/format,png)

- 导致类文件增多，特别是有大量需要初始化的sdk存在时。
- 版本较低，还没有发行正式版。