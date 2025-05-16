package com.stardust.notification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import com.stardust.view.accessibility.NotificationListener
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by Stardust on 2017/10/30.
 */

class NotificationListenerService : android.service.notification.NotificationListenerService() {

    private val mNotificationListeners = CopyOnWriteArrayList<NotificationListener>()

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        onNotificationPosted(sbn)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        for (listener in mNotificationListeners) {
            listener.onNotification(com.stardust.notification.Notification.create(
                    sbn.notification, sbn.packageName))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}

    override fun onNotificationRemoved(sbn: StatusBarNotification, rankingMap: RankingMap) {}

    fun addListener(listener: NotificationListener) {
        mNotificationListeners.add(listener)
    }

    fun removeListener(listener: NotificationListener): Boolean {
        return mNotificationListeners.remove(listener)
    }


    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    companion object {
        var instance: NotificationListenerService? = null
            private set

        fun hasNotificationAccess(context: Context): Boolean {
            val cn = ComponentName(context, NotificationListenerService::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            return flat.contains(cn.flattenToString())
        }

        fun toSettings(context: Context) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
