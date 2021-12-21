**1.使用new创建对象**

**2.通过clone的方式**

**3.通过反射的方式**

**4.通过反序列化的方式**

一.**使用new创建对象**

　　使用new会增加耦合度，所以要尽量减少使用new的频率。并且new是使用**强引用**方式来创建对象的。

```java
Hello hello = new Hello();
```

**二.使用反射的方式创建对象**

   1.使用Class类的newInstance方法来创建对象

```java
Class class = Class.forname("com.heyjia.test.Hello");
Hello hello = (Hello)class.newInstance();
```

   2.使用Constructor类的newInstance方法来创建兑现

```java
    Class class = Class.forName("com.heyjia.test.Hello");
    Constructor constructor = class.getConstructor();
    Hello hello = (Hello)constructor.newInstance();
```

三.**使用clone的方式创建对象**

  前提：需要有一个对象，使用该对象父类的clone方法可以创建一个内存大小跟它一样大的对象

深拷贝与浅拷贝

1：被拷贝对象 需要实现Cloneable 接口

2：重写 Object中的clone

​    native 层执行clone 操作

​    这个只是浅拷贝 只拷贝当前对象的非引用的属性变量，对于引用变量，不会创建新的对象，只是引用了引用对象

3：深拷贝对引用变量在重写Clone变量中执行Clone操作

```java
  // BEGIN Android-changed: Use native local helper for clone()
    // Checks whether cloning is allowed before calling native local helper.
    // protected native Object clone() throws CloneNotSupportedException;
    protected Object clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() +
                                                 " doesn't implement Cloneable");
        }

        return internalClone();
    }

    /*
     * Native helper method for cloning.
     */
    @FastNative
    private native Object internalClone();
    // END Android-changed: Use native local helper for clone()

```



```java
 /**
     * java 对象深拷贝
     * java 对象浅拷贝
     */
    public class classCloneTest{
        public void cloneTest(){
            //克隆方式创建
            AnimalFamily animalFamilyParent = new AnimalFamily();
            System.out.println("parent class info = "+ animalFamilyParent.toString());
            try{
                AnimalFamily animalFamilychildren = (AnimalFamily) animalFamilyParent.clone();
                System.out.println("children class info = "+ animalFamilychildren.toString());
            }catch (Exception e){
                System.out.println("children class clone error = "+e.toString());
            }
        }
        class AnimalFamily implements Cloneable{
            String familyName = "one by";
            private Dog mDog = new Dog();

            @Override
            public String toString() {
                return "AnimalFamily{" +
                        "familyName='" + familyName + '\'' +
                        ", mDog=" + mDog +
                        '}';
            }

            @NonNull
            @Override
            protected Object clone() throws CloneNotSupportedException {
                mDog = (Dog) mDog.clone();
                return super.clone();
            }
        }

        class Dog implements Cloneable{
            String name = "max";

            @NonNull
            @Override
            protected Object clone() throws CloneNotSupportedException {
                return super.clone();
            }
        }
    }
```

四.**使用反序列化的方式创建对象**

  在通过实现序列化serializable接口将对象存到硬盘中，通过反序列化可以获取改对象

```java
public class Serialize
{
    public static void main(String[] args)
    {
        Hello h = new Hello();

        //准备一个文件用于存储该对象的信息
        File f = new File("hello.obj");

        try(FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis)
            )
        {
            //序列化对象，写入到磁盘中
            oos.writeObject(h);
            //反序列化对象
            Hello newHello = (Hello)ois.readObject();

            //测试方法
            newHello.sayWorld();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }
}
```