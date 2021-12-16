AOP 的优点：不说了

# Aspect 能做什么？

通常来说，AOP都是为一些相对基础且固定的需求服务，实际常见的场景大致包括：

- 统计埋点
- 日志打印/打点
- 数据校验
- 行为拦截
- 性能监控
- 动态权限控制

# Aspect 库 获取：

成熟框架：

https://github.com/HujiangTechnology/gradle_plugin_android_aspectjx

https://github.com/JakeWharton/hugo  较为古老

Maven 仓库：

https://search.maven.org/artifact/org.aspectj/aspectjrt

![image-20211215122047430](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112151220521.png)

依赖：

```groovy
implementation 'org.aspectj:aspectjrt:1.9.7'
implementation 'org.aspectj:aspectjtools:1.9.7'
```

# 基本语义：

- @Aspect 用它声明一个类，表示一个需要执行的切面。
- @Pointcut 声明一个切点。其中Point   连接点
- @Before/@After/@Around/...（统称为Advice类型） 声明在切点前、后、中执行切面代码。

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161627458.webp)



![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161633212.webp)

### Join Point

上面的例子中少讲了一个连接点的概念，连接点表示可织入代码的点，它属于Pointcut的一部分。由于语法内容较多，实际使用过程中我们可以参考[语法手册](https://link.juejin.cn?target=https%3A%2F%2Fgithub.com%2Fhiphonezhu%2FAndroid-Demos%2Fblob%2Fmaster%2FAspectJDemo%2FAspectJ.pdf)，我们列出其中一部分Join Point：

| Joint Point           | 含义            |
| --------------------- | --------------- |
| Method call           | 方法被调用      |
| Method execution      | 方法执行        |
| Constructor call      | 构造函数被调用  |
| Constructor execution | 构造函数执行    |
| Static initialization | static 块初始化 |
| Field get             | 读取属性        |
| Field set             | 写入属性        |
| Handler               | 异常处理        |

### Pointcut

Pointcuts是具体的切入点，基本上Pointcuts 是和 Join Point 相对应的。

| Joint Point           | Pointcuts 表达式                  |
| --------------------- | --------------------------------- |
| Method call           | call(MethodPattern)               |
| Method execution      | execution(MethodPattern)          |
| Constructor call      | call(ConstructorPattern)          |
| Constructor execution | execution(ConstructorPattern)     |
| Static initialization | staticinitialization(TypePattern) |
| Field get             | get(FieldPattern)                 |
| Field set             | set(FieldPattern)                 |
| Handler               | handler(TypePattern)              |

除了上面与 Join Point 对应的选择外，Pointcuts 还有其他选择方法。

| Pointcuts 表达式               | 说明                                                         |
| ------------------------------ | ------------------------------------------------------------ |
| within(TypePattern)            | 符合 TypePattern 的代码中的 Join Point                       |
| withincode(MethodPattern)      | 在某些方法中的 Join Point                                    |
| withincode(ConstructorPattern) | 在某些构造函数中的 Join Point                                |
| cflow(Pointcut)                | Pointcut 选择出的切入点 P 的控制流中的所有 Join Point，包括 P 本身 |
| cflowbelow(Pointcut)           | Pointcut 选择出的切入点 P 的控制流中的所有 Join Point，不包括 P 本身 |
| this(Type or Id)               | Join Point 所属的 this 对象是否 instanceOf Type 或者 Id 的类型 |
| target(Type or Id)             | Join Point 所在的对象（例如 call 或 execution 操作符应用的对象）是否 instanceOf Type 或者 Id 的类型 |
| args(Type or Id, ...)          | 方法或构造函数参数的类型                                     |
| if(BooleanExpression)          | 满足表达式的 Join Point，表达式只能使用静态属性、Pointcuts 或 Advice 暴露的参数、thisJoinPoint 对象 |

# 规则

## @Aspect的pointcut

申明方式主要包含2个部分：

- Pointcut Expression
  - 真正指定Pointcut的地方。
  - 表达式中可以&& || ！这些逻辑运算符
- Pointcut Signature
  - 需要一个定义的方法做为载体，这个方法必须是void类型
  - 如果该方法是public的，那么这个pointcut可以被其他的Aspect引用，如果是private那么只能被当前Aspect类引用。

Aspectj的pointcut表述语言中有很多标志符，但是SpringAOP只能是用少数的几种，因为Spring只对方法级别的pointcut。

- execution
  - 规定格式：`execution(<修饰符模式>?<返回类型模式><方法名模式>(<参数模式>)<异常模式>?) `
  - 只有返回类型，方法名，参数模式是必须的，其他的可以省略。
  - 这里面我们可以使用2种通配符
    - `*` 匹配任意的意思
    - `..`当前包以及子包里面所有的类
- within
  - 只接受类型的声明，会匹配指定类型下面所有的Jointpoint。对SpringAOP来说及，匹配这个类下面所有的方法。
- this和target
  - this指代方法调用方，target指被调用方。
  - this(o1) && this(o2) 即表示当o1类型对象，调用o2类型对象的方法的时候，才会匹配。
- args
  - 指定参数的类型，当调用方法的参数类型匹配就会捕捉到。
- @within
  - 指定某些注解，如果某些类上面有指定的注解，那么这个类里面所有的方法都将被匹配。
- @target
  - 目标类是指定注解的时候，就会被匹配，SpringAOP中和@within没什么区别，只不过@within是静态匹配，@target是运行时动态匹配。
- @args
  - 如果传入的参数的类型 有其指定的注解类型，那么就被匹配。
- @annotation
  - 系统中所有的对象的类方法中，有注解了指定注解的方法，都会被匹配。

这些注解的pointcut在spring内部最终都会转为具体的pointcut对象。

## @AspectJ形式的Advice

主要就是一些Advice的注解：

- @Before
  - 想要获取方法的参数等信息：可以2种方法
    - 第一个参数设置为JoinPoint，这个参数必须要放在第一个位置，并且除了Around Advice和Introduction不可以用它，其他的Advice都可以使用。
    - args标志符绑定（不常用）
- @AfterReturning
  - 有一个独特属性：returning，可以获取到方法返回值。
- @AfterThrowing
  - 有一个独特属性：throwing 可以接受抛出的异常对象。
- @After（也叫finally）
  - 一般做资源的释放工作的
- @Around
  - 它的第一个参数必须是ProceedingJoinPoint类型，且必须指定。通过ProceedingJoinPoint的proceed()方法执行原方法的调用。
  - proceed()方法需要传入参数，则传入一个Object[]数组。
- @DeclareParents
  - 处理Introduction的，不多描述了。

注意点：

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161634985.webp)

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161634857.webp)

# 使用举例：

## @Around

```java
 @Around("execution(* android.view.View.OnClickListener.onClick(..))")
public void fastClickViewAround(ProceedingJoinPoint point) throws Throwable {

        Log.e("AspectJ", "before" );
        long startNanoTime = System.nanoTime();

        Object proceed = point.proceed();
        Log.e("AspectJ", "after" );

        long stopNanoTime = System.nanoTime();
        MethodSignature signature = (MethodSignature) point.getSignature();

        // 方法名
        String name = signature.getName();
        Log.e("AspectJ", "proceed" + name);

        Method method = signature.getMethod();
        Log.e("AspectJ", method.toGenericString());

        // 返回值类型
        Class returnType = signature.getReturnType();
        Log.e("AspectJ", returnType.getSimpleName());

        Class declaringType = signature.getDeclaringType();
        Log.e("AspectJ", declaringType.getSimpleName());

        Class signatureDeclaringType = signature.getDeclaringType();
        Log.e("AspectJ", signatureDeclaringType.getSimpleName());

        Class declaringType1 = signature.getDeclaringType();
        Log.e("AspectJ", declaringType1.getSimpleName());

        Class[] parameterTypes = signature.getParameterTypes();

        for (Class parameterType : parameterTypes) {
            Log.e("AspectJ", parameterType.getSimpleName());
        }

        for (String parameterName : signature.getParameterNames()) {
            Log.e("AspectJ", parameterName);
        }

        Log.e("AspectJ", String.valueOf(stopNanoTime - startNanoTime));
    }

```

```java
@Aspect
public class PointcutCategory {
   /**
    * 匹配任意返回值，任意方法，（任意参数）
    */
   @Around("execution(* *(..))")
   public Object weaveAllMethod(ProceedingJoinPoint joinPoint) throws Throwable{
   }
   
   /**
    * 匹配任意返回值，任意名称，（任意参数)的公共方法
    */
   @Pointcut("execution(public * *(..))")
   public void publicMethodPointcut() {

   }
  
   /**
    * 匹配 com.aspectj.practice 包及其子包中的所有方法
    */
   @Pointcut("within(com.aspectj.practice..*)")
   public void packagePointcut() {

   }
   
   /**
    * 匹配MainActivity类及其子类的所有方法
    */
   @Pointcut("within(com.aspectj.practice.MainActivity+)")
   public void subclassPointcut() {

   }
   
   /**
    * 匹配类 MainActivity 的所有方法
    */
   @Pointcut("within(com.aspectj.practice.SubMainActivity)")
   public void classPointcut() {

   }
   
   /**
    * 匹配所有实现 User 的类的所有方法
    */
   @Pointcut("within(com.aspectj.practice.framework.interfaces.User+)")
   public void interfaceIpm() {

   }
   
   /**
    * 匹配 test 开头，任意返回值的方法
    */
   @Pointcut("execution(* test*())")
   public void withSetPrefixPointcut() {

   }

   /**
    * 匹配 userImp 的所有方法
    */
   @Pointcut("execution(* com.aspectj.practice.userImp.*(..))")
   public void allMethodPointcut() {

   }

   /**
    * 匹配 userImp 的所有私有方法
    */
   @Pointcut("execution(private * com.aspectj.practice.userImp.*(..))")
   public void allPrivateMethodPointcut() {

   }
   
   /**
    * 匹配 userImp 的所有公有方法
    */
   @Pointcut("execution(public * com.aspectj.practice.userImp.*(int, ..))")
   public void allPublicMethodWithIntPointcut() {

   }
   
   /**
    * @within 匹配标注了注解 ClassAnnotation 的类及其子孙的所有方法
    */
   @Pointcut("@within(com.aspectj.annotation.ClassAnnotation)")
   public void withAnnotationClassPointcut() {

   }
   
   /**
    * 匹配使用了注解 MethodAnnotation 的方法
    */
   @Pointcut("@annotation(com.aspectj.annotation.MethodAnnotation)")
   public void withAnnotationMethodPointcut() {

   }

   /**
    * 匹配使用了注解 MethodAnnotation 的方法
    */
   @Pointcut("execution(@com.aspectj.annotation.MethodAnnotation * *(..))")
   public void withAnnotationMethodPointcut2() {

   }
   
   /**
    * 匹配任意实现了接口User的目标对象的方法并且方法使用了注解 MethodAnnotation
    */
   @Pointcut("target(com.aspectj.practice.framework.interfaces.User) && @annotation(com.aspectj.annotation.MethodAnnotation)")
   public void withOperatorPointcut() {

   }
   
   /**
    * 匹配任意实现了接口User的目标对象的方法并且方法名称为 add
    */
   @Pointcut("target(com.aspectj.practice.framework.interfaces.User) && execution(* com.aspectj.practice.framework.interfaces.User.add(..))")
   public void withOperatorAddNamePointcut() {

   }
   
   @Pointcut("call(* com.aspectj.practice.framework.interfaces.User.*(..))")
   public void methodCallPointcut() {
   
   }
   
   /**
    * 匹配 MemberDto 的属性 userName 的赋值
    */
   @Pointcut("set(java.lang.String com.aspectj.practice.dto.MemberDto.userName)")
   public void fieldSetPointCut() {

   }
   
   /**
    * 匹配 MemberDto 的属性 userName的获取
    */
   @Pointcut("get(java.lang.String com.aspectj.practice.dto.MemberDto.userName)")
   public void fieldGetPointCut() {

   }
   
   /**
    * 匹配 MemberDto 构造函数
    */
   @Pointcut("call(com.aspectj.practice.dto.MemberDto.new(..))")
   public void constructorCallPointcut() {

   }
}

```

# 集成：

忒别注意配置 依赖，要不然编译不过，很麻烦

1：Porject 配置：

```java
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven {
            url uri('./repos')
        }
        mavenLocal()
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.2.2'
        classpath 'com.miadapm.plugin:miadapm:1.0.0'
        classpath 'org.aspectj:aspectjtools:1.9.7'//引入Tools工具
    }
}

allprojects {
    repositories {
        maven {
            url uri('./repos')
        }
        mavenLocal()
        google()
        jcenter()
    }
}


task clean(type: Delete) {
    delete rootProject.buildDir
}
```

2:示例使用插件方式，也可以自己集成在项目中

![image-20211216164806021](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161648078.png)

app 配置：

```java
apply plugin:'com.miadapm.plugin' //唯一配置，进配置插件
```

插件配置：

```java
package com.miadapm.plugin;

import com.android.build.gradle.AppExtension
import com.miadapm.plugin.asm.AsmTransform
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

public class MiAdApmPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
//        AsmStart(project)
        AspectJStart(project)
    }


    private void AsmStart(Project project) {
        System.out.println("================ ASM Plugin start =============");
        AppExtension appExtension = (AppExtension) project.getProperties().get("android");
        appExtension.registerTransform(new AsmTransform(), Collections.EMPTY_LIST);
    }

    private void AspectJStart(Project hostProject) {
        System.out.println("================ AspectJ Plugin start =============");
        //代码引用
        hostProject.dependencies {
            //引入工具及 切面规则
            implementation 'org.aspectj:aspectjrt:1.9.7'
            implementation project(path: ':miadaspect')
        }

        //扩展使用
//        project.extensions.create('hugo', HugoExtension)

        final def log = hostProject.logger
        final def variants = hostProject.android.applicationVariants

        variants.all { variant ->
            if (!variant.buildType.isDebuggable()) {
                log.debug("Skipping non-debuggable build type '${variant.buildType.name}'.")
                return
            }
            JavaCompile javaCompile = variant.javaCompile
            javaCompile.doLast {
                String[] args = ["-showWeaveInfo",
                                 "-1.9",
                                 "-inpath", javaCompile.destinationDir.toString(),
                                 "-aspectpath", javaCompile.classpath.asPath,
                                 "-d", javaCompile.destinationDir.toString(),
                                 "-classpath", javaCompile.classpath.asPath,
                                 "-bootclasspath", hostProject.android.bootClasspath.join(File.pathSeparator)]
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
    }
}

```

3：切面配置：

```java
apply plugin: 'com.android.library'
apply plugin: 'maven'

android {
    compileSdkVersion 30
    buildToolsVersion "30.0.0"

    defaultConfig {
        minSdkVersion 14
        targetSdkVersion 30
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles "consumer-rules.pro"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'org.aspectj:aspectjrt:1.9.7'
}

def group='com.miadapm.aspect' //组
def version='1.0.0' //版本
def artifactId='methodtime' //唯一标示


//将插件打包上传到本地maven仓库
uploadArchives {
    repositories {
        mavenDeployer {
            pom.groupId = group
            pom.artifactId = artifactId
            pom.version = version
            //指定本地maven的路径，在项目根目录下
            repository(url: uri('../repos'))
        }
    }
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

    JavaCompile javaCompile = variant.javaCompiler
    javaCompile.doLast {
        String[] args = ["-showWeaveInfo",
                         "-1.9",
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

4：切面规则 举例

方法耗时：activiy Oncreate 耗时

```java
@Aspect
public class AspectAdMethodTime {

    @Pointcut("execution(* com.example.asmtest.MainActivity.onCreate(..))")
    public void callMethod() {
    }

    @Around("callMethod()")
    public void beforeMethodCall(ProceedingJoinPoint joinPoint) {
        Log.e("AspectJ", "before" );
        long startNanoTime = System.nanoTime();
        try {
            Object proceed = joinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        Log.e("AspectJ", "after" );
        long stopNanoTime = System.nanoTime();
        Log.e("AspectJ","method time = "+(stopNanoTime - startNanoTime));
    }
}
```



结果：

app：源文件：

![image-20211216165509126](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161655167.png)



编译后文件：

![image-20211216165452459](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161654519.png)

