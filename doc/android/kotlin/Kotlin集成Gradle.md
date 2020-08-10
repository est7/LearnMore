由于 Kotlin 具有丰富的功能，如一等函数和扩展方法等，因此它可以保留和改进 Gradle 构建脚本的最佳部分——包括简明的声明式语法以及轻松制作 DSL 的能力。

Gradle 团队与 Kotlin 团队密切合作，为 Gradle 开发了新的基于 Kotlin 脚本的构建配置语言，我们称之为 Gradle Script Kotlin，支持使用 Kotlin 编写构建和配置文件。同时，还支持在 IDE 中实现自动完成和编译检查等功能。有了Gradle Script Kotlin，我们可以使用 Kotlin 来写配置文件，就跟写普通代码一样。

我们在前面的章节中，已经有很多示例项目使用了 Gradle 来构建我们的 Kotlin 工程。本章我们将系统地来介绍一下使用 Kotlin 集成Gradle 开发的相关内容。

## 12.1 使用 Gradle 构建 Kotlin工程

### 12.1.1 kotlin-gradle 插件

为了用 Gradle 构建 Kotlin工程，我们需要设置好 *kotlin-gradle* 插件：



```bash
buildscript {
    ext {
        kotlinVersion = '1.1.3-2'
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
                ...
    }
}

apply plugin: 'kotlin'
```

并且添加 *kotlin-stdlib* 依赖：



```bash
dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:${kotlinVersion}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
}
```

当然，这些操作在我们新建项目的时候，通常我们只需要选择相应的选项， IntelliJ IDEA 就会直接帮我们完成了基本的配置了。

我们使用 `kotlin-gradle-plugin` 编译 Kotlin 源代码和模块。使用的 Kotlin 版本通常定义为 `kotlinVersion` 属性。

针对 JVM，我们需要应用 Kotlin 插件：



```groovy
apply plugin: "kotlin"
```

### 12.1.2 Kotlin 与 Java 混合编程

Kotlin 源代码可以与同一个文件夹或不同文件夹中的 Java 源代码混用。默认约定是使用不同的文件夹：



```groovy
sourceSets {
    main.kotlin.srcDirs += 'src/main/kotlin'
    main.java.srcDirs += 'src/main/java'
}
```

如果使用默认的目录，上面的配置可以省去不写。

如果不使用默认约定，那么应该更新相应的 *sourceSets* 属性



```groovy
sourceSets {
    main.kotlin.srcDirs += 'src/main/myKotlin'
    main.java.srcDirs += 'src/main/myJava'
}
```

### 12.1.3 配置  Gradle JavaScript 项目

当针对 JavaScript 时，须应用不同的插件：



```groovy
apply plugin: "kotlin2js"
```

除了输出的 JavaScript 文件，该插件默认会创建一个带二进制描述符的额外 JS 文件。

如果是构建其他 Kotlin 模块可以依赖的可重用库，那么该文件是必需的，并且与转换结果一起分发。

二进制描述符文件的生成由 `kotlinOptions.metaInfo` 选项控制：



```groovy
compileKotlin2Js {
    kotlinOptions.metaInfo = true
}
```

提示：示例工程可以参考
 https://github.com/EasyKotlin/chapter2_hello_world_kotlin2js

### 12.1.4  配置 Gradle Android项目

Android 的 Gradle 模型与普通 Gradle 有点不同，所以如果我们要构建一个用 Kotlin 编写的 Android 项目，我们需要用 *kotlin-android* 插件取代 *kotlin* 插件：



```groovy
buildscript {
    ext.kotlin_version = '1.1.2-4'
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.1'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}
```

通常我们使用 Android Studio，都是生成一个带 app 子项目的工程。多项目配置的实现通常是在一个根项目路径下将所有项目作为子文件夹包含进去。例如我们在项目根路径下面的settings.gradle中如下配置：



```php
include ':app'
```

每一个子项目都拥有自己的build.gradle文件来声明自己如何构建。

例如，我们在子项目app的构建配置文件 build.gradle 中一个完整的配置如下：



```bash
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

android {
    compileSdkVersion 25
    buildToolsVersion "25.0.2"
    defaultConfig {
        applicationId "com.kotlin.easy.kotlinandroid"
        minSdkVersion 14
        targetSdkVersion 25
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])
    androidTestCompile('com.android.support.test.espresso:espresso-core:2.2.2', {
        exclude group: 'com.android.support', module: 'support-annotations'
    })
    compile 'com.android.support:appcompat-v7:25.3.1'
    compile 'com.android.support.constraint:constraint-layout:1.0.2'
    testCompile 'junit:junit:4.12'
    compile "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"
}
repositories {
    mavenCentral()
}
```

其中，

apply plugin: 'kotlin-android' 是 Kotlin Android 插件。
 compile "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version" 是 Kotlin 运行标准库。

另外， Android Studio 默认加载源码的目录是 `src/main/java`，如果想指定 Kotlin 代码在`src/main/kotln`目录，可以在 android 下添加以下内容：



```groovy
android {
  ……
  sourceSets {
    main.java.srcDirs += 'src/main/kotlin'
  }
}
```

提示： 关于 Kotlin Android 的 Gradle 完整配置实例可参考  https://github.com/EasyKotlin/KotlinAndroid  。

### 12.1.5  配置Kotlin 标准库依赖

除了上面的 `kotlin-gradle-plugin` 依赖之外，我们还需要添加 Kotlin 标准库的依赖：



```groovy
repositories {
    mavenCentral()
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib"
}
```

如果针对 JavaScript，使用 `compile "org.jetbrains.kotlin:kotlin-stdlib-js"` 替代之。

如果是针对 JDK 7 或 JDK 8，那么可以使用扩展版本的 Kotlin 标准库，其中包含为新版 JDK 增加的额外的扩展函数。使用以下依赖之一来取代 `kotlin-stdlib`：



```groovy
compile "org.jetbrains.kotlin:kotlin-stdlib-jre7"
compile "org.jetbrains.kotlin:kotlin-stdlib-jre8"
```

如果项目中使用 Kotlin 反射，添加反射依赖：



```groovy
compile "org.jetbrains.kotlin:kotlin-reflect"
```

如果项目中使用测试框架，我们添加相应的测试库依赖：



```groovy
testCompile "org.jetbrains.kotlin:kotlin-test"
testCompile "org.jetbrains.kotlin:kotlin-test-junit"
```

### 12.1.6 增量编译

Kotlin 支持 Gradle 中可选的增量编译。增量编译跟踪构建之间源文件的改动，因此只有受这些改动影响的文件才会被编译。从 Kotlin 1.1.1 起，默认启用增量编译。

### 12.1.7  编译器选项

要指定附加的编译选项，可以使用 Kotlin 编译任务compileKotlin的 `kotlinOptions` 属性。

配置单个任务示例：



```groovy
compileKotlin {
    kotlinOptions {
        suppressWarnings = true
    }
}
```

## 12.2 使用 Kotlin 编写构建和配置文件

一个基于 Kotlin 来写 Gradle 构建脚本及插件的方式可能会是什么样的？ 它对团队的帮助如何——尤其是大型团队——加快工作速度并编写结构更好、更易于维护的构建脚本？

这些可能性非常诱人。

因为 Kotlin 是一种静态类型语言，在 IDEA 和 Eclipse 中都有深入的支持，所以可以从自动补全到重构，以及其间的一切都能为 Gradle 用户提供适当的 IDE 支持。 而且由于 Kotlin 具有丰富的功能，如一等函数和扩展方法，因此它可以保留和改进 Gradle 构建脚本的最佳部分——包括简明的声明式语法以及轻松制作 DSL 的能力。

Gradle 团队认真地考察了这些可能性，与 Kotlin 团队密切合作，为 Gradle 开发一种新的基于 Kotlin 的构建语言——我们称之为 Gradle Script Kotlin。

下面我们就来简要介绍一下使用 Kotlin 脚本来编写 Gradle 的配置文件。

我们就以上一章中的 chapter11_kotlin_springboot 工程为例。

首先我们在根目录下新建一个settings.gradle 配置文件：



```bash
rootProject.name = 'chapter11_kotlin_springboot'
rootProject.buildFileName = 'build.gradle.kts'
```

指定 gradle 构建文件名是 'build.gradle.kts' 。
 然后，我们新建 'build.gradle.kts' ， 完整的内容如下：



```kotlin
buildscript {
    val kotlinVersion = "1.1.3-2"
    val springBootVersion = "2.0.0.M2"
    extra["kotlinVersion"] = kotlinVersion

    repositories {
        mavenCentral()
        maven { setUrl("https://repo.spring.io/snapshot") }
        maven { setUrl("https://repo.spring.io/milestone") }
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    }
}

apply {
    plugin("kotlin")
    plugin("kotlin-spring")
    plugin("eclipse")
    plugin("org.springframework.boot")
    plugin("io.spring.dependency-management")
}

version = "0.0.1-SNAPSHOT"

configure<JavaPluginConvention> {
    setSourceCompatibility(1.8)
    setTargetCompatibility(1.8)
}


repositories {
    mavenCentral()
    maven { setUrl("https://repo.spring.io/snapshot") }
    maven { setUrl("https://repo.spring.io/milestone") }
}

val kotlinVersion = extra["kotlinVersion"] as String

dependencies {
    compile("org.springframework.boot:spring-boot-starter-actuator")
    compile("org.springframework.boot:spring-boot-starter-data-jpa")
    compile("org.springframework.boot:spring-boot-starter-freemarker")
    compile("org.springframework.boot:spring-boot-starter-web")
    compile("org.jetbrains.kotlin:kotlin-stdlib-jre8:${kotlinVersion}")
    compile("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    runtime("org.springframework.boot:spring-boot-devtools")
    runtime("mysql:mysql-connector-java")
    testCompile("org.springframework.boot:spring-boot-starter-test")
}
```

提示： 按照上述步骤，新建完文件build.gradle.kts后，IDEA 可能识别不了这些 DSL 函数，这个时候我们重启一下 IDEA 即可（这是一个 bug，后面会修复）。

这里面的 Gradle DSL 的相关函数与类都在 Gradle 软件包的 lib 目录下： lib/gradle-script-kotlin-(版本号).jar 。我们简单用下面的表格说明：

| 函数（类）               | 对应的gradle-script-kotlin代码                               |
| ------------------------ | ------------------------------------------------------------ |
| buildscript              | open fun buildscript(@Suppress("unused_parameter") block: ScriptHandlerScope.() -> Unit) = Unit |
| repositories             | fun ScriptHandler.repositories(configuration: RepositoryHandler.() -> Unit) =repositories.configuration() |
| buildscript.dependencies | DependencyHandlerScope(scriptHandler.dependencies)           |
| configure                | inline fun <reified T : Any> Project.configure(noinline configuration: T.() -> Unit) |
| Project.dependencies     | DependencyHandlerScope(dependencies).configuration()         |

也就是说，其实这些配置函数背后都是由 Gradle 的 DSL 来实现的。

其实，这些配置语法看起跟 Groovy 的很像。例如：

Groovy :



```bash
    repositories {
        mavenCentral()
        maven { url "https://repo.spring.io/snapshot" }
        maven { url "https://repo.spring.io/milestone" }
    }
```

Kotlin:



```bash
repositories {
    mavenCentral()
    maven { setUrl("https://repo.spring.io/snapshot") }
    maven { setUrl("https://repo.spring.io/milestone") }
}
```

再例如：
 Groovy:



```undefined
sourceCompatibility = 1.8
targetCompatibility = 1.8
```

Kotlin:



```xml
configure<JavaPluginConvention> {
    setSourceCompatibility(1.8)
    setTargetCompatibility(1.8)
}
```

提示： 本节示例工程源码  https://github.com/EasyKotlin/chapter11_kotlin_springboot/tree/build.gradle.kts

## 本章小结

本章我们简要介绍了使用 Kotlin 集成 Gradle 开发过程中的一些常用的配置方法。Gradle 是一个非常好用的构建工具，当我们的 Kotlin 工程的配置文件也是 Kotlin 代码的时候，我们的工作又更加单纯了许多，只需要专注 Kotlin 即可。

在下一章中，我们将学习使用 Kotlin 和 Anko 来进行Android开发的相关内容。