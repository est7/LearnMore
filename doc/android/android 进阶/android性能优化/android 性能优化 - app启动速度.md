# 1:启动速度耗时统计

## 命令：adb shell am start -S -W 包名/Activity名称

WaitTime:从关闭上一个应用 到 新应用的进程创建启动的的整体等待时间

TotalTime:新应用从启动包括进程创建和Activity创建的的启动耗时，但是不包括前一个应用Activity pause 的耗时

ThisTime:一连串Actiivty 启动到最后一个Activity启动的整体耗时

优化路径：

整体Application 到 第一个MainActivity 的排查耗时操作

## 可排查点：

1：IO 操作

2：第三框架初始化

## 观察工具：第一种

低版本：使用TraceView

高版本：CPU Profiling

注意点：

设置Profiling中  android 8.0 以上使用

CPU activity on startUp

选择Trace java Methods  跟踪java堆栈

Sample Java Methods 是采样跟踪jajva 方法，所以不够准确，容易遗漏，不使用他

橙色：系统代码

绿色：自己代码

蓝色：第三方

这个颜色分类不是很准确

解析后tabl

CallChart 		看大概



FlameChart 	 火焰图 看细节



TopDown	 	所有调用链接，进行耗时排序，进行层层调用耗时分析，就可以找到最耗时点，

​						也可以看调用链



BottomTop 查看 每一个方法的的被调用来源

1：发现大部分耗时可能会存在与 setContentVIew（）-> inflater() xml 布局中

​	所有会需要对布局进行优化

 *  布局层级太深，

    ----> 可以对布局层级进行优化

 *  布局层级很简单，怎么办？ 

    ---> 可以使用asynclayoutinflater 库 使用一部加载布局

    code：

    ```java
    //引用
    implementation "androidx.asynclayoutinflater:asynclayoutinfalte:1.0.0"
    
    new AsyncLlayoutInFlater(context,this).infalte(R.layout.main_activity,null,
           new AsyncLayoutInflater.OninflaterFinishedListener(){
         	public void onInflateFinished(View view,int resid,VIewGroup parent{
                setContentView(view);
            }                                             
         })
    ```

    

## 观察工具：第二种StrictMode 严苛模式 































