# Point cut


项目下载在 Spring 中, 所有的方法都可以认为是 joinpoint, 但是我们并不希望在所有的方法上都添加 Advice, 而 pointcut 的作用就是提供一组规则(使用 AspectJ pointcut expression language 来描述) 来匹配joinpoint, 给满足规则的 joinpoint 添加 Advice.

# 声明 pointcut

一个 pointcut 的声明由两部分组成:

一个方法签名, 包括方法名和相关参数
一个 pointcut 表达式, 用来指定哪些方法执行是我们感兴趣的(即因此可以织入 advice).
在@AspectJ 风格的 AOP 中, 我们使用一个方法来描述 pointcut, 即:
@Pointcut(“execution(* com.ailianshuo.springaop.sample03..*.*(..))”)

第一个表示匹配任意的方法返回值，..(两个点)表示零个或多个，上面的第一个..表示service包及其子包,第二个表示所有类,第三个*表示所有方法，第二个..表示方法的任意参数个数

```java
/**
* 定义一个切点然后复用这个方法必须无返回值.
*这个方法本身就是 pointcut signature, pointcut 表达式使用@Pointcut 注解指定.
*上面我们简单地定义了一个 pointcut, 这个 pointcut 所描述的是: 匹配所有在包   *com.ailianshuo.springaop.sample03.Math 下的所有方法的执行.
 */
@Pointcut("execution(* com.ailianshuo.springaop.sample03.Math.*(..))")   // 切点表达式      
public void pointcutMath(){// 切点前面
} 

@Before("pointcutMath()")
public void before(JoinPoint jp){
    //System.out.println(jp.getSignature().getName());
    System.out.println("----------before advice----------");
} 

@After("pointcutMath()")
public void after_execution(JoinPoint jp){
    System.out.println("----------after execution advice----------");
}
```



# 切点函数

切点函数可以定位到准确的横切逻辑位置
AspectJ 的切点表达式由标志符(designator)和操作参数组成. 如 “execution( greetTo(..))” 的切点表达式, execution 就是 标志符, 而圆括号里的 greetTo(..) 就是操作参数

@AspectJ使用AspectJ专门的切点表达式描述切面，Spring所支持的AspectJ表达式可分为四类:
方法切点函数：通过描述目标类方法信息定义连接点。
方法参数切点函数：通过描述目标类方法入参信息定义连接点。
目标类切点函数：通过描述目标类类型信息定义连接点。
代理类切点函数：通过描述代理类信息定义连接点。

常见的AspectJ表达式函数：
execution()：满足匹配模式字符串的所有目标类方法的连接点
@annotation()：任何标注了指定注解的目标方法链接点
args()：目标类方法运行时参数的类型指定连接点
@args()：目标类方法参数中是否有指定特定注解的连接点
within()：匹配指定的包的所有连接点
target()：匹配指定目标类的所有方法
@within()：匹配目标对象拥有指定注解的类的所有方法
@target()：匹配当前目标对象类型的执行方法，其中目标对象持有指定的注解
this()：匹配当前AOP代理对象类型的所有执行方法
最常用的是：execution(<修饰符模式>?<返回类型模式><方法名模式>(<参数模式>)<异常模式>?)切点函数，可以满足多数需求。

优先执行顺序：
annotation –> args –> execution –> this –> within

下面使用Spring实现AOP方式之二：使用注解配置 Spring AOP 中的样例继续开发讲解

样例
创建要被代理的StrUtil类

```java
package com.ailianshuo.springaop.sample03;

import org.springframework.stereotype.Component;

@Component("strUtil")
public class StrUtil {
    public void show(){
        System.out.println("Hello StrUtil!");
    }
}
```

测试代码

~~~java
package com.ailianshuo.springaop.sample03;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**

 * AspectJ切点函数

 * 切点函数可以定位到准确的横切逻辑位置

 * @author ailianshuo

 * 2017年7月25日 下午11:42:57
   */
   public class Test {

   public static void main(String[] args) {
       ApplicationContext ctx = new ClassPathXmlApplicationContext("aopsample03.xml");
       Math math = ctx.getBean("math", Math.class);
       int n1 = 100, n2 = 5;
       System.out.println("****************************** Add ******************************");
       math.add(n1, n2);
       System.out.println("");
       System.out.println("****************************** sub ******************************");

   ```
    * 
   
          math.sub(n1, n2);
          
          System.out.println("");
          System.out.println("****************************** pow ******************************");
          
          math.pow(n1 );
   
   
           StrUtil strUtil=ctx.getBean("strUtil",StrUtil.class);
       
           System.out.println("");
           System.out.println("****************************** strUtil ******************************");
           strUtil.show();
   
   
   
   }
   ```

   
~~~

execution切点函数
execution()：满足匹配模式字符串的所有目标类方法的连接点
execution(<修饰符模式>?<返回类型模式><方法名模式>(<参数模式>)<异常模式>?)

```java
package com.ailianshuo.springaop.sample03;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
/**

 *  通知类，横切逻辑

 * @author ailianshuo

 * 2017年7月25日 下午5:21:42

 * */
   @Component
   @Aspect
   public class Advices {
   @Before("execution(* com.ailianshuo.springaop.sample03.Math.*(..))")
   public void before(JoinPoint jp){
       System.out.println(jp.getSignature().getName());
       System.out.println("----------before advice----------");
   }

   /**

    * execution()：满足匹配模式字符串的所有目标类方法的连接点
    * com.ailianshuo.springaop.sample03包下所有类的所有方法被切入
    * @param jp
      */ 
      @After("execution(* com.ailianshuo.springaop.sample03.*.*(..))")
      public void after_execution(JoinPoint jp){
       System.out.println("----------after execution advice----------");
      }



}
```

运行结果：

```java
**************************** Add ******************************
add
----------before advice----------
10+5=15
----------after execution advice----------

****************************** sub ******************************
sub
----------before advice----------
10-5=5
----------after execution advice----------
****************************** pow ******************************
pow
----------before advice----------
10*10=100
----------after execution advice----------
```

****************************** strUtil ******************************
Hello StrUtil!

```
----------after execution advice----------
within切点函数
within()：匹配指定的包的所有连接点，匹配目标对象拥有指定注解的类的所有方法

/**
     * within()：匹配指定的包的所有连接点
     * com.ailianshuo.springaop.sample03包下所有类的所有方法被切入
     * @param jp
      */
    @After("within(com.ailianshuo.springaop.sample03.*)")
    public void after_within(JoinPoint jp){
        System.out.println("----------after within advice----------");
    }
```


运行结果：

****************************** Add ******************************
----------before advice----------
10+5=15
----------after within advice----------

****************************** sub ******************************
----------before advice----------
10-5=5
----------after within advice----------

****************************** pow ******************************
----------before advice----------
10*10=100
----------after within advice----------

****************************** strUtil ******************************
Hello StrUtil!
----------after within advice----------

this切点函数
匹配当前AOP代理对象类型的所有执行方法


    @After("this(com.ailianshuo.springaop.sample03.Math)")
    public void after_this_Math(JoinPoint jp){
        System.out.println("----------after this Math advice----------");
    }


    @After("this(com.ailianshuo.springaop.sample03.StrUtil)")
    public void after_this_StrUtil(JoinPoint jp){
        System.out.println("----------after this StrUtil advice----------");
    }

运行结果：

****************************** Add ******************************
----------before advice----------
10+5=15
----------after this Math advice----------

****************************** sub ******************************
----------before advice----------
10-5=5
----------after this Math advice----------

****************************** pow ******************************
----------before advice----------
10*10=100
----------after this Math advice----------

****************************** strUtil ******************************
Hello StrUtil!
----------after this StrUtil advice----------

args切点函数

```
@After("args(int,int)")
    public void after_args(JoinPoint jp){
        System.out.println("----------after args advice----------");
    }
```


运行结果

****************************** Add ******************************
----------before advice----------
10+5=15
----------after args advice----------

****************************** sub ******************************
----------before advice----------
10-5=5
----------after args advice----------

****************************** pow ******************************
----------before advice----------
10*10=100

****************************** strUtil ******************************
Hello StrUtil!
annotation切点函数
任何标注了指定注解的目标方法链接点

先自定义一个可以注解在方法上的注解

```
package com.ailianshuo.springaop.sample03;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 

 * @author ailianshuo
 * 2017年7月26日 上午10:45:17
   */
   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   @Documented
   public @interface MyAnno {

}

Advices.java

/**
     * 
     * @annotation切点函数
     * @annotation()：任何标注了指定注解的目标方法链接点
     * 要求方法必须被注解com.ailianshuo.springaop.sample03.MyAnno才会被织入横切逻辑
     * @param jp
    */
    @After("@annotation(com.ailianshuo.springaop.sample03.MyAnno)")
    public void after_annotation(JoinPoint jp){
        System.out.println("----------after annotation advice----------");
    }

StrUtil .java

 @MyAnno
    public void showAnno(){
        System.out.println("Hello MyAnno StrUtil!");
    }
```


运行结果：

****************************** Add ******************************
----------before advice----------
10+5=15

****************************** sub ******************************
----------before advice----------
10-5=5

****************************** pow ******************************
----------before advice----------
10*10=100

****************************** strUtil ******************************
Hello StrUtil!

****************************** showAnno ******************************
Hello MyAnno StrUtil!
----------after annotation advice----------