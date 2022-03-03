# Kotlin 中级篇（一）:类（class）

## 一、类的声明

**1、关键字**

> 声明类的关键字为**`class`**

**2、声明格式**

```
class Test{
    // 属性...
    ...
    // 构造函数
    ...
    // 函数
    ...
    // 内部类
   ...
   ...
}
复制代码
```

其中：

**当类没有结构体的时候，大括号可以省略。即：**

```
class Test
复制代码
```

## 二、类的构造函数

> - 在`Kotlin`中，允许有一个主构造函数和多个二级构造函数（辅助构造函数）。其中主构造函数是类头的一部分。
> - 关键字或者构造函数名：`constructor(参数)`

**1、主构造函数**

> - 主构造函数是类头的一部分，类名的后面跟上构造函数的关键字以及类型参数。

**1.1、举例说明：**

```
class Test constructor(num : Int){
     ...
}
复制代码
```

**等价于**

```
/*
     因为是默认的可见性修饰符且不存在任何的注释符
     故而主构造函数constructor关键字可以省略
*/
class Test(num: Int){
      ...
}
复制代码
```

**1.2、构造函数中的初始化代码块**

> - 构造函数中不能出现其他的代码，只能包含初始化代码。包含在初始化代码块中。
> - 关键字：`init{...}`
> - 值得注意的是，`init{...}`中能使用构造函数中的参数

例：

```
fun main(args: Array<String>) {
    // 类的实例化，会在下面讲解到，这里只是作为例子讲解打印结果
    var test = Test(1)
}

class Test constructor(var num : Int){
    init {
        num = 5
        println("num = $num")
    }
}
复制代码
```

输出结果为：

```
num = 5
复制代码
```

其中，上面的`constructor`关键字是可以省略的。

**1.3、声明属性的简便方法**

> - 即在主构造函数中声明。

例：

```
class Test(val num1 : Int, var num2 : Long, val str : String){
    ...
}
复制代码
```

则：相当于声明了3个属性。
其中，`var`表示变量（可读写），`val`表示常量（只读）。

**1.4、什么时候constructor可以省略**

> - 在构造函数不具有注释符或者默认的可见性修饰符时，`constructor`关键字可以省略。
> - 默认的可见性修饰符时`public`。可以省略不写。

例：

```
// 类似下面两种情况的，都必须存在constructor关键字，并且在修饰符或者注释符后面。
class Test private constructor(num: Int){
}

class Test @Inject constructor(num: Int){
}
复制代码
```

**2、辅助（二级）构造函数**

> - `Kotlin`中支持二级构造函数。它们以`constructor`关键字作为前缀。

**2.1、声明**

例：

```
class Test{
    constructor(参数列表){

    }
}
复制代码
```

**2.2、同时存在主构造函数和二级构造函数时的情况**

> - 如果类具有主构造函数，则每个辅助构造函数需要通过另一个辅助构造函数直接或间接地委派给主构造函数。 使用`this`关键字对同一类的另一个构造函数进行委派：

例：

```
fun main(args: Array<String>) {
    var test1 = Test(1)
    var test2 = Test(1,2)
}

// 这里是为了代码清晰，故而没有隐藏constructor关键字
class Test constructor(num: Int){

    init {
        println("num = $num")
    }

    constructor(num : Int, num2: Int) : this(num) {
        println(num + num2)
    }
}
复制代码
```

说明：二级构造函数中的参数1(`num`)，是委托了主构造函数的参数`num`。

可以看出，当实例化类的时候只传1个参数的时候，只会执行`init`代码块中的代码。当传2个参数的时候，除了执行了`init`代码块中代码外，还执行了二级构造函数中的代码。

输出结果为：

```
num = 1
num = 1
3
复制代码
```

**2.3、当类的主构造函数都存在默认值时的情况**

> - 在`JVM`上，如果类主构造函数的所有参数都具有默认值，编译器将生成一个额外的无参数构造函数，它将使用默认值。 这使得更容易使用`Kotlin`与诸如`Jackson`或`JPA`的库，通过无参数构造函数创建类实例。
> - 同理可看出，当类存在主构造函数并且有默认值时，二级构造函数也适用

例：

```
fun main(args: Array<String>) {
    var test = Test()
    var test1 = Test(1,2)
    var test2 = Test(4,5,6)
}

class Test constructor(num1: Int = 10 , num2: Int = 20){

    init {
        println("num1 = $num1\t num2 = $num2")
    }

    constructor(num1 : Int = 1, num2 : Int = 2, num3 : Int = 3) : this(num1 , num2){
        println("num1 = $num1\t num2 = $num2 \t num3 = $num3")
    }
}
复制代码
```

输出结果为：

```
num1 = 10    num2 = 20
num1 = 1     num2 = 2
num1 = 4     num2 = 5
num1 = 4     num2 = 5    num3 = 6
复制代码
```

说明： 当实例化无参的构造函数时。使用了参数的默认值。

## 三、类的实例化

> - 创建一个类的实例，需要调用类的构造函数，就像它是一个常规函数一样：

例：

```
var test = Test()
var test1 = Test(1,2)
复制代码
```

其实在上面的例子中就实例化类的运用。
注意：**这里和`Java`不同的点是，没有`new`这个关键字**