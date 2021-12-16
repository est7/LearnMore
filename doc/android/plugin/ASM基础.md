# 1:依赖：

```java
implementation 'org.ow2.asm:asm:7.1'
implementation 'org.ow2.asm:asm-commons:7.1'
```

# 2：关键类：

```java
ClassReader：读取calss 文件
ClassWriter ：写入class
ClassVisitror:访问class 属性，方法  
```

# 3：使用：

```java
 /**
     * 添加监控代码到class 文件
     * @param file
     */
    static void addMonitorByteCode(File file) {
        if(file == null || !file.exists()){
            return
        }
        def fileInputStream = new FileInputStream(file)
        def classReader = new ClassReader(fileInputStream)//输入文件流
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);//写入读文件
        def classVisitor = new ChangeVisitor(Opcodes.ASM7,classWriter)//访问文件- 参数1：协议
        classReader.accept(classVisitor,ClassReader.EXPAND_FRAMES)//开始访问
            //修改完成 开始写入源文件
        byte[] data = classWriter.toByteArray()
        fileInputStream.close()
        def fileOutputStream = new FileOutputStream(file)
        fileOutputStream.write(data)
        fileOutputStream.close()
    }

//关键类  class 访问器
    static class ChangeVisitor extends ClassVisitor {

        ChangeVisitor(int api,ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        //方法访问回调
        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (name.equals("<init>") || name.equals("<clinit>")) {//init clinit 构造方法
                return methodVisitor;
            }
            //使用方法适配器。来确定方法进出 添加代码
            return new AdviceAdapterImpl(Opcodes.ASM7, methodVisitor, access, name, desc);
        }

        //注解
        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return super.visitAnnotation(descriptor, visible)
        }
    }


    static class AdviceAdapterImpl extends AdviceAdapter{

        protected AdviceAdapterImpl(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor)
            System.out.println("AdviceAdapterImpl : name = "+name);
        }

        @Override
        void visitCode() {
            super.visitCode()
            System.out.println("AdviceAdapterImpl : 开始 name = "+name);
        }

        //进入方法，添加字节码
        @Override
        protected void onMethodEnter() {
            super.onMethodEnter()
            System.out.println("AdviceAdapterImpl : 进入 name = "+name);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLineNumber(17, label2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LSTORE, 2);
        }

        //退出方法添加字节码
        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode)
            System.out.println("AdviceAdapterImpl : 退出 name = "+name);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitLineNumber(18, label3);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LSTORE, 4);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitLineNumber(19, label4);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            String methodMsg  = " - method: "+name+" run time : ";
            mv.visitLdcInsn(methodMsg);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(LLOAD, 4);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitInsn(LSUB);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        @Override
        void visitEnd() {
            super.visitEnd()
            System.out.println("AdviceAdapterImpl : 结束 name = "+name);
        }
    }
```



//源文件

```java
//java 文件 onCreate 耗时
public class MainActivity extends AppCompatActivity {
    public MainActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        long var2 = System.currentTimeMillis();//开始时间
        
        super.onCreate(savedInstanceState);
        this.setContentView(layoutid);
        
        long var4 = System.currentTimeMillis();//结束时间
        System.out.println(this.getClass().getName() + " - method: onCreate run time : " + (var4 - var2));
    }
}
```

//字节码文件

```java
// class version 52.0 (52)
// access flags 0x21
public class com/example/asmtest/MainActivity extends androidx/appcompat/app/AppCompatActivity {

  // compiled from: MainActivity.java
  // access flags 0x19
  public final static INNERCLASS com/example/asmtest/R$layout com/example/asmtest/R layout

  // access flags 0x1
  public <init>()V
   L0
    LINENUMBER 7 L0
    ALOAD 0
    INVOKESPECIAL androidx/appcompat/app/AppCompatActivity.<init> ()V
    RETURN
   L1
    LOCALVARIABLE this Lcom/example/asmtest/MainActivity; L0 L1 0
    MAXSTACK = 1
    MAXLOCALS = 1

  // access flags 0x4
  protected onCreate(Landroid/os/Bundle;)V
    // parameter  savedInstanceState
   L0
    LINENUMBER 17 L0
    INVOKESTATIC java/lang/System.currentTimeMillis ()J//在这里将要添加的
    LSTORE 2
   L1
    LINENUMBER 11 L1
    ALOAD 0
    ALOAD 1
    INVOKESPECIAL androidx/appcompat/app/AppCompatActivity.onCreate (Landroid/os/Bundle;)V
   L2
    LINENUMBER 12 L2
    ALOAD 0
    LDC 2131427356
    INVOKEVIRTUAL com/example/asmtest/MainActivity.setContentView (I)V
   L3                                                    //从这里将要添加的
    LINENUMBER 13 L3
    LINENUMBER 18 L3
    INVOKESTATIC java/lang/System.currentTimeMillis ()J
    LSTORE 4
   L4
    LINENUMBER 19 L4
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    NEW java/lang/StringBuilder
    DUP
    INVOKESPECIAL java/lang/StringBuilder.<init> ()V
    ALOAD 0
    INVOKEVIRTUAL java/lang/Object.getClass ()Ljava/lang/Class;
    INVOKEVIRTUAL java/lang/Class.getName ()Ljava/lang/String;
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    LDC " - method: onCreate run time : "
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    LLOAD 4
    LLOAD 2
    LSUB
    INVOKEVIRTUAL java/lang/StringBuilder.append (J)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
    RETURN
   L5
    LOCALVARIABLE this Lcom/example/asmtest/MainActivity; L1 L5 0
    LOCALVARIABLE savedInstanceState Landroid/os/Bundle; L1 L5 1
    MAXSTACK = 6
    MAXLOCALS = 6
}
```



自己写这些估计会崩溃，万一错了，很尴尬。。。。

插件来帮我们：

![image-20211215101657755](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112151016814.png)

内容：Java ---- >ASM 添加字节码 -->摘取自己想要的

![image-20211215101955285](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112151019333.png)



```java
package asm.com.example.asmtest;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;

public class MainActivityDump implements Opcodes {

    public static byte[] dump() throws Exception {

        ClassWriter classWriter = new ClassWriter(0);
        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;
        AnnotationVisitor annotationVisitor0;

        classWriter.visit(V1_8, ACC_PUBLIC | ACC_SUPER, "com/example/asmtest/MainActivity", null, "androidx/appcompat/app/AppCompatActivity", null);

        classWriter.visitSource("MainActivity.java", null);

        classWriter.visitInnerClass("com/example/asmtest/R$layout", "com/example/asmtest/R", "layout", ACC_PUBLIC | ACC_FINAL | ACC_STATIC);

        {
            methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(7, label0);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "androidx/appcompat/app/AppCompatActivity", "<init>", "()V", false);
            methodVisitor.visitInsn(RETURN);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLocalVariable("this", "Lcom/example/asmtest/MainActivity;", null, label0, label1, 0);
            methodVisitor.visitMaxs(1, 1);
            methodVisitor.visitEnd();
        }
        {
            methodVisitor = classWriter.visitMethod(ACC_PROTECTED, "onCreate", "(Landroid/os/Bundle;)V", null, null);
            methodVisitor.visitParameter("savedInstanceState", 0);
            methodVisitor.visitCode();
            Label label0 = new Label();
            methodVisitor.visitLabel(label0);
            methodVisitor.visitLineNumber(17, label0);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            methodVisitor.visitVarInsn(LSTORE, 2);
            Label label1 = new Label();
            methodVisitor.visitLabel(label1);
            methodVisitor.visitLineNumber(11, label1);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitVarInsn(ALOAD, 1);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "androidx/appcompat/app/AppCompatActivity", "onCreate", "(Landroid/os/Bundle;)V", false);
            Label label2 = new Label();
            methodVisitor.visitLabel(label2);
            methodVisitor.visitLineNumber(12, label2);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitLdcInsn(new Integer(2131427356));
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "com/example/asmtest/MainActivity", "setContentView", "(I)V", false);
            Label label3 = new Label();
            methodVisitor.visitLabel(label3);
            methodVisitor.visitLineNumber(13, label3);
            methodVisitor.visitLineNumber(18, label3);
            methodVisitor.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            methodVisitor.visitVarInsn(LSTORE, 4);
            Label label4 = new Label();
            methodVisitor.visitLabel(label4);
            methodVisitor.visitLineNumber(19, label4);
            methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
            methodVisitor.visitInsn(DUP);
            methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            methodVisitor.visitVarInsn(ALOAD, 0);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitLdcInsn(" - method: onCreate run time : ");
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitVarInsn(LLOAD, 4);
            methodVisitor.visitVarInsn(LLOAD, 2);
            methodVisitor.visitInsn(LSUB);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            methodVisitor.visitInsn(RETURN);
            Label label5 = new Label();
            methodVisitor.visitLabel(label5);
            methodVisitor.visitLocalVariable("this", "Lcom/example/asmtest/MainActivity;", null, label1, label5, 0);
            methodVisitor.visitLocalVariable("savedInstanceState", "Landroid/os/Bundle;", null, label1, label5, 1);
            methodVisitor.visitMaxs(6, 6);
            methodVisitor.visitEnd();
        }
        classWriter.visitEnd();

        return classWriter.toByteArray();
    }
}

```



实战：android transform 添加 plugin 

添加方法耗时：

```java
/**
     * 添加监控代码到class 文件
     * @param file
     */
    static void addMonitorByteCode(File file) {
        if(file == null || !file.exists()){
            return
        }
        def fileInputStream = new FileInputStream(file)
        def classReader = new ClassReader(fileInputStream)//输入文件流
        def classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);//写入读文件
        def classVisitor = new ChangeVisitor(Opcodes.ASM7,classWriter)//访问文件- 参数1：协议
        classReader.accept(classVisitor,ClassReader.EXPAND_FRAMES)//开始访问
            //修改完成 开始写入源文件
        byte[] data = classWriter.toByteArray()
        fileInputStream.close()
        def fileOutputStream = new FileOutputStream(file)
        fileOutputStream.write(data)
        fileOutputStream.close()
    }

//关键类  class 访问器
    static class ChangeVisitor extends ClassVisitor {

        ChangeVisitor(int api,ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        //方法访问回调
        @Override
        MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
            if (name.equals("<init>") || name.equals("<clinit>")) {//init clinit 构造方法
                return methodVisitor;
            }
            //使用方法适配器。来确定方法进出 添加代码
            return new AdviceAdapterImpl(Opcodes.ASM7, methodVisitor, access, name, desc);
        }

        //注解
        @Override
        AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return super.visitAnnotation(descriptor, visible)
        }
    }


    static class AdviceAdapterImpl extends AdviceAdapter{

        protected AdviceAdapterImpl(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
            super(api, methodVisitor, access, name, descriptor)
            System.out.println("AdviceAdapterImpl : name = "+name);
        }

        @Override
        void visitCode() {
            super.visitCode()
            System.out.println("AdviceAdapterImpl : 开始 name = "+name);
        }

        //进入方法，添加字节码
        @Override
        protected void onMethodEnter() {
            super.onMethodEnter()
            System.out.println("AdviceAdapterImpl : 进入 name = "+name);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLineNumber(17, label2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LSTORE, 2);
        }

        //退出方法添加字节码
        @Override
        protected void onMethodExit(int opcode) {
            super.onMethodExit(opcode)
            System.out.println("AdviceAdapterImpl : 退出 name = "+name);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitLineNumber(18, label3);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
            mv.visitVarInsn(LSTORE, 4);
            Label label4 = new Label();
            mv.visitLabel(label4);
            mv.visitLineNumber(19, label4);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            String methodMsg  = " - method: "+name+" run time : ";
            mv.visitLdcInsn(methodMsg);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(LLOAD, 4);
            mv.visitVarInsn(LLOAD, 2);
            mv.visitInsn(LSUB);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }

        @Override
        void visitEnd() {
            super.visitEnd()
            System.out.println("AdviceAdapterImpl : 结束 name = "+name);
        }
    }
```



效果：

java源文件：

![image-20211215102600817](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112151026873.png)

生成CLASS 文件

![image-20211215102537012](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202112151025096.png)

