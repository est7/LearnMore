package com.cxyzy.demo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var sensorHelper: SensorManagerHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorHelper = SensorManagerHelper(this)
        sensorHelper.setOnShakeListener(object : SensorManagerHelper.OnShakeListener {
            override fun onShake() {
                Toast.makeText(this@MainActivity, IpGetUtil.getIPAddress(this@MainActivity), Toast.LENGTH_SHORT).show()
            }
        })
    }

    object IpGetUtil {
        fun getIPAddress(context: Context): String? {
            val info: NetworkInfo = (context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).getActiveNetworkInfo()
            if (info != null && info.isConnected()) {
                if (info.getType() === ConnectivityManager.TYPE_MOBILE) { //当前使用2G/3G/4G网络
                    try {
                        //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
                        while (en.hasMoreElements()) {
                            val intf: NetworkInterface = en.nextElement()
                            val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                            while (enumIpAddr.hasMoreElements()) {
                                val inetAddress: InetAddress = enumIpAddr.nextElement()
                                if (!inetAddress.isLoopbackAddress() && inetAddress is Inet4Address) {
                                    return inetAddress.getHostAddress()
                                }
                            }
                        }
                    } catch (e: SocketException) {
                        e.printStackTrace()
                    }
                } else if (info.getType() === ConnectivityManager.TYPE_WIFI) { //当前使用无线网络
                    val wifiManager: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo: WifiInfo = wifiManager.getConnectionInfo()
                    return intIP2StringIP(wifiInfo.getIpAddress())
                }
            } else {
                //当前无网络连接,请在设置中打开网络
            }
            return null
        }

        /**
         * 将得到的int类型的IP转换为String类型
         *
         * @param ip
         * @return
         */
        fun intIP2StringIP(ip: Int): String {
            return (ip and 0xFF).toString() + "." +
                    (ip shr 8 and 0xFF) + "." +
                    (ip shr 16 and 0xFF) + "." +
                    (ip shr 24 and 0xFF)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        sensorHelper.stop()
    }
}
