1、wait()、notify/notifyAll() 方法是Object的本地final方法，无法被重写。

2、wait()使当前线程阻塞，前提是 必须先获得锁，一般配合synchronized 关键字使用，即，一般在synchronized 同步代码块里使用 wait()、notify/notifyAll() 方法。

3、 由于 wait()、notify/notifyAll() 在synchronized 代码块执行，说明当前线程一定是获取了锁的。

当线程执行wait()方法时候，会释放当前的锁，然后让出CPU，进入等待状态。

只有当 notify/notifyAll() 被执行时候，才会唤醒一个或多个正处于等待状态的线程，然后继续往下执行，直到执行完synchronized 代码块的代码或是中途遇到wait() ，再次释放锁。

也就是说，notify/notifyAll() 的执行只是唤醒沉睡的线程，而不会立即释放锁，锁的释放要看代码块的具体执行情况。所以在编程中，尽量在使用了notify/notifyAll() 后立即退出临界区，以唤醒其他线程让其获得锁

4、wait() 需要被try catch包围，以便发生异常中断也可以使wait等待的线程唤醒。

5、notify 和wait 的顺序不能错，如果A线程先执行notify方法，B线程在执行wait方法，那么B线程是无法被唤醒的。

6、notify 和 notifyAll的区别

notify方法只唤醒一个等待（对象的）线程并使该线程开始执行。所以如果有多个线程等待一个对象，这个方法只会唤醒其中一个线程，选择哪个线程取决于操作系统对多线程管理的实现。notifyAll 会唤醒所有等待(对象的)线程，尽管哪一个线程将会第一个处理取决于操作系统的实现。如果当前情况下有多个线程需要被唤醒，推荐使用notifyAll 方法。比如在生产者-消费者里面的使用，每次都需要唤醒所有的消费者或是生产者，以判断程序是否可以继续往下执行。

7、在多线程中要测试某个条件的变化，使用if 还是while？

　　要注意，notify唤醒沉睡的线程后，线程会接着上次的执行继续往下执行。所以在进行条件判断时候，可以先把 wait 语句忽略不计来进行考虑；显然，要确保程序一定要执行，并且要保证程序直到满足一定的条件再执行，要使用while进行等待，直到满足条件才继续往下执行。如下代码：

[![复制代码](../../../../art/notify%E5%92%8CnotifyAll%E5%92%8Cwait/copycode.gif)](javascript:void(0);)

```
 1 public class K {
 2     //状态锁
 3     private Object lock;
 4     //条件变量
 5     private int now,need;
 6     public void produce(int num){
 7         //同步
 8         synchronized (lock){
 9            //当前有的不满足需要，进行等待，直到满足条件
10             while(now < need){
11                 try {
12                     //等待阻塞
13                     lock.wait();
14                 } catch (InterruptedException e) {
15                     e.printStackTrace();
16                 }
17                 System.out.println("我被唤醒了！");
18             }
19            // 做其他的事情
20         }
21     }
22 }
23             
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

显然，只有当前值满足需要值的时候，线程才可以往下执行，所以，必须使用while 循环阻塞。注意，wait() 当被唤醒时候，只是让while循环继续往下走.如果此处用if的话，意味着if继续往下走，会跳出if语句块。

8、实现生产者和消费者问题 

　　什么是生产者-消费者问题呢？[![img](../../../../art/notify%E5%92%8CnotifyAll%E5%92%8Cwait/834666-20171006132515411-1581950806.jpg)](https://images2017.cnblogs.com/blog/834666/201710/834666-20171006132515411-1581950806.jpg)

　　如上图，假设有一个公共的容量有限的池子，有两种人，一种是生产者，另一种是消费者。需要满足如下条件：

　　　　1、生产者产生资源往池子里添加，前提是池子没有满，如果池子满了，则生产者暂停生产，直到自己的生成能放下池子。

　　　　2、消费者消耗池子里的资源，前提是池子的资源不为空，否则消费者暂停消耗，进入等待直到池子里有资源数满足自己的需求。

　　- 仓库类

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 import java.util.LinkedList;
 2 
 3 /**
 4  *  生产者和消费者的问题
 5  *  wait、notify/notifyAll() 实现
 6  */
 7 public class Storage1 implements AbstractStorage {
 8     //仓库最大容量
 9     private final int MAX_SIZE = 100;
10     //仓库存储的载体
11     private LinkedList list = new LinkedList();
12 
13     //生产产品
14     public void produce(int num){
15         //同步
16         synchronized (list){
17             //仓库剩余的容量不足以存放即将要生产的数量，暂停生产
18             while(list.size()+num > MAX_SIZE){
19                 System.out.println("【要生产的产品数量】:" + num + "\t【库存量】:"
20                         + list.size() + "\t暂时不能执行生产任务!");
21 
22                 try {
23                     //条件不满足，生产阻塞
24                     list.wait();
25                 } catch (InterruptedException e) {
26                     e.printStackTrace();
27                 }
28             }
29 
30             for(int i=0;i<num;i++){
31                 list.add(new Object());
32             }
33 
34             System.out.println("【已经生产产品数】:" + num + "\t【现仓储量为】:" + list.size());
35 
36             list.notifyAll();
37         }
38     }
39 
40     //消费产品
41     public void consume(int num){
42         synchronized (list){
43 
44             //不满足消费条件
45             while(num > list.size()){
46                 System.out.println("【要消费的产品数量】:" + num + "\t【库存量】:"
47                         + list.size() + "\t暂时不能执行生产任务!");
48 
49                 try {
50                     list.wait();
51                 } catch (InterruptedException e) {
52                     e.printStackTrace();
53                 }
54             }
55 
56             //消费条件满足，开始消费
57             for(int i=0;i<num;i++){
58                 list.remove();
59             }
60 
61             System.out.println("【已经消费产品数】:" + num + "\t【现仓储量为】:" + list.size());
62 
63             list.notifyAll();
64         }
65     }
66 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　- 抽象仓库类

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
1 public interface AbstractStorage {
2     void consume(int num);
3     void produce(int num);
4 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　- 生产者

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 public class Producer extends Thread{
 2     //每次生产的数量
 3     private int num ;
 4 
 5     //所属的仓库
 6     public AbstractStorage abstractStorage;
 7 
 8     public Producer(AbstractStorage abstractStorage){
 9         this.abstractStorage = abstractStorage;
10     }
11 
12     public void setNum(int num){
13         this.num = num;
14     }
15 
16     // 线程run函数
17     @Override
18     public void run()
19     {
20         produce(num);
21     }
22 
23     // 调用仓库Storage的生产函数
24     public void produce(int num)
25     {
26         abstractStorage.produce(num);
27     }
28 }
```

[![复制代码](../../../../art/notify%E5%92%8CnotifyAll%E5%92%8Cwait/copycode.gif)](javascript:void(0);)

　　- 消费者

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 public class Consumer extends Thread{
 2     // 每次消费的产品数量
 3     private int num;
 4 
 5     // 所在放置的仓库
 6     private AbstractStorage abstractStorage1;
 7 
 8     // 构造函数，设置仓库
 9     public Consumer(AbstractStorage abstractStorage1)
10     {
11         this.abstractStorage1 = abstractStorage1;
12     }
13 
14     // 线程run函数
15     public void run()
16     {
17         consume(num);
18     }
19 
20     // 调用仓库Storage的生产函数
21     public void consume(int num)
22     {
23         abstractStorage1.consume(num);
24     }
25 
26     public void setNum(int num){
27         this.num = num;
28     }
29 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　- 测试

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
 1 public class Test{
 2     public static void main(String[] args) {
 3         // 仓库对象
 4         AbstractStorage abstractStorage = new Storage1();
 5 
 6         // 生产者对象
 7         Producer p1 = new Producer(abstractStorage);
 8         Producer p2 = new Producer(abstractStorage);
 9         Producer p3 = new Producer(abstractStorage);
10         Producer p4 = new Producer(abstractStorage);
11         Producer p5 = new Producer(abstractStorage);
12         Producer p6 = new Producer(abstractStorage);
13         Producer p7 = new Producer(abstractStorage);
14 
15         // 消费者对象
16         Consumer c1 = new Consumer(abstractStorage);
17         Consumer c2 = new Consumer(abstractStorage);
18         Consumer c3 = new Consumer(abstractStorage);
19 
20         // 设置生产者产品生产数量
21         p1.setNum(10);
22         p2.setNum(10);
23         p3.setNum(10);
24         p4.setNum(10);
25         p5.setNum(10);
26         p6.setNum(10);
27         p7.setNum(80);
28 
29         // 设置消费者产品消费数量
30         c1.setNum(50);
31         c2.setNum(20);
32         c3.setNum(30);
33 
34         // 线程开始执行
35         c1.start();
36         c2.start();
37         c3.start();
38 
39         p1.start();
40         p2.start();
41         p3.start();
42         p4.start();
43         p5.start();
44         p6.start();
45         p7.start();
46     }
47 }
```

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

　　- 输出

[![复制代码](https://common.cnblogs.com/images/copycode.gif)](javascript:void(0);)

```
【要消费的产品数量】:50    【库存量】:0    暂时不能执行生产任务!
【要消费的产品数量】:20    【库存量】:0    暂时不能执行生产任务!
【要消费的产品数量】:30    【库存量】:0    暂时不能执行生产任务!
【已经生产产品数】:10    【现仓储量为】:10
【要消费的产品数量】:30    【库存量】:10    暂时不能执行生产任务!
【要消费的产品数量】:20    【库存量】:10    暂时不能执行生产任务!
【要消费的产品数量】:50    【库存量】:10    暂时不能执行生产任务!
【已经生产产品数】:10    【现仓储量为】:20
【已经生产产品数】:10    【现仓储量为】:30
【要消费的产品数量】:50    【库存量】:30    暂时不能执行生产任务!
【已经消费产品数】:20    【现仓储量为】:10
【要消费的产品数量】:30    【库存量】:10    暂时不能执行生产任务!
【已经生产产品数】:10    【现仓储量为】:20
【要消费的产品数量】:50    【库存量】:20    暂时不能执行生产任务!
【要消费的产品数量】:30    【库存量】:20    暂时不能执行生产任务!
【已经生产产品数】:10    【现仓储量为】:30
【已经消费产品数】:30    【现仓储量为】:0
【要消费的产品数量】:50    【库存量】:0    暂时不能执行生产任务!
【已经生产产品数】:10    【现仓储量为】:10
【要消费的产品数量】:50    【库存量】:10    暂时不能执行生产任务!
【已经生产产品数】:80    【现仓储量为】:90
【已经消费产品数】:50    【现仓储量为】:40
```

