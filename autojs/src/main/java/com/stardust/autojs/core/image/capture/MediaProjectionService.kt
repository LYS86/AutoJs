package com.stardust.autojs.core.image.capture

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stardust.autojs.R

class MediaProjectionService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MediaProjectionService = this@MediaProjectionService
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
    }

    private fun startForegroundService() {
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.media_projection_notification_title))
            .setContentText(getString(R.string.media_projection_notification_text))
            .setSmallIcon(R.drawable.autojs_material)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.media_projection_notification_channel_name)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(name)
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .setSound(null, null)
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        val CHANNEL_ID: String = "${MediaProjectionService::class.simpleName}.channel"

        private var isRunning = false

        @JvmStatic
        @Synchronized
        fun start(context: Context) {
            if (isRunning) return
            isRunning = true
            ContextCompat.startForegroundService(
                context,
                Intent(context, MediaProjectionService::class.java)
            )
        }

        @JvmStatic
        @Synchronized
        fun stop(context: Context) {
            if (!isRunning) return
            isRunning = false
            context.stopService(Intent(context, MediaProjectionService::class.java))
        }

        @JvmStatic
        @Synchronized
        fun isServiceRunning(): Boolean = isRunning
    }
}
