## 7.8 注解类

Kotlin 的注解与 Java 的注解完全兼容。

### 7.8.1 声明注解



```kotlin
annotation class 注解名
```

代码示例：



```kotlin
@Target(AnnotationTarget.CLASS,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.EXPRESSION,
        AnnotationTarget.FIELD,
        AnnotationTarget.LOCAL_VARIABLE,
        AnnotationTarget.TYPE,
        AnnotationTarget.TYPEALIAS,
        AnnotationTarget.TYPE_PARAMETER,
        AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Repeatable
annotation class MagicClass


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Repeatable
annotation class MagicFunction


@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
@Repeatable
annotation class MagicConstructor
```

在上面的代码中，我们通过向注解类添加元注解(meta-annotation)的方法来指定其他属性：

- @Target ： 指定这个注解可被用于哪些元素(类, 函数, 属性, 表达式, 等等.);
- @Retention ： 指定这个注解的信息是否被保存到编译后的 class 文件中, 以及在运行时是否可以通过反
   射访问到它；
- @Repeatable：  允许在单个元素上多次使用同一个注解；
- @MustBeDocumented ： 表示这个注解是公开 API 的一部分, 在自动产生的 API 文档的类或者函数签名中, 应该包含这个注解的信息。

这几个注解定义在`kotlin/annotation/Annotations.kt`类中。

### 7.8.2 使用注解

注解可以用在类、函数、参数、变量（成员变量、局部变量）、表达式、类型上等。这个由该注解的元注解@Target定义。



```kotlin
@MagicClass class Foo @MagicConstructor constructor() {

    constructor(index: Int) : this() {
        this.index = index
    }

    @MagicClass var index: Int = 0
    @MagicFunction fun magic(@MagicClass name: String) {

    }
}
```

注解在主构造器上，主构造器必须加上关键字 “constructor”



```kotlin
@MagicClass class Foo @MagicConstructor constructor() {
...
}
```

## 7.9 单例模式(Singleton)与伴生对象(companion object)

## 7.9.1 单例模式(Singleton)

单例模式很常用。它是一种常用的软件设计模式。例如，Spring中的Bean默认就是单例。通过单例模式可以保证系统中一个类只有一个实例。即一个类只有一个对象实例。

我们用Java实现一个简单的单例类的代码如下：



```csharp
class Singleton {
    private static Singleton instance;

    private Singleton() {}

    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}
```

测试代码：



```undefined
Singleton singleton1 = Singleton.getInstance();
```

可以看出，我们先在单例类中声明了一个私有静态的`Singleton instance`变量，然后声明一个私有构造函数`private Singleton() {}`, 这个私有构造函数使得外部无法直接通过new的方式来构建对象：



```cpp
Singleton singleton2 = new Singleton(); //error, cannot private access
```

最后提供一个public的获取当前类的唯一实例的静态方法`getInstance()`。我们这里给出的是一个简单的单例类，是线程不安全的。

### 7.9.2 object对象

Kotlin中没有 **静态属性和方法**，但是也提供了实现**类似于单例的功能**，我们可以使用关键字 `object` 声明一个object对象:



```kotlin
object AdminUser {
    val username: String = "admin"
    val password: String = "admin"
    fun getTimestamp() = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
    fun md5Password() = EncoderByMd5(password + getTimestamp())
}
```

测试代码：



```go
    val adminUser = AdminUser.username
    val adminPassword = AdminUser.md5Password()
    println(adminUser)  // admin
    println(adminPassword)  // g+0yLfaPVYxUf6TMIdXFXw==，这个值具体运行时会变化
```

为了方便在REPL中演示说明，我们再写一个示例代码：



```python
>>> object User {
...     val username: String = "admin"
...     val password: String = "admin"
... }
```

object对象只能通过对象名字来访问：



```ruby
>>> User.username
admin
>>> User.password
admin
```

不能像下面这样使用构造函数：



```tsx
>>> val u = User()
error: expression 'User' of type 'Line130.User' cannot be invoked as a function. The function 'invoke()' is not found
val u = User()
        ^
```

为了更加直观的了解object对象的概念，我们把上面的`object User`的代码反编译成Java代码：



```java
public final class User {
   @NotNull
   private static final String username = "admin";
   @NotNull
   private static final String password = "admin";
   public static final User INSTANCE;

   @NotNull
   public final String getUsername() {
      return username;
   }

   @NotNull
   public final String getPassword() {
      return password;
   }

   private User() {
      INSTANCE = (User)this;
      username = "admin";
      password = "admin";
   }

   static {
      new User();
   }
}
```

从上面的反编译代码，我们可以直观了解Kotlin的object背后的一些原理。

### 7.9.3 嵌套（Nested）object对象

这个object对象还可以放到一个类里面：



```kotlin
class DataProcessor {
    fun process() {
        println("Process Data")
    }


    object FileUtils {
        val userHome = "/Users/jack/"

        fun getFileContent(file: String): String {
            var content = ""
            val f = File(file)
            f.forEachLine { content = content + it + "\n" }
            return content
        }

    }
}
```

测试代码：



```cpp
DataProcessor.FileUtils.userHome // /Users/jack/
DataProcessor.FileUtils.getFileContent("test.data") // 输出文件的内容
```

同样的，我们只能通过类的名称来直接访问object，不能使用对象实例引用。下面的写法是错误的：



```kotlin
val dp = DataProcessor()
dp.FileUtils.userHome // error, Nested object FileUtils cannot access object via reference
```

我们在Java中通常会写一些Utils类，这样的类我们在Kotlin中就可以直接使用object对象：



```kotlin
object HttpUtils {
    val client = OkHttpClient()

    @Throws(Exception::class)
    fun getSync(url: String): String? {
        val request = Request.Builder()
                .url(url)
                .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful()) throw IOException("Unexpected code " + response)

        val responseHeaders = response.headers()
        for (i in 0..responseHeaders.size() - 1) {
            println(responseHeaders.name(i) + ": " + responseHeaders.value(i))
        }
        return response.body()?.string()
    }

    @Throws(Exception::class)
    fun getAsync(url: String) {
        var result: String? = ""

        val request = Request.Builder()
                .url(url)
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException?) {
                e?.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful()) throw IOException("Unexpected code " + response)

                val responseHeaders = response.headers()
                for (i in 0..responseHeaders.size() - 1) {
                    println(responseHeaders.name(i) + ": " + responseHeaders.value(i))
                }
                result = response.body()?.string()
                println(result)

            }
        })
    }
}
```

测试代码：



```kotlin
    val url = "http://www.baidu.com"
    val html1 = HttpUtils.getSync(url) // 同步get
    println("html1=${html1}") 
    HttpUtils.getAsync(url) // 异步get
```

### 7.9.4 匿名object

还有，在代码行内，有时候我们需要的仅仅是一个简单的对象，我们这个时候就可以使用下面的匿名object的方式：



```kotlin
fun distance(x: Double, y: Double): Double {
    val porigin = object {
        var x = 0.0
        var y = 0.0
    }
    return Math.sqrt((x - porigin.x) * (x - porigin.x) + (y - porigin.y) * (y - porigin.y))
}
```

测试代码：



```css
distance(3.0, 4.0)
```

需要注意的是，匿名对象只可以用在本地和私有作用域中声明的类型。代码示例：



```kotlin
class AnonymousObjectType {
    // 私有函数，返回的是匿名object类型
    private fun privateFoo() = object {
        val x: String = "x"
    }

    // 公有函数，返回的类型是 Any
    fun publicFoo() = object {
        val x: String = "x" // 无法访问到
    }

    fun test() {
        val x1 = privateFoo().x        // Works
        //val x2 = publicFoo().x  // ERROR: Unresolved reference 'x'
    }
}


fun main(args: Array<String>) {
    AnonymousObjectType().publicFoo().x // Unresolved reference 'x'
}
```

跟 Java 匿名内部类类似，object对象表达式中的代码可以访问来自包含它的作用域的变量（与 Java 不同的是，这不限于 final 变量）:



```kotlin
fun countCompare() {
    var list = mutableListOf(1, 4, 3, 7, 11, 9, 10, 20)
    var countCompare = 0
    Collections.sort(list, object : Comparator<Int> {
        override fun compare(o1: Int, o2: Int): Int {
            countCompare++
            println("countCompare=$countCompare")
            println(list)
            return o1.compareTo(o2)
        }
    })
}
```

测试代码：



```csharp
countCompare()

countCompare=1
[1, 4, 3, 7, 11, 9, 10, 20]
...
countCompare=17
[1, 3, 4, 7, 9, 10, 11, 20]
```

### 7.9.5 伴生对象(companion object)

Kotlin中还提供了 **伴生对象** ，用`companion object`关键字声明:



```kotlin
class DataProcessor {
    fun process() {
        println("Process Data")
    }


    object FileUtils {
        val userHome = "/Users/jack/"

        fun getFileContent(file: String): String {
            var content = ""
            val f = File(file)
            f.forEachLine { content = content + it + "\n" }
            return content
        }

    }

    companion object StringUtils {
        fun isEmpty(s: String): Boolean {
            return s.isEmpty()
        }
    }

}
```

一个类只能有1个伴生对象。也就是是下面的写法是错误的：



```kotlin
class ClassA {
    companion object Factory {
        fun create(): ClassA = ClassA()
    }

    companion object Factory2 { // error, only 1 companion object is allowed per class
        fun create(): MyClass = MyClass()
    }
}
```

一个类的伴生对象默认引用名是Companion:



```kotlin
class ClassB {
    companion object {
        fun create(): ClassB = ClassB()
        fun get() = "Hi, I am CompanyB"
    }
}
```

我们可以直接像在Java静态类中使用静态方法一样使用一个类的伴生对象的函数，属性(但是在运行时，它们依旧是实体的实例成员)：



```css
    ClassB.Companion.index
    ClassB.Companion.create()
    ClassB.Companion.get()
```

其中， Companion可以省略不写：



```css
    ClassB.index
    ClassB.create()
    ClassB.get()
```

当然，我们也可以指定伴生对象的名称：



```kotlin
class ClassC {
    var index = 0
    fun get(index: Int): Int {
        return 0
    }

    companion object CompanyC {
        fun create(): ClassC = ClassC()
        fun get() = "Hi, I am CompanyC"
    }
}
```

测试代码：



```csharp
    ClassC.index
    ClassC.create()// com.easy.kotli.ClassC@7440e464，具体运行值会变化
    ClassC.get() // Hi, I am CompanyC
    ClassC.CompanyC.index
    ClassC.CompanyC.create()
    ClassC.CompanyC.get()
```

伴生对象的初始化是在相应的类被加载解析时，与 Java 静态初始化器的语义相匹配。

即使伴生对象的成员看起来像其他语言的静态成员，在运行时他们仍然是真实对象的实例成员。而且，还可以实现接口：



```kotlin
interface BeanFactory<T> {
    fun create(): T
}


class MyClass {
    companion object : BeanFactory<MyClass> {
        override fun create(): MyClass {
            println("MyClass Created!")
            return MyClass()
        }
    }
}
```

测试代码：



```bash
    MyClass.create()  // "MyClass Created!"
    MyClass.Companion.create() // "MyClass Created!"
```

另外，如果想使用Java中的静态成员和静态方法的话，我们可以用：

**@JvmField**注解：生成与该属性相同的静态字段
 **@JvmStatic注解**：在单例对象和伴生对象中生成对应的静态方法

## 7.10 sealed 密封类

### 7.10.1 为什么使用密封类

就像我们为什么要用enum类型一样，比如你有一个enum类型 MoneyUnit，定义了元、角、分这些单位。枚举就是为了控制住你所有要的情况是正确的，而不是用硬编码方式写成字符串“元”，“角”，“分”。

同样，sealed的目的类似，一个类之所以设计成sealed，就是为了限制类的继承结构，将一个值限制在有限集中的类型中，而不能有任何其他的类型。

在某种意义上，sealed类是枚举类的扩展：枚举类型的值集合也是受限的，但每个枚举常量只存在一个实例，而密封类的一个子类可以有可包含状态的多个实例。

### 7.10.1  声明密封类

要声明一个密封类，需要在类名前面添加 sealed 修饰符。密封类的所有子类都必须与密封类在同一个文件中声明（在 Kotlin 1.1 之前， 该规则更加严格：子类必须嵌套在密封类声明的内部）：



```kotlin
sealed class Expression

class Unit : Expression()
data class Const(val number: Double) : Expression()
data class Sum(val e1: Expression, val e2: Expression) : Expression()
data class Multiply(val e1: Expression, val e2: Expression) : Expression()
object NaN : Expression()
```

使用密封类的主要场景是在使用 when 表达式的时候，能够验证语句覆盖了所有情况，而无需再添加一个 else 子句：



```kotlin
fun eval(expr: Expression): Double = when (expr) {
    is Unit -> 1.0
    is Const -> expr.number
    is Sum -> eval(expr.e1) + eval(expr.e2)
    is Multiply -> eval(expr.e1) * eval(expr.e2)
    NaN -> Double.NaN
// 不再需要 `else` 子句，因为我们已经覆盖了所有的情况
}
```

测试代码：



```kotlin
fun main(args: Array<String>) {
    val u = eval(Unit())
    val a = eval(Const(1.1))
    val b = eval(Sum(Const(1.0), Const(9.0)))
    val c = eval(Multiply(Const(10.0), Const(10.0)))
    println(u)
    println(a)
    println(b)
    println(c)
}
```

输出：



```css
1.0
1.1
10.0
100.0
```

## 7.11 data 数据类

### 7.11.1 构造函数中的 `val/var` 

在开始讲数据类之前，我们先来看一下几种类声明的写法。

写法一：



```kotlin
class Aook(name: String)
```

这样写，这个name变量是无法被外部访问到的。它对应的反编译之后的Java代码如下：



```java
public final class Aook {
   public Aook(@NotNull String name) {
      Intrinsics.checkParameterIsNotNull(name, "name");
      super();
   }
}
```

写法二：
 要想这个name变量被访问到，我们可以在类体中再声明一个变量，然后把这个构造函数中的参数赋值给它：



```kotlin
class Cook(name: String) {
    val name = name
}
```

测试代码：



```kotlin
    val cook = Cook("Cook")
    cook.name
```

对应的Java实现代码是：



```kotlin
public final class Cook {
   @NotNull
   private final String name;

   @NotNull
   public final String getName() {
      return this.name;
   }

   public Cook(@NotNull String name) {
      Intrinsics.checkParameterIsNotNull(name, "name");
      super();
      this.name = name;
   }
}
```

写法三：



```kotlin
class Dook(val name: String)
class Eook(var name: String)
```

构造函数中带var、val修饰的变量，Kotlin编译器会自动为它们生成getter、setter函数。

上面的写法对应的Java代码就是：



```kotlin
public final class Dook {
   @NotNull
   private final String name;

   @NotNull
   public final String getName() {
      return this.name;
   }

   public Dook(@NotNull String name) {
      Intrinsics.checkParameterIsNotNull(name, "name");
      super();
      this.name = name;
   }
}

public final class Eook {
   @NotNull
   private String name;

   @NotNull
   public final String getName() {
      return this.name;
   }

   public final void setName(@NotNull String var1) {
      Intrinsics.checkParameterIsNotNull(var1, "<set-?>");
      this.name = var1;
   }

   public Eook(@NotNull String name) {
      Intrinsics.checkParameterIsNotNull(name, "name");
      super();
      this.name = name;
   }
}
```

测试代码：



```kotlin
    val dook = Dook("Dook")
    dook.name
    val eook = Eook("Eook")
    eook.name
```

下面我们来学习一下Kotlin中的数据类： `data class` 。

### 7.11.2  领域实体类

我们写Java代码的时候，会经常创建一些只保存数据的类。比如说：

- POJO类：POJO全称是Plain Ordinary Java Object / Pure Old Java Object，中文可以翻译成：普通Java类，具有一部分getter/setter方法的那种类就可以称作POJO。
- DTO类：Data Transfer Object，数据传输对象类，泛指用于展示层与服务层之间的数据传输对象。
- VO类：VO有两种说法,一个是ViewObject,一个是ValueObject。
- PO类：Persisent Object，持久对象。它们是由一组属性和属性的get和set方法组成。PO是在持久层所使用，用来封装原始数据。
- BO类：Business Object，业务对象层，表示应用程序领域内“事物”的所有实体类。
- DO类：Domain Object，领域对象，就是从现实世界中抽象出来的有形或无形的业务实体。

等等。

这些我们统称为领域模型中的实体类。最简单的实体类是POJO类，含有属性及属性对应的set和get方法，实体类常见的方法还有用于输出自身数据的toString方法。

### 7.11.3 数据类`data class`的概念

在 Kotlin 中，也有对应这样的领域实体类的概念，并在语言层面上做了支持，叫做数据类 ：



```kotlin
data class Book(val name: String)
data class Fook(var name: String)
data class User(
        val name: String,
        val gender: String,
        val age: Int
) {
    fun validate(): Boolean {
        return true
    }
}
```

这里的var/val是必须要带上的。因为编译器要把主构造函数中声明的所有属性，自动生成以下函数：



```csharp
equals()/hashCode() 
toString() : 格式是 User(name=Jacky, gender=Male, age=10)
componentN() 函数 : 按声明顺序对应于所有属性component1()、component2() ...
copy() 函数
```

如果我们自定义了这些函数，或者继承父类重写了这些函数，编译器就不会再去生成。

测试代码：



```kotlin
    val book = Book("Book")
    book.name
    book.copy("Book2")

    val jack = User("Jack", "Male", 1)
    jack.name
    jack.gender
    jack.age
    jack.toString()
    jack.validate()


    val olderJack = jack.copy(age = 2)
    val anotherJack = jack.copy(name = "Jacky", age = 10)
```

在一些场景下，我们需要复制一个对象来改变它的部分属性，而其余部分保持不变。 copy() 函数就是为此而生成。例如上面的的 User 类的copy函数的使用：



```go
    val olderJack = jack.copy(age = 2)
    val anotherJack = jack.copy(name = "Jacky", age = 10)
```

### 7.11.4 数据类的限制

数据类有以下的限制要求：

1.主构造函数需要至少有一个参数。下面的写法是错误的：



```kotlin
data class Gook // error, data class must have at least one primary constructor parameter
```

2.主构造函数的所有参数需要标记为 val 或 var；



```kotlin
data class Hook(name: String)// error, data class must have only var/val property
```

跟普通类一样，数据类也可以有次级构造函数：



```kotlin
data class LoginUser(val name: String = "", val password: String = "") : DBase(), IBaseA, IBaseB {

    var isActive = true

    constructor(name: String, password: String, isActive: Boolean) : this(name, password) {
        this.isActive = isActive
    }
    ...
}
```

3.数据类不能是抽象、开放、密封或者内部的。也就是说，下面的写法都是错误的：



```kotlin
abstract data class Iook(val name: String) // modifier abstract is incompatible with data
open data class Jook(val name: String) // modifier abstract is incompatible with data
sealed data class Kook(val name: String)// modifier sealed is incompatible with data
inner data class Look(val name: String)// modifier inner is incompatible with data
```

数据类只能是final的：



```kotlin
final data class Mook(val name: String) // modifier abstract is incompatible with data
```

4.在1.1之前数据类只能实现接口。自 1.1 起，数据类可以扩展其他类。代码示例：



```kotlin
open class DBase
interface IBaseA
interface IBaseB

data class LoginUser(val name: String, val password: String) : DBase(), IBaseA, IBaseB {

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }

    fun validate(): Boolean {
        return true
    }
}
```

测试代码：



```swift
    val loginUser1 = LoginUser("Admin", "admin")
    println(loginUser1.component1())
    println(loginUser1.component2())
    println(loginUser1.name)
    println(loginUser1.password)
    println(loginUser1.toString())
```

输出：



```css
Admin
admin
Admin
admin
com.easy.kotlin.LoginUser@7440e464
```

可以看出，由于我们重写了`override fun toString(): String`, 对应的输出使我们熟悉的类的输出格式。

如果我们不重写这个toString函数，则会默认输出：



```undefined
LoginUser(name=Admin, password=admin)
```

上面的类声明的构造函数，要求我们每次必须初始化name、password的值，如果我们想拥有一个无参的构造函数，我们只要对所有的属性指定默认值即可：



```kotlin
data class LoginUser(val name: String = "", val password: String = "") : DBase(), IBaseA, IBaseB {
...
}
```

这样我们在创建对象的时候，就可以直接使用：



```kotlin
    val loginUser3 = LoginUser()
    loginUser3.name
    loginUser3.password
```

### 7.11.5 数据类的解构

解构相当于 Component 函数的逆向映射：



```kotlin
    val helen = User("Helen", "Female", 15)
    val (name, gender, age) = helen
    println("$name, $gender, $age years of age")
```

输出：

Helen, Female, 15 years of age

### 7.11.6 标准数据类`Pair` 和`Triple` 

标准库中的二元组 Pair类就是一个数据类：



```kotlin
public data class Pair<out A, out B>(
        public val first: A,
        public val second: B) : Serializable {
    public override fun toString(): String = "($first, $second)"
}
```

Kotlin标准库中，对Pair类还增加了转换成List的扩展函数：



```kotlin
public fun <T> Pair<T, T>.toList(): List<T> = listOf(first, second)
```

还有三元组Triple类：



```kotlin
public data class Triple<out A, out B, out C>(
        public val first: A,
        public val second: B,
        public val third: C) : Serializable {
    public override fun toString(): String = "($first, $second, $third)"
}
 fun <T> Triple<T, T, T>.toList(): List<T> = listOf(first, second, third)
```

## 7.12 嵌套类（Nested Class）

## 7.12.1 嵌套类：类中的类

类可以嵌套在其他类中，可以嵌套多层：



```kotlin
class NestedClassesDemo {
    class Outer {
        private val zero: Int = 0
        val one: Int = 1

        class Nested {
            fun getTwo() = 2
            class Nested1 {
                val three = 3
                fun getFour() = 4
            }
        }
    }
}
```

测试代码：



```go
    val one = NestedClassesDemo.Outer().one
    val two = NestedClassesDemo.Outer.Nested().getTwo()
    val three = NestedClassesDemo.Outer.Nested.Nested1().three
    val four = NestedClassesDemo.Outer.Nested.Nested1().getFour()
    println(one)
    println(two)
    println(three)
    println(four)
```

我们可以看出，访问嵌套类的方式是直接使用 `类名.`， 有多少层嵌套，就用多少层类名来访问。

普通的嵌套类，没有持有外部类的引用，所以是无法访问外部类的变量的：



```kotlin
class NestedClassesDemo {
class Outer {
        private val zero: Int = 0
        val one: Int = 1


        class Nested {
            fun getTwo() = 2

            fun accessOuter() = {
                println(zero) // error, cannot access outer class
                println(one)  // error, cannot access outer class
            }
        }
}
}
```

我们在Nested类中，访问不到Outer类中的变量zero，one。
 如果想要访问到，我们只需要在Nested类前面加上`inner`关键字修饰，表明这是一个嵌套的内部类。

### 7.12.2 内部类（Inner Class）

类可以标记为 inner 以便能够访问外部类的成员。内部类会带有一个对外部类的对象的引用：



```kotlin
class NestedClassesDemo {
class Outer {
        private val zero: Int = 0
        val one: Int = 1

        inner class Inner {
            fun accessOuter() = {
                println(zero) // works
                println(one) // works
            }

        }
}
```

测试代码：



```kotlin
val innerClass = NestedClassesDemo.Outer().Inner().accessOuter()
```

我们可以看到，当访问`inner class Inner`的时候，我们使用的是`Outer().Inner()`, 这是持有了Outer的对象引用。跟普通嵌套类直接使用类名访问的方式区分。

### 7.12.3 匿名内部类（Annonymous Inner Class）

匿名内部类，就是没有名字的内部类。既然是内部类，那么它自然也是可以访问外部类的变量的。

我们使用对象表达式创建一个匿名内部类实例：



```kotlin
class NestedClassesDemo {
class AnonymousInnerClassDemo {
            var isRunning = false
            fun doRun() {
                Thread(object : Runnable {
                    override fun run() {
                        isRunning = true
                        println("doRun : i am running, isRunning = $isRunning")
                    }
                }).start()
            }
}
}
```

如果对象是函数式 Java 接口，即具有单个抽象方法的 Java 接口的实例，例如上面的例子中的Runnable接口：



```java
@FunctionalInterface
public interface Runnable {
    public abstract void run();
}
```

我们可以使用lambda表达式创建它，下面的几种写法都是可以的：



```kotlin
            fun doStop() {
                var isRunning = true
                Thread({
                    isRunning = false
                    println("doStop: i am not running, isRunning = $isRunning")
                }).start()
            }

            fun doWait() {
                var isRunning = true

                val wait = Runnable {
                    isRunning = false
                    println("doWait: i am waiting, isRunning = $isRunning")
                }

                Thread(wait).start()
            }

            fun doNotify() {
                var isRunning = true

                val wait = {
                    isRunning = false
                    println("doNotify: i notify, isRunning = $isRunning")
                }

                Thread(wait).start()
            }
```

测试代码：



```css
    NestedClassesDemo.Outer.AnonymousInnerClassDemo().doRun()
    NestedClassesDemo.Outer.AnonymousInnerClassDemo().doStop()
    NestedClassesDemo.Outer.AnonymousInnerClassDemo().doWait()
    NestedClassesDemo.Outer.AnonymousInnerClassDemo().doNotify()
```

输出：

doRun : i am running, isRunning = true
 doStop: i am not running, isRunning = false
 doWait: i am waiting, isRunning = false
 doNotify: i notify, isRunning = false

关于lambda表达式以及函数式编程，我们将在下一章中学习。

## 7.13 委托(Delegation)

### 7.13.1 代理模式（Proxy Pattern）

代理模式，也称委托模式。

在代理模式中，有两个对象参与处理同一个请求，接受请求的对象将请求委托给另一个对象来处理。代理模式是一项基本技巧，许多其他的模式，如状态模式、策略模式、访问者模式本质上是在特殊的场合采用了代理模式。

代理模式使得我们可以用聚合来替代继承，它还使我们可以模拟mixin（混合类型）。委托模式的作用是将委托者与实际实现代码分离出来，以达成解耦的目的。

一个代理模式的Java代码示例：



```java
package com.easy.kotlin;

/**
 * Created by jack on 2017/7/5.
 */
interface JSubject {
    public void request();
}

class JRealSubject implements JSubject {
    @Override
    public void request() {
        System.out.println("JRealSubject Requesting");
    }
}

class JProxy implements JSubject {
    private JSubject subject = null;

    //通过构造函数传递代理者
    public JProxy(JSubject sub) {
        this.subject = sub;
    }

    @Override
    public void request() { //实现接口中定义的方法
        this.before();
        this.subject.request();
        this.after();
    }

    private void before() {
        System.out.println("JProxy Before Requesting ");
    }

    private void after() {
        System.out.println("JProxy After Requesting ");
    }
}

public class DelegateDemo {
    public static void main(String[] args) {
        JRealSubject jRealSubject = new JRealSubject();
        JProxy jProxy = new JProxy(jRealSubject);
        jProxy.request();
    }
}
```

输出：

JProxy Before Requesting
 JRealSubject Requesting
 JProxy After Requesting

### 7.13.2 类的委托(Class Delegation)

就像支持单例模式的object对象一样，Kotlin 在语言层面原生支持委托模式。

代码示例：



```kotlin
package com.easy.kotlin

import java.util.*

/**
 * Created by jack on 2017/7/5.
 */

interface Subject {
    fun hello()
}

class RealSubject(val name: String) : Subject {
    override fun hello() {
        val now = Date()
        println("Hello, REAL $name! Now is $now")
    }
}

class ProxySubject(val sb: Subject) : Subject by sb {
    override fun hello() {
        println("Before ! Now is ${Date()}")
        sb.hello()
        println("After ! Now is ${Date()}")
    }
}

fun main(args: Array<String>) {
    val subject = RealSubject("World")
    subject.hello()
    println("-------------------------")
    val proxySubject = ProxySubject(subject)
    proxySubject.hello()
}
```

在这个例子中，委托代理类 ProxySubject 继承接口 Subject，并将其所有共有的方法委托给一个指定的对象sb :



```kotlin
class ProxySubject(val sb: Subject) : Subject by sb 
```

ProxySubject 的超类型Subject中的 `by sb` 表示sb 将会在 ProxySubject 中内部存储。

另外，我们在覆盖重写了函数`override fun hello()`。

测试代码：



```kotlin
fun main(args: Array<String>) {
    val subject = RealSubject("World")
    subject.hello()
    println("-------------------------")
    val proxySubject = ProxySubject(subject)
    proxySubject.hello()
}
```

输出：



```css
Hello, REAL World! Now is Wed Jul 05 02:45:42 CST 2017
-------------------------
Before ! Now is Wed Jul 05 02:45:42 CST 2017
Hello, REAL World! Now is Wed Jul 05 02:45:42 CST 2017
After ! Now is Wed Jul 05 02:45:42 CST 2017
```

### 7.13.3 委托属性 (Delegated Properties)

通常对于属性类型，我们是在每次需要的时候手动声明它们：



```dart
class NormalPropertiesDemo {
    var content: String = "NormalProperties init content"
}
```

那么这个content属性将会很“呆板”。属性委托赋予了属性富有变化的活力。

例如：

- 延迟属性（lazy properties）: 其值只在首次访问时计算
- 可观察属性（observable properties）: 监听器会收到有关此属性变更的通知
- 把多个属性储存在一个映射（map）中，而不是每个存在单独的字段中。

#### 委托属性

Kotlin 支持 *委托属性*:



```kotlin
class DelegatePropertiesDemo {
    var content: String by Content()

    override fun toString(): String {
        return "DelegatePropertiesDemo Class"
    }
}

class Content {
    operator fun getValue(delegatePropertiesDemo: DelegatePropertiesDemo, property: KProperty<*>): String {
        return "${delegatePropertiesDemo} property '${property.name}' = 'Balalala ... ' "
    }

    operator fun setValue(delegatePropertiesDemo: DelegatePropertiesDemo, property: KProperty<*>, value: String) {
        println("${delegatePropertiesDemo} property '${property.name}' is setting value: '$value'")
    }
}
```

在 `var content: String by Content()`中， `by` 后面的表达式的`Content()`就是该属性 *委托*的对象。content属性对应的 `get()`（和 `set()`）会被委托给`Content()`的 `operator fun getValue()` 和 `operator fun setValue()` 函数，这两个函数是必须的，而且得是操作符函数。

测试代码：



```go
    val n = NormalPropertiesDemo()
    println(n.content)
    n.content = "Lao tze"
    println(n.content)

    val e = DelegatePropertiesDemo()
    println(e.content) // call Content.getValue
    e.content = "Confucius" // call Content.setValue
    println(e.content) // call Content.getValue
```

输出：



```rust
NormalProperties init content
Lao tze
DelegatePropertiesDemo Class property 'content' = 'Balalala ... ' 
DelegatePropertiesDemo Class property 'content' is setting value: 'Confucius'
DelegatePropertiesDemo Class property 'content' = 'Balalala ... 
```

#### 懒加载属性委托 lazy

lazy() 函数定义如下：



```kotlin
@kotlin.jvm.JvmVersion
public fun <T> lazy(initializer: () -> T): Lazy<T> = SynchronizedLazyImpl(initializer)
```

它接受一个 lambda 并返回一个 Lazy <T> 实例的函数，返回的实例可以作为实现懒加载属性的委托：

第一次调用 get() 会执行已传递给 lazy() 的 lamda 表达式并记录下结果， 后续调用 get() 只是返回之前记录的结果。

代码示例：



```swift
    val synchronizedLazyImpl = lazy({
        println("lazyValueSynchronized1  3!")
        println("lazyValueSynchronized1  2!")
        println("lazyValueSynchronized1  1!")
        "Hello, lazyValueSynchronized1 ! "
    })

    val lazyValueSynchronized1: String by synchronizedLazyImpl
    println(lazyValueSynchronized1)
    println(lazyValueSynchronized1)

    val lazyValueSynchronized2: String by lazy {
        println("lazyValueSynchronized2  3!")
        println("lazyValueSynchronized2  2!")
        println("lazyValueSynchronized2  1!")
        "Hello, lazyValueSynchronized2 ! "
    }

    println(lazyValueSynchronized2)
    println(lazyValueSynchronized2)
```

输出：



```undefined
lazyValueSynchronized1  3!
lazyValueSynchronized1  2!
lazyValueSynchronized1  1!
Hello, lazyValueSynchronized1 ! 
Hello, lazyValueSynchronized1 ! 


lazyValueSynchronized2  3!
lazyValueSynchronized2  2!
lazyValueSynchronized2  1!
Hello, lazyValueSynchronized2 ! 
Hello, lazyValueSynchronized2 ! 
```

默认情况下，对于 lazy 属性的求值是同步的（synchronized）, 下面两种写法是等价的：



```swift
    val synchronizedLazyImpl = lazy({
        println("lazyValueSynchronized1  3!")
        println("lazyValueSynchronized1  2!")
        println("lazyValueSynchronized1  1!")
        "Hello, lazyValueSynchronized1 ! "
    })
    
    val synchronizedLazyImpl2 = lazy(LazyThreadSafetyMode.SYNCHRONIZED, {
        println("lazyValueSynchronized1  3!")
        println("lazyValueSynchronized1  2!")
        println("lazyValueSynchronized1  1!")
        "Hello, lazyValueSynchronized1 ! "
    })
```

该值是线程安全的。所有线程会看到相同的值。

如果初始化委托多个线程可以同时执行，不需要同步锁，使用`LazyThreadSafetyMode.PUBLICATION`：



```swift
    val lazyValuePublication: String by lazy(LazyThreadSafetyMode.PUBLICATION, {
        println("lazyValuePublication 3!")
        println("lazyValuePublication 2!")
        println("lazyValuePublication 1!")
        "Hello, lazyValuePublication ! "
    })
```

而如果属性的初始化是单线程的，那么我们使用 LazyThreadSafetyMode.NONE 模式(性能最高)：



```swift
    val lazyValueNone: String by lazy(LazyThreadSafetyMode.NONE, {
        println("lazyValueNone 3!")
        println("lazyValueNone 2!")
        println("lazyValueNone 1!")
        "Hello, lazyValueNone ! "
    })
```

#### Delegates.observable 可观察属性委托

我们把属性委托给`Delegates.observable`函数，当属性值被重新赋值的时候， 触发其中的回调函数 onChange。

该函数定义如下：



```kotlin
public inline fun <T> observable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(property, oldValue, newValue)
        }
```

代码示例：



```dart
class PostHierarchy {
    var level: String by Delegates.observable("P0",
            { property: KProperty<*>,
              oldValue: String,
              newValue: String ->
                println("$oldValue -> $newValue")
            })
}
```

测试代码：



```go
    val ph = PostHierarchy()
    ph.level = "P1"
    ph.level = "P2"
    ph.level = "P3"
    println(ph.level) // P3
```

输出：



```rust
P0 -> P1
P1 -> P2
P2 -> P3
P3
```

我们可以看出，属性`level`每次赋值，都回调了`Delegates.observable`中的lambda表达式所写的`onChange`函数。

#### Delegates.vetoable 可否决属性委托

这个函数定义如下：



```kotlin
public inline fun <T> vetoable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Boolean):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = onChange(property, oldValue, newValue)
        }
```

当我们把属性委托给这个函数时，我们可以通过`onChange`函数的返回值是否为true， 来选择属性的值是否需要改变。

代码示例:



```csharp
class PostHierarchy {
    var grade: String by Delegates.vetoable("T0", {
        property, oldValue, newValue ->
        true
    })

    var notChangeGrade: String by Delegates.vetoable("T0", {
        property, oldValue, newValue ->
        false
    })
}
```

测试代码：



```go
    ph.grade = "T1"
    ph.grade = "T2"
    ph.grade = "T3"
    println(ph.grade) // T3

    ph.notChangeGrade = "T1"
    ph.notChangeGrade = "T2"
    ph.notChangeGrade = "T3"
    println(ph.notChangeGrade) // T0
```

我们可以看出，当onChange函数返回值是false的时候，对属性notChangeGrade的赋值都没有生效，依然是原来的默认值T0 。

#### Delegates.notNull 非空属性委托

我们也可以使用委托来实现属性的非空限制：



```csharp
var name: String by Delegates.notNull()
```

这样name属性就被限制为不能为null，如果被赋值null，编译器直接报错：



```dart
ph.name = null // error 
Null can not be a value of a non-null type String
```

#### 属性委托给Map映射

我们也可以把属性委托给Map：



```kotlin
class Account(val map: Map<String, Any?>) {
    val name: String by map
    val password: String by map
}
```

测试代码：



```kotlin
val account = Account(mapOf(
            "name" to "admin",
            "password" to "admin"
    ))

println("Account(name=${account.name}, password = ${account.password})")
```

输出：



```undefined
Account(name=admin, password = admin)
```

如果是可变属性，这里也可以把只读的 Map 换成 MutableMap ：



```kotlin
class MutableAccount(val map: MutableMap<String, Any?>) {
    var name: String by map
    var password: String by map
}
```

测试代码：



```kotlin
val maccount = MutableAccount(mutableMapOf(
            "name" to "admin",
            "password" to "admin"
))

maccount.password = "root"
println("MutableAccount(name=${maccount.name}, password = ${maccount.password})")
```

输出：



```undefined
MutableAccount(name=admin, password = root)
```

## 本章小结

本章我们介绍了Kotlin面向对象编程的特性： 类与构造函数、抽象类与接口、继承以及多重继承等基础知识，同时介绍了Kotlin中的注解类、枚举类、数据类、密封类、嵌套类、内部类、匿名内部类等特性类。最后我们学习了Kotlin中对单例模式、委托模式的语言层面上的内置支持：object对象、委托。

总的来说，Kotlin相比于Java的面向对象编程，增加不少有趣的功能与特性支持，这使得我们代码写起来更加方便快捷了。

我们知道，在Java 8 中，引进了对函数式编程的支持：Lambda表达式、Function接口、stream API等，而在Kotlin中，对函数式编程的支持更加全面丰富，代码写起来也更加简洁优雅。下一章中，我们来一起学习Kotlin的函数式编程。

本章示例代码工程：

https://github.com/EasyKotlin/chatper7_oop