# javapoet 使用详解

### javapoet简介

https://github.com/square/javapoet

JavaPoet是square推出的开源java代码生成框架，提供Java Api生成.java源文件。这个框架功能非常有用，我们可以很方便的使用它根据注解、数据库模式、协议格式等来对应生成代码。

### JavaPoet的常用类

| 类名                  | 备注                                                      |
| --------------------- | --------------------------------------------------------- |
| TypeSpec              | 用于生成类、接口、枚举对象的类                            |
| MethodSpec            | 用于生成方法对象的类                                      |
| ParameterSpec         | 用于生成参数对象的类                                      |
| AnnotationSpec        | 用于生成注解对象的类                                      |
| FieldSpec             | 用于配置生成成员变量的类                                  |
| ClassName             | 通过包名和类名生成的对象，在JavaPoet中相当于为其指定Class |
| ParameterizedTypeName | 通过MainClass和IncludeClass生成包含泛型的Class            |
| JavaFile              | 控制生成的Java文件的输出的类                              |

### JavaPoet的常用方法

| 方法名称                            | 备注           |
| ----------------------------------- | -------------- |
| addModifiers(Modifier... modifiers) | 设置修饰关键字 |
| addAnnotation                       | 设置注解对象   |
| addJavadoc                          | 设置注释       |

### JavaPoet生成方法

JavaPoet生成方法分为两种

- 构造方法

```
MethodSpec.constructorBuilder()
```

- 常规方法

```
MethodSpec.methodBuilder(String name)
```

- 方法参数

```
addParameter(ParameterSpec parameterSpec)
```

- 返回值

```
returns(TypeName returnType)
```

- 方法体

在JavaPoet中，设置方法体内容有两个方法，分别是addCode和addStatement：

```
addCode()
addStatement()
复制代码
```

#### 方法体模板

在JavaPoet中，设置方法体使用模板是比较常见的，因为addCode和addStatement方法都存在这样的一个重载:

```
addCode(String format, Object... args)
addStatement(String format, Object... args)
复制代码
```

在JavaPoet中，format中存在三种特定的占位符：

|      | 备注                                                         |
| ---- | ------------------------------------------------------------ |
| $T   | 在JavaPoet代指的是TypeName，该模板主要将Class抽象出来，用传入的TypeName指向的Class来代替 |
| $N   | 代指的是一个名称，例如调用的方法名称，变量名称，这一类存在意思的名称 |
| $S   | 和String.format中%s一样,字符串的模板,将指定的字符串替换到S的地方，需要注意的是替换后的内容，默认自带了双引号，如果不需要双引号包裹，需要使用*S*的地方，需要注意的是替换后的内容，默认自带了双引号，如果不需要双引号包裹，需要使用L |

三种占位符的使用如下：

```
ClassName bundle = ClassName.get("android.os", "Bundle");
addStatement("$T bundle = new $T()",bundle)

复制代码
```

上述添加的代码内容为：

```
Bundle bundle = new Bundle();

复制代码
addStatement("data.$N()",toString)

复制代码
```

上述代码添加的内容：

```
data.toString();

复制代码
.addStatement("return $S", “name”)
复制代码
```

即将"name"字符串代替到$S的位置上.

### JavaPoet生成方法参数

```
addParameter(ParameterSpec parameterSpec)
复制代码
```

### JavaPoet使用案例

生成代码生成一般是由内向外的，先生成里面的主方法然后生成外层的主类

```java
private static void createDataBean(){
        String tacosPackage = "com.zbc.latte_compiler.javapoeatdemo";
        ClassName data = ClassName.get(tacosPackage, "BaseEntity", "DataBean");

        FieldSpec code = FieldSpec.builder(int.class, "code")
                .addModifiers(Modifier.PRIVATE)
                .build();

        FieldSpec msg = FieldSpec.builder(String.class, "msg")
                .addModifiers(Modifier.PRIVATE)
                .build();


        //使用 MethodSpec 方法生成
        MethodSpec getCode = MethodSpec.methodBuilder("getCode")      //主方法的名称
                .addModifiers(Modifier.PRIVATE)
                .returns(int.class)
                .addStatement("return this.code")
                .build();


        MethodSpec setCode = MethodSpec.methodBuilder("setCode")      //主方法的名称
                .addModifiers(Modifier.PRIVATE)
                .returns(Void.class)
                .addParameter(int.class, "code")
                .addStatement("this.$N = $N", "code", "code")
                .build();


        //内部类
        FieldSpec name = FieldSpec.builder(String.class, "name")
                .addModifiers(Modifier.PRIVATE)
                .build();
        MethodSpec getName = MethodSpec.methodBuilder("getName")
                .addModifiers(Modifier.PRIVATE)
                //定义返回值类型
                .returns(String.class)
                //代码方法内添加通用代码
                .addStatement("return this.name")
                .build();

        MethodSpec setName = MethodSpec.methodBuilder("setName")
                .addModifiers(Modifier.PRIVATE)
                //参数接收,这里注意，如果是int 就直接写int不用装箱
                .addParameter(String.class, "name")
                .addStatement("this.$N = $N", "name", "name")
                .build();

        TypeSpec dataBean = TypeSpec.classBuilder("DataBean")
                .addField(name)
                .addModifiers(Modifier.STATIC)
                .addMethod(getName)
                .addMethod(setName)
                .addJavadoc("内部类生成")
                .build();

        //使用 TypeSpec 生成 BaseEntity 类
        TypeSpec BaseEntity = TypeSpec.classBuilder("BaseEntity")   //主类的名称
                .addField(code)
                .addField(msg)
                .addField(data, "data")
                .addMethod(getCode)
                .addMethod(setCode)
                .addType(dataBean)
                .addJavadoc("注释注解代码块")
                .build();


        try {
            JavaFile javaFile = JavaFile.builder("com.zbc.latte_compiler.javapoeatdemo", BaseEntity)
                    .build();
            /**
             * 代码写入控制台
             */
            javaFile.writeTo(System.out);
            /**
             * 代码写入文件 E:\FastEc\latte_compiler\src\main\java
             */
            File file = new File("latte_compiler\src\main\java");
            System.out.println("___" + file.getAbsolutePath());
            javaFile.writeTo(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
复制代码
```

生成类为一个实体类

```
	package com.zbc.latte_compiler.javapoeatdemo;
	
	import java.lang.String;
	
	/**
	 * 请求实体Bean */
	class BaseEntity {
	  private int code;
	
	  private String msg;
	
	  private DataBean data;
	
	  private int getCode() {
	    return this.code;
	  }
	
	  private void setCode(int code) {
	    this.code = code;
	  }
	
	  private DataBean getData() {
	    return this.data;
	  }
	
	  private void setData(DataBean data) {
	    this.data = data;
	  }
	
	  /**
	   * 内部类生成 */
	  static class DataBean {
	    private String name;
	
	    private String getName() {
	      return this.name;
	    }
	
	    private void setName(String name) {
	      this.name = name;
	    }
	  }
	}

复制代码
```

[获取代码](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fhaoran2021%2FBaseJavaApp%2Ftree%2Fmaster)

[javapoet文档](https://link.juejin.cn/?target=https%3A%2F%2Fgithub.com%2Fsquare%2Fjavapoet)