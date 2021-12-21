## 关键类：

#### 1：屏幕信号订阅：此类实际与屏幕刷新同步信号交互 实现类为 Choreographer$FrameDisplayEventReceiver

```java
public abstract class DisplayEventReceiver {
    
    //订阅屏幕刷新信号   当屏幕信号回来时 调用dispatchVsync
    /**
     * Schedules a single vertical sync pulse to be delivered when the next
     * display frame begins.
     */
    @UnsupportedAppUsage
    public void scheduleVsync() {
        if (mReceiverPtr == 0) {
            Log.w(TAG, "Attempted to schedule a vertical sync pulse but the display event "
                    + "receiver has already been disposed.");
        } else {
            nativeScheduleVsync(mReceiverPtr);
        }
    }
    
    @FastNative
    private static native void nativeScheduleVsync(long receiverPtr);
    
    //屏幕刷新信号回调
    // Called from native code.
    @SuppressWarnings("unused")
    @UnsupportedAppUsage
    private void dispatchVsync(long timestampNanos, long physicalDisplayId, int frame) {
        onVsync(timestampNanos, physicalDisplayId, frame);
    }
}
```

#### 2：管理类：

```java
public final class Choreographer {
    //和底层屏幕绘制信号同步交互类
	private final class FrameDisplayEventReceiver extends DisplayEventReceiver  implements Runnable {
          @Override
        public void onVsync(long timestampNanos, long physicalDisplayId, int frame) {
          。。。
            Message msg = Message.obtain(mHandler, this);
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
        }
  
     @Override
        public void run() {
            mHavePendingVsync = false;
            doFrame(mTimestampNanos, mFrame);
        }
    }
    
    
    //屏幕刷新信号回来了 处理分发给订阅者
    void doFrame(long frameTimeNanos, int frame) {
        doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);
    }
    //回调订阅者callback
    void doCallbacks(int callbackType, long frameTimeNanos) {
        Trace.traceBegin(Trace.TRACE_TAG_VIEW, CALLBACK_TRACE_TITLES[callbackType]);
        for (CallbackRecord c = callbacks; c != null; c = c.next) {
            if (DEBUG_FRAMES) {
                Log.d(TAG, "RunCallback: type=" + callbackType
                      + ", action=" + c.action + ", token=" + c.token
                      + ", latencyMillis=" + (SystemClock.uptimeMillis() - c.dueTime));
            }
            c.run(frameTimeNanos);
        }
    }

    //添加订阅者callback回调  eg:属性动画调用
    public void postFrameCallback(FrameCallback callback) {
        //添加回调callback 会最终调用： 屏幕信号订阅同步 
        postFrameCallbackDelayed(callback, 0);//最终会执行 scheduleVsyncLocked 订阅屏幕信号
    }
      //添加订阅者callback回调  eg:view绘制调用
     public void postCallback(int callbackType, Runnable action, Object token) {
        postCallbackDelayed(callbackType, action, token, 0);;//最终会执行 scheduleVsyncLocked 订阅屏幕信号
    }
    
    @UnsupportedAppUsage
    private void scheduleVsyncLocked() {
        mDisplayEventReceiver.scheduleVsync();
    }
    
    
    //移除订阅者callback回调
    public void removeFrameCallback(FrameCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        removeCallbacksInternal(CALLBACK_ANIMATION, callback, FRAME_CALLBACK_TOKEN);
    }
}
```

#### 3：应用：

##### View 绘制  使用

```java
public final class ViewRootImpl implements ViewParent,        View.AttachInfo.Callbacks,ThreadedRenderer.DrawCallbacks {
    //请求View重新测量绘制
    @Override
    public void requestLayout() {
        if (!mHandlingLayoutInLayoutRequest) {
            checkThread();
            mLayoutRequested = true;
            scheduleTraversals();
        }
    }
    
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            //
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            //同屏幕刷新同步信号建立联系
            mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            //.....
        }
    }
    
     final TraversalRunnable mTraversalRunnable = new TraversalRunnable();
    
     final class TraversalRunnable implements Runnable {
        @Override
        public void run() {
            //同步信号回来时执行
            doTraversal();
        }
    }
    
    void doTraversal() {
        if (mTraversalScheduled) {
           ...
           //执行测量绘制流程
            performTraversals();

           ...
        }
    }
}
```

面试常问，requestLayout 是立即执行的吗？目前我理解不是，需要等待屏幕同步信号到来。

也可以利用此原理来监测屏幕掉帧情况，待搞

