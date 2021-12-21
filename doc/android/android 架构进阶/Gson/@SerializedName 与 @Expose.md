## 使用@Expose忽略某些域

### @Expose注解模型

在阅读了之前关于处理空值的教程后，你可能会想，你能够将一个Java对象设置为空值，但如何才能够使之不出现在JSON中呢？这在你不想传送你的私有或者敏感数据到网络上时是必要的。不用担心，Gson提供的@Expose注解能轻松解决这个问题。

@Expose注解是可选择的并且提供了两个配置参数：**serialize**和**deserialize**。默认情况下，它们都设置为**true**。因此，你不用强制给每个成员变量添加**@Expose**注解，就像我们之前做的，所有成员变量会默认添加。如果你添加了**@Expose**注解，而没有设置任何值为**false**，该成员变量也会默认包含的。

让我们重新看一下UserSimple类，然后添加**@Expose**注解：



```dart
public class UserSimple {  
    @Expose()
    String name; // equals serialize & deserialize

    @Expose(serialize = false, deserialize = false)
    String email; // equals neither serialize nor deserialize

    @Expose(serialize = false)
    int age; // equals only deserialize

    @Expose(deserialize = false)
    boolean isDeveloper; // equals only serialize
}
```

上例中，序列化的结果将只有**name**和**isDeveloper**会出现在JSON中。其他两个域，即使它们设置了，也不会转换。

在反序列化过程中，Java对象将只会拥有JSON中的**name**和**age**域，**email**和**isDeveloper**将会被忽略。

默认情况下，Gson实例会忽略掉@Expose注解。为了使用它，你需要自定义Gson实例：



```cpp
GsonBuilder builder = new GsonBuilder();  
builder.excludeFieldsWithoutExposeAnnotation();  
Gson gson = builder.create();  
```

遵循此用法的gson才会注重**Expose**注解。

**@Expose**注解使得你可以轻松的空值哪些值可以被序列化和反序列化。建议在需要转换所有值得情况下不要使用**@Expose**标签。它仅仅会毁掉你的模型类。

### Transient

**@Expose**的替代方法是在定义成员变量时添加**transient**关键字。添加了该关键字的成员变量将不会被转换。然而，你并不能像**@Expose**那样完全掌控。你不能使一个方向运行而反方向又不能运行，**transient**将使得该域既不能序列化也不能反序列化。



```java
public class UserSimple {  
    String name;
    String email;
    int age;
    boolean transient isDeveloper; // will not be serialized or deserialized
}
```

## 使用@SerializedName注解改变域名

### @SerializedName注解模型

**@SerializedName**是一个非常有用的注解。它改变了Java-JSON序列化和反序列化过程中的自动匹配。到目前为止，我们经常假设Java模型类和JSON有相同的“名”。然而，并不总是如此。可能是你没有获得继承Java模型类的允许，也可能是你必须遵循公司的命名规则，无论哪种情况，你都可以使用**@SerializedName**以使得Gson能够正确的匹配。

让我们看个例子。我们的**UserSimple**类已经去掉了**@Expose**，它可以映射所有域了。



```dart
public class UserSimple {  
    String name;
    String email;
    boolean isDeveloper;
    int age;
}
```

然而，让我们花点时间想象一下，API实现以及它所返回的JSON已经改变了。我们的API不再会返回**name**而是返回了fullName：



```json
{
  "age": 26,
  "email": "norman@futurestud.io",
  "fullName": "Norman",
  "isDeveloper": true
}
```

不用担心，我们并不需要改变我们的基础代码，我们仅仅需要在我们的模型中添加一个简单的注解：



```dart
public class UserSimple {  
    @SerializedName("fullName")
    String name;
    String email;
    boolean isDeveloper;
    int age;
}
```

在该注解的帮助下，Gson又可以映射良好了，我们又可以享受自动化带来的好处了。

当然，你可以使用**@SerializedName**去遵从你们公司的命名规则，但是依然可以正确匹配任何API。这在命名规则非常不同时是有用的。

## 使用@SerializedName反序列化多个名称

在之前的博客中，我们已经介绍了如何在序列化和反序列化过程中改变一个模型的属性名。如果你的服务器希望接收或者发送的属性名是不一样的，那么你可以使用**@SerializedName**。

在这篇博客中，我们将想你展示，如何实现一个属性对应多个名称的映射。这在你的应用需要与多个API通信时是非常有用的。尽管这些API使用不同的名称描述了相同的事情，你依然可以只使用一个Java模型类。

### 扩展@SerializedName注解模型

在介绍**@SerializedName**的第一篇博客中，我们向你介绍了如下用法：



```dart
public class UserSimple {  
    @SerializedName("fullName")
    String name;

    String email;
    boolean isDeveloper;
    int age;
}
```

你为该模型的一个属性添加了一个注解，并且向序列化和反序列化传递了一个字符串名。

但这并不是全部！**SerializeName**接收两个参数：**value**和**alternate**。前者使用了默认的参数。如果你仅仅传入了一个字符串，那么将该字符串设置给**value**而**alternate**设置为空值。但是你可以给这两个参数传递值：



```java
public class UserSimpleSerializedName {  
    @SerializedName(value = "fullName", alternate = "username")
    private String name;

    private String email;
    private boolean isDeveloper;
    private int age;
} 
```

强调一遍，**value**改变了序列化和反序列化的默认情况！因此，如果Gson根据你的Java模型类创建了一个JSON，它将会使用**value**作为该属性的名。

**alternate**仅仅是作为反序列化中的代选项。Gson将会JSON中的所有名称并且尝试映射到被注解了的属性中的某一个。在上面的模型类中，Gson将会检查到来的JSON中是否含有**fullName**或者**username**。无论是哪一个，都会映射到**name**属性：



```bash
{
    'fullName': 'Norman',
    'email': 'norman@futurestud.io'
} 
```

以及



```bash
{
    'username': 'Norman',
    'email': 'norman@futurestud.io'
}
```

上面两个JSON会映射到相同的Java对象。

如果有多个域匹配一个属性，Gson会使用最后一个遇到的域。例如，在下面的JSON中，**name**属性的值将会设置为**Marcus**，因为该值来的最晚：



```bash
{
    'username': 'Norman',
    'fullName': 'Marcus',
    'email': 'norman@futurestud.io'
}
```

如果你的服务器创建了自相矛盾的JSON，你将不会知道哪个属性会被匹配。