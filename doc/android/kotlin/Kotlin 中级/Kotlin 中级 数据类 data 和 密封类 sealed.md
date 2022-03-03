# Kotlin 中级篇（六）:数据类（data）、密封类（sealed）

[![img](../../../../art/Kotlin%20%E4%B8%AD%E7%BA%A7%20%E6%95%B0%E6%8D%AE%E7%B1%BB%20data%20%E5%92%8C%20%E5%AF%86%E5%B0%81%E7%B1%BB%20sealed/0303896f9fb8a26adb986e9f0999532c300x300.jpeg)](https://juejin.cn/user/4309685005226376)

[程序员喵大人 ![lv-3](../../../../art/Kotlin%20%E4%B8%AD%E7%BA%A7%20%E6%95%B0%E6%8D%AE%E7%B1%BB%20data%20%E5%92%8C%20%E5%AF%86%E5%B0%81%E7%B1%BB%20sealed/e108c685147dfe1fb03d4a37257fb417.svg+xml)](https://juejin.cn/user/4309685005226376)

2021年11月29日 16:02 · 阅读 529

已关注

这是我参与11月更文挑战的第21天，活动详情查看：[2021最后一次更文挑战](https://juejin.cn/post/7023643374569816095/)

## 一、数据类

> - **在`Java`中，或者在我们平时的`Android`开发中，为了解析后台人员给我们提供的接口返回的`Json`字符串，我们会根据这个字符串去创建一个`类`或者`实例对象`，在这个类中，只包含了一些我们需要的数据，以及为了处理这些数据而所编写的方法。这样的类，在`Kotlin`中就被称为`数据类`**。

### 1、关键字

> 声明数据类的关键字为：`data`

**1.1、声明格式**

```
data class 类名(var param1 ：数据类型,...){}
复制代码
```

或者

```
data class 类名 可见性修饰符 constructor(var param1 : 数据类型 = 默认值,...)
复制代码
```

说明：

> - `data`为声明`数据类`的关键字，必须书写在`class`关键字之前。
> - 在没有结构体的时候，大括号`{}`可省略。
> - 构造函数中必须存在至少一个参数，并且必须使用`val`或`var`修饰。这一点在下面`数据类特性中`会详细讲解。
> - 参数的默认值可有可无。（若要实例一个无参数的数据类，则就要用到默认值）

例：

```
// 定义一个名为Person的数据类
data class Preson(var name : String,val sex : Int, var age : Int)
复制代码
```

**1.2、约定俗成的规定**

> - 数据类也有其约定俗成的一些规定，这只是为增加代码的阅读性。

即，当构造函数中的参过多时，为了代码的阅读性，一个参数的定义占据一行。

例：

```
data class Person(var param1: String = "param1",
              var param2: String = "param2", 
              var param3 : String,
              var param4 : Long,
              var param5 : Int = 2,
              var param6 : String,
              var param7 : Float = 3.14f,
              var param8 : Int,
              var param9 : String){
    // exp
    .
    .
    .
}
复制代码
```

**1.3、编辑器为我们做的事情**

当我们声明一个`数据类`时，编辑器自动为这个类做了一些事情，不然它怎么又比`Java`简洁呢。它会根据主构造函数中所定义的所有属性自动生成下列方法：

> - 生成`equals()`函数与`hasCode()`函数
> - 生成`toString()`函数，由`类名（参数1 = 值1，参数2 = 值2，....）`构成
> - 由所定义的属性自动生成`component1()、component2()、...、componentN()`函数，其对应于属性的声明顺序。
> - copy()函数。在下面会实例讲解它的作用。

其中，**当这些函数中的任何一个在类体中显式定义或继承自其基类型，则不会生成该函数**

### 2、数据类的特性

`数据类`有着和`Kotlin`其他类不一样的特性。除了含有其他类的一些特性外，还有着其独特的特点。并且也是`数据类`必须满足的条件：

> - 主构造函数需要至少有一个参数
> - 主构造函数的所有参数需要标记为 val 或 var；
> - 数据类不能是抽象、开放、密封或者内部的；
> - 数据类是可以实现接口的，如(序列化接口)，同时也是可以继承其他类的，如继承自一个密封类。

### 3、用实例说明其比`Java`的简洁性

**3.1、数据类的对比**

**Kotlin版：**

```
data class User(val name : String, val pwd : String)
复制代码
```

**Java版：**

```
public class User {
    private String name;
    private String pwd;

    public User(){}

    public User(String name, String pwd) {
        this.name = name;
        this.pwd = pwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + ''' +
                ", pwd='" + pwd + ''' +
                '}';
    }
}
复制代码
```

分析：实现同一个功能，从代码量来说，`Koltin`比`Java`少了很多行代码，比起更简洁。

**3.2、修改数据类属性**

例：修改`User`类的`name`属性

**Kotlin版：**

> - `Koltin`要修改数据类的属性，则使用其独有的`copy()`函数。其作用就是：修改部分属性，但是保持其他不变

```
val mUser = User("kotlin","123456")
println(mUser)
val mNewUser = mUser.copy(name = "new Kotlin")
println(mNewUser)
复制代码
```

输出结果为：

```
User(name=kotlin, pwd=123456)
User(name=new Kotlin, pwd=123456)
复制代码
```

**Java版：**

```
User mUser = new User("Java","123456");
System.out.println(mUser);
mUser.setName("new Java");
System.out.println(mUser);    
复制代码
```

输出结果为：

```
User{name='Java', pwd='123456'}
User{name='new Java', pwd='123456'}
复制代码
```

分析：从上面对两种方式的实现中可以看出，`Kotlin`是使用其独有的`copy()`函数去修改属性值，而`Java`是使用`setXXX()`去修改

### 4、解构声明

> - 在前面讲到，`Kotlin`中定义一个数据类，则系统会默认自动根据参数的个数生成`component1() ... componentN()`函数。其`...,componentN()`函数就是用于解构声明的

```
val mUser = User("kotlin","123456")
val (name,pwd) = mUser
println("name = $name\tpwd = $pwd")
复制代码
```

输出结果为：

```
name = kotlin   pwd = 123456
复制代码
```

### 5、系统标准库中的标准数据类

> - 标准库提供了 Pair 和 Triple。尽管在很多情况下命名数据类是更好的设计选择， 因为它们通过为属性提供有意义的名称使代码更具可读性。
> - 其实这两个类的源码部分不多，故而贴出这个类的源代码来分析分析

**5.1、源码分析**

```
@file:kotlin.jvm.JvmName("TuplesKt")
package kotlin

// 这里去掉了源码中的注释
public data class Pair<out A, out B>(
        public val first: A,
        public val second: B) : Serializable {

    // toString()方法
    public override fun toString(): String = "($first, $second)"
}

// 转换
public infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

// 转换成List集合
public fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)

// 这里去掉了源码中的注释
public data class Triple<out A, out B, out C>(
        public val first: A,
        public val second: B,
        public val third: C ) : Serializable {

    // toString()方法
    public override fun toString(): String = "($first, $second, $third)"
}

// 转换成List集合
public fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)
复制代码
```

分析：从上面的源码可以看出，标准库中提供了两个标准的数据类，`Pair类`以及`Triple类`.其中：

> - 两个类中都实现了`toList()`方法以及`toString()`方法。
> - `to()`方法乃`Pair类`特有，起作用是参数转换
> - `Pair类`需要传递两个参数，`Triple类`需要传递三个参数。

**5.2、用法**

```
val pair = Pair(1,2)        // 实例
val triple = Triple(1,2,3)  // 实例
println("$pair \t $triple") // 打印：即调用了各自的toString()方法
println(pair.toList())      // 转换成List集合
println(triple.toList())    // 转换成List集合
println(pair.to(3))         // Pair类特有: 其作用是把参数Pair类中的第二个参数替换
复制代码
```

输出结果为：

```
(1, 2)   (1, 2, 3)
[1, 2]
[1, 2, 3]
((1, 2), 3)
复制代码
```

## 二、密封类

> 密封类是用来表示受限的`类继承结构`。若还不甚清楚`Kotlin`的类继承，请参见我的上一篇文章[Kotlin——中级篇（四）：继承类详解](https://link.juejin.cn/?target=https%3A%2F%2Flinks.jianshu.com%2Fgo%3Fto%3Dhttps%3A%2F%2Fwww.cnblogs.com%2FJetictors%2Fp%2F8647968.html)。

### 1、什么是受限的类继承结构

> - 所谓受限的类继承结构，即当类中的一个值只能是有限的几种类型，而不能是其他的任何类型。
> - 这种受限的类继承结构从某种意义上讲，它相当于是枚举类的扩展。但是，我们知道`Kotlin`的枚举类中的枚举常量是受限的，因为每一个枚举常量只能存在一个实例。若对`Kotlin`中的枚举类不甚了解的，请参见我的另一篇文章[Kotlin——中级篇（五）：枚举类（Enum）、接口类（Interface）详解](https://juejin.cn/post/7035606307235332103)。
> - 但是其和枚举类不同的地方在于，密封类的一个子类可以有可包含状态的多个实例。
> - 也可以说成，密封类是包含了一组受限的类集合，因为里面的类都是继承自这个密封类的。但是其和其他继承类（`open`）的区别在，密封类可以不被此文件外被继承，有效保护代码。但是，其密封类的子类的扩展是是可以在程序中任何位置的，即可以不再统一文件下。

上面的几点内容是密封类的特点，请详细的看下去，小生会对这几点内容进行详细的分析。

### 2、关键字

> 定义密封类的关键字：`sealed`

**2.1、声明格式**

```
sealed class SealedExpr()
复制代码
```

> 注意：**密封类是不能被实例化的**

即

```
val mSealedExpr = SealedExpr()  // 这段代码是错误的，编译器直接会报错不能编译通过。
复制代码
```

既然`密封类`是不能实例化，那么我们要怎么使用，或者说它的作用是什么呢？请继续往下看

### 3、密封类的作用及其详细用法。

**3.1、作用**

> 用来表示受限的类继承结构。

例：

```
sealed class SealedExpr{
data class Person(val num1 : Int, val num2 : Int) : SealedExpr()

object Add : SealedExpr()   // 单例模式
object Minus : SealedExpr() // 单例模式
}

// 其子类可以定在密封类外部，但是必须在同一文件中 v1.1之前只能定义在密封类内部
object NotANumber : SealedExpr() 
复制代码
```

分析：即所定义的子类都必须继承于密封类，表示一组受限的类

**3.2、和普通继承类的区别**

> - 我们知道普通的继承类使用`open`关键字定义，在项目中的类都可集成至该类。如果你对`Koltin`的继承类还不甚了解。请参见我的另一篇文章[Kotlin——中级篇（四）：继承类详解](https://juejin.cn/post/7034778439827587080)。
> - 而密封类的子类必须是在密封类的内部或必须存在于密封类的同一文件。这一点就是上面提到的有效的代码保护。

**3.3、和枚举类的区别**

> - 枚举类的中的每一个枚举常量都只能存在一个实例。而密封类的子类可以存在多个实例。

例：

```
val mPerson1 = SealedExpr.Person("name1",22)
println(mPerson1)

val mPerson2 = SealedExpr.Person("name2",23)
println(mPerson2)

println(mPerson1.hashCode())
println(mPerson2.hashCode())
复制代码
```

输出结果为：

```
Person(name=name1, age=22)
Person(name=name2, age=23)
-1052833328
-1052833296
复制代码
```

**3.4、其子类的类扩展实例**

> - 在`Kotlin`支持扩展功能，其和`C#`、`Go`语言类似。这一点是`Java`没有的。

为了演示密封类的子类的扩展是可以在项目中的任何位置这个功能，大家可以下载源码。源码链接在文章末尾会为大家奉上。
例：

```
// 其存在于SealedClassDemo.kt文件中

sealed class SealedExpr{
    data class Person(val name : String, val age : Int) : SealedExpr()
    object Add : SealedExpr()
    companion object Minus : SealedExpr()
}

object NotANumber : SealedExpr()

其存在TestSealedDemo.kt文件中

fun  <T>SealedExpr.Add.add(num1 : T, num2 : T) : Int{
    return 100
}

fun main(args: Array<String>) {
    println(SealedExpr.Add.add(1,2))
}
复制代码
```

输出结果为：

```
100
复制代码
```

> 说明：上面的扩展功能没有任何的意义，只是为了给大家展示密封类子类的扩展不局限与密封类同文件这一个功能而已。

**3.5、使用密封类的好处**

> - 有效的保护代码（上面已说明原因）
> - 在使用`when`表达式 的时候，如果能够验证语句覆盖了所有情况，就不需要为该语句再添加一个`else`子句了。

例：

```
sealed class SealedExpr{
    data class Person(val name : String, val age : Int) : SealedExpr()
    object Add : SealedExpr()
    companion object Minus : SealedExpr()
}

object NotANumber : SealedExpr()

fun eval(expr: SealedExpr) = when(expr){
    is SealedExpr.Add -> println("is Add")
    is SealedExpr.Minus -> println("is Minus")
    is SealedExpr.Person -> println(SealedExpr.Person("Koltin",22))
    NotANumber -> Double.NaN
}
复制代码
```

输出结果为：

```
is Minus
复制代码
```

## 三、总结

在实际的项目开发当中，数据类(`data`)类的用处是很多的，因为在开发`APP`时，往往会根据后台开发者所提供的接口返回的`json`而生成一个实体类，现在我们学习了数据类后，就不用再像`Java`一样写那么多代码了，即使是用编辑器提供的方法去自动生成。但是代码量上就能节省我们很多时间，并且也更加简洁。何乐而不为呢!密封类的情况在实际开发中不是很常见的。只有当时特殊的需求会用到的时候，才会使用密封类。当然我们还是要学习的