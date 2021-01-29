Handler 整体机制：

Loop 循环查询

MessageQueue 队列存储 Message消息

Message 是任务的载体

Handler 充当任务Message 中的target 属性变量  最终 会被Looper充MessageQueue中取出 执行Handler Runable方法或者CallBack方法

1：Loop创建



```java
//第一步app 启动时创建Loop
//a:App启动：唯一一个java Main方法入口
//ActivityThread.java （UI 线程）
public static void main(String[] args) {
    	...
        //app启动创建Loop
        Looper.prepareMainLooper();

        ...
      //创建ActivityThread 
        ActivityThread thread = new ActivityThread();
        ...
        //Loop开始循环
        Looper.loop();

        throw new RuntimeException("Main thread loop unexpectedly exited");
    }

//Looper.prepareMainLooper()
//Looper.java
public static void prepareMainLooper() {
    prepare(false);
    synchronized (Looper.class) {
        if (sMainLooper != null) {
            throw new IllegalStateException("The main Looper has already been prepared.");
        }
        sMainLooper = myLooper();
    }
}

private static void prepare(boolean quitAllowed) {
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    //threadLocal 设置当前线程的变量
    //后面当前线程获取Looper 会一直是一个对象
    sThreadLocal.set(new Looper(quitAllowed));
}

private Looper(boolean quitAllowed) {
      //创建出MessageQueue
        mQueue = new MessageQueue(quitAllowed);
    //获取当前线程
        mThread = Thread.currentThread();
    }

```

2：Loop创建完成后 开启Loop队列检查

```java
// Looper.loop();
public static void loop() {
        //基于ThreadLocal 获取的Looper 是唯一对象
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread.");
        }
    	//拿到Lopper 创建是所创建的MessageQueue
        final MessageQueue queue = me.mQueue;
		....
         //开启无线循环 不会阻塞主线程，我理解得益于 queue.next()
        for (;;) {
            //queue.next() 消息队列没有数据的情况下 不会执行下面，当有消息触发时 会继续执行
            Message msg = queue.next(); // might block
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }
			.....
            try {
                //message中的target 就是Handler 开始分发处理消息
                msg.target.dispatchMessage(msg);
                dispatchEnd = needEndTime ? SystemClock.uptimeMillis() : 0;
            } finally {
                if (traceTag != 0) {
                    Trace.traceEnd(traceTag);
                }
            }
            .....

            msg.recycleUnchecked();
        }
    }

//阻塞queue.next() 获取Message 消息
//重点在于 nativePollOnce  nativePollOnce 相当于 Object.wait 都不会浪费 CPU 周期
Message next() {
        // Return here if the message loop has already quit and been disposed.
        // This can happen if the application tries to restart a looper after quit
        // which is not supported.
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }

        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }
			//nativePollOnce 
            //****该方法将一直阻塞直到添加新消息为止. 此时,您可能会问nativePollOnce 如何知道何时醒来. 这是一个很好的问题. 当将 Message 添加到队列时, 框架调用 enqueueMessage 方法, 该方法不仅将消息插入队列, 而且还会调用native static void nativeWake(long). nativePollOnce 和 nativeWake 的核心魔术发生在 native 代码中. native MessageQueue 利用名为 epoll 的 Linux 系统调用, 该系统调用可以监视文件描述符中的 IO 事件. nativePollOnce 在某个文件描述符上调用 epoll_wait, 而 nativeWake 写入一个 IO 操作到描述符, epoll_wait 等待. 然后, 内核从等待状态中取出 epoll 等待线程, 并且该线程继续处理新消息. 如果您熟悉 Java 的 Object.wait()和 Object.notify()方法,可以想象一下 nativePollOnce 大致等同于 Object.wait(), nativeWake 等同于 Object.notify(),但它们的实现完全不同: nativePollOnce 使用 epoll, 而 Object.wait 使用 futex Linux 调用. 值得注意的是, nativePollOnce 和 Object.wait 都不会浪费 CPU 周期, 因为当线程进入任一方法时, 出于线程调度的目的, 该线程将被禁用(引用Object类的javadoc). 但是, 某些事件探查器可能会错误地将等待 epoll 等待(甚至是 Object.wait)的线程识别为正在运行并消耗 CPU 时间, 这是不正确的. 如果这些方法实际上浪费了 CPU 周期, 则所有空闲的应用程序都将使用 100％ 的 CPU, 从而加热并降低设备速度.
            //*****
            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }

                // Process the quit message now that all pending messages have been handled.
                if (mQuitting) {
                    dispose();
                    return null;
                }

                // If first time idle, then get the number of idlers to run.
                // Idle handles only run if the queue is empty or if the first message
                // in the queue (possibly a barrier) is due to be handled in the future.
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }

                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }

            // Run the idle handlers.
            // We only ever reach this code block during the first iteration.
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler

                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }

                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }

            // Reset the idle handler count to 0 so we do not run them again.
            pendingIdleHandlerCount = 0;

            // While calling an idle handler, a new message could have been delivered
            // so go back and look again for a pending message without waiting.
            nextPollTimeoutMillis = 0;
        }
    }


	//塞入消息message
    //重点在于 nativeWake(mPtr); nativeWake 相当于 java notify 或者 notifyAll
 boolean enqueueMessage(Message msg, long when) {
        if (msg.target == null) {
            throw new IllegalArgumentException("Message must have a target.");
        }
        if (msg.isInUse()) {
            throw new IllegalStateException(msg + " This message is already in use.");
        }

        synchronized (this) {
            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(
                        msg.target + " sending message to a Handler on a dead thread");
                Log.w(TAG, e.getMessage(), e);
                msg.recycle();
                return false;
            }

            msg.markInUse();
            msg.when = when;
            Message p = mMessages;
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                // New head, wake up the event queue if blocked.
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked;
            } else {
                // Inserted within the middle of the queue.  Usually we don't have to wake
                // up the event queue unless there is a barrier at the head of the queue
                // and the message is the earliest asynchronous message in the queue.
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }

            // We can assume mPtr != 0 because mQuitting is false.
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }

//nativePollOnce 挂起 和 nativeWake 只是用了Linux 的调度  Message 还是存放于Message

```



3：MessageQueue 中Message 是如何存放的    定时是如何做的

```java
//MessageQueue 中 Message 存储是使用链表存储的
//message 类信息
public final class Message implements Parcelable {
  @UnsupportedAppUsage
    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
    public long when; //触发的目标时间

    /*package*/ Bundle data;

    @UnsupportedAppUsage
    /*package*/ Handler target; //handler 引用  用于通知handler 触发

    @UnsupportedAppUsage
    /*package*/ Runnable callback;

    // sometimes we store linked lists of these things
    @UnsupportedAppUsage
    /*package*/ Message next;  //链表存储message  达到messageQueue的状态
}

//messageQueue 添加的时候会通过 when 目标时间来排列messageQueue 中msg的顺序  （按时间排列的）
  boolean enqueueMessage(Message msg, long when) {
           ...
            msg.when = when;
            Message p = mMessages;//链表头
            boolean needWake;
            if (p == null || when == 0 || when < p.when) {
                //时间小于链表头  创建新的表头
                msg.next = p;
                mMessages = msg;
                needWake = mBlocked; 
            } else {
                //时间大于表头  在后面链表中寻找合适位置
                needWake = mBlocked && p.target == null && msg.isAsynchronous();
                Message prev;
                for (;;) {
                    prev = p;
                    p = p.next;
                    if (p == null || when < p.when) {
                        break;
                    }
                    if (needWake && p.isAsynchronous()) {
                        needWake = false;
                    }
                }
                msg.next = p; // invariant: p == prev.next
                prev.next = msg;
            }
            if (needWake) {
                nativeWake(mPtr);
            }
        }
        return true;
    }

//定时任务 nativePollOnce(ptr, nextPollTimeoutMillis);

 Message next() {
        // Return here if the message loop has already quit and been disposed.
        // This can happen if the application tries to restart a looper after quit
        // which is not supported.
        final long ptr = mPtr;
        if (ptr == 0) {
            return null;
        }

        int pendingIdleHandlerCount = -1; // -1 only during first iteration
        int nextPollTimeoutMillis = 0;
        for (;;) {
            if (nextPollTimeoutMillis != 0) {
                Binder.flushPendingCommands();
            }

            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {
                // Try to retrieve the next message.  Return if found.
                final long now = SystemClock.uptimeMillis();
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {
                    // Stalled by a barrier.  Find the next asynchronous message in the queue.
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());
                }
                if (msg != null) {
                    if (now < msg.when) {
                        // Next message is not ready.  Set a timeout to wake up when it is ready.
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        // Got a message.
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // No more messages.
                    nextPollTimeoutMillis = -1;
                }

                // Process the quit message now that all pending messages have been handled.
                if (mQuitting) {
                    dispose();
                    return null;
                }

                // If first time idle, then get the number of idlers to run.
                // Idle handles only run if the queue is empty or if the first message
                // in the queue (possibly a barrier) is due to be handled in the future.
                if (pendingIdleHandlerCount < 0
                        && (mMessages == null || now < mMessages.when)) {
                    pendingIdleHandlerCount = mIdleHandlers.size();
                }
                if (pendingIdleHandlerCount <= 0) {
                    // No idle handlers to run.  Loop and wait some more.
                    mBlocked = true;
                    continue;
                }

                if (mPendingIdleHandlers == null) {
                    mPendingIdleHandlers = new IdleHandler[Math.max(pendingIdleHandlerCount, 4)];
                }
                mPendingIdleHandlers = mIdleHandlers.toArray(mPendingIdleHandlers);
            }

            // Run the idle handlers.
            // We only ever reach this code block during the first iteration.
            for (int i = 0; i < pendingIdleHandlerCount; i++) {
                final IdleHandler idler = mPendingIdleHandlers[i];
                mPendingIdleHandlers[i] = null; // release the reference to the handler

                boolean keep = false;
                try {
                    keep = idler.queueIdle();
                } catch (Throwable t) {
                    Log.wtf(TAG, "IdleHandler threw exception", t);
                }

                if (!keep) {
                    synchronized (this) {
                        mIdleHandlers.remove(idler);
                    }
                }
            }

            // Reset the idle handler count to 0 so we do not run them again.
            pendingIdleHandlerCount = 0;

            // While calling an idle handler, a new message could have been delivered
            // so go back and look again for a pending message without waiting.
            nextPollTimeoutMillis = 0;
        }
    }
```



3：Handler 如何发送消息

```java
 //handler 创建的时候会获取当前线程TheaderLocal的变量Looper  并且把looper 中的MessageQueue 拿到
 //通过  sendEmptyMessageDelayed 方法 ->sendMessageDelayed ->sendMessageAtTime->enqueueMessage
//最终通过 messageQueue中的enqueueMessage 塞入 消息
//此过程中也会把当前handler 引用传递给Mesage 的target ，提供最终的调用mssage 中target 也就是handler 进行回
//调处理
//eg
Handler handles = new Handler()
handles.postDelayed(new Runable(){
      ...
    },5000)

    public Handler() {
    this(null, false);
}
public Handler(@Nullable Callback callback, boolean async) {
    if (FIND_POTENTIAL_LEAKS) {
        final Class<? extends Handler> klass = getClass();
        if ((klass.isAnonymousClass() || klass.isMemberClass() || klass.isLocalClass()) &&
            (klass.getModifiers() & Modifier.STATIC) == 0) {
            Log.w(TAG, "The following Handler class should be static or leaks might occur: " +
                  klass.getCanonicalName());
        }
    }

    mLooper = Looper.myLooper();
    if (mLooper == null) {
        throw new RuntimeException(
            "Can't create handler inside thread " + Thread.currentThread()
            + " that has not called Looper.prepare()");
    }
    mQueue = mLooper.mQueue;
    mCallback = callback;
    mAsynchronous = async;
}

public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
    Message msg = Message.obtain();
    msg.what = what;
    return sendMessageDelayed(msg, delayMillis);
}
public final boolean sendMessageDelayed(@NonNull Message msg, long delayMillis) {
    if (delayMillis < 0) {
        delayMillis = 0;
    }
    return sendMessageAtTime(msg, SystemClock.uptimeMillis() + delayMillis);
}
public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
    MessageQueue queue = mQueue;
    if (queue == null) {
        RuntimeException e = new RuntimeException(
            this + " sendMessageAtTime() called with no mQueue");
        Log.w("Looper", e.getMessage(), e);
        return false;
    }
    return enqueueMessage(queue, msg, uptimeMillis);
}

private boolean enqueueMessage(@NonNull MessageQueue queue, @NonNull Message msg,
                               long uptimeMillis) {
    msg.target = this;
    msg.workSourceUid = ThreadLocalWorkSource.getUid();

    if (mAsynchronous) {
        msg.setAsynchronous(true);
    }
    return queue.enqueueMessage(msg, uptimeMillis);
}
```

