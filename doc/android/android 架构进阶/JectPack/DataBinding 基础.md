# 1：使用

开启配置：
![image-20211105151859012](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111051519722.png)

创建变量：
![image-20211105152018198](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111051520238.png)

创建布局与变量关联

![image-20211105152046058](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111051520106.png)

创建关联：

![image-20211105152227894](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202111051522951.png)

数据绑定databinding完成



DataBinding是Google早在2015年推出的数据绑定框架。使用DataBinding，省去了findViewById，并且能大量减少Activity的代码，让我们代码更有层级性，结构更加的清晰完善。而且有助于防止内存泄漏，并能够自动进行空检测以避免空指针。下面会介绍DataBinding的使用。
 [DataBinding 基础篇一](https://juejin.cn/post/6916418817673396231)
 [DataBinding 进阶篇二 BaseObservable](https://juejin.cn/post/6916422236404449288)
 [DataBinding 进阶篇三 BindingAdapter以及BindingConversion](https://juejin.cn/post/6916422925214023687)
 [DataBinding 进阶篇四 双向数据绑定](https://juejin.cn/post/6916422692295933959)

### 一：集成DataBinding

注意：DataBinding只能运行在Android 4.0（API级别14）或更高版本的设备上。
 在app的build.gradle里添加如下代码：

```kotlin
android {
    ...
    dataBinding {
        enabled = true
    }
}
复制代码
```

### 二：DataBinding 入门使用

#### 1.生成布局

新建一个布局，比如activity_main.xml,在布局文件中，
 选中最外层的布局->按住【Alt+回车键】->点击【Convert to data binding layout】
 或者选中最外层布局->右击->点击[Show Context Actions]->点击[Convert to data binding layout]。 即可生成DataBinding需要的布局规则，如下：

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
    
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

和原始的布局区别，是用layout标签包裹，并且多了个data标签，data标签主要用于声明需要用到的变量以及变量类型

#### 2.简单使用

先声明一个User类

```kotlin
class User(var name: String = "", var password: String = "",var age:Int=0):Serializable{}
复制代码
```

一个activity_main.xml布局

```kotlin
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
        <variable name="userInfo" type="User" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <TextView
            android:id="@+id/tv_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            android:text="@{userInfo.name}"/>

        <TextView
            android:id="@+id/tv_password"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            android:text="@{userInfo.password}"
            app:layout_constraintTop_toBottomOf="@+id/tv_name" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

- 用data来声明要使用到的变量名，类的全路径。

- import是导入类的包名 import type="com.example.jetpackdatabindingtestapp.ui.model.User"。

  如果存在import的类名相同的情况，可以使用alias指定别名

  ```kotlin
   <data>
        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
        <import alias="DetailUser" type="com.example.jetpackdatabindingtestapp.ui.model.detail.User" />
        <variable name="userInfo" type="User" />
        <variable name="detailUser" type="DetailUser" />
    </data>
  复制代码
  ```

- variable是声明变量 variable name="userInfo" type="User"

- 两个TextView里text用到userInfo的name跟password，android:text="@{userInfo.name}"，android:text="@{userInfo.password}"。@{userInfo.password}可以让View去引用到相关的变量，DataBinding会将之映射到对应类的对应属性的getter方法。 由于android:text="@{userInfo.name}在布局里并没有明确的值，有时候我们要在预览视图中看效果，那么其实我们可以加上默认值，默认值不需要添加双引号，添加默认值的方式如下：

  ```kotlin
  // 记住default的值，不需要添加双引号
  android:text="@{userInfo.name,default = 默认昵称}"
  复制代码
  ```

- 写完布局后记得重新编译，系统会自动生成对应的XXXBinding类。类名称基于布局文件的名称，会伊头峰的形式并在末尾添加Binding后缀。以上布局为例，activity_main.xml因此对应生成ActivityMainBinding类。该类位置在于app->build->generated->data_binding_base_class_source_out->debug->out->包名->databinding->xxx.java。如果想要自己义生成类名，可以如下设置：

  ```kotlin
    <data class="CustomBinding">
    </data>
  复制代码
  ```

下面是Activity中使用：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
   super.onCreate(savedInstanceState)
   val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
   binding.userInfo = User("ccm","12345")
}
复制代码
```

- 使用DataBindingUtil.setContentView()生成XXXBinding类。通过binding.userInfo给userInfo赋值，这样布局中就能使用到userInfo的值了。也可以使用XXXBinding.inflate()去生成XXXBinding类，代码如下：

  ```kotlin
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.userInfo = User("ccm","12345")
  }
  复制代码
  ```

在Fragment中使用。代码示例：

```kotlin
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
 binding = DataBindingUtil.inflate<FragmentRegisterBinding>(inflater,R.layout.fragment_register,container,false)
 binding.user = User("ccc","222")
 return binding.root
}
复制代码
```

使用DataBindingUtil.inflate的方式实例化binding。或者用XXXBinding.inflate方法，示例如下：

```kotlin
override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
  binding = FragmentRegisterBinding.inflate(inflater,container,false)
  binding.user = User("ccc","222")
  return binding.root
}
复制代码
```

在RecyclerView中使用。代码示例：

```kotlin
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseHolder {
        val binding = DataBindingUtil.inflate<ItemListCourseBinding>(LayoutInflater.from(parent.context),R.layout.item_list_course,parent,false)
        return CourseHolder(binding.root)
    }
    // 或者是
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseHolder {
        val binding = ItemListCourseBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return CourseHolder(binding.root)
    }
复制代码
```

#### 3.表达式语言

##### 3.1 常见功能

表达式语言中常见的功能可以使用以下运算符和关键字：

- 算术运算符 + - / * %

  ```kotlin
  // 举例+ - / * %可直接使用
  android:text="@{String.valueOf(userInfo.age+1)+'岁',default = 默认年龄}"
  // 其中的userInfo.age+1就是+的运用
  复制代码
  ```

- 字符串连接运算符 +

  ```kotlin
  // 举例+ - / * %可直接使用
  android:text="@{String.valueOf(userInfo.age+1)+'岁',default = 默认年龄}"
  // 其中的(userInfo.age+1)+'岁'就是字符串连接运算符 +的运用
  复制代码
  ```

- 逻辑运算符 && ||（注意&需要转义为& amp;，&&用& amp;& amp;表示。而||可以直接使用）

  ```kotlin
  // 如果age>10 && age<20
   android:visibility="@{userInfo.age>10 &amp;&amp; userInfo.age &lt; 20 ? View.VISIBLE:View.GONE}"
   // 如果age>10 || age<15
   android:visibility="@{userInfo.age>10 || userInfo.age &lt; 20 ? View.VISIBLE:View.GONE}"
  复制代码
  ```

- 二元运算符 & | ^ (注意&需要转义为 & amp;)

  ```kotlin
  // 注意&需要转义字符，而|跟^可直接使用
  android:visibility="@{userInfo.age>10 &amp; userInfo.age &lt; 20 ? View.VISIBLE:View.GONE}"
  复制代码
  ```

- 一元运算符 + - ! ~

  ```kotlin
  // 可直接使用，举例
  android:visibility="@{!flag ? View.VISIBLE:View.GONE}"
  复制代码
  ```

- 移位运算符 >> >>> << (<需要使用转义& lt;)

  ```kotlin
  // 举例
  android:visibility="@{1>>1 >2 ? View.VISIBLE:View.GONE}"
  android:visibility="@{1>>>1 >2 ? View.VISIBLE:View.GONE}"
  // 不过左移记得使用转义
  android:visibility="@{1&lt;&lt;1 >2 ? View.VISIBLE:View.GONE}"
  复制代码
  ```

- 比较运算符 == > < >= <=（请注意，< 需要转义为 & lt;）

  ```kotlin
  android:visibility="@{userInfo.age==10  ? View.VISIBLE:View.GONE}"
  android:visibility="@{userInfo.age>10  ? View.VISIBLE:View.GONE}"
  android:visibility="@{userInfo.age>=10  ? View.VISIBLE:View.GONE}"
  // age<10,注意是<号需要使用转义 &lt;
  android:visibility="@{userInfo.age&lt;10  ? View.VISIBLE:View.GONE}"
  // age<=10
  android:visibility="@{userInfo.age&lt;=10  ? View.VISIBLE:View.GONE}"
  复制代码
  ```

- instanceof

- 分组运算符 ()

  ```kotlin
  // ()可直接使用 (userInfo.age+1)/2这里就使用了()
   android:text="@{String.valueOf((userInfo.age+1)/2)+'岁',default = 默认年龄}"
  复制代码
  ```

- 字面量运算符 - 字符、字符串、数字、null

  ```kotlin
  // 比如字符串，如果外面是""包裹，则里面要用``反引号包裹字符串，如果外面用''，则里面用""
  android:text="@{String.valueOf(userInfo.age+1)+`岁的人`}" 等价于 
  android:text='@{String.valueOf(userInfo.age+1)+"岁的人"}'
  // 注意如果外面是双引号，而里面用的是单引号''去拼接，那么只能是拼接上一个字符。拼接上多个字符用反单引号``
  android:text="@{String.valueOf(userInfo.age+1)+'岁'}"
  android:text="@{String.valueOf(userInfo.age+1)+`岁的人`}"
  // 数字，null等直接使用
  android:text="@{age!=null?age:1}"
  复制代码
  ```

- 类型转换

  ```kotlin
  <TextView
       android:text="@{((User)(user.connection)).lastName}"
       android:layout_width="wrap_content"
       android:layout_height="wrap_content"/>
  复制代码
  ```

- 方法调用

  ```kotlin
  // String.valueOf这个就是调用了方法
  android:text="@{String.valueOf(userInfo.age+1)+'岁',default = 默认年龄}"
  复制代码
  ```

- 字段访问

- 数组访问 []

  ```kotlin
    <data>
       <import type="java.util.List"/>
       <variable name="list" type="List&lt;String>"/>
       <variable name="index" type="int"/>
    </data>
    ...
    android:text="@{list[index]}"
  复制代码
  ```

- 三元运算符 ?:

  ```kotlin
  android:visibility="@{age > 13 ? View.GONE : View.VISIBLE}"
  复制代码
  ```

##### 3.2 缺少的运算

使用的表达式语法中缺少以下运算：

- this
- super
- new
- 显式泛型调用

##### 3.3 Null 合并运算符

```kotlin
android:text="@{user.displayName ?? user.lastName}"
// 等价于
android:text="@{user.displayName != null ? user.displayName : user.lastName}"
复制代码
```

##### 3.4 属性引用

```kotlin
android:text="@{user.lastName}"
复制代码
```

##### 3.5 避免出现 Null 指针异常

生成的数据绑定代码会自动检查有没有 null 值并避免出现 Null 指针异常。例如，在表达式 @{user.name} 中，如果 user 为 Null，则为 user.name 分配默认值 null。如果您引用 user.age，其中 age 的类型为 int，则数据绑定使用默认值 0。

##### 3.6 视图引用

```kotlin
<EditText
    android:id="@+id/et_input_name"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:layout_constraintTop_toBottomOf="@+id/tv_age" />

<TextView
    android:id="@+id/tv_input_name"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="@{etInputName.text}"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@+id/et_input_name" />
复制代码
```

EditText里输入什么，TextView里就会显示EditTextView输入的内容
 注意：绑定类将 ID 转换为驼峰式大小写。

##### 3.7 集合

为方便起见，可使用 [] 运算符访问常见集合，例如数组、列表、稀疏列表和映射。

```kotlin
   <data>
        <import type="android.util.SparseArray"/>
        <import type="java.util.Map"/>
        <import type="java.util.List"/>
        <variable name="list" type="List&lt;String>"/>
        <variable name="sparse" type="SparseArray&lt;String>"/>
        <variable name="map" type="Map&lt;String, String>"/>
        <variable name="index" type="int"/>
        <variable name="key" type="String"/>
    </data>
    …
    android:text="@{list[index]}"
    …
    android:text="@{sparse[index]}"
    …
    android:text="@{map[key]}"
复制代码
```

注意：要使 XML 不含语法错误，您必须转义 < 字符。例如：不要写成 List<String> 形式，而是必须写成 List& lt;String>。

##### 3.8 字符串字面量

您可以使用单引号括住特性值，这样就可以在表达式中使用双引号，如以下示例所示：

```kotlin
android:text='@{map["firstName"]}'
复制代码
```

也可以使用双引号括住特性值。如果这样做，则还应使用反单引号 ` 将字符串字面量括起来：

```kotlin
android:text="@{map[`firstName`]}"
复制代码
```

##### 3.9 资源引用

dimens.xml

```kotlin
 <dimen name="dp_40">40dip</dimen>
复制代码
```

strings.xml

```kotlin
<string name="format">%s name is %s</string>
复制代码
android:layout_height="@{@dimen/dp_40}"
android:text='@{@string/format("my", "lilei")}'
复制代码
```

某些资源需要显式类型求值，如下表所示

| 类型              | 常规引用  | 表达式引用         |
| ----------------- | --------- | ------------------ |
| String[]          | @array    | @stringArray       |
| int[]             | @array    | @intArray          |
| TypedArray        | @array    | @typedArray        |
| Animator          | @animator | @animator          |
| StateListAnimator | @animator | @stateListAnimator |
| color int         | @color    | @color             |
| ColorStateList    | @color    | @colorStateList    |

举个stringArray的例子：

```kotlin
<string-array name="languages">
        <item>C语言</item>
        <item>Java </item>
        <item>C#</item>
        <item>PHP</item>
        <item>HTML</item>
    </string-array>
复制代码
// android:entries="@array/languages"等价于android:entries="@{@stringArray/languages}"
<Spinner
   android:id="@+id/spinner"
   app:layout_constraintStart_toStartOf="parent"
   app:layout_constraintEnd_toEndOf="parent"
   app:layout_constraintTop_toBottomOf="@+id/tv_input_name"
   android:layout_width="wrap_content"
   android:entries="@{@stringArray/languages}"
   android:layout_height="wrap_content"/>
复制代码
```

#### 4.事件处理

有很多回调事件可以使用：
 android:onClick
 android:onLongClick
 android:afterTextChanged
 android:onTextChanged
 ...等等等
 而我们主要有两种方式可以去使用这些事件：

##### 4.1 方法引用

建一个ClickHelper类

```kotlin
class ClickHelper(){

    fun onLoginClick(view:View){
        Toast.makeText(view.context,"click btn",Toast.LENGTH_SHORT).show()
    }
}
复制代码
```

布局里添加如下代码：

```kotlin
<data>
    <import type="com.example.jetpackdatabindingtestapp.ui.helper.ClickHelper"/>
    <variable name="clickHelper" type="ClickHelper" />
</data>
...
<Button
   android:id="@+id/btn_login"
   app:layout_constraintStart_toStartOf="parent"
   app:layout_constraintEnd_toEndOf="parent"
   app:layout_constraintTop_toTopOf="parent"
   android:text="登录"
   android:onClick="@{clickHelper::onLoginClick}"
   android:layout_width="wrap_content"
   android:layout_height="wrap_content"/>
复制代码
```

android:onClick="@{clickHelper::onLoginClick}" 使用这种方式进行方法的引用
 Activity的代码如下：

```kotlin
class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        binding.userInfo = User("ccm","12345",11)
        binding.clickHelper = ClickHelper()
    }
}
复制代码
```

##### 4.2 绑定监听器

建一个ClickHelper类

```kotlin
class ClickHelper(val context: Context){
    fun onRegistClick(user:User){
        Toast.makeText(context,"click btn ${user.name}",Toast.LENGTH_SHORT).show()
    }
}
复制代码
```

Activity的代码如下：

```kotlin
class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        binding.userInfo = User("ccm","12345",11)
        binding.clickHelper = ClickHelper(this)
    }
}
复制代码
```

布局代码如下：

```kotlin
<data>
    <import type="com.example.jetpackdatabindingtestapp.ui.helper.ClickHelper"/>
    <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
    <variable name="clickHelper" type="ClickHelper" />
    <variable name="user" type="User" />
</data>
...
<Button
  android:id="@+id/btn_regist"
  app:layout_constraintStart_toStartOf="parent"
  app:layout_constraintEnd_toEndOf="parent"
  app:layout_constraintTop_toTopOf="parent"
  android:text="注册"
  android:onClick="@{()->clickHelper.onRegistClick(userInfo)}"
  android:layout_width="wrap_content"
  android:layout_height="wrap_content"/>
复制代码
```

android:onClick="@{()->clickHelper.onRegistClick(userInfo)}" 使用绑定的方式调用 如果上面onLoginClick用绑定的形式调用，那么代码如下：

```kotlin
android:onClick="@{(theView)->clickHelper.onLoginClick(theView)}"
复制代码
```

#### 5.import，variable，include，viewStub

##### 5.1 import 导入

```kotlin
 <data>
    // 导入Android类
    <import type="android.view.View"/>
    // 导入其他类
    <import type="com.example.User"/>
    // 当类名存在冲突时，可以使用别名
    <import type="com.example.real.estate.View"
            alias="Vista"/>
 </data>
复制代码
```

还有一个是静态方法的导入使用

```kotlin
public class StringUtils {
    public static String toUpperCase(String str) {
        return str.toUpperCase();
    }
}

<data>
   <import type="com.example.StringUtils"/>
   <variable name="user" type="com.example.User"/>
</data>
 …
<TextView
   android:text="@{StringUtils.toUpperCase(user.lastName)}"
   android:layout_width="wrap_content"
   android:layout_height="wrap_content"/>
复制代码
```

##### 5.2 variable 变量

```kotlin
<data>
   <import type="com.example.jetpackdatabindingtestapp.ui.helper.ClickHelper"/>
   <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
   <variable name="userInfo" type="User" />
   <variable name="clickHelper" type="ClickHelper" />
 </data>
复制代码
```

用variable标签声明的userInfo,clickHelper这些就是变量

##### 5.3 include

创建一个用于include的布局layout_username.xml

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>

        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />

        <variable
            name="includeUser"
            type="User" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/layout_include_content"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@color/colorAccent">

        <TextView
            android:id="@+id/tv_username"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@{includeUser.name}"
            android:textColor="#ffffff"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

在activity_main.xml中引用include的布局

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
        <variable name="userInfo" type="User" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <include
            layout="@layout/layout_username"
            app:includeUser="@{userInfo}"/>

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

app:includeUser="@{userInfo}" 这里需要注意，includeUser需要是include布局里声明的变量名
 Activity的代码如下：

```kotlin
class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        binding.userInfo = User("ccm","12345",11)
    }
}
复制代码
```

##### 5.4 ViewStub

创建一个用于layout_viewstub.xml

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <data>
        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
        <variable
            name="viewStubUser"
            type="User" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/layout_viewstub_content"
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <TextView
            android:id="@+id/tv_viewstub_username"
            android:layout_width="wrap_content"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            android:text="@{(viewStubUser.name)+`的viewStub`}"
            android:layout_height="wrap_content"/>

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

在activity_main.xml中使用ViewStub

```kotlin
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">
    <data>
        <import type="com.example.jetpackdatabindingtestapp.ui.model.User" />
        <variable name="userInfo" type="User" />
    </data>

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <ViewStub
            android:id="@+id/layout_view_stub"
            app:layout_constraintTop_toTopOf="parent"
            android:layout_width="match_parent"
            android:layout="@layout/layout_viewstub"
            app:viewStubUser="@{userInfo}"
            android:layout_height="wrap_content"/>

    </androidx.constraintlayout.widget.ConstraintLayout>
</layout>
复制代码
```

app:viewStubUser="@{userInfo}" 这里需要注意，viewStubUser需要是viewstub的布局里声明的变量名
 Activity的代码如下：

```kotlin
class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
        binding.userInfo = User("ccm","12345",11)
        binding.layoutViewStub.viewStub?.inflate()
    }
}
复制代码
```

如果在布局中app:viewStubUser="@{userInfo}"没有使用这句代码进行数据绑定，那么也可以在ViewStub inflate的时候进行绑定,代码如下：

```kotlin
val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this,R.layout.activity_main)
val user = User("ccm","12345",11)
binding.userInfo = user
binding.layoutViewStub.viewStub?.setOnInflateListener { stub, inflated ->
   val viewStubBinding = DataBindingUtil.bind<LayoutViewstubBinding>(inflated)
   viewStubBinding?.viewStubUser = user
}
binding.layoutViewStub.viewStub?.inflate()
复制代码
```

以上这些就是DataBinding的简单的使用，以及xml里面能使用的表达式语言，和事件绑定。不过以上实现的数据绑定的方式，每当绑定的变量发生变化的时候，都需要重新向XXXBinding传递新的变量值，才能更新UI。接下来会讲到如何实现自动刷新UI。
