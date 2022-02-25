# 理解Java注解

**注解**就相当于对源代码打的**标签**，给代码打上**标签**和删除**标签**对源代码没有任何影响。有的人要说了，你尽几把瞎扯，没有影响，打这些标签干毛线呢？其实不是这些标签自己起了什么作用，而且外部工具通过访问这些标签，然后根据不同的标签做出了相应的处理。这是**注解**的精髓，理解了这一点一切就变得不再那么神秘。
 例如我们写代码用的**IDE**（例如 **IntelliJ Idea**）,它检查发现某一个方法上面有`@Deprecated`这个注解，它就会在所有调用这个方法的地方将这个方法标记为删除。

访问和处理**Annotation**的工具统称为**APT(Annotation Processing Tool)**

# 基本语法

注解可以分为以下3类

## 基本注解

Java内置的注解共有5个

**`@Override`**：让编译器检查被标记的方法，保证其重写了父类的某一个方法。此注解只能标记方法。源码如下：



```kotlin
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

**`@Deprecated`**：标记某些程序元素已经**过时**，程序员请不要再使用了。源码如下：



```kotlin
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE})
public @interface Deprecated {
}
```

**`@SuppressWarnings`** ：告诉编译器不要给老子显示**警告**，老子不想看，老子清楚的知道自己在干什么。源码如下：



```css
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.SOURCE)
public @interface SuppressWarnings {
    String[] value();
}           
```

其内部有一个String数组，根据传入的值来取消相应的警告：
 **deprecation**：使用了不赞成使用的类或方法时的警告；
 **unchecked**：执行了未检查的转换时的警告，例如当使用集合时没有用泛型 (Generics) 来指定集合保存的类型;
 **fallthrough**：当 Switch 程序块直接通往下一种情况而没有 Break 时的警告;
 **path**：在类路径、源文件路径等中有不存在的路径时的警告;
 **serial**：当在可序列化的类上缺少 serialVersionUID 定义时的警告;
 **finally**：任何 finally 子句不能正常完成时的警告;
 **all**：关于以上所有情况的警告。

**`@SafeVarargs`(Java7 新增)** ：`@SuppressWarnings`可以用在各种需要取消警告的地方，而 `@SafeVarargs`主要用在取消**参数**的警告。就是说编译器如果检查到你对**方法参数**的操作，有可能发生问题时会给出**警告**，但是你很自（任）性，老子不要警告，于是你就加上了这个标签。源码如下：



```kotlin
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface SafeVarargs {}
```

其实这个注解是专为取消**堆污染**警告设置的，因为Java7会对可能产生**堆污染**的代码提出警告，什么是堆污染？且看下面代码



```dart
 @SafeVarargs
 private static void method(List<String>... strLists) {
     List[] array = strLists;
     List<Integer> tmpList = Arrays.asList(42);
     array[0] = tmpList; //非法操作，但是没有警告
     String s = strLists[0].get(0); //ClassCastException at runtime!
 }
```

如果不使用  `@SafeVarargs`，这个方法在编译时候是会产生警告的 ： **“...使用了未经检查或不安全的操作。”**,用了就不会有警告，但是在运行时会抛异常。

**`@FunctionalInterface`(Java8 新增)**： 标记型注解，告诉编译器检查被标注的接口是否是一个**函数接口**，即检查这个接口是否只包含一个抽象方法，只有函数接口才可以使用`Lambda`表达式创建实例。源码如下：



```kotlin
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionalInterface {}
```

## 元注解

用来给其他注解打标签的注解，即用来注解其他注解的注解。元注解共有6个。从上面的基本注解的源代码中就会看到使用了元注解来注解自己。

### **@Retention**：

用于指定被此**元注解**标注的注解的保留时长，源代码如下：



```kotlin
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.ANNOTATION_TYPE)
    public @interface Retention {
        RetentionPolicy value();
    }
```

从源代码中可以看出，其有一个属性`value`,返回一个枚举`RetentionPolicy` 类型，有3种类型：

#### 	**RetentionPolicy.SOURCE**

​		注解信息只保留在源代码中，编译器编译源码时会将其直接丢弃。

#### 	**RetentionPolicy.CLASS**

​		注解信息保留在`class`文件中，但是虚拟机`VM`不会维护默认值。

#### 	**RetentionPolicy.RUNTIME**

​	注解信息保留在`class`文件中，而且`VM`也会持有此注解信息，所以可以通过反射的方式获得注解信息。

### **@Target**：

用于指定被此**元注解**标注的注解可以标注的程序元素，源码如下：



```kotlin
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Target {
    ElementType[] value();
}
```

从源码中可以看出，其有一个属性`value`,返回一个枚举`ElementType`类型的数组，这个数组的值就代表了可以使用的程序元素。



```swift
public enum ElementType {
   /**标明该注解可以用于类、接口（包括注解类型）或enum声明*/
   TYPE,

   /** 标明该注解可以用于字段(域)声明，包括enum实例 */
   FIELD,

   /** 标明该注解可以用于方法声明 */
   METHOD,

   /** 标明该注解可以用于参数声明 */
   PARAMETER,

   /** 标明注解可以用于构造函数声明 */
   CONSTRUCTOR,

   /** 标明注解可以用于局部变量声明 */
   LOCAL_VARIABLE,

   /** 标明注解可以用于注解声明(应用于另一个注解上)*/
   ANNOTATION_TYPE,

   /** 标明注解可以用于包声明 */
   PACKAGE,

   /**
    * 标明注解可以用于类型参数声明（1.8新加入）
    */
   TYPE_PARAMETER,

   /**
    * 类型使用声明（1.8新加入)
    */
   TYPE_USE
}
```

例如`@Override`注解使用了 `@Target(ElementType.METHOD)`，那么就意味着，它只能注解方法，不能注解其他程序元素。

当注解未指定Target值时，则此注解可以用于任何元素之上，多个值使用{}包含并用逗号隔开，下面代码表示，此`Annotation`既可以注解构造函数、字段和方法：



```css
@Target(value={CONSTRUCTOR, FIELD, METHOD})
```

**值得注意的是**，`TYPE_PARAMETER`，`TYPE_USE`是Java8 加入的新类型，在Java8之前，只能在声明各种程序元素时使用注解，而`TYPE_PARAMETER`允许使用注解修饰参数类型，`TYPE_USE`允许使用注解修饰任意类型。



```tsx
//TYPE_PARAMETER 修饰类型参数
class A<@Parameter T> { }

//TYPE_USE则可以用于标注任意类型(不包括class)

//用于父类或者接口
class Image implements @Rectangular Shape { }

//用于构造函数
new @Path String("/usr/bin")

//用于强制转换和instanceof检查,注意这些注解中用于外部工具，它们不会对类型转换或者instanceof的检查行为带来任何影响。
String path=(@Path String)input;
if(input instanceof @Path String)

//用于指定异常
public Person read() throws @Localized IOException.

//用于通配符绑定
List<@ReadOnly ? extends Person>
List<? extends @ReadOnly Person>

@NotNull String.class //非法，不能标注class
import java.lang.@NotNull String //非法，不能标注import
```

虽然Java8 提供了类型注解，但是没有提供`APT`,所以需要框架自己实现。

### **@Documented**

​	将被标注的注解生成到`javadoc`中。

**@Inherited**：其让被修饰的注解拥有被继承的能力。如下，我们有一个用`@Inherited`修饰的注解`@InAnnotation`，那么这个注解就拥有了被继承的能力。



```java
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InAnnotation{
}

@InAnnotation
class Base{}

class Son extends Base{}
```

当使用此注解修饰一个基类`Base`, 其子类`Son` 并没有使用任何注解修饰，但是其已经拥有了`@InAnnotation`这个注解，相当于`Son` 已经被`@InAnnotation`修饰了

### **@Repeatable** 

​	使被修饰的注解可以重复的注解某一个程序元素。例如下面的代码中`@ShuSheng`这个自定义注解使用了`@Repeatable`修饰，所以其可以按照下面的语法重复的注解一个类。



```kotlin
@ShuSheng(name="frank",age=18)
@ShuSheng(age = 20)
public class AnnotationDemo{}
```

如何定义一个重复注解呢,如下所示，我们需要先定义一个容器，例如`ShuShengs` ，然后将其作为参数传入`@Repeatable`中。



```kotlin
@Repeatable(ShuShengs.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShuSheng {
    String name() default "ben";
    int age();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShuShengs {
    ShuSheng[] value();
}
```

## 自定义注解

通过前面的讲解，很容易得出如何自定义一个注解。注解是以关键字`@interface` 来定义的，下面我们自定义一个注解。
 注解按照有无成员变量可以分为：

- 标记Annotation:无成员变量，只利用自身是否存在来提供信息。

  

  ```kotlin
  @Target(ElementType.METHOD)//只能应用于方法上。
  @Retention(RetentionPolicy.RUNTIME)//保存到运行时
  public @interface Test {
  }
  ```

- 元数据Annotation：有一个或者多个成员变量，可以接收外界信息。



~~~kotlin
```
@Target(ElementType.TYPE)//只能应用于类型上，包括类，接口。
@Retention(RetentionPolicy.RUNTIME)//保存到运行时
public @interface Table {
    String name() default "";
}
```
~~~

以上就是我们定义的两种注解，那么如何使用呢



```java
//在类上使用该注解
@Table (name = "MEMBER")
public class Member {
    @Test 
    public void method()
    {...}
}
```

# 如何使用注解

就像我们文章开头说的，当我们使用注解修饰了程序元素后，这种Annotation不会自己起作用，的需要`APT`的帮助，那么这些`APT`就需要读取代码中的属性信息，那么如何读取呢？答案是通过**反射**！

`Annotation`接口是所有注解的父接口（需要通过反编译查看），在`java.lang.reflect` 反射包下存在一个叫`AnnotatedElement`接口，其表示程序中可以接受注解的程序元素，例如 类，方法，字段，构造函数，包等等。而Java为使用反射的主要类实现了此接口，如反射包内的Constructor类、Field类、Method类、Package类和Class类。

当我们通过反射技术获取到反射包内的那些类型的实例后，就可以使用`AnnotatedElement`接口的中的API方法来获取注解的信息了。

- `<T extends Annotation> T getAnnotation(Class<T> annotationClass);`  ：  返回该元素上存在的指定类型的注解，如果不存在则返回 null。
- `default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass){}` ：返回该元素上存在的**直接修饰**该元素的指定类型的注解，如果不存在则返回null.
- `Annotation[] getAnnotations();`：返回该元素上存在的所有注解。
- `Annotation[] getDeclaredAnnotations();`：返回该元素上存在的**直接修饰**该元素的所有注解。
- `default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass){}`：该方法功能与前面`getAnnotation`方法类似，但是由于**Java8** 加入了重复注解功能，因此需要此方法获取修饰该程序元素的指定类型的多个`Annotation`

## 获取注解简单示例

首先我们定义了两个注解`@Master`与`@ShuSheng`，`@ShuSheng`是一个可重复注解



```kotlin
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Master {
}


@Repeatable(ShuShengs.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShuSheng {
    String name() default "ben";
    int age();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ShuShengs {
    ShuSheng[] value();
}
```

然后我们定义了两个类，使用定义好的注解来修饰，如下



```java
@Master
public class AnoBase {
}

@ShuSheng(name="frank",age=18)
@ShuSheng(age = 20)
public class AnnotationDemo extends AnoBase{
}
```

最后我们来调用相关函数获取相应的结果
 private static void getAnnotation()



```kotlin
   {
       Class<?> cInstance=AnnotationDemo.class;

       //获取AnnotationDemo上的重复注解
       ShuSheng[] ssAons= cInstance.getAnnotationsByType(ShuSheng.class);
       System.out.println("重复注解:"+Arrays.asList(ssAons).toString());

       //获取AnnotationDemo上的所有注解，包括从父类继承的
       Annotation[] allAno=cInstance.getAnnotations();
       System.out.println("所有注解:"+Arrays.asList(allAno).toString());

       //判断AnnotationDemo上是否存在Master注解
       boolean isP=cInstance.isAnnotationPresent(Master.class);
       System.out.println("是否存在Master: "+isP);
   }
```

执行结果如下：



```dart
重复注解:[@top.ss007.ShuSheng(name=frank, age=18), @top.ss007.ShuSheng(name=ben, age=20)]
所有注解:[@top.ss007.ShuShengs(value=[@top.ss007.ShuSheng(name=frank, age=18), @top.ss007.ShuSheng(name=ben, age=20)])]
是否存在Master: false
```

## 自定义注解处理器（APT）

了解完注解与反射的相关API后，就可以更进一步。下面的实例自定义了一个`APT`,完成通过注解构建`SQL`语句的功能。此处代码来自[此处](https://blog.csdn.net/javazejian/article/details/71860633)。下面代码要求对数据库有初步认识。

先定义相关的注解



```tsx
/**
 * 用来注解表
 */
@Target(ElementType.TYPE)//只能应用于类上
@Retention(RetentionPolicy.RUNTIME)//保存到运行时
public @interface DBTable {
    String name() default "";
}

/**
 * 注解Integer类型的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLInteger {
    //该字段对应数据库表列名
    String name() default "";
    //嵌套注解
    Constraints constraint() default @Constraints;
}

/**
 * 注解String类型的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLString {
    //对应数据库表的列名
    String name() default "";
    //列类型分配的长度，如varchar(30)的30
    int value() default 0;
    Constraints constraint() default @Constraints;
}

/**
 * 约束注解
 */
@Target(ElementType.FIELD)//只能应用在字段上
@Retention(RetentionPolicy.RUNTIME)
public @interface Constraints {
    //判断是否作为主键约束
    boolean primaryKey() default false;
    //判断是否允许为null
    boolean allowNull() default false;
    //判断是否唯一
    boolean unique() default false;
}

/**
 * 数据库表Member对应实例类bean
 */
@DBTable(name = "MEMBER")
public class Member {
    //主键ID
    @SQLString(name = "ID",value = 50, constraint = @Constraints(primaryKey = true))
    private String id;
    
    @SQLString(name = "NAME" , value = 30)
    private String name;
    
    @SQLInteger(name = "AGE")
    private int age;
    
    @SQLString(name = "DESCRIPTION" ,value = 150 , constraint = @Constraints(allowNull = true))
    private String description;//个人描述

   //省略set get.....
}
```

上述定义4个注解，分别是@DBTable(用于类上)、@Constraints(用于字段上)、 @SQLString(用于字段上)、@SQLString(用于字段上)并在Member类中使用这些注解，这些注解的作用的是用于帮助注解处理器生成创建数据库表MEMBER的构建语句，在这里有点需要注意的是，我们使用了嵌套注解@Constraints，该注解主要用于判断字段是否为null或者字段是否唯一。接下来就需要编写我们自己的注解处理器了。



```tsx
public class TableCreator {

  public static String createTableSql(String className) throws ClassNotFoundException {
    Class<?> cl = Class.forName(className);
    DBTable dbTable = cl.getAnnotation(DBTable.class);
    //如果没有表注解，直接返回
    if(dbTable == null) {
      System.out.println(
              "No DBTable annotations in class " + className);
      return null;
    }
    String tableName = dbTable.name();
    // If the name is empty, use the Class name:
    if(tableName.length() < 1)
      tableName = cl.getName().toUpperCase();
    List<String> columnDefs = new ArrayList<String>();
    //通过Class类API获取到所有成员字段
    for(Field field : cl.getDeclaredFields()) {
      String columnName = null;
      //获取字段上的注解
      Annotation[] anns = field.getDeclaredAnnotations();
      if(anns.length < 1)
        continue; // Not a db table column

      //判断注解类型
      if(anns[0] instanceof SQLInteger) {
        SQLInteger sInt = (SQLInteger) anns[0];
        //获取字段对应列名称，如果没有就是使用字段名称替代
        if(sInt.name().length() < 1)
          columnName = field.getName().toUpperCase();
        else
          columnName = sInt.name();
        //构建语句
        columnDefs.add(columnName + " INT" +
                getConstraints(sInt.constraint()));
      }
      //判断String类型
      if(anns[0] instanceof SQLString) {
        SQLString sString = (SQLString) anns[0];
        // Use field name if name not specified.
        if(sString.name().length() < 1)
          columnName = field.getName().toUpperCase();
        else
          columnName = sString.name();
        columnDefs.add(columnName + " VARCHAR(" +
                sString.value() + ")" +
                getConstraints(sString.constraint()));
      }
    }
    //数据库表构建语句
    StringBuilder createCommand = new StringBuilder(
            "CREATE TABLE " + tableName + "(");
    for(String columnDef : columnDefs)
      createCommand.append("\n    " + columnDef + ",");

    // Remove trailing comma
    String tableCreate = createCommand.substring(
            0, createCommand.length() - 1) + ");";
    return tableCreate;
  }

  /**
   * 判断该字段是否有其他约束
   * @param con
   * @return
   */
  private static String getConstraints(Constraints con) {
    String constraints = "";
    if(!con.allowNull())
      constraints += " NOT NULL";
    if(con.primaryKey())
      constraints += " PRIMARY KEY";
    if(con.unique())
      constraints += " UNIQUE";
    return constraints;
  }

  public static void main(String[] args) throws Exception {
    String[] arg={"com.zejian.annotationdemo.Member"};
    for(String className : arg) {
      System.out.println("Table Creation SQL for " +
              className + " is :\n" + createTableSql(className));
    }
  }
}
```

输出结果为：



```cpp
Table Creation SQL for com.zejian.annotationdemo.Member is :
CREATE TABLE MEMBER(
        ID VARCHAR(50) NOT NULL PRIMARY KEY,
        NAME VARCHAR(30) NOT NULL,
        AGE INT NOT NULL,
        DESCRIPTION VARCHAR(150)
        );
```

# 常用场景

**Annotation**，特别是自定义注解，一般是在构建框架或者通用库时候使用的较多。下面列出了些我知道的，其他的欢迎补充。

**Spring**框架：解耦神器。
 **JUnit** :测试框架
 **ButterKnife** :在Android中使用的视图注解框架，Android的小伙伴们都知道。
 **Dagger2**  :依赖注入框架，在Android中用的也比较多。
 **Retrofit**  ：Http网络访问框架，Android网络请求标配。
 **Room**     ：Google 发布的用于Android开发的本地数据库解决方案库。