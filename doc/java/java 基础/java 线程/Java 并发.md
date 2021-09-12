# 1：死锁 条件

syncnized造成死锁的情况

![image-20210905221924141](art/image-20210905221924141.png)

如何避免

使用：lock锁  

尝试拿锁机制

![image-20210905222045033](art/image-20210905222045033.png)

如果没有 Thread.sleep(r.nextInt(3))  会造成“活锁”

![image-20210905223538836](art/image-20210905223538836.png)



# 2：ThreadLocal

![image-20210905224150441](art/image-20210905224150441.png)



![image-20210905225432372](art/image-20210905225432372.png)



# 3：CAS

关键 CPU 提供的CAS指令

![image-20210905225800498](art/image-20210905225800498.png)

使用：

![image-20210905231549709](art/image-20210905231549709.png)

![image-20210905231621328](art/image-20210905231621328.png)



![image-20210905230929728](art/image-20210905230929728.png)

### a:CAS ABA问题：

![image-20210905231211679](art/image-20210905231211679.png)

解决问题：添加版本

![image-20210905231832630](art/image-20210905231832630.png)



AtomicMarkableReference 和 AtomicStampedReference 区别

AtomicMarkableReference 只关心 版本

AtomicStampedReference 不仅关心版本还关心更改了几次



b:开销问题：



c:只能保证一个共享变量的原子操作



# 4：阻塞队列



![image-20210905233816046](art/image-20210905233816046.png)

解决生产者消费者问题



![image-20210905234125924](art/image-20210905234125924.png)

