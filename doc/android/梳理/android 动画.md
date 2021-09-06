Android 动画

## 1：属性动画

```kotlin
Animator 
	-- ValueAnimator
		-- ObjectAnimator

ViewPropertyAnimator 专门针对View 的 可以同时设置多个属性动画 内部使用一个ValueAnimator
ViewPropertyAnimator 与AnimatorSet 区别在于 animatorSet可以设置animator 的执行顺序


```

#### 原理：其中所有的 动画都依赖与 ValueAnimator - >依赖屏幕刷新机制回调



```kotlin
1:动画第一次订阅 能够监测Vsync 信号
scheduleVsyncLocked:826, Choreographer (android.view)//订阅Vsyc
scheduleFrameLocked:632, Choreographer (android.view)
postCallbackDelayedInternal:461, Choreographer (android.view)
postFrameCallbackDelayed:539, Choreographer (android.view)
postFrameCallback:519, Choreographer (android.view)
postFrameCallback:248, AnimationHandler$MyFrameCallbackProvider (android.animation)
addAnimationFrameCallback:95, AnimationHandler (android.animation)
addAnimationCallback:1521, ValueAnimator (android.animation)
start:1068, ValueAnimator (android.animation)
start:1098, ValueAnimator (android.animation)//动画开始


动画执行中订阅：
scheduleVsyncLocked:826, Choreographer (android.view)//订阅
scheduleFrameLocked:632, Choreographer (android.view)
postCallbackDelayedInternal:461, Choreographer (android.view)
postFrameCallbackDelayed:539, Choreographer (android.view)
postFrameCallback:519, Choreographer (android.view)
postFrameCallback:248, AnimationHandler$MyFrameCallbackProvider (android.animation)
doFrame:56, AnimationHandler$1 (android.animation)
run:964, Choreographer$CallbackRecord (android.view)
doCallbacks:790, Choreographer (android.view)
doFrame:721, Choreographer (android.view)
run:951, Choreographer$FrameDisplayEventReceiver (android.view)//信号回来后再次订阅

动画回调：

onAnimationUpdate:85, FishDrawable$1 (com.enjoy.fish)//收到UpDate 信息
animateValue:1558, ValueAnimator (android.animation)
animateBasedOnTime:1349, ValueAnimator (android.animation)
doAnimationFrame:1481, ValueAnimator (android.animation)
doAnimationFrame:146, AnimationHandler (android.animation)
access$100:37, AnimationHandler (android.animation)
doFrame:54, AnimationHandler$1 (android.animation)
run:964, Choreographer$CallbackRecord (android.view)
doCallbacks:790, Choreographer (android.view)
doFrame:721, Choreographer (android.view)
run:951, Choreographer$FrameDisplayEventReceiver (android.view)//信号回来 

```

2:视图动画

```
Animation
	|- AlphaAnimation
	|- ScaleAnimation
	|- TranslateAnimation
	|- RotateAnimation
	
```



原理：onDraw 时 使用 矩阵变化

```java
View{

 public void startAnimation(Animation animation) {
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidateParentCaches();
        invalidate(true);
    }
    
}

ViewGroup 执行
  protected void dispatchDraw(Canvas canvas) {
     for (int i = 0; i < childrenCount; i++) {
            while (transientIndex >= 0 && mTransientIndices.get(transientIndex) == i) {
                final View transientChild = mTransientViews.get(transientIndex);
                if ((transientChild.mViewFlags & VISIBILITY_MASK) == VISIBLE ||
                        transientChild.getAnimation() != null) {
                    more |= drawChild(canvas, transientChild, drawingTime);
                }
                transientIndex++;
                if (transientIndex >= transientCount) {
                    transientIndex = -1;
                }
            }

            final int childIndex = getAndVerifyPreorderedIndex(childrenCount, i, customOrder);
            final View child = getAndVerifyPreorderedView(preorderedList, children, childIndex);
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
                more |= drawChild(canvas, child, drawingTime);
            }
        }
}

protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return child.draw(canvas, this, drawingTime);
    }

View 中
boolean draw(Canvas canvas, ViewGroup parent, long drawingTime) {
     
        boolean concatMatrix = false;
        final boolean scalingRequired = mAttachInfo != null && mAttachInfo.mScalingRequired;
    	//获取当前动画
        final Animation a = getAnimation();
        if (a != null) {
            more = applyLegacyAnimation(parent, drawingTime, a, scalingRequired);
            concatMatrix = a.willChangeTransformationMatrix();
            if (concatMatrix) {
                mPrivateFlags3 |= PFLAG3_VIEW_IS_ANIMATING_TRANSFORM;
            }
            transformToApply = parent.getChildTransformation();
        }
       //最终会执行 使用MATRIX 进行视图变换
       canvas.concat(transformToApply.getMatrix());
}

//以渐变为例
public class AlphaAnimation extends Animation {
 /**
     * Changes the alpha property of the supplied {@link Transformation}
     */
    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final float alpha = mFromAlpha;
        t.setAlpha(alpha + ((mToAlpha - alpha) * interpolatedTime));
    }
}

    
 
```

