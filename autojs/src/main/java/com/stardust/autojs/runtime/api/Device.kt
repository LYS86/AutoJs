package com.stardust.autojs.runtime.api

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.TelephonyManager
import com.stardust.autojs.R
import com.stardust.autojs.permission.PermissionManager
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.util.ScreenMetrics
import java.io.File
import java.io.IOException
import java.net.NetworkInterface
import kotlin.math.roundToInt

class Device(private val context: Context) {

    companion object {
        @JvmField
        val width: Int = ScreenMetrics.getDeviceScreenWidth()

        @JvmField
        val height: Int = ScreenMetrics.getDeviceScreenHeight()

        @JvmField
        val buildId: String = Build.ID

        @JvmField
        val buildDisplay: String = Build.DISPLAY

        @JvmField
        val product: String = Build.PRODUCT

        @JvmField
        val board: String = Build.BOARD

        @JvmField
        val brand: String = Build.BRAND

        @JvmField
        val device: String = Build.DEVICE

        @JvmField
        val model: String = Build.MODEL

        @JvmField
        val bootloader: String = Build.BOOTLOADER

        @JvmField
        val hardware: String = Build.HARDWARE

        @JvmField
        val fingerprint: String = Build.FINGERPRINT

        @SuppressLint("AnnotateVersionCheck")
        @JvmField
        val sdkInt: Int = Build.VERSION.SDK_INT

        @JvmField
        val incremental: String = Build.VERSION.INCREMENTAL

        @JvmField
        val release: String = Build.VERSION.RELEASE

        @JvmField
        val baseOS: String = Build.VERSION.BASE_OS

        @JvmField
        val securityPatch: String = Build.VERSION.SECURITY_PATCH

        @JvmField
        val codename: String = Build.VERSION.CODENAME

        @JvmField
        val serial: String = getSerialSafely()

        private const val FAKE_MAC_ADDRESS = "02:00:00:00:00:00"

        @SuppressLint("HardwareIds", "MissingPermission")
        private fun getSerialSafely(): String {
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Build.getSerial()
                } else {
                    @Suppress("DEPRECATION") Build.SERIAL
                }
            } catch (_: SecurityException) {
                Build.UNKNOWN
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockFlag: Int = 0

    @SuppressLint("HardwareIds", "MissingPermission")
    fun getIMEI(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        checkReadPhoneStatePermission()
        return try {
            val tm = context.getSystemService(TelephonyManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) tm?.imei
            else @Suppress("DEPRECATION") tm?.deviceId
        } catch (_: SecurityException) {
            null
        }
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    @Throws(Settings.SettingNotFoundException::class)
    fun getBrightness(): Int {
        return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    @Throws(Settings.SettingNotFoundException::class)
    fun getBrightnessMode(): Int {
        return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    }

    fun setBrightness(b: Int) {
        checkWriteSettingsPermission()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, b)
    }

    fun setBrightnessMode(b: Int) {
        checkWriteSettingsPermission()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, b)
    }

    fun getMusicVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    fun getNotificationVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)

    fun getAlarmVolume(): Int = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

    fun getMusicMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    fun getNotificationMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)

    fun getAlarmMaxVolume(): Int = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)

    fun setMusicVolume(i: Int) {
        checkWriteSettingsPermission()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
    }

    fun setAlarmVolume(i: Int) {
        checkWriteSettingsPermission()
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, i, 0)
    }

    fun setNotificationVolume(i: Int) {
        checkWriteSettingsPermission()
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, i, 0)
    }

    fun getBattery(): Float {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return -1f
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val battery = (level.toFloat() / scale) * 100.0f
        return (battery * 10).roundToInt() / 10f
    }

    fun getTotalMem(): Long {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.totalMem
    }

    fun getAvailMem(): Long {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.availMem
    }

    fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: throw ScriptException("Cannot retrieve the battery state")
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
    }

    fun keepAwake(flags: Int, timeout: Long) {
        checkWakeLock(flags)
        wakeLock!!.acquire(timeout)
    }

    @SuppressLint("WakelockTimeout")
    fun keepAwake(flags: Int) {
        checkWakeLock(flags)
        wakeLock!!.acquire()
    }

    fun isScreenOn(): Boolean {
        return context.getSystemService(PowerManager::class.java).isInteractive
    }

    fun wakeUpIfNeeded() {
        if (!isScreenOn()) wakeUp()
    }

    fun wakeUp() {
        keepScreenOn(200)
    }

    @Suppress("DEPRECATION")
    fun keepScreenOn() {
        keepAwake(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    @Suppress("DEPRECATION")
    fun keepScreenOn(timeout: Long) {
        keepAwake(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, timeout)
    }

    @Suppress("DEPRECATION")
    fun keepScreenDim() {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    @Suppress("DEPRECATION")
    fun keepScreenDim(timeout: Long) {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, timeout)
    }

    private fun checkWakeLock(flags: Int) {
        if (wakeLock == null || flags != wakeLockFlag) {
            cancelKeepingAwake()
            wakeLock = context.getSystemService(PowerManager::class.java).newWakeLock(flags, Device::class.java.name)
            wakeLockFlag = flags
        }
    }

    fun cancelKeepingAwake() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    fun vibrate(millis: Long) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(millis)
        }
    }

    fun cancelVibration() {
        context.getSystemService(Vibrator::class.java).cancel()
    }

    private fun checkWriteSettingsPermission() {
        if (PermissionManager.canWriteSettings(context)) return
        throw SecurityException(context.getString(R.string.no_write_settings_permissin))
    }

    private fun checkReadPhoneStatePermission() {
        if (PermissionManager.checkCompat(context, Manifest.permission.READ_PHONE_STATE)) return
        throw SecurityException(context.getString(R.string.no_read_phone_state_permissin))
    }

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    fun getMacAddress(): String? {
        val wifiMan = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val wifiInf = wifiMan.connectionInfo ?: return getMacByFile()
        val mac = wifiInf.macAddress?.takeIf { it != FAKE_MAC_ADDRESS }
        return mac ?: getMacByInterface() ?: getMacByFile()
    }

    private fun getMacByInterface(): String? {
        return NetworkInterface.getNetworkInterfaces()?.asSequence()?.find {
            it.name.equals("wlan0",
                           ignoreCase = true)
        }?.hardwareAddress?.joinToString(":") { String.format("%02X", it) }
    }

    private fun getMacByFile(): String? {
        return try {
            File("/sys/class/net/wlan0/address").readText()
        } catch (_: IOException) {
            null
        }
    }

    private val audioManager: AudioManager
        get() = context.getSystemService(AudioManager::class.java)

    override fun toString(): String {
        return "Device(width=$width, height=$height, buildId='$buildId', buildDisplay='$buildDisplay', product='$product', board='$board', brand='$brand', device='$device', model='$model', bootloader='$bootloader', hardware='$hardware', fingerprint='$fingerprint', sdkInt=$sdkInt, incremental='$incremental', release='$release', baseOS='$baseOS', securityPatch='$securityPatch', serial='$serial')"
    }
}
