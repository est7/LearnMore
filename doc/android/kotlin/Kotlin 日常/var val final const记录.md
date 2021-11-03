1、var、val定义
/**

 * kotlin对变量有两种分类
 * var：可变的变量
 * val：不可变的变量（类似java中final修饰的变量）

 *Kotlin官方是推荐我们尽量使用val变量的，因为在复杂的逻辑中，val变量更加安全可靠。
 */

var age: Int = 18
val age1: Int = 19
2、var、val本质区别
区别一：

var 可以重写set、get方法

val 只能重写get方法

区别二：

var、val 修饰的变量必须初始化值，不同的是：用lateinit关键字修饰的var变量可以不用初始化值，但是用lateinit修饰的val变量也必须初始化值

lateinit var sex: String
       lateinit val sex1: String

  就比如下面的isMain 属性，用val修饰，但是如果你重写它的get方法，会直接报错。

data class Person(var name: String, var age: Int) {
   var weight: Int = 0
        get() = field
        set(value) {
            this.weight=value
        }
    val isMan:Boolean
        get() {
            return this.sex=="男"
        }

    lateinit var sex: String
    lateinit val sex1: String="女" //不初始化就会报错
}

3、final变量与val变量区别
3.1、final变量
a. 必须初始化值。
b. 被final修饰的成员变量赋值，有两种方式：1、直接赋值 2、全部在构造方法中赋初值。
c. 如果修饰的成员变量是基本类型，则表示这个变量的值不能改变（二次赋值）。
d. 如果修饰的成员变量是一个引用类型，则是说这个引用的地址的值不能修改，但是这个引用所指向的对象里面的内容还是可以改变的。

3.2、val变量
 val变量也需要初始化值
final的第二点在val上也适用，下面代码中：要么val修饰的person2赋值，要么就是放在构造函数中age 去赋值
用val修饰的基本类型，也无法进行二次赋值
用val修饰的变量如果是一个引用类型，必须下面的 isMain 属性虽然不能二次赋值，但是我们可以sex的值，来改变isMain的值，但是它的引用地址不会修改。
val person2:Person1?=null //必须赋值

data class Person1(var name: String, val age: Int) {
    
    //用lateinit关键字修饰的属性，可以没有初始值，可以后期set、get
    lateinit var sex: String
    lateinit val sex1: String //报错
    
    val isMan:Boolean
        get() {
            return this.sex=="男"
        }

}
4、const关键字
4.1、定义
为了定义常量所存在的。

4.2、限制条件
const只能修饰val的变量
const只能修饰object的属性，或者 top-level的属性（这里的top-level就是只能定义在kotlin文件的中，不能定义在class中）
const的变量的值，必须在编译期间就确定下来，所以它的类型只能是String和基本类型
val name: String = "A"

//const var name1:String="B"        修饰var变量--报错
const val name2: String = "B"

//const class TestVal{}                 修饰类---报错
//const val person1:Person1?=null   修饰引用类型--报错

class TestVal {
    //const val name3:String="C"        放在类中-----报错
    object TestConst {
        const val name4: String = "B"
    }
}
5、val与const val 的区别
只用val修饰的变量，即使加上public修饰，编译之后的也是private修饰
用const val 修饰的变量默认就是public 
结论：我们想要创建常量，必须使用const val 修饰变量

//Kotlin代码
   val test1: String = "A"
   const val test2: String = "B"

   public val test3: String = "C"
   public const val test4: String = "D"

   private val test5: String = "E"
   private const val test6: String = "F"

 //反编译class文件得到的源码
   @NotNull
   private static final String test1 = "A";
   @NotNull
   public static final String test2 = "B";
   @NotNull
   private static final String test3 = "C";
   @NotNull
   public static final String test4 = "D";
   private static final String test5 = "E";
   private static final String test6 = "F";


6、Java中final的其他属性
6.1、修饰类
当用final去修饰一个类的时候，表示这个类不能被继承。

 a. 被final修饰的类，final类中的成员变量可以根据自己的实际需要设计为final。

 b. final类中的成员方法都会被隐式的指定为final方法。

 

6.2、 修饰方法
被final修饰的方法不能被重写。

a. 一个类的private方法会隐式的被指定为final方法。
b. 如果父类中有final修饰的方法，那么子类不能去重写。
6.3、 修饰成员变量

a. 必须初始化值。
b. 被final修饰的成员变量赋值，有两种方式：1、直接赋值 2、全部在构造方法中赋初值。
c. 如果修饰的成员变量是基本类型，则表示这个变量的值不能改变（二次赋值）。
d. 如果修饰的成员变量是一个引用类型，则是说这个引用的地址的值不能修改，但是这个引用所指向的对象里面的内容还是可以改变的。