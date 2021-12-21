## （一）Javassist是什么

Javassist是可以动态编辑Java字节码的类库。它可以在Java程序运行时定义一个新的类，并加载到JVM中；还可以在JVM加载时修改一个类文件。Javassist使用户不必关心字节码相关的规范也是可以编辑类文件的。

## （二）Javassist核心API

在Javassist中每个需要编辑的class都对应一个CtCLass实例，CtClass的含义是编译时的类（compile time class），这些类会存储在Class Pool中（Class poll是一个存储CtClass对象的容器）。
 CtClass中的CtField和CtMethod分别对应Java中的字段和方法。通过CtClass对象即可对类新增字段和修改方法等操作了。

![image.png](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161712768.webp)

# ClassPool

1. getDefault : 返回默认的`ClassPool` 是单例模式的，一般通过该方法创建我们的ClassPool；
2. appendClassPath, insertClassPath : 将一个`ClassPath`加到类搜索路径的末尾位置 或 插入到起始位置。通常通过该方法写入额外的类搜索路径，以解决多个类加载器环境中找不到类的尴尬；
3. toClass : 将修改后的CtClass加载至当前线程的上下文类加载器中，CtClass的`toClass`方法是通过调用本方法实现。**需要注意的是一旦调用该方法，则无法继续修改已经被加载的class**；
4. get , getCtClass : 根据类路径名获取该类的CtClass对象，用于后续的编辑。

# `CtClass`

1. freeze : 冻结一个类，使其不可修改；
2. isFrozen : 判断一个类是否已被冻结；
3. prune : 删除类不必要的属性，以减少内存占用。调用该方法后，许多方法无法将无法正常使用，慎用；
4. defrost : 解冻一个类，使其可以被修改。如果事先知道一个类会被defrost， 则禁止调用 prune 方法；
5. detach : 将该class从ClassPool中删除；
6. writeFile : 根据CtClass生成 `.class` 文件；
7. toClass : 通过类加载器加载该CtClass。

上面我们创建一个新的方法使用了`CtMethod`类。CtMthod代表类中的某个方法，可以通过CtClass提供的API获取或者CtNewMethod新建，通过CtMethod对象可以实现对方法的修改。

# `CtMethod`

1. insertBefore : 在方法的起始位置插入代码；

2. insterAfter : 在方法的所有 return 语句前插入代码以确保语句能够被执行，除非遇到exception；

3. insertAt : 在指定的位置插入代码；

4. setBody : 将方法的内容设置为要写入的代码，当方法被 abstract修饰时，该修饰符被移除；

5. make : 创建一个新的方法。

   

# eg：

## 1：创建一个测试类，代码如下：

```java
package com.ssdmbbl.javassist;

import javassist.*;

import java.io.IOException;

public class JavassistTest {
    public static void main(String[] args) throws CannotCompileException, IOException {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("com.ssdmbbl.javassist.Hello");
        ctClass.writeFile("./");
    }
}
复制代码
```

当运行这个代码的时候，可以看到已经在项目的根目录下创建了一个“com.ssdmbbl.javassist”包，在这个包下创建了“Hello.java”的java文件。
 ![image.png](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112161713742.webp)

2：米盟聚合sdk使用：规避开放接口

```java
package com.transform.mediationplugin

import com.xiaomi.mediationannotation.HideClass
import com.xiaomi.mediationannotation.HideMethod
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Project

public class MediationRemoveMethod {
    private final static ClassPool pool = ClassPool.getDefault();

    public static void inject(String path, Project project) {
        pool.appendClassPath(path)
        File dir = new File(path)
        println("file path =="+ dir.getAbsolutePath())
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                try {
                    if (file.getName().endsWith("class") && file.exists()) {
                        InputStream io = new FileInputStream(file)
                        CtClass ctClass = pool.makeClass(io)
                        if (ctClass.isFrozen()) {
                            ctClass.defrost()
                        }
                        HideClass classAnnotation = ctClass.getAnnotation(HideClass.class)
                        if (classAnnotation == null ){
                            ctClass.detach()
                            return
                        }
                        if(classAnnotation.value()){
                            ctClass.detach()
                            io.close()
                            boolean delete = file.delete()
                            println(" remove class success : "+delete+"\n class info :"+ctClass.toString())
                            return
                        }
                        println(" change class =======>  "+ctClass.toString()+"   annotation ======>"+classAnnotation.toString())
                        for (CtMethod method : ctClass.getDeclaredMethods()) {
                            HideMethod methodAnnotaion = method.getAnnotation(HideMethod.class)
                            if (methodAnnotaion != null) {
                                println("annotation  =======>"+methodAnnotaion.value())
                                if(methodAnnotaion.value()){
                                    println("remove method ======> "+method.getName()+"   annotation ======>"+methodAnnotaion.toString())
                                    ctClass.removeMethod(method)
                                }
                            }
                        }
                        ctClass.writeFile(path)
                        ctClass.detach()//释放
                    }
                } catch (Exception e) {
                }
            }
        }

    }
}
```

