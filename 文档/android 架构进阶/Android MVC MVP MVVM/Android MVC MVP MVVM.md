# MVC

android MVC 模型 ： 

问题在于 Controller (Activity fragment) 即会承担mode 也会承担view 的职责。

违背java oop 职责单一原则



MVC很容易引起内存泄露（GCroot--->线程--->虚拟机栈---->栈桢）



# MVP 思想精髓

![image-20200627155609270](Android MVC MVP MVVM.assets/image-20200627155609270.png)

Persenter 负责转发View 和Model 的实际逻辑

1：创建一个协议类。来声明Persenter View Model  中 PM   PV  的协议接口

2：Activity/ Persenter 实现PV 协议    Persenter/Model 实现PM协议接口

3：Activity 初始化 Persenter ，调用persente 方法

​      Persenter  持有Activity   ，初始化Model，

​      persenter  中PV接口 调用 Model PM实现方法

​      PM完成后，回调 persenter 中PM----PV-----Activity



可能存在内存泄露问题；persenter 持有  Activity



# MVP  思路提升 与基础框架搭建



