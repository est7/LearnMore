 自定义View、多线程、网络，被认为是Android开发者必须牢固掌握的最基础的三大基本功。Android View的绘制流程原理又是学好自定义View的理论基础，所以掌握好View的绘制原理是Android开发进阶中无法绕过的一道坎。而关乎到原理性的东西往往又让很多初学者感到难以下手，所以真正掌握的人并不多。本文采用非常浅显的语言，从顺着Android源码的思路，对View的整个绘制流程进行近乎“地毯式搜索”般的方式，对其中的关键流程和知识点进行查证和分析，以图让初级程序员都能轻松读懂。本文最大的特点，就是最大限度地向源码要答案，从源码中追流程的来龙去脉，在注释中查功能的点点滴滴，所有的结论都尽量在源码和注释中找根据。

​    为了能对其中的重难点分析透彻，文中贴出了大量的源码依据以及源码中的注释，并对重要的注释进行了翻译和讲解，所以文章会比较长。讲解该知识点的文章普遍都非常长，所以希望读者能够秉承程序员吃苦耐劳的精神，攻克这个难关。本文中的源码是基于API26的，即Android8.0系统版本，主要内容大致如下：

 

![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190528103310149-1836591885.png)

 

**一、View绘制的三个流程**

​    我们知道，在自定义View的时候一般需要重写父类的onMeasure()、onLayout()、onDraw()三个方法，来完成视图的展示过程。当然，这三个暴露给开发者重写的方法只不过是整个绘制流程的冰山一角，更多复杂的幕后工作，都让系统给代劳了。一个完整的绘制流程包括measure、layout、draw三个步骤，其中：

   measure：测量。系统会先根据xml布局文件和代码中对控件属性的设置，来获取或者计算出每个View和ViewGrop的尺寸，并将这些尺寸保存下来。

   layout：布局。根据测量出的结果以及对应的参数，来确定每一个控件应该显示的位置。

   draw：绘制。确定好位置后，就将这些控件绘制到屏幕上。

 

**二、Android视图层次结构简介** 

​    在介绍View绘制流程之前，咱们先简单介绍一下Android视图层次结构以及DecorView，因为View的绘制流程的入口和DecorView有着密切的联系。

 ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190518164042396-2093768305.png)

​    咱们平时看到的视图，其实存在如上的嵌套关系。上图是针对比较老的Android系统版本中制作的，新的版本中会略有出入，还有一个状态栏，但整体上没变。我们平时在Activity中setContentView(...)中对应的layout内容，对应的是上图中ViewGrop的树状结构，实际上添加到系统中时，会再裹上一层FrameLayout，就是上图中最里面的浅蓝色部分了。

​    这里咱们再通过一个实例来继续查看。AndroidStudio工具中提供了一个布局视察器工具，通过Tools > Android > Layout Inspector可以查看具体某个Activity的布局情况。下图中，左边树状结构对应了右边的可视图，可见DecorView是整个界面的根视图，对应右边的红色框，是整个屏幕的大小。黄色边框为状态栏部分；那个绿色边框中有两个部分，一个是白框中的ActionBar，对应了上图中紫色部分的TitleActionBar部分，即标题栏，平时咱们可以在Activity中将其隐藏掉；另外一个蓝色边框部分，对应上图中最里面的蓝色部分，即ContentView部分。下图中左边有两个蓝色框，上面那个中有个“contain_layout”，这个就是Activity中setContentView中设置的layout.xml布局文件中的最外层父布局，咱们能通过layout布局文件直接完全操控的也就是这一块，当其被add到视图系统中时，会被系统裹上ContentFrameLayout（显然是FrameLayout的子类），这也就是为什么添加layout.xml视图的方法叫setContentView(...)而不叫setView(...)的原因。

 ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190518170653465-864329761.png)

 

**三、故事开始的地方**

​    如果对Activity的启动流程有一定了解的话，应该知道这个启动过程会在ActivityThread.java类中完成，在启动Activity的过程中，会调用到handleResumeActivity(...)方法，关于视图的绘制过程最初就是从这个方法开始的。

 

 1、View绘制起源UML时序图

​    整个调用链如下图所示，直到ViewRootImpl类中的performTraversals()中，才正式开始绘制流程了，所以一般都是以该方法作为正式绘制的源头。

![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190520105432689-870375609.png)

图3.1 View绘制起源UML时序图

 

  2、handleResumeActivity()方法

​    在这咱们先大致看看ActivityThread类中的handleResumeActivity方法，咱们这里只贴出关键代码：

[![复制代码](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/copycode.gif)](javascript:void(0);)

```
 1 //===========ActivityThread.java==========
 2 final void handleResumeActivity(...) {
 3     ......
 4     //跟踪代码后发现其初始赋值为mWindow = new PhoneWindow(this, window, activityConfigCallback);
 5     r.window = r.activity.getWindow(); 
 6        //从PhoneWindow实例中获取DecorView  
 7     View decor = r.window.getDecorView();
 8     ......
 9     //跟踪代码后发现，vm值为上述PhoneWindow实例中获取的WindowManager。
10     ViewManager wm = a.getWindowManager();
11     ......
12     //当前window的属性，从代码跟踪来看是PhoneWindow窗口的属性
13     WindowManager.LayoutParams l = r.window.getAttributes();
14     ......
15     wm.addView(decor, l);
16     ......
17 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    上述代码第8行中，ViewManager是一个接口，addView是其中定义个一个空方法，WindowManager是其子类，WindowManagerImpl是WindowManager的实现类（顺便啰嗦一句，这种方式叫做面向接口编程，在父类中定义，在子类中实现，在Java中很常见）。第4行代码中的r.window的值可以根据Activity.java的如下代码得知，其值为PhoneWindow实例。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===============Activity.java=============
 2 private Window mWindow;
 3 public Window getWindow() {
 4    return mWindow;
 5 }
 6 
 7 final void attach(...){
 8    ......
 9    mWindow = new PhoneWindow(this, window, activityConfigCallback);
10    ......
11 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 

3、两个重要参数分析

​    之所以要在这里特意分析handleResumeActivity()方法，除了因为它是整个绘制流程的最初源头外，还有就是addView的两个参数比较重要，它们经过一层一层传递后进入到ViewRootImpl中，在后面分析绘制中要用到。这里再看看这两个参数的相关信息：

  （1）参数decor

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //==========PhoneWindow.java===========
 2 // This is the top-level view of the window, containing the window decor.
 3 private DecorView mDecor;
 4 ......
 5 public PhoneWindow(...){
 6    ......
 7    mDecor = (DecorView) preservedWindow.getDecorView();
 8    ......
 9 }
10 
11 @Override
12 public final View getDecorView() {
13    ......
14    return mDecor;
15 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

可见decor参数表示的是DecorView实例。注释中也有说明：这是window的顶级视图，包含了window的decor。

  （2）参数l

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===================Window.java===================
 2 //The current window attributes.
 3     private final WindowManager.LayoutParams mWindowAttributes =
 4         new WindowManager.LayoutParams();
 5 ......
 6 public final WindowManager.LayoutParams getAttributes() {
 7         return mWindowAttributes;
 8     }
 9 ......
10 
11 
12 //==========WindowManager.java的内部类LayoutParams extends ViewGroup.LayoutParams=============
13 public LayoutParams() {
14             super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
15             ......
16         }
17 
18 
19 //==============ViewGroup.java内部类LayoutParams====================
20 public LayoutParams(int width, int height) {
21             this.width = width;
22             this.height = height;
23         }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

该参数表示l的是PhoneWindow的LayoutParams属性，其width和height值均为LayoutParams.MATCH_PARENT。

 

​    在源码中，WindowPhone和DecorView通过组合方式联系在一起的，而DecorView是整个View体系的根View。在前面handleResumeActivity(...)方法代码片段中，当Actiivity启动后，就通过第14行的addView方法，来间接调用ViewRootImpl类中的performTraversals()，从而实现视图的绘制。

 

**四、主角登场** 

```
   无疑，performTraversals()方法是整个过程的主角，它把控着整个绘制的流程。该方法的源码有大约800行，这里咱们仅贴出关键的流程代码，如下所示：
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 // =====================ViewRootImpl.java=================
 2 private void performTraversals() {
 3    ......
 4    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
 5    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);      
 6    ......
 7    // Ask host how big it wants to be
 8    performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);
 9    ......
10    performLayout(lp, mWidth, mHeight);
11    ......
12    performDraw();
13 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 上述代码中就是一个完成的绘制流程，对应上了第一节中提到的三个步骤：

   1）performMeasure()：从根节点向下遍历View树，完成所有ViewGroup和View的测量工作，计算出所有ViewGroup和View显示出来需要的高度和宽度；

   2）performLayout()：从根节点向下遍历View树，完成所有ViewGroup和View的布局计算工作，根据测量出来的宽高及自身属性，计算出所有ViewGroup和View显示在屏幕上的区域；

   3）performDraw()：从根节点向下遍历View树，完成所有ViewGroup和View的绘制工作，根据布局过程计算出的显示区域，将所有View的当前需显示的内容画到屏幕上。

咱们后续就是通过对这三个方法来展开研究整个绘制过程。

 

**五、measure过程分析**

​    这三个绘制流程中，measure是最复杂的，这里会花较长的篇幅来分析它。本节会先介绍整个流程中很重要的两个类MeasureSpec和ViewGroup.LayoutParams类，然后介绍ViewRootImpl、View及ViewGroup中测量流程涉及到的重要方法，最后简单梳理DecorView测量的整个流程并链接一个测量实例分析整个测量过程。

 

 1、MeasureSpec简介

​    这里咱们直接上源码吧，先直接通过源码和注释认识一下它，如果看不懂也没关系，在后面使用的时候再回头来看看。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * A MeasureSpec encapsulates the layout requirements passed from parent to child.
 3      * Each MeasureSpec represents a requirement for either the width or the height.
 4      * A MeasureSpec is comprised of a size and a mode. There are three possible
 5      * modes:
 6      * <dl>
 7      * <dt>UNSPECIFIED</dt>
 8      * <dd>
 9      * The parent has not imposed any constraint on the child. It can be whatever size
10      * it wants.
11      * </dd>
12      *
13      * <dt>EXACTLY</dt>
14      * <dd>
15      * The parent has determined an exact size for the child. The child is going to be
16      * given those bounds regardless of how big it wants to be.
17      * </dd>
18      *
19      * <dt>AT_MOST</dt>
20      * <dd>
21      * The child can be as large as it wants up to the specified size.
22      * </dd>
23      * </dl>
24      *
25      * MeasureSpecs are implemented as ints to reduce object allocation. This class
26      * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
27      */
28     public static class MeasureSpec {
29         private static final int MODE_SHIFT = 30;
30         private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
31         ......
32         /**
33          * Measure specification mode: The parent has not imposed any constraint
34          * on the child. It can be whatever size it wants.
35          */
36         public static final int UNSPECIFIED = 0 << MODE_SHIFT;
37 
38         /**
39          * Measure specification mode: The parent has determined an exact size
40          * for the child. The child is going to be given those bounds regardless
41          * of how big it wants to be.
42          */
43         public static final int EXACTLY     = 1 << MODE_SHIFT;
44 
45         /**
46          * Measure specification mode: The child can be as large as it wants up
47          * to the specified size.
48          */
49         public static final int AT_MOST     = 2 << MODE_SHIFT;
50         ......
51        /**
52          * Creates a measure specification based on the supplied size and mode.
53          *...... 
54          *@return the measure specification based on size and mode        
55          */
56         public static int makeMeasureSpec(@IntRange(from = 0, to = (1 << MeasureSpec.MODE_SHIFT) - 1) int size,
57                                           @MeasureSpecMode int mode) {
58             if (sUseBrokenMakeMeasureSpec) {
59                 return size + mode;
60             } else {
61                 return (size & ~MODE_MASK) | (mode & MODE_MASK);
62             }
63             ......
64             
65         }
66         ......
67         /**
68          * Extracts the mode from the supplied measure specification.
69          *......
70          */
71         @MeasureSpecMode
72         public static int getMode(int measureSpec) {
73             //noinspection ResourceType
74             return (measureSpec & MODE_MASK);
75         }
76 
77         /**
78          * Extracts the size from the supplied measure specification.
79          *......
80          * @return the size in pixels defined in the supplied measure specification
81          */
82         public static int getSize(int measureSpec) {
83             return (measureSpec & ~MODE_MASK);
84         }
85         ......
86 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 从这段代码中，咱们可以得到如下的信息：

  1）MeasureSpec概括了从父布局传递给子view布局要求。每一个MeasureSpec代表了宽度或者高度要求，它由size（尺寸）和mode（模式）组成。

  2）有三种可能的mode：UNSPECIFIED、EXACTLY、AT_MOST

  3）UNSPECIFIED：未指定尺寸模式。父布局没有对子view强加任何限制。它可以是任意想要的尺寸。（笔者注：这个在工作中极少碰到，据说一般在系统中才会用到，后续会讲得很少）

  4）EXACTLY：精确值模式。父布局决定了子view的准确尺寸。子view无论想设置多大的值，都将限定在那个边界内。（笔者注：也就是layout_width属性和layout_height属性为具体的数值，如50dp，或者设置为match_parent，设置为match_parent时也就明确为和父布局有同样的尺寸，所以这里不要以为笔者搞错了。当明确为精确的尺寸后，其也就被给定了一个精确的边界）

  5）AT_MOST：最大值模式。子view可以一直大到指定的值。（笔者注：也就是其宽高属性设置为wrap_content，那么它的最大值也不会超过父布局给定的值，所以称为最大值模式）

  6）MeasureSpec被实现为int型来减少对象分配。该类用于将size和mode元组装包和拆包到int中。（笔者注：也就是将size和mode组合或者拆分为int型数据）

  7）分析代码可知，一个MeasureSpec的模式如下所示，int长度为32位置，高2位表示mode，后30位用于表示size

​      ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190521193635415-455047678.png)

   8）UNSPECIFIED、EXACTLY、AT_MOST这三个mode的示意图如下所示：

​       ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190521194344561-115486598.png)

  9）makeMeasureSpec（int mode，int size）用于将mode和size打包成一个int型的MeasureSpec。

  10）getSize(int measureSpec)方法用于从指定的measureSpec值中获取其size。

  11）getMode(int measureSpec)方法用户从指定的measureSpec值中获取其mode。

 

 2、ViewGroup.LayoutParams简介

  该类的源码及注释分析如下所示。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //============================ViewGroup.java===============================
 2 /**
 3      * LayoutParams are used by views to tell their parents how they want to be
 4      * laid out. 
 5      *......
 6      * <p>
 7      * The base LayoutParams class just describes how big the view wants to be
 8      * for both width and height. For each dimension, it can specify one of:
 9      * <ul>
10      * <li>FILL_PARENT (renamed MATCH_PARENT in API Level 8 and higher), which
11      * means that the view wants to be as big as its parent (minus padding)
12      * <li> WRAP_CONTENT, which means that the view wants to be just big enough
13      * to enclose its content (plus padding)
14      * <li> an exact number
15      * </ul>
16      * There are subclasses of LayoutParams for different subclasses of
17      * ViewGroup. For example, AbsoluteLayout has its own subclass of
18      * LayoutParams which adds an X and Y value.</p>
19      * ......
20      * @attr ref android.R.styleable#ViewGroup_Layout_layout_height
21      * @attr ref android.R.styleable#ViewGroup_Layout_layout_width
22      */
23     public static class LayoutParams {
24         ......
25 
26         /**
27          * Special value for the height or width requested by a View.
28          * MATCH_PARENT means that the view wants to be as big as its parent,
29          * minus the parent's padding, if any. Introduced in API Level 8.
30          */
31         public static final int MATCH_PARENT = -1;
32 
33         /**
34          * Special value for the height or width requested by a View.
35          * WRAP_CONTENT means that the view wants to be just large enough to fit
36          * its own internal content, taking its own padding into account.
37          */
38         public static final int WRAP_CONTENT = -2;
39 
40         /**
41          * Information about how wide the view wants to be. Can be one of the
42          * constants FILL_PARENT (replaced by MATCH_PARENT
43          * in API Level 8) or WRAP_CONTENT, or an exact size.
44          */
45         public int width;
46 
47         /**
48          * Information about how tall the view wants to be. Can be one of the
49          * constants FILL_PARENT (replaced by MATCH_PARENT
50          * in API Level 8) or WRAP_CONTENT, or an exact size.
51          */
52         public int height;
53         ......
54 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 这对其中重要的信息做一些翻译和整理：

  1）LayoutParams被view用于告诉它们的父布局它们想要怎样被布局。（笔者注：字面意思就是布局参数）

  2）该LayoutParams基类仅仅描述了view希望宽高有多大。对于每一个宽或者高，可以指定为以下三种值中的一个：MATCH_PARENT,WRAP_CONTENT,an exact number。（笔者注：FILL_PARENT从API8开始已经被MATCH_PARENT取代了，所以下文就只提MATCH_PARENT）

  3）MATCH_PARENT：意味着该view希望和父布局尺寸一样大，如果父布局有padding，则要减去该padding值。

  4）WRAP_CONTENT：意味着该view希望其大小为仅仅足够包裹住其内容即可，如果自己有padding，则要加上该padding值。

  5）对ViewGroup不同的子类，也有相应的LayoutParams子类。 

  6）其width和height属性对应着layout_width和layout_height属性。

 

 3、View测量的基本流程及重要方法分析

​    View体系的测量是从DecorView这个根view开始递归遍历的，而这个View体系树中包含了众多的叶子view和ViewGroup的子类容器。这一小节中会从ViewRootImpl.performMeasure()开始，分析测量的基本流程。

   （1）ViewRootImpl.performMeasure()方法

​    跟踪源码，进入到performMeasure方法分析，这里仅贴出关键流程代码。

```
1 //=============ViewRootImpl.java==============
2 private void performMeasure(int childWidthMeasureSpec, int childHeightMeasureSpec) {
3        ......
4        mView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
5        ......
6 }
```

 这个mView是谁呢？跟踪代码可以找到给它赋值的地方：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //========================ViewRootImpl.java======================
2 public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
3       ......
4       mView = view;
5       ......
6 
7       mWindowAttributes.copyFrom(attrs);
8       ......
9 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    看到这里，是不是有些似曾相识呢？在第二节的绘制流程中提到过，这里setView的参数view和attrs是ActivityThread类中addView方法传递过来的，所以咱们这里可以确定mView指的是DecorView了。上述performMeasure()中，其实就是DecorView在执行measure()操作。如果您这存在“mView不是View类型的吗，怎么会指代DecorView作为整个View体系的根view呢”这样的疑惑，那这里就啰嗦一下，DecorView extends FrameLayout extends ViewGroup extends View，通过这个继承链可以看到，DecorView是一个容器，但ViewGroup也是View的子类，View是所有控件的基类，所以这里View类型的mView指代DecorView是没毛病的。

  （2）View.measure()方法

​    尽管mView就是DecorView，但是由于measure()方法是final型的，View子类都不能重写该方法，所以这里追踪measure()的时候就直接进入到View类中了，这里贴出关键流程代码：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===========================View.java===============================
 2 /**
 3      * <p>
 4      * This is called to find out how big a view should be. The parent
 5      * supplies constraint information in the width and height parameters.
 6      * </p>
 7      *
 8      * <p>
 9      * The actual measurement work of a view is performed in
10      * {@link #onMeasure(int, int)}, called by this method. Therefore, only
11      * {@link #onMeasure(int, int)} can and must be overridden by subclasses.
12      * </p>
13      *
14      *
15      * @param widthMeasureSpec Horizontal space requirements as imposed by the
16      *        parent
17      * @param heightMeasureSpec Vertical space requirements as imposed by the
18      *        parent
19      *
20      * @see #onMeasure(int, int)
21      */
22 public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
23       ......
24       // measure ourselves, this should set the measured dimension flag back
25       onMeasure(widthMeasureSpec, heightMeasureSpec);
26       ......
27 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    这里面注释提供了很多信息，这简单翻译并整理一下：

​    1）该方法被调用，用于找出view应该多大。父布局在witdh和height参数中提供了限制信息；

​    2）一个view的实际测量工作是在被本方法所调用的onMeasure(int，int)方法中实现的。所以，只有onMeasure(int,int)可以并且必须被子类重写（笔者注：这里应该指的是，ViewGroup的子类必须重写该方法，才能绘制该容器内的子view。如果是自定义一个子控件，extends View，那么并不是必须重写该方法）；

​    3）参数widthMeasureSpec：父布局加入的水平空间要求；

​    4）参数heightMeasureSpec：父布局加入的垂直空间要求。

​    系统将其定义为一个final方法，可见系统不希望整个测量流程框架被修改。

  （3）View.onMeasure()方法

​    在上述方法体内看到onMeasure(int,int)方法时，是否有一丝慰藉呢？终于看到咱们最熟悉的身影了，很亲切吧！咱们编写自定义View时，基本上都会重写的方法！咱们看看其源码：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===========================View.java===============================
 2 /**
 3      * <p>
 4      * Measure the view and its content to determine the measured width and the
 5      * measured height. This method is invoked by {@link #measure(int, int)} and
 6      * should be overridden by subclasses to provide accurate and efficient
 7      * measurement of their contents.
 8      * </p>
 9      *
10      * <p>
11      * <strong>CONTRACT:</strong> When overriding this method, you
12      * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
13      * measured width and height of this view. Failure to do so will trigger an
14      * <code>IllegalStateException</code>, thrown by
15      * {@link #measure(int, int)}. Calling the superclass'
16      * {@link #onMeasure(int, int)} is a valid use.
17      * </p>
18      *
19      * <p>
20      * The base class implementation of measure defaults to the background size,
21      * unless a larger size is allowed by the MeasureSpec. Subclasses should
22      * override {@link #onMeasure(int, int)} to provide better measurements of
23      * their content.
24      * </p>
25      *
26      * <p>
27      * If this method is overridden, it is the subclass's responsibility to make
28      * sure the measured height and width are at least the view's minimum height
29      * and width ({@link #getSuggestedMinimumHeight()} and
30      * {@link #getSuggestedMinimumWidth()}).
31      * </p>
32      *
33      * @param widthMeasureSpec horizontal space requirements as imposed by the parent.
34      *                         The requirements are encoded with
35      *                         {@link android.view.View.MeasureSpec}.
36      * @param heightMeasureSpec vertical space requirements as imposed by the parent.
37      *                         The requirements are encoded with
38      *                         {@link android.view.View.MeasureSpec}.
39      *
40      * @see #getMeasuredWidth()
41      * @see #getMeasuredHeight()
42      * @see #setMeasuredDimension(int, int)
43      * @see #getSuggestedMinimumHeight()
44      * @see #getSuggestedMinimumWidth()
45      * @see android.view.View.MeasureSpec#getMode(int)
46      * @see android.view.View.MeasureSpec#getSize(int)
47      */
48     protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
49         setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
50                 getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
51     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    函数体内也就一句代码而已，注释却写了这么一大堆，可见这个方法的重要性了。这里翻译和整理一下这些注释：

   1）测量该view以及它的内容来决定测量的宽度和高度。该方法被measure(int，int)（笔者注：就是前面提到过的那个方法）调用，并且应该被子类重写来提供准确而且有效的对它们的内容的测量。

   2）当重写该方法时，您必须调用setMeasuredDimension(int,int)来存储该view测量出的宽和高。如果不这样做将会触发IllegalStateException，由measure(int,int)抛出。调用基类的onMeasure(int,int)方法是一个有效的方法。

   3）测量的基类实现默认为背景的尺寸，除非更大的尺寸被MeasureSpec所允许。子类应该重写onMeasure(int,int)方法来提供对内容更好的测量。

   4）如果该方法被重写，子类负责确保测量的高和宽至少是该view的mininum高度和mininum宽度值（链接getSuggestedMininumHeight()和getSuggestedMininumWidth()）；

   5） widthMeasureSpec：父布局加入的水平空间要求。该要求被编码到android.view.View.MeasureSpec中。

   6）heightMeasureSpec：父布局加入的垂直空间要求。该要求被编码到android.view.View.MeasureSpec中。

​    注释中最后提到了7个方法，这些方法后面会再分析。注释中花了不少的篇幅对该方法进行说明，但读者恐怕对其中的一些信息表示有些懵吧，比如MeasureSpec是什么，mininum高度和mininum宽度值是怎么回事等，MeasureSpec在本节的开头介绍过，可以回头再看看，其它的后面会作进一步的阐述，到时候咱们再回头来看看这些注释。

​    注意：容器类控件都是ViewGroup的子类，如FrameLayout、LinearLayout等，都会重写onMeasure方法，根据自己的特性来进行测量；如果是叶子节点view，即最里层的控件，如TextView等，也可能会重写onMeasure方法，所以当流程走到onMeasure(...)时，流程可能就会切到那些重写的onMeasure()方法中去。最后通过从根View到叶子节点的遍历和递归，最终还是会在叶子view中调用setMeasuredDimension(...)来实现最终的测量。

  （4）View.setMeasuredDimension()方法

   继续看setMeasuredDimension方法：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * <p>This method must be called by {@link #onMeasure(int, int)} to store the
 3      * measured width and measured height. Failing to do so will trigger an
 4      * exception at measurement time.</p>
 5      *
 6      * @param measuredWidth The measured width of this view.  May be a complex
 7      * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
 8      * {@link #MEASURED_STATE_TOO_SMALL}.
 9      * @param measuredHeight The measured height of this view.  May be a complex
10      * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
11      * {@link #MEASURED_STATE_TOO_SMALL}.
12      */
13     protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
14         ......
15         setMeasuredDimensionRaw(measuredWidth, measuredHeight);
16     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

  这里需要重点关注注释中对参数的说明：

​    measuredWidth：该view被测量出宽度值。

​    measuredHeight：该view被测量出的高度值。

   到这个时候才正式明确提到宽度和高度，通过getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec)，参数由widthMeasureSpec变成了measuredWidth，即由“父布局加入的水平空间要求”转变为了view的宽度，measuredHeigh也是一样。咱们先继续追踪源码分析width的值：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * Returns the suggested minimum width that the view should use. This
 3      * returns the maximum of the view's minimum width
 4      * and the background's minimum width
 5      *  ({@link android.graphics.drawable.Drawable#getMinimumWidth()}).
 6      * <p>
 7      * When being used in {@link #onMeasure(int, int)}, the caller should still
 8      * ensure the returned width is within the requirements of the parent.
 9      *
10      * @return The suggested minimum width of the view.
11      */
12     protected int getSuggestedMinimumWidth() {
13         return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinimumWidth());
14     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    这个方法是干嘛用的呢？注释的翻译如下：

   1）返回建议该view应该使用的最小宽度值。该方法返回了view的最小宽度值和背景的最小宽度值（链接android.graphics.drawable.Drawable#getMinimumWidth()）之间的最大值。

   2）当在onMeasure(int,int)使用时，调用者应该仍然确保返回的宽度值在父布局的要求之内。

   3）返回值：view的建议最小宽度值。

   这其中提到的"mininum width“指的是在xml布局文件中该view的“android:minWidth"属性值，“background's minimum width”值是指“android:background”的宽度。该方法的返回值就是两者之间较大的那一个值，用来作为该view的最小宽度值，现在应该很容易理解了吧,当一个view在layout文件中同时设置了这两个属性时，为了两个条件都满足，自然要选择值大一点的那个了。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * Utility to return a default size. Uses the supplied size if the
 3      * MeasureSpec imposed no constraints. Will get larger if allowed
 4      * by the MeasureSpec.
 5      *
 6      * @param size Default size for this view
 7      * @param measureSpec Constraints imposed by the parent
 8      * @return The size this view should be.
 9      */
10     public static int getDefaultSize(int size, int measureSpec) {
11         int result = size;
12         int specMode = MeasureSpec.getMode(measureSpec);
13         int specSize = MeasureSpec.getSize(measureSpec);
14 
15         switch (specMode) {
16         case MeasureSpec.UNSPECIFIED:
17             result = size;
18             break;
19         case MeasureSpec.AT_MOST:
20         case MeasureSpec.EXACTLY:
21             result = specSize;
22             break;
23         }
24         return result;
25     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    通过本节开头的介绍，您应该对MeasureSpec有了一个比较明确的认识了，再看看getDefaultSize(int size，int measureSpec)方法，就很容易理解了。正如其注释中所说，如果父布局没有施加任何限制，即MeasureSpec的mode为UNSPECIFIED，那么返回值为参数中提供的size值。如果父布局施加了限制，则返回的默认尺寸为保存在参数measureSpec中的specSize值。所以到目前为止，需要绘制的宽和高值就被确定下来了。只是，我们还需要明确这两个值最初是从哪里传过来的，后面我们还会顺藤摸瓜，找到这两个尺寸的出处。

​    既然宽度值measuredWidth和高度值measuredHeight已经确定下来，我们继续追踪之前的setMeasuredDimension(int measuredWidth, int measuredHeight)方法，其内部最后调用了如下的方法：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * ......
 3      * @param measuredWidth The measured width of this view.  May be a complex
 4      * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
 5      * {@link #MEASURED_STATE_TOO_SMALL}.
 6      * @param measuredHeight The measured height of this view.  May be a complex
 7      * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
 8      * {@link #MEASURED_STATE_TOO_SMALL}.
 9      */
10     private void setMeasuredDimensionRaw(int measuredWidth, int measuredHeight) {
11         mMeasuredWidth = measuredWidth;
12         mMeasuredHeight = measuredHeight;
13         ......
14     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    到目前为止，View中的成员变量mMeasureWidth和mMeasureHeight就被赋值了，这也就意味着，View的测量就结束了。前面讲onMeasure()方法时介绍过，View子类（包括ViewGroup子类）通常会重写onMeasure()，当阅读FrameLayout、LinearLayout、TextView等重写的onMeasure()方法时，会发现它们最终都会调用setMeasuredDimension() 方法，从而完成测量。这里可以对应上前面介绍View.onMeasure()时，翻译注释的第2）点以及setMeasuredDimension()方法的注释说明。

  （5）getMeasureWidth()方法

​    在View的onMeasure()方法的注释中提到了该方法，这里顺便也介绍一下。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //==================View.java==============
2 public static final int MEASURED_SIZE_MASK = 0x00ffffff;
3 /**
4  * ......
5  * @return The raw measured width of this view.
6  */
7 public final int getMeasuredWidth() {
8    return mMeasuredWidth & MEASURED_SIZE_MASK;
9 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    获取原始的测量宽度值，一般会拿这个方法和layout执行后getWidth()方法做比较。该方法需要在setMeasuredDimension()方法执行后才有效，否则返回值为0。

  （6）getMeasureHeight()方法

​    在View的onMeasure()方法的注释中提到了该方法，这里顺便也介绍一下。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //==================View.java==============
2 /**
3   * ......
4   * @return The raw measured height of this view.
5   */
6 public final int getMeasuredHeight() {
7    return mMeasuredHeight & MEASURED_SIZE_MASK;
8 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    获取原始的测量高度值，一般会拿这个方法和layout执行后getHeight()方法做比较。该方法需要在setMeasuredDimension()方法执行后才有效，否则返回值为0。

 

 4、performMeasure()方法中RootMeasureSpec参数来源分析

​    前面讲到getDefaultSize(int size，int measureSpec)方法时提到过，要找到其中measureSpec的来源。事实上，根据View体系的不断往下遍历和递归中，前面流程中传入getDefaultSize()方法中的值是根据上一次的值变动的，所以咱们需要找到最初参数值。根据代码往回看，可以看到前文performTraversals()源码部分第三行和第四行中，该参数的来源。咱们先看看传入performMeasure(int,int)的childWidthMeasureSpec是怎么来的。

```
int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
```

​    getRootMeasureSpec(int,int)方法的完整源码如下所示：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * Figures out the measure spec for the root view in a window based on it's
 3      * layout params.
 4      *
 5      * @param windowSize
 6      *            The available width or height of the window
 7      *
 8      * @param rootDimension
 9      *            The layout params for one dimension (width or height) of the
10      *            window.
11      *
12      * @return The measure spec to use to measure the root view.
13      */
14     private static int getRootMeasureSpec(int windowSize, int rootDimension) {
15         int measureSpec;
16         switch (rootDimension) {
17 
18         case ViewGroup.LayoutParams.MATCH_PARENT:
19             // Window can't resize. Force root view to be windowSize.
20             measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.EXACTLY);
21             break;
22         case ViewGroup.LayoutParams.WRAP_CONTENT:
23             // Window can resize. Set max size for root view.
24             measureSpec = MeasureSpec.makeMeasureSpec(windowSize, MeasureSpec.AT_MOST);
25             break;
26         default:
27             // Window wants to be an exact size. Force root view to be that size.
28             measureSpec = MeasureSpec.makeMeasureSpec(rootDimension, MeasureSpec.EXACTLY);
29             break;
30         }
31         return measureSpec;
32     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 照例先翻译一下注释

   1）基于window的layout params，在window中为root view 找出measure spec。（笔者注：也就是找出DecorView的MeasureSpec，这里的window也就是PhoneWindow了）

   2）参数windowSize：window的可用宽度和高度值。

   3）参数rootDimension：window的宽/高的layout param值。

   4）返回值：返回用于测量root view的MeasureSpec。  

​    如果不清楚LayoutParams类，可以看看本节开头的介绍。在getRootMeasureSpec(int,int)中，MeasureSpec.makeMeasureSpec方法在前面介绍MeasureSpec类的时候提到过，就是将size和mode组合成一个MeasureSpec值。这里我们可以看到ViewGroup.LayoutParam的width/height值和MeasureSpec的mode值存在如下的对应关系：

![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190522153353850-1628612895.png)

​    我们再继续看看windowSize和rootDimension的实际参数mWidth和lp.width的来历。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===========================ViewRootImpl.java=======================
 2 ......
 3 final Rect mWinFrame; // frame given by window manager.
 4 ......
 5 private void performTraversals() {
 6     ......
 7     Rect frame = mWinFrame;
 8     ......
 9     mWidth = frame.width();
10     ......
11 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    从源码中对mWinFrame的注释来看，是由WindowManager提供的，该矩形正好是整个屏幕（这里暂时还没有在源码中找到明确的证据，后续找到后再补上）。在文章【[Android图形系统（三）-View绘制流程](https://www.jianshu.com/p/58d22426e79e)】的“2.2 窗口布局阶段”中有提到，WindowManagerService服务计算Activity窗口的大小，并将Activity窗口的大小保存在成员变量mWinFrame中。对Activity窗口大小计算的详情，有兴趣的可以阅读一下大神罗升阳的博文【[Android窗口管理服务WindowManagerService计算Activity窗口大小的过程分析](https://blog.csdn.net/Luoshengyang/article/details/8479101)】。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //=================================ViewRootImpl.java================================
 2 ......
 3 final WindowManager.LayoutParams mWindowAttributes = new WindowManager.LayoutParams();
 4 ......
 5 public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
 6     ......
 7     mWindowAttributes.copyFrom(attrs);
 8     ......
 9 }
10 private void performTraversals() {
11      ......
12      WindowManager.LayoutParams lp = mWindowAttributes;
13      ......     
14 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    第5行setView方法，在上一节中讲过，其中的参数就是ActivityThread类中传过来的，attrs是PhoneWindow的LayoutParams值，在第三节中就专门讲过这个参数，其width和height属性值均为LayoutParams.MATCH_PARENT。结合getRootMeasureSpec(int windowSize, int rootDimension)方法，可以得出如下结果：

   ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190522163151435-1032765476.png)

​    此时，我们就得到了DecorView的MeasureSpec了，后面的递归操作就是在此基础上不断将测量要求从父布局传递到子view。

 

 5、ViewGroup中辅助重写onMeasure的几个重要方法介绍

​    前面我们介绍的很多方法都是View类中提供的，ViewGroup中也提供了一些方法用于辅助ViewGroup子类容器的测量。这里重点介绍三个方法：measureChild(...)、measureChildWithMargins(...)和measureChildWithMargins(...)方法。

  （1）measureChild()方法和measureChildWithMargins()方法

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //================ViewGroup.java===============
 2 /**
 3      * Ask one of the children of this view to measure itself, taking into
 4      * account both the MeasureSpec requirements for this view and its padding.
 5      * The heavy lifting is done in getChildMeasureSpec.
 6      *
 7      * @param child The child to measure
 8      * @param parentWidthMeasureSpec The width requirements for this view
 9      * @param parentHeightMeasureSpec The height requirements for this view
10      */
11     protected void measureChild(View child, int parentWidthMeasureSpec,
12             int parentHeightMeasureSpec) {
13         final LayoutParams lp = child.getLayoutParams();
14 
15         final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
16                 mPaddingLeft + mPaddingRight, lp.width);
17         final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
18                 mPaddingTop + mPaddingBottom, lp.height);
19 
20         child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
21     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //===================ViewGroup.java===================
 2 /**
 3      * Ask one of the children of this view to measure itself, taking into
 4      * account both the MeasureSpec requirements for this view and its padding
 5      * and margins. The child must have MarginLayoutParams The heavy lifting is
 6      * done in getChildMeasureSpec.
 7      *
 8      * @param child The child to measure
 9      * @param parentWidthMeasureSpec The width requirements for this view
10      * @param widthUsed Extra space that has been used up by the parent
11      *        horizontally (possibly by other children of the parent)
12      * @param parentHeightMeasureSpec The height requirements for this view
13      * @param heightUsed Extra space that has been used up by the parent
14      *        vertically (possibly by other children of the parent)
15      */
16     protected void measureChildWithMargins(View child,
17             int parentWidthMeasureSpec, int widthUsed,
18             int parentHeightMeasureSpec, int heightUsed) {
19         final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
20 
21         final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
22                 mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
23                         + widthUsed, lp.width);
24         final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
25                 mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
26                         + heightUsed, lp.height);
27 
28         child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
29     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    对比这两个方法可以发现，它们非常相似，从注释上来看，后者在前者的基础上增加了已经使用的宽高和margin值。其实它们的功能都是一样的，最后都是生成子View的MeasureSpec，并传递给子View继续测量，即最后一句代码child.measure(childWidthMeasureSpec, childHeightMeasureSpec)。一般根据容器自身的需要来选择其中一个，比如，在FrameLayout和LinearLayout中重写的onMeasure方法中调用的就是后者，而AbsoluteLayout中就是间接地调用的前者。而RelativeLayout中，两者都没有调用，而是自己写了一套方法，不过该方法和后者方法仅略有差别，但基本功能还是一样，读者可以自己去看看它们的源码，这里就不贴出来了。

  （2）getChildMeasureSpec()方法

​    前两个方法中都用到了这个方法，它很重要，它用于将父布局传递来的MeasureSpec和其子view的LayoutParams，整合为一个最有可能的子View的MeasureSpec。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //==================ViewGroup.java====================
 2  /**
 3      * Does the hard part of measureChildren: figuring out the MeasureSpec to
 4      * pass to a particular child. This method figures out the right MeasureSpec
 5      * for one dimension (height or width) of one child view.
 6      *
 7      * The goal is to combine information from our MeasureSpec with the
 8      * LayoutParams of the child to get the best possible results. For example,
 9      * if the this view knows its size (because its MeasureSpec has a mode of
10      * EXACTLY), and the child has indicated in its LayoutParams that it wants
11      * to be the same size as the parent, the parent should ask the child to
12      * layout given an exact size.
13      *
14      * @param spec The requirements for this view
15      * @param padding The padding of this view for the current dimension and
16      *        margins, if applicable
17      * @param childDimension How big the child wants to be in the current
18      *        dimension
19      * @return a MeasureSpec integer for the child
20      */
21     public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
22         int specMode = MeasureSpec.getMode(spec);
23         int specSize = MeasureSpec.getSize(spec);
24 
25         int size = Math.max(0, specSize - padding);
26 
27         int resultSize = 0;
28         int resultMode = 0;
29 
30         switch (specMode) {
31         // Parent has imposed an exact size on us
32         case MeasureSpec.EXACTLY:
33             if (childDimension >= 0) {
34                 resultSize = childDimension;
35                 resultMode = MeasureSpec.EXACTLY;
36             } else if (childDimension == LayoutParams.MATCH_PARENT) {
37                 // Child wants to be our size. So be it.
38                 resultSize = size;
39                 resultMode = MeasureSpec.EXACTLY;
40             } else if (childDimension == LayoutParams.WRAP_CONTENT) {
41                 // Child wants to determine its own size. It can't be
42                 // bigger than us.
43                 resultSize = size;
44                 resultMode = MeasureSpec.AT_MOST;
45             }
46             break;
47 
48         // Parent has imposed a maximum size on us
49         case MeasureSpec.AT_MOST:
50             if (childDimension >= 0) {
51                 // Child wants a specific size... so be it
52                 resultSize = childDimension;
53                 resultMode = MeasureSpec.EXACTLY;
54             } else if (childDimension == LayoutParams.MATCH_PARENT) {
55                 // Child wants to be our size, but our size is not fixed.
56                 // Constrain child to not be bigger than us.
57                 resultSize = size;
58                 resultMode = MeasureSpec.AT_MOST;
59             } else if (childDimension == LayoutParams.WRAP_CONTENT) {
60                 // Child wants to determine its own size. It can't be
61                 // bigger than us.
62                 resultSize = size;
63                 resultMode = MeasureSpec.AT_MOST;
64             }
65             break;
66 
67         // Parent asked to see how big we want to be
68         case MeasureSpec.UNSPECIFIED:
69             if (childDimension >= 0) {
70                 // Child wants a specific size... let him have it
71                 resultSize = childDimension;
72                 resultMode = MeasureSpec.EXACTLY;
73             } else if (childDimension == LayoutParams.MATCH_PARENT) {
74                 // Child wants to be our size... find out how big it should
75                 // be
76                 resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
77                 resultMode = MeasureSpec.UNSPECIFIED;
78             } else if (childDimension == LayoutParams.WRAP_CONTENT) {
79                 // Child wants to determine its own size.... find out how
80                 // big it should be
81                 resultSize = View.sUseZeroUnspecifiedMeasureSpec ? 0 : size;
82                 resultMode = MeasureSpec.UNSPECIFIED;
83             }
84             break;
85         }
86         //noinspection ResourceType
87         return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
88     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 咱们依然先翻译和整理一下开头的注释：

   1）处理measureChildren的困难部分：计算出Measure传递给指定的child。该方法计算出一个子view的宽或高的正确MeasureSpec。

   2）其目的是组合来自我们MeasureSpec的信息和child的LayoutParams来得到最有可能的结果。比如：如果该view知道它的尺寸（因为它的MeasureSpec的mode为EXACTLY），并且它的child在它的LayoutParams中表示它想和父布局有一样大，那么父布局应该要求该child按照精确的尺寸进行布局。

   3）参数spec：对该view的要求（笔者注：父布局对当前child的MeasureSpec要求）

   4）参数padding：该view宽/高的padding和margins值，如果可应用的话。

   5）参数childDimension：该child在宽/高上希望多大。

   6）返回：返回该child的MeasureSpec整数。

​    如果明白了前文中对MeasureSpec的介绍后，这一部分的代码应该就容易理解了，specMode的三种值，LayoutParams的width和height的三种值，以及和layout_width、layout_height之间的关对应关系，在文章的开头已经介绍过了，不明白的可以再回头复习一下。specMode和specSize分别是父布局传下来的要求，size的值是父布局尺寸要求减去其padding值，最小不会小于0。代码最后就是将重新得到的mode和size组合生成一个新的MeasureSpec，传递给子View，一直递归下去，该方法也在前面讲过。本段代码重难点就是这里新mode和新size值的确定，specMode和childDimension各有3种值，所以最后会有9种组合。如果对这段代码看不明白的，可以看看笔者对这段代码的解释（width和height同理，这里以width为例）：

- 如果specMode的值为MeasureSpec.EXACTLY，即父布局对子view的尺寸要求是一个精确值，这有两种情况，父布局中layout_width属性值被设置为具体值，或者match_parent，它们都被定义为精确值。针对childDimension的值

​     i）childDimension也为精确值时。它是LayoutParams中width属性，是一个具体值，不包括match_parent情况，这个一定要和MeasureSpec中的精确值EXACTLY区别开来。此时resultSize为childDimension的精确值，resultMode理所当然为MeasureSpec.EXACTLY。这里不知道读者会不会又疑问，如果子View的layout_width值比父布局的大，那这个结论还成立吗？按照我们的经验，似乎不太能理解，因为子view的宽度再怎么样也不会比父布局大。事实上，我们平时经验看到的，是最后布局后绘制出来的结果，而当前步骤为测量值，是有差别的。读者可以自定义一个View，将父布局layout_width设置为100px，该自定义的子view则设置为200px，然后在子view中重写的onMeasure方法中打印出getMeasuredWidth()值看看，其值一定是200。甚至如果子view设置的值超过屏幕尺寸，其打印值也是设置的值。

​    ii）childDimension值为LayoutParams.MATCH_PARENT时。这个容易理解，它的尺寸和父布局一样，也是个精确值，所以resultSize为前面求出的size值，由父布局决定，resultMode为MeasureSpec.EXACTLY。

​    iii）childDimension值为LayoutParams.WRAP_CONTENT时。当子view的layout_width被设置为wrap_content时，即使最后我们肉眼看到屏幕上真正显示出来的控件很小，但在测量时和父布局一样的大小。这一点仍然可以通过打印getMeasuredWidth值来理解。所以一定不要被“经验”所误。所以resultSize值为size大小，resultMode为MeasureSpec.AT_MOST。

- 如果specMode值为MeasureSpec.AT_MOST。其对应于layout_width为wrap_content，此时，我们可以想象到，子View对结果的决定性很大。

​    i）childDimension为精确值时。很容易明确specSize为自身的精确值，specMode为MeasureSpec.EXACTLY。

​    ii）childDimension为LayoutParams.MATCH_PARENT时。specSize由父布局决定，为size；specMode为MeasureSpec.AT_MOST。

​    iii）childDimension为LayoutParams.WRAP_CONTENT时。specSize由父布局决定，为size；specMode为MeasureSpec.AT_MOST。

- 如果specMode值为MeasureSpec.UNSPECIFIED。前面说过，平时很少用，一般用在系统中，不过这里还是简单说明一下。这一段有个变量View.sUseZeroUnspecifiedMeasureSpec，它是用于表示当前的目标api是否低于23（对应系统版本为Android M）的，低于23则为true，否则为false。现在系统版本基本上都是Android M及以上的，所以这里该值我们当成false来处理。

​    i）childDimension为精确值时。很容易明确specSize为自身的精确值，specMode为MeasureSpec.EXACTLY。

​    ii）childDimension为LayoutParams.MATCH_PARENT时。specSize由父布局决定，为size；specMode和父布局一样，为MeasureSpec.UNSPECIFIED。

​    iii）childDimension为LayoutParams.WRAP_CONTENT时。specSize由父布局决定，为size；specMode和父布局一样，为MeasureSpec.UNSPECIFIED。

​    这个方法对理解测量时MeasureSpec的传递过程非常重要，并且需要记忆和理解的内容也不少，所以这里花的篇幅比较多。

 

​    通过这一节，我们介绍了ViewGroup在测量过程中要用到的方法。通过这些方法，我们更加深入理解了测量过程中ViewGroup是如何测量子View的了。

 

 6、DecorView测量的大致流程

​    前面我们提到过DecorView的继承链：DecorView extends FrameLayout extends ViewGroup extends View。所以在这个继承过程中一定会有子类重写onMeasure方法，当DecorView第一次调用到measure()方法后，流程就开始切换到重写的onMeasure()中了。我们按照这个继承顺序看看measure流程的相关源码：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //=============DecorView.java=============
 2 @Override
 3 protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 4        ......
 5     super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 6        ......
 7 }
 8 
 9 //=============FrameLayout.java=============
10 @Override
11 protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
12   int count = getChildCount();
13   for (int i = 0; i < count; i++) {
14        final View child = getChildAt(i);
15        ......
16        measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
17        ......             
18    }
19    ......
20    setMeasuredDimension(......)
21    ...... }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    第16行中measureChildWithMargins()方法是ViewGroup提供的方法，前面我们介绍过了。从上述FrameLayout中重写的onMeasure方法中可以看到，是先把子view测量完成后，最后才去调用setMeasuredDimension(...)来测量自己的。事实上，整个测量过程就是从子view开始测量，然后一层层往上再测量父布局，直到DecorView为止的。

​    可能到这里有些读者会有个疑问，DecorView中onMeasure方法的参数值是从哪里传过来的呢？呵呵，前面花了很大的篇幅，就在不断地讲它俩，这里再强调啰嗦一次：

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //=====================ViewRootImpl.java=================
2 private void performTraversals() {
3    ......
4    int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);
5    int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);      
6    ......
7 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 如果还是不明白，回过头去再看看这部分的说明吧，这里就不再赘述了。

 

 7、DecorView视图树的简易measure流程图

​    到目前为止，DecorView的整个测量流程就接上了，从ViewRootImpl类的performTraversals()开始，经过递归遍历，最后到叶子view测量结束，DecorView视图树的测量就完成了。这里再用一个流程图简单描述一下整个流程：

 ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190527134518936-2000774802.png)

 

​    在这一节的最后，推荐一篇博文，这里面有个非常详细的案例分析，如何一步一步从DecorView开始遍历，到整个View树测量完成，以及如何测量出每个view的宽高值：【[Android View的绘制流程：https://www.jianshu.com/p/5a71014e7b1b?from=singlemessage](https://www.jianshu.com/p/5a71014e7b1b?from=singlemessage)】Measure过程的第4点。认真分析完该实例，一定会对测量过程有个更深刻的认识。

 

**六、layout过程分析**

​    当measure过程完成后，接下来就会进行layout阶段，即布局阶段。在前面measure的作用是测量每个view的尺寸，而layout的作用是根据前面测量的尺寸以及设置的其它属性值，共同来确定View的位置。

 1、performLayout方法引出DecorView的布局流程

​    测量完成后，会在ViewRootImpl类的performTraverserals()方法中，开始调用performLayout方法：

```
performLayout(lp, mWidth, mHeight);
```

​    传入该方法的参数我们在上一节中已经分析过了，lp中width和height均为LayoutParams.MATCH_PARENT，mWidth和mHeight分别为屏幕的宽高。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //=====================ViewRootImpl.java===================
2 private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
3             int desiredWindowHeight) {
4    ......
5    final View host = mView;
6    ......
7    host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
8    ......
9 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    mView的值上一节也讲过，就是DecorView，布局流程也是从DecorView开始遍历和递归。

 

 2、layout方法正式启动布局流程

​    由于DecorView是一个容器，是ViewGroup子类，所以跟踪代码的时候，实际上是先进入到ViewGroup类中的layout方法中。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //==================ViewGroup.java================
 2     @Override
 3     public final void layout(int l, int t, int r, int b) {
 4         if (!mSuppressLayout && (mTransition == null || !mTransition.isChangingLayout())) {
 5             if (mTransition != null) {
 6                 mTransition.layoutChange(this);
 7             }
 8             super.layout(l, t, r, b);
 9         } else {
10             // record the fact that we noop'd it; request layout when transition finishes
11             mLayoutCalledWhileSuppressed = true;
12         }
13     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    这是一个final类型的方法，所以自定义 的ViewGroup子类无法重写该方法，可见系统不希望自定义的ViewGroup子类破坏layout流程。继续追踪super.layout方法，又跳转到了View中的layout方法。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //=================View.java================
 2  /**
 3      * Assign a size and position to a view and all of its
 4      * descendants
 5      *
 6      * <p>This is the second phase of the layout mechanism.
 7      * (The first is measuring). In this phase, each parent calls
 8      * layout on all of its children to position them.
 9      * This is typically done using the child measurements
10      * that were stored in the measure pass().</p>
11      *
12      * <p>Derived classes should not override this method.
13      * Derived classes with children should override
14      * onLayout. In that method, they should
15      * call layout on each of their children.</p>
16      *
17      * @param l Left position, relative to parent
18      * @param t Top position, relative to parent
19      * @param r Right position, relative to parent
20      * @param b Bottom position, relative to parent
21      */
22     @SuppressWarnings({"unchecked"})
23     public void layout(int l, int t, int r, int b) {
24         ......
25         boolean changed = isLayoutModeOptical(mParent) ?
26                 setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);  
27         if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
28             onLayout(changed, l, t, r, b);
29             ......
30          }
31          ......
32 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 先翻译一下注释中对该方法的描述：

   1）给view和它的所有后代分配尺寸和位置。

   2）这是布局机制的第二个阶段（第一个阶段是测量）。在这一阶段中，每一个父布局都会对它的子view进行布局来放置它们。一般来说，该过程会使用在测量阶段存储的child测量值。

   3）派生类不应该重写该方法。有子view的派生类（笔者注：也就是容器类，父布局）应该重写onLayout方法。在重写的onLayout方法中，它们应该为每一子view调用layout方法进行布局。

   4）参数依次为：Left、Top、Right、Bottom四个点相对父布局的位置。

 

 3、setFrame方法真正执行布局任务

​    在上面的方法体中，我们先重点看看setFrame方法。至于setOpticalFrame方法，其中也是调用的setFrame方法。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //=================View.java================
 2 /**
 3      * Assign a size and position to this view.
 4      *
 5      * This is called from layout.
 6      *
 7      * @param left Left position, relative to parent
 8      * @param top Top position, relative to parent
 9      * @param right Right position, relative to parent
10      * @param bottom Bottom position, relative to parent
11      * @return true if the new size and position are different than the
12      *         previous ones
13      * {@hide}
14      */
15     protected boolean setFrame(int left, int top, int right, int bottom) {
16         boolean changed = false;
17         ......
18         if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
19             changed = true;
20             ......
21             int oldWidth = mRight - mLeft;
22             int oldHeight = mBottom - mTop;
23             int newWidth = right - left;
24             int newHeight = bottom - top;
25             boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);
26 
27             // Invalidate our old position
28             invalidate(sizeChanged);
29 
30             mLeft = left;
31             mTop = top;
32             mRight = right;
33             mBottom = bottom;
34             ......
35         }
36         return changed;
37  }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 注释中重要的信息有：

   1）该方法用于给该view分配尺寸和位置。（笔者注：也就是实际的布局工作是在这里完成的）

   2）返回值：如果新的尺寸和位置和之前的不同，返回true。（笔者注：也就是该view的位置或大小发生了变化）

​    在方法体中，从第27行开始，对view的四个属性值进行了赋值，即mLeft、mTop、mRight、mBottom四条边界坐标被确定，表明这里完成了对该View的布局。

 

 4、onLayout方法让父布局调用对子view的布局

   再返回到layout方法中，会看到如果view发生了改变，接下来会调用onLayout方法，这和measure调用onMeasure方法类似。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //============View.java============
 2 /**
 3      * Called from layout when this view should
 4      * assign a size and position to each of its children.
 5      *
 6      * Derived classes with children should override
 7      * this method and call layout on each of
 8      * their children.
 9      * @param changed This is a new size or position for this view
10      * @param left Left position, relative to parent
11      * @param top Top position, relative to parent
12      * @param right Right position, relative to parent
13      * @param bottom Bottom position, relative to parent
14      */
15     protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
16     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 先翻译一下关键注释：

   1）当该view要分配尺寸和位置给它的每一个子view时，该方法会从layout方法中被调用。

   2）有子view的派生类（笔者注：也就是容器，父布局）应该重写该方法并且为每一个子view调用layout。

​    我们发现这是一个空方法，因为layout过程是父布局容器布局子view的过程，onLayout方法叶子view没有意义，只有ViewGroup才有用。所以，如果当前View是一个容器，那么流程会切到被重写的onLayout方法中。我们先看ViewGroup类中的重写：

```
1 //=============ViewGroup.java===========
2   @Override
3    protected abstract void onLayout(boolean changed,
4            int l, int t, int r, int b);
```

​    进入到ViewGroup类中发现，该方法被定义为了abstract方法，所以以后凡是直接继承自ViewGroup类的容器，就必须要重写onLayout方法。 事实上，layout流程是绘制流程中必需的过程，而前面讲过的measure流程，其实可以不要，这一点等会再说。

​    咱们先直接进入到DecorView中查看重写的onLayout方法。

```
1 //==============DecorView.java================
2  @Override
3  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
4      super.onLayout(changed, left, top, right, bottom);
5      ......
6 }
```

​    DecerView继承自FrameLayout，咱们继续到FrameLayout类中重写的onLayout方法看看。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //================FrameLayout.java==============
 2     @Override
 3     protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
 4         layoutChildren(left, top, right, bottom, false /* no force left gravity */);
 5     }
 6 
 7     void layoutChildren(int left, int top, int right, int bottom, boolean forceLeftGravity) {
 8         final int count = getChildCount();
 9         ......
10         for (int i = 0; i < count; i++) {
11              final View child = getChildAt(i);
12              if (child.getVisibility() != GONE) {
13                  final LayoutParams lp = (LayoutParams) child.getLayoutParams();
14 
15                  final int width = child.getMeasuredWidth();
16                  final int height = child.getMeasuredHeight();
17                  ......
18                  child.layout(childLeft, childTop, childLeft + width, childTop + height);
19             }
20     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    这里仅贴出关键流程的代码，咱们可以看到，这里面也是对每一个child调用layout方法的。如果该child仍然是父布局，会继续递归下去；如果是叶子view，则会走到view的onLayout空方法，该叶子view布局流程走完。另外，我们看到第15行和第16行中，width和height分别来源于measure阶段存储的测量值，如果这里通过其它渠道赋给width和height值，那么measure阶段就不需要了，这也就是我前面提到的，onLayout是必需要实现的（不仅会报错，更重要的是不对子view布局的话，这些view就不会显示了），而measure过程可以不要。当然，肯定是不建议这么做的，采用其它方式很实现我们要的结果。

 

 5、DecorView视图树的简易布局流程图

​    如果是前面搞清楚了DecorView视图树的测量流程，那这一节的布局流程也就非常好理解了，咱们这里再简单梳理一下：

 ![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190527140844001-1305183349.png)

 

**七、draw过程分析**

​    当layout完成后，就进入到draw阶段了，在这个阶段，会根据layout中确定的各个view的位置将它们画出来。该过程的分析思路和前两个过程类似，如果前面读懂了，那这个流程也就很容易理解了。

 1、从performDraw方法到draw方法

​    draw过程，自然也是从performTraversals()中的performDraw()方法开始的，咱们从该方法追踪，咱们这里仅贴出关键流程代码，至于其它的逻辑，不是本文的重点，这里就先略过，有兴趣的可以自行研究。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //==================ViewRootImpl.java=================
 2 private void performDraw() {
 3       ......
 4       boolean canUseAsync = draw(fullRedrawNeeded);
 5       ......
 6 }
 7 
 8 private boolean draw(boolean fullRedrawNeeded) {
 9       ......
10       if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset,
11                         scalingRequired, dirty, surfaceInsets)) {
12                     return false;
13                 }
14       ......
15 }
16 
17 private boolean drawSoftware(......){
18       ......
19       mView.draw(canvas);
20       ......
21 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    前面我们讲过了，这mView就是DecorView，这样就开始了DecorView视图树的draw流程了。

 2、DecorView树递归完成“画”流程

​    DecorView类中重写了draw()方法，追踪源码后进入到该部分。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //================DecorView.java==============
2 @Override
3 public void draw(Canvas canvas) {
4      super.draw(canvas);
5 
6      if (mMenuBackground != null) {
7          mMenuBackground.draw(canvas);
8      }
9 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    从这段代码来看， 调用完super.draw后，还画了菜单背景，当然super.draw是咱们关注的重点，这里还做了啥咱们不用太关心。由于FrameLayout和ViewGroup都没有重写该方法，所以就直接进入都了View类中的draw方法了。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //====================View.java===================== 
 2  /**
 3      * Manually render this view (and all of its children) to the given Canvas.
 4      * The view must have already done a full layout before this function is
 5      * called.  When implementing a view, implement
 6      * {@link #onDraw(android.graphics.Canvas)} instead of overriding this method.
 7      * If you do need to override this method, call the superclass version.
 8      *
 9      * @param canvas The Canvas to which the View is rendered.
10      */
11     @CallSuper
12     public void draw(Canvas canvas) {
13        ......
14         /*
15          * Draw traversal performs several drawing steps which must be executed
16          * in the appropriate order:
17          *
18          *      1. Draw the background
19          *      2. If necessary, save the canvas' layers to prepare for fading
20          *      3. Draw view's content
21          *      4. Draw children
22          *      5. If necessary, draw the fading edges and restore layers
23          *      6. Draw decorations (scrollbars for instance)
24          */
25 
26         // Step 1, draw the background, if needed
27         int saveCount;
28 
29         if (!dirtyOpaque) {
30             drawBackground(canvas);
31         }
32 
33         // skip step 2 & 5 if possible (common case)
34         ......
35         // Step 3, draw the content
36         if (!dirtyOpaque) onDraw(canvas);
37 
38         // Step 4, draw the children
39         dispatchDraw(canvas);
40         ......
41         // Step 6, draw decorations (foreground, scrollbars)
42         onDrawForeground(canvas);45         ......
43     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

   这段代码描述了draw阶段完成的7个主要步骤，这里咱们先翻译一下其注释：

   1）手动渲染该view（以及它的所有子view）到给定的画布上。

   2）在该方法调用之前，该view必须已经完成了全面的布局。当正在实现一个view是，实现onDraw(android.graphics.Cavas)而不是本方法。如果您确实需要重写该方法，调用超类版本。

   3）参数canvas：将view渲染到的画布。

   从代码上看，这里做了很多工作，咱们简单说明一下，有助于理解这个“画”工作。

   1）第一步：画背景。对应我我们在xml布局文件中设置的“android:background”属性，这是整个“画”过程的第一步，这一步是不重点，知道这里干了什么就行。

   2）第二步：画内容（第2步和第5步只有有需要的时候才用到，这里就跳过）。比如TextView的文字等，这是重点，onDraw方法，后面详细介绍。

   3）第三步：画子view。dispatchDraw方法用于帮助ViewGroup来递归画它的子view。这也是重点，后面也要详细讲到。

   4）第四步：画装饰。这里指画滚动条和前景。其实平时的每一个view都有滚动条，只是没有显示而已。同样这也不是重点，知道做了这些事就行。

​    咱们进入onDraw方法看看

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //=================View.java===============
2 /**
3      * Implement this to do your drawing.
4      *
5      * @param canvas the canvas on which the background will be drawn
6      */
7     protected void onDraw(Canvas canvas) {
8     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

 注释中说：实现该方法来做“画”工作。也就是说，具体的view需要重写该方法，来画自己想展示的东西，如文字，线条等。DecorView中重写了该方法，所以流程会走到DecorView中重写的onDraw方法。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 //===============DocerView.java==============
2 @Override
3     public void onDraw(Canvas c) {
4         super.onDraw(c);
5       mBackgroundFallback.draw(this, mContentRoot, c, mWindow.mContentParent,
6         mStatusColorViewState.view, mNavigationColorViewState.view);
7  }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

   这里调用了onDraw的父类方法，同时第4行还画了自己特定的东西。由于FrameLayout和ViewGroup也没有重写该方法，且View中onDraw为空方法，所以super.onDraw方法其实是啥都没干的。DocerView画完自己的东西，紧接着流程就又走到dispatchDraw方法了。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //================View.java===============
 2 /**
 3      * Called by draw to draw the child views. This may be overridden
 4      * by derived classes to gain control just before its children are drawn
 5      * (but after its own view has been drawn).
 6      * @param canvas the canvas on which to draw the view
 7      */
 8     protected void dispatchDraw(Canvas canvas) {
 9 
10     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    先看看注释：被draw方法调用来画子View。该方法可能会被派生类重写来获取控制，这个过程正好在该view的子view被画之前（但在它自己被画完成后）。

​    也就是说当本view被画完之后，就开始要画它的子view了。这个方法也是一个空方法，实际上对于叶子view来说，该方法没有什么意义，因为它没有子view需要画了，而对于ViewGroup来说，就需要重写该方法来画它的子view。

​    在源码中发现，像平时常用的LinearLayout、FrameLayout、RelativeLayout等常用的布局控件，都没有再重写该方法，DecorView中也一样，而是只在ViewGroup中实现了dispatchDraw方法的重写。所以当DecorView执行完onDraw方法后，流程就会切到ViewGroup中的dispatchDraw方法了。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //=============ViewGroup.java============
 2  @Override
 3  protected void dispatchDraw(Canvas canvas) {
 4         final int childrenCount = mChildrenCount;
 5         final View[] children = mChildren;
 6         ......
 7         for (int i = 0; i < childrenCount; i++) {
 8             more |= drawChild(canvas, child, drawingTime);
 9             ......
10         }
11         ...... 
12  }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    从上述源码片段可以发现，这里其实就是对每一个child执行drawChild操作。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 /**
 2      * Draw one child of this View Group. This method is responsible for getting
 3      * the canvas in the right state. This includes clipping, translating so
 4      * that the child's scrolled origin is at 0, 0, and applying any animation
 5      * transformations.
 6      *
 7      * @param canvas The canvas on which to draw the child
 8      * @param child Who to draw
 9      * @param drawingTime The time at which draw is occurring
10      * @return True if an invalidate() was issued
11      */
12     protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
13         return child.draw(canvas, this, drawingTime);
14     }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

先翻译注释的内容：

   1）画当前ViewGroup中的某一个子view。该方法负责在正确的状态下获取画布。这包括了裁剪，移动，以便子view的滚动原点为0、0，以及提供任何动画转换。

   2）参数drawingTime：“画”动作发生的时间点。

​    继续追踪源码，进入到如下流程。

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 //============View.java===========
 2 /**
 3      * This method is called by ViewGroup.drawChild() to have each child view draw itself.
 4      *
 5      * This is where the View specializes rendering behavior based on layer type,
 6      * and hardware acceleration.
 7      */
 8     boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
 9       ......
10       draw(canvas);
11       ......
12 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

​    注释中说：该方法被ViewGroup.drawChild()方法调用，来让每一个子view画它自己。

​    该方法中，又回到了draw(canvas)方法中了，然后再开始画其子view，这样不断递归下去，直到画完整棵DecorView树。

 

 3、DecorView视图树的简易draw流程图

​    针对上述的代码追踪流程，这里梳理了DecorView整个view树的draw过程的关键流程，其中节点比较多，需要耐心分析。

![img](art/AndroidView%20%E7%BB%98%E5%88%B6%E6%B5%81%E7%A8%8B.assets/472002-20190527183905535-1898063916.png)

   

​    到目前为止，View的绘制流程就介绍完了。根节点是DecorView，整个View体系就是一棵以DecorView为根的View树，依次通过遍历来完成measure、layout和draw过程。而如果要自定义view，一般都是通过重写onMeasure()，onLayout()，onDraw()来完成要自定义的部分，整个绘制流程也基本上是围绕着这几个核心的地方来展开的。

## Activity中View如何添加xml到窗口

我们使用AndroidStudio新建一个项目，无须添加任何代码，就打开MainActivity，首先我们想到既然setContentView是给Activity设置contentview的，那你对这个activity的整个页面的View结构有什么概念呢？

我们点击AndroidStudio的顶部菜单栏的Tools菜单，依次点击里面的Android》Layout Inspector；这样就会弹出一个展示Activity窗口视图结构的窗口出来，如图


当前activity的布局里只添加了一个textview，并且当前Activity在setContentView之前没有进行Features设置。

当用鼠标放在左边具体view上面，右边视图上会展示相应的范围

从这里可以看出来Activity最根部的view是DecorView，后面我们会说到DecorView其实是一个FrameLayout
DecorView包含两部分，一个装载内容的LinearLayout和一个顶部View
LinearLayout包含一个ViewStub和FrameLayout，其实就是一个action_bar，只不过使用ViewStub来修饰，因为开发者是可以设置不要标题栏的；至于FrameLayout就是真正装载我们通过setContentView设置进去的View
最后这个TextView就是我们自己布局的内容
结构了解之后再回来通过setContentView去探索它是怎么形成的：

Activity setContentView
通过在onCreate中点击setContentView方法进入到源码中查看

public void setContentView(@LayoutRes int layoutResID) {
        getWindow().setContentView(layoutResID);
        initWindowDecorActionBar();

这里先调用getWindow()方法

public Window getWindow() {
        return mWindow;
}

这个mWindow是activity类的全局变量 private Window mWindow;它是在attach方法中实例化的，并且实现了Callback接口，这个后续会用到

mWindow = new PhoneWindow(this, window);
mWindow.setCallback(this);
1
2
可以看到其实是实例化了一个PhoneWindow对象，Window是个什么东西呢，可以这样说，它是顶级窗口外观和行为策略的抽象基类。 应该将此类的实例用作添加到窗口管理器的顶级视图。 它提供标准的UI策略，例如背景，标题区域，默认密钥处理等。此抽象类的唯一现有实现是android.view.PhoneWindow。

PhoneWindow setContentView
回到setContentView方法，获取窗口实例后，调用PhoneWindow的setContentView方法：

@Override
    public void setContentView(int layoutResID) {
        // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
        // decor, when theme attributes and the like are crystalized. Do not check the feature
        // before this happens.
        if (mContentParent == null) {
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
              cb.onContentChanged();
        }
        mContentParentExplicitlySet = true;
    }

我们第一次进来的时候，这个mContentParent（这个就是装载我们设置的布局view的容器）肯定是null，那么就进入installDecor方法，这个方法很长，就不全部贴上来了，贴关键部分

if (mDecor == null) {
            //通过new 创建Decorview
            mDecor = generateDecor(-1);
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        } else {
            mDecor.setWindow(this);
        }
        if (mContentParent == null) {
            //根据mDecor实例获取mContentParent
            mContentParent = generateLayout(mDecor);

第一步：如果DecorView为null，就通过generateDecor来构造一个实例，看看DecorView这个类

public class DecorView extends FrameLayout

说明这就是布局控件，也表明Activity的根布局就是一个FrameLayout

第二步：根布局有了，就需要往里面塞东西了，这时候就判断如果mContentParent 为null，就通过generateLayout来构建，我们看看这个方法，这个方法很长，我把一些类似的代码删了

protected ViewGroup generateLayout(DecorView decor) {
        // Apply data from current theme.

        //根据Manifest里设置的theme来进行相应设置
        TypedArray a = getWindowStyle();
    
        if (false) {
            System.out.println("From style:");
            String s = "Attrs:";
            for (int i = 0; i < R.styleable.Window.length; i++) {
                s = s + " " + Integer.toHexString(R.styleable.Window[i]) + "="
                        + a.getString(i);
            }
            System.out.println(s);
        }
    
        mIsFloating = a.getBoolean(R.styleable.Window_windowIsFloating, false);
        int flagsToUpdate = (FLAG_LAYOUT_IN_SCREEN|FLAG_LAYOUT_INSET_DECOR)
                & (~getForcedWindowFlags());
        if (mIsFloating) {
            setLayout(WRAP_CONTENT, WRAP_CONTENT);
            setFlags(0, flagsToUpdate);
        } else {
            setFlags(FLAG_LAYOUT_IN_SCREEN|FLAG_LAYOUT_INSET_DECOR, flagsToUpdate);
        }
    
        //比如主题里设置了notitle，这里就会进行设置
        if (a.getBoolean(R.styleable.Window_windowNoTitle, false)) {
            requestFeature(FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.Window_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestFeature(FEATURE_ACTION_BAR);
        }
    	.
    	.
    	.
    	.
        // Inflate the window decor.
        //添加布局到DecorView，前面说到，DecorView是继承与FrameLayout，它本身也是一个ViewGroup，
        // 而我们前面创建它的时候，只是调用了new DecorView，此时里面并无什么东西。
        // 而下面的步骤则是根据用户设置的Feature来创建相应的默认布局主题。举个例子，
        // 如果我在setContentView之前调用了requestWindowFeature(Window.FEATURE_NO_TITLE)，
        // 这里则会通过getLocalFeatures来获取你设置的feature，进而选择加载对应的布局，
        // 此时MainActivity没有进行任何设置，对应的就是R.layout.screen_simple
        int layoutResource;
        int features = getLocalFeatures();
        // System.out.println("Features: 0x" + Integer.toHexString(features));
        if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
            layoutResource = R.layout.screen_swipe_dismiss;
        } else if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogTitleIconsDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_title_icons;
            }
            // XXX Remove this once action bar supports these features.
            removeFeature(FEATURE_ACTION_BAR);
            // System.out.println("Title Icons!");
        } else if ((features & ((1 << FEATURE_PROGRESS) | (1 << FEATURE_INDETERMINATE_PROGRESS))) != 0
                && (features & (1 << FEATURE_ACTION_BAR)) == 0) {
            // Special case for a window with only a progress bar (and title).
            // XXX Need to have a no-title version of embedded windows.
            layoutResource = R.layout.screen_progress;
            // System.out.println("Progress!");
        } else if ((features & (1 << FEATURE_CUSTOM_TITLE)) != 0) {
            // Special case for a window with a custom title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogCustomTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_custom_title;
            }
            // XXX Remove this once action bar supports these features.
            removeFeature(FEATURE_ACTION_BAR);
        } else if ((features & (1 << FEATURE_NO_TITLE)) == 0) {
            // If no other features and not embedded, only need a title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else if ((features & (1 << FEATURE_ACTION_BAR)) != 0) {
                layoutResource = a.getResourceId(
                        R.styleable.Window_windowActionBarFullscreenDecorLayout,
                        R.layout.screen_action_bar);
            } else {
                layoutResource = R.layout.screen_title;
            }
            // System.out.println("Title!");
        } else if ((features & (1 << FEATURE_ACTION_MODE_OVERLAY)) != 0) {
            layoutResource = R.layout.screen_simple_overlay_action_mode;
        } else {
            // Embedded, so no decoration is needed.
            layoutResource = R.layout.screen_simple;
            // System.out.println("Simple!");
        }
    
        mDecor.startChanging();
        //将上述确定的布局添加到mDecor中 mDecor其实是一个FrameLayout
        mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);
        //contentParent是mDecor布局中的一个子view FrameLayout id是content
        ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
        if (contentParent == null) {
            throw new RuntimeException("Window couldn't find content container view");
        }
    
        if ((features & (1 << FEATURE_INDETERMINATE_PROGRESS)) != 0) {
            ProgressBar progress = getCircularProgressBar(false);
            if (progress != null) {
                progress.setIndeterminate(true);
            }
        }
    
        if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
            registerSwipeCallbacks();
        }
    
        // Remaining setup -- of background and title -- that only applies
        // to top-level windows.
        if (getContainer() == null) {
            final Drawable background;
            if (mBackgroundResource != 0) {
                background = getContext().getDrawable(mBackgroundResource);
            } else {
                background = mBackgroundDrawable;
            }
            mDecor.setWindowBackground(background);
    
            final Drawable frame;
            if (mFrameResource != 0) {
                frame = getContext().getDrawable(mFrameResource);
            } else {
                frame = null;
            }
            mDecor.setWindowFrame(frame);
    
            mDecor.setElevation(mElevation);
            mDecor.setClipToOutline(mClipToOutline);
    
            if (mTitle != null) {
                setTitle(mTitle);
            }
    
            if (mTitleColor == 0) {
                mTitleColor = mTextColor;
            }
            setTitleColor(mTitleColor);
        }
    
        mDecor.finishChanging();
    
        return contentParent;
    }

方法很长，我们从头看，

首先调用getWindowStyle()方法，这个就是获取我们在AndroidManifest里对activity设置的Theme，然后做相应的设置；比如主题里设置了windowNoTitle，那这里就会设置不要标题栏
接下来会调用getLocalFeatures()方法，我们有时候会在activity的setContentView方法前设置requestWindowFeature(Window.FEATURE_NO_TITLE)类似的属性，这个方法就是获取这些设置的并做相应的处理；这也说明了为什么我们要在setContentView之前调用这些设置方法了
我上面的例子没有做相关设置，if-else语句最终会走到最后一个layoutResource = R.layout.screen_simple;这个布局文件就会通过下面的 mDecor.onResourcesLoaded(mLayoutInflater, layoutResource)方法被添加到DecorView中，看看这个布局代码
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    android:orientation="vertical">
    <ViewStub android:id="@+id/action_mode_bar_stub"
              android:inflatedId="@+id/action_mode_bar"
              android:layout="@layout/action_mode_bar"
              android:layout_width="match_parent"
              android:layout_height="wrap_content"
              android:theme="?attr/actionBarTheme" />
    <FrameLayout
         android:id="@android:id/content"
         android:layout_width="match_parent"
         android:layout_height="match_parent"
         android:foregroundInsidePadding="false"
         android:foregroundGravity="fill_horizontal|top"
         android:foreground="?android:attr/windowContentOverlay" />
</LinearLayout>

这个布局是不是跟我最上面贴的activity的视图结构一样的，这样之前构建的DecorView这个ViewGroup里就有内容了
4. 添加完之后就要获取这个布局里面的view拿来用了，然后就有这句

ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
public static final int ID_ANDROID_CONTENT = com.android.internal.R.id.content;
1
2
这样就拿到了布局里的FrameLayout并赋值给contentParent，最后把这个contentParent返回

第三步：mContentParent 这样就有值了，installDecor就基本上结束了

最后再回到setContentView方法

通过installDecor方法构建了一个DecorView，这个是作为Activity的根部View，并且添加了一个布局到DecorView里；通过id找到布局里的FrameLayout赋值给mContentParent ，这个ViewGroup就是用来给我们放置要设置的布局的

接下来会执行到mLayoutInflater.inflate(layoutResID, mContentParent);第一个参数就是我们传入的布局id，第二个参数就是上面构建的FrameLayout。

这个inflate方法最终会走到下面

public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        if (DEBUG) {
            Log.d(TAG, "INFLATING from resource: \"" + res.getResourceName(resource) + "\" ("
                    + Integer.toHexString(resource) + ")");
        }

        final XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }
public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
        synchronized (mConstructorArgs) {
	        //记录解析日志
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "inflate");

            final Context inflaterContext = mContext;
            //通过parser得到layout中的所有view的属性集保存在attrs中
            final AttributeSet attrs = Xml.asAttributeSet(parser);
            Context lastContext = (Context) mConstructorArgs[0];
            mConstructorArgs[0] = inflaterContext;
            View result = root;
    
            try {
                // Look for the root node.
                int type;
                while ((type = parser.next()) != XmlPullParser.START_TAG &&
                        type != XmlPullParser.END_DOCUMENT) {
                    // Empty
                }
    
                if (type != XmlPullParser.START_TAG) {
                    throw new InflateException(parser.getPositionDescription()
                            + ": No start tag found!");
                }
    			//得到layout的节点，例如，view、merge、include等
                final String name = parser.getName();
                
                if (DEBUG) {
                    System.out.println("**************************");
                    System.out.println("Creating root view: "
                            + name);
                    System.out.println("**************************");
                }
    
                if (TAG_MERGE.equals(name)) {
                    if (root == null || !attachToRoot) {
                        throw new InflateException("<merge /> can be used only with a valid "
                                + "ViewGroup root and attachToRoot=true");
                    }
    
                    rInflate(parser, root, inflaterContext, attrs, false);
                } else {
                    // Temp is the root view that was found in the xml
                    final View temp = createViewFromTag(root, name, inflaterContext, attrs);
    
                    ViewGroup.LayoutParams params = null;
    
                    if (root != null) {
                        if (DEBUG) {
                            System.out.println("Creating params from root: " +
                                    root);
                        }
                        // Create layout params that match root, if supplied
                        params = root.generateLayoutParams(attrs);
                        if (!attachToRoot) {
                            // Set the layout params for temp if we are not
                            // attaching. (If we are, we use addView, below)
                            temp.setLayoutParams(params);
                        }
                    }
    
                    if (DEBUG) {
                        System.out.println("-----> start inflating children");
                    }
    
                    // Inflate all children under temp against its context.
                    rInflateChildren(parser, temp, attrs, true);
    
                    if (DEBUG) {
                        System.out.println("-----> done inflating children");
                    }
    
                    // We are supposed to attach all the views we found (int temp)
                    // to root. Do that now.
                    if (root != null && attachToRoot) {
                        root.addView(temp, params);
                    }
    
                    // Decide whether to return the root that was passed in or the
                    // top view found in xml.
                    if (root == null || !attachToRoot) {
                        result = temp;
                    }
                }
    
            } catch (XmlPullParserException e) {
                final InflateException ie = new InflateException(e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } catch (Exception e) {
                final InflateException ie = new InflateException(parser.getPositionDescription()
                        + ": " + e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } finally {
                // Don't retain static reference on context.
                mConstructorArgs[0] = lastContext;
                mConstructorArgs[1] = null;
    
                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
            }
    
            return result;
        }
    }

这方法逻辑就是通过createViewFromTag方法不断解析并创建出对应的view，然后通过root.addView(temp, params)方法，就将我们编写的布局添加到了mContentParent 这个FrameLayout布局中。

最后就是通过cb.onContentChanged()来通知activity布局内容已经变化了；在最上面我们知道Activity的attach方法中实例化PhoneWindow的时候注册了这个接口，并实现了onContentChanged方法

public void onContentChanged() {
}

显然这是一个空的实现，所以我们可以在我们自己的Activity里重写这个方法来监测页面布局的变化。

到这里setContentView方法就分析结束了，做个总结

Activity启动后在attach方法中会实例化一个PhoneWindow对象，说明每个Activity都会拥有一个Window窗口，并且界面展示的特性是由Window控制
我们的Activity通过setContentView方法设置页面内容，会走到PhoneWindow类的setContentView方法中
在setContentView方法中，会实例化一个DecorView，这是每个Activity根布局，并根据Theme，Feature往这个布局里添加了一个LinearLayout
4.LinearLayout布局里有title和FrameLayout，把FrameLayout作为mContentParent来存放我们添加的ViewGroup或者View
最后通过Callback来通知activity页面布局更新
最终就是这张图，

## Activity的View如何与ViewRootImpl关联

1：view的requestLayout 会调用mPatent中的requestLayout  这样循环往根布局寻找

2：循环寻找那个如何找到ViewRootImpl中的，

​       a: 查看View源码发现mPartent赋值是在assignParent中

​        b:viewGroup 在addVIew的时候 会调用addViewInner 中会调用 child.assignParent(this)

​       也就是说view中的mPartent 是父布局ViewGroup 那么循环往上推到就会到DecorView 根布局

​    那么根布局的mPartent是谁呢？这个是在Activity 启动的时候 走到onResume 时候创建的ViewRootImpl

​    并且在ViewRootImpl的setView中会有view.assignParent（this）这就就把ViewRootImpl和View整个关联起来了

看代码：

~~~java
mDecor.onResourcesLoaded(mLayoutInflater, layoutResource)
这个方法最终会走到ViewGroup的addView方法；而且generateLayout方法最后会调用mDecor.finishChanging()这个方法

public void addView(View child, int index, LayoutParams params) {
        if (DBG) {
            System.out.println(this + " addView");
        }

```java
    if (child == null) {
        throw new IllegalArgumentException("Cannot add a null child view to a ViewGroup");
    }

    // addViewInner() will call child.requestLayout() when setting the new LayoutParams
    // therefore, we call requestLayout() on ourselves before, so that the child's request
    // will be blocked at our level
    requestLayout();
    invalidate(true);
    addViewInner(child, index, params, false);
}
```

void finishChanging() {
        mChanging = false;
        drawableChanged();
}
private void drawableChanged() {
        if (mChanging) {
            return;
        }

        setPadding(mFramePadding.left + mBackgroundPadding.left,
                mFramePadding.top + mBackgroundPadding.top,
                mFramePadding.right + mBackgroundPadding.right,
                mFramePadding.bottom + mBackgroundPadding.bottom);
        requestLayout();
        invalidate();



~~~


确定Activity的根布局后，最后回到PhoneWindow的setContentView方法时，几个重载方法里面的调用如下
mLayoutInflater.inflate(layoutResID, mContentParent);
或者mContentParent.addView(view, params);

这几个方法是真正把我们设置的布局内容添加到Activity的窗口里；同时这几个方法最终也会走到上面的addView方法。

很明显可以看到上述贴出的方法里有两句代码

```java
requestLayout();
invalidate(true);
```

我们平时进行自定义View的开发的时候知道这两个方法的调用会引发View的重新布局和重新绘制，但是这里就这么调用了难道在这里就开始绘制View了？

感觉不对劲啊，Android开发者都知道View的绘制是由ViewParent（ViewRootImpl类是这个类的实现类，ViewParent是一个接口）去完成的，但是我们研究setContentView源码下来暂时没发现View跟这个ViewRootImpl产生关联啊，也就是没看到View拿到View的绘制工具，那问题就来了，那他们两是什么时候关联了呢？

既然正方向不好找，那就从反反向倒推吧，上面贴出的代码都会走requestLayout()方法，它是View类的方法

```java
public void requestLayout() {
        if (mMeasureCache != null) mMeasureCache.clear();

        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == null) {
            // Only trigger request-during-layout logic if this is the view requesting it,
            // not the views in its parent hierarchy
            ViewRootImpl viewRoot = getViewRootImpl();
            if (viewRoot != null && viewRoot.isInLayout()) {
                if (!viewRoot.requestLayoutDuringLayout(this)) {
                    return;
                }
            }
            mAttachInfo.mViewRequestingLayout = this;
        }
    
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;
    
        if (mParent != null && !mParent.isLayoutRequested()) {
            mParent.requestLayout();
        }
        if (mAttachInfo != null && mAttachInfo.mViewRequestingLayout == this) {
            mAttachInfo.mViewRequestingLayout = null;
        }
    }



```


可以看到此时会调用到mParent.requestLayout()，由mParent去确定布局，这个mParent是什么呢，看定义

```java
protected ViewParent mParent;

这个类就是View的绘制工具了，由ViewRootImpl实现，接下来在这个类里找它被初始化的地方，如下

void assignParent(ViewParent parent) {
        if (mParent == null) {
            mParent = parent;
        } else if (parent == null) {
            mParent = null;
        } else {
            throw new RuntimeException("view " + this + " being added, but"
                    + " it already has a parent");
        }
    }

找了一下发现View类里没有调用这个方法，那看看ViewGroup类，因为ViewGroup是View的子类；可以看到ViewGroup有调用

private void addViewInner(View child, int index, LayoutParams params,
            boolean preventRequestLayout) {

        if (mTransition != null) {
            // Don't prevent other add transitions from completing, but cancel remove
            // transitions to let them complete the process before we add to the container
            mTransition.cancel(LayoutTransition.DISAPPEARING);
        }
    	....
    	....
        // tell our children
        if (preventRequestLayout) {
            child.assignParent(this);
        } else {
            child.mParent = this;
        }
    	....
    	....
    }


```

继续查找这个addViewInner被谁调用了，最后是ViewGroup的addView方法调用的，好像又绕回来了，这不是死局吗，再理一下思路

DecorView是Activity根布局，他添加一个layout的时候最终会调用ViewGroup的addView方法，其实DecorView就是ViewGroup的子类
DecorView的layout中有一个FrameLayout是作为mContentParent用来添加我们设置的布局；添加的时候也会调用ViewGroup的addView方法，因为mContentParent就是ViewGroup
ViewGroup最终会调用View的requestLayout去确定布局，但是ViewGroup也是View的子类，这样第一步和第二步产生的ViewGroup跳过中间环节就是走到了View里，并且View里会让ViewParent去完成最终的绘制工作
我们知道View的绘制是先绘制最外层父布局，然后循环迭代绘制里面的view，而绘制工作是由ViewParent完成，而它的实现类是ViewRootImpl，那结果就是DecorView使用ViewRootImpl去绘制视图，也就是整个Activity的视图交由ViewRootImpl去绘制
DecorView的终极父类是View，那最终的目的就是要搞清View是什么时候和ViewRootImpl产生关联的
上面分析可知View持有ViewParent的引用，并且只有一处是ViewParent的初始化，也就是assignParent这个方法；那解决思路就是要找到这个方法是在哪里被调用以让View持有它的引用，用来绘制视图
assignParent被调用之处
这里涉及到Activity的启动相关知识，这一复杂流程就留在下一篇文章分析，这里抛出结果。
每个应用都有一个主线程，也就是main线程，所有的UI操作都要在主线程操作，而这个主线程就是ActivityThread，在这个类里有一个内部类H，继承自Handler，在里面的handleMessage方法中有下面这段代码

```java
case RESUME_ACTIVITY:
                    Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "activityResume");
                    SomeArgs args = (SomeArgs) msg.obj;
                    handleResumeActivity((IBinder) args.arg1, true, args.argi1 != 0, true,
                            args.argi3, "RESUME_ACTIVITY");
                    Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                    break;
```


这里走到了handleResumeActivity这个方法

```java
final void handleResumeActivity(IBinder token,
            boolean clearHide, boolean isForward, boolean reallyResume, int seq, String reason) {
        ActivityClientRecord r = mActivities.get(token);
        if (!checkAndUpdateLifecycleSeq(seq, r, "resumeActivity")) {
            return;
        }

        // TODO Push resumeArgs into the activity for consideration
        r = performResumeActivity(token, clearHide, reason);
    
        if (r != null) {
            final Activity a = r.activity;
            ......
            ......
            ......
            if (r.window == null && !a.mFinished && willBeVisible) {
                r.window = r.activity.getWindow();
                View decor = r.window.getDecorView();
                decor.setVisibility(View.INVISIBLE);
                ViewManager wm = a.getWindowManager();
                WindowManager.LayoutParams l = r.window.getAttributes();
                a.mDecor = decor;
                l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
                l.softInputMode |= forwardBit;
                if (r.mPreserveWindow) {
                    a.mWindowAdded = true;
                    r.mPreserveWindow = false;
                    // Normally the ViewRoot sets up callbacks with the Activity
                    // in addView->ViewRootImpl#setView. If we are instead reusing
                    // the decor view we have to notify the view root that the
                    // callbacks may have changed.
                    ViewRootImpl impl = decor.getViewRootImpl();
                    if (impl != null) {
                        impl.notifyChildRebuilt();
                    }
                }
                if (a.mVisibleFromClient && !a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l);
                }
                ...
                ...
                ...
              
    }


```

方法很长，省略了很多，留了关键的，可以看到在if (r.window == null && !a.mFinished && willBeVisible) 这个判断里

先通过 r.activity.getWindow()获取当前Activity的PhoneWindow，再通过PhoneWindow获取DecorView，然后把DecorView设置隐藏；其实这里也说明在这个handler的RESUME_ACTIVITY消息里，一开始Activity的View还没显示
通过a.getWindowManager()获取PhoneWindow的ViewManager;最后通过wm.addView(decor, l)把DecorView添加到了ViewManager
ViewManager是个接口，子类是WindowManager，实现类是WindowManagerImpl
我们找到WindowManagerImpl的addview方法

@Override
    public void addView(@NonNull View view, @NonNull ViewGroup.LayoutParams params) {
        applyDefaultToken(params);
        mGlobal.addView(view, params, mContext.getDisplay(), mParentWindow);
    }

mGlobal是WindowManagerGlobal，继续到这个类去

```java
public void addView(View view, ViewGroup.LayoutParams params,
            Display display, Window parentWindow) {
		  ...
		  ....
		  ....

        ViewRootImpl root;
        View panelParentView = null;
    	...
    	...
    	...
        root = new ViewRootImpl(view.getContext(), display);
    
        view.setLayoutParams(wparams);
    
        mViews.add(view);
        mRoots.add(root);
        mParams.add(wparams);
        
        // do this last because it fires off messages to start doing things
        try {
            root.setView(view, wparams, panelParentView);
        } catch (RuntimeException e) {
            // BadTokenException or InvalidDisplayException, clean up.
            synchronized (mLock) {
                final int index = findViewLocked(view, false);
                if (index >= 0) {
                    removeViewLocked(index, true);
                }
            }
            throw e;
        }
    }


```

这个方法里new了一个ViewRootImpl实例，然后调用了它的setView方法，这个方法很长，省略如下

```java
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
     synchronized (this) {
         if (mView == null) {
             mView = view;
             ……
             view.assignParent(this);
         }
     }
 }
```


可以看到调用了View类的assignParent方法，其实这个view是前面传入的DecorView，到这里DecorView总算跟ViewRootImpl关联起来了。

可以看到调用了View类的assignParent方法，其实这个view是前面传入的DecorView，到这里DecorView总算跟ViewRootImpl关联起来了。

## View 中requestLayout 和 invalidate 的区别

## invalidate()

我们直接来看View的`invalidate()`方法：



```java
public void invalidate() {
    invalidate(true);
}

public void invalidate(boolean invalidateCache) {
    invalidateInternal(0, 0, mRight - mLeft, mBottom - mTop, invalidateCache, true);
}
```

可以看出View的`invalidate()`方法最终会调用`invalidateInternal()`方法：



```java
void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,
                        boolean fullInvalidate) {
    // ...
    // 判断是否跳过重绘
    if (skipInvalidate()) {
        return;
    }
    
    // 判断是否需要重绘
    if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)) == (PFLAG_DRAWN | PFLAG_HAS_BOUNDS)
            || (invalidateCache && (mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == PFLAG_DRAWING_CACHE_VALID)
            || (mPrivateFlags & PFLAG_INVALIDATED) != PFLAG_INVALIDATED
            || (fullInvalidate && isOpaque() != mLastIsOpaque)) {
        if (fullInvalidate) {
            mLastIsOpaque = isOpaque();
            mPrivateFlags &= ~PFLAG_DRAWN;
        }

        mPrivateFlags |= PFLAG_DIRTY;

        if (invalidateCache) {
            // 设置PFLAG_INVALIDATED标志位
            mPrivateFlags |= PFLAG_INVALIDATED;
            // 移除PFLAG_DRAWING_CACHE_VALID标志位
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        }

        final AttachInfo ai = mAttachInfo;
        final ViewParent p = mParent;
        if (p != null && ai != null && l < r && t < b) {
            // damage表示要重绘的区域
            final Rect damage = ai.mTmpInvalRect;
            damage.set(l, t, r, b);
            // 将要重绘的区域传给父View
            p.invalidateChild(this, damage);
        }
        // ...
    }
}
```

`invalidateInternal()`方法中首先会根据`skipInvalidate()`方法判断是否跳过绘制，如果同时满足以下三个条件就直接return，跳过重绘。

- View是不可见的
- 当前没有设置动画
- 父View的类型不是ViewGroup或者父ViewGoup不处于过渡态

接下来根据一系列条件判断是否需要重绘，如果满足以下任意一条就进行重绘。

- View已经被绘制完成并且具有边界
- invalidateCache为true并且设置了**PFLAG_DRAWING_CACHE_VALID**标志位，即绘制缓存可用
- 没有设置**PFLAG_INVALIDATED**标志位，即没有被重绘过
- fullInvalidate为true并且透明度发生了变化

接下来判断如果invalidateCache为true，就给View设置**PFLAG_INVALIDATED**标志位，这一步很重要，后面还会提到，通过上面的调用也能看出这里的invalidateCache传入的值为true，因此会设置这个标志位。方法的最后会调用mParent即父View的`invalidateChild()`方法，将要重绘的区域damage传递给父View。下面我们来看ViewGroup的`invalidateChild()`方法：



```php
@Override
public final void invalidateChild(View child, final Rect dirty) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null && attachInfo.mHardwareAccelerated) {
        // 开启了硬件加速
        onDescendantInvalidated(child, child);
        return;
    }

    ViewParent parent = this;
    if (attachInfo != null) {
        // ...
        do {
            View view = null;
            if (parent instanceof View) {
                view = (View) parent;
            }
            // ...
            parent = parent.invalidateChildInParent(location, dirty);
            // ...
        } while (parent != null);
    }
}
```

方法内部首先会判断是否开启了硬件加速，接下来我们分别看一下关闭和开启硬件加速情况下的重绘流程。

- **关闭硬件加速**

关闭硬件加速的情况下会循环调用`invalidateChildInParent()`方法，将返回值赋给parent，当parent为null时退出循环，我们来看ViewGroup的`invalidateChildInParent()`方法。



```java
@Override
public ViewParent invalidateChildInParent(final int[] location, final Rect dirty) {
    if ((mPrivateFlags & (PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID)) != 0) {
        // ...
        // 移除PFLAG_DRAWING_CACHE_VALID标志位
        mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        // ...
        return mParent;
    }
    return null;
}
```

这里省略了大量代码，主要是对子View传递过来的重绘区域进行运算处理，方法最后会返回mParent。因此在`invalidateChild()`方法中会通过循环逐层调用父View的`invalidateChildInParent()`方法，那么当调用到最顶层ViewGroup——DecorView的`invalidateChild()`方法时，它的mParent是谁呢？我们可以回顾一下ViewRootImpl的`setView()`方法：



```java
public void setView(View view, WindowManager.LayoutParams attrs, View panelParentView) {
    synchronized (this) {
        if (mView == null) {
            mView = view;
            // ...
            requestLayout();
            // ...
            view.assignParent(this);
            // ...
        }
    }
}
```

可以发现方法执行了`view.assignParent(this)`，这里的view其实就是DecorView（ActivityThread的`handleResumeActivity()`方法中调用`wm.addView()`传过来的），我们来看一下`assignParent()`方法，它定义在View中：



```csharp
void assignParent(ViewParent parent) {
    if (mParent == null) {
        mParent = parent;
    } else if (parent == null) {
        mParent = null;
    } else {
        throw new RuntimeException("view " + this + " being added, but"
                + " it already has a parent");
    }
}
```

很明显`assignParent()`方法完成了View的成员变量mParent的赋值，因此DecorView的mParent就是上面传入的this，也就是ViewRootImpl。既然清楚了DecorView的mParent，接下来我们就来看一下ViewRootImpl的`invalidateChildInParent()`方法：



```java
@Override
public ViewParent invalidateChildInParent(int[] location, Rect dirty) {
    checkThread();
    // ...
    invalidateRectOnScreen(dirty);
    return null;
}
```

方法内部首先会调用`checkThread()`方法：



```cpp
void checkThread() {
    if (mThread != Thread.currentThread()) {
        throw new CalledFromWrongThreadException(
                "Only the original thread that created a view hierarchy can touch its views.");
    }
}
```

`checkThread()`方法会判断当前线程是否为主线程，如果不是主线程就直接抛出异常，因此我们需要特别注意，**`invalidate()`方法必须在主线程中调用**。回到ViewRootImpl的`invalidateChildInParent()`方法，最后调用了`invalidateRectOnScreen()`方法，同时由于返回值为null，因此执行完`invalidateChildInParent()`方法后parent被赋值为null，退出do-while循环。接下来我们就来看一下`invalidateRectOnScreen()`方法：



```cpp
private void invalidateRectOnScreen(Rect dirty) {
    // ...
    if (!mWillDrawSoon && (intersected || mIsAnimating)) {
        scheduleTraversals();
    }
}
```

`invalidateRectOnScreen()`方法内部会调用`scheduleTraversals()`方法，这个方法我们很熟悉了，接下来会调用`performTraversals()`方法，开始View的三大流程，这里再来回顾一下：



```java
private void performTraversals() {
    // ...
    boolean layoutRequested = mLayoutRequested && (!mStopped || mReportNextDraw);
    if (layoutRequested) {
        // ...
        // 调用performMeasure()方法开始measure流程
        measureHierarchy(host, lp, res,
                desiredWindowWidth, desiredWindowHeight);
    }
    // ...
    final boolean didLayout = layoutRequested && (!mStopped || mReportNextDraw);
    if (didLayout) {
        // ...
        // 开始layout流程
        performLayout(lp, mWidth, mHeight);
    }
    // ...
    // 开始draw流程
    performDraw();
    // ...
}
```

由于此时没有给mLayoutRequested赋值，它的默认值为false，因此不会调用`measureHierarchy()`和`performLayout()`方法，只调用`performDraw()`方法，换句话说就是不会执行measure和layout流程，只执行draw流程，接下来就是调用DecorView的`draw()`方法，遍历DecorView的子View，逐层完成子View的绘制。

- **开启硬件加速**

开启硬件加速时在ViewGroup的`invalidateChild()`方法中会调用`onDescendantInvalidated()`方法并直接返回，不会执行后面的`invalidateChildInParent()`方法，我们来看一下`onDescendantInvalidated()`方法：



```kotlin
@Override
public void onDescendantInvalidated(@NonNull View child, @NonNull View target) {
    // ...
    if (mParent != null) {
        mParent.onDescendantInvalidated(this, target);
    }
}
```

方法内部会调用mParent的`onDescendantInvalidated()`方法，和`invalidateChildInParent()`类似，接下来会逐级调用父View的`onDescendantInvalidated()`方法，最后来到ViewRootImpl的`onDescendantInvalidated()`方法。



```tsx
@Override
public void onDescendantInvalidated(@NonNull View child, @NonNull View descendant) {
    if ((descendant.mPrivateFlags & PFLAG_DRAW_ANIMATION) != 0) {
        mIsAnimating = true;
    }
    invalidate();
}
```

接下来会调用ViewRootImpl的`invalidate`方法：



```cpp
void invalidate() {
    mDirty.set(0, 0, mWidth, mHeight);
    if (!mWillDrawSoon) {
        scheduleTraversals();
    }
}
```

可以看到这里同样调用了`scheduleTraversals()`方法，之后的流程和关闭硬件加速的情况类似，同样是调用`performDraw()`方法，不同的是开启硬件加速的情况下会执行`mAttachInfo.mThreadedRenderer.draw(mView, mAttachInfo, this, callback)`，上一篇文章[Android自定义View的基石——View工作原理总结](https://www.jianshu.com/p/7f635283067e)中分析View的draw流程时也介绍过了，这里就不再分析了，之后会依次调用ThreadedRenderer的`updateRootDisplayList()`、`updateViewTreeDisplayList()`方法。



```cpp
private void updateViewTreeDisplayList(View view) {
    view.mPrivateFlags |= View.PFLAG_DRAWN;
    view.mRecreateDisplayList = (view.mPrivateFlags & View.PFLAG_INVALIDATED)
            == View.PFLAG_INVALIDATED;
    view.mPrivateFlags &= ~View.PFLAG_INVALIDATED;
    view.updateDisplayListIfDirty();
    view.mRecreateDisplayList = false;
}
```

这里需要注意，根据此前的分析，在调用View的`invalidate()`方法后，会给当前View设置**PFLAG_INVALIDATED**标志位，因此它的mRecreateDisplayList变量值为true，而其他的父级View由于没有设置**PFLAG_INVALIDATED**标志位，mRecreateDisplayList值为false。接下来会调用view的`updateDisplayListIfDirty()`方法，这里的view是DecorView。



```kotlin
public RenderNode updateDisplayListIfDirty() {
    final RenderNode renderNode = mRenderNode;
    // ...
    if ((mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == 0
            || !renderNode.isValid()
            || (mRecreateDisplayList)) {
        if (renderNode.isValid()
                && !mRecreateDisplayList) {
            // 不需要重新进行绘制
            mPrivateFlags |= PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID;
            mPrivateFlags &= ~PFLAG_DIRTY_MASK;
            dispatchGetDisplayList();

            return renderNode;
        }

        // 需要重新进行绘制
        mRecreateDisplayList = true;

        try {
            // ...
            if ((mPrivateFlags & PFLAG_SKIP_DRAW) == PFLAG_SKIP_DRAW) {
                // 如果设置了PFLAG_SKIP_DRAW标志位，执行dispatchDraw()方法
                dispatchDraw(canvas);
                drawAutofilledHighlight(canvas);
                if (mOverlay != null && !mOverlay.isEmpty()) {
                    mOverlay.getOverlayView().draw(canvas);
                }
                if (debugDraw()) {
                    debugDrawFocus(canvas);
                }
            } else {
                // 没有设置PFLAG_SKIP_DRAW标志位，执行draw()方法
                draw(canvas);
            }
            // ...
        } finally {
            renderNode.end(canvas);
            setDisplayListProperties(renderNode);
        }
    } else {
        mPrivateFlags |= PFLAG_DRAWN | PFLAG_DRAWING_CACHE_VALID;
        mPrivateFlags &= ~PFLAG_DIRTY_MASK;
    }
    return renderNode;
}
```

由于页面首次绘制完成后执行了`renderNode.end(canvas)`，因此这里`renderNode.isValid()`返回值为true，而DecorView的mRecreateDisplayList值为false，因不会执行后面的重新绘制逻辑，取而代之的是调用`dispatchGetDisplayList()`方法，我们来看一下这个方法：



```java
@Override
protected void dispatchGetDisplayList() {
    final int count = mChildrenCount;
    final View[] children = mChildren;
    for (int i = 0; i < count; i++) {
        final View child = children[i];
        if (((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null)) {
            recreateChildDisplayList(child);
        }
    }
    // ...
}

private void recreateChildDisplayList(View child) {
    child.mRecreateDisplayList = (child.mPrivateFlags & PFLAG_INVALIDATED) != 0;
    child.mPrivateFlags &= ~PFLAG_INVALIDATED;
    child.updateDisplayListIfDirty();
    child.mRecreateDisplayList = false;
}
```

`dispatchGetDisplayList()`方法内部会遍历子View，依次调用`recreateChildDisplayList()`方法，不难看出`recreateChildDisplayList()`方法和`updateViewTreeDisplayList()`方法很像，接下来同样会调用`updateDisplayListIfDirty()`方法，对于没有设置**PFLAG_INVALIDATED**标志位的View，它的mRecreateDisplayList值为false，会重复上面的过程，即调用`dispatchGetDisplayList()`方法；而对于调用了`invalidate()`方法的View，由于设置了**PFLAG_INVALIDATED**标志位，它的mRecreateDisplayList值为true，会执行`updateDisplayListIfDirty()`方法最后的重绘逻辑，即调用`dispatchDraw()`方法或者`draw()`方法完成自身及子View的绘制。
 最后总结一下`invalidate()`方法，调用View的`invalidate()`方法后会逐级调用父View的方法，最终导致ViewRootImpl的`scheduleTraversals()`方法被调用，进而调用`performTraversals()`方法。由于mLayoutRequested的值为false，因此不会执行measure和layout流程，只执行draw流程。draw流程的执行过程和是否开启硬件加速有关：

- 如果关闭了硬件加速，从DecorView开始的所有View都会重新完成绘制
- 如果开启了硬件加速，只有调用`invalidate()`方法的View（包括它的子View）会完成重新绘制

由此也可以看出，开启硬件加速确实可以提高重绘的效率。
 此外，由于`invalidate()`方法必须在主线程中调用，那么如果我们想要在子线程中刷新视图要怎么做呢？不用担心，官方还为我们提供了一个`postInvalidate()`方法，其实从名称上我们也能猜到它的作用了，就是用于在子线程中刷新视图，简单看一下它的定义：



```java
public void postInvalidate() {
    postInvalidateDelayed(0);
}

public void postInvalidateDelayed(long delayMilliseconds) {
    final AttachInfo attachInfo = mAttachInfo;
    if (attachInfo != null) {
        attachInfo.mViewRootImpl.dispatchInvalidateDelayed(this, delayMilliseconds);
    }
}

public void dispatchInvalidateDelayed(View view, long delayMilliseconds) {
    Message msg = mHandler.obtainMessage(MSG_INVALIDATE, view);
    mHandler.sendMessageDelayed(msg, delayMilliseconds);
}
```

哈哈，果然还是用到了Handler，mHandler是ViewRootImpl中的一个成员变量，类型为**ViewRootHandler**，我们来看一下ViewRootHandler对**MSG_INVALIDATE**消息的处理：



```java
final class ViewRootHandler extends Handler {
    // ...
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_INVALIDATE:
                ((View) msg.obj).invalidate();
                break;
            // ...
        }
    }
}
```

可以看出最后还是调用了`invalidate()`方法，因此`postInvalidate()`方法其实就是通过Handler完成了线程的切换，使得`invalidate()`方法在主线程中被调用。

## requestLayout()

我们来看View的`requestLayout()`方法：



```csharp
public void requestLayout() {
    // ...
    // 给View添加两个标志位
    mPrivateFlags |= PFLAG_FORCE_LAYOUT;
    mPrivateFlags |= PFLAG_INVALIDATED;

    if (mParent != null && !mParent.isLayoutRequested()) {
        // 调用父类的requestLayout()方法
        mParent.requestLayout();
    }
    // ...
}
```

`requestLayout()`方法内部会调用mParent即父View的`requestLayout()`方法，最终会来到ViewRootImpl的`requestLayout()`方法：



```java
@Override
public void requestLayout() {
    if (!mHandlingLayoutInLayoutRequest) {
        checkThread();
        mLayoutRequested = true;
        scheduleTraversals();
    }
}
```

首先还是会进行线程的检查，因此`requestLayout()`方法同样只能在主线程中调用。接着会把mLayoutRequested赋值为true并调用`scheduleTraversals()`方法。后面的流程相信也不用我多说了，调用`performTraversals()`方法，由于将mLayoutRequested赋值为true，因此会依次执行`measureHierarchy()`、`performLayout()`和`performDraw()`方法，开始View的三大流程。
 到这里还没完，我们需要探究一下View调用`requrestLayout()`是否会导致View树中的所有View都进行重新测量、布局和绘制。我们注意到调用`requestLayout()`方法后，会为当前View及所有父级View添加**PFLAG_FORCE_LAYOUT**和**PFLAG_INVALIDATED**标志位。首先来回顾一下View的`measure()`方法：



```java
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    // ...
    final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;

    final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec
            || heightMeasureSpec != mOldHeightMeasureSpec;
    final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
            && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
    final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
            && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
    final boolean needsLayout = specChanged
            && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize);

    if (forceLayout || needsLayout) {
        // ...
        onMeasure(widthMeasureSpec, heightMeasureSpec);
        // ...
        // 设置PFLAG_LAYOUT_REQUIRED标志位
        mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
    }

    mOldWidthMeasureSpec = widthMeasureSpec;
    mOldHeightMeasureSpec = heightMeasureSpec;
    // ...
}
```

可以看出，当View设置了**PFLAG_FORCE_LAYOUT**标志位后，forceLayout的值为true，因此会执行`onMeasure()` 方法，而对于没有设置**PFLAG_FORCE_LAYOUT**标志位的View，需要判断测量尺寸是否发生了改变，如果改变了才会调用`onMeasure()`方法。在调用`onMeasure()`方法后会给View设置**PFLAG_LAYOUT_REQUIRED**标志位，我们再来看View的`layout()`方法：



```java
public void layout(int l, int t, int r, int b) {
    // ...
    int oldL = mLeft;
    int oldT = mTop;
    int oldB = mBottom;
    int oldR = mRight;

    boolean changed = isLayoutModeOptical(mParent) ?
            setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);

    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
        onLayout(changed, l, t, r, b);
        // ...
    }
    // ...
}
```

对于设置了**PFLAG_LAYOUT_REQUIRED**标志位的View，`onLayout()`方法肯定会执行，另一种情况就是View的四个顶点坐标发生改变，也会执行`onLayout()`方法。
 结合上面的分析可以得出结论，当View调用了`requestLayout()`方法后，自身及父级View的`onMeasure()`和`onLayout()`方法会被调用，对于它的子View，`onMeasure()`和`onLayout()`方法不一定被调用。
 对于draw流程，`performDraw()`方法会调用ViewRootImpl中的`draw()`方法：



```java
private boolean draw(boolean fullRedrawNeeded) {
    // ...
    final Rect dirty = mDirty;
    // ...
    if (!dirty.isEmpty() || mIsAnimating || accessibilityFocusDirty) {
        if (mAttachInfo.mThreadedRenderer != null && mAttachInfo.mThreadedRenderer.isEnabled()) {
            // ...
            // 开启了硬件加速
            mAttachInfo.mThreadedRenderer.draw(mView, mAttachInfo, this, callback);
        } else {
            // ...
            // 关闭了硬件加速
            if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset,
                    scalingRequired, dirty, surfaceInsets)) {
                return false;
            }
        }
    }
    // ...
    return useAsyncReport;
}
```

dirty指向ViewRootImpl中的一个成员变量mDirty，类型为Rect，在ViewRootImpl的`invalidate()`方法中会调用`set()`方法为其设置四个边界值，由于此时没有调用`invalidate()`方法，因此`mDirty.isEmpty()`返回true，不会执行后面的绘制方法，因此整个View树不会进行重新绘制。不过也有这样一种情况，我们知道在执行VIew的layout流程时会调用`setFrame()`方法，在`setFrame()`方法中有这样的逻辑：



```java
int oldWidth = mRight - mLeft;
int oldHeight = mBottom - mTop;
int newWidth = right - left;
int newHeight = bottom - top;
boolean sizeChanged = (newWidth != oldWidth) || (newHeight != oldHeight);

invalidate(sizeChanged);
```

可以看出当View的宽或高发生改变时会调用`invalidate()`方法，导致View的重新绘制。

## 总结

我们已经分析过了`invalidate()`和`requestLayout()`的具体实现，现在就来总结一下`invalidate()`和`requestLayout()`的异同：
 **相同点**
 1.`invalidate()`和`requestLayout()`方法最终都会调用ViewRootImpl的`performTraversals()`方法。
 **不同点**
 1.`invalidate()`方法不会执行`measureHierarchy()`和`performLayout()`方法，也就不会执行measure和layout流程，只执行draw流程，如果开启了硬件加速则只进行调用者View的重绘。
 2.`requestLayout()`方法会依次`measureHierarchy()`、`performLayout()`和`performDraw()`方法，调用者View和它的父级View会重新进行measure、layout，一般情况下不会执行draw流程，子View不一定会重新measure和layout。
 综上，当只需要进行重新绘制时就调用`invalidate()`，如果需要重新测量和布局就调用`requestLayout()`，但是`requestLayout()`不保证进行重新绘制，如果要进行重新绘制可以再手动调用`invalidate()`。