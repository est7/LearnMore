## 1：ViewGroup

### 1.1：ViewGroup 默认不走onDraw方法

```java
//打开方式
setWillNotDraw(false)
```

### 1.2：ViewGroup测量子View

```java
measureChild(child,widthMeasureSpec,heightMeasureSpec)
```

### 1.3：ViewGroup 绘制子View

```java
child.layout(int l, int t, int r, int b)
```

### 1.4:MeasureSpec

mode:

 UNSPECIFIED 

EXACTLY :精确的  100dp | mathParten

AT_MOST:最多   warpParten

```
wrap_content-> MeasureSpec.AT_MOST
match_parent -> MeasureSpec.EXACTLY
具体值 -> MeasureSpec.EXACTLY
```



```java
//对应11000000000000000000000000000000;总共32位，前两位是1
int MODE_MASK  = 0xc0000000;
 
//提取模式
public static int getMode(int measureSpec) {
    return (measureSpec & MODE_MASK);
}
//提取数值
public static int getSize(int measureSpec) {
    return (measureSpec & ~MODE_MASK);
}

MeasureSpec.getMode(int spec) //获取MODE
MeasureSpec.getSize(int spec) //获取数值
```

### 1,4：ViewGroup子View测量

```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
    int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
    int measureWidthMode = MeasureSpec.getMode(widthMeasureSpec);
    int measureHeightMode = MeasureSpec.getMode(heightMeasureSpec);
 
    int height = 0;
    int width = 0;
    int count = getChildCount();
    for (int i=0;i<count;i++) {
		//测量子控件
        View child = getChildAt(i);
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
		//获得子控件的高度和宽度
        int childHeight = child.getMeasuredHeight();
        int childWidth = child.getMeasuredWidth();
		//得到最大宽度，并且累加高度
        height += childHeight;
        width = Math.max(childWidth, width);
    }
 
    setMeasuredDimension((measureWidthMode == MeasureSpec.EXACTLY) ? measureWidth: width, (measureHeightMode == MeasureSpec.EXACTLY) ? measureHeight: height);
}
```

### 1.5：View group  获取子View的margin

```java
//重写generateLayoutParams（）函数
@Override
protected LayoutParams generateLayoutParams(LayoutParams p) {
    return new MarginLayoutParams(p);
}
 
@Override
public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new MarginLayoutParams(getContext(), attrs);
}
 
@Override
protected LayoutParams generateDefaultLayoutParams() {
    return new MarginLayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
}
```

原理：

先看ViewGroup 加载调用堆栈

```java
setLayoutParams:17269, View (android.view)
addViewInner:5121, ViewGroup (android.view)//解析布局中View的LayoutParams (默认是只解析LayoutParams(只会读取宽高))
addView:4935, ViewGroup (android.view)
addView:4907, ViewGroup (android.view)
rInflate:1127, LayoutInflater (android.view)
rInflateChildren:1084, LayoutInflater (android.view)
inflate:682, LayoutInflater (android.view)
inflate:534, LayoutInflater (android.view)
inflate:481, LayoutInflater (android.view)//layoutInflater 解析添加ViewGroup
onResourcesLoaded:2084, DecorView (com.android.internal.policy)//decorView 创建
generateLayout:2627, PhoneWindow (com.android.internal.policy)
installDecor:2694, PhoneWindow (com.android.internal.policy)
getDecorView:2094, PhoneWindow (com.android.internal.policy)//phoneWindow创建
createSubDecor:864, AppCompatDelegateImpl (androidx.appcompat.app)
ensureSubDecor:806, AppCompatDelegateImpl (androidx.appcompat.app)
setContentView:693, AppCompatDelegateImpl (androidx.appcompat.app)
setContentView:170, AppCompatActivity (androidx.appcompat.app)
onCreate:13, SliderViewActivity (com.example.demo.sundu.custview.SliderView)//activity启动
performCreate:7802, Activity (android.app)
performCreate:7791, Activity (android.app)
callActivityOnCreate:1299, Instrumentation (android.app)
performLaunchActivity:3245, ActivityThread (android.app)
handleLaunchActivity:3409, ActivityThread (android.app)
execute:83, LaunchActivityItem (android.app.servertransaction)
executeCallbacks:135, TransactionExecutor (android.app.servertransaction)
execute:95, TransactionExecutor (android.app.servertransaction)
handleMessage:2016, ActivityThread$H (android.app)
dispatchMessage:107, Handler (android.os)
loop:214, Looper (android.os)
main:7356, ActivityThread (android.app)
invoke:-1, Method (java.lang.reflect)
run:492, RuntimeInit$MethodAndArgsCaller (com.android.internal.os)
main:930, ZygoteInit (com.android.internal.os)
```

关键方法

```java
//viewgroup 在初始化的时候
 private void addViewInner(View child, int index, LayoutParams params,
            boolean preventRequestLayout) {

     //获取layoutParams
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
		
        if (preventRequestLayout) {
            child.mLayoutParams = params;
        } else {
            child.setLayoutParams(params);
        }

        if (index < 0) {
            index = mChildrenCount;
        }
    }
//创建LayoutParams 
 public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

//只会读取宽高，比较尴尬
   public LayoutParams(Context c, AttributeSet attrs) {
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ViewGroup_Layout);
            setBaseAttributes(a,
                    R.styleable.ViewGroup_Layout_layout_width,
                    R.styleable.ViewGroup_Layout_layout_height);
            a.recycle();
        }

```



获取方式：

```java
//重写generateLayoutParams（）函数

@Override
protected LayoutParams generateLayoutParams(LayoutParams p) {
    return new MarginLayoutParams(p);
}
 
@Override
public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new MarginLayoutParams(getContext(), attrs);
}
 
@Override
protected LayoutParams generateDefaultLayoutParams() {
    return new MarginLayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
}
```

MarginLayoutParams 会获取不同margin数据

```java
 public MarginLayoutParams(Context c, AttributeSet attrs) {
            super();

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ViewGroup_MarginLayout);
            setBaseAttributes(a,
                    R.styleable.ViewGroup_MarginLayout_layout_width,
                    R.styleable.ViewGroup_MarginLayout_layout_height);

            int margin = a.getDimensionPixelSize(
                    com.android.internal.R.styleable.ViewGroup_MarginLayout_layout_margin, -1);
            if (margin >= 0) {
                leftMargin = margin;
                topMargin = margin;
                rightMargin= margin;
                bottomMargin = margin;
            } else {
                int horizontalMargin = a.getDimensionPixelSize(
                        R.styleable.ViewGroup_MarginLayout_layout_marginHorizontal, -1);
                int verticalMargin = a.getDimensionPixelSize(
                        R.styleable.ViewGroup_MarginLayout_layout_marginVertical, -1);

                if (horizontalMargin >= 0) {
                    leftMargin = horizontalMargin;
                    rightMargin = horizontalMargin;
                } else {
                    leftMargin = a.getDimensionPixelSize(
                            R.styleable.ViewGroup_MarginLayout_layout_marginLeft,
                            UNDEFINED_MARGIN);
                    if (leftMargin == UNDEFINED_MARGIN) {
                        mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
                        leftMargin = DEFAULT_MARGIN_RESOLVED;
                    }
                    rightMargin = a.getDimensionPixelSize(
                            R.styleable.ViewGroup_MarginLayout_layout_marginRight,
                            UNDEFINED_MARGIN);
                    if (rightMargin == UNDEFINED_MARGIN) {
                        mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;
                        rightMargin = DEFAULT_MARGIN_RESOLVED;
                    }
                }

                startMargin = a.getDimensionPixelSize(
                        R.styleable.ViewGroup_MarginLayout_layout_marginStart,
                        DEFAULT_MARGIN_RELATIVE);
                endMargin = a.getDimensionPixelSize(
                        R.styleable.ViewGroup_MarginLayout_layout_marginEnd,
                        DEFAULT_MARGIN_RELATIVE);

                if (verticalMargin >= 0) {
                    topMargin = verticalMargin;
                    bottomMargin = verticalMargin;
                } else {
                    topMargin = a.getDimensionPixelSize(
                            R.styleable.ViewGroup_MarginLayout_layout_marginTop,
                            DEFAULT_MARGIN_RESOLVED);
                    bottomMargin = a.getDimensionPixelSize(
                            R.styleable.ViewGroup_MarginLayout_layout_marginBottom,
                            DEFAULT_MARGIN_RESOLVED);
                }

                if (isMarginRelative()) {
                   mMarginFlags |= NEED_RESOLUTION_MASK;
                }
            }

            final boolean hasRtlSupport = c.getApplicationInfo().hasRtlSupport();
            final int targetSdkVersion = c.getApplicationInfo().targetSdkVersion;
            if (targetSdkVersion < JELLY_BEAN_MR1 || !hasRtlSupport) {
                mMarginFlags |= RTL_COMPATIBILITY_MODE_MASK;
            }

            // Layout direction is LTR by default
            mMarginFlags |= LAYOUT_DIRECTION_LTR;

            a.recycle();
        }
```



处理：onMeasure



```java
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
    int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
    int measureWidthMode = MeasureSpec.getMode(widthMeasureSpec);
    int measureHeightMode = MeasureSpec.getMode(heightMeasureSpec);
 
    int height = 0;
    int width = 0;
    int count = getChildCount();
    for (int i=0;i<count;i++) {
 
        View child = getChildAt(i);
        measureChild(child, widthMeasureSpec, heightMeasureSpec);
 
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childHeight = child.getMeasuredHeight()+lp.topMargin+lp.bottomMargin;
        int childWidth = child.getMeasuredWidth()+lp.leftMargin+lp.rightMargin;
 
        height += childHeight;
        width = Math.max(childWidth, width);
    }
 
    setMeasuredDimension((measureWidthMode == MeasureSpec.EXACTLY) ? measureWidth: width, (measureHeightMode == MeasureSpec.EXACTLY) ? measureHeight: height);
}
```





1.6：ViewGroup 设置子View 的位置

```java
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int top = 0;
    int count = getChildCount();
    for (int i=0;i<count;i++) {
 
        View child = getChildAt(i);
 
        int childHeight = child.getMeasuredHeight();
        int childWidth = child.getMeasuredWidth();
 
        child.layout(0, top, childWidth, top + childHeight);
        top += childHeight;
    }
}
```



使用 marginlayout数据

```java
@Override
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    int top = 0;
    int count = getChildCount();
    for (int i=0;i<count;i++) {
 
        View child = getChildAt(i);
 
        MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
        int childHeight = child.getMeasuredHeight()+lp.topMargin+lp.bottomMargin;
        int childWidth = child.getMeasuredWidth()+lp.leftMargin+lp.rightMargin;
 
        child.layout(0, top, childWidth, top + childHeight);
        top += childHeight;
    }
}
```



