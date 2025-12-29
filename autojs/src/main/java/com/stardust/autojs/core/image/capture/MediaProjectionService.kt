package com.stardust.autojs.core.image.capture

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stardust.autojs.R

/**
 * 前台服务
 * 功能说明：
 * - 适配 Android 10+ 的前台服务类型要求
 * - 支持通过通知停止服务
 */
class MediaProjectionService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val ACTION_STOP = "action.STOP_CAPTURE"

        fun start(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MediaProjectionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        if (intent.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notif = buildNotification()
        @SuppressLint("InlinedApi")
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /*构建通知*/
    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, MediaProjectionService::class.java).apply {
            action = ACTION_STOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.autojs_material)
            .setWhen(System.currentTimeMillis()).setContentTitle("屏幕录制")
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentText("正在后台录制屏幕")
            .setOngoing(true).setColor(Color.RED).addAction(
                0, "停止录制", stopPendingIntent
            ).build()
    }

    /*创建通知渠道（Android 8.0+）*/
    private fun createNotificationChannel() {
        val manager = NotificationManagerCompat.from(this)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW
        ).setName("屏幕录制").build()
        manager.createNotificationChannel(channel)
    }
}