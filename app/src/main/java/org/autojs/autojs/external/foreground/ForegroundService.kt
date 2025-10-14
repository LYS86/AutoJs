package org.autojs.autojs.external.foreground

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.PendingIntentCompat
import androidx.core.content.ContextCompat
import org.autojs.autojs.R
import org.autojs.autojs.ui.main.MainActivity
import timber.log.Timber

class ForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private val CHANNEL_ID = "${ForegroundService::class.java.name}.foreground"

        fun start(context: Context) {
            val intent = Intent(context, ForegroundService::class.java)
            ContextCompat. startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ForegroundService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
//        Timber.d("ForegroundService created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
            createNotificationChannel()

        val contentIntent = PendingIntentCompat.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT,
            false
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.autojs_material)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent).setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = NotificationManagerCompat.from(this)
        val hasChannel = manager.getNotificationChannel(CHANNEL_ID)
        if (hasChannel != null) return

        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_DEFAULT
        ).setName(getString(R.string.foreground_notification_channel_name))
            .setDescription(getString(R.string.foreground_notification_channel_name)).build()
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        super.onDestroy()
    }
}