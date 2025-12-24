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
import android.os.Vibrator
import android.provider.Settings
import android.telephony.TelephonyManager
import com.stardust.autojs.R
import com.stardust.autojs.permission.PermissionManager.hasPermission
import com.stardust.autojs.permission.PermissionManager.requestPermission
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.pio.UncheckedIOException
import com.stardust.util.ScreenMetrics
import java.io.File
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

class Device(private val context: Context) {

    val width: Int by lazy { ScreenMetrics.getDeviceScreenWidth() }
    val height: Int by lazy { ScreenMetrics.getDeviceScreenHeight() }
    val buildId: String by lazy { Build.ID }
    val buildDisplay: String by lazy { Build.DISPLAY }
    val product: String by lazy { Build.PRODUCT }
    val board: String by lazy { Build.BOARD }
    val brand: String by lazy { Build.BRAND }
    val device: String by lazy { Build.DEVICE }
    val model: String by lazy { Build.MODEL }
    val bootloader: String by lazy { Build.BOOTLOADER }
    val hardware: String by lazy { Build.HARDWARE }
    val fingerprint: String by lazy { Build.FINGERPRINT }
    val sdkInt: Int by lazy { Build.VERSION.SDK_INT }
    val incremental: String by lazy { Build.VERSION.INCREMENTAL }
    val release: String by lazy { Build.VERSION.RELEASE }
    val baseOS: String? by lazy {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Build.VERSION.BASE_OS
            else -> null
        }

    }
    val securityPatch: String? by lazy {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Build.VERSION.SECURITY_PATCH
            else -> null
        }
    }
    val codename: String by lazy { Build.VERSION.CODENAME }

    @Suppress("DEPRECATION")
    val serial: String by lazy {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Build.getSerial()
            else -> Build.SERIAL
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockFlag: Int = 0

    private companion object {
        const val FAKE_MAC_ADDRESS = "02:00:00:00:00:00"
    }

    @SuppressLint("HardwareIds")
    fun getIMEI(): String? {
        checkReadPhoneStatePermission()
        return try {
            (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
        } catch (_: SecurityException) {
            null
        }
    }

    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getBrightness(): Int {
        return Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    }

    fun getBrightnessMode(): Int {
        return Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )
    }

    fun setBrightness(value: Int) {
        checkWriteSettingsPermission()
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
    }

    fun setBrightnessMode(value: Int) {
        checkWriteSettingsPermission()
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            value
        )
    }

    private inline fun <reified T> getSystemService(serviceName: String): T {
        @Suppress("UNCHECKED_CAST")
        return context.getSystemService(serviceName) as? T
            ?: throw RuntimeException("should never happen...$serviceName")
    }

    fun getMusicVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    fun getNotificationVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamVolume(AudioManager.STREAM_NOTIFICATION)
    }

    fun getAlarmVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamVolume(AudioManager.STREAM_ALARM)
    }

    fun getMusicMaxVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    fun getNotificationMaxVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
    }

    fun getAlarmMaxVolume(): Int {
        return getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .getStreamMaxVolume(AudioManager.STREAM_ALARM)
    }

    fun setMusicVolume(value: Int) {
        checkWriteSettingsPermission()
        getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    }

    fun setAlarmVolume(value: Int) {
        checkWriteSettingsPermission()
        getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .setStreamVolume(AudioManager.STREAM_ALARM, value, 0)
    }

    fun setNotificationVolume(value: Int) {
        checkWriteSettingsPermission()
        getSystemService<AudioManager>(Context.AUDIO_SERVICE)
            .setStreamVolume(AudioManager.STREAM_NOTIFICATION, value, 0)
    }

    fun getBattery(): Float {
        val batteryIntent =
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                ?: return -1f
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val battery = (level.toFloat() / scale) * 100.0f
        return Math.round(battery * 10) / 10.0f
    }

    fun getTotalMem(): Long {
        val activityManager = getSystemService<ActivityManager>(Context.ACTIVITY_SERVICE)
        val info = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(info)
        return info.totalMem
    }

    fun getAvailMem(): Long {
        val activityManager = getSystemService<ActivityManager>(Context.ACTIVITY_SERVICE)
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
        wakeLock?.acquire(timeout)
    }

    @SuppressLint("WakelockTimeout")
    fun keepAwake(flags: Int) {
        checkWakeLock(flags)
        wakeLock?.acquire()
    }

    fun isScreenOn(): Boolean {
        return getSystemService<PowerManager>(Context.POWER_SERVICE).isScreenOn
    }

    fun wakeUpIfNeeded() {
        if (!isScreenOn()) {
            wakeUp()
        }
    }

    fun wakeUp() {
        keepScreenOn(200)
    }

    fun keepScreenOn() {
        keepAwake(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    fun keepScreenOn(timeout: Long) {
        keepAwake(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            timeout
        )
    }

    fun keepScreenDim() {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    fun keepScreenDim(timeout: Long) {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, timeout)
    }

    private fun checkWakeLock(flags: Int) {
        if (wakeLock == null || flags != wakeLockFlag) {
            cancelKeepingAwake()
            val powerManager = getSystemService<PowerManager>(Context.POWER_SERVICE)
            wakeLock = powerManager.newWakeLock(flags, Device::class.java.name)
            wakeLockFlag = flags
        }
    }

    fun cancelKeepingAwake() {
        wakeLock?.takeIf { it.isHeld }?.release()
    }

    fun vibrate(millis: Long) {
        getSystemService<Vibrator>(Context.VIBRATOR_SERVICE).vibrate(millis)
    }

    fun cancelVibration() {
        getSystemService<Vibrator>(Context.VIBRATOR_SERVICE).cancel()
    }

    private fun checkWriteSettingsPermission() {
        if (hasPermission(context, Manifest.permission.WRITE_SETTINGS)) {
            return
        }
        requestPermission(context, Manifest.permission.WRITE_SETTINGS)
        throw SecurityException(context.getString(R.string.no_write_settings_permissin))
    }

    private fun checkReadPhoneStatePermission() {
        if (hasPermission(context, Manifest.permission.READ_PHONE_STATE).not()) {
            throw SecurityException(context.getString(R.string.no_read_phone_state_permissin))
        }
    }

    @SuppressLint("HardwareIds")
    fun getMacAddress(): String? {
        val wifiMan = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val wifiInf = wifiMan.connectionInfo ?: return getMacByFile()

        var mac = wifiInf.macAddress
        if (FAKE_MAC_ADDRESS == mac) {
            mac = null
        }
        if (mac == null) {
            mac = getMacByInterface()
            if (mac == null) {
                mac = getMacByFile()
            }
        }
        return mac
    }

    private fun getMacByInterface(): String? {
        val networkInterfaces = try {
            Collections.list(NetworkInterface.getNetworkInterfaces())
        } catch (_: SocketException) {
            return null
        }

        for (networkInterface in networkInterfaces) {
            if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                val macBytes = networkInterface.hardwareAddress ?: return null

                val mac = StringBuilder()
                for (b in macBytes) {
                    mac.append(String.format("%02X:", b))
                }

                if (mac.isNotEmpty()) {
                    mac.deleteCharAt(mac.length - 1)
                }
                return mac.toString()
            }
        }
        return null
    }

    private fun getMacByFile(): String? {
        return try {
            File("/sys/class/net/wlan0/address").readText()
        } catch (_: UncheckedIOException) {
            null
        }
    }

    override fun toString(): String {
        return "Device1{" +
                "width=$width" +
                ", height=$height" +
                ", buildId='$buildId'" +
                ", buildDisplay='$buildDisplay'" +
                ", product='$product'" +
                ", board='$board'" +
                ", brand='$brand'" +
                ", device='$device'" +
                ", model='$model'" +
                ", bootloader='$bootloader'" +
                ", hardware='$hardware'" +
                ", fingerprint='$fingerprint'" +
                ", sdkInt=$sdkInt" +
                ", incremental='$incremental'" +
                ", release='$release'" +
                ", baseOS='$baseOS'" +
                ", securityPatch='$securityPatch'" +
                ", serial='$serial'" +
                '}'
    }

}