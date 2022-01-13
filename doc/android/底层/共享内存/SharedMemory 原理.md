## 简介

Ashmem即Android Shared Memory, 是Android提供的一种内存共享的机制。

## 使用

- Java层借助`MemoryFile`或者`SharedMemory`。
- Native层借助`MemoryHeapBase`或者`MemoryBase`。
- Native层直接调用libc的`ashmem_create_region`和`mmap`系统调用。

> `MemoryFile`基于`SharedMemory`。 `MemoryBase`基于`MemoryHeapBase`。 `SharedMemory`、`MemoryHeapBase`都是基于`ashmem_create_region/mmap`。

### MemoryFile

MemoryFile是对SharedMemory的包装，官方推荐直接使用SharedMemory。

> Applications should generally prefer to use {@link SharedMemory} which offers more flexible access & control over the shared memory region than MemoryFile does.

### SharedMemory

`SharedMemory`只能通过调用`SharedMemory.create`静态方法或者通过Parcel反序列化的方式进行创建。 `SharedMemory`的创建进程通过`SharedMemory.create`创建，使用进程通过Parcel反序列化创建。

因为`SharedMemory`类实现了`Parcelable`，所以可以通过binder跨进程传输。

### MemoryBase和MemoryHeapBase

`MemoryBase`是对`MemoryHeapBase`的包装。`MemoryHeapBase`对应一块共享内存，使用`ashmem_create_region/mmap`创建，`MemoryHeapBase`内部保存了共享内存的地址和大小。通过`MemoryBase`可以获取其包装的`MemoryHeapBase`。

`MemoryBase`和`MemoryHeapBase`都是Binder本地对象(BBinder)，可以直接传到其他进程。其他进程分别使用`IMemory`和`IMemoryHeap`进行跨进程调用。

`MemoryHeapBase`跨进程传输本质上传输的是共享内存的fd，fd在经过binder驱动时会被转换成目标进程的fd，`MemoryHeapBase`的客户端代理对象`BpMemoryHeap`在创建时候会将fd映射到自己的内存空间，这样客户端进程在使用`IMemoryHeap`接口获取到的内存地址就是自己进程空间的地址。

### ashmem_create_region 和 mmap

`ashmem_create_region`/`mmap`是SharedMemory和MemoryHeapBase的实现基础。

```
int ashmem_create_region(const char *name, size_t size)
复制代码
```

用于创建共享内存，函数内部首先通过`open`函数打开`/dev/ashmem`设备，得到文件描述符后，通过调用`ioctl`设置fd的名称和大小。

```
void *mmap(void *addr, size_t length, int prot, int flags, int fd, off_t offset);
复制代码
```

通过binder将fd传递到其他进程后，其他进程可以通过`mmap`系统调用，将共享内存映射到当前进程的地址空间，之后就可以通过返回的内存首地址进行内存读写。

这样，两个进程之间就实现了直接的内存共享，获得了极高的进程间通信效率。

### Ashmem的Pin和Unpin

ashmem驱动提供了两个用于内存管理的`ioctl`操作命令：`pin/unpin`，直接通过`ashmem_create_region`创建的共享内存默认是`pined`的状态，也就是说，应用程序不主动关闭共享内存fd的情况下，这篇内存会始终保留，直到进程死亡。

如果调用`unpin`将共享内存中的某段内存解除锁定，之后如果系统内存不足，会自动释放这部分内存，再次使用同一段内存前应该先执行pin操作，如果pin操作返回`ASHMEM_WAS_PURGED`，也就是说内存已经被回收，已经回收的内存再次访问会触发缺页中断重新进行物理内存的分配，因此这段内存里的数据已经不是起初的那个数据了，如果仍旧当做原始数据进行访问必然引发错误。

通过`pin/unpin`命令，配合`ashmem`驱动，可以进行简单的内存管理。

## 原理

Ashmem的核心原理主要是两部分：驱动和fd传递。

### 驱动

Ashmem是Linux内核中的一个misc设备，对应的设备文件是`/dev/ashmem`，此设备是一个虚拟设备，不存在实际文件，只在内核驱动中对应一个inode节点。Ashmem在驱动层是基于linux系统的共享内存功能实现的，Ashmem可以理解为只是对原生的共享内存进行了一层包装，使其更方便在Android系统上使用。

`ashmem`设备文件支持如下操作：

```
// /drivers/staging/android/ashmem.c
809static const struct file_operations ashmem_fops = {
810	.owner = THIS_MODULE,
811	.open = ashmem_open,
812	.release = ashmem_release,
813	.read = ashmem_read,
814	.llseek = ashmem_llseek,
815	.mmap = ashmem_mmap,
816	.unlocked_ioctl = ashmem_ioctl,
817#ifdef CONFIG_COMPAT
818	.compat_ioctl = compat_ashmem_ioctl,
819#endif
820};
复制代码
```

#### ashmem创建：（从Java层到驱动层的调用链）

```
[java] android.os.SharedMemory#create
[jni] /frameworks/base/core/jni/android_os_SharedMemory.cpp#SharedMemory_create
[libc] /system/core/libcutils/ashmem-dev.c#ashmem_create_region
[driver] /drivers/staging/android/ashmem.c#ashmem_open
复制代码
```

##### ashmem_open

`ashmem_open`中只是创建了一个标识`ashmem`的结构体，然后返回fd，并没有进行实际的内存分配（无论是虚拟内存还是物理内存）。 得到文件描述符后，就可以使用`ashmem_mmap`将内核中的共享内存区域映射到进程的虚拟地址空间。

##### ashmem_mmap

`ashmem_mmap`通过调用内核中`shmem`相关函数在[tempfs](https://link.juejin.cn?target=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FTmpfs)创建了一个大小等于创建`ashmem`时传入大小的临时文件（由于是内存文件，所以磁盘上不存在实际的文件）,然后将文件对应的内存映射到调用mmap的进程。（注意map的是临时文件而不是`ashmem`文件）

其中涉及到的`shmem`函数包括`shmem_file_setup`和`shmem_set_file`，他们为该临时文件创建inode节点，将文件关联到为该文件配的虚拟内存，同时为该文件设置自己的文件操作函数（Linux共享内存shmem的文件操作），并为虚拟内存设置缺页处理函数。这样后续对共享内存的操作就变为了对tempfs文件节点的操作。当首次访问共享内存时触发缺页中断处理函数并为该虚拟内存分配实际的物理内存。

> `tempfs`是Unix-like系统中一种基于内存的文件系统，具有极高的访问效率。
>  `shmem`是Linux自带的进程间通信机制：共享内存`Shared Memory`。
>  共享内存的虚拟文件记录在`/proc/<pid>/maps`文件中，pid表示打开这个共享内存文件的进程ID。

##### ashmem_pin/ashmem_unpin

`pin`和`unpin`是`ashmem`的`ioctl`支持的两个操作，用于共享内存的分块使用和分块回收，用于节省实际的物理内存。 新创建的共享内存默认都是pined的，当调用`unpin`时，驱动将unpined的内存区域所在的页挂在一个`unpinned_list`链表上，后续内存回收就是基于`unpinned_list`链表进行。

在`ashmem`驱动初始化函数`ashmem_init`里调用了内核函数`register_shrinker`，注册了一个内存回收回调函数`ashmem_shrink`，当系统内存紧张时，就会回调`ashmem_shrink`，由驱动自身进行适当的内存回收。驱动就是在`ashmem_shrink`中遍历`unpinned_list`进行内存回收，以释放物理内存。

### ashmem fd的传递：

fd通过Binder传递。

Binder机制不仅支持binder对象的传递，还支持文件描述符的传递。fd经过binder驱动时，binder驱动会将源进程的fd转换成目标进程的fd，转换过程为：取出发送方binder数据里的fd，通过fd找到文件对象，然后为目标进程创建fd，将目标进程fd和文件对象进行关联，将发送方binder数据里的fd改为目标进程的fd，然后将数据发送给目标进程。这个过程相当于文件在目标进程又打开了一次，目标进程使用的是自己的fd，但和源进程都指向的是同一个文件。这样源进程和目标进程就都可以map到同一片内存了。

## 使用场景

- 进程间共享体积较大的数据，比如bitmap。
- 提升进程间传输数据的效率，比如ContentProvider基于共享内存进行数据传送。
- 借助Bitmap解码的inPurgeable属性，在android4.x及以下系统版本中实现内存在ashmem中分配，以节省Java堆内存。比如fresco图片加载库针对Android4.x及以下的机型对inPurgeable属性的使用。

## 总结

`Ashmem`通过对Linux共享内存的扩展，一方面使其使用更简单，另一方面使其只能通过binder传递，增加了安全性。`Ashmem`在Android系统中起着非常重要的作用，比如整个显示系统App-WMS-SurfaceFlinger之间就是通过`Ashmem`传递的帧数据。