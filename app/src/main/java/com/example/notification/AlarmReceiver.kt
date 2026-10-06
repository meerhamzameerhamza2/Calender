package com.example.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.DynamicIconManager

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.example.ACTION_DISMISS_ALARM") {
            // Turn off alarm ring and vibration
            AlarmSoundPlayer.stop()

            // Dismiss the notification
            val notifId = intent.getIntExtra("NOTIFICATION_ID", -1)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notifId != -1) {
                manager?.cancel(notifId)
            }
            return
        }

        if (action == "com.example.ACTION_MIDNIGHT_ICON_UPDATE" ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            DynamicIconManager.updateDynamicIcon(context)
            return
        }

        val eventId = intent.getIntExtra("EVENT_ID", 0)
        val title = intent.getStringExtra("EVENT_TITLE") ?: "Calendar Event"
        val details = intent.getStringExtra("EVENT_DETAILS") ?: "Upcoming event"
        val alertType = intent.getStringExtra("EVENT_ALERT_TYPE") ?: "Alarm & Sound, Vibrate"

        NotificationHelper.showEventNotification(context, title, details, eventId, alertType)
    }
}

