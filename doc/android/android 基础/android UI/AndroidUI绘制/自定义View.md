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

