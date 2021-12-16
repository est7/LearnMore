# 一、Javassist入门

## （一）Javassist是什么

Javassist是可以动态编辑Java字节码的类库。它可以在Java程序运行时定义一个新的类，并加载到JVM中；还可以在JVM加载时修改一个类文件。Javassist使用户不必关心字节码相关的规范也是可以编辑类文件的。

## （二）Javassist核心API

在Javassist中每个需要编辑的class都对应一个CtCLass实例，CtClass的含义是编译时的类（compile time class），这些类会存储在Class Pool中（Class poll是一个存储CtClass对象的容器）。
 CtClass中的CtField和CtMethod分别对应Java中的字段和方法。通过CtClass对象即可对类新增字段和修改方法等操作了。

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/133be14b32c544b69a3843031738a1bd~tplv-k3u1fbpfcp-watermark.awebp)

## （三）简单示例

为了减少演示的复杂度，示例以及之后的操作，都在Maven项目下进行，因为我们可以直接引入依赖就可以达到我们导包的目的，很方便，不用再去下载jar包，然后自己手动导入了。

1、创建一个maven项目

> 如果你使用的是IDEA，可以像我一样；如果是其他工具，可以自行百度，或者按照自己的经验来创建即可。

![image.png](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161702474.webp)

2、创建一个测试类，代码如下：

```java
package com.ssdmbbl.javassist;

import javassist.*;

import java.io.IOException;

/**
 * @author zhenghui
 * @description: Javassist使用演示测试
 * @date 2021/4/6 6:38 下午
 */
public class JavassistTest {
    public static void main(String[] args) throws CannotCompileException, IOException {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("com.ssdmbbl.javassist.Hello");
        ctClass.writeFile("./");
    }
}
复制代码
```

当运行这个代码的时候，可以看到已经在项目的根目录下创建了一个“com.ssdmbbl.javassist”包，在这个包下创建了“Hello.java”的java文件。
 ![image.png](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161702445.webp)

内容如下：

```java
package com.ssdmbbl.javassist;

public class Hello {
    public Hello() {
    }
}
复制代码
```

# 二、Javassist操作字节码示例

回想一下，咱们如果对一个Java正常操作的话，大概存在哪些操作呢？

- 1、咱们会对一个类添加字段；
- 2、咱们会对一个类添加方法；

好像没其他的了吧。其余的就是在方法里写代码了呗。

## （一）新增一个方法

咱们继续套用上面简单示例的代码，在此基础之上进行新增一个方法。

新增方法的名字为"hello1"，传递两个参数分别为int和double类型，并且没有返回值。

```java
package com.ssdmbbl.javassist;

import javassist.*;

import java.io.IOException;
import java.net.URL;

/**
 * @author zhenghui
 * @description: Javassist使用演示测试
 * @date 2021/4/6 6:38 下午
 */
public class JavassistTest2 {


    public static void main(String[] args) throws CannotCompileException, IOException {
		
        //找到本文件的路径，与之保存在一起
        URL resource = JavassistTest2.class.getClassLoader().getResource("");
        String file = resource.getFile();
        System.out.println("文件存储路径："+file);

        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("com.ssdmbbl.javassist.Hello");

        //创建一个类名为"hello"，传递参数的顺序为(int,double)，没有返回值的类
        /*
        CtMethod（...）源代码：
        public CtMethod(CtClass returnType,//这个方法的返回值类型，
                        String mname, //（method name）方法的名字是什么
                        CtClass[] parameters, //方法传入的参数类型是什么
                        CtClass declaring //添加到哪个类中
                        ) {....}
         */
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
		//设置hello方法的权限为public
        ctMethod.setModifiers(Modifier.PUBLIC);
        
        //向ctClass中添加这个方法
        ctClass.addMethod(ctMethod);
        
        //写入本地
        ctClass.writeFile(file);
    }
}
复制代码
```

执行后，就可以查看生成的代码了：

> 可以看到，我们并没有指定参数的名字，也会给生成var1、var2依次类推的名字。

> var1和var2其实class变量表中存放的名字。

```java
package com.ssdmbbl.javassist;

public class Hello {
    public void hello1(int var1, double var2) {
    }

    public Hello() {
    }
}
复制代码
```

可以设置的返回值类型：

```java
public static CtClass booleanType;
public static CtClass charType;
public static CtClass byteType;
public static CtClass shortType;
public static CtClass intType;
public static CtClass longType;
public static CtClass floatType;
public static CtClass doubleType;
public static CtClass voidType;
复制代码
```

## （二）新增一个变量

```java
URL resource = JavassistTest2.class.getClassLoader().getResource("");
String file = resource.getFile();
System.out.println("文件存储路径："+file);

ClassPool cp = ClassPool.getDefault();
CtClass ctClass = cp.makeClass("com.ssdmbbl.javassist.Hello");

//添加一个hello1的方法
CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
ctMethod.setModifiers(Modifier.PUBLIC);
ctClass.addMethod(ctMethod);

//添加一个int类型的，名字为value的变量
CtField ctField = new CtField(CtClass.intType,"value",ctClass);
ctField.setModifiers(Modifier.PRIVATE);
ctClass.addField(ctField);


ctClass.writeFile(file);
复制代码
```

那么执行后的内容就是如下了：

```java
package com.ssdmbbl.javassist;

public class Hello {
    private int value;

    public void hello1(int var1, double var2) {
    }

    public Hello() {
    }
}
复制代码
```

## （三）给变量新增get和set方法

代码修改如下：

```java
   public static void main(String[] args) throws CannotCompileException, IOException {

        URL resource = JavassistTest2.class.getClassLoader().getResource("");
        String file = resource.getFile();
        System.out.println("文件存储路径："+file);

        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("com.ssdmbbl.javassist.Hello");

        //添加一个hello1的方法
        CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
        ctMethod.setModifiers(Modifier.PUBLIC);
        ctClass.addMethod(ctMethod);

        //添加一个int类型的，名字为value的变量
        CtField ctField = new CtField(CtClass.intType,"value",ctClass);
        ctField.setModifiers(Modifier.PRIVATE);
        ctClass.addField(ctField);

        //为value变量添加set方法
        CtMethod setValue = new CtMethod(CtClass.voidType, "setValue", new CtClass[]{CtClass.intType}, ctClass);
        setValue.setModifiers(Modifier.PUBLIC);
        ctClass.addMethod(setValue);

        //为value变量添加get方法
        CtMethod getValue = new CtMethod(CtClass.intType, "getValue", new CtClass[]{}, ctClass);
        getValue.setModifiers(Modifier.PUBLIC);
        ctClass.addMethod(getValue);


        ctClass.writeFile(file);
    }
复制代码
```

执行效果：

```java
package com.ssdmbbl.javassist;

public class Hello {
    private int value;

    public void hello1(int var1, double var2) {
    }

    public void setValue(int var1) {
    }

    public int getValue() {
    }

    public Hello() {
    }
}
复制代码
```

## （四）给方法内部添加代码

> 你是不是很好奇，set和get方法内部并没有代码，当程序运行的时候，肯定会出错的。
>  下面就来看一下。

我们预想的结果：

```java
    private int value;

    public void setValue(int var1) {
        this.value = var1
    }

    public int getValue() {
        return this.value;
    }
复制代码
```

修改如下：

```java
//为value变量添加set方法
CtMethod setValue = new CtMethod(CtClass.voidType, "setValue", new CtClass[]{CtClass.intType}, ctClass);
setValue.setModifiers(Modifier.PUBLIC);
//设置方法体
setValue.setBody("this.value = var1;");

ctClass.addMethod(setValue);

//为value变量添加get方法
CtMethod getValue = new CtMethod(CtClass.intType, "getValue", new CtClass[]{}, ctClass);
getValue.setModifiers(Modifier.PUBLIC);
//设置方法体
getValue.setBody("return this.value;");
ctClass.addMethod(getValue);
复制代码
```

很倒霉，说找不到这个var1这个变量

> 报错的堆栈信息如下：

```java
Exception in thread "main" javassist.CannotCompileException: [source error] no such field: var1
	at javassist.CtBehavior.setBody(CtBehavior.java:474)
	at javassist.CtBehavior.setBody(CtBehavior.java:440)
	at com.ssdmbbl.javassist.JavassistTest2.main(JavassistTest2.java:41)
Caused by: compile error: no such field: var1
复制代码
```

这个原因我们前面其实提到了，因为在编译的时候，会把变量名抹掉，传递的参数会依次在局部变量表中的顺序。

如果传递：

```java
public void test(int a,int b,int c){
    ...
}
复制代码
```

那么a,b,c就对应本地变量表中的1,2,3的位置。

```java
 —— ——
｜a｜1｜
｜b｜2｜
｜c｜3｜
 —— ——
复制代码
```

> 那么我们获取变量时就不能使用原始的名字了，在Javassist中访问方法中的参数使用的是$1, 2 , 2, 2,…，而不是直接使用原始的名字。

我们修改如下：

```java
//为value变量添加set方法
CtMethod setValue = new CtMethod(CtClass.voidType, "setValue", new CtClass[]{CtClass.intType}, ctClass);
setValue.setModifiers(Modifier.PUBLIC);
//设置方法体
setValue.setBody("this.value = $1;");

ctClass.addMethod(setValue);

//为value变量添加get方法
CtMethod getValue = new CtMethod(CtClass.intType, "getValue", new CtClass[]{}, ctClass);
getValue.setModifiers(Modifier.PUBLIC);
//设置方法体
getValue.setBody("return this.value;");
ctClass.addMethod(getValue);
复制代码
```

结果成功了：

```java
public class Hello {
    private int value;

    public void hello1(int var1, double var2) {
    }

    public void setValue(int var1) {
        this.value = var1;
    }

    public int getValue() {
        return this.value;
    }

    public Hello() {
    }
}
复制代码
```

再来一个：修改hello1方法体，使传递的两个参数相加然后赋值给value；

```java
//添加一个hello1的方法
CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
ctMethod.setModifiers(Modifier.PUBLIC);
ctMethod.setBody("this.value = $1 + $2;");
ctClass.addMethod(ctMethod);
复制代码
```

执行结果如下：

> 因为我们value是int，$1是int，$2是double，所以做了强制转型处理。

```java
public void hello1(int var1, double var2) {
    this.value = (int)((double)var1 + var2);
}
复制代码
```

## （五）在方法体的前后分别插入代码

测试代码如下：

```java
//添加一个hello1的方法
CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
ctMethod.setModifiers(Modifier.PUBLIC);
ctMethod.setBody("this.value = $1 + $2;");
ctMethod.insertBefore("System.out.println(\"我在前面插入了：\"+$1);");
ctMethod.insertAfter("System.out.println(\"我在最后插入了：\"+$1);");
ctClass.addMethod(ctMethod);
复制代码
```

结果如下：

```java
public void hello1(int var1, double var2) {
    System.out.println("我在前面插入了：" + var1);
    this.value = (int)((double)var1 + var2);
    Object var5 = null;
    System.out.println("我在最后插入了：" + var1);
}
复制代码
```

# 三、Javassist中的一些特殊参数示例讲解

在[www.javassist.org/tutorial/tu…](https://link.juejin.cn?target=http%3A%2F%2Fwww.javassist.org%2Ftutorial%2Ftutorial2.html)官方文档中看到有几个比较特殊的标识符，还有几个比较特殊的标识符需要了解。

| 标识符                         | 作用                                        |
| ------------------------------ | ------------------------------------------- |
| 0、0、0、1、$2、 3 、 3、 3、… | this和方法参数（1-N是方法参数的顺序）       |
| $args                          | 方法参数数组，类型为Object[]                |
| $$                             | 所有方法参数，例如：m($$)相当于m(1,1,1,2,…) |
| $cflow(…)                      | control flow 变量                           |
| $r                             | 返回结果的类型，在强制转换表达式中使用。    |
| $w                             | 包装器类型，在强制转换表达式中使用。        |
| $_                             | 返回的结果值                                |
| $sig                           | 类型为java.lang.Class的参数类型对象数组     |
| $type                          | 类型为java.lang.Class的返回值类型           |
| $class                         | 类型为java.lang.Class的正在修改的类         |

下面咱们来一起分别来看一下，分析一下，怎么用，有什么用吧。

> 只介绍几个常见和常用到的吧。
>  有兴趣的话，剩下的可以看官方文档：[www.javassist.org/tutorial/tu…](https://link.juejin.cn?target=http%3A%2F%2Fwww.javassist.org%2Ftutorial%2Ftutorial2.html)

## （一）$0,$1,$2,…

这个其实咱们已经在上面用到过了，再来细说一下吧。$0代表this，$1, 2 , 2, 2,…，依次对应方法中参数的顺序。
 如果有：

```java
public void test(int a,int b,int c){}
复制代码
```

那么如果想引用a和b和c的话，需要这样：

```java
ctMethod.setBody("return $1 + $2 + $3;");
复制代码
```

对了还有：静态方法是没有$0的，所以静态方法下$0是不可用的。

## （二）$args

$args变量表示所有参数的数组，它是一个Object类型的数组（new Object[]{…}），如果参数中有原始类型的参数，会被转换成对应的包装类型。比如原始数据类型为int，则会被转换成java.lang.Integer，然后存储在args中。

例如我们测试代码如下：

```java
CtMethod ctMethod = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
ctMethod.setModifiers(Modifier.PUBLIC);
ctMethod.setBody("System.out.println($args);");
复制代码
```

编译后的结果如下：

```java
public void hello1(int var1, double var2) {
    System.out.println(new Object[]{new Integer(var1), new Double(var2)});
}
复制代码
```

## （三）$$

变量 是 所 有 参 数 的 缩 写 ， 参 数 用 逗 号 分 割 ， 例 如 ： m ( 是所有参数的缩写，参数用逗号分割，例如：m( 是所有参数的缩写，参数用逗号分割，例如：m()相当于：m($1,$2,$3,…)。

如何使用呢？

> 我们经常做一些代码优化，把较为复杂的方法内部的逻辑，提炼出来，提炼到一个公共的方法里。

> 或者说一个方法调用另一个方法，这两个方法传递的参数是一样的时候就用到了。

例如原java：

```java
public Object m1(String name,String age){
	...省略10000行代码逻辑
}
复制代码
```

提炼后的：

> 提炼出来的代码，我们也可以在其他地方使用（所谓的公共的代码）。

```java
public Object m1(String name,String age){
    ...省略20行代码逻辑
    return m2(name,age);
}

private Object m2(String name,String age){
	...
}
复制代码
```

那么我们就造一个这个场景来说明一下用法吧。

> 简单点来说，就是一个方法调用另一个方法，传递全参数时。

```java
CtMethod hello2 = new CtMethod(CtClass.voidType, "hello2", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
hello2.setModifiers(Modifier.PUBLIC);
hello2.setBody("this.value = $1 + $2;");
ctClass.addMethod(hello2);

//添加一个hello1的方法
CtMethod hello1 = new CtMethod(CtClass.voidType, "hello1", new CtClass[]{CtClass.intType, CtClass.doubleType}, ctClass);
hello1.setModifiers(Modifier.PUBLIC);
hello1.setBody("this.value = $1 + $2;");
hello1.insertAfter("hello2($$);");
复制代码
```

> 可以看到我们hello1调用hello2时，需要传递全部参数。此时即可写成$$方法，当然了也可以写成hello2($1,$2)。

编译后的结果：

```java
public void hello2(int var1, double var2) {
    this.value = (int)((double)var1 + var2);
}

public void hello1(int var1, double var2) {
    this.value = (int)((double)var1 + var2);
    Object var5 = null;
    this.hello2(var1, var2);
}
复制代码
```

## （四）$cflow(…)

$cflow 的全名为：control flow，这是一个只读变量，返回指定方法递归调用的深度。

我们以计算n的斐波拉契数列为例，来演示一下如何使用。

我们正确的递归算法代码如下：

```java
public int f(int n){
	if(n <= 1){
    	return n;
    }
    return f(n-1) + f(n - 2)
 }
复制代码
```

对于上面这段代码，我们可以这样写：

```java
CtMethod f = new CtMethod(CtClass.intType,"f", new CtClass[]{CtClass.intType}, ctClass);
f.setBody("{if($1 <= 1){" +
          "     return $1;" +
          "}" +
          "return f($1 - 1) + f( $1 - 2);}");
f.setModifiers(Modifier.PUBLIC);
复制代码
```

编译后的：

```java
public int f(int var1) {
    return var1 <= 1 ? var1 : this.f(var1 - 1) + this.f(var1 - 2);
}
复制代码
```

那么我们只想在递归到前n次的时候打印log，我们该怎么做呢。

> 例如，我们下面写的是"$cflow(f) == 1"时

```java
CtMethod f = new CtMethod(CtClass.intType,"f", new CtClass[]{CtClass.intType}, ctClass);
f.setBody("{if($1 <= 1){" +
          "     return $1;" +
          "}" +
          "return f($1 - 1) + f( $1 - 2);}");
f.setModifiers(Modifier.PUBLIC);

//在代码body的前面插入
f.useCflow("f");
f.insertBefore("if($cflow(f) == 1){" +
               "               System.out.println(\"我执行了，此时的n是：\"+$1);" +
               "           }");
复制代码
```

编译后的代码：

```java
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ssdmbbl.javassist;

import javassist.runtime.Cflow;

public class Hello {
    public static Cflow _cflow$0 = new Cflow();

    public int f(int var1) {
        if (_cflow$0.value() == 1) {
            System.out.println("我执行到来第2次，此时的n是：" + var1);
        }

        boolean var6 = false;

        int var10000;
        try {
            var6 = true;
            _cflow$0.enter();
            if (var1 <= 1) {
                var10000 = var1;
                var6 = false;
            } else {
                var10000 = this.f(var1 - 1) + this.f(var1 - 2);
                var6 = false;
            }
        } finally {
            if (var6) {
                boolean var3 = false;
                _cflow$0.exit();
            }
        }

        int var8 = var10000;
        _cflow$0.exit();
        return var8;
    }

    public Hello() {
    }
}
复制代码
```

通过查看源码，发先关键的一个地方是调用了Cflow对象的enter方法：

```java
public static Cflow _cflow$0 = new Cflow();

...
    
    
_cflow$0.enter();
复制代码
```

点进enter()的内部发现，调用了get().inc()方法：

```java
public class Cflow extends ThreadLocal<Cflow.Depth> {
    protected static class Depth {
        private int depth;
        Depth() { depth = 0; }
        int value() { return depth; }
        void inc() { ++depth; }
        void dec() { --depth; }
    }

    @Override
    protected synchronized Depth initialValue() {
        return new Depth();
    }

    /**
     * Increments the counter.
     */
    public void enter() { get().inc(); }

    /**
     * Decrements the counter.
     */
    public void exit() { get().dec(); }

    /**
     * Returns the value of the counter.
     */
    public int value() { return get().value(); }
}
复制代码
```

而inc()方法控制着一个全局变量的增加操作。

```java
void inc() { ++depth; }
复制代码
```

boolean var6 = false;相当于是一个开关，控制着是否开始和结束。

当参数var1<=1时，即结束

```java
if (var1 <= 1) {
    var10000 = var1;
    var6 = false;
}
复制代码
```

我们可以使用反射来验证一下，测试代码如下：

```java
package com.ssdmbbl.javassist;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zhenghui
 * @description:
 * @date 2021/4/8 10:20 上午
 */
public class Test {

    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        Class<?> aClass = Class.forName("com.ssdmbbl.javassist.Hello");

        //初始化这个类
        Object obj = aClass.newInstance();

        //获取所有的方法
        Method[] methods = aClass.getMethods();
        //遍历所有的方法
        for (Method method : methods) {
            //当方法为f的时候，进行调用
            if(method.getName().equals("f")){
                //调用并传递参数为5，即f(5)
                method.invoke(obj,5);
            }
        }
    }
}
复制代码
```

执行的结果：

> 为什么是2次呢？原因上面也说了：if(var1 <= 1){…}，所以是

```java
我执行了，此时的n是：4
我执行了，此时的n是：3
```