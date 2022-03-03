# Kotlin基础语法（四）:空类型、空安全、非空断言、类型转换等特性



## 一、可空类型、空安全

### 1.1、定义一个可空类型的变量

> 定义一个可空类型的变量的格式为：`修饰符 变量名 ： 类型? = 值`

这里为了演示，定义变量和定义可空变量的区别，会提到定义变量的代码。

例：

```
// 定义一个不可为空的变量，用var修饰的变量可以被重新赋值，用val修饰的变量则不能，但是不能赋值为null
var a : Int = 12
val b : Int = 13

a = 20
// a = null 不能复制为null   
// b = 20   不能被重新赋值

if(a == null){
    // 这样的判断毫无意义，因为变量a永远不可能null
}

/*
    定义可空类型的变量,即变量可以被赋值为null
    定义格式为：修饰符 变量名 ： 类型? = 值
*/
var nullA : Int? = 12
val nullB : Int? = 13

nullA = null

if(nullA == null){
    println("nullA = $nullA")
}
复制代码
```

可以看出：`变量 nullA 的值为 null`

分析：要定义一个可空类型的变量时，即在定义变量的类型后面加上`?`符号就行了。在使用的时候，记住要判断该段该变量是否为空，这个操作在`Java`中经常会用到，如果定义一个不可为空类型的变量时，则判断将毫无意义，因为这个变量永远不会为空。

### 1.2、判断可空类型的两种使用方式

> 在上面我们提到，可空类型需要判断在使用，这里介绍除了`if ... else...`之外的其他方式

**1.2.1、if...else...判断**

例：

```
var str : String? = "123456"
str = null

if (str == null){
    println("变量str为空")
}else{
    println("str.length => ${str.length}")
}
复制代码
```

输出结果为：

```
变量str为空
复制代码
```

**1.2.2、使用符号`?.`判断**

> - 该符号的用法为：`可空类型变量?.属性/方法`。如果可空类型变量为null时，返回null
> - 这种用法大量用于**链式操作**的用法中，能有效避免`空引用异常（NullPointException）`，因为只要链式其中的一个为`null`，则整个表达式都为`null`

例：

```
var str : String? = "123456"
str = null

println(str?.length)   // 当变量str为null时，会返回空(null)
复制代码
```

输出结果为：

```
null
复制代码
```

**1.2.3、链式调用**

> `?.`这种符号去判断是否为`null`，在`Kotlin`中使用的地方是很多，特别是对于链式调用来说体验性更好。

例：这里简单写一个建造者模式，来模拟`?.`在链式调用中的用法

```
class Test{

    class Builder{
        private var name : String? = "Tom"
        private var age : Int? = 0
        private var sex : String? = "男"

        fun setName(name : String) : Builder?{
            this.name = name
            return this
        }

        fun setAge(age : Int) : Builder?{
            this.age = age
            return this
        }

        fun setSex(sex: String?) : Builder?{
            this.sex = sex
            return this
        }

        override fun toString(): String {
            return "Builder(name=$name, age=$age, sex=$sex)"
        }
    }
}

fun main(args: Array<String>) {
    val builder : Test.Builder? = Test.Builder().setName("Lily")?.setSex("nv")?.setAge(10)
    println(builder.toString())
}
复制代码
```

输出结果为：

```
Builder(name=Lily, age=10, sex=女)
复制代码
```

如果你上面的代码看不懂可以看下的代码，你可以看下面的例子：顶一个可空类型的字符串的长度加5再减去10

```
val testStr : String? = null
val result = testStr?.length?.plus(5)?.minus(10)
println(result)
复制代码
```

可以看出输出结果为: `null`

**1.2.4、函数中使用可空类型的情况下**

> 当一个函数/方法有返回值时，如果方法中的代码使用`?.`去返回一个值，那么方法的返回值的类型后面也要加上`?`符号

例：

```
fun funNullMethod() : Int? {
    val str : String? = "123456"
    return str?.length
}
复制代码
```

输出结果为：

```
6
复制代码
```

**1.2.5、let操作符**

> - `let`操作符的作用：当时用符号`?.`验证的时候忽略掉`null`
> - `let`的用法：`变量?.let{ ... }`

例：排除掉数组中的空元素

```
val arrTest : Array<Int?> = arrayOf(1,2,null,3,null,5,6,null)

// 传统写法
for (index in arrTest) {
    if (index == null){
        continue
    }
    println("index => $index")
}

// let写法
for (index in arrTest) {
    index?.let { println("index => $it") }
}
复制代码
```

输出结果为：

```
index => 1
index => 2
index => 3
index => 5
index => 6
复制代码
```

注意：上面的两种写法的效果是相同的，可以看出使用let{}高阶函数减少了好几行代码

## Evils操作符

> `Evils`其实不是一个操作符，而是`evil`的复数，而`evil`的意思在这里可以理解为`屏蔽、安全的`操作符，这样的操作符有三种：
>
> 1. `?:` 这个操作符表示在判断一个可空类型时，会返回一个我们自己设定好的默认值.
> 2. `!!` 这个操作符表示在判断一个可空类型时，会显示的抛出`空引用异常（NullPointException）`.
> 3. `as?` 这个操作符表示为安全的类型转换.

### 2.1、?:操作符

> 当我们定义了一个可空类型的变量时，如果该变量不为空，则使用，反之使用另外一个不为空的值

例：

```
val testStr : String? = null

var length = 0

// 例： 当testStr不为空时，输出其长度，反之输出-1

// 传统写法
length = if (testStr != null) testStr.length else -1

// ?: 写法
length = testStr?.length ?: -1

println(length)
复制代码
```

输出结果为：

```
-1
复制代码
```

分析：此操作符一般和`?.`操作符连用。当且仅当`?:`左边的表达式为`null`时，才会执行`?:`右边的表达式。

### 2.2、!!操作符

> `!!`操作符可谓是给爱好`空引用异常（NullPointException）`的开发者使用，因为在使用一个可空类型变量时，在该变量后面加上`!!`操作符，会显示的抛出`NullPointException`异常

例：

```
val testStr : String? = null
println(testStr!!.length)
复制代码
```

输出结果为：

![img](../../../../art/Kotlin%20%E5%9F%BA%E7%A1%80%20%E7%A9%BA%E5%AE%89%E5%85%A8%E6%93%8D%E4%BD%9C%E7%AC%A6/5be5e04ed909476fbad7e2d70ab7d7c8tplv-k3u1fbpfcp-watermark.webp)

可以看出，在未做空判断的情况下直接使用操作符`!!`的情况下，抛出了**空异常**

### 2.3、as?操作符

> 其实这里是指`as`操作符，表示类型转换，如果不能正常转换的情况下使用`as?`操作符。当使用`as`操作符的使用不能正常的转换的情况下会抛出`类型转换（ClassCastException）异常`，而使用`as?`操作符则会返回`null`,但是不会抛出异常

**2.3.1、使用as**

例：

```
// 会抛出ClassCastException异常
val num1 : Int? = "Koltin" as Int
println("nun1 = $num1")
复制代码
```

输出结果为：

![image.png](../../../../art/Kotlin%20%E5%9F%BA%E7%A1%80%20%E7%A9%BA%E5%AE%89%E5%85%A8%E6%93%8D%E4%BD%9C%E7%AC%A6/6dba8bd47c1e436ca68d8234315860eftplv-k3u1fbpfcp-watermark.webp)

**2.3.2、使用as?**

例：

```
val num2 : Int? = "Koltin" as? Int
println("nun2 = $num2)
复制代码
```

输出结果为：

```
num2 = null
复制代码
```

## 总结

这一章在实际的项目开发当中用到的地方是很多的，如果用好了各种空安全的操作符，估计你的项目中就不会抛出`以及`异常了，在这里我做了一个总结，希望会对各位有所帮助：

- 项目中会抛出`空引用（NullPointerException）`异常的情况

  > 1. 在可空类型变量的使用时，用了`!!`操作符
  > 2. 显式抛出空引用异常 `throw NullPointerException()`
  > 3. 外部 Java 代码导致的
  > 4. 对于初始化，有一些数据不一致(如一个未初始化的 this 用于构造函数的某个地方)

- 项目中会抛出`类型转换（ClassCastException）`异常的情况

  > 1. 在类型转换中使用了`as`操作符
  > 2. 使用了`toXXX()`方法不能转换的情况下
  > 3. 外部 Java 代码导致的

- 尽量避免使用的操作符

  > 1. 尽可能的不要使用`!!`操作符，多使用`?:`、`?.`操作符，以及`let{}`函数
  > 2. 尽可能的使用`as?`操作符去替换掉`as`,在不确定是否可以安全转换的情况下不使用`toXXX()`方法