package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.notification.AlarmReceiver
import com.example.widget.DynamicCalendarWidgetProvider
import java.util.Calendar

object DynamicIconManager {
    private const val TAG = "DynamicIconManager"

    fun updateDynamicIcon(context: Context) {
        try {
            val pm = context.packageManager
            val packageName = context.packageName
            val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

            // Discover actual registered activities/aliases in the package
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                }
            } catch (e: Exception) {
                null
            }

            val registeredActivities = packageInfo?.activities?.map { it.name }?.toSet() ?: emptySet()

            fun resolveComponentName(day: Int): ComponentName? {
                val suffix = "MainActivityDay$day"
                val match = registeredActivities.firstOrNull { it.endsWith(suffix) }
                    ?: if (registeredActivities.contains("com.example.$suffix")) "com.example.$suffix"
                    else if (registeredActivities.contains("$packageName.$suffix")) "$packageName.$suffix"
                    else "com.example.$suffix"

                return ComponentName(packageName, match)
            }

            // Enable today's alias
            val activeComponent = resolveComponentName(today)
            if (activeComponent != null) {
                try {
                    val currentState = pm.getComponentEnabledSetting(activeComponent)
                    if (currentState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                        pm.setComponentEnabledSetting(
                            activeComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP
                        )
                        Log.d(TAG, "Enabled launcher alias: ${activeComponent.className}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not enable alias ${activeComponent.className}: ${e.message}")
                }
            }

            // Disable all other aliases
            for (day in 1..31) {
                if (day != today) {
                    val otherComponent = resolveComponentName(day)
                    if (otherComponent != null) {
                        try {
                            val otherState = pm.getComponentEnabledSetting(otherComponent)
                            if (otherState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                                pm.setComponentEnabledSetting(
                                    otherComponent,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                    PackageManager.DONT_KILL_APP
                                )
                            }
                        } catch (e: Exception) {
                            // Silently ignore if component not found or restricted
                        }
                    }
                }
            }

            // Also update the 1x1 Dynamic Calendar Widget
            DynamicCalendarWidgetProvider.triggerUpdate(context)

            // Schedule the next midnight update
            scheduleMidnightUpdate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating dynamic icon", e)
        }
    }

    fun scheduleMidnightUpdate(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.example.ACTION_MIDNIGHT_ICON_UPDATE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Calculate next midnight (00:00:02 tomorrow)
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 2)
                set(Calendar.MILLISECOND, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled next midnight icon update for: ${nextMidnight.time}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling midnight update", e)
        }
    }
}
