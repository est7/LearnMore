1：新建library 删除无用 （除去andorid 相关的）

2：创建resources 文件 固定格式

![image-20211216172216946](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161722977.png)

.properties 之前的就是你的插件名称

内容：

```java
implementation-class=com.miadapm.plugin.MiAdApmPlugin //插件入口类
```

插件gradle配置：

```java
apply plugin: 'groovy'//语言
apply plugin: 'maven'//仓库

dependencies {
    implementation gradleApi()
    implementation localGroovy()
    implementation 'com.android.tools.build:gradle:4.2.2'
    implementation 'org.ow2.asm:asm:7.1'
    implementation 'org.ow2.asm:asm-commons:7.1'
    implementation 'org.aspectj:aspectjrt:1.9.7'
    implementation 'org.aspectj:aspectjtools:1.9.7'
}

repositories {
    mavenCentral()
}


def group='com.miadapm.plugin' //组
def version='1.0.0' //版本
def artifactId='miadapm' //唯一标示


//将插件打包上传到本地maven仓库
uploadArchives {
    repositories {
        mavenDeployer {
            pom.groupId = group
            pom.artifactId = artifactId
            pom.version = version
            //指定本地maven的路径，在项目根目录下
            repository(url: uri('../repos'))
        }
    }
}
```

project gradle 配置：

```java
// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        maven {
            url uri('./repos')
        }
        mavenLocal()
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.2.2'
        classpath 'com.miadapm.plugin:miadapm:1.0.0'
        classpath 'org.aspectj:aspectjtools:1.9.7'
    }
}

allprojects {
    repositories {
        maven {
            url uri('./repos')
        }
        mavenLocal()
        google()
        jcenter()
    }
}


task clean(type: Delete) {
    delete rootProject.buildDir
}
```

app 使用时：

project ：配置：

```
classpath 'com.miadapm.plugin:miadapm:1.0.0'
```

app：

```java
apply plugin: 'xxxx'//插件名称

```

```java

package com.miadapm.plugin;

//gradle 插件入口
public class MiAdApmPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
    }
```

