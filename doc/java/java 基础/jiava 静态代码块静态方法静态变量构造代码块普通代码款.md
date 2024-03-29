我们知道java中类的生命周期为**装载**、**连接**、**初始化**、**使用**和**卸载**五个过程，如下图所示：

![img](../../../art/jiava%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E5%9D%97%E9%9D%99%E6%80%81%E6%96%B9%E6%B3%95%E9%9D%99%E6%80%81%E5%8F%98%E9%87%8F%E6%9E%84%E9%80%A0%E4%BB%A3%E7%A0%81%E5%9D%97%E6%99%AE%E9%80%9A%E4%BB%A3%E7%A0%81%E6%AC%BE/20180605173239239.png)

1.加载 

  我们编写一个java类的代码，经过编译之后生成一个后缀名为.class的文件，java虚拟机就能识别这种文件。java的生命周期就是class文件从加载到消亡的过程。 关于加载，其实，就是将源文件的class文件找到类的信息将其加载到方法区中，然后在堆区中实例化一个java.lang.Class对象，作为方法区中这个类的信息的入口。但是这一功能是在JVM之外实现的，主要的原因是方便让应用程序自己决定如何获取这个类，在不同的虚拟机实现的方式不一定相同，hotspot虚拟机是采用需要时在加载的方式，也有其他是先预先加载的。 （可以参考深入理解JVM这本书）

2.连接 
  连接一般是加载阶段和初始化阶段交叉进行，过程由以下三部分组成：
  （1）验证：确定该类是否符合java语言的规范，有没有属性和行为的重复，继承是否合理，总之，就是保证jvm能够执行 
  （2）准备：主要做的就是为由static修饰的成员变量分配内存，并设置默认的初始值 
    默认初始值如下：

​          1.八种基本数据类型默认的初始值是0 
​          2.引用类型默认的初始值是null 

​          3.有static final修饰的会直接赋值，例如：static final int x=10；则默认就是10.

（3）解析：这一阶段的任务就是把常量池中的符号引用转换为直接引用，说白了就是jvm会将所有的类或接口名、字段名、方法名转换为具体的内存地址。

3.初始化 
  初始化这个阶段就是将静态变量（类变量）赋值的过程，即只有static修饰的才能被初始化，执行的顺序就是：父类静态域或着静态代码块，然后是子类静态域或者子类静态代码块（静态代码块先被加载，然后再是静态属性）

4.使用 

  在类的使用过程中依然存在以下三步：

  （1）对象实例化：就是执行类中构造函数的内容，如果该类存在父类JVM会通过显示或者隐示的方式先执行父类的构造函数，在堆内存中为父类的实例变量开辟空间，并赋予默认的初始值，然后在根据构造函数的代码内容将真正的值赋予实例变量本身，然后，引用变量获取对象的首地址，通过操作对象来调用实例变量和方法 



  （2）垃圾收集：当对象不再被引用的时候，就会被虚拟机标上特别的垃圾记号，在堆中等待GC回收 
  （3）对象的终结：对象被GC回收后，对象就不再存在，对象的生命也就走到了尽头
5.类卸载 
  类卸载即类的生命周期走到了最后一步，程序中不再有该类的引用，该类也就会被JVM执行垃圾回收，从此生命结束…

代码示例：

```
package com.etc.test;
class` `A{
  ` `static` `int` `a;` `//类变量
  ` `String name;
  ` `int` `id;
  ` `//静态代码块
  ` `static` `{
    ` `a=10;
    ` `System.` `out` `.println(` `"这是父类的静态代码块"` `+a);
  ` `}
  ` `//构造代码块
  ` `{
    ` `id=11;
    ` `System.` `out` `.println(` `"这是父类的构造代码块id:"` `+id);
  ` `}
  ` `A(){
    ` `System.` `out` `.println(` `"这是父类的无参构造函数"` `);
  ` `}
  ` `A(String name){
    ` `System.` `out` `.println(` `"这是父类的name"` `+name);
  ` `}
}
class` `B extends A{
  ` `String name;
  ` `static` `int` `b;
  ` `static` `{
    ` `b=12;
    ` `System.` `out` `.println(` `"这是子类的静态代码块"` `+b);
  ` `}
   ` `B(String name) {
    ` `super();
    ` `this` `.name = name;
    ` `System.` `out` `.println(` `"这是子类的name:"` `+name);
  ` `}
}
public` `class` `Test666 {
public` `static` `void` `main(String[] args) {
  ` `B bb=` `new` `B(` `"GG"` `);
}
}
```

![img](../../../art/jiava%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E5%9D%97%E9%9D%99%E6%80%81%E6%96%B9%E6%B3%95%E9%9D%99%E6%80%81%E5%8F%98%E9%87%8F%E6%9E%84%E9%80%A0%E4%BB%A3%E7%A0%81%E5%9D%97%E6%99%AE%E9%80%9A%E4%BB%A3%E7%A0%81%E6%AC%BE/20180605174854853.png)



静态代码在类的初始化阶段被初始化。

非静态代码则在类的使用阶段（也就是实例化一个类的时候）才会被初始化。

- 静态变量
- 可以将静态变量理解为类变量（与对象无关），而实例变量则属于一个特定的对象。

> 静态变量有两种情况：
>
> - 静态变量是基本数据类型，这种情况下在类的外部不必创建该类的实例就可以直接使用
> - 静态变量是一个引用。这种情况比较特殊，主要问题是由于静态变量是一个对象的引用，那么必须初始化这个对象之后才能将引用指向它。
> - 因此如果要把一个引用定义成static的，就必须在定义的时候就对其对象进行初始化。
>
> [java] 
>
>  
>
> 1. **public \**class TestForStaticObject{\**** 
> 2. **static testObject o = \**new testObject (); //定义一个静态变量并实例化\**** 
> 3. **public \**static \*\*void main(String args[]){\*\**\*** 
> 4. //在main中直接以“类名.静态变量名.方法名”的形式使用testObject的方法 
> 5. } 
> 6. } 

- 静态方法

 与类变量不同，方法（静态方法与实例方法）在内存中只有一份，无论该类有多少个实例，都共用一个方法。

  静态方法与实例方法的不同主要有：

> - 静态方法可以直接使用，而实例方法必须在类实例化之后通过对象来调用。
> - 在外部调用静态方法时，可以使用“类名.方法名”或者“对象名.方法名”的形式。
> - 实例方法只能使用这种方式对象名.方法名。
> - 静态方法只允许访问静态成员。而实例方法中可以访问静态成员和实例成员。
> - 静态方法中不能使用this（因为this是与实例相关的）。

- 静态代码块

 在java类中，可以将某一块代码声明为静态的。

> [java] 
>
>  
>
> 1. **static {** 
> 2. //静态代码块中的语句 
> 3. } 
>
>  
>
> 静态代码块主要用于类的初始化。它只执行一次，并且在同属于一个类的main函数之前执行。
>
> 静态代码块的特点主要有：
>
> - 静态代码块会在类被加载时自动执行。
> - 静态代码块只能定义在类里面，不能定义在方法里面。
> - 静态代码块里的变量都是局部变量，只在块内有效。
> - 一个类中可以定义多个静态代码块，按顺序执行。
> - 静态代码块只能访问类的静态成员，而不允许访问实例成员。
>
>  静态代码块和静态函数的区别
>
> java 静态代码块：
>
> 一般情况下,如果有些代码必须在项目启动前就执行的时候,需要使用静态代码块,这种代码是主动执行的，它只执行一次，并且在同属于一个类的main函数之前执行。
>
> 静态函数：
>
> 需要在项目启动的时候就初始化,在不创建对象的情况下,其他程序来调用的时候,需要使用静态方法,这种代码是被动执行的.
>
> 注意：
>
> （1）静态变量是属于整个类的变量而不是属于某个对象的。注意不能把任何方法体内的变量声明为静态，例如：
> fun()
> {
> static int i=0;//非法。
> }
>
> （2）一个类可以使用不包含在任何方法体中的静态代码块，当类被载入时，静态代码块被执行，且只被执行一次，静态块常用来执行类属性的初始化。例如：
> static
> {
> }
>
> 主程序类中的的静态变量先于静态代码块初始化，其后进入主函数类(程序入口处)，其后根据静态函数的调用情况，才能选择性的初始化。
>
> 
>
> 参考大神博客：https://www.cnblogs.com/lubocsu/p/5099558.html
>
> ​          https://www.cnblogs.com/ipetergo/p/6441310.html



















## 一、静态代码块

　　1.在java类中(方法中不能存在静态代码块)使用static关键字和{}声明的代码块：

```
public class CodeBlock{
     static{
        System.out.println("静态代码块");  
}      
}
```

　　2.执行时机

　　　　静态代码块在类被加载的时候就运行了，而且只运行一次，并且优先于各种代码块以及构造函数。如果一个类中有多个静态代码块，就会按照书写的顺序执行。

　　3.静态代码块的作用：

　　　　一般情况下，如果有些代码需要在项目启动的时候执行，这时就需要静态代码快，比如一个项目启动需要加载很多配置文件等资源，就可以都放在静态代码块中。

　　4.静态代码块不能存在于任何方法体中

　　　　这个很好理解，首先要明确静态代码块是在类加载的时候就运行了，我们分情况进行讨论：

　　　　(1)对于普通方法，由于普通方法是通过加载类，然后new出实例化对象，通过对象才能运行这个方法，而静态代码块只需要加载类之后就能运行了。

　　　　(2)对于静态方法，在类加载的时候，静态方法就已经加载了，但是我们必须通过类名或者对象名才能进行访问，也就是说相对于静态代码块，静态代码块是主动运行的，而静态方法是被动运行的。

　　　　(3)不管哪种方法，我们需要明确的是静态代码块的存在在类加载的时候就自动运行了，而放在不管是普通方法中还是静态方法中，都是不能自动运行的。

　　5.静态代码块不能访问普通代变量

　　　　(1)这个理解思维同上，普通代码块只能通过对象来进行调用，而不能防砸静态代码块中。

## 二、构造代码块

　　1.格式：java类中使用{}声明的代码块(和静态代码块的区别是少了static关键字)

[![复制代码](../../../art/jiava%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E5%9D%97%E9%9D%99%E6%80%81%E6%96%B9%E6%B3%95%E9%9D%99%E6%80%81%E5%8F%98%E9%87%8F%E6%9E%84%E9%80%A0%E4%BB%A3%E7%A0%81%E5%9D%97%E6%99%AE%E9%80%9A%E4%BB%A3%E7%A0%81%E6%AC%BE/copycode.gif)](javascript:void(0);)

```
1 public class codeBlock {
2     static {
3         System.out.println("静态代码块");
4     }
5     {
6         System.out.println("构造代码块");
7     }
8 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　2.执行时机

　　构造代码块在创建对象的时候被调用，每创建一次对象都会调用一次，但是优先于构造函数执行，需要注意的是，听名字我们就知道，构造代码块不是优先于构造函数执行的，而是依托于构造函数，也就是说，如果你不实例化对象，构造代码块是不会执行的。怎么理解呢？先看看下面的代码段：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
public class codeBlock {
    static {
        System.out.println("静态代码块");
    }
    {
        System.out.println("构造代码块");
    }
    public codeBlock(){
        System.out.println("无参构造函数");
    }
    public codeBlock(String str){
        System.out.println("有参构造函数");
    }

}
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　我们反编译生成的.class文件

![img](../../../art/jiava%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E5%9D%97%E9%9D%99%E6%80%81%E6%96%B9%E6%B3%95%E9%9D%99%E6%80%81%E5%8F%98%E9%87%8F%E6%9E%84%E9%80%A0%E4%BB%A3%E7%A0%81%E5%9D%97%E6%99%AE%E9%80%9A%E4%BB%A3%E7%A0%81%E6%AC%BE/1441368-20190307084425085-1908147327.png)

 

 　3.构造代码块的作用：

　　　　(1)和构造函数的作用类似，都能够对象记性初始化，并且只要创建一个对象，构造代码块都会执行一次。但是反过来，构造函数则不会再每个对象创建的时候都执行(多个构造函数的情况下，建立对象时传入的参数不同则初始化使用对应的构造函数)

　　　　(2)利用每次创建对象的时候都会提前调用一次构造代码块特性，我们做诸如统计创建对象的次数等功能。

## 三、构造函数

　　1.构造函数必须和类名完全相同。在java中，普通函数可以和构造函数同名，但是必须带有返回值。

　　2.构造函数的功能主要用于在类创建时定义初始化的状态。没有返回值，也不能用void来进行修饰。这就保证额它不仅什么也不用自动返回，而且根本不能有任何选择，而其他方法都有返回值，尽管方法体本身不会自动返回什么，但是仍然可以返回一些东西，而这些东西可能是不安全的；

　　3.构造函数不能被直接调用，必须通过New运算符在创建对象的时才会自动调用；而一般的方法是在程序执行到它的时候被调用的

　　4.当定义一个类的时候，通常情况下都会现实该类的构造函数，并在函数中指定初始化的工作也可省略，不过Java编译期会提供一个默认的构造函数，此默认的构造函数是不带参数的，即空参构造。而一般的方法不存在这一特点。

## 四、普通代码块

　　1.普通代码块和构造代码块的区别是，构造代码块是在类中定义的，而普通代码块是在方法体重定义的。并且普通代码块的执行顺序和书写顺序是一致的

```
public class sayHelllo {
    {
        System.out.println("普通代码块");
    }
}
```

## 五、执行顺序

　　1.静态代码块>构造代码块>构造函数>普通代码块

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
public class codeBlock {
    static {
        System.out.println("静态代码块");
    }
    {
        System.out.println("构造代码块");
    }
    public codeBlock(){
        System.out.println("无参构造函数");
    }
    public codeBlock(String str){
        System.out.println("有参构造函数");
    }
    public void sayHello(){
        System.out.println("普通代码块");
    }

    public static void main(String[] args) {
        System.out.println("执行了main方法");

        new codeBlock().sayHello();

        System.out.println("---------------------------");

        new codeBlock().sayHello();


    }

}
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　![img](../../../art/jiava%20%E9%9D%99%E6%80%81%E4%BB%A3%E7%A0%81%E5%9D%97%E9%9D%99%E6%80%81%E6%96%B9%E6%B3%95%E9%9D%99%E6%80%81%E5%8F%98%E9%87%8F%E6%9E%84%E9%80%A0%E4%BB%A3%E7%A0%81%E5%9D%97%E6%99%AE%E9%80%9A%E4%BB%A3%E7%A0%81%E6%AC%BE/1441368-20190307091009680-1897549312.png)

## 六、实例

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
public class Test1 {
    static{
        int x = 5 ;
    }
    static int x ,y;
    public static void main(String args[]){
        x--;
        myMethod();//运行myMethod方法，x之前是-1，开始调用myMethod()函数
        System.out.println(x+y++ +x);
    }
    public static void myMethod(){
        y=x++ + ++x;　　//步骤2：这个地方的调用要注意：x++ + ++x 是将-1先自加然后加1，得到y=0　　　　 System.out.println(y);　　　　 System.out.println(x);步骤3：此时x=1

    }
}//最终的运行结果为2
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

执行过程分析：

　　1.static { int x =5;}　　//静态代码块，在类加载的时候回被加载并且执行，但是由于是局部变量，所以x= 5 不影响后面的值

　　2.static int x,y;　　这个时候会将x和y进行初始化，得到x=0；y=0