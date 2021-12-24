# 一、异常原因

在使用aidl进行进程间通信时，有时候在客户端调用服务端的接口会抛出DeadObjectException异常，原因一般是由于某种原因服务端程序崩溃重启或者服务对象由于内存紧张被回收导致的，最近开发的时候遇到过此问题，解决方案有两种，实测有效。

# 二、解决方案如下两种方案

1. 方案一：针对应用开发，可以在服务端进程启动的时候发个消息给客户端，

  客户端收到消息的时候重新进行绑定操作，目的是为了同步客户端和服务端的连接，

  客户端进程启动的时候也要绑定一次（注：在已经连接的情况下，服务端由于某种原因进程重启了，如果客户端没有收到回调，客户端保存的连接不为空，这时调用服务端接口就会抛出DeadObjectException异常）

##### 2. 方案二：进行死亡监听

###### 1）在调用服务端接口的时候先进行判断bind是否还活着

```java
if (mIMyAidlInterface != null && mIMyAidlInterface.asBinder().isBinderAlive()) {
    try {
        mIMyAidlInterface.startRecord();
    } catch (Exception e) {
        Log.e(TAG, "Exception");
        e.printStackTrace();
    }
}

```

2）注册死亡代理

```java
private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {

    @Override
    public void binderDied() {                           
    // 当绑定的service异常断开连接后，自动执行此方法
        Log.e(TAG,"binderDied " );
        if (mIMyAidlInterface != null){
    // 当前绑定由于异常断开时，将当前死亡代理进行解绑        mIMyAidlInterface.asBinder().unlinkToDeath(mDeathRecipient, 0);
            //  重新绑定服务端的service
            bindService(new Intent("com.service.bind"),mMyServiceConnection,BIND_AUTO_CREATE);      
        }
    }
};

```

3）在service绑定成功后，调用linkToDeath（）注册进service，当service发生异常断开连接后会自动调用binderDied()

```java
public void onServiceConnected(ComponentName name, IBinder service) {          
    //绑定成功回调
    Log.d(TAG, "onServiceConnected");
    mIMyAidlInterface = IMyAidlInterface.Stub.asInterface(service);     
    //获取服务端提供的接口
    try {
    // 注册死亡代理
    if(mIMyAidlInterface != null){
    Log.d(TAG, mIMyAidlInterface.getName());
    service.linkToDeath(mDeathRecipient, 0); 
    }       
    } catch (RemoteException e) {
        e.printStackTrace();
    }
}

```

