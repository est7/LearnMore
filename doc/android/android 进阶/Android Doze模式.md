# Android中的Doze模式

Android 6.0引入的Doze机制在于节省系统耗电量，保护电池，延长电池的使用时间。当设备未连接至电源，且长时间处于闲置状态时，系统会将应用进入Doze，置于`App Standby`模式。而最终的效果，能提升30%的电量续航能力。

## Doze模式的状态

该状态与API版本无关，未适配API23以上的应用只要运行在6.0以上的系统上就会受到Doze模式的影响。

- 在屏幕熄灭30分钟、没有晃动并且在不充电的时候，会进入Doze模式
- 在进入Doze模式后，每间隔一段时间，会进入一段时长为30s的`maintenance window`的窗口期，可以唤醒系统，进行网络交互等等
- 进入Doze模式后，如果没有退出的话，系统唤醒的间隔时长会越来越长

![img](https://upload-images.jianshu.io/upload_images/1941624-8bcafda4d477b9b1.png?imageMogr2/auto-orient/strip|imageView2/2/w/554/format/webp)

Doze模式

当系统处于Doze模式下，系统和白名单之外的应用将受到以下限制：

- 无法访问网络
- Wake Locks被忽略
- AlarmManager闹铃会被推迟到下一个maintenance window响应
  - 使用`setAndAllowWhileIdle`或`SetExactAndAllowWhileIdle`设置闹铃的闹钟则不会受到Doze模式的影响
  - `setAlarmClock`设置的闹铃在Doze模式下仍然生效，但系统会在闹铃生效前退出Doze
- 系统不执行Wi-Fi/GPS扫描；
- 系统不允许同步适配器运行；
- 系统不允许JobScheduler运行；

而位于白名单中的应用可以：

- 继续使用网络并保留部分wake lock
- Job和同步仍然会被推迟
- 常规的AlarmManager闹铃也不会被触发

## 应用申请加入白名单

App可以通过`PowerManager.isIgnoringBatteryOptimizations`检查本App是否在系统的白名单列表中。

如果不在，则可以通过在`AndroidManifest.xml`中添加`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`权限，并且通过发送`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`的Intent来向用户申请该权限

## 原理

Doze的原理是在框架层对资源加入了一层资源的调度。在监听系统硬件或者屏幕亮暗的中断信号所发出来的广播，然后对于`JobScheduler`以及`AlarmManager`中任务进行统一调度。

而Doze的源码在于[链接](https://links.jianshu.com/go?to=http%3A%2F%2Fandroidxref.com%2F6.0.0_r1%2Fxref%2Fframeworks%2Fbase%2Fservices%2Fcore%2Fjava%2Fcom%2Fandroid%2Fserver%2FDeviceIdleController.java):
`/frameworks/base/services/core/java/com/android/server/DeviceIdleController.java`

在`DeviceIdleController`中存在一个`mState`变量来保存当前设备的状态，状态值如下：



```cpp
    /** Device is currently active. */
    private static final int STATE_ACTIVE = 0;
    /** Device is inactve (screen off, no motion) and we are waiting to for idle. */
    private static final int STATE_INACTIVE = 1;
    /** Device is past the initial inactive period, and waiting for the next idle period. */
    private static final int STATE_IDLE_PENDING = 2;
    /** Device is currently sensing motion. */
    private static final int STATE_SENSING = 3;
    /** Device is currently finding location (and may still be sensing). */
    private static final int STATE_LOCATING = 4;
    /** Device is in the idle state, trying to stay asleep as much as possible. */
    private static final int STATE_IDLE = 5;
    /** Device is in the idle state, but temporarily out of idle to do regular maintenance. */
    private static final int STATE_IDLE_MAINTENANCE = 6;
```

`DeviceIdleController`继承自`SystemService`，在SystemServer初始化的时候，会初始化该对象，并且将它添加到`ServiceManager`中

![img](https://upload-images.jianshu.io/upload_images/1941624-42ac21c919b455d3.png?imageMogr2/auto-orient/strip|imageView2/2/w/1200/format/webp)

DeviceIdleController

而在`onBootPhase`，即设备Boot初始化阶段，也就是所有的SystemService都初始化完毕后，`DeviceIdleController`会初始化需要用到的AlarmManager、LocationManager等，并且会调用`updateDisplayLoced`



```java
@Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_SYSTEM_SERVICES_READY) {
            synchronized (this) {
                mAlarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                mBatteryStats = BatteryStatsService.getService();
                mLocalPowerManager = getLocalService(PowerManagerInternal.class);
                mNetworkPolicyManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
                mDisplayManager = (DisplayManager) getContext().getSystemService(Context.DISPLAY_SERVICE);
                mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
                mSigMotionSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
                mLocationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
                mLocationRequest = new LocationRequest().setQuality(LocationRequest.ACCURACY_FINE).setInterval(0).setFastestInterval(0).setNumUpdates(1);
                mAnyMotionDetector = new AnyMotionDetector(
                        (PowerManager) getContext().getSystemService(Context.POWER_SERVICE),
                        mHandler, mSensorManager, this);
                Intent intent = new Intent(ACTION_STEP_IDLE_STATE)
                        .setPackage("android")
                        .setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                mAlarmIntent = PendingIntent.getBroadcast(getContext(), 0, intent, 0);

                Intent intentSensing = new Intent(ACTION_STEP_IDLE_STATE)
                        .setPackage("android")
                        .setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                mSensingAlarmIntent = PendingIntent.getBroadcast(getContext(), 0, intentSensing, 0);
                mIdleIntent = new Intent(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
                mIdleIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY| Intent.FLAG_RECEIVER_FOREGROUND);

                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                filter.addAction(ACTION_STEP_IDLE_STATE);
                getContext().registerReceiver(mReceiver, filter);

                mLocalPowerManager.setDeviceIdleWhitelist(mPowerSaveWhitelistAllAppIdArray);
                mDisplayManager.registerDisplayListener(mDisplayListener, null);
                updateDisplayLocked();
            }
        }
    }
```

而在`updateDisplayLocked`与`updateChargingLocked`函数中会判断当前屏幕是否亮着，或者是否在充电，如果屏幕熄灭或者没在充电的话，则会调用`becomeInactiveIfAppropriateLocked`开始准备进入Doze状态。
PS：后者是在收到`ACTION_BATTERY_CHANGED`的时候调用的，代表充电的变化



```java
void updateDisplayLocked() {
        mCurDisplay = mDisplayManager.getDisplay(Display.DEFAULT_DISPLAY);
        // We consider any situation where the display is showing something to be it on,
        // because if there is anything shown we are going to be updating it at some
        // frequency so can't be allowed to go into deep sleeps.
        boolean screenOn = mCurDisplay.getState() == Display.STATE_ON;
        if (DEBUG) Slog.d(TAG, "updateDisplayLocked: screenOn=" + screenOn);
        if (!screenOn && mScreenOn) {
            mScreenOn = false;
            if (!mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (screenOn) {
            mScreenOn = true;
            if (!mForceIdle) {
                becomeActiveLocked("screen", Process.myUid());
            }
        }
    }

    void updateChargingLocked(boolean charging) {
        if (DEBUG) Slog.i(TAG, "updateChargingLocked: charging=" + charging);
        if (!charging && mCharging) {
            mCharging = false;
            if (!mForceIdle) {
                becomeInactiveIfAppropriateLocked();
            }
        } else if (charging) {
            mCharging = charging;
            if (!mForceIdle) {
                becomeActiveLocked("charging", Process.myUid());
            }
        }
    }
```

在`becomeInactiveIfAppropriateLocked`函数中：

- 将状态设置成`STATE_INACTIVE`
- 取消定位、传感器监听的闹钟
- 重新设置`mInactiveTimeout`时长的闹钟，也就是30分钟或者3分钟
- 在闹钟的Intent中，会发送一个广播`ACTION_STEP_IDLE_STATE`



```cpp
void becomeInactiveIfAppropriateLocked() {
        if (DEBUG) Slog.d(TAG, "becomeInactiveIfAppropriateLocked()");
        if (((!mScreenOn && !mCharging) || mForceIdle) && mEnabled && mState == STATE_ACTIVE) {
            // Screen has turned off; we are now going to become inactive and start
            // waiting to see if we will ultimately go idle.
            mState = STATE_INACTIVE;
            if (DEBUG) Slog.d(TAG, "Moved from STATE_ACTIVE to STATE_INACTIVE");
            resetIdleManagementLocked();
            scheduleAlarmLocked(mInactiveTimeout, false);
            EventLogTags.writeDeviceIdle(mState, "no activity");
        }
    }

    void resetIdleManagementLocked() {
        mNextIdlePendingDelay = 0;
        mNextIdleDelay = 0;
        cancelAlarmLocked();
        cancelSensingAlarmLocked();
        cancelLocatingLocked();
        stopMonitoringSignificantMotion();
        mAnyMotionDetector.stop();
    }
```

在接收到`ACTION_STEP_IDLE_STATE`的广播后，会调用`stepIdleStateLocked`，在该函数中，处理所有的状态变化，而在状态处理的过程中还会有几个Alarm被设置。在该函数中，主要涉及一些状态变化，以及闹钟的设置，借图说明：

![img](https://upload-images.jianshu.io/upload_images/1941624-265763a32f583301.png?imageMogr2/auto-orient/strip|imageView2/2/w/1151/format/webp)

Doze状态变化



最终，在进入Doze模式后，会通过`mHandler`发送一个`MSG_REPORT_IDLE_ON`的消息，在该消息中，通过`mNetworkPolicyManager.setDeviceIdleMode`禁止网络连接，通过`PowerManager`来限制WakeLock



```csharp
case MSG_REPORT_IDLE_ON: {
                    EventLogTags.writeDeviceIdleOnStart();
                    mLocalPowerManager.setDeviceIdleMode(true);
                    try {
                        mNetworkPolicyManager.setDeviceIdleMode(true);
                        mBatteryStats.noteDeviceIdleMode(true, null, Process.myUid());
                    } catch (RemoteException e) {
                    }
                    getContext().sendBroadcastAsUser(mIdleIntent, UserHandle.ALL);
                    EventLogTags.writeDeviceIdleOnComplete();
```