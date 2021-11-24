AOP
全称“Aspect Oriented Programming”,面向切面编程，由于面向对象的思想要求高内聚，低耦合的风格，使模块代码间的可见性变差，对于埋点，日志输出等需求，就会变的十分复杂，如果手动编写代码，入侵性很大，不利于扩展，AOP应运而生。

AspectJ
AspectJ实际上是对AOP编程的实践，目前还有很多的AOP实现，如ASMDex，但笔者选用的是AspectJ。

### 使用场景

当我们需要在某个方法运行前和运行后做一些处理时，便可使用AOP技术。具体有：

- 统计埋点
- 日志打印/打点
- 数据校验
- 行为拦截
- 性能监控
- 动态权限控制

AOP（aspect-oriented programming），指的是面向切面编程。而AspectJ是实现AOP的其中一款框架，内部通过处理字节码实现代码注入。

AspectJ从2001年发展至今，已经非常**成熟稳定**，同时**使用简单**是它的一大优点。至于它的使用场景，可以看本文中的一些小例子，获取能给你启发。

### 1.集成AspectJ

使用插件gradle-android-aspectj-plugin

这种方式接入简单。但是此插件截止目前已经一年多没有维护了，考虑到AGP的兼容性，害怕以后无法使用。这里就不推荐了。（这里存在特殊情况，文章后面会提到。）

常规的Gradle 配置方式

这种方法相对配置会多一些，但相对可控。

首先在项目根目录的build.gradle中添加：

```
classpath "com.android.tools.build:gradle:4.2.1"
classpath 'org.aspectj:aspectjtools:1.9.6'
```

然后在app的build.gradle中添加：

```
dependencies {
    ...
    implementation 'org.aspectj:aspectjrt:1.9.6'
}

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

final def log = project.logger
final def variants = project.android.applicationVariants

variants.all { variant ->
	// 注意这里控制debug下生效，可以自行控制是否生效
    if (!variant.buildType.isDebuggable()) {
        log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
        return
    }

    JavaCompile javaCompile = variant.javaCompileProvider.get()
    javaCompile.doLast {
        String[] args = ["-showWeaveInfo",
                         "-1.8",
                         "-inpath", javaCompile.destinationDir.toString(),
                         "-aspectpath", javaCompile.classpath.asPath,
                         "-d", javaCompile.destinationDir.toString(),
                         "-classpath", javaCompile.classpath.asPath,
                         "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]
        log.debug "ajc args: " + Arrays.toString(args)

        MessageHandler handler = new MessageHandler(true)
        new Main().run(args, handler)
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    break
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break
            }
        }
    }
}
```

在 module 使用的话一样需要添加配置代码（略有不同）：

```
dependencies {
	...
    implementation 'org.aspectj:aspectjrt:1.9.6'

}

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

final def log = project.logger

android.libraryVariants.all{ variant ->
    if (!variant.buildType.isDebuggable()) {
        log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
        return
    }

    JavaCompile javaCompile = variant.javaCompileProvider.get()
    javaCompile.doLast {
        String[] args = ["-showWeaveInfo",
                         "-1.8",
                         "-inpath", javaCompile.destinationDir.toString(),
                         "-aspectpath", javaCompile.classpath.asPath,
                         "-d", javaCompile.destinationDir.toString(),
                         "-classpath", javaCompile.classpath.asPath,
                         "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]
        
        log.debug "ajc args: " + Arrays.toString(args)

        MessageHandler handler = new MessageHandler(true)
        new Main().run(args, handler)
        for (IMessage message : handler.getMessages(null, true)) {
            switch (message.getKind()) {
                case IMessage.ABORT:
                case IMessage.ERROR:
                case IMessage.FAIL:
                    log.error message.message, message.thrown
                    break
                case IMessage.WARNING:
                    log.warn message.message, message.thrown
                    break
                case IMessage.INFO:
                    log.info message.message, message.thrown
                    break
                case IMessage.DEBUG:
                    log.debug message.message, message.thrown
                    break
            }
        }
    }
}
```

### 2.AspectJ基础语法

Join Points

连接点，用来连接我们需要操作的位置。比如连接普通方法、构造方法还是静态初始化块等位置，以及是调用方法外部还是调用方法内部。常用类型有`Method call`、`Method execution`、`Constructor call`、`Constructor execution`等。

Pointcuts

切入点，是带条件的Join Points，确定切入点位置。

| Pointcuts语法                     | 说明            |
| :-------------------------------- | :-------------- |
| execution(MethodPattern)          | 方法执行        |
| call(MethodPattern)               | 方法被调用      |
| execution(ConstructorPattern)     | 构造方法执行    |
| call(ConstructorPattern)          | 构造方法被调用  |
| get(FieldPattern)                 | 读取属性        |
| set(FieldPattern)                 | 设置属性        |
| staticinitialization(TypePattern) | static 块初始化 |
| handler(TypePattern)              | 异常处理        |

execution和call的区别如下图：

![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706695.jpeg)

Pattern规则如下：

| Pattern            | 规则（注意空格）                                             |
| :----------------- | :----------------------------------------------------------- |
| MethodPattern      | [@注解] [访问权限] 返回值类型 [类名.]方法名(参数) [throws 异常类型] |
| ConstructorPattern | [@注解] [访问权限] [类名.]new(参数) [throws 异常类型]        |
| FieldPattern       | [@注解] [访问权限] 变量类型 [类名.]变量名                    |
| TypePattern        | `*` 单独使用事表示匹配任意类型，`..` 匹配任意字符串，`..` 单独使用时表示匹配任意长度任意类型，`+` 匹配其自身及子类，还有一个 `...` 表示不定个数。也可以使用`&&，||，！`进行逻辑运算。 |

- 上表中中括号为可选项，没有可以不写
- 方法匹配例子：

```
 java.*.Date：可以表示java.sql.Date，也可以表示java.util.Date  
Test*：可以表示TestBase，也可以表示TestDervied  
 java..*：表示java任意子类
java..*Model+：表示Java任意package中名字以Model结尾的子类，比如TabelModel，TreeModel 等
```

参数匹配例子：

```
(int, char)：表示参数只有两个，并且第一个参数类型是int，第二个参数类型是char 
 (String, ..)：表示至少有一个参数。并且第一个参数类型是String，后面参数类型不限.
..代表任意参数个数和类型  
 (Object ...)：表示不定个数的参数，且类型都是Object，这里的...不是通配符，而是Java中代表不定参数的意思
```

Advice

用来指定代码插入到Pointcuts的什么位置。

| Advice          | 说明                                                         |
| :-------------- | :----------------------------------------------------------- |
| @Before         | 在执行JPoint之前                                             |
| @After          | 在执行JPoint之后                                             |
| @AfterReturning | 方法执行后，返回结果后再执行。                               |
| @AfterThrowing  | 处理未处理的异常。                                           |
| @Around         | 可以替换原代码。如果需要执行原代码，可以使用ProceedingJoinPoint#proceed()。 |

After、Before 示例

这里我们实现一个功能，在所有Activity的onCreate方法中添加Trace方法，来统计onCreate方法耗时。

```
@Aspect // <-注意添加，才会生效参与编译
public class TraceTagAspectj {

    @Before("execution(* android.app.Activity+.onCreate(..))")
    public void before(JoinPoint joinPoint) {
        Trace.beginSection(joinPoint.getSignature().toString());
    }

    @After("execution(* android.app.Activity+.onCreate(..))")
    public void after() {
        Trace.endSection();
    }
}
```

编译后的class代码如下：

> 可以看到经过处理后，它并不会直接把 Trace 函数直接插入到代码中，而是经过一系列自己的封装。如果想针对所有的函数都做插桩，AspectJ 会带来不少的性能影响。
> 不过大部分情况，我们可能只会插桩某一小部分函数，这样 AspectJ 带来的性能影响就可以忽略不计了。

AfterReturning示例

获取切点的返回值，比如这里我们获取TextView，打印它的text值。

```
private TextView testAfterReturning() {
    return findViewById(R.id.tv);
}
@Aspect
public class TextViewAspectj {

    @AfterReturning(pointcut = "execution(* *..*.testAfterReturning())", returning = "textView") // "textView"必须和下面参数名称一样
    public void getTextView(TextView textView) {
        Log.d("weilu", "text--->" + textView.getText().toString());
    }
}
```

编译后的class代码如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706413.jpeg)
log打印：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706364.jpeg)
使用`@AfterReturning`你可以对方法的返回结果做一些修改（注意是“=”赋值，String无法通过此方法修改）。

AfterThrowing示例

当方法执行出现异常，且异常没有处理时，可以使用`@AfterThrowing`。比如下面的例子中，我们捕获异常并上报（这里用log输出实现）

```
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testAfterThrowing();
    }

    private void testAfterThrowing() {
        TextView textView = null;
        textView.setText("aspectj");
    }
}
@Aspect
public class ReportExceptionAspectj {

    @AfterThrowing(pointcut = "call(* *..*.testAfterThrowing())", throwing = "throwable")  // "throwable"必须和下面参数名称一样
    public void reportException(Throwable throwable) {
        Log.e("weilu", "throwable--->" + throwable);
    }
}
```

编译后的class代码如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706468.jpeg)
log打印：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706987.jpeg)
这里要注意的是，程序最终还是会崩溃，因为最后执行了`throw var3`。如果你想不崩溃，可以使用@Around。

Around示例

接着上面的例子，我们这次直接try catch住异常代码：

```
@Aspect
public class TryCatchAspectj {
    
    @Pointcut("execution(* *..*.testAround())")
    public void methodTryCatch() {
    }

    @Around("methodTryCatch()")
    public void aroundTryJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
       
         try {
             joinPoint.proceed(); // <- 调用原代码
         } catch (Exception e) {
              e.printStackTrace();
         }
    }
}
```

编译后的class代码如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241707036.jpeg)
`@Around` 明显更加灵活，我们可以自定义，实现"偷梁换柱"的效果，比如上面提到的替换方法的返回值。

### 3.进阶

withincode

`withincode`表示某个方法执行过程中涉及到的JPoint，通常用来过滤切点。例如我们有一个Person对象：

```
public class Person {

    private String name;
    private int age;

    public Person() {
        this.name = "weilu";
        this.age = 18;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
```

`Person`对象中有两处set age的地方，如果我们只想让构造方法的生效，让`setAge`方法失效，可以使用`@Around("execution(* com.weilu.aspectj.demo.Person.setAge(..))")`不过如果有更多处set age的地方，我们这样一个个去匹配就很麻烦。

这里就可以考虑使用`set`这个Pointcuts：

```
public class FieldAspectJ {

    @Around("set(int com.weilu.aspectj.demo.Person.age)")
    public void aroundFieldSet(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.e("weilu", "around->" + joinPoint.getTarget().toString() + "#" + joinPoint.getSignature().getName());
    }
}
```

由于`set(FieldPattern)`的FieldPattern限制，不能指定参数，这样会将所有的set age都切入：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706566.jpeg)
这时就可以使用`withincode`添加过滤条件：

```
@Aspect
public class FieldAspectJ {

    @Pointcut("!withincode(com.weilu.aspectj.demo.Person.new())")
    public void invokePerson() {
    }

    @Around("set(int com.weilu.aspectj.demo.Person.age) && invokePerson()")
    public void aroundFieldSet(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.e("weilu", "around->" + joinPoint.getTarget().toString() + "#" + joinPoint.getSignature().getName());
    }
}
```

结果如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241707122.jpeg)

还有一个`within`，它和`withincode`类似。不同的是，它的范围是类，而`withincode`是方法。例如：`within(com.weilu.activity.*)`表示此包下任意的JPoint。

args

用来指定当前执行方法的参数条件。比如上一个例子中，如果需要指定第一个参数是int，后面参数不限。就可以这样写。

```
@Around("execution(* com.weilu.aspectj.withincode.Person.setAge(..)) && args(int,..)")
```

cflow

> cflow是call flow的意思，cflow的条件是一个`pointcut`

举一个例子来说明一下它的用途，a方法中调用了b、c、d方法。此时要统计各个方法的耗时，如果按之前掌握的语法，我们最多需要写四个Pointcut，方法越多越麻烦。

使用cflow，我们可以方便的掌握方法的“调用流”。我们测试方法如下：

```
private void test() {
    testAfterReturning();
    testAround();
    testWithInCode();
}
```

实现如下：

```
@Aspect
public class TimingAspect {

    @Around("execution(* *(..)) && cflow(execution(* com.weilu.aspectj.demo.MainActivity.test(..)))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = currentTimeMillis();
        Log.e("weilu", joinPoint.getSignature().toString() + " -> " + (endTime - startTime) + " ms");
        return result;
    }

}
```

`cflow(execution(* com.weilu.aspectj.demo.MainActivity.test(..)))`表示调用test方法时所包含的JPoint，包括自身JPoint。

`execution(* *(..))`的作用是去除`TimingAspect`自身的代码，避免自己拦截自己，形成死循环。

log结果如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241707208.jpeg)

还有一个`cflowbelow`，它和`cflow`类似。不同的是，它不包括自身JPoint。也就是例子中不会获取test方法的耗时。

4.实战 拦截点击

拦截点击的目的是避免因快速点击控件，导致重复执行点击事件。例如打开多次页面，弹出多次弹框，请求多次接口，我之前发现在部分机型上，很容易复现此类情况。所以避免抖动这算是项目中的一个常见需求。

例如`butterknife`中就自带`DebouncingOnClickListener`来避免此类问题。
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706601.jpeg)
如果你已不在使用`butterknife`，也可以复制这段代码。一个个的替换已有的`View.OnClickListener`。还有以前使用Rxjava操作符来处理防抖。但这些方式侵入式大且替换的工作量也大。

这种场景就可以考虑AOP的方式处理。拦截onClick方法，判断是否可以点击。

```
@Aspect
public class InterceptClickAspectJ {

    // 最后一次点击的时间
    private Long lastTime = 0L;
    // 点击间隔时长
    private static final Long INTERVAL = 300L;

    @Around("execution(* android.view.View.OnClickListener.onClick(..))")
    public void clickIntercept(ProceedingJoinPoint joinPoint) throws Throwable {
        // 大于间隔时间可点击
        if (System.currentTimeMillis() - lastTime >= INTERVAL) {
            // 记录点击时间
            lastTime = System.currentTimeMillis();
            // 执行点击事件
            joinPoint.proceed();
        } else {
            Log.e("weilu", "重复点击");
        }
    }

}
```

实现代码很简单，效果如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706574.jpeg)
考虑到有些view的点击事件不需要防抖，例如checkBox。否则checkBox状态变了，但事件没有执行。我们可以定义一个注解，用`withincode`过滤有此注解的方法。具体需求可以根据实际项目自行拓展，这里仅提供思路。

埋点

前面的例子中都是无侵入的方式使用AspectJ。这里说一下侵入式的方式，简单说就是使用自定义注解，用注解作为切入点的规则。（其实也可以自定义一种方法命名，来当做切入规则）

首先定义两个注解，一个用来传固定参数比如eventName、eventId，同时负责当做切入点，一个用来传动态参数的key。

```
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackEvent {
    /**
     * 事件名称
     */
    String eventName() default "";

    /**
     * 事件id
     */
    String eventId() default "";
}

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackParameter {

    String value() default "";

}
```

Aspectj代码如下：

```
@Aspect
public class TrackEventAspectj {

    @Around("execution(@com.weilu.aspectj.tracking.TrackEvent * *(..))")
    public void trackEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 获取方法上的注解
        TrackEvent trackEvent = signature.getMethod().getAnnotation(TrackEvent.class);

        String eventName = trackEvent.eventName();
        String eventId = trackEvent.eventId();

        JSONObject params = new JSONObject();
        params.put("eventName", eventName);
        params.put("eventId", eventId);

        // 获取方法参数的注解
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        if (parameterAnnotations.length != 0) {
            int i = 0;
            for (Annotation[] parameterAnnotation : parameterAnnotations) {
                for (Annotation annotation : parameterAnnotation) {
                    if (annotation instanceof TrackParameter) {
                        // 获取key value
                        String key = ((TrackParameter) annotation).value();
                        params.put(key, joinPoint.getArgs()[i++]);
                    }
                }
            }
        }

        // 上报
        Log.e("weilu", "上报数据---->" + params.toString());

        try {
            joinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
```

使用方法：

```
 @TrackEvent(eventName = "点击按钮", eventId = "100")
    private void trackMethod(@TrackParameter("uid") int uid, String name) {
        Intent intent = new Intent(this, KotlinActivity.class);
        intent.putExtra("uid", uid);
        intent.putExtra("name", name);
        startActivity(intent);
    }

	trackMethod(10, "weilu");
```

结果如下：
![在Android项目中使用AspectJ的方法](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111241706148.jpeg)
由于匹配key value的代码问题，建议将需要动态传入的参数都写在前面，避免下标越界。

还有一些使用场景，比如权限控制。总结一下，AOP适合将一些通用逻辑分离出来，然后通过AOP将此部分注入到业务代码中。这样我们可以更加注重业务的实现，代码也显得清晰起来。

5.其他问题 lambda

如果我们代码中有使用lambda，例如点击事件会变为：

```
tv.setOnClickListener(v -> Log.e("weilu", "点击事件执行"));
```

这样之前的点击切入点就无效了，这里涉及到D8这个脱糖工具和invokedynamic字节码指令相关知识，这里我也无法说的清楚详细。简单说使用lambda会生成`lambda$`开头的中间方法，所以只能如下处理：

```
@Around("execution(* *..lambda$*(android.view.View))")
```

这种暂时处理起来比较麻烦，且可以看出容错率也比较低，很容易切入其他无关方法，所以建议AOP不要使用lambda。

配置

一开始介绍了两种配置，虽说AspectJX插件最近不太维护了，但是它的支持了AAR、JAR及Kotlin的切入，而默认仅是对自己的代码进行切入。

> 在AspectJ常规配置中有这样的代码："-inpath", javaCompile.destinationDir.toString()，代表只对源文件进行织入。在查看Aspectjx源码时，发现在“-inputs”配置加入了.jar文件，使得class类可以被织入代码。这么理解来看，AspectJ也是支持对class文件的织入的，只是需要对它进行相关的配置，而配置比较繁琐，所以诞生了AspectJx等插件。

例如Kotlin在需要在常规的Gradle 配置上增加如下配置：

```
def buildType = variant.buildType.name

String[] kotlinArgs = [
	"-showWeaveInfo",
    "-1.8",
    "-inpath", project.buildDir.path + "/tmp/kotlin-classes/" + buildType,
    "-aspectpath", javaCompile.classpath.asPath,
    "-d", project.buildDir.path + "/tmp/kotlin-classes/" + buildType,
    "-classpath", javaCompile.classpath.asPath,
    "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)]

MessageHandler handler = new MessageHandler(true)
new Main().run(kotlinArgs, handler)
```

同时注意用kotlin写对应的Aspect类，毕竟你需要注入的是kotlin代码，用java的肯定不行，但是反过来却可行。

建议有AAR、JAR及Kotlin需求的使用插件方式，即使后期无人维护，可自行修改源码适配GAP，相对难度不大。

------

这部分内容较多同时也比较枯燥，断断续续整理了一周的时间。基本介绍了AspectJ在Android 中的配置，以及常用的语法与使用场景。对于应用AspectJ来说够用了。

