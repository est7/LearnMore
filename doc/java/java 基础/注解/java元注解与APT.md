### 1 元注解

#### 1.1 什么是元注解

所谓元注解其实就是可以注解到别的注解上的注解,被注解的注解称之为组合注解,组合注解具备其上元注解的功能.

#### 1.2 四种元注解

在JDK中提供了4个标准的用来对注解类型进行注解的注解类，我们称之为 meta-annotation（元注解），他们分别是:

- @Target
- @Retention
- @Documented
- @Inherited

我们可以使用这4个元注解来对我们自定义的注解类型进行注解.

#### 1.3 @Target注解

Target注解的作用是：描述注解的使用范围(即被修饰的注解可以用在什么地方).

Target注解用来说明那些被它所注解的注解类可修饰的对象范围：注解可以用于修饰 packages、types（类、接口、枚举、注解类）、类成员（方法、构造方法、成员变量、枚举值）、方法参数和本地变量（如循环变量、catch参数），在定义注解类时使用了@Target 能够更加清晰的知道它能够被用来修饰哪些对象，它的取值范围定义在ElementType 枚举中.
 源码

```java
@Documented  
@Retention(RetentionPolicy.RUNTIME)  
@Target(ElementType.ANNOTATION_TYPE)  
public @interface Target {  
    ElementType[] value();  
}  
```

ElementType

```java
public enum ElementType {
 
    TYPE, // 类、接口、枚举类
 
    FIELD, // 成员变量（包括：枚举常量）
 
    METHOD, // 成员方法
 
    PARAMETER, // 方法参数
 
    CONSTRUCTOR, // 构造方法
 
    LOCAL_VARIABLE, // 局部变量
 
    ANNOTATION_TYPE, // 注解类
 
    PACKAGE, // 可用于修饰：包
 
    TYPE_PARAMETER, // 类型参数，JDK 1.8 新增
 
    TYPE_USE // 使用类型的任何地方，JDK 1.8 新增
 
}
```

#### 1.4 @Retention

Reteniton注解的作用是：描述注解保留的时间范围(即：被描述的注解在它所修饰的类中可以被保留到何时).

Reteniton注解用来限定那些被它所注解的注解类在注解到其他类上以后，可被保留到何时，一共有三种策略，定义在RetentionPolicy枚举中.
 RetentionPolicy

```java
public enum RetentionPolicy {
 
    SOURCE,    // 源文件保留
    CLASS,       // 编译期保留，默认值
    RUNTIME   // 运行期保留，可通过反射去获取注解信息
}
```

生命周期长度 SOURCE < CLASS < RUNTIME ，前者能作用的地方后者一定也能作用。如果需要在运行时去动态获取注解信息，那只能用 RUNTIME 注解；如果要在编译时进行一些预处理操作，比如生成一些辅助代码（如 ButterKnife），就用 CLASS注解；如果只是做一些检查性的操作，比如 @Override 和 @SuppressWarnings，则可选用 SOURCE 注解。

#### 1.5 @Documented

Documented注解的作用是：描述在使用 javadoc 工具为类生成帮助文档时是否要保留其注解信息。
 这里验证@Documented的作用,我们创建一个自定义注解:

```java
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface MyDocumentedt {

    public String value() default "这是@Documented注解为文档添加的注释";

}
```

然后创建一个测试类,在方法和类上都加入自定义的注解

```java
@MyDocumentedt
public class MyDocumentedTest {
    /**
     * 测试 document
     * @return String the response
     */
    @MyDocumentedt
    public String test(){
        return "sdfadsf";
    }
}
```

打开java文件所在的目录下,打开命令行输入:

```java
javac .\MyDocumentedt.java .\MyDocumentedTest.java
复制代码
javadoc -d doc .\MyDocumentedTest.java .\MyDocumentedt.java
复制代码
```



![img](../../../../art/16d3967dc9262748tplv-t2oaga2asx-zoom-in-crop-mark1304000.awebp)

打开生成的doc文件夹,打开`index.html`,可以发现在类和方法上都保留了 MyDocumentedt 注解信息。



#### 1.6 @Inherited

Inherited注解的作用是：使被它修饰的注解具有继承性（如果某个类使用了被@Inherited修饰的注解，则其子类将自动具有该注解）。
 通过代码来进行验证,创建一个自定义注解

```java
@Target({ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface MyInherited {
}
```

验证

```java
@MyInherited
public class A {
    public static void main(String[] args) {
        System.out.println(A.class.getAnnotation(MyInherited.class));
        System.out.println(B.class.getAnnotation(MyInherited.class));
        System.out.println(C.class.getAnnotation(MyInherited.class));
    }
}

class B extends A{
}

class C extends B{
}
```

执行main方法,从控制台中可以看到打印的信息



![img](../../../../art/16d3ce0690be8515tplv-t2oaga2asx-zoom-in-crop-mark1304000.awebp)



#### 1.7 重复注解 @Repeatable

**重复注解**：即允许在同一申明类型（类，属性，或方法）前多次使用同一个类型注解。

在java8 以前，同一个程序元素前最多只能有一个相同类型的注解；如果需要在同一个元素前使用多个相同类型的注解，则必须使用注解“容器”。 java8之前的做法

```java
public @interface Roles {
    Role[] roles();
}

public @interface Roles {
    Role[] value();
}

public class RoleAnnoTest {
    @Roles(roles = {@Role(roleName = "role1"), @Role(roleName = "role2")})
    public String doString(){
        return "";
    }
}

```

java8之后增加了重复注解,使用方式如下:

```java
public @interface Roles {
    Role[] value();
}

@Repeatable(Roles.class)
public @interface Role {
    String roleName();
}

public class RoleAnnoTest {
    @Role(roleName = "role1")
    @Role(roleName = "role2")
    public String doString(){
        return "";
    }
}

```

不同的地方是，创建重复注解 Role 时，加上@Repeatable，指向存储注解 Roles，在使用时候，直接可以重复使用 Role 注解。从上面例子看出，java 8里面做法更适合常规的思维，可读性强一点。但是，仍然需要定义容器注解。

两种方法获得的效果相同。重复注解只是一种简化写法，这种简化写法是一种假象：多个重复注解其实会被作为“容器”注解的 value 成员的数组元素处理。

#### 1.8 类型注解

Java8 为 ElementType 枚举增加了TYPE_PARAMETER、TYPE_USE两个枚举值，从而可以使用 @Target(ElementType_TYPE_USE) 修饰注解定义，这种注解被称为类型注解，可以用在任何使用到类型的地方。

在 java8 以前，注解只能用在各种程序元素（定义类、定义接口、定义方法、定义成员变量…）上。从 java8 开始，类型注解可以用在任何使用到类型的地方。

**TYPE_PARAMETER**：表示该注解能写在类型参数的声明语句中。

**TYPE_USE**：表示注解可以再任何用到类型的地方使用，比如允许在如下位置使用：

- 创建对象（用 new 关键字创建）
- 类型转换
- 使用 implements 实现接口
- 使用 throws 声明抛出异常

```java
public class TypeUserTest {

    public static void main(String[] args) {
        String str = "str";
        Object obj = (@isNotNull Object) str;
    }

}

@Target(ElementType.TYPE_USE)
@interface isNotNull{
}
```

这种无处不在的注解，可以让编译器执行更严格的代码检查，从而提高程序的健壮性。

### 2 自定义注解

通过上面的学习已经初步了解了元注解是怎么一回事,下面撸一个自定义注解来融会贯通. 自定义注解

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Name {

    public String value() default "";
}
复制代码
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sex {

    public enum GenderType {
        Male("男"),
        Female("女");
        private String genderStr;
        private GenderType(String arg0) {
            this.genderStr = arg0;
        }
        @Override
        public String toString() {
            return genderStr;
        }
    }
    GenderType gender() default GenderType.Male;
}
```

使用自定义注解的实体类

```java
@Data
public class User {

    @Name(value = "wtj")
    public String name;
    public String age;
    @Sex(gender = Sex.GenderType.Male)
    public String sex;

}
```

测试

```
public class AnnotionUtils {

    public static String getInfo(Class<?> cs){
        String result = "";
        //通过反射获取所有声明的字段
        Field[] declaredFields = cs.getDeclaredFields();
        //获取所有字段
        for (Field field : declaredFields){
            if(field.isAnnotationPresent(Name.class)){
                //获取程序元素上的注解
                Name annotation = field.getAnnotation(Name.class);
                String value = annotation.value();
                result += (field.getName() + ":" + value + "\n");
            }
            if(field.isAnnotationPresent(Sex.class)){
                Sex annotation = field.getAnnotation(Sex.class);
                String value = annotation.gender().name();
                result += (field.getName() + ":" + value + "\n");
            }
        }
        return result;
    }

    public static void main(String[] args){
        String info = getInfo(User.class);
        System.out.println(info);
    }

}
复制代码
```

main方法运行后就可以在控制台中看到使用注解时传入的数据了.
 上面就是一个简单的注解使用的demo,当然,在实际工作中使用的会相对复杂,这就需要我们根据业务需求及代码需求进行封装和使用自定义注解了.