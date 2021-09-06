1：ThreadLocal  提供线程的局部变量

使用：

1：在线程中创建ThreadLocal 泛型声明要存储的变量类型

2：threaLocal .set 设置存储数据

3：注意一个线程可以创建多个ThreadLocal

存储数据结构为

Thread

​	|---ThreadLoacMap(key = 当前ThreadLocal的hasCode ==>)

​		|--Entry[] table

​			|--Entry {

​							key----当前threadlocal 对象的hascode

​                             value----当前存入泛型

​						}

原理：

1：在ThreadLocal第一个set数据时，会给当前线程的 ThreadLocalMap 变量创建对应的ThreadLoaclMap对象

2：ThreadLocalMap 会使用当前 的ThreadLocal 为Key ,存储对象为Value 进行存储



 

ThreadLocal 在不使用的时候记得remove掉

1：Thread 中有局部变量

```java
/* ThreadLocal values pertaining to this thread. This map is maintained
     * by the ThreadLocal class. */
    ThreadLocal.ThreadLocalMap threadLocals = null;
```



2：ThreadLoack中的set get 最终得到的数据都是ThreadLocalMap 中产生的

```java
//set get 操作的都是 ThreadLocalMap  也就是Thread中的ThreadLocalMap  变量
//然后 key 是当前的ThreadLocal 对象   Value 为目标保存的对象
public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }

/**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```

3：ThreadLocalMap

```java
//比较重要的是Entry 弱引用存在  当ThreadLocal = null  时会被回收
 static class Entry extends WeakReference<ThreadLocal<?>> {
            /** The value associated with this ThreadLocal. */
            Object value;

            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

//Entry数组  存储了ThreadLocal  Key  和对应的Value  
private void set(ThreadLocal<?> key, Object value) {

            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.
			
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal<?> k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            tab[i] = new Entry(key, value);
            int sz = ++size;
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                rehash();
        }
        
          /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
```



在内存中的展示：





![1611834974836](art/1611834974836.png)











