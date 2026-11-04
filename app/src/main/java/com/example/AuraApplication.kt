package com.example

import android.app.Application
import com.example.data.local.AuraDatabase
import com.example.notifications.AlarmScheduler
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuraApplication : Application() {

    lateinit var database: AuraDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AuraDatabase.getInstance(this)
        NotificationHelper.createNotificationChannels(this)

        CoroutineScope(Dispatchers.IO).launch {
            // Ensure sample data is initialized and alarms scheduled
            AlarmScheduler.rescheduleAll(this@AuraApplication)
        }
    }
}
