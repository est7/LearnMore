# 面向切面编程 横向切面

针对业务中重复或者相同的业务场景进行 横向抽象（横向切面）

eg：

```java
//android application 中监控所有Activity的声明周期
registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
    .....
    .....
    .....
    .....
    
});
```

Eg:

![image-20200626225158226](AOP面向切面架构设计.assets/image-20200626225158226.png)

Eg：

动态代理切面需求：

![image-20200626225431007](AOP面向切面架构设计.assets/image-20200626225431007.png)



```java
public interface DBOperation{
	void insert();
	void delete();
	void update();
  //数据备份
	void save();
}
```

```java
public class MainAction implements DBOperation{
  
  private DBOperation db;
  
  public void main(){
    db = Proxy.newProxyInstance(DBOperation.class.getClassLoader(),new DBHandler(this));
  	db.delete();
  }
  
  class DBHandler implements InvocationHandler{
    private DBOperation db;
    public DBHandler(DBOperation db){
      this.db = db;
    }
    @Override
    public Object invoke(Object proxy,Method method,Object[] args) throws Throwable{
      if(db != null){
        save();//数据备份完成
        return method.invoke(this,args)
      }
      return null;
    }
  }
  
  @Override
  public void insert(){
    
  }
  @Override
  public void delete(){
   
  }
  @Override
  public void upadate(){
    
  }
  @Override
  public void save(){
    
  }
}
```

# 面向切面思想之集中式登录架构设计 （预编译）

![image-20200626231854605](AOP面向切面架构设计.assets/image-20200626231854605.png)

![image-20200626231942957](AOP面向切面架构设计.assets/image-20200626231942957.png)



![image-20200626232042897](AOP面向切面架构设计.assets/image-20200626232042897.png)

# AspectJ  切面框架  



java ----->calss         javac

  AspectJ   定制的javac编译器



```java
//版本界限 AS-3.0.1+gradle4.4-all (需要配置r17ndk 环境)
//As-3.2.1+gradle4.6-all (正常使用 没有警告)
classpath 'org.aspectj:aspectjtools:1.8.9'
classpath 'org.aspectj:aspectjweaver:1.8.9'
```



```java
//app gralde 修改

buildsrcipt{//编译器利用Aspect 专门的编译器 不再使用传统的javac
  repositories{
    mavenCentral()
  }
  dependencies{
    classpath 'org.aspectj:aspectjtools:1.8.9'
		classpath 'org.aspectj:aspectjweaver:1.8.9'
  }
}
```

