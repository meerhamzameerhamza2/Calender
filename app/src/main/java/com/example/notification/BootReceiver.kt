package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            com.example.util.DynamicIconManager.updateDynamicIcon(context)
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val events = db.eventDao().getAllEvents().first()
                val now = System.currentTimeMillis()
                for (event in events) {
                    if (event.startTimestamp > now) {
                        NotificationHelper.scheduleAlarm(context, event)
                    }
                }
            }
        }
    }
}
