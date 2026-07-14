package com.remoteaudiosync.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MediaNotificationListenerService : NotificationListenerService() {
    companion object {
        var listener: NotificationListener? = null

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, MediaNotificationListenerService::class.java)
        }
    }

    interface NotificationListener {
        fun onNotificationPosted(
            id: String,
            title: String,
            text: String,
            packageName: String,
            appName: String,
            timestamp: Long,
            isOngoing: Boolean
        )
        fun onNotificationRemoved(id: String, packageName: String)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        
        val id = sbn.key ?: sbn.id.toString()
        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val isOngoing = sbn.isOngoing
        val timestamp = sbn.postTime
        val pm = packageManager
        val appName = try {
            if (pm != null) {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } else {
                packageName
            }
        } catch (e: Exception) {
            packageName
        }

        listener?.onNotificationPosted(id, title, text, packageName, appName, timestamp, isOngoing)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        
        val id = sbn.key ?: sbn.id.toString()
        val packageName = sbn.packageName ?: ""
        listener?.onNotificationRemoved(id, packageName)
    }
}
