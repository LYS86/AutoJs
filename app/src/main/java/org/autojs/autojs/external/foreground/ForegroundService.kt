package org.autojs.autojs.external.foreground

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.autojs.autojs.R
import org.autojs.autojs.ui.main.MainActivity

class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onBind(intent: Intent): IBinder? = null
    private fun buildNotification(): Notification {
        createNotificationChannel()
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text)).setSmallIcon(R.drawable.logo)
            .setWhen(System.currentTimeMillis()).setContentIntent(contentIntent).setChannelId(CHANNEL_ID)
            .setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.foreground_notification_channel_name)
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).setName(name).setLightsEnabled(false).build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        val CHANNEL_ID = "${ForegroundService::class.simpleName}.channel"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ForegroundService::class.java))
        }
    }
}
