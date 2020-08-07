package com.cxyzy.demo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

/**
 * 重力加速度检测类
 */
class SensorManagerHelper(private val context: Context) : SensorEventListener {
    // 两次检测的时间间隔
    private val UPTATE_INTERVAL_TIME = 800
    // 传感器管理器
    private var sensorManager: SensorManager? = null
    // 传感器
    private var sensor: Sensor? = null
    // 重力感应监听器
    private var onShakeListener: OnShakeListener? = null

    private var lastUpdateTime: Long = 0

    init {
        start()
    }

    fun start() {
        sensorManager = context
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager != null) {

            sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        if (sensor != null) {
            sensorManager!!.registerListener(
                this, sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    fun stop() {
        sensorManager!!.unregisterListener(this)
    }

    interface OnShakeListener {
        fun onShake()
    }

    fun setOnShakeListener(listener: OnShakeListener) {
        onShakeListener = listener
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentUpdateTime = System.currentTimeMillis()
        val timeInterval = currentUpdateTime - lastUpdateTime

        if (timeInterval < UPTATE_INTERVAL_TIME) return
        lastUpdateTime = currentUpdateTime
        // 获得x,y,z坐标
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
      if((Math.abs(x) > 10 || Math.abs(y) > 10 || Math
                      .abs(z) > 10)){
            onShakeListener!!.onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

}
