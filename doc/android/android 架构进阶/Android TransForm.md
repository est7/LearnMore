什么是 Transform API
Android Gradle 工具从 1.5.0-beta1 版本开始，包含了 Transform API，它允许第三方插件在将编译后的类文件转换为 dex 文件之前对 .class 文件进行操作。

Transform 的工作原理
接下来看下它的工作原理:

 ![Transform 工作原理](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE1MzU0Mi5wbmc.png) 

很明显它是一个链式结构，每个 Transform 都是一个 Gradle 的 Task，Android 编译器通过 TaskManager 将每个 Transform 串联起来。
第一个 Transform 接收 javac 编译的结果，以及 jar 包依赖和 resource 资源，这些编译的中间产物在 Transform 链上流动。其中，我们自定义的 Transform 会插入到最前面
Transform API 的使用
Transform API 其实就是继承自 Transform 写一个实现类，我们看下 Transform 需要实现的方法：

 ![Transform 需要实现的方法](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2MDQzOS5wbmc.png) 

简单介绍下这几个方法：

getName()：用于指定 Transform 的名字，对应了该 Transform 所代表的 Task 的名称，例如：

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2MDU1Ni5wbmc.png) 

isIncremental()：方法指明是否支持增量编译。

getInputTypes()：用于指定 Transform 的输入类型，可以作为输入过滤的一种手段。在 TransformManager 中定义了很多类型：

CONTENT_CLASS // 代表 javac 编译成的 class 文件（一般用它）
CONTENT_JARS
CONTENT_RESOURCES // 这里的 resources 单指 java 的资源
CONTENT_NATIVE_LIBS
CONTENT_DEX
CONTENT_DEX_WITH_RESOURCES
DATA_BINDING_BASE_CLASS_LOG_ARTIFACT
getScopes()：用于指定 Transform 的作用域。同样在 TransformManager 中定义了很多类型，常用的是 SCOPE_FULL_PROJECT，即代表所有 Project。

确定了 ContentType 和 Scope 后就确定了该自定义 Transform 需要处理的资源流。
例如，上面提到的常用输入类型（CONTENT_CLASS）和常用作用域（SCOPE_FULL_PROJECT）表示的就是 所有项目中 java 编译成的 class 组成的资源流。

接下来我们来看需要复写的核心方法：

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2NDMzNy5wbmc.png)  

TransformInvocation 接口定义如下：



看下 TransformInput 的接口定义：

所谓 Transform 就是对输入的 class 文件转变成目标字节码文件，TransformInput 就是这些输入文件的抽象。目前它包括两部分：DirectoryInput 集合与 JarInput 集合。

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2NDQ0Ni5wbmc.png) 

TransformOutputProvider 通过调用 getContentLocation 来获取输出目录：

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2NDUxMC5wbmc.png) 

复写完 Transform 的核心方法之后，我们需要通过插件注册它才可使用：



整体看下 CustomTransform 类：

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2NDY1MS5wbmc.png) 

接下来看下读取 class 文件的流程：

 ![img](art/Android%20TransForm.assets/aHR0cHM6Ly9naXRlZS5jb20vbHVsdXpoYW5nL0ltYWdlQ0ROL3Jhdy9tYXN0ZXIvYmxvZy8yMDIwMDYzMDE2NDczNy5wbmc.png) 

Demo 工程
Android 工程：https://github.com/changer0/ASMInjectDemo