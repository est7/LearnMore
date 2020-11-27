 object是Kotlin中的一个重要的关键字，也是Java中没有的。object主要有以下三种使用场景：

- **对象声明**（Object Declaration）
- **伴生对象**（Companion Object）
- **对象表达式**（Object Expression）

下面就一一介绍它们所表示的含义、用法以及注意点，保证你在看完本篇之后就可以完全掌握object关键字的用法。

**1. 对象声明（Object Declaration**）

1) 语法含义：将类的声明和定义该类的单例对象结合在一起（即通过object就实现了单例模式）
2) 基本示例

```kotlin
object RepositoryManager{
    fun method(){
        println("I'm in object declaration")
    }
}
```

  即将class关键字替换为object关键字，来声明一个类，与此同时也声明它的一个对象。只要编写这么多代码，这个类就已经是单例的了。

3) 使用

a. 在Kotlin中：

```kotlin
fun main(args: Array<String>) {
    RepositoryManager.method()
}
```

像在Java中调用静态方法（在kotlin中没有静态方法）一样去调用其中定义的方法，但实际上是使用RepositoryManager类的单例***\*对象\****去调用实例方法。如果对此还不能理解，可以看看下面对在Java中去使用的说明。

b. 在Java中：

```cpp
public class JavaTest {
    public static void main(String[] args) {
        RepositoryManager.INSTANCE.method();
    }
}
```

  换句话说，object declaration的类最终被编译成：一个类拥有一个静态成员来持有对自己的引用，并且这个静态成员的名称为INSTANCE，当然这个INSTANCE是单例的，故这里可以这么去使用。如果用Java代码来声明这个RepositoryManager的话，可以有如下代码：

```cpp
class RepositoryManager{
    private RepositoryManager(){}
    public static final RepositoryManager INSTANCE = new RepositoryManager();

}
```

4) 注意点:

- 尽管和普通类的声明一样，可以包含属性、方法、初始化代码块以及可以继承其他类或者实现某个接口，但是它不能包含构造器（包括主构造器以及次级构造器）
- 它也可以定义在一个类的内部：

```kotlin
 class ObjectOuter {
     object Inner{
         fun method(){
             println("I'm in inner class")
         }
     }
 }
 fun main(args: Array<String>) {
     ObjectOuter.Inner.method()
 }
```

**2. 伴生对象（Companion object）**

在阐述伴生对象之前，首先我们要明确一点：在Kotlin中是没有static关键字的，也就是意味着没有了静态方法和静态成员。那么在kotlin中如果要想表示这种概念，取而代之的是包级别函数（package-level function）和我们这里提到的伴生对象。至于它们之间的区别，不急，我们后面再说。

1) 语法形式：

```kotlin
class A{
    companion object 伴生对象名(可以省略){
        //define method and field here
    }
}
```

2) 基本示例：

```kotlin
class ObjectTest {

    companion object MyObjec{

        val a = 20

        fun method() {
            println("I'm in companion object")
        }
    }
}
```

 

3) 使用：

a. 在Kotlin中：

```kotlin
fun main(args: Array<String>) {
    //方式一
    ObjectTest.MyObject.method()
    println(ObjectTest.MyObject.a)

    //方式二（推荐方式）
    ObjectTest.method()
    println(ObjectTest.a)
}
```

在这里请注意：在定义（定义时如果省略了伴生对象名，那么编译器会为其提供默认的名字Companion）和调用时伴生对象名是可以省略的。而且在方式二中，注意调用形式，是通过类名.方法名()的形式进行的，我们在没有生成ObjectTest类的对象时，调用了定义其内部伴生对象中定义的属性和方法，是不是类似Java中的静态方法的概念。

4) 通过现象看本质

通过javap命令，让我们看看其生成的字节码：

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180312203901905.png)

 

注意红框中，这个MyObject成员变量的类型，是使用$进行连接的，那么说明我们在定义伴生对象的时候，实际上是把它当做内部类来看待的，并且目标类会持有该内部类的一个引用，那么最终调用的方法实际上是定义在这个内部类中的实例方法，不信你还可以javap 这个ObjectTest$MyObject这个内部类。注意它这里并没有外部类的引用，说明是以**静态内部类**的形式存在的。

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180313102935601.png)

5) 还记得我们在前面遗留的问题：同样都可以用来替代Java中的static的概念，那么在伴生对象中定义的方法和包级别函数有什么区别呢？

先来反编译一个包含包级别函数的kt文件（或者说是类）：

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180312204822460.png)

可以看出，一个名叫ObjectTest2.kt文件，实际上最终会生成一个名叫ObjectTest2Kt的类，而在这个kt文件中定义的顶级函数（包级别函数）是作为这个类的静态方法的形态存在的。

那么现在可以回答遗留的问题了：实际上就是平级类（姑且称之）中的静态方法和静态内部类中的方法的区别，因为静态内部类中的方法是可以访问外部类中定义的static方法和成员的，哪怕是private的（包括私有构造器，我们常用的基于静态内部类实现的单例模式就是基于这一点），而平级类中方法是访问不到当前类中静态的private成员的。如果你觉得文字这么描述还不够直观，那么我们来看下面这一张图（盗自Kotlin in action）：

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180314102249748.png)

6) @JvmStatic注解：我们把前面定义的method方法上加上此注解，重新build工程，然后再来反编译ObjectTest和ObjectTest$MyObject这个两个类，看会有什么变化。

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180313102820225.png)

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180313102833903.png)

对于这个静态内部类而言，加与不加@JvmStatic注解其类的结构是没有变化的。但是对于目标类而言，很明显多了一个静态方法，这样我们就不难理解@JvmStatic注解的作用了：将伴生对象类中定义的实例方法和属性，添加到目标类中，并且以静态的形式存在。

7) 对于伴生对象，最后再补充一点：一个类的伴生对象只能有一个。仔细想想也很好理解，伴生对象的名称是可以省略的。如果允许对应多个伴生对象，那么我们在多个伴生对象中都定义了一模一样的函数，在调用时到底是使用哪个伴生对象的方法呢？就会产生歧义，这样就不难理解这条语法规定了。

**3. 对象表达式（Object Expression）**

1) Java的匿名内部类回顾：

在去学习对象表达式之前，我们先来回顾一下Java中的匿名内部类。

```csharp
interface Contents {
    void absMethod();
}
public class Hello {

    public Contents contents() {
        return new Contents() {
           
            @Override
            public void absMethod() {
                System.out.println("method invoked...");
            }
        };
    }

    public static void main(String[] args) {

        Hello hello = new Hello();
        hello.contents().absMethod();    //打印method invoked...
    }
}
```

这个contents()方法返回的是一个匿名内部类的对象，这个匿名内部类实现了Contents接口。这些代码很熟悉，不多说了。现在提出两个局限性问题：

a. 如果在匿名内部类中新添加了一些属性和方法，那么在外界是无法调用的

```csharp
return new Contents() {
            private int i = 1;

            public int value() {
                return i;
            }

            @Override
            public void absMethod() {
                System.out.println("method invoked...");
            }
        };

 public static void main(String[] args) {

        Hello hello = new Hello();
        hello.contents().absMethod();
        hello.contents().value();  //Cannot resolve method 'value()'
    }
```

当你想使用这个value方法时，编译器会报错。也好理解，就是多态的知识，父类型的引用是无法知晓子类添加方法的存在的。

b. 一个匿名内部类肯定是实现了一个接口或者是继承一个类，并且只能是一个，用数学术语说是“有且只有一个”

2) 语法形式：

object [ : 接口1,接口2,类型1, 类型2]{}  //中括号中的可省略

3) 使用示例：

a. 实现一个接口或类

```kotlin
interface AA {
    fun a()
}

fun main(args: Array<String>) {

    val aa = object : AA {
        override fun a() {
            println("a invoked")
        }
    }

    aa.a()
}
```

b. 不实现任何接口和类，并且在匿名内部类中添加方法

```kotlin
fun main(args: Array<String>) {

    val obj = object  {
        fun a() {
            println("a invoked")
        }
    }

    obj.a()  //打印：a invoked
}
```

从这个例子可以看出，前面我们提到的Java匿名内部类的第一个局限的地方在Kotlin中就不存在了，新添加的方法也是可以调用的

c. 实现多个接口和类

```kotlin
fun main(args: Array<String>) {
    val cc = object : AA, BB() {
        override fun a() {

        }

        override fun b() {

        }

    }

    cc.a()
    cc.b()

}
```

从这个例子可以看出，前面我们提到的Java匿名内部类的第二个局限性在kotlin中也不存在

4) 使用注意点：

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180313195045608.png)

这是Kotlin官方文档上的一段话：匿名对象只有定义成局部变量和private成员变量时，才能体现它的真实类型。如果你是将匿名对象作为public函数的返回值或者是public属性时，你只能将它看做是它的父类，当然你不指定任何类型时就当做Any看待。这时，你在匿名对象中添加的属性和方法是不能够被访问的。

再来举个例子帮助大家理解：

```kotlin
class MyTest {

    private val foo = object {
        fun method() {
            println("private")
        }
    }

    val foo2 = object {
        fun method() {
            println("public")
        }
    }

    fun m() = object {
        fun method(){
            println("method")
        }
    }

    fun invoke(){

        val local = object {
            fun method(){
                println("local")
            }
        }

        local.method()  //编译通过
        foo.method()    //编译通过
        foo2.method()   //编译通不过
        m().method()    //编译通不过
    }
}
```

5) 关于在匿名对象中访问同一作用下定义的局部变量的问题：

在Java中，如果在匿名内部类中访问外部定义的局部变量，那么该局部变量必须使用final关键字进行修饰，至于为什么大家可以看我之前的[一篇博文](http://blog.csdn.net/xlh1191860939/article/details/53088806)。而在Kotlin中，这条限制没有了，看下面的例子：

```kotlin
 var a = 1
 val obj = object {
     fun method() {
         a++
     }
 }

 obj.method()
 println(a)    //打印出2
```

再来解释一下：在Java中，实际上在method方法中使用的a实际上是局部变量a的一份拷贝，而不是它本身。而在Kotlin最终也是要编译成字节码供JVM去执行，所以本质上它是不会违背这一点的。那么它是怎么处理的呢？

当你访问的局部变量是val时，那么也是很Java一样，持有的是一份拷贝；而当你是一个可变变量（var）时，它的值是被存储在Ref这个类的实例成员中，Ref变量是final的，而他其中的成员变量是可以改变的。反编译后是可以看到Ref的身影的。

![img](../../../../art/Object%E4%BD%BF%E7%94%A8%E5%9C%BA%E6%99%AF/20180313202544495.png)

这里还有段有点意思的代码，给大家贴出：

```kotlin
fun tryToCountButtonClicks(button: Button): Int{
	
	var clicks = 0

	button.setOnClickListener{
	    clicks++
	}

	return clicks
}
```

button是个按钮，这段代码的本意上是想要统计Button被点击的次数。但是这个函数的返回值始终是0，哪怕你点击再多次。因为你对局部变量clicks值得修改是异步的，而此函数的返回值是在执行时就确定了的，就是你的值还没有被修改，函数已经返回了。如果真的想统计点击次数，可以将clicks定义成类的成员变量。

\4. 对比object declaration、Companion object以及object expression的初始化时机：

a. object declaration：当第一次访问它时才初始化，是一种懒初始化

b. Companion object：当它对应的类被加载后，它才初始化，类似Java中的静态代码块

c. object expression：一旦它被执行，立马初始化

至此，关于Kotlin中的object关键字的使用就介绍完了，希望大家能有所收获~