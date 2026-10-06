package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.EventEntity

object NotificationHelper {
    const val CHANNEL_ID = "calendar_reminders_channel"
    const val CHANNEL_NAME = "Calendar Reminders"

    const val CHANNEL_ALARM_ID = "calendar_alarm_channel"
    const val CHANNEL_ALARM_NAME = "Calendar Alarms"

    const val CHANNEL_VIBRATE_ID = "calendar_vibrate_channel"
    const val CHANNEL_VIBRATE_NAME = "Calendar Silent Vibrate"

    const val CHANNEL_SILENT_ID = "calendar_silent_channel"
    const val CHANNEL_SILENT_NAME = "Calendar Silent"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributesAlarm = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val audioAttributesNotif = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // 1. Alarm Channel (High priority + Alarm Sound + Strong Vibration)
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                CHANNEL_ALARM_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms with sound and strong vibration"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributesAlarm)
            }
            manager.createNotificationChannel(alarmChannel)

            // 2. Standard Reminder Channel (Notification sound + Vibration)
            val notifChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled events and important milestones"
                enableLights(true)
                enableVibration(true)
                setSound(notifSound, audioAttributesNotif)
            }
            manager.createNotificationChannel(notifChannel)

            // 3. Vibrate Only Channel
            val vibrateChannel = NotificationChannel(
                CHANNEL_VIBRATE_ID,
                CHANNEL_VIBRATE_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Vibration only without sound"
                enableVibration(true)
                setSound(null, null)
            }
            manager.createNotificationChannel(vibrateChannel)

            // 4. Silent Channel
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT_ID,
                CHANNEL_SILENT_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Silent notifications without sound or vibration"
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(silentChannel)
        }
    }

    fun showEventNotification(
        context: Context,
        eventTitle: String,
        eventDetails: String,
        eventId: Int,
        alertType: String = "Alarm & Sound, Vibrate"
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to turn off alarm directly from the notification popup
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_DISMISS_ALARM"
            putExtra("NOTIFICATION_ID", eventId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            eventId + 200000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelToUse = when {
            alertType.contains("Alarm", ignoreCase = true) -> CHANNEL_ALARM_ID
            alertType.contains("Vibrate", ignoreCase = true) && !alertType.contains("Alarm", ignoreCase = true) -> CHANNEL_VIBRATE_ID
            alertType.contains("Silent", ignoreCase = true) -> CHANNEL_SILENT_ID
            else -> CHANNEL_ID
        }

        val builder = NotificationCompat.Builder(context, channelToUse)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(eventTitle)
            .setContentText(eventDetails)
            .setStyle(NotificationCompat.BigTextStyle().bigText(eventDetails))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "OK (Turn Off Alarm)",
                dismissPendingIntent
            )

        when {
            alertType.contains("Alarm", ignoreCase = true) -> {
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setSound(sound)
                // Start sustained alarm ring and vibration until user clicks OK
                AlarmSoundPlayer.play(context, withSound = true, withVibrate = true)
            }
            alertType.contains("Vibrate", ignoreCase = true) -> {
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                builder.setVibrate(longArrayOf(0, 400, 200, 400))
                builder.setSound(null)
                AlarmSoundPlayer.play(context, withSound = false, withVibrate = true)
            }
            alertType.contains("Silent", ignoreCase = true) -> {
                builder.setPriority(NotificationCompat.PRIORITY_LOW)
                builder.setSound(null)
            }
            else -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(eventId, builder.build())
    }

    fun scheduleAlarm(context: Context, event: EventEntity) {
        if (event.reminderMinutesBefore < 0) return

        val reminderTime = event.startTimestamp - (event.reminderMinutesBefore * 60 * 1000L)
        if (reminderTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EVENT_ID", event.id.toInt())
            putExtra("EVENT_TITLE", event.title)
            putExtra("EVENT_DETAILS", if (event.location.isNotBlank()) "Location: ${event.location}" else "Calendar Reminder")
            putExtra("EVENT_ALERT_TYPE", event.alertType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, eventId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}
