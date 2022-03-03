# Kotlin 中级篇（八）:高阶函数详解与标准的高阶函数使用

## 一、高阶函数介绍

> 在`Kotlin`中，高阶函数即指：将函数用作一个函数的参数或者返回值的函数。

### 1.1、将函数用作函数参数的情况的高阶函数

这里介绍字符串中的`sumBy{}`高阶函数。先看一看源码

```
// sumBy函数的源码
public inline fun CharSequence.sumBy(selector: (Char) -> Int): Int {
    var sum: Int = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
复制代码
```

源码说明：

1. 大家这里可以不必纠结`inline`，和`sumBy`函数前面的`CharSequence.`。因为这是`Koltin`中的`内联函数`与`扩展功能`。在后面的章节中会给大家讲解到的。这里主要分析高阶函数，故而这里不多做分析。
2. 该函数返回一个`Int`类型的值。并且接受了一个`selector()`函数作为该函数的参数。其中，`selector()`函数接受一个`Char`类型的参数，并且返回一个`Int`类型的值。
3. 定义一个`sum`变量，并且循环这个字符串，循环一次调用一次`selector()`函数并加上`sum`。用作累加。其中`this`关键字代表字符串本身。

所以这个函数的作用是：**把字符串中的每一个字符转换为`Int`的值，用于累加，最后返回累加的值**

例：

```
val testStr = "abc"
val sum = testStr.sumBy { it.toInt() }
println(sum)
复制代码
```

输出结果为：

```
294  // 因为字符a对应的值为97,b对应98，c对应99，故而该值即为 97 + 98 + 99 = 294
复制代码
```

### 1.2、将函数用作一个函数的返回值的高阶函数。

这里使用官网上的一个例子来讲解。`lock()`函数，先看一看他的源码实现

```
fun <T> lock(lock: Lock, body: () -> T): T {
    lock.lock()
    try {
        return body()
    }
    finally {
        lock.unlock()
    }
}
复制代码
```

源码说明：

1. 这其中用到了`kotlin`中`泛型`的知识点，这里赞不考虑。我会在后续的文章为大家讲解。
2. 从源码可以看出，该函数接受一个`Lock`类型的变量作为参数`1`，并且接受一个无参且返回类型为`T`的函数作为参数`2`.
3. 该函数的返回值为一个函数，我们可以看这一句代码`return body()`可以看出。

例：使用`lock`函数，下面的代码都是伪代码，我就是按照官网的例子直接拿过来用的

```
fun toBeSynchronized() = sharedResource.operation()
val result = lock(lock, ::toBeSynchronized)    
复制代码
```

其中，`::toBeSynchronized`即为对函数`toBeSynchronized()`的引用，其中关于双冒号`::`的使用在这里不做讨论与讲解。

上面的写法也可以写作：

```
val result = lock(lock, {sharedResource.operation()} )
复制代码
```

### 1.3、高阶函数的使用

在上面的两个例子中，我们出现了`str.sumBy{ it.toInt }`这样的写法。其实这样的写法在前一章节`Lambda使用`中已经讲解过了。这里主要讲高阶函数中对`Lambda语法`的简写。

从上面的例子我们的写法应该是这样的：

```
str.sumBy( { it.toInt } )
复制代码
```

但是根据`Kotlin`中的约定，即当函数中只有一个函数作为参数，并且您使用了`lambda`表达式作为相应的参数，则可以省略函数的小括号`()`。故而我们可以写成：

```
str.sumBy{ it.toInt }
复制代码
```

还有一个约定，即当函数的最后一个参数是一个函数，并且你传递一个`lambda`表达式作为相应的参数，则可以在圆括号之外指定它。故而上面例`2`中的代码我们可写成：

```
val result = lock(lock){
     sharedResource.operation()
}
复制代码
```

## 二、自定义高阶函数

我记得在上一章节中我写了一个例子：

```
// 源代码
fun test(a : Int , b : Int) : Int{
    return a + b
}

fun sum(num1 : Int , num2 : Int) : Int{
    return num1 + num2
}

// 调用
test(10,sum(3,5)) // 结果为：18

// lambda
fun test(a : Int , b : (num1 : Int , num2 : Int) -> Int) : Int{
    return a + b.invoke(3,5)
}

// 调用
test(10,{ num1: Int, num2: Int ->  num1 + num2 })  // 结果为：18
复制代码
```

可以看出上面的代码中，直接在我的方法体中写死了数值，这在开发中是很不合理的，并且也不会这么写。上面的例子只是在阐述`Lambda`的语法。接下来我另举一个例子：

例：传入两个参数，并传入一个函数来实现他们不同的逻辑

例：

```
private fun resultByOpt(num1 : Int , num2 : Int , result : (Int ,Int) -> Int) : Int{
    return result(num1,num2)
}

private fun testDemo() {
    val result1 = resultByOpt(1,2){
        num1, num2 ->  num1 + num2
    }

    val result2 = resultByOpt(3,4){
        num1, num2 ->  num1 - num2
    }

    val result3 = resultByOpt(5,6){
        num1, num2 ->  num1 * num2
    }

    val result4 = resultByOpt(6,3){
        num1, num2 ->  num1 / num2
    }

    println("result1 = $result1")
    println("result2 = $result2")
    println("result3 = $result3")
    println("result4 = $result4")
}
复制代码
```

输出结果为：

```
result1 = 3
result2 = -1
result3 = 30
result4 = 2  
复制代码
```

这个例子是根据传入不同的`Lambda`表达式，实现了两个数的`+、-、*、/`。
当然了，在实际的项目开发中，自己去定义高阶函数的实现是很少了，因为用系统给我们提供的高阶函数已经够用了。不过，当我们掌握了`Lambda`语法以及怎么去定义高阶函数的用法后。在实际开发中有了这种需求的时候也难不倒我们了。

## 三、常用的标准高阶函数介绍

下面介绍几个`Kotlin`中常用的标准高阶函数。熟练的用好下面的几个函数，能减少很多的代码量，并增加代码的可读性。下面的几个高阶函数的源码几乎上都出自`Standard.kt`文件

### 3.1、TODO函数

这个函数不是一个高阶函数，它只是一个抛出异常以及测试错误的一个普通函数。

> 此函数的作用：显示抛出`NotImplementedError`错误。`NotImplementedError`错误类继承至`Java`中的`Error`。我们看一看他的源码就知道了：

```
public class NotImplementedError(message: String = "An operation is not implemented.") : Error(message)
复制代码
```

`TODO`函数的源码

```
@kotlin.internal.InlineOnly
public inline fun TODO(): Nothing = throw NotImplementedError()

@kotlin.internal.InlineOnly
public inline fun TODO(reason: String): Nothing = 
throw NotImplementedError("An operation is not implemented: $reason")
复制代码
```

举例说明：

```
fun main(args: Array<String>) {
    TODO("测试TODO函数，是否显示抛出错误")
}
复制代码
```

输出结果为：

![img](../../../../art/Kotlin%20%E4%B8%AD%E7%BA%A7%20%E9%AB%98%E9%98%B6%E5%87%BD%E6%95%B0/c96b07f6ea20407ebd130e2937e9e5abtplv-k3u1fbpfcp-watermark.webp)

如果调用`TODO()`时，不传参数的，则会输出`An operation is not implemented.`

### 3.2 、run()函数

`run`函数这里分为两种情况讲解，因为在源码中也分为两个函数来实现的。采用不同的`run`函数会有不同的效果。

#### 3.2.1、run()

我们看下其源码：

```
public inline fun <R> run(block: () -> R): R {
contract {
    callsInPlace(block, InvocationKind.EXACTLY_ONCE)
}
return block()
}
复制代码
```

关于`contract`这部分代码小生也不是很懂其意思。在一些大牛的`blog`上说是其编辑器对上下文的推断。但是我也不知道对不对，因为在官网中，对这个东西也没有讲解到。不过这个单词的意思是`契约，合同`等等意思。我想应该和这个有关。在这里我就不做深究了。主要讲讲`run{}`函数的用法其含义。

这里我们只关心`return block()`这行代码。从源码中我们可以看出，`run`函数仅仅是执行了我们的`block()`，即一个`Lambda`表达式，而后返回了执行的结果。

**用法1：**

> 当我们需要执行一个`代码块`的时候就可以用到这个函数,并且这个代码块是独立的。即我可以在`run()`函数中写一些和项目无关的代码，因为它不会影响项目的正常运行。

例: 在一个函数中使用

```
private fun testRun1() {
    val str = "kotlin"

    run{
        val str = "java"   // 和上面的变量不会冲突
        println("str = $str")
    }

    println("str = $str")
}    
复制代码
```

输出结果：

```
str = java
str = kotlin
复制代码
```

**用法2：**

> 因为`run`函数执行了我传进去的`lambda`表达式并返回了执行的结果，所以当一个业务逻辑都需要执行同一段代码而根据不同的条件去判断得到不同结果的时候。可以用到`run`函数

例：都要获取字符串的长度。

```
val index = 3
val num = run {
    when(index){
        0 -> "kotlin"
        1 -> "java"
        2 -> "php"
        3 -> "javaScript"
        else -> "none"
    }
}.length
println("num = $num")
复制代码
```

输出结果为：

```
num = 10
复制代码
```

当然这个例子没什么实际的意义。

#### 3.2.2、T.run()

其实`T.run()`函数和`run()`函数差不多，关于这两者之间的差别我们看看其源码实现就明白了：

```
public inline fun <T, R> T.run(block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
复制代码
```

从源码中我们可以看出，`block()`这个函数参数是一个扩展在`T`类型下的函数。这说明我的`block()`函数可以可以使用当前对象的上下文。所以**当我们传入的`lambda`表达式想要使用当前对象的上下文的时候，我们可以使用这个函数。**

**用法：**

> 这里就不能像上面`run()`函数那样当做单独的一个`代码块`来使用。

例：

```
val str = "kotlin"
str.run {
    println( "length = ${this.length}" )
    println( "first = ${first()}")
    println( "last = ${last()}" )
}
复制代码
```

输出结果为：

```
length = 6
first = k
last = n
复制代码
```

在其中，可以使用`this`关键字，因为在这里它就代码`str`这个对象，也可以省略。因为在源码中我们就可以看出，`block`()
就是一个`T`类型的扩展函数。

这在实际的开发当中我们可以这样用：

例： 为`TextView`设置属性。

```
val mTvBtn = findViewById<TextView>(R.id.text)
mTvBtn.run{
    text = "kotlin"
    textSize = 13f
    ...
}
复制代码
```

### 3.3 、with()函数

其实`with()`函数和`T.run()`函数的作用是相同的，我们这里看下其实现源码：

```
public inline fun <T, R> with(receiver: T, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return receiver.block()
}
复制代码
```

这里我们可以看出和`T.run()`函数的源代码实现没有太大的差别。故而这两个函数的区别在于：

> 1. `with`是正常的高阶函数，`T.run()`是扩展的高阶函数。
> 2. `with`函数的返回值指定了`receiver`为接收者。

故而上面的`T.run()`函数的列子我也可用`with`来实现相同的效果：

例：

```
val str = "kotlin"
with(str) {
    println( "length = ${this.length}" )
    println( "first = ${first()}")
    println( "last = ${last()}" )
}
复制代码
```

输出结果为：

```
length = 6
first = k
last = n
复制代码
```

为`TextView`设置属性，也可以用它来实现。这里我就不举例了。

在上面举例的时候，都是正常的列子，这里举一个特例：当我的对象可为`null`的时候，看两个函数之间的便利性。

例：

```
val newStr : String? = "kotlin"

with(newStr){
    println( "length = ${this?.length}" )
    println( "first = ${this?.first()}")
    println( "last = ${this?.last()}" )
}

newStr?.run {
    println( "length = $length" )
    println( "first = ${first()}")
    println( "last = ${last()}" )
}
复制代码
```

从上面的代码我们就可以看出，当我们使用对象可为`null`时，使用`T.run()`比使用`with()`函数从代码的可读性与简洁性来说要好一些。当然关于怎样去选择使用这两个函数，就得根据实际的需求以及自己的喜好了。

### 3.4、T.apply()函数

我们先看下`T.apply()`函数的源码：

```
public inline fun <T> T.apply(block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return this
}
复制代码
```

从`T.apply()`源码中在结合前面提到的`T.run()`函数的源码我们可以得出,这两个函数的逻辑差不多，唯一的区别是`T,apply`执行完了`block()`函数后，返回了自身对象。而`T.run`是返回了执行的结果。

故而： `T.apply`的作用除了实现能实现`T.run`函数的作用外，还可以后续的再对此操作。下面我们看一个例子：

例：为`TextView`设置属性后，再设置点击事件等

```
val mTvBtn = findViewById<TextView>(R.id.text)
mTvBtn.apply{
    text = "kotlin"
    textSize = 13f
    ...
}.apply{
    // 这里可以继续去设置属性或一些TextView的其他一些操作
}.apply{
    setOnClickListener{ .... }
}
复制代码
```

或者：设置为`Fragment`设置数据传递

```
// 原始方法
fun newInstance(id : Int , name : String , age : Int) : MimeFragment{
        val fragment = MimeFragment()
        fragment.arguments.putInt("id",id)
        fragment.arguments.putString("name",name)
        fragment.arguments.putInt("age",age)

        return fragment
}

// 改进方法
fun newInstance(id : Int , name : String , age : Int) = MimeFragment().apply {
        arguments.putInt("id",id)
        arguments.putString("name",name)
        arguments.putInt("age",age)
}
复制代码
```

### 3.5、T.also()函数

关于`T.also`函数来说，它和`T.apply`很相似，。我们先看看其源码的实现：

```
public inline fun <T> T.also(block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block(this)
    return this
}
复制代码
```

从上面的源码在结合`T.apply`函数的源码我们可以看出： `T.also`函数中的参数`block`函数传入了自身对象。故而这个函数的作用是用用`block`函数调用自身对象，最后在返回自身对象

这里举例一个简单的例子，并用实例说明其和`T.apply`的区别

例：

```
"kotlin".also {
    println("结果：${it.plus("-java")}")
}.also {
    println("结果：${it.plus("-php")}")
}

"kotlin".apply {
    println("结果：${this.plus("-java")}")
}.apply {
    println("结果：${this.plus("-php")}")
}
复制代码
```

他们的输出结果是相同的：

**

```
结果：kotlin-java
结果：kotlin-php

结果：kotlin-java
结果：kotlin-php
复制代码
```

从上面的实例我们可以看出，他们的区别在于，`T.also`中只能使用`it`调用自身,而`T.apply`中只能使用`this`调用自身。因为在源码中`T.also`是执行`block(this)`后在返回自身。而`T.apply`是执行`block()`后在返回自身。这就是为什么在一些函数中可以使用`it`,而一些函数中只能使用`this`的关键所在

### 3.6、T.let()函数

在前面讲解`空安全、可空属性`章节中，我们讲解到可以使用`T.let()`函数来规避空指针的问题。今天来说一下他的源码实现：

```
public inline fun <T, R> T.let(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this)
}
复制代码
```

从上面的源码中我们可以得出，它其实和`T.also`以及`T.apply`都很相似。而`T.let`的作用也不仅仅在使用`空安全`这一个点上。用`T.let`也可实现其他操作

例：

```
"kotlin".let {
    println("原字符串：$it")         // kotlin
    it.reversed()
}.let {
    println("反转字符串后的值：$it")     // niltok
    it.plus("-java")
}.let {
    println("新的字符串：$it")          // niltok-java
}

"kotlin".also {
    println("原字符串：$it")     // kotlin
    it.reversed()
}.also {
    println("反转字符串后的值：$it")     // kotlin
    it.plus("-java")
}.also {
    println("新的字符串：$it")        // kotlin
}

"kotlin".apply {
    println("原字符串：$this")     // kotlin
    this.reversed()
}.apply {
    println("反转字符串后的值：$this")     // kotlin
    this.plus("-java")
}.apply {
    println("新的字符串：$this")        // kotlin
}
复制代码
```

输出结果看是否和注释的结果一样呢：

```
原字符串：kotlin
反转字符串后的值：niltok
新的字符串：niltok-java

原字符串：kotlin
反转字符串后的值：kotlin
新的字符串：kotlin

原字符串：kotlin
反转字符串后的值：kotlin
新的字符串：kotlin
复制代码
```

### 3.7、T.takeIf()函数

从函数的名字我们可以看出，这是一个关于`条件判断`的函数,我们在看其源码实现：

```
public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate(this)) this else null
}
复制代码
```

从源码中我们可以得出这个函数的作用是：

> 传入一个你希望的一个条件，如果对象符合你的条件则返回自身，反之，则返回`null`。

例： 判断一个字符串是否由某一个字符起始，若条件成立则返回自身，反之，则返回`null`

```
val str = "kotlin"

val result = str.takeIf {
    it.startsWith("ko") 
}

println("result = $result")
复制代码
```

输出结果为：

```
result = kotlin
复制代码
```

### 3.8、T.takeUnless()函数

这个函数的作用和`T.takeIf()`函数的作用是一样的。只是和其的逻辑是相反的。即：传入一个你希望的一个条件，如果对象符合你的条件则返回`null`，反之，则返回自身。

这里看一看它的源码就明白了。

```
public inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T? {
    contract {
        callsInPlace(predicate, InvocationKind.EXACTLY_ONCE)
    }
    return if (!predicate(this)) this else null
}
复制代码
```

这里就举和`T.takeIf()`函数中一样的例子，看他的结果和`T.takeIf()`中的结果是不是相反的。

例：

```
val str = "kotlin"

val result = str.takeUnless {
    it.startsWith("ko") 
}

println("result = $result")
复制代码
```

输出结果为：

```
result = null
复制代码
```

### 3.8、repeat()函数

首先，我们从这个函数名就可以看出是关于`重复`相关的一个函数，再看起源码，从源码的实现来说明这个函数的作用：

```
public inline fun repeat(times: Int, action: (Int) -> Unit) {
    contract { callsInPlace(action) }

    for (index in 0..times - 1) {
        action(index)
    }
}
复制代码
```

从上面的代码我们可以看出这个函数的作用是：

> 根据传入的重复次数去重复执行一个我们想要的动作(函数)

例：

```
repeat(5){
    println("我是重复的第${it + 1}次，我的索引为：$it")
}
复制代码
```

输出结果为：

```
我是重复的第1次，我的索引为：0
我是重复的第2次，我的索引为：1
我是重复的第3次，我的索引为：2
我是重复的第4次，我的索引为：3
我是重复的第5次，我的索引为：4
复制代码
```

### 3.9、lazy()函数

关于`Lazy()`函数来说，它共实现了`4`个重载函数，都是用于延迟操作，不过这里不多做介绍。因为在实际的项目开发中常用都是用于延迟初始化属性。

## 四、对标准的高阶函数总结

关于重复使用同一个函数的情况一般都只有`T.also`、`T.let`、`T.apply`这三个函数。而这个三个函数在上面讲解这些函数的时候都用实例讲解了他们的区别。故而这里不做详细实例介绍。并且连贯着使用这些高阶函数去处理一定的逻辑，在实际项目中很少会这样做。一般都是单独使用一个，或者两个、三个这个连贯这用。

关于他们之间的区别，以及他们用于实际项目中在一定的需求下到底该怎样去选择哪一个函数进行使用希望大家详细的看下他们的源码并且根据我前面说写的实例进行分析。